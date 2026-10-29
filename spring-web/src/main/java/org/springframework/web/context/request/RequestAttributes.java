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

package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

/**
 * 用于访问与请求关联的属性对象的抽象。
 * 既支持访问请求作用域属性，也支持访问会话作用域属性，并可选用“全局会话”的概念。
 *
 * <p>可为任何类型的请求/会话机制实现，特别是 Servlet 请求。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see ServletRequestAttributes
 */
public interface RequestAttributes {

	/**
	 * 表示请求作用域的常量。
	 */
	int SCOPE_REQUEST = 0;

	/**
	 * 表示会话作用域的常量。
	 * <p>如果存在这种区分，最好指本地隔离的会话；否则仅指公共会话。
	 */
	int SCOPE_SESSION = 1;


	/**
	 * 指向请求对象的标准引用名称：“request”。
	 * @see #resolveReference
	 */
	String REFERENCE_REQUEST = "request";

	/**
	 * 指向会话对象的标准引用名称：“session”。
	 * @see #resolveReference
	 */
	String REFERENCE_SESSION = "session";


	/**
	 * 返回给定名称的作用域属性的值（如果有）。
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 * @return the current attribute value, or {@code null} if not found
	 */
	@Nullable
	Object getAttribute(String name, int scope);

	/**
	 * 设置给定名称的作用域属性的值，替换现有值（如果有）。
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 * @param value the value for the attribute
	 */
	void setAttribute(String name, Object value, int scope);

	/**
	 * 移除给定名称的作用域属性（如果存在）。
	 * <p>注意，实现还应移除为指定属性注册的销毁回调（如果有）。不过在这种情况下<i>无需</i><i>执行
	 * </i>已注册的销毁回调，因为对象将由调用方销毁（如果合适）。
	 * @param name the name of the attribute
	 * @param scope the scope identifier
	 */
	void removeAttribute(String name, int scope);

	/**
	 * 获取作用域中所有属性的名称。
	 * @param scope the scope identifier
	 * @return the attribute names as String array
	 */
	String[] getAttributeNames(int scope);

	/**
	 * 注册一个回调，在给定作用域中的指定属性被销毁时执行。
	 * <p>实现应尽最大努力在适当的时机执行该回调：即在请求完成或会话终止时。如果底层运行时环境不支持
	 * 此类回调，则<i>必须忽略</i>该回调，并记录相应的警告。
	 * <p>注意，“销毁”通常对应于整个作用域的销毁，而不是应用程序显式移除了单个属性。如果属性通过此门
	 * 面的 {@link #removeAttribute(String, int)} 方法被移除，则任何已注册的销毁回调也应停用，因为假
	 * 定被移除的对象会被重用或手动销毁。
	 * <p><b>注意：</b>如果要为会话作用域注册回调对象，这些对象通常应可序列化。否则回调（甚至整个会话
	 * ）可能无法在 Web 应用重启后存活。
	 * @param name the name of the attribute to register the callback for
	 * @param callback the destruction callback to be executed
	 * @param scope the scope identifier
	 */
	void registerDestructionCallback(String name, Runnable callback, int scope);

	/**
	 * 解析给定键的上下文引用（如果有）。
	 * <p>至少应支持：键“request”对应的 HttpServletRequest 引用，以及键“session”对应的 HttpSession 引
	 * 用。
	 * @param key the contextual key
	 * @return the corresponding object, or {@code null} if none found
	 */
	@Nullable
	Object resolveReference(String key);

	/**
	 * 返回当前底层会话的 id。
	 * @return the session id as String (never {@code null})
	 */
	String getSessionId();

	/**
	 * 暴露底层会话可用的最佳互斥体：即用于对底层会话进行同步的对象。
	 * @return the session mutex to use (never {@code null})
	 */
	Object getSessionMutex();

}
