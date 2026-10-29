/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.Interceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.UnknownAdviceTypeException;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean} 实现，基于 Spring
 * {@link org.springframework.beans.factory.BeanFactory} 中的 Bean 构建 AOP 代理。
 *
 * <p>{@link org.aopalliance.intercept.MethodInterceptor MethodInterceptors} 和
 * {@link org.springframework.aop.Advisor Advisors} 通过当前 Bean 工厂中的一组 Bean 名称来标识，这
 * 些名称由 "interceptorNames" 属性指定。列表中的最后一项可以是目标 Bean 的名称或一个
 * {@link org.springframework.aop.TargetSource}；不过，通常更推荐改用
 * "targetName"/"target"/"targetSource" 属性。
 *
 * <p>可以在工厂级别添加全局拦截器和通知器。指定的内容会在一个拦截器列表中展开，其中如果包含 "xxx*"
 * 形式的条目，则会按给定前缀匹配 Bean 名称 &mdash; 例如，"global*" 会同时匹配 "globalBean1" 和
 * "globalBean2"；而 "*" 会匹配所有已定义的拦截器。匹配到的拦截器会按照其返回的顺序值应用，前提是它
 * 们实现了 {@link org.springframework.core.Ordered} 接口。
 *
 * <p>如果给出了代理接口，则创建 JDK 代理；否则为实际目标类创建 CGLIB 代理。请注意，后者仅在目标类
 * 不含 final 方法时才能工作，因为运行时将创建动态子类。
 *
 * <p>可以将从此工厂获得的代理转换为 {@link Advised}，也可以获取 ProxyFactoryBean 引用并以编程方式
 * 操作它。这对已有的原型引用不起作用，因为它们是相互独立的。不过，对随后从工厂获取的原型实例是有效
 * 的。对拦截的修改会立即对单例（包括已有引用）生效。但是，要更改接口或目标，必须从工厂获取新实例。
 * 这意味着从工厂获得的单例实例并不具有相同的对象标识。不过它们具有相同的拦截器和目标，更改任何引用
 * 都会改变所有对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setInterceptorNames
 * @see #setProxyInterfaces
 * @see org.aopalliance.intercept.MethodInterceptor
 * @see org.springframework.aop.Advisor
 * @see Advised
 */
