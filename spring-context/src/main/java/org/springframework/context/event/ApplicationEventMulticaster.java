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

package org.springframework.context.event;

import java.util.function.Predicate;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * 由能够管理若干 {@link ApplicationListener} 对象并向其发布事件的对象实现的接口。
 *
 * <p>一个 {@link org.springframework.context.ApplicationEventPublisher}（通常是 Spring 的
 * {@link org.springframework.context.ApplicationContext}）可以将
 * {@code ApplicationEventMulticaster} 用作实际发布事件的委托。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see ApplicationListener
 */
public interface ApplicationEventMulticaster {

	/**
	 * 添加一个监听器，以便在所有事件发生时收到通知。
	 * @param listener 要添加的监听器
	 * @see #removeApplicationListener(ApplicationListener)
	 * @see #removeApplicationListeners(Predicate)
	 */
	void addApplicationListener(ApplicationListener<?> listener);

	/**
	 * 添加一个监听器 Bean，以便在所有事件发生时收到通知。
	 * @param listenerBeanName 要添加的监听器 Bean 的名称
	 * @see #removeApplicationListenerBean(String)
	 * @see #removeApplicationListenerBeans(Predicate)
	 */
	void addApplicationListenerBean(String listenerBeanName);

	/**
	 * 从通知列表中移除一个监听器。
	 * @param listener 要移除的监听器
	 * @see #addApplicationListener(ApplicationListener)
	 * @see #removeApplicationListeners(Predicate)
	 */
	void removeApplicationListener(ApplicationListener<?> listener);

	/**
	 * 从通知列表中移除一个监听器 Bean。
	 * @param listenerBeanName 要移除的监听器 Bean 的名称
	 * @see #addApplicationListenerBean(String)
	 * @see #removeApplicationListenerBeans(Predicate)
	 */
	void removeApplicationListenerBean(String listenerBeanName);

	/**
	 * 从已注册的 {@code ApplicationListener} 实例集合中移除所有匹配的监听器（其中包括适配器类，如
	 * {@link ApplicationListenerMethodAdapter}，例如用于带注解的 {@link EventListener} 方法）。<p>注意：
	 * 这只适用于实例注册，不适用于按 Bean 名称注册的监听器。
	 * @param predicate 用于识别要移除的监听器实例的 predicate，
	 * 例如检查 {@link SmartApplicationListener#getListenerId()}
	 * @since 5.3.5
	 * @see #addApplicationListener(ApplicationListener)
	 * @see #removeApplicationListener(ApplicationListener)
	 */
	void removeApplicationListeners(Predicate<ApplicationListener<?>> predicate);

	/**
	 * 从已注册的监听器 Bean 名称集合中移除所有匹配的监听器 Bean（这些名称指向的 Bean 类直接实现了
	 * {@link ApplicationListener} 接口）。<p>注意：这只适用于 Bean 名称注册，不适用于以编程方式注册的
	 * {@code ApplicationListener} 实例。
	 * @param predicate 用于识别要移除的监听器 Bean 名称的 predicate
	 * @since 5.3.5
	 * @see #addApplicationListenerBean(String)
	 * @see #removeApplicationListenerBean(String)
	 */
	void removeApplicationListenerBeans(Predicate<String> predicate);

	/**
	 * 移除此多播器中注册的所有监听器。<p>调用移除方法后，多播器在事件通知时将不执行任何操作，直到注册
	 * 新的监听器。
	 * @see #removeApplicationListeners(Predicate)
	 */
	void removeAllListeners();

	/**
	 * 将给定的应用事件多播给适当的监听器。<p>如有可能，请考虑使用
	 * {@link #multicastEvent(ApplicationEvent, ResolvableType)}，因为它对基于泛型的事件提供更好的支持。
	 * @param event 要广播的事件
	 */
	void multicastEvent(ApplicationEvent event);

	/**
	 * 将给定的应用事件多播给适当的监听器。<p>如果 {@code eventType} 为 {@code null}，则会基于
	 * {@code event} 实例构建默认类型。
	 * @param event 要广播的事件
	 * @param eventType 事件类型（可以为 {@code null}）
	 * @since 4.2
	 */
	void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType);

}
