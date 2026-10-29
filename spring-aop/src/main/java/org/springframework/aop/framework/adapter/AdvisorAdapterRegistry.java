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

package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * Advisor 适配器注册表的接口。
 *
 * <p><i>这是一个 SPI 接口，任何 Spring 用户都不应实现它。</i>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
public interface AdvisorAdapterRegistry {

	/**
	 * 返回包装给定 advice 的 {@link Advisor}。
	 * <p>默认情况下至少应支持 {@link org.aopalliance.intercept.MethodInterceptor}、
	 * {@link org.springframework.aop.MethodBeforeAdvice}、
	 * {@link org.springframework.aop.AfterReturningAdvice}、
	 * {@link org.springframework.aop.ThrowsAdvice}。
	 * @param advice an object that should be an advice
	 * @return an Advisor wrapping the given advice (never {@code null};
	 * if the advice parameter is an Advisor, it is to be returned as-is)
	 * @throws UnknownAdviceTypeException if no registered advisor adapter
	 * can wrap the supposed advice
	 */
	Advisor wrap(Object advice) throws UnknownAdviceTypeException;

	/**
	 * 返回 AOP Alliance MethodInterceptor 数组，以便在基于拦截的框架中使用给定的 Advisor。
	 * <p>如果 {@link Advisor} 是 {@link org.springframework.aop.PointcutAdvisor}，不必担心与之关联的切
	 * 点：只需返回一个拦截器。
	 * @param advisor the Advisor to find an interceptor for
	 * @return an array of MethodInterceptors to expose this Advisor's behavior
	 * @throws UnknownAdviceTypeException if the Advisor type is
	 * not understood by any registered AdvisorAdapter
	 */
	MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

	/**
	 * 注册给定的 {@link AdvisorAdapter}。请注意，无需为 AOP Alliance Interceptor 或 Spring Advice 注册
	 * 适配器：它们必须由 {@code AdvisorAdapterRegistry} 实现自动识别。
	 * @param adapter an AdvisorAdapter that understands particular Advisor or Advice types
	 */
	void registerAdvisorAdapter(AdvisorAdapter adapter);

}
