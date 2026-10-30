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

import org.springframework.test.util.ExceptionCollector;

/**
 * 允许对已执行请求的结果应用操作，例如断言。
 *
 * <p>请参见 {@link org.springframework.test.web.servlet.result.MockMvcResultMatchers} 和
 * {@link org.springframework.test.web.servlet.result.MockMvcResultHandlers} 中的静态工厂方法。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Michał Rowicki
 * @since 3.2
 */
public interface ResultActions {

	/**
	 * 执行一个预期。
	 *
	 * <h4>示例</h4>
	 * <p>您可以多次调用 {@code andExpect()}，如以下
	 * 示例所示。
	 * <pre class="code">
	 * // static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/person/1"))
	 * .andExpect(status().isOk())
	 * .andExpect(content().contentType(MediaType.APPLICATION_JSON))
	 * .andExpect(jsonPath("$.person.name").value("Jason"));
	 * </pre>
	 * @see #andExpectAll(ResultMatcher...)
	 */
	ResultActions andExpect(ResultMatcher matcher) throws Exception;

	/**
	 * 执行多个预期，并保证即使一个或多个预期因异常失败，
	 * 所有预期仍会被断言。
	 * <p>如果抛出单个 {@link Error} 或 {@link Exception}，它将被
	 * 重新抛出。
	 * <p>如果抛出多个异常，此方法将抛出
	 * {@link AssertionError}，其错误消息是所有异常的摘要。
	 * 此外，每个异常都会作为
	 * {@linkplain Throwable#addSuppressed(Throwable) 抑制异常} 添加到
	 * 该 {@code AssertionError}。
	 * <p>此功能类似于 AssertJ 中的 {@code SoftAssertions} 支持
	 * 和 JUnit Jupiter 中的 {@code assertAll()} 支持。
	 *
	 * <h4>示例</h4>
	 * <p>与多次调用 {@code andExpect()} 不同，您可以调用
	 * {@code andExpectAll()}，如以下示例所示。
	 * <pre class="code">
	 * // static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/person/1"))
	 * .andExpectAll(
	 * status().isOk(),
	 * content().contentType(MediaType.APPLICATION_JSON),
	 * jsonPath("$.person.name").value("Jason")
	 * );
	 * </pre>
	 * @since 5.3.10
	 * @see #andExpect(ResultMatcher)
	 */
	default ResultActions andExpectAll(ResultMatcher... matchers) throws Exception {
		ExceptionCollector exceptionCollector = new ExceptionCollector();
		for (ResultMatcher matcher : matchers) {
			exceptionCollector.execute(() -> this.andExpect(matcher));
		}
		exceptionCollector.assertEmpty();
		return this;
	}

	/**
	 * 执行通用操作。
	 *
	 * <h4>示例</h4>
	 * <pre class="code">
	 * static imports: MockMvcRequestBuilders.*, MockMvcResultMatchers.*
	 *
	 * mockMvc.perform(get("/form")).andDo(print());
	 * </pre>
	 */
	ResultActions andDo(ResultHandler handler) throws Exception;

	/**
	 * 返回已执行请求的结果，以便直接访问结果。
	 * @return 请求的结果
	 */
	MvcResult andReturn();

}
