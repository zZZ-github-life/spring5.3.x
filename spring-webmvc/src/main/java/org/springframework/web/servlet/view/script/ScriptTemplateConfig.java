/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.view.script;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.springframework.lang.Nullable;

/**
 * 用于在 Web 环境中配置和管理 JSR-223 {@link ScriptEngine}
 * 以便自动查找的接口。由 {@link ScriptTemplateView} 检测并使用。
 *
 * @author Sebastien Deleuze
 * @since 4.2
 */
public interface ScriptTemplateConfig {

	/**
	 * 返回视图要使用的 {@link ScriptEngine}。
	 */
	@Nullable
	ScriptEngine getEngine();

	/**
	 * 返回用于实例化 {@link ScriptEngine} 的引擎提供者（supplier）。
	 * @since 5.2
	 */
	@Nullable
	Supplier<ScriptEngine> getEngineSupplier();

	/**
	 * 返回用于实例化 {@link ScriptEngine} 的引擎名称。
	 */
	@Nullable
	String getEngineName();

	/**
	 * 返回是让所有线程共享同一个引擎，
	 * 还是为每个线程创建线程本地的引擎实例。
	 */
	@Nullable
	Boolean isSharedEngine();

	/**
	 * 返回要由脚本引擎加载的脚本（库脚本或用户提供的脚本）。
	 */
	@Nullable
	String[] getScripts();

	/**
	 * 返回渲染函数所属的对象（可选）。
	 */
	@Nullable
	String getRenderObject();

	/**
	 * 返回渲染函数名称（可选）。如果未指定，
	 * 脚本模板将通过 {@link ScriptEngine#eval(String, Bindings)} 进行求值。
	 */
	@Nullable
	String getRenderFunction();

	/**
	 * 返回响应的内容类型。
	 * @since 4.2.1
	 */
	@Nullable
	String getContentType();

	/**
	 * 返回用于读取脚本和模板文件的字符集。
	 */
	@Nullable
	Charset getCharset();

	/**
	 * 通过 Spring 资源位置返回资源加载路径。
	 */
	@Nullable
	String getResourceLoaderPath();

}
