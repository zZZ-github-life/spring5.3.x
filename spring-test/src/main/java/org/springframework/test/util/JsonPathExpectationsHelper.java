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

package org.springframework.test.util;

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 用于通过 JSON 路径表达式应用断言的辅助类。
 *
 * <p>基于 <a href="https://github.com/jayway/JsonPath">JsonPath</a> 项目：要求版本 0.9+，强烈推荐 1.1+。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Craig Andrews
 * @author Sam Brannen
 * @since 3.2
 */
public class JsonPathExpectationsHelper {

	private final String expression;

	private final JsonPath jsonPath;


	/**
	 * 构造一个新的 {@code JsonPathExpectationsHelper}。
	 * @param expression {@link JsonPath} 表达式；永不为 {@code null} 或空
	 * @param args 用于为 {@code JsonPath} 表达式提供参数的参数，
	 * 使用 {@link String#format(String, Object...)} 中定义的格式说明符
	 */
	public JsonPathExpectationsHelper(String expression, Object... args) {
		Assert.hasText(expression, "expression must not be null or empty");
		this.expression = String.format(expression, args);
		this.jsonPath = JsonPath.compile(this.expression);
	}


	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并使用给定的 {@code Matcher} 断言结果值。
	 * @param content JSON 内容
	 * @param matcher 用于断言结果的匹配器
	 */
	@SuppressWarnings("unchecked")
	public <T> void assertValue(String content, Matcher<? super T> matcher) {
		T value = (T) evaluateJsonPath(content);
		MatcherAssert.assertThat("JSON path \"" + this.expression + "\"", value, matcher);
	}

