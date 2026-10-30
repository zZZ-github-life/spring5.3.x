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

package org.springframework.web;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Servlet 3.0 {@link ServletContainerInitializer}，旨在支持使用 Spring 的
 * {@link WebApplicationInitializer} SPI 对 Servlet 容器进行基于代码的配置，
 * 而不是（或可能结合）传统的基于 {@code web.xml} 的方式。
 *
 * <h2>工作机制</h2>
 * 假设 {@code spring-web} 模块的 JAR 存在于类路径上，任何符合 Servlet 3.0 的容器
 * 在容器启动期间都会加载并实例化此类，并调用其 {@link #onStartup} 方法。
 * 这是通过 JAR Services API 的 {@link ServiceLoader#load(Class)} 方法检测
 * {@code spring-web} 模块的 {@code META-INF/services/javax.servlet.ServletContainerInitializer}
 * 服务提供者配置文件来实现的。完整细节请参阅
 * <a href="https://download.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider">
 * JAR Services API 文档</a> 以及 Servlet 3.0 最终草案规范的 <em>8.2.4</em> 节。
 *
 * <h3>与 {@code web.xml} 结合使用</h3>
 * Web 应用可以选择限制 Servlet 容器在启动时进行的类路径扫描量，方式是通过
 * {@code web.xml} 中的 {@code metadata-complete} 属性（控制 Servlet 注解的扫描），
 * 或通过 {@code web.xml} 中的 {@code <absolute-ordering>} 元素（控制哪些 Web 片段
 * （即 jar）允许执行 {@code ServletContainerInitializer} 扫描）。使用此特性时，
 * 可以通过将 "spring_web" 添加到 {@code web.xml} 中的命名 Web 片段列表来启用
 * {@link SpringServletContainerInitializer}，如下所示：
 *
 * <pre class="code">
 * &lt;absolute-ordering&gt;
 *   &lt;name&gt;some_web_fragment&lt;/name&gt;
 *   &lt;name&gt;spring_web&lt;/name&gt;
 * &lt;/absolute-ordering&gt;
 * </pre>
 *
 * <h2>与 Spring {@code WebApplicationInitializer} 的关系</h2>
 * Spring 的 {@code WebApplicationInitializer} SPI 仅由一个方法组成：
 * {@link WebApplicationInitializer#onStartup(ServletContext)}。其签名有意与
 * {@link ServletContainerInitializer#onStartup(Set, ServletContext)} 非常相似：
 * 简而言之，{@code SpringServletContainerInitializer} 负责实例化任何用户定义的
 * {@code WebApplicationInitializer} 实现，并将 {@code ServletContext} 委托给它们。
 * 随后由每个 {@code WebApplicationInitializer} 负责完成初始化
 * {@code ServletContext} 的实际工作。委托的具体过程在下文
 * {@link #onStartup onStartup} 文档中有详细描述。
 *
 * <h2>一般说明</h2>
 * 通常，应将此类视为更重要、面向用户的 {@code WebApplicationInitializer} SPI 的
 * <em>支撑基础设施</em>。利用此容器初始化器也完全是<em>可选的</em>：虽然此初始化器
 * 确实会在所有 Servlet 3.0+ 运行时中被加载和调用，但是否让任何
 * {@code WebApplicationInitializer} 实现出现在类路径上仍由用户决定。如果未检测到
 * 任何 {@code WebApplicationInitializer} 类型，此容器初始化器将不产生任何效果。
 *
 * <p>请注意，使用此容器初始化器和 {@code WebApplicationInitializer} 与 Spring MVC
 * 没有任何“绑定”关系，只是这些类型随 {@code spring-web} 模块 JAR 一起发布。
 * 相反，可以将其视为通用工具，能够方便地对 {@code ServletContext} 进行基于代码的
 * 配置。换句话说，任何 Servlet、监听器或过滤器都可以在
 * {@code WebApplicationInitializer} 中注册，而不仅仅是 Spring MVC 特定的组件。
 *
 * <p>此类既不是为扩展而设计，也不打算被扩展。应将其视为内部类型，
 * 而 {@code WebApplicationInitializer} 是对外公开的 SPI。
 *
 * <h2>另请参阅</h2>
 * 参阅 {@link WebApplicationInitializer} 的 Javadoc 以获取示例和详细的使用建议。<p>
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see #onStartup(Set, ServletContext)
 * @see WebApplicationInitializer
 */
@HandlesTypes(WebApplicationInitializer.class)
public class SpringServletContainerInitializer implements ServletContainerInitializer {

	/**
	 * 将 {@code ServletContext} 委托给应用类路径上存在的任何 {@link WebApplicationInitializer}
	 * 实现。
	 * <p>由于此类声明了 @{@code HandlesTypes(WebApplicationInitializer.class)}，
	 * Servlet 3.0+ 容器会自动扫描类路径以查找 Spring {@code WebApplicationInitializer}
	 * 接口的实现，并将所有这些类型组成的集合提供给此方法的
	 * {@code webAppInitializerClasses} 参数。
	 * <p>如果在类路径上未找到任何 {@code WebApplicationInitializer} 实现，
	 * 此方法实际上是一个空操作。将发出 INFO 级别的日志消息，通知用户
	 * {@code ServletContainerInitializer} 确实已被调用，但未找到任何
	 * {@code WebApplicationInitializer} 实现。
	 * <p>假设检测到一个或多个 {@code WebApplicationInitializer} 类型，它们将被实例化
	 * （如果存在 @{@link
	 * org.springframework.core.annotation.Order @Order} 注解或实现了
	 * {@link org.springframework.core.Ordered Ordered} 接口，还会进行<em>排序</em>）。
	 * 然后将在每个实例上调用 {@link WebApplicationInitializer#onStartup(ServletContext)}
	 * 方法，委托 {@code ServletContext}，以便每个实例可以注册和配置 Servlet
	 * （如 Spring 的 {@code DispatcherServlet}）、监听器（如 Spring 的
	 * {@code ContextLoaderListener}）或任何其他 Servlet API 组件（如过滤器）。
	 * @param webAppInitializerClasses 在应用类路径上找到的所有 {@link WebApplicationInitializer} 实现
	 * @param servletContext 要初始化的 Servlet 上下文
	 * @see WebApplicationInitializer#onStartup(ServletContext)
	 * @see AnnotationAwareOrderComparator
	 */
	@Override
	public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)
			throws ServletException {

		List<WebApplicationInitializer> initializers = Collections.emptyList();

		if (webAppInitializerClasses != null) {
			initializers = new ArrayList<>(webAppInitializerClasses.size());
			for (Class<?> waiClass : webAppInitializerClasses) {
				// Be defensive: Some servlet containers provide us with invalid classes,
				// no matter what @HandlesTypes says...
				if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
						WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
					try {
						initializers.add((WebApplicationInitializer)
								ReflectionUtils.accessibleConstructor(waiClass).newInstance());
					}
					catch (Throwable ex) {
						throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
					}
				}
			}
		}

		if (initializers.isEmpty()) {
			servletContext.log("No Spring WebApplicationInitializer types detected on classpath");
			return;
		}

		servletContext.log(initializers.size() + " Spring WebApplicationInitializers detected on classpath");
		AnnotationAwareOrderComparator.sort(initializers);
		for (WebApplicationInitializer initializer : initializers) {
			initializer.onStartup(servletContext);
		}
	}

}
