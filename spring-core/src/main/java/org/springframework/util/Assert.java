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

package org.springframework.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;

/**
 * 用于协助校验参数的断言工具类。
 *
 * <p>有助于在运行时尽早、清晰地识别程序员错误。
 *
 * <p>例如，如果某个公共方法的约定声明其不允许 {@code null} 参数，就可以用 {@code Assert} 来校验该
 * 约定。这样做在发生约定违反时能清晰地指出问题，并保护类的不变量。
 *
 * <p>通常用于校验方法参数而非配置属性，以检查那些通常属于程序员错误而非配置错误的情况。与配置初始
 * 化代码不同，在此类方法中回退到默认值通常没有意义。
 *
 * <p>此类类似于 JUnit 的断言库。如果某个参数值被认为无效，通常会抛出
 * {@link IllegalArgumentException}。例如：
 *
 * <pre class="code">
 * Assert.notNull(clazz, "The class must not be null");
 * Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
 *
 * <p>主要用于框架内部；如需更全面的断言工具集，可考虑
 * <a href="https://commons.apache.org/proper/commons-lang/">Apache Commons Lang</a> 中的
 * {@code org.apache.commons.lang3.Validate}、Google Guava 的
 * <a href="https://github.com/google/guava/wiki/PreconditionsExplained">Preconditions</a>，或类似
 * 的第三方库。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Colin Sampaleanu
 * @author Rob Harrop
 * @since 1.1.2
 */
public abstract class Assert {

