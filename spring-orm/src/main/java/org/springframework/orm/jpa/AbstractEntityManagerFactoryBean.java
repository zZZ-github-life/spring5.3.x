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

package org.springframework.orm.jpa;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * 抽象的 {@link org.springframework.beans.factory.FactoryBean}，
 * 在 Spring 应用上下文中创建本地 JPA
 * {@link javax.persistence.EntityManagerFactory} 实例。
 *
 * <p>封装了不同 JPA 引导契约（独立以及容器）之间的公共功能。
 *
 * <p>支持标准 JPA 配置约定以及 Spring 可定制的
 * {@link JpaVendorAdapter} 机制，并控制 EntityManagerFactory 的生命周期。
 *
 * <p>此类还实现了
 * {@link org.springframework.dao.support.PersistenceExceptionTranslator} 接口，
 * 会被 Spring 的
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor}
 * 自动检测，用于基于 AOP 将原生异常转换为 Spring 的 DataAccessException。
 * 因此，例如 LocalEntityManagerFactoryBean 的存在会自动启用
 * PersistenceExceptionTranslationPostProcessor 来转换 JPA 异常。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 2.0
 * @see LocalEntityManagerFactoryBean
 * @see LocalContainerEntityManagerFactoryBean
 */
@SuppressWarnings("serial")
public abstract class AbstractEntityManagerFactoryBean implements
		FactoryBean<EntityManagerFactory>, BeanClassLoaderAware, BeanFactoryAware, BeanNameAware,
		InitializingBean, DisposableBean, EntityManagerFactoryInfo, PersistenceExceptionTranslator, Serializable {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private PersistenceProvider persistenceProvider;

	@Nullable
	private String persistenceUnitName;

	private final Map<String, Object> jpaPropertyMap = new HashMap<>();

	@Nullable
	private Class<? extends EntityManagerFactory> entityManagerFactoryInterface;

	@Nullable
	private Class<? extends EntityManager> entityManagerInterface;

	@Nullable
	private JpaDialect jpaDialect;

	@Nullable
	private JpaVendorAdapter jpaVendorAdapter;

	@Nullable
	private Consumer<EntityManager> entityManagerInitializer;

	@Nullable
	private AsyncTaskExecutor bootstrapExecutor;

	private ClassLoader beanClassLoader = getClass().getClassLoader();

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private String beanName;

	/** PersistenceProvider 返回的原始 EntityManagerFactory。 */
	@Nullable
	private EntityManagerFactory nativeEntityManagerFactory;

	/** 用于延迟初始化原始目标 EntityManagerFactory 的 Future。 */
	@Nullable
	private Future<EntityManagerFactory> nativeEntityManagerFactoryFuture;

	/** 暴露给客户端的 EntityManagerFactory 代理。 */
	@Nullable
	private EntityManagerFactory entityManagerFactory;


	/**
	 * 设置用于创建 EntityManagerFactory 的 PersistenceProvider 实现类。
	 * 如果未指定，则将从 JpaVendorAdapter（如果有）中获取持久化提供者，
	 * 或通过扫描获取（尽最大可能）。
	 * @see JpaVendorAdapter#getPersistenceProvider()
	 * @see javax.persistence.spi.PersistenceProvider
	 * @see javax.persistence.Persistence
	 */
	public void setPersistenceProviderClass(Class<? extends PersistenceProvider> persistenceProviderClass) {
		this.persistenceProvider = BeanUtils.instantiateClass(persistenceProviderClass);
	}

	/**
	 * 设置用于创建 EntityManagerFactory 的 PersistenceProvider 实例。
	 * 如果未指定，则将从 JpaVendorAdapter（如果有）中获取持久化提供者，
	 * 或由持久化单元部署描述符确定（尽最大可能）。
	 * @see JpaVendorAdapter#getPersistenceProvider()
	 * @see javax.persistence.spi.PersistenceProvider
	 * @see javax.persistence.Persistence
	 */
	public void setPersistenceProvider(@Nullable PersistenceProvider persistenceProvider) {
		this.persistenceProvider = persistenceProvider;
	}

	@Override
	@Nullable
	public PersistenceProvider getPersistenceProvider() {
		return this.persistenceProvider;
	}

	/**
	 * 指定 EntityManagerFactory 配置的名称。
	 * <p>默认不指定，表示使用默认的 EntityManagerFactory 配置。
	 * 如果发现存在歧义的 EntityManager 配置，持久化提供者将抛出异常。
	 * @see javax.persistence.Persistence#createEntityManagerFactory(String)
	 */
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	@Nullable
	public String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}

	/**
	 * 指定要传入 {@code Persistence.createEntityManagerFactory} 的 JPA 属性（如果有）。
	 * <p>可以通过 String 类型的 "value"（由 PropertiesEditor 解析）
	 * 或 XML bean 定义中的 "props" 元素来填充。
	 * @see javax.persistence.Persistence#createEntityManagerFactory(String, Map)
	 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map)
	 */
	public void setJpaProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.jpaPropertyMap);
	}

	/**
	 * 以 Map 形式指定要传入 {@code Persistence.createEntityManagerFactory} 的
	 * JPA 属性（如果有）。
	 * <p>可以通过 XML bean 定义中的 "map" 或 "props" 元素来填充。
	 * @see javax.persistence.Persistence#createEntityManagerFactory(String, Map)
	 * @see javax.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory(PersistenceUnitInfo, Map)
	 */
	public void setJpaPropertyMap(@Nullable Map<String, ?> jpaProperties) {
		if (jpaProperties != null) {
			this.jpaPropertyMap.putAll(jpaProperties);
		}
	}

	/**
	 * 允许以 {@code Map} 方式访问要传递给持久化提供者的 JPA 属性，
	 * 并可以选择添加或覆盖特定条目。
	 * <p>适用于直接指定条目，例如通过
	 * {@code jpaPropertyMap[myKey]}。
	 */
	public Map<String, Object> getJpaPropertyMap() {
		return this.jpaPropertyMap;
	}

	/**
	 * 指定此 EntityManagerFactory 代理应实现的（可能为供应商特定的）
	 * EntityManagerFactory 接口。
	 * <p>默认值将取自特定的 JpaVendorAdapter（如果有），
	 * 否则设置为标准的 {@code javax.persistence.EntityManagerFactory} 接口。
	 * @see JpaVendorAdapter#getEntityManagerFactoryInterface()
	 */
	public void setEntityManagerFactoryInterface(Class<? extends EntityManagerFactory> emfInterface) {
		this.entityManagerFactoryInterface = emfInterface;
	}

	/**
	 * 指定此工厂的 EntityManager 应实现的（可能为供应商特定的）
	 * EntityManager 接口。
	 * <p>默认值将取自特定的 JpaVendorAdapter（如果有），
	 * 否则设置为标准的 {@code javax.persistence.EntityManager} 接口。
	 * @see JpaVendorAdapter#getEntityManagerInterface()
	 * @see EntityManagerFactoryInfo#getEntityManagerInterface()
	 */
	public void setEntityManagerInterface(@Nullable Class<? extends EntityManager> emInterface) {
		this.entityManagerInterface = emInterface;
	}

	@Override
	@Nullable
	public Class<? extends EntityManager> getEntityManagerInterface() {
		return this.entityManagerInterface;
	}

	/**
	 * 指定要与此 EntityManagerFactory 关联的供应商特定 JpaDialect 实现。
	 * 它将通过 EntityManagerFactoryInfo 接口暴露，
	 * 以便打算使用 JpaDialect 功能的访问器将其作为默认方言获取。
	 * @see EntityManagerFactoryInfo#getJpaDialect()
	 */
	public void setJpaDialect(@Nullable JpaDialect jpaDialect) {
		this.jpaDialect = jpaDialect;
	}

	@Override
	@Nullable
	public JpaDialect getJpaDialect() {
		return this.jpaDialect;
	}

	/**
	 * 为所需的 JPA 提供者指定 JpaVendorAdapter 实现（如果有）。
	 * 这将为给定的提供者初始化适当的默认值，
	 * 例如持久化提供者类和 JpaDialect，除非在此 FactoryBean 中被局部覆盖。
	 */
	public void setJpaVendorAdapter(@Nullable JpaVendorAdapter jpaVendorAdapter) {
		this.jpaVendorAdapter = jpaVendorAdapter;
	}

	/**
	 * 返回此 EntityManagerFactory 的 JpaVendorAdapter 实现，
	 * 如果未知则返回 {@code null}。
	 */
	@Nullable
	public JpaVendorAdapter getJpaVendorAdapter() {
		return this.jpaVendorAdapter;
	}

	/**
	 * 指定一个回调，用于自定义所暴露的 {@code EntityManagerFactory}
	 * 创建的每个 {@code EntityManager}。
	 * <p>这是 {@code JpaVendorAdapter} 级 {@code postProcessEntityManager}
	 * 实现的替代方式，可针对应用程序用途进行便捷定制，
	 * 例如设置 Hibernate 过滤器。
	 * @since 5.3
	 * @see JpaVendorAdapter#postProcessEntityManager
	 * @see JpaTransactionManager#setEntityManagerInitializer
	 */
	public void setEntityManagerInitializer(Consumer<EntityManager> entityManagerInitializer) {
		this.entityManagerInitializer = entityManagerInitializer;
	}

	/**
	 * 指定用于后台引导的异步执行器，
	 * 例如 {@link org.springframework.core.task.SimpleAsyncTaskExecutor}。
	 * <p>随后 {@code EntityManagerFactory} 的初始化将切换到后台引导模式，
	 * 立即返回一个 {@code EntityManagerFactory} 代理用于注入，
	 * 而无需等待 JPA 提供者的引导完成。
	 * 但请注意，对 {@code EntityManagerFactory} 方法的第一次实际调用
	 * 将阻塞，直到 JPA 提供者的引导完成（如果届时尚未就绪）。
	 * 为获得最大收益，请确保避免在相关 bean 的初始化方法中
	 * 过早调用 {@code EntityManagerFactory}，即使只是为了元数据内省。
	 * @since 4.3
	 */
	public void setBootstrapExecutor(@Nullable AsyncTaskExecutor bootstrapExecutor) {
		this.bootstrapExecutor = bootstrapExecutor;
	}

	/**
	 * 返回用于后台引导的异步执行器（如果有）。
	 * @since 4.3
	 */
	@Nullable
	public AsyncTaskExecutor getBootstrapExecutor() {
		return this.bootstrapExecutor;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public void afterPropertiesSet() throws PersistenceException {
		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null) {
			if (this.persistenceProvider == null) {
				this.persistenceProvider = jpaVendorAdapter.getPersistenceProvider();
			}
			PersistenceUnitInfo pui = getPersistenceUnitInfo();
			Map<String, ?> vendorPropertyMap = (pui != null ? jpaVendorAdapter.getJpaPropertyMap(pui) :
					jpaVendorAdapter.getJpaPropertyMap());
			if (!CollectionUtils.isEmpty(vendorPropertyMap)) {
				vendorPropertyMap.forEach((key, value) -> {
					if (!this.jpaPropertyMap.containsKey(key)) {
						this.jpaPropertyMap.put(key, value);
					}
				});
			}
			if (this.entityManagerFactoryInterface == null) {
				this.entityManagerFactoryInterface = jpaVendorAdapter.getEntityManagerFactoryInterface();
				if (!ClassUtils.isVisible(this.entityManagerFactoryInterface, this.beanClassLoader)) {
					this.entityManagerFactoryInterface = EntityManagerFactory.class;
				}
			}
			if (this.entityManagerInterface == null) {
				this.entityManagerInterface = jpaVendorAdapter.getEntityManagerInterface();
				if (!ClassUtils.isVisible(this.entityManagerInterface, this.beanClassLoader)) {
					this.entityManagerInterface = EntityManager.class;
				}
			}
			if (this.jpaDialect == null) {
				this.jpaDialect = jpaVendorAdapter.getJpaDialect();
			}
		}

		AsyncTaskExecutor bootstrapExecutor = getBootstrapExecutor();
		if (bootstrapExecutor != null) {
			this.nativeEntityManagerFactoryFuture = bootstrapExecutor.submit(this::buildNativeEntityManagerFactory);
		}
		else {
			this.nativeEntityManagerFactory = buildNativeEntityManagerFactory();
		}

		// Wrap the EntityManagerFactory in a factory implementing all its interfaces.
		// This allows interception of createEntityManager methods to return an
		// application-managed EntityManager proxy that automatically joins
		// existing transactions.
		this.entityManagerFactory = createEntityManagerFactoryProxy(this.nativeEntityManagerFactory);
	}

	private EntityManagerFactory buildNativeEntityManagerFactory() {
		EntityManagerFactory emf;
		try {
			emf = createNativeEntityManagerFactory();
		}
		catch (PersistenceException ex) {
			if (ex.getClass() == PersistenceException.class) {
				// Plain PersistenceException wrapper for underlying exception?
				// Make sure the nested exception message is properly exposed,
				// along the lines of Spring's NestedRuntimeException.getMessage()
				Throwable cause = ex.getCause();
				if (cause != null) {
					String message = ex.getMessage();
					String causeString = cause.toString();
					if (!message.endsWith(causeString)) {
						ex = new PersistenceException(message + "; nested exception is " + causeString, cause);
					}
				}
			}
			if (logger.isErrorEnabled()) {
				logger.error("Failed to initialize JPA EntityManagerFactory: " + ex.getMessage());
			}
			throw ex;
		}

		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null) {
			jpaVendorAdapter.postProcessEntityManagerFactory(emf);
		}

		if (logger.isInfoEnabled()) {
			logger.info("Initialized JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
		}
		return emf;
	}

	/**
	 * 为给定的 {@link EntityManagerFactory} 创建代理。这样做是为了能够
	 * 为应用程序管理的 {@link EntityManager} 返回一个事务感知的代理。
	 * @param emf 持久化提供者返回的 EntityManagerFactory（如果已初始化）
	 * @return EntityManagerFactory 代理
	 */
	protected EntityManagerFactory createEntityManagerFactoryProxy(@Nullable EntityManagerFactory emf) {
		Set<Class<?>> ifcs = new LinkedHashSet<>();
		Class<?> entityManagerFactoryInterface = this.entityManagerFactoryInterface;
		if (entityManagerFactoryInterface != null) {
			ifcs.add(entityManagerFactoryInterface);
		}
		else if (emf != null) {
			ifcs.addAll(ClassUtils.getAllInterfacesForClassAsSet(emf.getClass(), this.beanClassLoader));
		}
		else {
			ifcs.add(EntityManagerFactory.class);
		}
		ifcs.add(EntityManagerFactoryInfo.class);

		try {
			return (EntityManagerFactory) Proxy.newProxyInstance(this.beanClassLoader,
					ClassUtils.toClassArray(ifcs), new ManagedEntityManagerFactoryInvocationHandler(this));
		}
		catch (IllegalArgumentException ex) {
			if (entityManagerFactoryInterface != null) {
				throw new IllegalStateException("EntityManagerFactory interface [" + entityManagerFactoryInterface +
						"] seems to conflict with Spring's EntityManagerFactoryInfo mixin - consider resetting the "+
						"'entityManagerFactoryInterface' property to plain [javax.persistence.EntityManagerFactory]", ex);
			}
			else {
				throw new IllegalStateException("Conflicting EntityManagerFactory interfaces - " +
						"consider specifying the 'jpaVendorAdapter' or 'entityManagerFactoryInterface' property " +
						"to select a specific EntityManagerFactory interface to proceed with", ex);
			}
		}
	}

	/**
	 * 委托来自代理的传入调用，
	 * 相应分派给 EntityManagerFactoryInfo 或原生 EntityManagerFactory。
	 */
	Object invokeProxyMethod(Method method, @Nullable Object[] args) throws Throwable {
		if (method.getDeclaringClass().isAssignableFrom(EntityManagerFactoryInfo.class)) {
			return method.invoke(this, args);
		}
		else if (method.getName().equals("createEntityManager") && args != null && args.length > 0 &&
				args[0] == SynchronizationType.SYNCHRONIZED) {
			// JPA 2.1's createEntityManager(SynchronizationType, Map)
			// Redirect to plain createEntityManager and add synchronization semantics through Spring proxy
			EntityManager rawEntityManager = (args.length > 1 ?
					getNativeEntityManagerFactory().createEntityManager((Map<?, ?>) args[1]) :
					getNativeEntityManagerFactory().createEntityManager());
			postProcessEntityManager(rawEntityManager);
			return ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, true);
		}

		// Look for Query arguments, primarily JPA 2.1's addNamedQuery(String, Query)
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg instanceof Query && Proxy.isProxyClass(arg.getClass())) {
					// Assumably a Spring-generated proxy from SharedEntityManagerCreator:
					// since we're passing it back to the native EntityManagerFactory,
					// let's unwrap it to the original Query object from the provider.
					try {
						args[i] = ((Query) arg).unwrap(null);
					}
					catch (RuntimeException ex) {
						// Ignore - simply proceed with given Query object then
					}
				}
			}
		}

		// Standard delegation to the native factory, just post-processing EntityManager return values
		Object retVal = method.invoke(getNativeEntityManagerFactory(), args);
		if (retVal instanceof EntityManager) {
			// Any other createEntityManager variant - expecting non-synchronized semantics
			EntityManager rawEntityManager = (EntityManager) retVal;
			postProcessEntityManager(rawEntityManager);
			retVal = ExtendedEntityManagerCreator.createApplicationManagedEntityManager(rawEntityManager, this, false);
		}
		return retVal;
	}

	/**
	 * 子类必须实现此方法，以创建将由 {@code getObject()} 方法返回的
	 * EntityManagerFactory。
	 * @return 此 FactoryBean 返回的 EntityManagerFactory 实例
	 * @throws PersistenceException 如果无法创建 EntityManager
	 */
	protected abstract EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException;


	/**
	 * PersistenceExceptionTranslator 接口的实现，
	 * 由 Spring 的 PersistenceExceptionTranslationPostProcessor 自动检测。
	 * <p>如果可能，使用方言的转换；否则回退到
	 * 标准的 JPA 异常转换。
	 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
	 * @see JpaDialect#translateExceptionIfPossible
	 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
	 */
	@Override
	@Nullable
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		JpaDialect jpaDialect = getJpaDialect();
		return (jpaDialect != null ? jpaDialect.translateExceptionIfPossible(ex) :
				EntityManagerFactoryUtils.convertJpaAccessExceptionIfPossible(ex));
	}

	@Override
	public EntityManagerFactory getNativeEntityManagerFactory() {
		if (this.nativeEntityManagerFactory != null) {
			return this.nativeEntityManagerFactory;
		}
		else {
			Assert.state(this.nativeEntityManagerFactoryFuture != null, "No native EntityManagerFactory available");
			try {
				return this.nativeEntityManagerFactoryFuture.get();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted during initialization of native EntityManagerFactory", ex);
			}
			catch (ExecutionException ex) {
				Throwable cause = ex.getCause();
				if (cause instanceof PersistenceException) {
					// Rethrow a provider configuration exception (possibly with a nested cause) directly
					throw (PersistenceException) cause;
				}
				throw new IllegalStateException("Failed to asynchronously initialize native EntityManagerFactory: " +
						ex.getMessage(), cause);
			}
		}
	}

	@Override
	public EntityManager createNativeEntityManager(@Nullable Map<?, ?> properties) {
		EntityManager rawEntityManager = (!CollectionUtils.isEmpty(properties) ?
				getNativeEntityManagerFactory().createEntityManager(properties) :
				getNativeEntityManagerFactory().createEntityManager());
		postProcessEntityManager(rawEntityManager);
		return rawEntityManager;
	}

	/**
	 * 可选回调，用于在激活使用之前对原生 EntityManager 进行后处理。
	 * <p>默认实现委托给
	 * {@link JpaVendorAdapter#postProcessEntityManager}（如果可用）。
	 * @param rawEntityManager 要后处理的 EntityManager
	 * @since 5.3
	 * @see #createNativeEntityManager
	 * @see JpaVendorAdapter#postProcessEntityManager
	 */
	protected void postProcessEntityManager(EntityManager rawEntityManager) {
		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
		if (jpaVendorAdapter != null) {
			jpaVendorAdapter.postProcessEntityManager(rawEntityManager);
		}
		Consumer<EntityManager> customizer = this.entityManagerInitializer;
		if (customizer != null) {
			customizer.accept(rawEntityManager);
		}
	}

	@Override
	@Nullable
	public PersistenceUnitInfo getPersistenceUnitInfo() {
		return null;
	}

	@Override
	@Nullable
	public DataSource getDataSource() {
		return null;
	}


	/**
	 * 返回单例 EntityManagerFactory。
	 */
	@Override
	@Nullable
	public EntityManagerFactory getObject() {
		return this.entityManagerFactory;
	}

	@Override
	public Class<? extends EntityManagerFactory> getObjectType() {
		return (this.entityManagerFactory != null ? this.entityManagerFactory.getClass() : EntityManagerFactory.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 在 bean 工厂关闭时关闭 EntityManagerFactory。
	 */
	@Override
	public void destroy() {
		if (this.entityManagerFactory != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Closing JPA EntityManagerFactory for persistence unit '" + getPersistenceUnitName() + "'");
			}
			this.entityManagerFactory.close();
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		throw new NotSerializableException("An EntityManagerFactoryBean itself is not deserializable - " +
				"just a SerializedEntityManagerFactoryBeanReference is");
	}

	protected Object writeReplace() throws ObjectStreamException {
		if (this.beanFactory != null && this.beanName != null) {
			return new SerializedEntityManagerFactoryBeanReference(this.beanFactory, this.beanName);
		}
		else {
			throw new NotSerializableException("EntityManagerFactoryBean does not run within a BeanFactory");
		}
	}


	/**
	 * 对周围 AbstractEntityManagerFactoryBean 的最小 bean 引用。
	 * 在反序列化时解析为实际的 AbstractEntityManagerFactoryBean 实例。
	 */
	@SuppressWarnings("serial")
	private static class SerializedEntityManagerFactoryBeanReference implements Serializable {

		private final BeanFactory beanFactory;

		private final String lookupName;

		public SerializedEntityManagerFactoryBeanReference(BeanFactory beanFactory, String beanName) {
			this.beanFactory = beanFactory;
			this.lookupName = BeanFactory.FACTORY_BEAN_PREFIX + beanName;
		}

		private Object readResolve() {
			return this.beanFactory.getBean(this.lookupName, AbstractEntityManagerFactoryBean.class);
		}
	}


	/**
	 * {@link EntityManagerFactory} 的动态代理调用处理器，
	 * 从 {@code createEntityManager} 方法返回一个代理
	 * {@link EntityManager}（如有必要）。
	 */
	@SuppressWarnings("serial")
	private static class ManagedEntityManagerFactoryInvocationHandler implements InvocationHandler, Serializable {

		private final AbstractEntityManagerFactoryBean entityManagerFactoryBean;

		public ManagedEntityManagerFactoryInvocationHandler(AbstractEntityManagerFactoryBean emfb) {
			this.entityManagerFactoryBean = emfb;
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
				case "unwrap":
					// Handle JPA 2.1 unwrap method - could be a proxy match.
					Class<?> targetClass = (Class<?>) args[0];
					if (targetClass == null) {
						return this.entityManagerFactoryBean.getNativeEntityManagerFactory();
					}
					else if (targetClass.isInstance(proxy)) {
						return proxy;
					}
					break;
			}

			try {
				return this.entityManagerFactoryBean.invokeProxyMethod(method, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
