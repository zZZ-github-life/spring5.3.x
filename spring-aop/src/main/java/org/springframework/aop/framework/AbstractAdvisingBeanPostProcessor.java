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

package org.springframework.aop.framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.SmartClassLoader;
import org.springframework.lang.Nullable;

/**
 * 用于将 Spring AOP {@link Advisor} 应用于特定 Bean 的 {@link BeanPostProcessor} 实现的基类。
 *
 * @author Juergen Hoeller
 * @since 3.2
 */
@SuppressWarnings("serial")
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyProcessorSupport implements BeanPostProcessor {

	@Nullable
	protected Advisor advisor;

	protected boolean beforeExistingAdvisors = false;

	private final Map<Class<?>, Boolean> eligibleBeans = new ConcurrentHashMap<>(256);


	/**
	 * 设置当遇到已被通知过的对象时，此后处理器的 advisor 是否应在此之前已有的 advisor 之前应用。
	 * <p>默认值为 "false"，即在现有 advisor 之后应用该 advisor，也就是尽可能靠近目标方法。将其切换为
	 * "true"，以便此后处理器的 advisor 也能包裹现有 advisor。
	 * <p>注意：请查看具体后处理器的 javadoc，了解它是否会根据其 advisor 的性质默认更改此标志。
	 */
	public void setBeforeExistingAdvisors(boolean beforeExistingAdvisors) {
		this.beforeExistingAdvisors = beforeExistingAdvisors;
	}


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (this.advisor == null || bean instanceof AopInfrastructureBean) {
			// Ignore AOP infrastructure such as scoped proxies.
			return bean;
		}

		if (bean instanceof Advised) {
			Advised advised = (Advised) bean;
			if (!advised.isFrozen() && isEligible(AopUtils.getTargetClass(bean))) {
				// Add our local Advisor to the existing proxy's Advisor chain...
				if (this.beforeExistingAdvisors) {
					advised.addAdvisor(0, this.advisor);
				}
				else {
					advised.addAdvisor(this.advisor);
				}
				return bean;
			}
		}

		if (isEligible(bean, beanName)) {
			ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName);
			if (!proxyFactory.isProxyTargetClass()) {
				evaluateProxyInterfaces(bean.getClass(), proxyFactory);
			}
			proxyFactory.addAdvisor(this.advisor);
			customizeProxyFactory(proxyFactory);

			// Use original ClassLoader if bean class not locally loaded in overriding class loader
			ClassLoader classLoader = getProxyClassLoader();
			if (classLoader instanceof SmartClassLoader && classLoader != bean.getClass().getClassLoader()) {
				classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();
			}
			return proxyFactory.getProxy(classLoader);
		}

		// No proxy needed.
		return bean;
	}

	/**
	 * 检查给定的 Bean 是否有资格被此后处理器的 {@link Advisor} 通知。
	 * <p>目标类检查委托给 {@link #isEligible(Class)}。可以重写该方法，例如按名称专门排除某些 Bean。
	 * <p>注意：仅对普通 Bean 实例调用，不会对现有代理实例调用；后者实现了 {@link Advised}，并允许将本
	 * 地 {@link Advisor} 添加到现有代理的 {@link Advisor} 链中。对于后者，会直接调用
	 * {@link #isEligible(Class)}，并传入现有代理背后的实际目标类（由
	 * {@link AopUtils#getTargetClass(Object)} 确定）。
	 * @param bean Bean 实例
	 * @param beanName Bean 的名称
	 * @see #isEligible(Class)
	 */
	protected boolean isEligible(Object bean, String beanName) {
		return isEligible(bean.getClass());
	}

	/**
	 * 检查给定的类是否有资格被此后处理器的 {@link Advisor} 通知。
	 * <p>按 Bean 目标类缓存 {@code canApply} 的结果。
	 * @param targetClass 要检查的类
	 * @see AopUtils#canApply(Advisor, Class)
	 */
	protected boolean isEligible(Class<?> targetClass) {
		Boolean eligible = this.eligibleBeans.get(targetClass);
		if (eligible != null) {
			return eligible;
		}
		if (this.advisor == null) {
			return false;
		}
		eligible = AopUtils.canApply(this.advisor, targetClass);
		this.eligibleBeans.put(targetClass, eligible);
		return eligible;
	}

	/**
	 * 为给定的 Bean 准备一个 {@link ProxyFactory}。
	 * <p>子类可以自定义目标实例的处理方式，特别是目标类的暴露方式。随后会应用默认的非目标类代理接口内
	 * 省以及已配置的 advisor；{@link #customizeProxyFactory} 允许在创建代理之前立即对这些部分进行后期
	 * 定制。
	 * @param bean 要为其创建代理的 Bean 实例
	 * @param beanName 对应的 Bean 名称
	 * @return ProxyFactory，已使用此处理器的 {@link ProxyConfig} 设置
	 * 和指定的 Bean 初始化
	 * @since 4.2.3
	 * @see #customizeProxyFactory
	 */
	protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.copyFrom(this);
		proxyFactory.setTarget(bean);
		return proxyFactory;
	}

	/**
	 * 子类可以选择实现此方法：例如，更改所暴露的接口。
	 * <p>默认实现为空。
	 * @param proxyFactory 已配置好目标、通知器和接口的 ProxyFactory，
	 * 它将在该方法返回后立即用于创建代理
	 * @since 4.2.3
	 * @see #prepareProxyFactory
	 */
	protected void customizeProxyFactory(ProxyFactory proxyFactory) {
	}

}