	/**
	 * 断言一个布尔表达式，如果表达式的值为 {@code false}，则抛出 {@code IllegalStateException}。
	 * <p>如果希望在断言失败时抛出 {@code IllegalArgumentException}，请调用 {@link #isTrue}。
	 * <pre class="code">Assert.state(id == null, "The id property must not already be initialized");</pre>
	 * @param expression 布尔表达式
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalStateException 如果 {@code expression} 为 {@code false}
	 */
	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式的值为 {@code false}，则抛出 {@code IllegalStateException}。
	 * <p>如果希望在断言失败时抛出 {@code IllegalArgumentException}，请调用 {@link #isTrue}。
	 * <pre class="code">
	 * Assert.state(entity.getId() == null,
	 *     () -&gt; "ID for entity " + entity.getName() + " must not already be initialized");
	 * </pre>
	 * @param expression 布尔表达式
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalStateException 如果 {@code expression} 为 {@code false}
	 * @since 5.0
	 */
	public static void state(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalStateException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式的值为 {@code false}，则抛出 {@code IllegalStateException}。
	 * @deprecated as of 4.3.7, in favor of {@link #state(boolean, String)}
	 */
	@Deprecated
	public static void state(boolean expression) {
		state(expression, "[Assertion failed] - this state invariant must be true");
	}

	/**
	 * 断言一个布尔表达式，如果表达式的值为 {@code false}，则抛出 {@code IllegalArgumentException}。
	 * <pre class="code">Assert.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
	 * @param expression 布尔表达式
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果 {@code expression} 为 {@code false}
	 */
	public static void isTrue(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式的值为 {@code false}，则抛出 {@code IllegalArgumentException}。
	 * <pre class="code">
	 * Assert.isTrue(i &gt; 0, () -&gt; "The value '" + i + "' must be greater than zero");
	 * </pre>
	 * @param expression 布尔表达式
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果 {@code expression} 为 {@code false}
	 * @since 5.0
	 */
	public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式的值为 {@code false}，则抛出 {@code IllegalArgumentException}。
	 * @deprecated as of 4.3.7, in favor of {@link #isTrue(boolean, String)}
	 */
	@Deprecated
	public static void isTrue(boolean expression) {
		isTrue(expression, "[Assertion failed] - this expression must be true");
	}

	/**
	 * 断言对象为 {@code null}。
	 * <pre class="code">Assert.isNull(value, "The value must be null");</pre>
	 * @param object 要检查的对象
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果对象不为 {@code null}
	 */
	public static void isNull(@Nullable Object object, String message) {
		if (object != null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言对象为 {@code null}。
	 * <pre class="code">
	 * Assert.isNull(value, () -&gt; "The value '" + value + "' must be null");
	 * </pre>
	 * @param object 要检查的对象
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果对象不为 {@code null}
	 * @since 5.0
	 */
	public static void isNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object != null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言对象为 {@code null}。
	 * @deprecated as of 4.3.7, in favor of {@link #isNull(Object, String)}
	 */
	@Deprecated
	public static void isNull(@Nullable Object object) {
		isNull(object, "[Assertion failed] - the object argument must be null");
	}

	/**
	 * 断言对象不为 {@code null}。
	 * <pre class="code">Assert.notNull(clazz, "The class must not be null");</pre>
	 * @param object 要检查的对象
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果对象为 {@code null}
	 */
	public static void notNull(@Nullable Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言对象不为 {@code null}。
	 * <pre class="code">
	 * Assert.notNull(entity.getId(),
	 *     () -&gt; "ID for entity " + entity.getName() + " must not be null");
	 * </pre>
	 * @param object 要检查的对象
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果对象为 {@code null}
	 * @since 5.0
	 */
	public static void notNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object == null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言对象不为 {@code null}。
	 * @deprecated as of 4.3.7, in favor of {@link #notNull(Object, String)}
	 */
	@Deprecated
	public static void notNull(@Nullable Object object) {
		notNull(object, "[Assertion failed] - this argument is required; it must not be null");
	}

	/**
	 * 断言给定 String 不为空；也就是说，它不能为 {@code null}，也不能是空 String。
	 * <pre class="code">Assert.hasLength(name, "Name must not be empty");</pre>
	 * @param text 要检查的字符串
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果文本为空
	 * @see StringUtils#hasLength
	 */
	public static void hasLength(@Nullable String text, String message) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定 String 不为空；也就是说，它不能为 {@code null}，也不能是空 String。
	 * <pre class="code">
	 * Assert.hasLength(account.getName(),
	 *     () -&gt; "Name for account '" + account.getId() + "' must not be empty");
	 * </pre>
	 * @param text 要检查的字符串
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果文本为空
	 * @since 5.0
	 * @see StringUtils#hasLength
	 */
	public static void hasLength(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定 String 不为空；也就是说，它不能为 {@code null}，也不能是空 String。
	 * @deprecated as of 4.3.7, in favor of {@link #hasLength(String, String)}
	 */
	@Deprecated
	public static void hasLength(@Nullable String text) {
		hasLength(text,
				"[Assertion failed] - this String argument must have length; it must not be null or empty");
	}

	/**
	 * 断言给定 String 包含有效的文本内容；也就是说，它不能为 {@code null}，并且必须至少包含一个非空白
	 * 字符。
	 * <pre class="code">Assert.hasText(name, "'name' must not be empty");</pre>
	 * @param text 要检查的字符串
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果文本不包含有效的文本内容
	 * @see StringUtils#hasText
	 */
	public static void hasText(@Nullable String text, String message) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定 String 包含有效的文本内容；也就是说，它不能为 {@code null}，并且必须至少包含一个非空白
	 * 字符。
	 * <pre class="code">
	 * Assert.hasText(account.getName(),
	 *     () -&gt; "Name for account '" + account.getId() + "' must not be empty");
	 * </pre>
	 * @param text 要检查的字符串
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果文本不包含有效的文本内容
	 * @since 5.0
	 * @see StringUtils#hasText
	 */
	public static void hasText(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定 String 包含有效的文本内容；也就是说，它不能为 {@code null}，并且必须至少包含一个非空白
	 * 字符。
	 * @deprecated as of 4.3.7, in favor of {@link #hasText(String, String)}
	 */
	@Deprecated
	public static void hasText(@Nullable String text) {
		hasText(text,
				"[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
	}

	/**
	 * 断言给定文本不包含给定的子字符串。
	 * <pre class="code">Assert.doesNotContain(name, "rod", "Name must not contain 'rod'");</pre>
	 * @param textToSearch 要搜索的文本
	 * @param substring 要在文本中查找的子字符串
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果文本包含该子字符串
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, String message) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定文本不包含给定的子字符串。
	 * <pre class="code">
	 * Assert.doesNotContain(name, forbidden, () -&gt; "Name must not contain '" + forbidden + "'");
	 * </pre>
	 * @param textToSearch 要搜索的文本
	 * @param substring 要在文本中查找的子字符串
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果文本包含该子字符串
	 * @since 5.0
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, Supplier<String> messageSupplier) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定文本不包含给定的子字符串。
	 * @deprecated as of 4.3.7, in favor of {@link #doesNotContain(String, String, String)}
	 */
	@Deprecated
	public static void doesNotContain(@Nullable String textToSearch, String substring) {
		doesNotContain(textToSearch, substring,
				() -> "[Assertion failed] - this String argument must not contain the substring [" + substring + "]");
	}

	/**
	 * 断言数组包含元素；也就是说，它不能为 {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">Assert.notEmpty(array, "The array must contain elements");</pre>
	 * @param array 要检查的数组
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果对象数组为 {@code null} 或不包含任何元素
	 */
	public static void notEmpty(@Nullable Object[] array, String message) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言数组包含元素；也就是说，它不能为 {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">
	 * Assert.notEmpty(array, () -&gt; "The " + arrayType + " array must contain elements");
	 * </pre>
	 * @param array 要检查的数组
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果对象数组为 {@code null} 或不包含任何元素
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言数组包含元素；也就是说，它不能为 {@code null}，并且必须至少包含一个元素。
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Object[], String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Object[] array) {
		notEmpty(array, "[Assertion failed] - this array must not be empty: it must contain at least 1 element");
	}

	/**
	 * 断言数组不包含 {@code null} 元素。
	 * <p>注意：如果数组为空，则不会报错！
	 * <pre class="code">Assert.noNullElements(array, "The array must contain non-null elements");</pre>
	 * @param array 要检查的数组
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果对象数组包含 {@code null} 元素
	 */
	public static void noNullElements(@Nullable Object[] array, String message) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * 断言数组不包含 {@code null} 元素。
	 * <p>注意：如果数组为空，则不会报错！
	 * <pre class="code">
	 * Assert.noNullElements(array, () -&gt; "The " + arrayType + " array must contain non-null elements");
	 * </pre>
	 * @param array 要检查的数组
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果对象数组包含 {@code null} 元素
	 * @since 5.0
	 */
	public static void noNullElements(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(nullSafeGet(messageSupplier));
				}
			}
		}
	}

	/**
	 * 断言数组不包含 {@code null} 元素。
	 * @deprecated as of 4.3.7, in favor of {@link #noNullElements(Object[], String)}
	 */
	@Deprecated
	public static void noNullElements(@Nullable Object[] array) {
		noNullElements(array, "[Assertion failed] - this array must not contain any null elements");
	}

	/**
	 * 断言集合包含元素；也就是说，它不能为 {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">Assert.notEmpty(collection, "Collection must contain elements");</pre>
	 * @param collection 要检查的集合
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果集合为 {@code null}
	 * 或不包含任何元素
	 */
	public static void notEmpty(@Nullable Collection<?> collection, String message) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言集合包含元素；也就是说，它不能为 {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">
	 * Assert.notEmpty(collection, () -&gt; "The " + collectionType + " collection must contain elements");
	 * </pre>
	 * @param collection 要检查的集合
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果集合为 {@code null}
	 * 或不包含任何元素
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言集合包含元素；也就是说，它不能为 {@code null}，并且必须至少包含一个元素。
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Collection, String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Collection<?> collection) {
		notEmpty(collection,
				"[Assertion failed] - this collection must not be empty: it must contain at least 1 element");
	}

	/**
	 * 断言集合不包含 {@code null} 元素。
	 * <p>注意：如果集合为空，则不会报错！
	 * <pre class="code">Assert.noNullElements(collection, "Collection must contain non-null elements");</pre>
	 * @param collection 要检查的集合
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果集合包含 {@code null} 元素
	 * @since 5.2
	 */
	public static void noNullElements(@Nullable Collection<?> collection, String message) {
		if (collection != null) {
			for (Object element : collection) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * 断言集合不包含 {@code null} 元素。
	 * <p>注意：如果集合为空，则不会报错！
	 * <pre class="code">
	 * Assert.noNullElements(collection, () -&gt; "Collection " + collectionName + " must contain non-null elements");
	 * </pre>
	 * @param collection 要检查的集合
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果集合包含 {@code null} 元素
	 * @since 5.2
	 */
	public static void noNullElements(@Nullable Collection<?> collection, Supplier<String> messageSupplier) {
		if (collection != null) {
			for (Object element : collection) {
				if (element == null) {
					throw new IllegalArgumentException(nullSafeGet(messageSupplier));
				}
			}
		}
	}

	/**
	 * 断言 Map 包含条目；也就是说，它不能为 {@code null}，并且必须至少包含一个条目。
	 * <pre class="code">Assert.notEmpty(map, "Map must contain entries");</pre>
	 * @param map 要检查的 Map
	 * @param message 断言失败时使用的异常消息
	 * @throws IllegalArgumentException 如果 Map 为 {@code null} 或不包含任何条目
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, String message) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言 Map 包含条目；也就是说，它不能为 {@code null}，并且必须至少包含一个条目。
	 * <pre class="code">
	 * Assert.notEmpty(map, () -&gt; "The " + mapType + " map must contain entries");
	 * </pre>
	 * @param map 要检查的 Map
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier
	 * @throws IllegalArgumentException 如果 Map 为 {@code null} 或不包含任何条目
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言 Map 包含条目；也就是说，它不能为 {@code null}，并且必须至少包含一个条目。
	 * @deprecated as of 4.3.7, in favor of {@link #notEmpty(Map, String)}
	 */
	@Deprecated
	public static void notEmpty(@Nullable Map<?, ?> map) {
		notEmpty(map, "[Assertion failed] - this map must not be empty; it must contain at least one entry");
	}

	/**
	 * 断言提供的对象是提供的类的实例。
	 * <pre class="code">Assert.instanceOf(Foo.class, foo, "Foo expected");</pre>
	 * @param type 要对照检查的类型
	 * @param obj 要检查的对象
	 * @param message 将被前置以提供更多上下文的消息。
	 * 如果为空，或以 ":"、";"、","、"." 结尾，
	 * 将追加完整的异常消息。如果以空格结尾，
	 * 将追加违规对象的类型名称。其他情况下，
	 * 将追加 ":"、一个空格以及违规对象的类型名称。
	 * @throws IllegalArgumentException 如果对象不是该类型的实例
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, String message) {
		notNull(type, "Type to check against must not be null");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, message);
		}
	}

	/**
	 * 断言提供的对象是提供的类的实例。
	 * <pre class="code">
	 * Assert.instanceOf(Foo.class, foo, () -&gt; "Processing " + Foo.class.getSimpleName() + ":");
	 * </pre>
	 * @param type 要对照检查的类型
	 * @param obj 要检查的对象
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier。
	 * 详见 {@link #isInstanceOf(Class, Object, String)}。
	 * @throws IllegalArgumentException 如果对象不是该类型的实例
	 * @since 5.0
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, Supplier<String> messageSupplier) {
		notNull(type, "Type to check against must not be null");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言提供的对象是提供的类的实例。
	 * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
	 * @param type 要对照检查的类型
	 * @param obj 要检查的对象
	 * @throws IllegalArgumentException 如果对象不是该类型的实例
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj) {
		isInstanceOf(type, obj, "");
	}

	/**
	 * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
	 * <pre class="code">Assert.isAssignable(Number.class, myClass, "Number expected");</pre>
	 * @param superType 要对照检查的父类型
	 * @param subType 要检查的子类型
	 * @param message 将被前置以提供更多上下文的消息。
	 * 如果为空，或以 ":"、";"、","、"." 结尾，
	 * 将追加完整的异常消息。如果以空格结尾，
	 * 将追加违规子类型的名称。其他情况下，
	 * 将追加 ":"、一个空格以及违规子类型的名称。
	 * @throws IllegalArgumentException 如果类不可赋值
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, String message) {
		notNull(superType, "Supertype to check against must not be null");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, message);
		}
	}

	/**
	 * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
	 * <pre class="code">
	 * Assert.isAssignable(Number.class, myClass, () -&gt; "Processing " + myAttributeName + ":");
	 * </pre>
	 * @param superType 要对照检查的父类型
	 * @param subType 要检查的子类型
	 * @param messageSupplier 断言失败时使用的异常消息的 Supplier。
	 * 详见 {@link #isAssignable(Class, Class, String)}。
	 * @throws IllegalArgumentException 如果类不可赋值
	 * @since 5.0
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, Supplier<String> messageSupplier) {
		notNull(superType, "Supertype to check against must not be null");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言 {@code superType.isAssignableFrom(subType)} 为 {@code true}。
	 * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
	 * @param superType 要检查的父类型
	 * @param subType 要检查的子类型
	 * @throws IllegalArgumentException 如果类不可赋值
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType) {
		isAssignable(superType, subType, "");
	}


	private static void instanceCheckFailed(Class<?> type, @Nullable Object obj, @Nullable String msg) {
		String className = (obj != null ? obj.getClass().getName() : "null");
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, className);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + ("Object of class [" + className + "] must be an instance of " + type);
		}
		throw new IllegalArgumentException(result);
	}

	private static void assignableCheckFailed(Class<?> superType, @Nullable Class<?> subType, @Nullable String msg) {
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, subType);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + (subType + " is not assignable to " + superType);
		}
		throw new IllegalArgumentException(result);
	}

	private static boolean endsWithSeparator(String msg) {
		return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
	}

	private static String messageWithTypeName(String msg, @Nullable Object typeName) {
		return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
	}

	@Nullable
	private static String nullSafeGet(@Nullable Supplier<String> messageSupplier) {
		return (messageSupplier != null ? messageSupplier.get() : null);
	}

}
