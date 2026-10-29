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

package org.springframework.test.web.client.match;

import java.io.IOException;
import java.text.ParseException;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.client.RequestMatcher;

/**
 * 使用 <a href="https://github.com/jayway/JsonPath">JsonPath</a> 表达式
 * 对请求内容进行断言的工厂。
 *
 * <p>通常通过
 * {@link MockRestRequestMatchers#jsonPath(String, Matcher)} 或
 * {@link MockRestRequestMatchers#jsonPath(String, Object...)} 访问此类的实例。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.2
 */
public class JsonPathRequestMatchers {

	private final JsonPathExpectationsHelper jsonPathHelper;


	/**
	 * 受保护的构造函数。
	 * <p>请使用 {@link MockRestRequestMatchers#jsonPath(String, Matcher)} 或
	 * {@link MockRestRequestMatchers#jsonPath(String, Object...)}。
	 * @param expression the {@link JsonPath} expression; never {@code null} or empty
	 * @param args arguments to parameterize the {@code JsonPath} expression with,
	 * using formatting specifiers defined in {@link String#format(String, Object...)}
	 */
	protected JsonPathRequestMatchers(String expression, Object ... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}


	/**
	 * 对请求内容求值 JSON 路径表达式，
	 * 并使用给定的 Hamcrest {@link Matcher} 断言结果值。
	 */
	public <T> RequestMatcher value(Matcher<? super T> matcher) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), matcher);
			}
		};
	}

	/**
	 * 对 {@link #value(Matcher)} 的重载变体，还接受结果值的目标类型，
	 * 使匹配器能够可靠地针对该类型工作。
	 * <p>这对于可靠地匹配数字很有用 &mdash; 例如，
	 * 将整数强制转换为 double。
	 * @since 4.3.3
	 */
	public <T> RequestMatcher value(Matcher<? super T> matcher, Class<T> targetType) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				String body = request.getBodyAsString();
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(body, matcher, targetType);
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，并断言结果等于提供的值。
	 */
	public RequestMatcher value(Object expectedValue) {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValue(request.getBodyAsString(), expectedValue);
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，
	 * 并断言给定路径处存在非 null 值。
	 * <p>如果 JSON 路径表达式不是 {@linkplain JsonPath#isDefinite
	 * 确定的}，此方法断言给定路径处的值不为
	 * <em>空</em>。
	 */
	public RequestMatcher exists() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.exists(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，
	 * 并断言给定路径处不存在值。
	 * <p>如果 JSON 路径表达式不是 {@linkplain JsonPath#isDefinite
	 * 确定的}，此方法断言给定路径处的值
	 * <em>为空</em>。
	 */
	public RequestMatcher doesNotExist() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.doesNotExist(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对响应内容求值 JSON 路径表达式，
	 * 并断言存在值（可能为 {@code null}）。
	 * <p>如果 JSON 路径表达式不是
	 * {@linkplain JsonPath#isDefinite() 确定的}，此方法断言
	 * 给定路径处的值列表不为 <em>空</em>。
	 * @since 5.0.3
	 * @see #exists()
	 * @see #isNotEmpty()
	 */
	public RequestMatcher hasJsonPath() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) {
				JsonPathRequestMatchers.this.jsonPathHelper.hasJsonPath(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对提供的 {@code content} 求值 JSON 路径表达式，
	 * 并断言给定路径处不存在值（包括 {@code null} 值）。
	 * <p>如果 JSON 路径表达式不是
	 * {@linkplain JsonPath#isDefinite() 确定的}，此方法断言
	 * 给定路径处的值列表 <em>为空</em>。
	 * @since 5.0.3
	 * @see #doesNotExist()
	 * @see #isEmpty()
	 */
	public RequestMatcher doesNotHaveJsonPath() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) {
				JsonPathRequestMatchers.this.jsonPathHelper.doesNotHaveJsonPath(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，
	 * 并断言给定路径处存在空值。
	 * <p>有关 <em>空</em> 的语义，请参阅
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)} 的 Javadoc。
	 * @since 4.2.1
	 * @see #isNotEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public RequestMatcher isEmpty() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsEmpty(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，
	 * 并断言给定路径处存在非空值。
	 * <p>有关 <em>空</em> 的语义，请参阅
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)} 的 Javadoc。
	 * @since 4.2.1
	 * @see #isEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public RequestMatcher isNotEmpty() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNotEmpty(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，并断言结果为 {@link String}。
	 * @since 4.2.1
	 */
	public RequestMatcher isString() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsString(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，并断言结果为 {@link Boolean}。
	 * @since 4.2.1
	 */
	public RequestMatcher isBoolean() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsBoolean(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，并断言结果为 {@link Number}。
	 * @since 4.2.1
	 */
	public RequestMatcher isNumber() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsNumber(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，并断言结果为数组。
	 */
	public RequestMatcher isArray() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			protected void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsArray(request.getBodyAsString());
			}
		};
	}

	/**
	 * 对请求内容求值 JSON 路径表达式，并断言结果为 {@link java.util.Map}。
	 * @since 4.2.1
	 */
	public RequestMatcher isMap() {
		return new AbstractJsonPathRequestMatcher() {
			@Override
			public void matchInternal(MockClientHttpRequest request) throws IOException, ParseException {
				JsonPathRequestMatchers.this.jsonPathHelper.assertValueIsMap(request.getBodyAsString());
			}
		};
	}


	/**
	 * 基于 {@code JsonPath} 的 {@link RequestMatcher RequestMatchers} 的抽象基类。
	 * @see #matchInternal
	 */
	private abstract static class AbstractJsonPathRequestMatcher implements RequestMatcher {

		@Override
		public final void match(ClientHttpRequest request) throws IOException, AssertionError {
			try {
				MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
				matchInternal(mockRequest);
			}
			catch (ParseException ex) {
				throw new AssertionError("Failed to parse JSON request content", ex);
			}
		}

		abstract void matchInternal(MockClientHttpRequest request) throws IOException, ParseException;
	}

}
