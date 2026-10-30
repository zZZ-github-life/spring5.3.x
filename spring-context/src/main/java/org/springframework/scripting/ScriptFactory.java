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

package org.springframework.scripting;

import java.io.IOException;

import org.springframework.lang.Nullable;

/**
 * 脚本定义接口，封装特定脚本的配置，以及用于创建实际脚本化 Java {@code Object} 的工厂方法。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see #getScriptSourceLocator
 * @see #getScriptedObject
 */
public interface ScriptFactory {

	/**
	 * 返回指向脚本源的定位器。由实际创建脚本的后处理器解释。
	 * <p>通常支持的定位器包括 Spring 资源位置（例如 "file:C:/myScript.bsh" 或
	 * "classpath:myPackage/myScript.bsh"）以及内联脚本（"inline:myScriptText..."）。
	 * @return 脚本源定位符
	 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor#convertToScriptSource
	 * @see org.springframework.core.io.ResourceLoader
	 */
	String getScriptSourceLocator();

	/**
	 * 返回脚本应当实现的业务接口。
	 * <p>如果脚本本身确定了其 Java 接口（例如 Groovy 的情况），则可以返回 {@code null}。
	 * @return 脚本的接口
	 */
	@Nullable
	Class<?>[] getScriptInterfaces();

	/**
	 * 返回脚本是否需要为其生成配置接口。通常，对于自身不确定 Java 签名、且未在
	 * {@code getScriptInterfaces()} 中指定合适配置接口的脚本，情况便是如此。
	 * @return 脚本是否需要生成的配置接口
	 * @see #getScriptInterfaces()
	 */
	boolean requiresConfigInterface();

	/**
	 * 用于创建脚本化 Java 对象的工厂方法。
	 * <p>鼓励实现缓存脚本元数据，例如生成的脚本类。请注意，此方法可能会被并发调用，必须以线程安全的方
	 * 式实现。
	 * @param scriptSource 用于获取脚本源文本的实际 ScriptSource（永不为 {@code null}）
	 * @param actualInterfaces 要暴露的实际接口，包括脚本接口以及生成的配置接口
	 * （如果适用；可为 {@code null}）
	 * @return 脚本化的 Java 对象
	 * @throws IOException 如果脚本获取失败
	 * @throws ScriptCompilationException 如果脚本编译失败
	 */
	@Nullable
	Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException;

	/**
	 * 确定脚本化 Java 对象的类型。
	 * <p>鼓励实现缓存脚本元数据，例如生成的脚本类。请注意，此方法可能会被并发调用，必须以线程安全的方
	 * 式实现。
	 * @param scriptSource 用于获取脚本源文本的实际 ScriptSource（永不为 {@code null}）
	 * @return 脚本化 Java 对象的类型，如果无法确定则为 {@code null}
	 * @throws IOException 如果脚本获取失败
	 * @throws ScriptCompilationException 如果脚本编译失败
	 * @since 2.0.3
	 */
	@Nullable
	Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException;

	/**
	 * 确定是否需要刷新（例如通过 ScriptSource 的 {@code isModified()} 方法）。
	 * @param scriptSource 用于获取脚本源文本的实际 ScriptSource（永不为 {@code null}）
	 * @return 是否需要重新调用 {@link #getScriptedObject}
	 * @since 2.5.2
	 * @see ScriptSource#isModified()
	 */
	boolean requiresScriptedObjectRefresh(ScriptSource scriptSource);

}
