/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;

/**
 * 由 {@link ConfigurableBeanFactory} 使用的策略接口，表示用于容纳 Bean 实例的目标作用域。它允许在
 * BeanFactory 的标准作用域 {@link ConfigurableBeanFactory#SCOPE_SINGLETON "singleton"} 和
 * {@link ConfigurableBeanFactory#SCOPE_PROTOTYPE "prototype"} 之外，通过
 * {@link ConfigurableBeanFactory#registerScope(String, Scope) specific key} 注册自定义的更多作用域。
 *
 * <p>{@link org.springframework.context.ApplicationContext} 实现（例如
 * {@link org.springframework.web.context.WebApplicationContext}）可以基于此 Scope SPI 注册特定于其
 * 环境的额外标准作用域，例如
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST "request"} 和
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_SESSION "session"}。
 *
 * <p>即使其主要用途是 Web 环境中的扩展作用域，此 SPI 也完全通用：它提供了从任何底层存储机制（例如
 * HTTP 会话或自定义会话机制）获取和存放对象的能力。传入此类的 {@code get} 和 {@code remove} 方法的
 * 名称将标识当前作用域中的目标对象。
 *
 * <p>期望 {@code Scope} 实现是线程安全的。如有需要，一个 {@code Scope} 实例可以同时与多个 Bean 工
 * 厂一起使用（除非它明确希望感知所在的 BeanFactory），并可由任意数量的线程从任意数量的工厂并发访问
 * 该 {@code Scope}。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see ConfigurableBeanFactory#registerScope
 * @see CustomScopeConfigurer
 * @see org.springframework.aop.scope.ScopedProxyFactoryBean
 * @see org.springframework.web.context.request.RequestScope
 * @see org.springframework.web.context.request.SessionScope
 */
public interface Scope {

	/**
	 * 从底层作用域返回具有给定名称的对象；如果在底层存储机制中未找到，则
	 * {@link org.springframework.beans.factory.ObjectFactory#getObject() creating it}。
	 * <p>这是 Scope 的核心操作，也是唯一绝对必需的操作。
	 * @param name the name of the object to retrieve
	 * @param objectFactory the {@link ObjectFactory} to use to create the scoped
	 * object if it is not present in the underlying storage mechanism
	 * @return the desired object (never {@code null})
	 * @throws IllegalStateException if the underlying scope is not currently active
	 */
	Object get(String name, ObjectFactory<?> objectFactory);

	/**
	 * 从底层作用域中移除具有给定 {@code name} 的对象。
	 * <p>如果未找到对象，则返回 {@code null}；否则返回被移除的 {@code Object}。
	 * <p>请注意，实现还应移除为指定对象注册的销毁回调（如果有）。不过在这种情况下，它<i>不</i>需要<i>
	 * 执行</i>已注册的销毁回调，因为对象将由调用方销毁（如果合适）。
	 * <p><b>注意：这是一项可选操作。</b> 如果实现不支持显式移除对象，则可能抛出
	 * {@link UnsupportedOperationException}。
	 * @param name the name of the object to remove
	 * @return the removed object, or {@code null} if no object was present
	 * @throws IllegalStateException if the underlying scope is not currently active
	 * @see #registerDestructionCallback
	 */
	@Nullable
	Object remove(String name);

	/**
	 * 注册一个回调，在该作用域中指定对象销毁时（或者如果作用域不销毁单个对象而只是整体终止，则在整个作
	 * 用域销毁时）执行。
	 * <p><b>注意：这是一项可选操作。</b> 此方法仅会为具有实际销毁配置的作用域 Bean 调用
	 * （DisposableBean、destroy-method、DestructionAwareBeanPostProcessor）。实现应尽最大努力在适当的时
	 * 间执行给定回调。如果底层运行时环境完全不支持此类回调，则<i>必须忽略该回调并记录相应的警告</i>。
	 * <p>请注意，“销毁”指的是对象作为作用域自身生命周期一部分的自动销毁，而不是应用程序显式移除了某个
	 * 作用域对象。如果某个作用域对象通过此门面的 {@link #remove(String)} 方法被移除，那么任何已注册的
	 * 销毁回调也应被移除，假定被移除的对象将被重用或手动销毁。
	 * @param name the name of the object to execute the destruction callback for
	 * @param callback the destruction callback to be executed.
	 * Note that the passed-in Runnable will never throw an exception,
	 * so it can safely be executed without an enclosing try-catch block.
	 * Furthermore, the Runnable will usually be serializable, provided
	 * that its target object is serializable as well.
	 * @throws IllegalStateException if the underlying scope is not currently active
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getDestroyMethodName()
	 * @see DestructionAwareBeanPostProcessor
	 */
	void registerDestructionCallback(String name, Runnable callback);

	/**
	 * 解析给定键的上下文对象（如果有）。例如，键 "request" 对应的 HttpServletRequest 对象。
	 * @param key the contextual key
	 * @return the corresponding object, or {@code null} if none found
	 * @throws IllegalStateException if the underlying scope is not currently active
	 */
	@Nullable
	Object resolveContextualObject(String key);

	/**
	 * 返回当前底层作用域的<em>会话 ID</em>（如果有）。
	 * <p>会话 ID 的确切含义取决于底层存储机制。对于会话作用域对象，会话 ID 通常等于（或派生自）
	 * {@link javax.servlet.http.HttpSession#getId() session ID}；对于位于整体会话内的自定义会话，当前
	 * 会话的特定 ID 则是合适的。
	 * <p><b>注意：这是一项可选操作。</b> 如果底层存储机制没有明显适合此类 ID 的候选者，在此方法的实现
	 * 中返回 {@code null} 是完全有效的。
	 * @return the conversation ID, or {@code null} if there is no
	 * conversation ID for the current scope
	 * @throws IllegalStateException if the underlying scope is not currently active
	 */
	@Nullable
	String getConversationId();

}
