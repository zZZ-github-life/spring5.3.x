/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.web.servlet;

/**
 * {@code ResultMatcher} 将已执行请求的结果与
 * 某种预期进行匹配。
 *
 * <p>请参见
 * {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers
 * MockMvcResultMatchers} 中的静态工厂方法。
 *
 * <h3>使用状态和内容结果匹配器的示例</h3>
 *
 * <pre class="code">
 * import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
 * import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 * import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;
 *
 * // ...
 *
 * WebApplicationContext wac = ...;
 *
 * MockMvc mockMvc = webAppContextSetup(wac).build();
 *
 * mockMvc.perform(get("/form"))
 * .andExpectAll(
 * status().isOk(),
 * content().mimeType(MediaType.APPLICATION_JSON));
 * </pre>
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
@FunctionalInterface
public interface ResultMatcher {

	/**
	 * 断言已执行请求的结果。
	 * @param result the result of the executed request
	 * @throws Exception if a failure occurs
	 */
	void match(MvcResult result) throws Exception;


	/**
	 * 用于与结果匹配器数组进行匹配的静态方法。
	 * @param matchers the matchers
	 * @since 5.1
	 * @deprecated as of Spring Framework 5.3.10, in favor of
	 * {@link ResultActions#andExpectAll(ResultMatcher...)}
	 */
	@Deprecated
	static ResultMatcher matchAll(ResultMatcher... matchers) {
		return result -> {
			for (ResultMatcher matcher : matchers) {
				matcher.match(result);
			}
		};
	}

}