	/**
	 * {@link #assertValue(String, Matcher)} 的重载变体，它还接受结果值的目标类型。例如，这可用于可靠地匹配数字，如将整数强制转换为双精度浮点数。
	 * @param content JSON 内容
	 * @param matcher 用于断言结果的匹配器
	 * @param targetType 结果值的期望类型
	 * @since 4.3.3
	 */
	@SuppressWarnings("unchecked")
	public <T> void assertValue(String content, Matcher<? super T> matcher, Class<T> targetType) {
		T value = (T) evaluateJsonPath(content, targetType);
		MatcherAssert.assertThat("JSON path \"" + this.expression + "\"", value, matcher);
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言结果等于期望值。
	 * @param content JSON 内容
	 * @param expectedValue 期望值
	 */
	public void assertValue(String content, @Nullable Object expectedValue) {
		Object actualValue = evaluateJsonPath(content);
		if ((actualValue instanceof List) && !(expectedValue instanceof List)) {
			@SuppressWarnings("rawtypes")
			List actualValueList = (List) actualValue;
			if (actualValueList.isEmpty()) {
				AssertionErrors.fail("No matching value at JSON path \"" + this.expression + "\"");
			}
			if (actualValueList.size() != 1) {
				AssertionErrors.fail("Got a list of values " + actualValue +
						" instead of the expected single value " + expectedValue);
			}
			actualValue = actualValueList.get(0);
		}
		else if (actualValue != null && expectedValue != null &&
				!actualValue.getClass().equals(expectedValue.getClass())) {
			try {
				actualValue = evaluateJsonPath(content, expectedValue.getClass());
			}
			catch (AssertionError error) {
				String message = String.format(
					"At JSON path \"%s\", value <%s> of type <%s> cannot be converted to type <%s>",
					this.expression, actualValue, ClassUtils.getDescriptiveType(actualValue),
					ClassUtils.getDescriptiveType(expectedValue));
				throw new AssertionError(message, error.getCause());
			}
		}
		AssertionErrors.assertEquals("JSON path \"" + this.expression + "\"", expectedValue, actualValue);
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言结果值是 {@link String}。
	 * @param content JSON 内容
	 * @since 4.2.1
	 */
	public void assertValueIsString(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a string", value), value, CoreMatchers.instanceOf(String.class));
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言结果值是 {@link Boolean}。
	 * @param content JSON 内容
	 * @since 4.2.1
	 */
	public void assertValueIsBoolean(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a boolean", value), value, CoreMatchers.instanceOf(Boolean.class));
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言结果值是 {@link Number}。
	 * @param content JSON 内容
	 * @since 4.2.1
	 */
	public void assertValueIsNumber(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a number", value), value, CoreMatchers.instanceOf(Number.class));
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言结果值是一个数组。
	 * @param content JSON 内容
	 */
	public void assertValueIsArray(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("an array", value), value, CoreMatchers.instanceOf(List.class));
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言结果值是 {@link Map}。
	 * @param content JSON 内容
	 * @since 4.2.1
	 */
	public void assertValueIsMap(String content) {
		Object value = assertExistsAndReturn(content);
		MatcherAssert.assertThat(failureReason("a map", value), value, CoreMatchers.instanceOf(Map.class));
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言在给定路径处存在非空值（可能是空数组或映射）。
	 * <p>请注意，如果 JSON 路径表达式不是{@linkplain JsonPath#isDefinite() 确定性的}，则此方法断言给定路径处的值列表不为<em>空</em>。
	 * @param content JSON 内容
	 */
	public void exists(String content) {
		assertExistsAndReturn(content);
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言给定路径处不存在非空值。
	 * <p>请注意，如果 JSON 路径表达式不是{@linkplain JsonPath#isDefinite() 确定性的}，则此方法断言给定路径处的值列表为<em>空</em>。
	 * @param content JSON 内容
	 */
	public void doesNotExist(String content) {
		Object value;
		try {
			value = evaluateJsonPath(content);
		}
		catch (AssertionError ex) {
			return;
		}
		String reason = failureReason("no value", value);
		if (pathIsIndefinite() && value instanceof List) {
			AssertionErrors.assertTrue(reason, ((List<?>) value).isEmpty());
		}
		else {
			AssertionErrors.assertTrue(reason, (value == null));
		}
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言在给定路径处存在空值。
	 * <p>有关<em>空</em>的语义，请查阅 {@link ObjectUtils#isEmpty(Object)} 的 Javadoc。
	 * @param content JSON 内容
	 */
	public void assertValueIsEmpty(String content) {
		Object value = evaluateJsonPath(content);
		AssertionErrors.assertTrue(failureReason("an empty value", value), ObjectUtils.isEmpty(value));
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言在给定路径处存在非空值。
	 * <p>有关<em>空</em>的语义，请查阅 {@link ObjectUtils#isEmpty(Object)} 的 Javadoc。
	 * @param content JSON 内容
	 */
	public void assertValueIsNotEmpty(String content) {
		Object value = evaluateJsonPath(content);
		AssertionErrors.assertTrue(failureReason("a non-empty value", value), !ObjectUtils.isEmpty(value));
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言存在某个值（可能为 {@code null}）。
	 * <p>如果 JSON 路径表达式不是{@linkplain JsonPath#isDefinite() 确定性的}，则此方法断言给定路径处的值列表不为<em>空</em>。
	 * @param content JSON 内容
	 * @since 5.0.3
	 */
	public void hasJsonPath(String content) {
		Object value = evaluateJsonPath(content);
		if (pathIsIndefinite() && value instanceof List) {
			String message = "No values for JSON path \"" + this.expression + "\"";
			AssertionErrors.assertTrue(message, !((List<?>) value).isEmpty());
		}
	}

	/**
	 * 针对所提供的 {@code content} 求值 JSON 路径表达式，并断言在给定路径处不存在值（包括 {@code null} 值）。
	 * <p>如果 JSON 路径表达式不是{@linkplain JsonPath#isDefinite() 确定性的}，则此方法断言给定路径处的值列表为<em>空</em>。
	 * @param content JSON 内容
	 * @since 5.0.3
	 */
	public void doesNotHaveJsonPath(String content) {
		Object value;
		try {
			value = evaluateJsonPath(content);
		}
		catch (AssertionError ex) {
			return;
		}
		if (pathIsIndefinite() && value instanceof List) {
			AssertionErrors.assertTrue(failureReason("no values", value), ((List<?>) value).isEmpty());
		}
		else {
			AssertionErrors.fail(failureReason("no value", value));
		}
	}

	private String failureReason(String expectedDescription, @Nullable Object value) {
		return String.format("Expected %s at JSON path \"%s\" but found: %s", expectedDescription, this.expression,
				ObjectUtils.nullSafeToString(StringUtils.quoteIfString(value)));
	}

	/**
	 * 求值 JSON 路径并返回结果值。
	 * @param content 要据此求值的内容
	 * @return 求值结果
	 * @throws AssertionError 如果求值失败。
	 */
	@Nullable
	public Object evaluateJsonPath(String content) {
		try {
			return this.jsonPath.read(content);
		}
		catch (Throwable ex) {
			throw new AssertionError("No value at JSON path \"" + this.expression + "\"", ex);
		}
	}

	/**
	 * {@link #evaluateJsonPath(String)} 的变体，带有目标类型。
	 * <p>例如，这可用于可靠地匹配数字，如将整数强制转换为双精度浮点数。
	 * @param content 要据此求值的内容
	 * @return 求值结果
	 * @throws AssertionError 如果求值失败。
	 */
	public Object evaluateJsonPath(String content, Class<?> targetType) {
		try {
			return JsonPath.parse(content).read(this.expression, targetType);
		}
		catch (Throwable ex) {
			String message = "No value at JSON path \"" + this.expression + "\"";
			throw new AssertionError(message, ex);
		}
	}

	@Nullable
	private Object assertExistsAndReturn(String content) {
		Object value = evaluateJsonPath(content);
		String reason = "No value at JSON path \"" + this.expression + "\"";
		AssertionErrors.assertTrue(reason, value != null);
		if (pathIsIndefinite() && value instanceof List) {
			AssertionErrors.assertTrue(reason, !((List<?>) value).isEmpty());
		}
		return value;
	}

	private boolean pathIsIndefinite() {
		return !this.jsonPath.isDefinite();
	}

}
