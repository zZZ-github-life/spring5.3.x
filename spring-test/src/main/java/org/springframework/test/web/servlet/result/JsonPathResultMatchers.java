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

package org.springframework.test.web.servlet.result;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringStartsWith;

import org.springframework.lang.Nullable;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.StringUtils;

/**
 * 使用 <a href="https://github.com/jayway/JsonPath">JsonPath</a> 表达式
 * 对响应内容进行断言的工厂。
 *
 * <p>此类的实例通常通过
 * {@link MockMvcResultMatchers#jsonPath(String, Object...)} 访问。
 * @author Rossen Stoyanchev
 * @author Craig Andrews
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 3.2
 */
public class JsonPathResultMatchers {

	private final JsonPathExpectationsHelper jsonPathHelper;

	@Nullable
	private String prefix;


	/**
	 * 受保护的构造函数。
	 * <p>请使用 {@link MockMvcResultMatchers#jsonPath(String, Object...)} 或
	 * {@link MockMvcResultMatchers#jsonPath(String, Matcher)}。
	 * @param expression {@link JsonPath} 表达式；永不为 {@code null} 或空
	 * @param args 用于对 {@code JsonPath} 表达式进行参数化的参数，
	 * 使用 {@link String#format(String, Object...)} 中定义的格式说明符
	 */
	protected JsonPathResultMatchers(String expression, Object... args) {
		this.jsonPathHelper = new JsonPathExpectationsHelper(expression, args);
	}

	/**
	 * 配置当前 {@code JsonPathResultMatchers} 实例，
	 * 以验证 JSON 载荷带有给定的前缀。
	 * <p>如果 JSON 载荷带有前缀以避免跨站脚本包含（XSSI）攻击，
	 * 请使用此方法。
	 * @param prefix 前置到实际 JSON 载荷的字符串前缀
	 * @since 4.3
	 */
	public JsonPathResultMatchers prefix(String prefix) {
		this.prefix = prefix;
		return this;
	}


	/**
	 * 针对响应内容求值 JSON 路径表达式，
	 * 并使用给定的 Hamcrest {@link Matcher} 断言结果值。
	 * @see #value(Matcher, Class)
	 * @see #value(Object)
	 */
	public <T> ResultMatcher value(Matcher<? super T> matcher) {
		return result -> this.jsonPathHelper.assertValue(getContent(result), matcher);
	}

	/**
	 * {@link #value(Matcher)} 的重载变体，还接受结果值的目标类型，
	 * 以便匹配器能够可靠地对其进行处理。
	 * <p>这对于可靠地匹配数字很有用 &mdash; 例如，
	 * 将整数强制转换为 double。
	 * @since 4.3.15
	 * @see #value(Matcher)
	 * @see #value(Object)
	 */
	public <T> ResultMatcher value(Matcher<? super T> matcher, Class<T> targetType) {
		return result -> this.jsonPathHelper.assertValue(getContent(result), matcher, targetType);
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，
	 * 并断言结果等于提供的值。
	 * @see #value(Matcher)
	 * @see #value(Matcher, Class)
	 */
	public ResultMatcher value(@Nullable Object expectedValue) {
		return result -> this.jsonPathHelper.assertValue(getContent(result), expectedValue);
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，
	 * 并断言给定路径处存在非 null 值（可能是空数组或空映射）。
	 * <p>如果 JSON 路径表达式不是 {@linkplain JsonPath#isDefinite definite}，
	 * 此方法断言给定路径处的值不是<em>空</em>。
	 */
	public ResultMatcher exists() {
		return result -> this.jsonPathHelper.exists(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，
	 * 并断言给定路径处不存在非 null 值。
	 * <p>如果 JSON 路径表达式不是 {@linkplain JsonPath#isDefinite definite}，
	 * 此方法断言给定路径处的值是<em>空</em>。
	 */
	public ResultMatcher doesNotExist() {
		return result -> this.jsonPathHelper.doesNotExist(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，
	 * 并断言给定路径处存在空值。
	 * <p>关于<em>空</em>的语义，请参阅
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)} 的 Javadoc。
	 * @since 4.2.1
	 * @see #isNotEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public ResultMatcher isEmpty() {
		return result -> this.jsonPathHelper.assertValueIsEmpty(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，
	 * 并断言给定路径处存在非空值。
	 * <p>关于<em>空</em>的语义，请参阅
	 * {@link org.springframework.util.ObjectUtils#isEmpty(Object)} 的 Javadoc。
	 * @since 4.2.1
	 * @see #isEmpty()
	 * @see #exists()
	 * @see #doesNotExist()
	 */
	public ResultMatcher isNotEmpty() {
		return result -> this.jsonPathHelper.assertValueIsNotEmpty(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，
	 * 并断言存在值（可能为 {@code null}）。
	 * <p>如果 JSON 路径表达式不是
	 * {@linkplain JsonPath#isDefinite() definite}，此方法断言
	 * 给定路径处的值列表不是<em>空</em>。
	 * @since 5.0.3
	 * @see #exists()
	 * @see #isNotEmpty()
	 */
	public ResultMatcher hasJsonPath() {
		return result -> this.jsonPathHelper.hasJsonPath(getContent(result));
	}

	/**
	 * 针对提供的 {@code content} 求值 JSON 路径表达式，
	 * 并断言给定路径处不存在值（包括 {@code null} 值）。
	 * <p>如果 JSON 路径表达式不是
	 * {@linkplain JsonPath#isDefinite() definite}，此方法断言
	 * 给定路径处的值列表是<em>空</em>。
	 * @since 5.0.3
	 * @see #doesNotExist()
	 * @see #isEmpty()
	 */
	public ResultMatcher doesNotHaveJsonPath() {
		return result -> this.jsonPathHelper.doesNotHaveJsonPath(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，并断言结果为 {@link String}。
	 * @since 4.2.1
	 */
	public ResultMatcher isString() {
		return result -> this.jsonPathHelper.assertValueIsString(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，并断言结果为 {@link Boolean}。
	 * @since 4.2.1
	 */
	public ResultMatcher isBoolean() {
		return result -> this.jsonPathHelper.assertValueIsBoolean(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，并断言结果为 {@link Number}。
	 * @since 4.2.1
	 */
	public ResultMatcher isNumber() {
		return result -> this.jsonPathHelper.assertValueIsNumber(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，并断言结果为数组。
	 */
	public ResultMatcher isArray() {
		return result -> this.jsonPathHelper.assertValueIsArray(getContent(result));
	}

	/**
	 * 针对响应内容求值 JSON 路径表达式，并断言结果为 {@link java.util.Map}。
	 * @since 4.2.1
	 */
	public ResultMatcher isMap() {
		return result -> this.jsonPathHelper.assertValueIsMap(getContent(result));
	}

	private String getContent(MvcResult result) throws UnsupportedEncodingException {
		String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		if (StringUtils.hasLength(this.prefix)) {
			try {
				String reason = String.format("Expected a JSON payload prefixed with \"%s\" but found: %s",
						this.prefix, StringUtils.quote(content.substring(0, this.prefix.length())));
				MatcherAssert.assertThat(reason, content, StringStartsWith.startsWith(this.prefix));
				return content.substring(this.prefix.length());
			}
			catch (StringIndexOutOfBoundsException ex) {
				throw new AssertionError("JSON prefix \"" + this.prefix + "\" not found", ex);
			}
		}
		else {
			return content;
		}
	}

}
