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

package org.springframework.test.web.servlet.setup;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.web.context.WebApplicationContext;

/**
 * 要导入以访问所有可用 {@link MockMvcBuilder MockMvcBuilders} 的主类。
 *
 * <h3>Eclipse 用户</h3>
 * <p>建议将此添加为 Java 编辑器收藏项。要导航到此设置，
 * 请打开 Preferences 并输入 “favorites”。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 * @see #webAppContextSetup(WebApplicationContext)
 * @see #standaloneSetup(Object...)
 */
public final class MockMvcBuilders {

	private MockMvcBuilders() {
	}


	/**
	 * 使用给定的、已完全初始化（即已<em>刷新</em>）的
	 * {@link WebApplicationContext} 构建 {@link MockMvc} 实例。
	 * <p>{@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * 将使用该上下文来发现其中的 Spring MVC 基础设施和应用控制器。
	 * 该上下文必须已配置 {@link javax.servlet.ServletContext ServletContext}。
	 */
	public static DefaultMockMvcBuilder webAppContextSetup(WebApplicationContext context) {
		return new DefaultMockMvcBuilder(context);
	}

	/**
	 * 通过注册一个或多个 {@code @Controller} 实例并以编程方式
	 * 配置 Spring MVC 基础设施来构建 {@link MockMvc} 实例。
	 * <p>这允许完全控制控制器及其依赖项的实例化和初始化，
	 * 类似于普通单元测试，同时还使得可以一次测试一个控制器。
	 * <p>使用此构建器时，将自动创建
	 * {@link org.springframework.web.servlet.DispatcherServlet DispatcherServlet}
	 * 以注解控制器处理请求所需的最低基础设施，并且可以对其进行自定义，
	 * 从而产生与 MVC Java 配置等效的配置，只是使用构建器风格的方法。
	 * <p>如果应用的 Spring MVC 配置相对简单 &mdash; 例如，在
	 * XML 中使用 MVC 命名空间或使用 MVC Java 配置 &mdash; 那么使用此构建器
	 * 可能是测试大多数控制器的一个不错选择。在这种情况下，可以使用
	 * 更少的测试来专注于测试和验证实际的 Spring MVC 配置。
	 * @param controllers one or more {@code @Controller} instances to test
	 * (specified {@code Class} will be turned into instance)
	 */
	public static StandaloneMockMvcBuilder standaloneSetup(Object... controllers) {
		return new StandaloneMockMvcBuilder(controllers);
	}

}
