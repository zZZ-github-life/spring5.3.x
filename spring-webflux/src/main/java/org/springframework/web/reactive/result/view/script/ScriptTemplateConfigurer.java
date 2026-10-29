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

package org.springframework.web.reactive.result.view.script;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.springframework.lang.Nullable;

/**
 * Spring WebFlux {@link ScriptTemplateConfig} 的实现，用于创建在 Web 应用程序中
 * 使用的 {@code ScriptEngine}。
 *
 * <pre class="code">
 * // Add the following to an &#64;Configuration class
 * &#64;Bean
 * public ScriptTemplateConfigurer mustacheConfigurer() {
 *    ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
 *    configurer.setEngineName("nashorn");
 *    configurer.setScripts("mustache.js");
 *    configurer.setRenderObject("Mustache");
 *    configurer.setRenderFunction("render");
 *    return configurer;
 * }
 * </pre>
 *
 * <p><b>注意：</b>通过将 {@link #setSharedEngine sharedEngine} 属性设置为
 * {@code false}，可以将非线程安全的脚本引擎与并非为并发而设计的模板库（例如在
 * Nashorn 上运行的 Handlebars 或 React）一起使用。
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see ScriptTemplateView
 */
public class ScriptTemplateConfigurer implements ScriptTemplateConfig {

	@Nullable
	private ScriptEngine engine;

	@Nullable
	private Supplier<ScriptEngine> engineSupplier;

	@Nullable
	private String engineName;

	@Nullable
	private Boolean sharedEngine;

	@Nullable
	private String[] scripts;

	@Nullable
	private String renderObject;

	@Nullable
	private String renderFunction;

	@Nullable
	private Charset charset;

	@Nullable
	private String resourceLoaderPath;


	/**
	 * 默认构造函数。
	 */
	public ScriptTemplateConfigurer() {
	}

	/**
	 * 使用给定的引擎名称创建一个新的 ScriptTemplateConfigurer。
	 */
	public ScriptTemplateConfigurer(String engineName) {
		this.engineName = engineName;
	}


	/**
	 * 设置视图要使用的 {@link ScriptEngine}。
	 * 如果指定了 {@code renderFunction}，则脚本引擎必须实现 {@code Invocable}。
	 * 必须定义 {@code engine} 或 {@code engineName}，不能同时定义两者。
	 * <p>当 {@code sharedEngine} 标志设置为 {@code false} 时，不应通过此 setter
	 * 指定脚本引擎，而应使用 {@link #setEngineName(String)}
	 * 或 {@link #setEngineSupplier(Supplier)}（因为这意味着需要对脚本引擎进行
	 * 多次延迟实例化）。
	 * @see #setEngineName(String)
	 * @see #setEngineSupplier(Supplier)
	 */
	public void setEngine(@Nullable ScriptEngine engine) {
		this.engine = engine;
	}

	@Override
	@Nullable
	public ScriptEngine getEngine() {
		return this.engine;
	}

	/**
	 * 设置视图要使用的 {@link ScriptEngine} 提供者，通常与设置为 {@code false} 的
	 * {@link #setSharedEngine(Boolean)} 一起使用。
	 * 如果指定了 {@code renderFunction}，则脚本引擎必须实现 {@code Invocable}。
	 * 必须定义 {@code engineSupplier}、{@code engine} 或 {@code engineName} 之一。
	 * @since 5.2
	 * @see #setEngine(ScriptEngine)
	 * @see #setEngineName(String)
	 */
	public void setEngineSupplier(@Nullable Supplier<ScriptEngine> engineSupplier) {
		this.engineSupplier = engineSupplier;
	}

	@Override
	@Nullable
	public Supplier<ScriptEngine> getEngineSupplier() {
		return this.engineSupplier;
	}

	/**
	 * 设置用于实例化 {@link ScriptEngine} 的引擎名称。
	 * 如果指定了 {@code renderFunction}，则脚本引擎必须实现 {@code Invocable}。
	 * 必须定义 {@code engine} 或 {@code engineName}，不能同时定义两者。
	 * @see #setEngine(ScriptEngine)
	 * @see #setEngineSupplier(Supplier)
	 */
	public void setEngineName(@Nullable String engineName) {
		this.engineName = engineName;
	}

