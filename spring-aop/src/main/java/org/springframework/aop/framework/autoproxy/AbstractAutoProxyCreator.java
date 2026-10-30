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

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.ProxyProcessorSupport;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} 实现，用 AOP 代理包装每个符合
 * 条件的 Bean，在调用 Bean 本身之前委托给指定的拦截器。
 *
 * <p>此类区分“公共”拦截器（对其创建的所有代理共享）和“特定”拦截器（每个 Bean 实例独有）。公共拦截
 * 器可以不存在。如果存在，则通过 interceptorNames 属性设置。与
 * {@link org.springframework.aop.framework.ProxyFactoryBean} 一样，这里使用的是当前工厂中的拦截器
 * 名称而不是 Bean 引用，以便正确处理原型 Advisor 和拦截器：例如，支持有状态的 mixin。
 * {@link #setInterceptorNames "interceptorNames"} 条目支持任何 advice 类型。
 *
 * <p>当有大量 Bean 需要用相似的代理（即委托给相同的拦截器）包装时，这种自动代理特别有用。对于 x 个
 * 目标 Bean，您无需编写 x 个重复的代理定义，只需向 Bean 工厂注册一个这样的后处理器即可达到相同效果。
 *
 * <p>子类可以应用任何策略来决定是否代理某个 Bean，例如按类型、按名称、按定义细节等。它们还可以返回
 * 只应应用于特定 Bean 实例的附加拦截器。一个简单的具体实现是 {@link BeanNameAutoProxyCreator}，它
 * 通过给定名称识别要代理的 Bean。
 *
 * <p>可以使用任意数量的 {@link TargetSourceCreator} 实现来创建自定义目标源：例如，用于池化原型对象。
 * 只要 TargetSourceCreator 指定了自定义的 {@link org.springframework.aop.TargetSource}，即使没有
 * 通知也会进行自动代理。如果未设置任何 TargetSourceCreators，或没有一个匹配，则默认使用
 * {@link org.springframework.aop.target.SingletonTargetSource} 包装目标 Bean 实例。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 13.10.2003
 * @see #setInterceptorNames
 * @see #getAdvicesAndAdvisorsForBean
 * @see BeanNameAutoProxyCreator
 * @see DefaultAdvisorAutoProxyCreator
 */
@SuppressWarnings("serial")
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
		implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {

	/**
	 * 供子类使用的便捷常量：表示“不代理”的返回值。
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Nullable
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * 供子类使用的便捷常量：表示“进行代理但不添加额外拦截器，仅使用公共拦截器”的返回值。
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 默认为全局 AdvisorAdapterRegistry。 */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * 指示是否应冻结代理。从父类覆盖，以防止配置过早被冻结。
	 */
	private boolean freezeProxy = false;

	/** 默认没有公共拦截器。 */
	private String[] interceptorNames = new String[0];

	private boolean applyCommonInterceptorsFirst = true;

	@Nullable
	private TargetSourceCreator[] customTargetSourceCreators;

	@Nullable
	private BeanFactory beanFactory;

	private final Set<String> targetSourcedBeans = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

	private final Map<Object, Object> earlyProxyReferences = new ConcurrentHashMap<>(16);

	private final Map<Object, Class<?>> proxyTypes = new ConcurrentHashMap<>(16);

	private final Map<Object, Boolean> advisedBeans = new ConcurrentHashMap<>(256);


	/**
	 * 设置是否应冻结代理，从而在代理创建后阻止向其添加通知。
	 * <p>从超类覆盖此方法，以防止代理配置在代理创建之前被冻结。
	 */
	@Override
	public void setFrozen(boolean frozen) {
		this.freezeProxy = frozen;
	}

	@Override
	public boolean isFrozen() {
		return this.freezeProxy;
	}

	/**
	 * 指定要使用的 {@link AdvisorAdapterRegistry}。
	 * <p>默认是全局 {@link AdvisorAdapterRegistry}。
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * 设置要按此顺序应用的自定义 {@code TargetSourceCreators}。如果列表为空，或它们都返回 null，则会为
	 * 每个 Bean 创建一个 {@link SingletonTargetSource}。
	 * <p>请注意，即使目标 Bean 未找到任何 advice 或 advisor，TargetSourceCreator 也会生效。如果某个
	 * {@code TargetSourceCreator} 为特定 Bean 返回了 {@link TargetSource}，则该 Bean 无论如何都会被代
	 * 理。
	 * <p>只有在 {@link BeanFactory} 中使用此外后处理器，并触发其 {@link BeanFactoryAware} 回调时，才能
	 * 调用 {@code TargetSourceCreators}。
	 * @param targetSourceCreators {@code TargetSourceCreators} 列表。顺序很重要：将使用第一个匹配的
	 * {@code TargetSourceCreator}（即第一个返回非 null 的）所返回的
	 * {@code TargetSource}。
	 */
	public void setCustomTargetSourceCreators(TargetSourceCreator... targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * 设置公共拦截器。这些必须是当前工厂中的 Bean 名称。它们可以是 Spring 支持的任何 advice 或 advisor
	 * 类型。
	 * <p>如果未设置此属性，则不会有任何公共拦截器。如果“特定”拦截器（如匹配的 Advisor）就是我们想要的
	 * 全部内容，这完全有效。
	 */
	public void setInterceptorNames(String... interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * 设置公共拦截器是否应在 Bean 特定的拦截器之前应用。默认值为 "true"；否则，将先应用 Bean 特定的拦
	 * 截器。
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回所属的 {@link BeanFactory}。可能为 {@code null}，因为此后处理器无需属于某个 Bean 工厂。
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}


	@Override
	@Nullable
	public Class<?> predictBeanType(Class<?> beanClass, String beanName) {
		if (this.proxyTypes.isEmpty()) {
			return null;
		}
		Object cacheKey = getCacheKey(beanClass, beanName);
		return this.proxyTypes.get(cacheKey);
	}

	@Override
	@Nullable
	public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {
		return null;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) {
		Object cacheKey = getCacheKey(bean.getClass(), beanName);
		this.earlyProxyReferences.put(cacheKey, bean);
		return wrapIfNecessary(bean, beanName, cacheKey);
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
		Object cacheKey = getCacheKey(beanClass, beanName);

		if (!StringUtils.hasLength(beanName) || !this.targetSourcedBeans.contains(beanName)) {
			if (this.advisedBeans.containsKey(cacheKey)) {
				return null;
			}
			if (isInfrastructureClass(beanClass) || shouldSkip(beanClass, beanName)) {
				this.advisedBeans.put(cacheKey, Boolean.FALSE);
				return null;
			}
		}

		// Create proxy here if we have a custom TargetSource.
		// Suppresses unnecessary default instantiation of the target bean:
		// The TargetSource will handle target instances in a custom fashion.
		TargetSource targetSource = getCustomTargetSource(beanClass, beanName);
		if (targetSource != null) {
			if (StringUtils.hasLength(beanName)) {
				this.targetSourcedBeans.add(beanName);
			}
			Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(beanClass, beanName, targetSource);
			Object proxy = createProxy(beanClass, beanName, specificInterceptors, targetSource);
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		return null;
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
		return pvs;  // skip postProcessPropertyValues
	}

	/**
	 * 如果 Bean 被子类标识为需要代理，则使用配置的拦截器创建代理。
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	@Override
	public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {
		if (bean != null) {
			Object cacheKey = getCacheKey(bean.getClass(), beanName);
			if (this.earlyProxyReferences.remove(cacheKey) != bean) {
				return wrapIfNecessary(bean, beanName, cacheKey);
			}
		}
		return bean;
	}


	/**
	 * 为给定 Bean 类和 Bean 名称构建缓存键。
	 * <p>注意：自 4.2.3 起，此实现不再返回拼接的类/名称字符串，而是返回尽可能高效的缓存键：一个纯 Bean
	 * 名称；对于 {@code FactoryBean}，会在前面加上 {@link BeanFactory#FACTORY_BEAN_PREFIX}；如果未指定
	 * Bean 名称，则直接使用给定的 Bean {@code Class}。
	 * @param beanClass Bean 类
	 * @param beanName Bean 名称
	 * @return 给定类和名称的缓存键
	 */
	protected Object getCacheKey(Class<?> beanClass, @Nullable String beanName) {
		if (StringUtils.hasLength(beanName)) {
			return (FactoryBean.class.isAssignableFrom(beanClass) ?
					BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
		}
		else {
			return beanClass;
		}
	}

	/**
	 * 如有必要（即有资格被代理），包装给定 Bean。
	 * @param bean 原始 Bean 实例
	 * @param beanName Bean 的名称
	 * @param cacheKey 用于元数据访问的缓存键
	 * @return 包装该 Bean 的代理，或原样的原始 Bean 实例
	 */
	protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
		if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {
			return bean;
		}
		if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
			return bean;
		}
		if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
			this.advisedBeans.put(cacheKey, Boolean.FALSE);
			return bean;
		}

		// Create proxy if we have advice.
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
		if (specificInterceptors != DO_NOT_PROXY) {
			this.advisedBeans.put(cacheKey, Boolean.TRUE);
			Object proxy = createProxy(
					bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
			this.proxyTypes.put(cacheKey, proxy.getClass());
			return proxy;
		}

		this.advisedBeans.put(cacheKey, Boolean.FALSE);
		return bean;
	}

	/**
	 * 返回给定 Bean 类是否表示一个绝不应被代理的基础设施类。
	 * <p>默认实现将 Advice、Advisor 和 AopInfrastructureBeans 视为基础设施类。
	 * @param beanClass Bean 的类
	 * @return 该 Bean 是否表示基础设施类
	 * @see org.aopalliance.aop.Advice
	 * @see org.springframework.aop.Advisor
	 * @see org.springframework.aop.framework.AopInfrastructureBean
	 * @see #shouldSkip
	 */
	protected boolean isInfrastructureClass(Class<?> beanClass) {
		boolean retVal = Advice.class.isAssignableFrom(beanClass) ||
				Pointcut.class.isAssignableFrom(beanClass) ||
				Advisor.class.isAssignableFrom(beanClass) ||
				AopInfrastructureBean.class.isAssignableFrom(beanClass);
		if (retVal && logger.isTraceEnabled()) {
			logger.trace("Did not attempt to auto-proxy infrastructure class [" + beanClass.getName() + "]");
		}
		return retVal;
	}

	/**
	 * 如果给定 Bean 不应被此后处理器考虑进行自动代理，子类应覆盖此方法并返回 {@code true}。
	 * <p>有时我们需要避免这种情况发生，例如，如果它会导致循环引用，或者需要保留现有的目标实例。除非
	 * Bean 名称按照 {@code AutowireCapableBeanFactory} 约定表示“原始实例”，否则此实现返回
	 * {@code false}。
	 * @param beanClass Bean 的类
	 * @param beanName Bean 的名称
	 * @return 是否跳过给定的 Bean
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#ORIGINAL_INSTANCE_SUFFIX
	 */
	protected boolean shouldSkip(Class<?> beanClass, String beanName) {
		return AutoProxyUtils.isOriginalInstance(beanName, beanClass);
	}

	/**
	 * 为 Bean 实例创建目标源。如果设置了任何 TargetSourceCreators，则使用它们。如果不应使用自定义
	 * TargetSource，则返回 {@code null}。
	 * <p>此实现使用 "customTargetSourceCreators" 属性。子类可以覆盖此方法以使用不同的机制。
	 * @param beanClass 要为其创建 TargetSource 的 Bean 的类
	 * @param beanName Bean 的名称
	 * @return 该 Bean 的 TargetSource
	 * @see #setCustomTargetSourceCreators
	 */
	@Nullable
	protected TargetSource getCustomTargetSource(Class<?> beanClass, String beanName) {
		// We can't create fancy target sources for directly registered singletons.
		if (this.customTargetSourceCreators != null &&
				this.beanFactory != null && this.beanFactory.containsBean(beanName)) {
			for (TargetSourceCreator tsc : this.customTargetSourceCreators) {
				TargetSource ts = tsc.getTargetSource(beanClass, beanName);
				if (ts != null) {
					// Found a matching TargetSource.
					if (logger.isTraceEnabled()) {
						logger.trace("TargetSourceCreator [" + tsc +
								"] found custom TargetSource for bean with name '" + beanName + "'");
					}
					return ts;
				}
			}
		}

		// No custom TargetSource found.
		return null;
	}

	/**
	 * 为给定 Bean 创建 AOP 代理。
	 * @param beanClass Bean 的类
	 * @param beanName Bean 的名称
	 * @param specificInterceptors 特定于此 Bean 的拦截器集合（可以为空，但不能为 null）
	 * @param targetSource 代理的 TargetSource，已预先配置好以访问该 Bean
	 * @return 该 Bean 的 AOP 代理
	 * @see #buildAdvisors
	 */
	protected Object createProxy(Class<?> beanClass, @Nullable String beanName,
			@Nullable Object[] specificInterceptors, TargetSource targetSource) {

		if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
			AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
		}

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);

		if (proxyFactory.isProxyTargetClass()) {
			// Explicit handling of JDK proxy targets and lambdas (for introduction advice scenarios)
			if (Proxy.isProxyClass(beanClass) || ClassUtils.isLambdaClass(beanClass)) {
				// Must allow for introductions; can't just set interfaces to the proxy's interfaces only.
				for (Class<?> ifc : beanClass.getInterfaces()) {
					proxyFactory.addInterface(ifc);
				}
			}
		}
		else {
			// No proxyTargetClass flag enforced, let's apply our default checks...
			if (shouldProxyTargetClass(beanClass, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				evaluateProxyInterfaces(beanClass, proxyFactory);
			}
		}

		Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
		proxyFactory.addAdvisors(advisors);
		proxyFactory.setTargetSource(targetSource);
		customizeProxyFactory(proxyFactory);

		proxyFactory.setFrozen(this.freezeProxy);
		if (advisorsPreFiltered()) {
			proxyFactory.setPreFiltered(true);
		}

		// Use original ClassLoader if bean class not locally loaded in overriding class loader
		ClassLoader classLoader = getProxyClassLoader();
		if (classLoader instanceof SmartClassLoader && classLoader != beanClass.getClassLoader()) {
			classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
		}
		return proxyFactory.getProxy(classLoader);
	}

	/**
	 * 确定给定 Bean 是应使用其目标类还是其接口进行代理。
	 * <p>检查相应 Bean 定义的
	 * {@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE "preserveTargetClass" attribute} 属性。
	 * @param beanClass Bean 的类
	 * @param beanName Bean 的名称
	 * @return 给定 Bean 是否应使用其目标类进行代理
	 * @see AutoProxyUtils#shouldProxyTargetClass
	 */
	protected boolean shouldProxyTargetClass(Class<?> beanClass, @Nullable String beanName) {
		return (this.beanFactory instanceof ConfigurableListableBeanFactory &&
				AutoProxyUtils.shouldProxyTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName));
	}

	/**
	 * 返回子类返回的 Advisor 是否已经预筛选以匹配 Bean 的目标类，从而在为 AOP 调用构建通知器链时可以跳
	 * 过 ClassFilter 检查。
	 * <p>默认值为 {@code false}。如果子类始终返回预筛选的 Advisor，则可以覆盖此方法。
	 * @return Advisor 是否已预先过滤
	 * @see #getAdvicesAndAdvisorsForBean
	 * @see org.springframework.aop.framework.Advised#setPreFiltered
	 */
	protected boolean advisorsPreFiltered() {
		return false;
	}

	/**
	 * 确定给定 Bean 的通知器，包括特定拦截器以及公共拦截器，全部适配为 Advisor 接口。
	 * @param beanName Bean 的名称
	 * @param specificInterceptors 特定于此 Bean 的拦截器集合（可以为空，但不能为 null）
	 * @return 给定 Bean 的 Advisor 列表
	 */
	protected Advisor[] buildAdvisors(@Nullable String beanName, @Nullable Object[] specificInterceptors) {
		// Handle prototypes correctly...
		Advisor[] commonInterceptors = resolveInterceptorNames();

		List<Object> allInterceptors = new ArrayList<>();
		if (specificInterceptors != null) {
			if (specificInterceptors.length > 0) {
				// specificInterceptors may equal PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
				allInterceptors.addAll(Arrays.asList(specificInterceptors));
			}
			if (commonInterceptors.length > 0) {
				if (this.applyCommonInterceptorsFirst) {
					allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
				}
				else {
					allInterceptors.addAll(Arrays.asList(commonInterceptors));
				}
			}
		}
		if (logger.isTraceEnabled()) {
			int nrOfCommonInterceptors = commonInterceptors.length;
			int nrOfSpecificInterceptors = (specificInterceptors != null ? specificInterceptors.length : 0);
			logger.trace("Creating implicit proxy for bean '" + beanName + "' with " + nrOfCommonInterceptors +
					" common interceptors and " + nrOfSpecificInterceptors + " specific interceptors");
		}

		Advisor[] advisors = new Advisor[allInterceptors.size()];
		for (int i = 0; i < allInterceptors.size(); i++) {
			advisors[i] = this.advisorAdapterRegistry.wrap(allInterceptors.get(i));
		}
		return advisors;
	}

	/**
	 * 将指定的拦截器名称解析为 Advisor 对象。
	 * @see #setInterceptorNames
	 */
	private Advisor[] resolveInterceptorNames() {
		BeanFactory bf = this.beanFactory;
		ConfigurableBeanFactory cbf = (bf instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) bf : null);
		List<Advisor> advisors = new ArrayList<>();
		for (String beanName : this.interceptorNames) {
			if (cbf == null || !cbf.isCurrentlyInCreation(beanName)) {
				Assert.state(bf != null, "BeanFactory required for resolving interceptor names");
				Object next = bf.getBean(beanName);
				advisors.add(this.advisorAdapterRegistry.wrap(next));
			}
		}
		return advisors.toArray(new Advisor[0]);
	}

	/**
	 * 子类可以选择实现此方法：例如，更改暴露的接口。
	 * <p>默认实现为空。
	 * @param proxyFactory 已配置好 TargetSource 和接口的 ProxyFactory，
	 * 它将在该方法返回后立即用于创建代理
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}


	/**
	 * 返回给定 Bean 是否要被代理，以及要应用哪些附加 advice（例如 AOP Alliance 拦截器）和通知器。
	 * @param beanClass 要通知的 Bean 的类
	 * @param beanName Bean 的名称
	 * @param customTargetSource 由 {@link #getCustomTargetSource} 方法返回的 TargetSource：可以被忽略。
	 * 如果没有使用自定义目标源，则为 {@code null}。
	 * @return 针对特定 Bean 的附加拦截器数组；如果没有附加拦截器而只有公共拦截器，
	 * 则为空数组；如果根本没有代理，甚至连公共拦截器都没有，
	 * 则为 {@code null}。参见常量 DO_NOT_PROXY 和
	 * PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS。
	 * @throws BeansException 如果发生错误
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	@Nullable
	protected abstract Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
			@Nullable TargetSource customTargetSource) throws BeansException;

}
