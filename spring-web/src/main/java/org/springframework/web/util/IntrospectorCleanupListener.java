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

package org.springframework.web.util;

import java.beans.Introspector;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springframework.beans.CachedIntrospectionResults;

/**
 * 在 Web 应用关闭时刷新 JDK {@link java.beans.Introspector JavaBeans Introspector}
 * 缓存的监听器。请在 {@code web.xml} 中注册此监听器，以确保正确释放
 * Web 应用类加载器及其已加载的类。
 *
 * <p><b>如果 JavaBeans Introspector 已用于分析应用类，
 * 系统级 Introspector 缓存将持有对这些类的强引用。
 * 因此，这些类和 Web 应用类加载器在 Web 应用关闭时不会被垃圾回收！</b>
 * 此监听器会执行适当的清理，使垃圾回收能够生效。
 *
 * <p>不幸的是，清理 Introspector 的唯一方法是刷新整个缓存，
 * 因为无法具体确定其中引用了哪些应用类。这也会移除服务器中
 * 所有其他应用的缓存内省结果。
 *
 * <p>请注意，在应用内使用 Spring 的 bean 基础设施时，此监听器
 * <i>并不</i>必要，因为 Spring 自己的内省结果缓存会立即从
 * JavaBeans Introspector 缓存中刷新已分析的类，并且仅在应用自身的
 * ClassLoader 内保留缓存。
 *
 * <b>尽管 Spring 本身不会造成 JDK Introspector 泄漏，但请注意，
 * 在 Spring 框架类本身位于“公共”ClassLoader（例如系统 ClassLoader）
 * 的场景中，仍应使用此监听器。</b>在这种情况下，此监听器将正确清理
 * Spring 的内省缓存。
 *
 * <p>应用类几乎从不需要直接使用 JavaBeans Introspector，
 * 因此通常不是 Introspector 资源泄漏的原因。相反，许多库和框架
 * 没有清理 Introspector，例如 Struts 和 Quartz。
 *
 * <p>请注意，仅仅一处这样的 Introspector 泄漏就会导致整个 Web 应用
 * 类加载器无法被垃圾回收！其后果是，在 Web 应用关闭后，你仍会看到
 * 该应用的所有静态类资源（如单例）存在，而这不是这些类的过错！
 *
 * <p><b>此监听器应作为 {@code web.xml} 中的第一个监听器注册，
 * 位于任何应用监听器（如 Spring 的 ContextLoaderListener）之前。</b>
 * 这样可以使其在生命周期的正确时机充分发挥作用。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see java.beans.Introspector#flushCaches()
 * @see org.springframework.beans.CachedIntrospectionResults#acceptClassLoader
 * @see org.springframework.beans.CachedIntrospectionResults#clearClassLoader
 */
public class IntrospectorCleanupListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		CachedIntrospectionResults.acceptClassLoader(Thread.currentThread().getContextClassLoader());
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		CachedIntrospectionResults.clearClassLoader(Thread.currentThread().getContextClassLoader());
		Introspector.flushCaches();
	}

}