	@Override
	@Nullable
	public String getEngineName() {
		return this.engineName;
	}

	/**
	 * 当设置为 {@code false} 时，将为每个请求创建一个新的 {@link ScriptEngine} 实例，
	 * 否则将重用同一个实例。
	 * 对于将非线程安全的脚本引擎与并非为并发而设计的模板库（例如在 Nashorn 上运行的
	 * Handlebars 或 React）一起使用的场景，此标志应设置为 {@code false}。
	 * <p>当此标志设置为 {@code false} 时，必须使用 {@link #setEngineName(String)}
	 * 指定脚本引擎。不能使用 {@link #setEngine(ScriptEngine)}，因为需要为每个请求
	 * 创建多个脚本引擎实例。
	 * @see <a href="https://docs.oracle.com/javase/8/docs/api/javax/script/ScriptEngineFactory.html#getParameter-java.lang.String-">THREADING ScriptEngine parameter</a>
	 */
	public void setSharedEngine(@Nullable Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	@Override
	@Nullable
	public Boolean isSharedEngine() {
		return this.sharedEngine;
	}

	/**
	 * 设置要由脚本引擎加载的脚本（库脚本或用户提供的脚本）。
	 * 由于 {@code resourceLoaderPath} 的默认值为 "classpath:"，你可以轻松加载
	 * 类路径上可用的任何脚本。
	 * <p>例如，为了使用作为 WebJars 依赖提供的 JavaScript 库以及自定义的
	 * "render.js" 文件，应调用
	 * {@code configurer.setScripts("/META-INF/resources/webjars/library/version/library.js",
	 * "com/myproject/script/render.js");}。
	 * @see #setResourceLoaderPath
	 * @see <a href="https://www.webjars.org">WebJars</a>
	 */
	public void setScripts(@Nullable String... scriptNames) {
		this.scripts = scriptNames;
	}

	@Override
	@Nullable
	public String[] getScripts() {
		return this.scripts;
	}

	/**
	 * 设置渲染函数所属的对象（可选）。
	 * 例如，为了调用 {@code Mustache.render()}，应将 {@code renderObject}
	 * 设置为 {@code "Mustache"}，并将 {@code renderFunction} 设置为 {@code "render"}。
	 */
	public void setRenderObject(@Nullable String renderObject) {
		this.renderObject = renderObject;
	}

	@Override
	@Nullable
	public String getRenderObject() {
		return this.renderObject;
	}

	/**
	 * 设置渲染函数名称（可选）。如果未指定，脚本模板将通过
	 * {@link ScriptEngine#eval(String, Bindings)} 求值。
	 * <p>将使用以下参数调用此函数：
	 * <ol>
	 * <li>{@code String template}：模板内容</li>
	 * <li>{@code Map model}：视图模型</li>
	 * <li>{@code RenderingContext context}：渲染上下文</li>
	 * </ol>
	 * @see RenderingContext
	 */
	public void setRenderFunction(@Nullable String renderFunction) {
		this.renderFunction = renderFunction;
	}

	@Override
	@Nullable
	public String getRenderFunction() {
		return this.renderFunction;
	}

	/**
	 * 设置用于读取脚本和模板文件的字符集。
	 * （默认 {@code UTF-8}）。
	 */
	public void setCharset(@Nullable Charset charset) {
		this.charset = charset;
	}

	@Override
	@Nullable
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * 通过 Spring 资源位置设置资源加载器路径。
	 * 接受以逗号分隔的路径列表形式指定的多个位置。
	 * 支持 Spring {@link org.springframework.core.io.ResourceLoader} 所理解的
	 * "file:" 和 "classpath:" 等标准 URL 以及伪 URL。
	 * 在 ApplicationContext 中运行时允许使用相对路径。
	 * <p>默认值为 "classpath:"。
	 */
	public void setResourceLoaderPath(@Nullable String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	@Override
	@Nullable
	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}

}
