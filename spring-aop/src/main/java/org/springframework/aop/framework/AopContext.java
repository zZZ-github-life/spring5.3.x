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

package org.springframework.aop.framework;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * 包含用于获取当前 AOP 调用相关信息的静态方法的类。
 *
 * <p>如果 AOP 框架配置为暴露当前代理（这不是默认设置），则可以使用 {@code currentProxy()} 方法。它
 * 返回正在使用的 AOP 代理。目标对象或通知可以用它来发起被通知的调用，就像在 EJB 中可以使用
 * {@code getEJBObject()} 一样。它们也可以用它来查找通知配置。
 *
 * <p>Spring 的 AOP 框架默认不暴露代理，因为这样做会有性能开销。
 *
 * <p>需要访问调用过程中资源的目标对象可能会使用此类中的功能。然而，在有合理替代方案时不应采用这种
 * 做法，因为它会使应用代码依赖于在 AOP 下使用，尤其是依赖于 Spring AOP 框架。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13.03.2003
 */
public final class AopContext {

	/**
	 * 与此线程关联的 AOP 代理的 ThreadLocal 持有者。除非控制该代理的配置上的 "exposeProxy" 属性已被设
	 * 置为 "true"，否则其中将包含 {@code null}。
	 * @see ProxyConfig#setExposeProxy
	 */
	private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<>("Current AOP proxy");


	private AopContext() {
	}


	/**
	 * 尝试返回当前的 AOP 代理。仅当调用方法通过 AOP 调用，并且 AOP 框架已设置为暴露代理时，此方法才可
	 * 用。否则，此方法将抛出 IllegalStateException。
	 * @return 当前的 AOP 代理（永不返回 {@code null}）
	 * @throws IllegalStateException 如果找不到代理，因为该方法是在 AOP 调用上下文之外调用的，
	 * 或者因为 AOP 框架未配置为暴露代理
	 */
	public static Object currentProxy() throws IllegalStateException {
		Object proxy = currentProxy.get();
		if (proxy == null) {
			throw new IllegalStateException(
					"Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available, and " +
							"ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation context.");
		}
		return proxy;
	}

	/**
	 * 使给定的代理可通过 {@code currentProxy()} 方法获取。
	 * <p>请注意，调用方应小心，酌情保留旧值。
	 * @param proxy 要暴露的代理（或 {@code null} 以重置它）
	 * @return 原来的代理，如果没有绑定任何代理则为 {@code null}
	 * @see #currentProxy()
	 */
	@Nullable
	static Object setCurrentProxy(@Nullable Object proxy) {
		Object old = currentProxy.get();
		if (proxy != null) {
			currentProxy.set(proxy);
		}
		else {
			currentProxy.remove();
		}
		return old;
	}

}
