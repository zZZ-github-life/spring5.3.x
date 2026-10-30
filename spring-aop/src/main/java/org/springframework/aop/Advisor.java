/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * 持有 AOP <b>通知</b>（在连接点上执行的动作）以及一个用于确定通知适用性（例如切点）的过滤器的基础
 * 接口。<i>此接口不是供 Spring 用户使用的，而是为了在支持不同类型的通知时保持一致性。</i>
 *
 * <p>Spring AOP 基于通过方法<b>拦截</b>提供的<b>环绕通知</b>，并遵循 AOP Alliance 拦截 API。
 * Advisor 接口允许支持不同类型的通知，
 * 例如<b>前置</b>和<b>后置</b>通知，这些通知不必通过拦截来实现。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface Advisor {

	/**
	 * 用于空 {@code Advice} 的公共占位符，当尚未配置合适的通知时，由 {@link #getAdvice()} 返回。
	 * @since 5.0
	 */
	Advice EMPTY_ADVICE = new Advice() {};


	/**
	 * 返回此切面中的通知部分。通知可以是拦截器、前置通知、抛出通知等。
	 * @return 如果切点匹配则应应用的通知
	 * @see org.aopalliance.intercept.MethodInterceptor
	 * @see BeforeAdvice
	 * @see ThrowsAdvice
	 * @see AfterReturningAdvice
	 */
	Advice getAdvice();

	/**
	 * 返回此通知是与特定实例关联（例如创建 mixin），还是与从同一 Spring Bean 工厂获取的被通知类的所有
	 * 实例共享。
	 * <p><b>请注意，此方法目前未被框架使用。</b>典型的 Advisor 实现始终返回 {@code true}。请使用单例/
	 * 原型 Bean 定义或适当的编程式代理创建，以确保 Advisor 具有正确的生命周期模型。
	 * @return 该通知是否与特定的目标实例相关联
	 */
	boolean isPerInstance();

}
