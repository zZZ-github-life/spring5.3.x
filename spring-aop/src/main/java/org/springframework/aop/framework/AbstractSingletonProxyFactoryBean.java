/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 用于生成单例作用域代理对象的 {@link FactoryBean} 类型的便捷超类。
 *
 * <p>管理前置和后置拦截器（与 {@link ProxyFactoryBean} 中一样是引用，而不是拦截器名称），并提供一
 * 致的接口管理。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractSingletonProxyFactoryBean extends ProxyConfig
		implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Object target;

	@Nullable
	private Class<?>[] proxyInterfaces;

	@Nullable
	private Object[] preInterceptors;

	@Nullable
	private Object[] postInterceptors;

	/** 默认为全局 AdvisorAdapterRegistry。 */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	@Nullable
	private transient ClassLoader proxyClassLoader;

	@Nullable
	private Object proxy;


	/**
	 * 设置目标对象，即要用事务代理包装的 Bean。
	 * <p>目标可以是任何对象，此时会创建一个 SingletonTargetSource。如果它是一个 TargetSource，则不会创
	 * 建包装用的 TargetSource：这允许使用池化或原型 TargetSource 等。
	 * @see org.springframework.aop.TargetSource
	 * @see org.springframework.aop.target.SingletonTargetSource
	 * @see org.springframework.aop.target.LazyInitTargetSource
	 * @see org.springframework.aop.target.PrototypeTargetSource
	 * @see org.springframework.aop.target.CommonsPool2TargetSource
	 */
	public void setTarget(Object target) {
		this.target = target;
	}

	/**
	 * 指定要代理的接口集。
	 * <p>如果未指定（默认情况），AOP 基础设施会通过分析目标来确定需要代理哪些接口，并代理目标对象实现
	 * 的所有接口。
	 */
	public void setProxyInterfaces(Class<?>[] proxyInterfaces) {
		this.proxyInterfaces = proxyInterfaces;
	}

	/**
	 * 设置要在隐式事务拦截器之前应用的附加拦截器（或 advisor），例如 PerformanceMonitorInterceptor。
	 * <p>可以指定任何 AOP Alliance MethodInterceptor 或其他 Spring AOP Advice，以及 Spring AOP Advisor。
	 * @see org.springframework.aop.interceptor.PerformanceMonitorInterceptor
	 */
	public void setPreInterceptors(Object[] preInterceptors) {
		this.preInterceptors = preInterceptors;
	}

	/**
	 * 设置要在隐式事务拦截器之后应用的附加拦截器（或 advisor）。
	 * <p>可以指定任何 AOP Alliance MethodInterceptor 或其他 Spring AOP Advice，以及 Spring AOP Advisor。
	 */
	public void setPostInterceptors(Object[] postInterceptors) {
		this.postInterceptors = postInterceptors;
	}

	/**
	 * 指定要使用的 AdvisorAdapterRegistry。默认是全局 AdvisorAdapterRegistry。
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * 设置用于生成代理类的 ClassLoader。
	 * <p>默认是 Bean 的 ClassLoader，即包含它的 BeanFactory 用来加载所有 Bean 类的 ClassLoader。可以在
	 * 此针对特定的代理进行覆盖。
	 */
	public void setProxyClassLoader(ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (this.proxyClassLoader == null) {
			this.proxyClassLoader = classLoader;
		}
	}


	@Override
	public void afterPropertiesSet() {
		if (this.target == null) {
			throw new IllegalArgumentException("Property 'target' is required");
		}
		if (this.target instanceof String) {
			throw new IllegalArgumentException("'target' needs to be a bean reference, not a bean name as value");
		}
		if (this.proxyClassLoader == null) {
			this.proxyClassLoader = ClassUtils.getDefaultClassLoader();
		}

		ProxyFactory proxyFactory = new ProxyFactory();

		if (this.preInterceptors != null) {
			for (Object interceptor : this.preInterceptors) {
				proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
			}
		}

		// Add the main interceptor (typically an Advisor).
		proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(createMainInterceptor()));

		if (this.postInterceptors != null) {
			for (Object interceptor : this.postInterceptors) {
				proxyFactory.addAdvisor(this.advisorAdapterRegistry.wrap(interceptor));
			}
		}

		proxyFactory.copyFrom(this);

		TargetSource targetSource = createTargetSource(this.target);
		proxyFactory.setTargetSource(targetSource);

		if (this.proxyInterfaces != null) {
			proxyFactory.setInterfaces(this.proxyInterfaces);
		}
		else if (!isProxyTargetClass()) {
			// Rely on AOP infrastructure to tell us what interfaces to proxy.
			Class<?> targetClass = targetSource.getTargetClass();
			if (targetClass != null) {
				proxyFactory.setInterfaces(ClassUtils.getAllInterfacesForClass(targetClass, this.proxyClassLoader));
			}
		}

		postProcessProxyFactory(proxyFactory);

		this.proxy = proxyFactory.getProxy(this.proxyClassLoader);
	}

	/**
	 * 为给定的目标（或 TargetSource）确定一个 TargetSource。
	 * @param target the target. If this is an implementation of TargetSource it is
	 * used as our TargetSource; otherwise it is wrapped in a SingletonTargetSource.
	 * @return a TargetSource for this object
	 */
	protected TargetSource createTargetSource(Object target) {
		if (target instanceof TargetSource) {
			return (TargetSource) target;
		}
		else {
			return new SingletonTargetSource(target);
		}
	}

	/**
	 * 供子类在使用 {@link ProxyFactory} 创建代理实例之前对其进行后处理的钩子。
	 * @param proxyFactory the AOP ProxyFactory about to be used
	 * @since 4.2
	 */
	protected void postProcessProxyFactory(ProxyFactory proxyFactory) {
	}


	@Override
	public Object getObject() {
		if (this.proxy == null) {
			throw new FactoryBeanNotInitializedException();
		}
		return this.proxy;
	}

	@Override
	@Nullable
	public Class<?> getObjectType() {
		if (this.proxy != null) {
			return this.proxy.getClass();
		}
		if (this.proxyInterfaces != null && this.proxyInterfaces.length == 1) {
			return this.proxyInterfaces[0];
		}
		if (this.target instanceof TargetSource) {
			return ((TargetSource) this.target).getTargetClass();
		}
		if (this.target != null) {
			return this.target.getClass();
		}
		return null;
	}

	@Override
	public final boolean isSingleton() {
		return true;
	}


	/**
	 * 为此代理工厂 Bean 创建“主”拦截器。通常是 Advisor，但也可以是任何类型的 Advice。
	 * <p>前置拦截器将在此拦截器之前应用，后置拦截器将在此拦截器之后应用。
	 */
	protected abstract Object createMainInterceptor();

}
