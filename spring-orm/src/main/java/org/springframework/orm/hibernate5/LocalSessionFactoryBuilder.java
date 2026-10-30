/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.orm.hibernate5;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.InfrastructureProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Spring 提供的标准 Hibernate {@link Configuration} 类扩展，
 * 默认添加 {@link SpringSessionContext}，并提供便捷方式来
 * 指定 JDBC {@link DataSource} 和应用程序类加载器。
 *
 * <p>此设计用于编程式使用，例如在 {@code @Bean} 工厂方法中；
 * 对于 XML bean 定义文件，请考虑使用 {@link LocalSessionFactoryBean}。
 * 通常与 {@link HibernateTransactionManager} 结合使用，以便针对
 * {@code SessionFactory} 及其 JDBC {@code DataSource} 进行声明式事务管理。
 *
 * <p>自 Spring 5.3 起，兼容 Hibernate 5.2/5.3/5.4。
 * 这个 Hibernate 专用的工厂构建器也是设置 JPA {@code EntityManagerFactory}
 * 的便捷方式，因为 Hibernate {@code SessionFactory} 现在也原生暴露了
 * JPA {@code EntityManagerFactory} 接口。
 *
 * <p>此构建器支持 Hibernate 5.3/5.4 {@code BeanContainer} 集成、
 * 来自自定义 {@link BootstrapServiceRegistryBuilder} 配置的 {@link MetadataSources}，
 * 以及超出标准 JPA 引导契约的其他高级 Hibernate 配置选项。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see HibernateTransactionManager
 * @see LocalSessionFactoryBean
 * @see #setBeanContainer
 * @see #LocalSessionFactoryBuilder(DataSource, ResourceLoader, MetadataSources)
 * @see BootstrapServiceRegistryBuilder
 */
@SuppressWarnings("serial")
public class LocalSessionFactoryBuilder extends Configuration {

	private static final String RESOURCE_PATTERN = "/**/*.class";

	private static final String PACKAGE_INFO_SUFFIX = ".package-info";

	private static final TypeFilter[] DEFAULT_ENTITY_TYPE_FILTERS = new TypeFilter[] {
			new AnnotationTypeFilter(Entity.class, false),
			new AnnotationTypeFilter(Embeddable.class, false),
			new AnnotationTypeFilter(MappedSuperclass.class, false)};

	private static final TypeFilter CONVERTER_TYPE_FILTER = new AnnotationTypeFilter(Converter.class, false);


	private final ResourcePatternResolver resourcePatternResolver;

	@Nullable
	private TypeFilter[] entityTypeFilters = DEFAULT_ENTITY_TYPE_FILTERS;


	/**
	 * 为给定的 DataSource 创建一个新的 LocalSessionFactoryBuilder。
	 * @param dataSource 生成的 Hibernate SessionFactory 应使用的 JDBC DataSource（可以为 {@code null}）
	 */
	public LocalSessionFactoryBuilder(@Nullable DataSource dataSource) {
		this(dataSource, new PathMatchingResourcePatternResolver());
	}

	/**
	 * 为给定的 DataSource 创建一个新的 LocalSessionFactoryBuilder。
	 * @param dataSource 生成的 Hibernate SessionFactory 应使用的 JDBC DataSource（可以为 {@code null}）
	 * @param classLoader 用于加载应用类的 ClassLoader
	 */
	public LocalSessionFactoryBuilder(@Nullable DataSource dataSource, ClassLoader classLoader) {
		this(dataSource, new PathMatchingResourcePatternResolver(classLoader));
	}

	/**
	 * 为给定的 DataSource 创建一个新的 LocalSessionFactoryBuilder。
	 * @param dataSource 生成的 Hibernate SessionFactory 应使用的 JDBC DataSource（可以为 {@code null}）
	 * @param resourceLoader 用于加载应用类的 ResourceLoader
	 */
	public LocalSessionFactoryBuilder(@Nullable DataSource dataSource, ResourceLoader resourceLoader) {
		this(dataSource, resourceLoader, new MetadataSources(
				new BootstrapServiceRegistryBuilder().applyClassLoader(resourceLoader.getClassLoader()).build()));
	}

