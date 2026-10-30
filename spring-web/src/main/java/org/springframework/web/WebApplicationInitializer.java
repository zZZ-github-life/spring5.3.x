/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * 在 Servlet 3.0+ 环境中实现的接口，用于以编程方式配置
 * {@link ServletContext} —— 而不是（或可能结合）传统的基于 {@code web.xml} 的方式。
 *
 * <p>此 SPI 的实现将由 {@link
 * SpringServletContainerInitializer} 自动检测，而后者本身会由任何 Servlet 3.0 容器
 * 自动引导。有关此引导机制的详细信息，请参阅
 * {@linkplain SpringServletContainerInitializer 其 Javadoc}。
 *
 * <h2>示例</h2>
 * <h3>传统的基于 XML 的方式</h3>
 * 大多数构建 Web 应用的 Spring 用户都需要注册 Spring 的 {@code
 * DispatcherServlet}。作为参考，在 WEB-INF/web.xml 中，通常如下所示：
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;dispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;
 *     org.springframework.web.servlet.DispatcherServlet
 *   &lt;/servlet-class&gt;
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;contextConfigLocation&lt;/param-name&gt;
 *     &lt;param-value&gt;/WEB-INF/spring/dispatcher-config.xml&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 *   &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;dispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <h3>使用 {@code WebApplicationInitializer} 的基于代码的方式</h3>
 * 以下是等效的 {@code DispatcherServlet} 注册逻辑，采用
 * {@code WebApplicationInitializer} 风格：
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      XmlWebApplicationContext appContext = new XmlWebApplicationContext();
 *      appContext.setConfigLocation("/WEB-INF/spring/dispatcher-config.xml");
 *
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(appContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为上述方式的替代方案，你也可以继承 {@link
 * org.springframework.web.servlet.support.AbstractDispatcherServletInitializer}。
 *
 * 如你所见，得益于 Servlet 3.0 新增的 {@link ServletContext#addServlet} 方法，
 * 我们实际上注册的是 {@code DispatcherServlet} 的一个<em>实例</em>，这意味着
 * {@code DispatcherServlet} 现在可以像任何其他对象一样对待 —— 在此例中，
 * 是通过构造函数注入其应用上下文。
 *
 * <p>这种风格既更简单又更简洁。无需担心处理 init-param 等，只需普通的
 * JavaBean 风格属性和构造函数参数。你可以在必要时自由创建和操作 Spring 应用上下文，
 * 然后将其注入 {@code DispatcherServlet}。
 *
 * <p>大多数主要的 Spring Web 组件都已更新以支持这种注册风格。你会发现
 * {@code DispatcherServlet}、{@code FrameworkServlet}、{@code ContextLoaderListener}
 * 和 {@code DelegatingFilterProxy} 现在都支持构造函数参数。即使某个组件
 * （例如非 Spring 的其他第三方组件）没有专门针对在
 * {@code WebApplicationInitializers} 中使用而更新，它们在任何情况下仍可使用。
 * Servlet 3.0 的 {@code ServletContext} API 允许以编程方式设置 init-param、
 * context-param 等。
 *
 * <h2>100% 基于代码的配置方式</h2>
 * 在上面的示例中，{@code WEB-INF/web.xml} 已成功被
 * {@code WebApplicationInitializer} 形式的代码所替代，但实际的
 * {@code dispatcher-config.xml} Spring 配置仍然是基于 XML 的。
 * {@code WebApplicationInitializer} 非常适合与 Spring 基于代码的
 * {@code @Configuration} 类一起使用。完整细节请参阅 @{@link
 * org.springframework.context.annotation.Configuration Configuration} 的 Javadoc，
 * 但以下示例展示了重构为使用 Spring 的
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * AnnotationConfigWebApplicationContext} 代替 {@code XmlWebApplicationContext}，
 * 并使用用户定义的 {@code @Configuration} 类 {@code AppConfig} 和
 * {@code DispatcherConfig} 代替 Spring XML 文件。此示例还比上面的示例更进一步，
 * 演示了“根”应用上下文的典型配置以及 {@code ContextLoaderListener} 的注册：
 * <pre class="code">
 * public class MyWebAppInitializer implements WebApplicationInitializer {
 *
 *    &#064;Override
 *    public void onStartup(ServletContext container) {
 *      // Create the 'root' Spring application context
 *      AnnotationConfigWebApplicationContext rootContext =
 *        new AnnotationConfigWebApplicationContext();
 *      rootContext.register(AppConfig.class);
 *
 *      // Manage the lifecycle of the root application context
 *      container.addListener(new ContextLoaderListener(rootContext));
 *
 *      // Create the dispatcher servlet's Spring application context
 *      AnnotationConfigWebApplicationContext dispatcherContext =
 *        new AnnotationConfigWebApplicationContext();
 *      dispatcherContext.register(DispatcherConfig.class);
 *
 *      // Register and map the dispatcher servlet
 *      ServletRegistration.Dynamic dispatcher =
 *        container.addServlet("dispatcher", new DispatcherServlet(dispatcherContext));
 *      dispatcher.setLoadOnStartup(1);
 *      dispatcher.addMapping("/");
 *    }
 *
 * }</pre>
 *
 * 作为上述方式的替代方案，你也可以继承 {@link
 * org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer}。
 *
 * 请记住，{@code WebApplicationInitializer} 实现是<em>自动检测</em>的 ——
 * 因此你可以随意将它们打包到应用中。
 *
 * <h2>对 {@code WebApplicationInitializer} 的执行排序</h2>
 * {@code WebApplicationInitializer} 实现可以选择在类级别使用 Spring 的 @{@link
 * org.springframework.core.annotation.Order Order} 注解进行标注，或者实现 Spring 的
 * {@link org.springframework.core.Ordered Ordered} 接口。若如此，初始化器将在调用前
 * 进行排序。这为用户提供了一种机制，以确保 Servlet 容器初始化的执行顺序。
 * 预计该特性很少使用，因为典型的应用可能会将所有容器初始化集中在一个
 * {@code WebApplicationInitializer} 中。
 *
 * <h2>注意事项</h2>
 *
 * <h3>web.xml 版本控制</h3>
 * <p>{@code WEB-INF/web.xml} 与 {@code WebApplicationInitializer} 的使用并不互斥；
 * 例如，web.xml 可以注册一个 Servlet，而 {@code WebApplicationInitializer} 可以注册
 * 另一个。初始化器甚至可以通过 {@link ServletContext#getServletRegistration(String)}
 * 等方法<em>修改</em>在 {@code web.xml} 中执行的注册。<strong>但是，如果应用中存在
 * {@code WEB-INF/web.xml}，其 {@code version} 属性必须设置为 "3.0" 或更高，
 * 否则 Servlet 容器将忽略 {@code ServletContainerInitializer} 引导。</strong>
 *
 * <h3>在 Tomcat 下映射到 '/'</h3>
 * <p>Apache Tomcat 将其内部的 {@code DefaultServlet} 映射到 "/"，在 Tomcat
 * 7.0.14 及更低版本上，此 Servlet 映射<em>无法以编程方式覆盖</em>。
 * 7.0.15 修复了此问题。在 GlassFish 3.1 下覆盖 "/" Servlet 映射也已成功测试。<p>
 *
 * @author Chris Beams
 * @since 3.1
 * @see SpringServletContainerInitializer
 * @see org.springframework.web.context.AbstractContextLoaderInitializer
 * @see org.springframework.web.servlet.support.AbstractDispatcherServletInitializer
 * @see org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer
 */
public interface WebApplicationInitializer {

	/**
	 * 使用初始化此 Web 应用所需的任何 Servlet、过滤器、监听器、
	 * context-param 和属性来配置给定的 {@link ServletContext}。示例请参阅
	 * {@linkplain WebApplicationInitializer 上文}。
	 * @param servletContext 要初始化的 {@code ServletContext}
	 * @throws ServletException 如果对给定的 {@code ServletContext} 的任何调用抛出 {@code ServletException}
	 */
	void onStartup(ServletContext servletContext) throws ServletException;

}
