/*
 * Copyright 2002-2015 the original author or authors.
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

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * AOP Alliance {@link org.aopalliance.intercept.MethodInvocation} 接口的扩展，允许访问发起该方法调
 * 用的代理。
 *
 * <p>在必要时可以用代理替换返回值，例如当调用目标返回了自身时，这会很有用。
 *
 * @author Juergen Hoeller
 * @author Adrian Colyer
 * @since 1.1.3
 * @see org.springframework.aop.framework.ReflectiveMethodInvocation
 * @see org.springframework.aop.support.DelegatingIntroductionInterceptor
 */
public interface ProxyMethodInvocation extends MethodInvocation {

	/**
	 * 返回发起此方法调用的代理。
	 * @return the original proxy object
	 */
	Object getProxy();

	/**
	 * 创建此对象的克隆。如果在对此对象调用 {@code proceed()} 之前完成克隆，则每个克隆都可以调用一次
	 * {@code proceed()}，从而多次调用该连接点（以及通知链的其余部分）。
	 * @return an invocable clone of this invocation.
	 * {@code proceed()} can be called once per clone.
	 */
	MethodInvocation invocableClone();

	/**
	 * 创建此对象的克隆。如果在对此对象调用 {@code proceed()} 之前完成克隆，则每个克隆都可以调用一次
	 * {@code proceed()}，从而多次调用该连接点（以及通知链的其余部分）。
	 * @param arguments the arguments that the cloned invocation is supposed to use,
	 * overriding the original arguments
	 * @return an invocable clone of this invocation.
	 * {@code proceed()} can be called once per clone.
	 */
	MethodInvocation invocableClone(Object... arguments);

	/**
	 * 设置在此链中任何通知的后续调用中使用的参数。
	 * @param arguments the argument array
	 */
	void setArguments(Object... arguments);

	/**
	 * 将具有给定值的指定用户属性添加到此调用中。<p>此类属性不会在 AOP 框架本身内使用。它们仅作为调用对
	 * 象的一部分保留，供特殊拦截器使用。
	 * @param key the name of the attribute
	 * @param value the value of the attribute, or {@code null} to reset it
	 */
	void setUserAttribute(String key, @Nullable Object value);

	/**
	 * 返回指定用户属性的值。
	 * @param key the name of the attribute
	 * @return the value of the attribute, or {@code null} if not set
	 * @see #setUserAttribute
	 */
	@Nullable
	Object getUserAttribute(String key);

}
