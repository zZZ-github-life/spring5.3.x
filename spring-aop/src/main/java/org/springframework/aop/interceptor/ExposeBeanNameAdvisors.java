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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.Advisor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.support.DefaultIntroductionAdvisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.factory.NamedBean;
import org.springframework.lang.Nullable;

/**
 * 用于创建通知器的便捷方法，这些通知器可在自动代理由 Spring IoC 容器创建的 Bean 时使用，并将 Bean
 * 名称绑定到当前调用。可以与 AspectJ 配合支持 {@code bean()} 切点指示符。
 *
 * <p>通常用于 Spring 自动代理场景，此时 Bean 名称在创建代理时即已确定。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.NamedBean
 */
public abstract class ExposeBeanNameAdvisors {

	/**
	 * 用于在当前被调用的 Bean 上绑定 Bean 名称，该名称存放在 ReflectiveMethodInvocation 的
	 * userAttributes Map 中。
	 */
	private static final String BEAN_NAME_ATTRIBUTE = ExposeBeanNameAdvisors.class.getName() + ".BEAN_NAME";


	/**
	 * 查找当前调用的 Bean 名称。假定拦截器链中已包含 ExposeBeanNameAdvisor，并且调用已通过
	 * ExposeInvocationInterceptor 暴露。
	 * @return the bean name (never {@code null})
	 * @throws IllegalStateException if the bean name has not been exposed
	 */
	public static String getBeanName() throws IllegalStateException {
		return getBeanName(ExposeInvocationInterceptor.currentInvocation());
	}

	/**
	 * 查找给定调用的 Bean 名称。假定拦截器链中已包含 ExposeBeanNameAdvisor。
	 * @param mi the MethodInvocation that should contain the bean name as an attribute
	 * @return the bean name (never {@code null})
	 * @throws IllegalStateException if the bean name has not been exposed
	 */
	public static String getBeanName(MethodInvocation mi) throws IllegalStateException {
		if (!(mi instanceof ProxyMethodInvocation)) {
			throw new IllegalArgumentException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
		}
		ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
		String beanName = (String) pmi.getUserAttribute(BEAN_NAME_ATTRIBUTE);
		if (beanName == null) {
			throw new IllegalStateException("Cannot get bean name; not set on MethodInvocation: " + mi);
		}
		return beanName;
	}

	/**
	 * 创建一个新的通知器，用于暴露给定的 Bean 名称，且不包含引入。
	 * @param beanName bean name to expose
	 */
	public static Advisor createAdvisorWithoutIntroduction(String beanName) {
		return new DefaultPointcutAdvisor(new ExposeBeanNameInterceptor(beanName));
	}

	/**
	 * 创建一个新的通知器，用于暴露给定的 Bean 名称，并引入 NamedBean 接口，使 Bean 名称可被访问，而无
	 * 需让目标对象感知这一 Spring IoC 概念。
	 * @param beanName the bean name to expose
	 */
	public static Advisor createAdvisorIntroducingNamedBean(String beanName) {
		return new DefaultIntroductionAdvisor(new ExposeBeanNameIntroduction(beanName));
	}


	/**
	 * 将指定的 Bean 名称作为调用属性暴露出来的拦截器。
	 */
	private static class ExposeBeanNameInterceptor implements MethodInterceptor {

		private final String beanName;

		public ExposeBeanNameInterceptor(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@Nullable
		public Object invoke(MethodInvocation mi) throws Throwable {
			if (!(mi instanceof ProxyMethodInvocation)) {
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}
			ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
			pmi.setUserAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);
			return mi.proceed();
		}
	}


	/**
	 * 将指定的 Bean 名称作为调用属性暴露出来的引入。
	 */
	@SuppressWarnings("serial")
	private static class ExposeBeanNameIntroduction extends DelegatingIntroductionInterceptor implements NamedBean {

		private final String beanName;

		public ExposeBeanNameIntroduction(String beanName) {
			this.beanName = beanName;
		}

		@Override
		@Nullable
		public Object invoke(MethodInvocation mi) throws Throwable {
			if (!(mi instanceof ProxyMethodInvocation)) {
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}
			ProxyMethodInvocation pmi = (ProxyMethodInvocation) mi;
			pmi.setUserAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);
			return super.invoke(mi);
		}

		@Override
		public String getBeanName() {
			return this.beanName;
		}
	}

}
