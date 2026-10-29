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

package org.springframework.test.web.servlet.setup;

import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

/**
 * 用于以某种特定方式自定义 {@code ConfigurableMockMvcBuilder} 的契约，
 * 例如希望为设置 MockMvc 提供快捷方式的第三方库。
 *
 * <p>此接口的实现可以通过
 * {@link ConfigurableMockMvcBuilder#apply} 插入，此类型的实例通常
 * 通过静态方法创建，例如：
 *
 * <pre class="code">
 * import static org.example.ExampleSetup.mySetup;
 *
 * // ...
 *
 * MockMvcBuilders.webAppContextSetup(context).apply(mySetup("foo","bar")).build();
 * </pre>
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter
 */
public interface MockMvcConfigurer {

	/**
	 * 当通过 {@link ConfigurableMockMvcBuilder#apply} 添加此 {@code MockMvcConfigurer} 时立即调用。
	 * @param builder the builder for the MockMvc
	 */
	default void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
	}

	/**
	 * 当即将使用 MockMvc 构建器以及将传递给 {@code DispatcherServlet} 的
	 * Spring WebApplicationContext 创建 MockMvc 实例时调用。
	 * @param builder the builder for the MockMvc
	 * @param context the Spring configuration
	 * @return a post processor to be applied to every request performed
	 * through the {@code MockMvc} instance.
	 */
	@Nullable
	default RequestPostProcessor beforeMockMvcCreated(
			ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {

		return null;
	}

}