@SuppressWarnings("serial")
public class ProxyFactoryBean extends ProxyCreatorSupport
		implements FactoryBean<Object>, BeanClassLoaderAware, BeanFactoryAware {

	/**
	 * 拦截器列表中值的此后缀表示要展开全局项。
	 */
	public static final String GLOBAL_SUFFIX = "*";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String[] interceptorNames;

	@Nullable
	private String targetName;

	private boolean autodetectInterfaces = true;

	private boolean singleton = true;

	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	private boolean freezeProxy = false;

	@Nullable
	private transient ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private transient boolean classLoaderConfigured = false;

	@Nullable
	private transient BeanFactory beanFactory;

	/** advisor 链是否已经初始化。 */
	private boolean advisorChainInitialized = false;

	/** 如果这是单例，则为缓存的单例代理实例。 */
	@Nullable
	private Object singletonInstance;


	/**
	 * 设置我们要代理的接口名称。如果未给出接口，则将针对实际类创建 CGLIB 代理。
	 * <p>这实质上等同于 "setInterfaces" 方法，但名称与 TransactionProxyFactoryBean 的
	 * "setProxyInterfaces" 保持一致。
	 * @see #setInterfaces
	 * @see AbstractSingletonProxyFactoryBean#setProxyInterfaces
	 */
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) throws ClassNotFoundException {
		setInterfaces(proxyInterfaces);
	}

	/**
	 * 设置 Advice/Advisor Bean 名称列表。要在 Bean 工厂中使用此工厂 Bean，必须始终设置该属性。
	 * <p>被引用的 Bean 应为 Interceptor、Advisor 或 Advice 类型。列表中的最后一项可以是工厂中任意 Bean
	 * 的名称。如果它既不是 Advice 也不是 Advisor，则会添加一个新的 SingletonTargetSource 将其包装。如
	 * 果设置了 "target" 或 "targetSource" 或 "targetName" 属性，则不能使用这样的目标 Bean，此时
	 * "interceptorNames" 数组必须只包含 Advice/Advisor Bean 名称。
	 * <p><b>注意：在 "interceptorNames" 列表中将目标 Bean 指定为最后一项的做法已废弃，并将在未来的
	 * Spring 版本中移除。</b>请改用 {@link #setTargetName "targetName"} 属性。
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see org.springframework.aop.Advisor
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.target.SingletonTargetSource
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * 设置目标 Bean 的名称。这是在 "interceptorNames" 数组末尾指定目标名称的替代方式。
	 * <p>也可以通过 "target"/"targetSource" 属性分别直接指定目标对象或 TargetSource 对象。
	 * @see #setInterceptorNames(String[])
	 * @see #setTarget(Object)
	 * @see #setTargetSource(org.springframework.aop.TargetSource)
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	/**
	 * 设置未指定接口时是否自动探测代理接口。
	 * <p>默认值为 "true"。如果未指定接口，请关闭此标志，以便为完整目标类创建 CGLIB 代理。
	 * @see #setProxyTargetClass
	 */
	public void setAutodetectInterfaces(boolean autodetectInterfaces) {
		this.autodetectInterfaces = autodetectInterfaces;
	}

	/**
	 * 设置 singleton 属性的值。它决定此工厂应始终返回同一个代理实例（意味着同一个目标），还是返回新的
	 * 原型实例（这意味着如果目标与拦截器来自原型 Bean 定义，它们也可能是新实例）。这样可以在对象图中精
	 * 细控制独立性与唯一性。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * 指定要使用的 AdvisorAdapterRegistry。默认是全局 AdvisorAdapterRegistry。
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	/**
	 * 设置用于生成代理类的 ClassLoader。
	 * <p>默认是 Bean 的 ClassLoader，即包含它的 BeanFactory 用于加载所有 Bean 类的 ClassLoader。可以在
	 * 此为特定代理覆盖此设置。
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		checkInterceptorNames();
	}


	/**
	 * 返回一个代理。当客户端从此工厂 Bean 获取 Bean 时调用。创建要由此工厂返回的 AOP 代理实例。该实例
	 * 对单例会被缓存，而对代理则在每次调用 {@code getObject()} 时创建。
	 * @return a fresh AOP proxy reflecting the current state of this factory
	 */
	@Override
	@Nullable
	public Object getObject() throws BeansException {
		initializeAdvisorChain();
		if (isSingleton()) {
			return getSingletonInstance();
		}
		else {
			if (this.targetName == null) {
				logger.info("Using non-singleton proxies with singleton targets is often undesirable. " +
						"Enable prototype proxies by setting the 'targetName' property.");
			}
			return newPrototypeInstance();
		}
	}

	/**
	 * 返回代理的类型。如果单例实例已创建，将检查该实例；否则回退到代理接口（仅当只有一个接口时）、目标
	 * Bean 类型或 TargetSource 的目标类。
	 * @see org.springframework.aop.TargetSource#getTargetClass
	 */
	@Override
	public Class<?> getObjectType() {
		synchronized (this) {
			if (this.singletonInstance != null) {
				return this.singletonInstance.getClass();
			}
		}
		Class<?>[] ifcs = getProxiedInterfaces();
		if (ifcs.length == 1) {
			return ifcs[0];
		}
		else if (ifcs.length > 1) {
			return createCompositeInterface(ifcs);
		}
		else if (this.targetName != null && this.beanFactory != null) {
			return this.beanFactory.getType(this.targetName);
		}
		else {
			return getTargetClass();
		}
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}


	/**
	 * 为给定接口创建一个组合接口 Class，在一个 Class 中实现这些接口。
	 * <p>默认实现会为给定接口构建一个 JDK 代理类。
	 * @param interfaces the interfaces to merge
	 * @return the merged interface as Class
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.proxyClassLoader);
	}

	/**
	 * 返回此类代理对象的单例实例，如果尚未创建则延迟创建。
	 * @return the shared singleton proxy
	 */
	private synchronized Object getSingletonInstance() {
		if (this.singletonInstance == null) {
			this.targetSource = freshTargetSource();
			if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
				// Rely on AOP infrastructure to tell us what interfaces to proxy.
				Class<?> targetClass = getTargetClass();
				if (targetClass == null) {
					throw new FactoryBeanNotInitializedException("Cannot determine target class for proxy");
				}
				setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
			// Initialize the shared singleton instance.
			super.setFrozen(this.freezeProxy);
			this.singletonInstance = getProxy(createAopProxy());
		}
		return this.singletonInstance;
	}

	/**
	 * 创建此类所创建的代理对象的新原型实例，由独立的 AdvisedSupport 配置支持。
	 * @return a totally independent proxy, whose advice we may manipulate in isolation
	 */
	private synchronized Object newPrototypeInstance() {
		// In the case of a prototype, we need to give the proxy
		// an independent instance of the configuration.
		// In this case, no proxy will have an instance of this object's configuration,
		// but will have an independent copy.
		ProxyCreatorSupport copy = new ProxyCreatorSupport(getAopProxyFactory());

		// The copy needs a fresh advisor chain, and a fresh TargetSource.
		TargetSource targetSource = freshTargetSource();
		copy.copyConfigurationFrom(this, targetSource, freshAdvisorChain());
		if (this.autodetectInterfaces && getProxiedInterfaces().length == 0 && !isProxyTargetClass()) {
			// Rely on AOP infrastructure to tell us what interfaces to proxy.
			Class<?> targetClass = targetSource.getTargetClass();
			if (targetClass != null) {
				copy.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
		}
		copy.setFrozen(this.freezeProxy);

		return getProxy(copy.createAopProxy());
	}

	/**
	 * 返回要暴露的代理对象。
	 * <p>默认实现使用工厂的 Bean ClassLoader 调用 {@code getProxy}。可以覆盖此方法以指定自定义类加载器。
	 * @param aopProxy the prepared AopProxy instance to get the proxy from
	 * @return the proxy object to expose
	 * @see AopProxy#getProxy(ClassLoader)
	 */
	protected Object getProxy(AopProxy aopProxy) {
		return aopProxy.getProxy(this.proxyClassLoader);
	}

	/**
	 * 检查 interceptorNames 列表，看其最后一个元素是否为目标名称。如果找到，则从列表中移除该名称并将其
	 * 设置为 targetName。
	 */
	private void checkInterceptorNames() {
		if (!ObjectUtils.isEmpty(this.interceptorNames)) {
			String finalName = this.interceptorNames[this.interceptorNames.length - 1];
			if (this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				// The last name in the chain may be an Advisor/Advice or a target/TargetSource.
				// Unfortunately we don't know; we must look at type of the bean.
				if (!finalName.endsWith(GLOBAL_SUFFIX) && !isNamedBeanAnAdvisorOrAdvice(finalName)) {
					// The target isn't an interceptor.
					this.targetName = finalName;
					if (logger.isDebugEnabled()) {
						logger.debug("Bean with name '" + finalName + "' concluding interceptor chain " +
								"is not an advisor class: treating it as a target or TargetSource");
					}
					this.interceptorNames = Arrays.copyOf(this.interceptorNames, this.interceptorNames.length - 1);
				}
			}
		}
	}

	/**
	 * 查看 Bean 工厂元数据，以判断这个位于 interceptorNames 列表末尾的 Bean 名称是 Advisor 还是 Advice，
	 * 或者可能是目标。
	 * @param beanName bean name to check
	 * @return {@code true} if it's an Advisor or Advice
	 */
	private boolean isNamedBeanAnAdvisorOrAdvice(String beanName) {
		Assert.state(this.beanFactory != null, "No BeanFactory set");
		Class<?> namedBeanClass = this.beanFactory.getType(beanName);
		if (namedBeanClass != null) {
			return (Advisor.class.isAssignableFrom(namedBeanClass) || Advice.class.isAssignableFrom(namedBeanClass));
		}
		// Treat it as a target bean if we can't tell.
		if (logger.isDebugEnabled()) {
			logger.debug("Could not determine type of bean with name '" + beanName +
					"' - assuming it is neither an Advisor nor an Advice");
		}
		return false;
	}

	/**
	 * 创建通知器（拦截器）链。来自 BeanFactory 的通知器会在每次新增原型实例时刷新。通过工厂 API 以编程
	 * 方式添加的拦截器不受此类更改影响。
	 */
	private synchronized void initializeAdvisorChain() throws AopConfigException, BeansException {
		if (!this.advisorChainInitialized && !ObjectUtils.isEmpty(this.interceptorNames)) {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve interceptor names " + Arrays.toString(this.interceptorNames));
			}

			// Globals can't be last unless we specified a targetSource using the property...
			if (this.interceptorNames[this.interceptorNames.length - 1].endsWith(GLOBAL_SUFFIX) &&
					this.targetName == null && this.targetSource == EMPTY_TARGET_SOURCE) {
				throw new AopConfigException("Target required after globals");
			}

			// Materialize interceptor chain from bean names.
			for (String name : this.interceptorNames) {
				if (name.endsWith(GLOBAL_SUFFIX)) {
					if (!(this.beanFactory instanceof ListableBeanFactory)) {
						throw new AopConfigException(
								"Can only use global advisors or interceptors with a ListableBeanFactory");
					}
					addGlobalAdvisors((ListableBeanFactory) this.beanFactory,
							name.substring(0, name.length() - GLOBAL_SUFFIX.length()));
				}

				else {
					// If we get here, we need to add a named interceptor.
					// We must check if it's a singleton or prototype.
					Object advice;
					if (this.singleton || this.beanFactory.isSingleton(name)) {
						// Add the real Advisor/Advice to the chain.
						advice = this.beanFactory.getBean(name);
					}
					else {
						// It's a prototype Advice or Advisor: replace with a prototype.
						// Avoid unnecessary creation of prototype bean just for advisor chain initialization.
						advice = new PrototypePlaceholderAdvisor(name);
					}
					addAdvisorOnChainCreation(advice);
				}
			}

			this.advisorChainInitialized = true;
		}
	}


	/**
	 * 返回独立的通知器链。每当返回新的原型实例时，我们都需要这样做，以便返回原型 Advisor 和 Advice 的
	 * 不同实例。
	 */
	private List<Advisor> freshAdvisorChain() {
		Advisor[] advisors = getAdvisors();
		List<Advisor> freshAdvisors = new ArrayList<>(advisors.length);
		for (Advisor advisor : advisors) {
			if (advisor instanceof PrototypePlaceholderAdvisor) {
				PrototypePlaceholderAdvisor pa = (PrototypePlaceholderAdvisor) advisor;
				if (logger.isDebugEnabled()) {
					logger.debug("Refreshing bean named '" + pa.getBeanName() + "'");
				}
				// Replace the placeholder with a fresh prototype instance resulting from a getBean lookup
				if (this.beanFactory == null) {
					throw new IllegalStateException("No BeanFactory available anymore (probably due to " +
							"serialization) - cannot resolve prototype advisor '" + pa.getBeanName() + "'");
				}
				Object bean = this.beanFactory.getBean(pa.getBeanName());
				Advisor refreshedAdvisor = namedBeanToAdvisor(bean);
				freshAdvisors.add(refreshedAdvisor);
			}
			else {
				// Add the shared instance.
				freshAdvisors.add(advisor);
			}
		}
		return freshAdvisors;
	}

	/**
	 * 添加所有全局拦截器和切点。
	 */
	private void addGlobalAdvisors(ListableBeanFactory beanFactory, String prefix) {
		String[] globalAdvisorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Advisor.class);
		String[] globalInterceptorNames =
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Interceptor.class);
		if (globalAdvisorNames.length > 0 || globalInterceptorNames.length > 0) {
			List<Object> beans = new ArrayList<>(globalAdvisorNames.length + globalInterceptorNames.length);
			for (String name : globalAdvisorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			for (String name : globalInterceptorNames) {
				if (name.startsWith(prefix)) {
					beans.add(beanFactory.getBean(name));
				}
			}
			AnnotationAwareOrderComparator.sort(beans);
			for (Object bean : beans) {
				addAdvisorOnChainCreation(bean);
			}
		}
	}

	/**
	 * 在创建通知链时调用。
	 * <p>将给定的 advice、advisor 或对象添加到拦截器列表。由于存在这三种可能性，我们无法把签名定义得更
	 * 具体。
	 * @param next advice, advisor or target object
	 */
	private void addAdvisorOnChainCreation(Object next) {
		// We need to convert to an Advisor if necessary so that our source reference
		// matches what we find from superclass interceptors.
		addAdvisor(namedBeanToAdvisor(next));
	}

	/**
	 * 返回创建代理时要使用的 TargetSource。如果目标未在 interceptorNames 列表末尾指定，则 TargetSource
	 * 将是此类的 TargetSource 成员。否则，我们会获取目标 Bean，并在必要时将其包装在 TargetSource 中。
	 */
	private TargetSource freshTargetSource() {
		if (this.targetName == null) {
			// Not refreshing target: bean name not specified in 'interceptorNames'
			return this.targetSource;
		}
		else {
			if (this.beanFactory == null) {
				throw new IllegalStateException("No BeanFactory available anymore (probably due to serialization) " +
						"- cannot resolve target with name '" + this.targetName + "'");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Refreshing target with name '" + this.targetName + "'");
			}
			Object target = this.beanFactory.getBean(this.targetName);
			return (target instanceof TargetSource ? (TargetSource) target : new SingletonTargetSource(target));
		}
	}

	/**
	 * 将通过对 interceptorNames 数组中的名称调用 getBean() 而得到的对象转换为 Advisor 或 TargetSource。
	 */
	private Advisor namedBeanToAdvisor(Object next) {
		try {
			return this.advisorAdapterRegistry.wrap(next);
		}
		catch (UnknownAdviceTypeException ex) {
			// We expected this to be an Advisor or Advice,
			// but it wasn't. This is a configuration error.
			throw new AopConfigException("Unknown advisor type " + next.getClass() +
					"; can only include Advisor or Advice type beans in interceptorNames chain " +
					"except for last entry which may also be target instance or TargetSource", ex);
		}
	}

	/**
	 * 在通知变更时清除并重新缓存单例。
	 */
	@Override
	protected void adviceChanged() {
		super.adviceChanged();
		if (this.singleton) {
			logger.debug("Advice has changed; re-caching singleton instance");
			synchronized (this) {
				this.singletonInstance = null;
			}
		}
	}


	//---------------------------------------------------------------------
	// Serialization support
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// Rely on default serialization; just initialize state after deserialization.
		ois.defaultReadObject();

		// Initialize transient fields.
		this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
	}


	/**
	 * 用于拦截器链中，在创建代理时需要用原型替换 Bean 的场景。
	 */
	private static class PrototypePlaceholderAdvisor implements Advisor, Serializable {

		private final String beanName;

		private final String message;

		public PrototypePlaceholderAdvisor(String beanName) {
			this.beanName = beanName;
			this.message = "Placeholder for prototype Advisor/Advice with bean name '" + beanName + "'";
		}

		public String getBeanName() {
			return this.beanName;
		}

		@Override
		public Advice getAdvice() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public boolean isPerInstance() {
			throw new UnsupportedOperationException("Cannot invoke methods: " + this.message);
		}

		@Override
		public String toString() {
			return this.message;
		}
	}

}
