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
	 * @return the script source locator
	 * @see org.springframework.scripting.support.ScriptFactoryPostProcessor#convertToScriptSource
	 * @see org.springframework.core.io.ResourceLoader
	 */
	String getScriptSourceLocator();

	/**
	 * 返回脚本应当实现的业务接口。
	 * <p>如果脚本本身确定了其 Java 接口（例如 Groovy 的情况），则可以返回 {@code null}。
	 * @return the interfaces for the script
	 */
	@Nullable
	Class<?>[] getScriptInterfaces();

	/**
	 * 返回脚本是否需要为其生成配置接口。通常，对于自身不确定 Java 签名、且未在
	 * {@code getScriptInterfaces()} 中指定合适配置接口的脚本，情况便是如此。
	 * @return whether the script requires a generated config interface
	 * @see #getScriptInterfaces()
	 */
	boolean requiresConfigInterface();

	/**
	 * 用于创建脚本化 Java 对象的工厂方法。
	 * <p>鼓励实现缓存脚本元数据，例如生成的脚本类。请注意，此方法可能会被并发调用，必须以线程安全的方
	 * 式实现。
	 * @param scriptSource the actual ScriptSource to retrieve
	 * the script source text from (never {@code null})
	 * @param actualInterfaces the actual interfaces to expose,
	 * including script interfaces as well as a generated config interface
	 * (if applicable; may be {@code null})
	 * @return the scripted Java object
	 * @throws IOException if script retrieval failed
	 * @throws ScriptCompilationException if script compilation failed
	 */
	@Nullable
	Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException;

	/**
	 * 确定脚本化 Java 对象的类型。
	 * <p>鼓励实现缓存脚本元数据，例如生成的脚本类。请注意，此方法可能会被并发调用，必须以线程安全的方
	 * 式实现。
	 * @param scriptSource the actual ScriptSource to retrieve
	 * the script source text from (never {@code null})
	 * @return the type of the scripted Java object, or {@code null}
	 * if none could be determined
	 * @throws IOException if script retrieval failed
	 * @throws ScriptCompilationException if script compilation failed
	 * @since 2.0.3
	 */
	@Nullable
	Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException;

	/**
	 * 确定是否需要刷新（例如通过 ScriptSource 的 {@code isModified()} 方法）。
	 * @param scriptSource the actual ScriptSource to retrieve
	 * the script source text from (never {@code null})
	 * @return whether a fresh {@link #getScriptedObject} call is required
	 * @since 2.5.2
	 * @see ScriptSource#isModified()
	 */
	boolean requiresScriptedObjectRefresh(ScriptSource scriptSource);

}
