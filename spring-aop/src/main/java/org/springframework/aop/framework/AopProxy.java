/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 已配置的 AOP 代理的委托接口，允许创建实际的代理对象。
 *
 * <p>开箱即用的实现可用于 JDK 动态代理和 CGLIB 代理，正如 {@link DefaultAopProxyFactory} 所应用的
 * 那样。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see DefaultAopProxyFactory
 */
public interface AopProxy {

	/**
	 * 创建一个新的代理对象。
	 * <p>使用 AopProxy 的默认类加载器（如果代理创建需要）：通常是线程上下文类加载器。
	 * @return the new proxy object (never {@code null})
	 * @see Thread#getContextClassLoader()
	 */
	Object getProxy();

	/**
	 * 创建一个新的代理对象。
	 * <p>使用给定的类加载器（如果代理创建需要）。{@code null} 会被直接向下传递，从而采用底层代理设施的
	 * 默认值，这通常不同于 AopProxy 实现的 {@link #getProxy()} 方法所选择的默认值。
	 * @param classLoader the class loader to create the proxy with
	 * (or {@code null} for the low-level proxy facility's default)
	 * @return the new proxy object (never {@code null})
	 */
	Object getProxy(@Nullable ClassLoader classLoader);

}
