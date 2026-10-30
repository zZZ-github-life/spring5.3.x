/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.ServiceRegistry;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 创建 Hibernate {@link SessionFactory} 的 {@link FactoryBean}。这是在 Spring 应用程序上下文中
 * 设置共享 Hibernate SessionFactory 的常用方式；随后可以通过依赖注入将
 * SessionFactory 传递给数据访问对象。
 *
 * <p>自 Spring 5.3 起，兼容 Hibernate 5.2/5.3/5.4。
 * 对于常见的 JPA 用途，这个 Hibernate 专用的 {@code LocalSessionFactoryBean}
 * 可以直接替代 {@link org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean}：
 * 特别是在 Hibernate 5.3/5.4 中，Hibernate {@code SessionFactory}
 * 会原生暴露 JPA {@code EntityManagerFactory} 接口，并且
 * 会自动注册 Hibernate {@code BeanContainer} 集成。
 * 与 {@link HibernateTransactionManager} 结合使用时，这自然允许在同一个事务中
 * 混合使用 JPA 访问代码和原生 Hibernate 访问代码。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setDataSource
 * @see #setPackagesToScan
 * @see HibernateTransactionManager
 * @see LocalSessionFactoryBuilder
 * @see org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
 */
public class LocalSessionFactoryBean extends HibernateExceptionTranslator
		implements FactoryBean<SessionFactory>, ResourceLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

	@Nullable
	private DataSource dataSource;

	@Nullable
	private Resource[] configLocations;

	@Nullable
	private String[] mappingResources;

	@Nullable
	private Resource[] mappingLocations;

	@Nullable
	private Resource[] cacheableMappingLocations;

	@Nullable
	private Resource[] mappingJarLocations;

	@Nullable
	private Resource[] mappingDirectoryLocations;

	@Nullable
	private Interceptor entityInterceptor;

	@Nullable
	private ImplicitNamingStrategy implicitNamingStrategy;

	@Nullable
	private PhysicalNamingStrategy physicalNamingStrategy;

	@Nullable
	private Object jtaTransactionManager;

	@Nullable
	private RegionFactory cacheRegionFactory;

	@Nullable
	private MultiTenantConnectionProvider multiTenantConnectionProvider;

	@Nullable
	private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

	@Nullable
	private Properties hibernateProperties;

	@Nullable
	private TypeFilter[] entityTypeFilters;

	@Nullable
	private Class<?>[] annotatedClasses;

	@Nullable
	private String[] annotatedPackages;

	@Nullable
	private String[] packagesToScan;

	@Nullable
	private AsyncTaskExecutor bootstrapExecutor;

	@Nullable
	private Integrator[] hibernateIntegrators;

	private boolean metadataSourcesAccessed = false;

	@Nullable
	private MetadataSources metadataSources;

	@Nullable
	private ResourcePatternResolver resourcePatternResolver;

	@Nullable
	private ConfigurableListableBeanFactory beanFactory;

	@Nullable
	private Configuration configuration;

	@Nullable
	private SessionFactory sessionFactory;


	/**
	 * 设置 SessionFactory 要使用的 DataSource。
	 * 如果设置，将覆盖 Hibernate 属性中的相应设置。
	 * <p>如果设置了此项，Hibernate 设置中就不应再定义
	 * 连接提供器，以避免无意义的重复配置。
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * 设置单个 Hibernate XML 配置文件的位置，例如
	 * 类路径资源 "classpath:hibernate.cfg.xml"。
	 * <p>注意：当所有必需的属性和映射资源
	 * 都通过此 bean 在本地指定时，可以省略此项。
	 * @see Configuration#configure(java.net.URL)
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocations = new Resource[] {configLocation};
	}

	/**
	 * 设置多个 Hibernate XML 配置文件的位置，例如
	 * 类路径资源 "classpath:hibernate.cfg.xml,classpath:extension.cfg.xml"。
	 * <p>注意：当所有必需的属性和映射资源
	 * 都通过此 bean 在本地指定时，可以省略此项。
	 * @see Configuration#configure(java.net.URL)
	 */
	public void setConfigLocations(Resource... configLocations) {
		this.configLocations = configLocations;
	}

	/**
	 * 设置要在类路径中查找的 Hibernate 映射资源，
	 * 例如 "example.hbm.xml" 或 "mypackage/example.hbm.xml"。
	 * 类似于 Hibernate XML 配置文件中的映射条目。
	 * 是更通用的 setMappingLocations 方法的替代方案。
	 * <p>可用于补充 Hibernate XML 配置文件中的映射，
	 * 或在本地指定所有映射。
	 * @see #setMappingLocations
	 * @see Configuration#addResource
	 */
	public void setMappingResources(String... mappingResources) {
		this.mappingResources = mappingResources;
	}

	/**
	 * 设置 Hibernate 映射文件的位置，例如作为类路径
	 * 资源 "classpath:example.hbm.xml"。通过 Spring 的资源抽象
	 * 支持任意资源位置，例如在应用程序上下文中运行时使用
	 * 相对路径 "WEB-INF/mappings/example.hbm.xml"。
	 * <p>可用于补充 Hibernate XML 配置文件中的映射，
	 * 或在本地指定所有映射。
	 * @see Configuration#addInputStream
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * 设置可缓存的 Hibernate 映射文件的位置，例如作为 Web 应用
	 * 资源 "/WEB-INF/mapping/example.hbm.xml"。通过 Spring 的资源抽象
	 * 支持任意资源位置，只要该资源能够在
	 * 文件系统中解析。
	 * <p>可用于补充 Hibernate XML 配置文件中的映射，
	 * 或在本地指定所有映射。
	 * @see Configuration#addCacheableFile(File)
	 */
	public void setCacheableMappingLocations(Resource... cacheableMappingLocations) {
		this.cacheableMappingLocations = cacheableMappingLocations;
	}

	/**
	 * 设置包含 Hibernate 映射资源的 jar 文件位置，
	 * 例如 "WEB-INF/lib/example.hbm.jar"。
	 * <p>可用于补充 Hibernate XML 配置文件中的映射，
	 * 或在本地指定所有映射。
	 * @see Configuration#addJar(File)
	 */
	public void setMappingJarLocations(Resource... mappingJarLocations) {
		this.mappingJarLocations = mappingJarLocations;
	}

	/**
	 * 设置包含 Hibernate 映射资源的目录位置，
	 * 例如 "WEB-INF/mappings"。
	 * <p>可用于补充 Hibernate XML 配置文件中的映射，
	 * 或在本地指定所有映射。
	 * @see Configuration#addDirectory(File)
	 */
	public void setMappingDirectoryLocations(Resource... mappingDirectoryLocations) {
		this.mappingDirectoryLocations = mappingDirectoryLocations;
	}

	/**
	 * 设置 Hibernate 实体拦截器，允许在写入和读取数据库
	 * 之前检查和更改属性值。
	 * 将应用于此工厂创建的任何新 Session。
	 * @see Configuration#setInterceptor
	 */
	public void setEntityInterceptor(Interceptor entityInterceptor) {
		this.entityInterceptor = entityInterceptor;
	}

	/**
	 * 为 SessionFactory 设置 Hibernate 5 {@link ImplicitNamingStrategy}。
	 * @see Configuration#setImplicitNamingStrategy
	 */
	public void setImplicitNamingStrategy(ImplicitNamingStrategy implicitNamingStrategy) {
		this.implicitNamingStrategy = implicitNamingStrategy;
	}

	/**
	 * 为 SessionFactory 设置 Hibernate 5 {@link PhysicalNamingStrategy}。
	 * @see Configuration#setPhysicalNamingStrategy
	 */
	public void setPhysicalNamingStrategy(PhysicalNamingStrategy physicalNamingStrategy) {
		this.physicalNamingStrategy = physicalNamingStrategy;
	}

	/**
	 * 设置要与 Hibernate 一起使用的 Spring
	 * {@link org.springframework.transaction.jta.JtaTransactionManager}
	 * 或 JTA {@link javax.transaction.TransactionManager}（如果有）。
	 * 会隐式设置 {@code JtaPlatform}。
	 * @see LocalSessionFactoryBuilder#setJtaTransactionManager
	 */
	public void setJtaTransactionManager(Object jtaTransactionManager) {
		this.jtaTransactionManager = jtaTransactionManager;
	}

	/**
	 * 设置 SessionFactory 要使用的 Hibernate {@link RegionFactory}。
	 * 允许使用 Spring 管理的 {@code RegionFactory} 实例。
	 * <p>注意：如果设置了此项，Hibernate 设置中就不应再定义
	 * 缓存提供器，以避免无意义的重复配置。
	 * @since 5.1
	 * @see LocalSessionFactoryBuilder#setCacheRegionFactory
	 */
	public void setCacheRegionFactory(RegionFactory cacheRegionFactory) {
		this.cacheRegionFactory = cacheRegionFactory;
	}

	/**
	 * 设置要传递给 SessionFactory 的 {@link MultiTenantConnectionProvider}。
	 * @since 4.3
	 * @see LocalSessionFactoryBuilder#setMultiTenantConnectionProvider
	 */
	public void setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
		this.multiTenantConnectionProvider = multiTenantConnectionProvider;
	}

	/**
	 * 设置要传递给 SessionFactory 的 {@link CurrentTenantIdentifierResolver}。
	 * @see LocalSessionFactoryBuilder#setCurrentTenantIdentifierResolver
	 */
	public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
		this.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
	}

	/**
	 * 设置 Hibernate 属性，例如 "hibernate.dialect"。
	 * <p>注意：使用 Spring 驱动的事务时，不要在此指定事务提供器。
	 * 另外建议省略连接提供器设置，改用 Spring 设置的
	 * DataSource。
	 * @see #setDataSource
	 */
	public void setHibernateProperties(Properties hibernateProperties) {
		this.hibernateProperties = hibernateProperties;
	}

	/**
	 * 返回 Hibernate 属性（如果有）。主要用于通过指定各个键的
	 * 属性路径进行配置。
	 */
	public Properties getHibernateProperties() {
		if (this.hibernateProperties == null) {
			this.hibernateProperties = new Properties();
		}
		return this.hibernateProperties;
	}

	/**
	 * 为基于 Spring 的实体类扫描指定自定义类型过滤器。
	 * <p>默认会在所有指定的包中搜索使用
	 * {@code @javax.persistence.Entity}、{@code @javax.persistence.Embeddable}
	 * 或 {@code @javax.persistence.MappedSuperclass} 注解的类。
	 * @see #setPackagesToScan
	 */
	public void setEntityTypeFilters(TypeFilter... entityTypeFilters) {
		this.entityTypeFilters = entityTypeFilters;
	}

	/**
	 * 指定要注册到此 Hibernate SessionFactory 的注解实体类。
	 * @see Configuration#addAnnotatedClass(Class)
	 */
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	/**
	 * 指定注解包的名称，将读取其包级
	 * 注解元数据。
	 * @see Configuration#addPackage(String)
	 */
	public void setAnnotatedPackages(String... annotatedPackages) {
		this.annotatedPackages = annotatedPackages;
	}

	/**
	 * 指定要在类路径中搜索以自动检测实体类的包。
	 * 这类似于 Spring 的组件扫描特性
	 * （{@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}）。
	 */
	public void setPackagesToScan(String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * 指定用于后台引导的异步执行器，
	 * 例如 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}。
	 * <p>{@code SessionFactory} 初始化随后将切换到后台
	 * 引导模式，立即返回 {@code SessionFactory} 代理用于
	 * 注入目的，而不是等待 Hibernate 的引导完成。
	 * 但请注意，对 {@code SessionFactory} 方法的第一次实际调用
	 * 届时会阻塞，直到 Hibernate 引导完成（如果那时尚未就绪）。
	 * 为获得最大收益，请确保避免在相关 bean 的初始化方法中
	 * 提前调用 {@code SessionFactory}，即使只是为了元数据内省。
	 * @since 4.3
	 * @see LocalSessionFactoryBuilder#buildSessionFactory(AsyncTaskExecutor)
	 */
	public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * 指定要应用的一个或多个 Hibernate {@link Integrator} 实现。
	 * <p>仅适用于内部构建的 {@link MetadataSources}
	 * 实例。{@link #setMetadataSources} 实际上会覆盖此类设置，
	 * 改为将 Integrator 应用于外部构建的 {@link MetadataSources}。
	 * @since 5.1
	 * @see #setMetadataSources
	 * @see BootstrapServiceRegistryBuilder#applyIntegrator
	 */
	public void setHibernateIntegrators(Integrator... hibernateIntegrators) {
		this.hibernateIntegrators = hibernateIntegrators;
	}

	/**
	 * 指定要使用的 Hibernate {@link MetadataSources} 服务（例如复用
	 * 现有的服务），它也可以填充自定义的 Hibernate 引导
	 * {@link org.hibernate.service.ServiceRegistry}。
	 * @since 4.3
	 * @see MetadataSources#MetadataSources(ServiceRegistry)
	 * @see BootstrapServiceRegistryBuilder#build()
	 */
	public void setMetadataSources(MetadataSources metadataSources) {
		this.metadataSourcesAccessed = true;
		this.metadataSources = metadataSources;
	}

	/**
	 * 确定要使用的 Hibernate {@link MetadataSources}。
	 * <p>也可以在外部调用，以初始化并预填充一个 {@link MetadataSources}
	 * 实例，随后将其用于构建 {@link SessionFactory}。
	 * @return 要使用的 MetadataSources（永不为 {@code null}）
	 * @since 4.3
	 * @see LocalSessionFactoryBuilder#LocalSessionFactoryBuilder(DataSource, ResourceLoader, MetadataSources)
	 */
	public MetadataSources getMetadataSources() {
		this.metadataSourcesAccessed = true;
		if (this.metadataSources == null) {
			BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
			if (this.resourcePatternResolver != null) {
				builder = builder.applyClassLoader(this.resourcePatternResolver.getClassLoader());
			}
			if (this.hibernateIntegrators != null) {
				for (Integrator integrator : this.hibernateIntegrators) {
					builder = builder.applyIntegrator(integrator);
				}
			}
			this.metadataSources = new MetadataSources(builder.build());
		}
		return this.metadataSources;
	}

	/**
	 * 指定用于 Hibernate 元数据的 Spring {@link ResourceLoader}。
	 * @param resourceLoader 要使用的 ResourceLoader（永不为 {@code null}）
	 */
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
	}

	/**
	 * 确定用于 Hibernate 元数据的 Spring {@link ResourceLoader}。
	 * @return 要使用的 ResourceLoader（永不为 {@code null}）
	 * @since 4.3
	 */
	public ResourceLoader getResourceLoader() {
		if (this.resourcePatternResolver == null) {
			this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
		}
		return this.resourcePatternResolver;
	}

	/**
	 * 接受所属的 {@link BeanFactory}，并在可能的情况下为它注册相应的
	 * Hibernate {@link org.hibernate.resource.beans.container.spi.BeanContainer}
	 * 集成。这要求类路径上有 Spring {@link ConfigurableListableBeanFactory}
	 * 和 Hibernate 5.3 或更高版本。
	 * @since 5.1
	 * @see SpringBeanContainer
	 * @see LocalSessionFactoryBuilder#setBeanContainer
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory &&
				ClassUtils.isPresent("org.hibernate.resource.beans.container.spi.BeanContainer",
						getClass().getClassLoader())) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
	}


	@Override
	public void afterPropertiesSet() throws IOException {
		if (this.metadataSources != null && !this.metadataSourcesAccessed) {
			// Repeated initialization with no user-customized MetadataSources -> clear it.
			this.metadataSources = null;
		}

		LocalSessionFactoryBuilder sfb = new LocalSessionFactoryBuilder(
				this.dataSource, getResourceLoader(), getMetadataSources());

		if (this.configLocations != null) {
			for (Resource resource : this.configLocations) {
				// Load Hibernate configuration from given location.
				sfb.configure(resource.getURL());
			}
		}

		if (this.mappingResources != null) {
			// Register given Hibernate mapping definitions, contained in resource files.
			for (String mapping : this.mappingResources) {
				Resource mr = new ClassPathResource(mapping.trim(), getResourceLoader().getClassLoader());
				sfb.addInputStream(mr.getInputStream());
			}
		}

		if (this.mappingLocations != null) {
			// Register given Hibernate mapping definitions, contained in resource files.
			for (Resource resource : this.mappingLocations) {
				sfb.addInputStream(resource.getInputStream());
			}
		}

		if (this.cacheableMappingLocations != null) {
			// Register given cacheable Hibernate mapping definitions, read from the file system.
			for (Resource resource : this.cacheableMappingLocations) {
				sfb.addCacheableFile(resource.getFile());
			}
		}

		if (this.mappingJarLocations != null) {
			// Register given Hibernate mapping definitions, contained in jar files.
			for (Resource resource : this.mappingJarLocations) {
				sfb.addJar(resource.getFile());
			}
		}

		if (this.mappingDirectoryLocations != null) {
			// Register all Hibernate mapping definitions in the given directories.
			for (Resource resource : this.mappingDirectoryLocations) {
				File file = resource.getFile();
				if (!file.isDirectory()) {
					throw new IllegalArgumentException(
							"Mapping directory location [" + resource + "] does not denote a directory");
				}
				sfb.addDirectory(file);
			}
		}

		if (this.entityInterceptor != null) {
			sfb.setInterceptor(this.entityInterceptor);
		}

		if (this.implicitNamingStrategy != null) {
			sfb.setImplicitNamingStrategy(this.implicitNamingStrategy);
		}

		if (this.physicalNamingStrategy != null) {
			sfb.setPhysicalNamingStrategy(this.physicalNamingStrategy);
		}

		if (this.jtaTransactionManager != null) {
			sfb.setJtaTransactionManager(this.jtaTransactionManager);
		}

		if (this.beanFactory != null) {
			sfb.setBeanContainer(this.beanFactory);
		}

		if (this.cacheRegionFactory != null) {
			sfb.setCacheRegionFactory(this.cacheRegionFactory);
		}

		if (this.multiTenantConnectionProvider != null) {
			sfb.setMultiTenantConnectionProvider(this.multiTenantConnectionProvider);
		}

		if (this.currentTenantIdentifierResolver != null) {
			sfb.setCurrentTenantIdentifierResolver(this.currentTenantIdentifierResolver);
		}

		if (this.hibernateProperties != null) {
			sfb.addProperties(this.hibernateProperties);
		}

		if (this.entityTypeFilters != null) {
			sfb.setEntityTypeFilters(this.entityTypeFilters);
		}

		if (this.annotatedClasses != null) {
			sfb.addAnnotatedClasses(this.annotatedClasses);
		}

		if (this.annotatedPackages != null) {
			sfb.addPackages(this.annotatedPackages);
		}

		if (this.packagesToScan != null) {
			sfb.scanPackages(this.packagesToScan);
		}

		// Build SessionFactory instance.
		this.configuration = sfb;
		this.sessionFactory = buildSessionFactory(sfb);
	}

	/**
	 * 子类可以重写此方法以执行 SessionFactory 实例的自定义初始化，
	 * 通过此 LocalSessionFactoryBean 准备好的给定 Configuration
	 * 对象来创建它。
	 * <p>默认实现调用 LocalSessionFactoryBuilder 的 buildSessionFactory。
	 * 自定义实现可以以特定方式准备该实例（例如应用
	 * 自定义 ServiceRegistry）或使用自定义的 SessionFactoryImpl 子类。
	 * @param sfb 由本 LocalSessionFactoryBean 准备的 LocalSessionFactoryBuilder
	 * @return SessionFactory 实例
	 * @see LocalSessionFactoryBuilder#buildSessionFactory
	 */
	protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
		return (this.bootstrapExecutor != null ? sfb.buildSessionFactory(this.bootstrapExecutor) :
				sfb.buildSessionFactory());
	}

	/**
	 * 返回用于构建 SessionFactory 的 Hibernate Configuration 对象。
	 * 允许访问其中存储的配置元数据（很少需要）。
	 * @throws IllegalStateException 如果 Configuration 对象尚未初始化
	 */
	public final Configuration getConfiguration() {
		if (this.configuration == null) {
			throw new IllegalStateException("Configuration not initialized yet");
		}
		return this.configuration;
	}


	@Override
	@Nullable
	public SessionFactory getObject() {
		return this.sessionFactory;
	}

	@Override
	public Class<?> getObjectType() {
		return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.sessionFactory != null) {
			this.sessionFactory.close();
		}
	}

}