	/**
	 * 为给定的 DataSource 创建一个新的 LocalSessionFactoryBuilder。
	 * @param dataSource 生成的 Hibernate SessionFactory 应使用的 JDBC DataSource（可以为 {@code null}）
	 * @param resourceLoader 用于加载应用类的 ResourceLoader
	 * @param metadataSources 要使用的 Hibernate MetadataSources 服务（例如复用已有的实例）
	 * @since 4.3
	 */
	public LocalSessionFactoryBuilder(
			@Nullable DataSource dataSource, ResourceLoader resourceLoader, MetadataSources metadataSources) {

		super(metadataSources);

		getProperties().put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, SpringSessionContext.class.getName());
		if (dataSource != null) {
			getProperties().put(AvailableSettings.DATASOURCE, dataSource);
		}
		getProperties().put(AvailableSettings.CONNECTION_HANDLING,
				PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD);

		getProperties().put(AvailableSettings.CLASSLOADERS, Collections.singleton(resourceLoader.getClassLoader()));
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}


	/**
	 * 设置要与 Hibernate 一起使用的 Spring {@link JtaTransactionManager}
	 * 或 JTA {@link TransactionManager}（如果有）。允许为 Hibernate 5 的
	 * 会话和缓存同步使用 Spring 管理的事务管理器，
	 * 并自动将 "hibernate.transaction.jta.platform" 设置为它。
	 * <p>传入的 Spring {@link JtaTransactionManager} 需要包含 JTA
	 * {@link TransactionManager} 引用才能在此使用，但 WebSphere
	 * 情况除外，此时我们会相应地自动设置 {@code WebSphereExtendedJtaPlatform}。
	 * <p>注意：如果设置了此项，Hibernate 设置中就不应包含 JTA 平台
	 * 设置，以避免无意义的重复配置。
	 */
	public LocalSessionFactoryBuilder setJtaTransactionManager(Object jtaTransactionManager) {
		Assert.notNull(jtaTransactionManager, "Transaction manager reference must not be null");

		if (jtaTransactionManager instanceof JtaTransactionManager) {
			boolean webspherePresent = ClassUtils.isPresent("com.ibm.wsspi.uow.UOWManager", getClass().getClassLoader());
			if (webspherePresent) {
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						"org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform");
			}
			else {
				JtaTransactionManager jtaTm = (JtaTransactionManager) jtaTransactionManager;
				if (jtaTm.getTransactionManager() == null) {
					throw new IllegalArgumentException(
							"Can only apply JtaTransactionManager which has a TransactionManager reference set");
				}
				getProperties().put(AvailableSettings.JTA_PLATFORM,
						new ConfigurableJtaPlatform(jtaTm.getTransactionManager(), jtaTm.getUserTransaction(),
								jtaTm.getTransactionSynchronizationRegistry()));
			}
		}
		else if (jtaTransactionManager instanceof TransactionManager) {
			getProperties().put(AvailableSettings.JTA_PLATFORM,
					new ConfigurableJtaPlatform((TransactionManager) jtaTransactionManager, null, null));
		}
		else {
			throw new IllegalArgumentException(
					"Unknown transaction manager type: " + jtaTransactionManager.getClass().getName());
		}

		getProperties().put(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta");
		getProperties().put(AvailableSettings.CONNECTION_HANDLING,
				PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT);

		return this;
	}

	/**
	 * 为给定的 Spring {@link ConfigurableListableBeanFactory} 设置
	 * Hibernate {@link org.hibernate.resource.beans.container.spi.BeanContainer}。
	 * <p>注意：Bean 容器集成要求 Hibernate 5.3 或更高版本。
	 * 它支持 Hibernate 属性转换器和实体监听器的自动装配。
	 * @since 5.1
	 * @see SpringBeanContainer
	 * @see AvailableSettings#BEAN_CONTAINER
	 */
	public LocalSessionFactoryBuilder setBeanContainer(ConfigurableListableBeanFactory beanFactory) {
		getProperties().put(AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(beanFactory));
		return this;
	}

	/**
	 * 设置 SessionFactory 要使用的 Hibernate {@link RegionFactory}。
	 * 允许使用 Spring 管理的 {@code RegionFactory} 实例。
	 * <p>注意：如果设置了此项，Hibernate 设置中就不应再定义
	 * 缓存提供器，以避免无意义的重复配置。
	 * @since 5.1
	 * @see AvailableSettings#CACHE_REGION_FACTORY
	 */
	public LocalSessionFactoryBuilder setCacheRegionFactory(RegionFactory cacheRegionFactory) {
		getProperties().put(AvailableSettings.CACHE_REGION_FACTORY, cacheRegionFactory);
		return this;
	}

	/**
	 * 设置要传递给 SessionFactory 的 {@link MultiTenantConnectionProvider}。
	 * @since 4.3
	 * @see AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
	 */
	public LocalSessionFactoryBuilder setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
		getProperties().put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
		return this;
	}

	/**
	 * 重写以确保将 {@link CurrentTenantIdentifierResolver} 可靠地传递给 SessionFactory。
	 * @since 4.3.2
	 * @see AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
	 */
	@Override
	public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		getProperties().put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
		super.setCurrentTenantIdentifierResolver(currentTenantIdentifierResolver);
	}

	/**
	 * 为基于 Spring 的实体类扫描指定自定义类型过滤器。
	 * <p>默认会在所有指定的包中搜索使用
	 * {@code @javax.persistence.Entity}、{@code @javax.persistence.Embeddable}
	 * 或 {@code @javax.persistence.MappedSuperclass} 注解的类。
	 * @see #scanPackages
	 */
	public LocalSessionFactoryBuilder setEntityTypeFilters(TypeFilter... entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
		return this;
	}

	/**
	 * 批量添加给定的注解类。
	 * @see #addAnnotatedClass
	 * @see #scanPackages
	 */
	public LocalSessionFactoryBuilder addAnnotatedClasses(Class<?>... annotatedClasses) {
		for (Class<?> annotatedClass : annotatedClasses) {
			addAnnotatedClass(annotatedClass);
		}
		return this;
	}

	/**
	 * 批量添加给定的注解包。
	 * @see #addPackage
	 * @see #scanPackages
	 */
	public LocalSessionFactoryBuilder addPackages(String... annotatedPackages) {
		for (String annotatedPackage : annotatedPackages) {
			addPackage(annotatedPackage);
		}
		return this;
	}

	/**
	 * 执行基于 Spring 的实体类扫描，将它们作为注解类
	 * 注册到此 {@code Configuration}。
	 * @param packagesToScan 一个或多个 Java 包名
	 * @throws HibernateException 如果扫描因任何原因失败
	 */
	@SuppressWarnings("unchecked")
	public LocalSessionFactoryBuilder scanPackages(String... packagesToScan) throws HibernateException {
		Set<String> entityClassNames = new TreeSet<>();
		Set<String> converterClassNames = new TreeSet<>();
		Set<String> packageNames = new TreeSet<>();
		try {
			for (String pkg : packagesToScan) {
				String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
						ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;
				Resource[] resources = this.resourcePatternResolver.getResources(pattern);
				MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(this.resourcePatternResolver);
				for (Resource resource : resources) {
					try {
						MetadataReader reader = readerFactory.getMetadataReader(resource);
						String className = reader.getClassMetadata().getClassName();
						if (matchesEntityTypeFilter(reader, readerFactory)) {
							entityClassNames.add(className);
						}
						else if (CONVERTER_TYPE_FILTER.match(reader, readerFactory)) {
							converterClassNames.add(className);
						}
						else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
							packageNames.add(className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
						}
					}
					catch (FileNotFoundException ex) {
						// Ignore non-readable resource
					}
				}
			}
		}
		catch (IOException ex) {
			throw new MappingException("Failed to scan classpath for unlisted classes", ex);
		}
		try {
			ClassLoader cl = this.resourcePatternResolver.getClassLoader();
			for (String className : entityClassNames) {
				addAnnotatedClass(ClassUtils.forName(className, cl));
			}
			for (String className : converterClassNames) {
				addAttributeConverter((Class<? extends AttributeConverter<?, ?>>) ClassUtils.forName(className, cl));
			}
			for (String packageName : packageNames) {
				addPackage(packageName);
			}
		}
		catch (ClassNotFoundException ex) {
			throw new MappingException("Failed to load annotated classes from classpath", ex);
		}
		return this;
	}

	/**
	 * 检查配置的任何实体类型过滤器是否匹配
	 * 元数据读取器中包含的当前类描述符。
	 */
	private boolean matchesEntityTypeFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
		if (this.entityTypeFilters != null) {
			for (TypeFilter filter : this.entityTypeFilters) {
				if (filter.match(reader, readerFactory)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 通过后台引导构建 Hibernate {@code SessionFactory}，
	 * 使用给定执行器进行并行初始化阶段
	 * （例如 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}）。
	 * <p>{@code SessionFactory} 初始化随后将切换到后台
	 * 引导模式，立即返回 {@code SessionFactory} 代理用于
	 * 注入目的，而不是等待 Hibernate 的引导完成。
	 * 但请注意，对 {@code SessionFactory} 方法的第一次实际调用
	 * 届时会阻塞，直到 Hibernate 引导完成（如果那时尚未就绪）。
	 * 为获得最大收益，请确保避免在相关 bean 的初始化方法中
	 * 提前调用 {@code SessionFactory}，即使只是为了元数据内省。
	 * @since 4.3
	 * @see #buildSessionFactory()
	 */
	public SessionFactory buildSessionFactory(AsyncTaskExecutor bootstrapExecutor) {
		Assert.notNull(bootstrapExecutor, "AsyncTaskExecutor must not be null");
		return (SessionFactory) Proxy.newProxyInstance(this.resourcePatternResolver.getClassLoader(),
				new Class<?>[] {SessionFactoryImplementor.class, InfrastructureProxy.class},
				new BootstrapSessionFactoryInvocationHandler(bootstrapExecutor));
	}


	/**
	 * 用于后台引导的代理调用处理器，仅在确实需要时
	 * 才强制要求目标 {@code SessionFactory} 完全初始化。
	 * @since 4.3
	 */
	private class BootstrapSessionFactoryInvocationHandler implements InvocationHandler {

		private final Future<SessionFactory> sessionFactoryFuture;

		public BootstrapSessionFactoryInvocationHandler(AsyncTaskExecutor bootstrapExecutor) {
			this.sessionFactoryFuture = bootstrapExecutor.submit(
					(Callable<SessionFactory>) LocalSessionFactoryBuilder.this::buildSessionFactory);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of EntityManagerFactory proxy.
					return System.identityHashCode(proxy);
				case "getProperties":
					return getProperties();
				case "getWrappedObject":
					// Call coming in through InfrastructureProxy interface...
					return getSessionFactory();
			}

			// Regular delegation to the target SessionFactory,
			// enforcing its full initialization...
			try {
				return method.invoke(getSessionFactory(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

		private SessionFactory getSessionFactory() {
			try {
				return this.sessionFactoryFuture.get();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted during initialization of Hibernate SessionFactory", ex);
			}
			catch (ExecutionException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof HibernateException) {
					// Rethrow a provider configuration exception (possibly with a nested cause) directly
					throw (HibernateException) cause;
				}
				throw new IllegalStateException("Failed to asynchronously initialize Hibernate SessionFactory: " +
						ex.getMessage(), cause);
			}
		}
	}

}
