/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;

/**
 * 各种对象工具方法。
 *
 * <p>主要用于框架内部。
 *
 * <p>感谢 Alex Ruiz 为本类贡献了若干改进！
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 * @since 19.03.2004
 * @see ClassUtils
 * @see CollectionUtils
 * @see StringUtils
 */
public abstract class ObjectUtils {

	private static final int INITIAL_HASH = 7;
	private static final int MULTIPLIER = 31;

	private static final String EMPTY_STRING = "";
	private static final String NULL_STRING = "null";
	private static final String ARRAY_START = "{";
	private static final String ARRAY_END = "}";
	private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
	private static final String ARRAY_ELEMENT_SEPARATOR = ", ";
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];


	/**
	 * 返回给定的 throwable 是否为受检异常：
	 * 即既不是 RuntimeException 也不是 Error。
	 * @param ex 要检查的 throwable
	 * @return 该 throwable 是否为受检异常
	 * @see java.lang.Exception
	 * @see java.lang.RuntimeException
	 * @see java.lang.Error
	 */
	public static boolean isCheckedException(Throwable ex) {
		return !(ex instanceof RuntimeException || ex instanceof Error);
	}

	/**
	 * 检查给定异常是否与指定的异常类型兼容，如 throws 子句中所声明的那样。
	 * @param ex 要检查的异常
	 * @param declaredExceptions throws 子句中声明的异常类型
	 * @return 给定异常是否兼容
	 */
	public static boolean isCompatibleWithThrowsClause(Throwable ex, @Nullable Class<?>... declaredExceptions) {
		if (!isCheckedException(ex)) {
			return true;
		}
		if (declaredExceptions != null) {
			for (Class<?> declaredException : declaredExceptions) {
				if (declaredException.isInstance(ex)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 判断给定对象是否为数组：
	 * 即 Object 数组或基本类型数组。
	 * @param obj 要检查的对象
	 */
	public static boolean isArray(@Nullable Object obj) {
		return (obj != null && obj.getClass().isArray());
	}

	/**
	 * 判断给定数组是否为空：
	 * 即 {@code null} 或长度为零。
	 * @param array 要检查的数组
	 * @see #isEmpty(Object)
	 */
	public static boolean isEmpty(@Nullable Object[] array) {
		return (array == null || array.length == 0);
	}

	/**
	 * 判断给定对象是否为空。
	 * <p>此方法支持以下对象类型。
	 * <ul>
	 * <li>{@code Optional}：如果不满足 {@link Optional#isPresent()} 则视为空</li>
	 * <li>{@code Array}：如果长度为零则视为空</li>
	 * <li>{@link CharSequence}：如果长度为零则视为空</li>
	 * <li>{@link Collection}：委托给 {@link Collection#isEmpty()}</li>
	 * <li>{@link Map}：委托给 {@link Map#isEmpty()}</li>
	 * </ul>
	 * <p>如果给定对象非 null 且不属于上述受支持的类型，此方法返回 {@code false}。
	 * @param obj 要检查的对象
	 * @return 如果对象为 {@code null} 或<em>空</em>，则返回 {@code true}
	 * @since 4.2
	 * @see Optional#isPresent()
	 * @see ObjectUtils#isEmpty(Object[])
	 * @see StringUtils#hasLength(CharSequence)
	 * @see CollectionUtils#isEmpty(java.util.Collection)
	 * @see CollectionUtils#isEmpty(java.util.Map)
	 */
	public static boolean isEmpty(@Nullable Object obj) {
		if (obj == null) {
			return true;
		}

		if (obj instanceof Optional) {
			return !((Optional<?>) obj).isPresent();
		}
		if (obj instanceof CharSequence) {
			return ((CharSequence) obj).length() == 0;
		}
		if (obj.getClass().isArray()) {
			return Array.getLength(obj) == 0;
		}
		if (obj instanceof Collection) {
			return ((Collection<?>) obj).isEmpty();
		}
		if (obj instanceof Map) {
			return ((Map<?, ?>) obj).isEmpty();
		}

		// else
		return false;
	}

	/**
	 * 解包给定对象，它可能是一个 {@link java.util.Optional}。
	 * @param obj 候选对象
	 * @return {@code Optional} 中持有的值（如果 {@code Optional} 为空则为 {@code null}），
	 * 或者就是给定的对象本身
	 * @since 5.0
	 */
	@Nullable
	public static Object unwrapOptional(@Nullable Object obj) {
		if (obj instanceof Optional) {
			Optional<?> optional = (Optional<?>) obj;
			if (!optional.isPresent()) {
				return null;
			}
			Object result = optional.get();
			Assert.isTrue(!(result instanceof Optional), "Multi-level Optional usage not supported");
			return result;
		}
		return obj;
	}

	/**
	 * 检查给定数组是否包含给定元素。
	 * @param array 要检查的数组（可为 {@code null}，
	 * 此时返回值始终为 {@code false}）
	 * @param element 要查找的元素
	 * @return 在给定数组中是否找到了该元素
	 */
	public static boolean containsElement(@Nullable Object[] array, Object element) {
		if (array == null) {
			return false;
		}
		for (Object arrayEle : array) {
			if (nullSafeEquals(arrayEle, element)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的枚举常量数组是否包含具有给定名称的常量，在确定匹配时忽略大小写。
	 * @param enumValues 要检查的枚举值，通常通过 {@code MyEnum.values()} 获得
	 * @param constant 要查找的常量名称（不能为 null 或空字符串）
	 * @return 在给定数组中是否找到了该常量
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant) {
		return containsConstant(enumValues, constant, false);
	}

	/**
	 * 检查给定的枚举常量数组是否包含具有给定名称的常量。
	 * @param enumValues 要检查的枚举值，通常通过 {@code MyEnum.values()} 获得
	 * @param constant 要查找的常量名称（不能为 null 或空字符串）
	 * @param caseSensitive 确定匹配时是否区分大小写
	 * @return 在给定数组中是否找到了该常量
	 */
	public static boolean containsConstant(Enum<?>[] enumValues, String constant, boolean caseSensitive) {
		for (Enum<?> candidate : enumValues) {
			if (caseSensitive ? candidate.toString().equals(constant) :
					candidate.toString().equalsIgnoreCase(constant)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * {@link Enum#valueOf(Class, String)} 的不区分大小写的替代方法。
	 * @param <E> 具体的 Enum 类型
	 * @param enumValues 所有相关 Enum 常量的数组，通常来自 {@code Enum.values()}
	 * @param constant 要获取其枚举值的常量
	 * @throws IllegalArgumentException 如果给定的常量在给定的枚举值数组中未找到。
	 * 可使用 {@link #containsConstant(Enum[], String)} 作为守卫来避免此异常。
	 */
	public static <E extends Enum<?>> E caseInsensitiveValueOf(E[] enumValues, String constant) {
		for (E candidate : enumValues) {
			if (candidate.toString().equalsIgnoreCase(constant)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Constant [" + constant + "] does not exist in enum type " +
				enumValues.getClass().getComponentType().getName());
	}

	/**
	 * 将给定对象追加到给定数组，返回一个新数组，
	 * 其中包含输入数组的内容加上给定对象。
	 * @param array 要追加到的数组（可为 {@code null}）
	 * @param obj 要追加的对象
	 * @return 新的数组（组件类型相同；绝不为 {@code null}）
	 */
	public static <A, O extends A> A[] addObjectToArray(@Nullable A[] array, @Nullable O obj) {
		Class<?> compType = Object.class;
		if (array != null) {
			compType = array.getClass().getComponentType();
		}
		else if (obj != null) {
			compType = obj.getClass();
		}
		int newArrLength = (array != null ? array.length + 1 : 1);
		@SuppressWarnings("unchecked")
		A[] newArr = (A[]) Array.newInstance(compType, newArrLength);
		if (array != null) {
			System.arraycopy(array, 0, newArr, 0, array.length);
		}
		newArr[newArr.length - 1] = obj;
		return newArr;
	}

	/**
	 * 将给定数组（可能是基本类型数组）转换为对象数组（如有必要，转换为基本类型包装对象）。
	 * <p>{@code null} 源值将被转换为空的 Object 数组。
	 * @param source （可能为基本类型的）数组
	 * @return 对应的对象数组（绝不为 {@code null}）
	 * @throws IllegalArgumentException 如果参数不是数组
	 */
	public static Object[] toObjectArray(@Nullable Object source) {
		if (source instanceof Object[]) {
			return (Object[]) source;
		}
		if (source == null) {
			return EMPTY_OBJECT_ARRAY;
		}
		if (!source.getClass().isArray()) {
			throw new IllegalArgumentException("Source is not an array: " + source);
		}
		int length = Array.getLength(source);
		if (length == 0) {
			return EMPTY_OBJECT_ARRAY;
		}
		Class<?> wrapperType = Array.get(source, 0).getClass();
		Object[] newArray = (Object[]) Array.newInstance(wrapperType, length);
		for (int i = 0; i < length; i++) {
			newArray[i] = Array.get(source, i);
		}
		return newArray;
	}


	//---------------------------------------------------------------------
	// Convenience methods for content-based equality/hash-code handling
	//---------------------------------------------------------------------

	/**
	 * 判断给定对象是否相等，如果两者都为 {@code null} 则返回 {@code true}，如果只有一个为 {@code null}
	 * 则返回 {@code false}。
	 * <p>使用 {@code Arrays.equals} 比较数组，基于数组元素而不是数组引用执行相等性检查。
	 * @param o1 要比较的第一个 Object
	 * @param o2 要比较的第二个 Object
	 * @return 给定对象是否相等
	 * @see Object#equals(Object)
	 * @see java.util.Arrays#equals
	 */
	public static boolean nullSafeEquals(@Nullable Object o1, @Nullable Object o2) {
		if (o1 == o2) {
			return true;
		}
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.equals(o2)) {
			return true;
		}
		if (o1.getClass().isArray() && o2.getClass().isArray()) {
			return arrayEquals(o1, o2);
		}
		return false;
	}

	/**
	 * 使用 {@code Arrays.equals} 比较给定数组，基于数组元素而不是数组引用执行相等性检查。
	 * @param o1 要比较的第一个数组
	 * @param o2 要比较的第二个数组
	 * @return 给定对象是否相等
	 * @see #nullSafeEquals(Object, Object)
	 * @see java.util.Arrays#equals
	 */
	private static boolean arrayEquals(Object o1, Object o2) {
		if (o1 instanceof Object[] && o2 instanceof Object[]) {
			return Arrays.equals((Object[]) o1, (Object[]) o2);
		}
		if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		}
		if (o1 instanceof byte[] && o2 instanceof byte[]) {
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		}
		if (o1 instanceof char[] && o2 instanceof char[]) {
			return Arrays.equals((char[]) o1, (char[]) o2);
		}
		if (o1 instanceof double[] && o2 instanceof double[]) {
			return Arrays.equals((double[]) o1, (double[]) o2);
		}
		if (o1 instanceof float[] && o2 instanceof float[]) {
			return Arrays.equals((float[]) o1, (float[]) o2);
		}
		if (o1 instanceof int[] && o2 instanceof int[]) {
			return Arrays.equals((int[]) o1, (int[]) o2);
		}
		if (o1 instanceof long[] && o2 instanceof long[]) {
			return Arrays.equals((long[]) o1, (long[]) o2);
		}
		if (o1 instanceof short[] && o2 instanceof short[]) {
			return Arrays.equals((short[]) o1, (short[]) o2);
		}
		return false;
	}

	/**
	 * 返回给定对象的哈希码；通常是 {@code Object#hashCode()}} 的值。如果对象是数组，此方法将委托给此类
	 * 中用于数组的任意 {@code nullSafeHashCode} 方法。如果对象为 {@code null}，此方法返回 0。
	 * @see Object#hashCode()
	 * @see #nullSafeHashCode(Object[])
	 * @see #nullSafeHashCode(boolean[])
	 * @see #nullSafeHashCode(byte[])
	 * @see #nullSafeHashCode(char[])
	 * @see #nullSafeHashCode(double[])
	 * @see #nullSafeHashCode(float[])
	 * @see #nullSafeHashCode(int[])
	 * @see #nullSafeHashCode(long[])
	 * @see #nullSafeHashCode(short[])
	 */
	public static int nullSafeHashCode(@Nullable Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj.getClass().isArray()) {
			if (obj instanceof Object[]) {
				return nullSafeHashCode((Object[]) obj);
			}
			if (obj instanceof boolean[]) {
				return nullSafeHashCode((boolean[]) obj);
			}
			if (obj instanceof byte[]) {
				return nullSafeHashCode((byte[]) obj);
			}
			if (obj instanceof char[]) {
				return nullSafeHashCode((char[]) obj);
			}
			if (obj instanceof double[]) {
				return nullSafeHashCode((double[]) obj);
			}
			if (obj instanceof float[]) {
				return nullSafeHashCode((float[]) obj);
			}
			if (obj instanceof int[]) {
				return nullSafeHashCode((int[]) obj);
			}
			if (obj instanceof long[]) {
				return nullSafeHashCode((long[]) obj);
			}
			if (obj instanceof short[]) {
				return nullSafeHashCode((short[]) obj);
			}
		}
		return obj.hashCode();
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable Object[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (Object element : array) {
			hash = MULTIPLIER * hash + nullSafeHashCode(element);
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable boolean[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (boolean element : array) {
			hash = MULTIPLIER * hash + Boolean.hashCode(element);
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable byte[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (byte element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable char[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (char element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable double[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (double element : array) {
			hash = MULTIPLIER * hash + Double.hashCode(element);
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable float[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (float element : array) {
			hash = MULTIPLIER * hash + Float.hashCode(element);
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable int[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (int element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable long[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (long element : array) {
			hash = MULTIPLIER * hash + Long.hashCode(element);
		}
		return hash;
	}

	/**
	 * 返回基于指定数组内容的哈希码。
	 * 如果 {@code array} 为 {@code null}，此方法返回 0。
	 */
	public static int nullSafeHashCode(@Nullable short[] array) {
		if (array == null) {
			return 0;
		}
		int hash = INITIAL_HASH;
		for (short element : array) {
			hash = MULTIPLIER * hash + element;
		}
		return hash;
	}

	/**
	 * 返回与 {@link Boolean#hashCode(boolean)}} 相同的值。
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(boolean bool) {
		return Boolean.hashCode(bool);
	}

	/**
	 * 返回与 {@link Double#hashCode(double)}} 相同的值。
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(double dbl) {
		return Double.hashCode(dbl);
	}

	/**
	 * 返回与 {@link Float#hashCode(float)}} 相同的值。
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(float flt) {
		return Float.hashCode(flt);
	}

	/**
	 * 返回与 {@link Long#hashCode(long)}} 相同的值。
	 * @deprecated as of Spring Framework 5.0, in favor of the native JDK 8 variant
	 */
	@Deprecated
	public static int hashCode(long lng) {
		return Long.hashCode(lng);
	}


	//---------------------------------------------------------------------
	// Convenience methods for toString output
	//---------------------------------------------------------------------

	/**
	 * 返回对象整体标识的 String 表示形式。
	 * @param obj 对象（可为 {@code null}）
	 * @return 对象的标识作为 String 表示形式，
	 * 如果对象为 {@code null} 则为空 String
	 */
	public static String identityToString(@Nullable Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return obj.getClass().getName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * 返回对象标识哈希码的十六进制 String 形式。
	 * @param obj 对象
	 * @return 对象的标识码，以十六进制表示
	 */
	public static String getIdentityHexString(Object obj) {
		return Integer.toHexString(System.identityHashCode(obj));
	}

	/**
	 * 如果 {@code obj} 不为 {@code null}，则返回基于内容的 String 表示形式；否则返回空 String。
	 * <p>与 {@link #nullSafeToString(Object)} 的区别在于，对于 {@code null} 值，它返回空 String 而不是
	 * "null"。
	 * @param obj 要为其构建显示 String 的对象
	 * @return {@code obj} 的显示 String 表示形式
	 * @see #nullSafeToString(Object)
	 */
	public static String getDisplayString(@Nullable Object obj) {
		if (obj == null) {
			return EMPTY_STRING;
		}
		return nullSafeToString(obj);
	}

	/**
	 * 确定给定对象的类名。
	 * <p>如果 {@code obj} 为 {@code null}，则返回 {@code "null"} String。
	 * @param obj 要内省的对象（可为 {@code null}）
	 * @return 对应的类名
	 */
	public static String nullSafeClassName(@Nullable Object obj) {
		return (obj != null ? obj.getClass().getName() : NULL_STRING);
	}

	/**
	 * 返回指定 Object 的 String 表示形式。
	 * <p>如果对象是数组，则构建其内容的 String 表示形式。
	 * 如果 {@code obj} 为 {@code null}，则返回 {@code "null"} String。
	 * @param obj 要为其构建 String 表示形式的对象
	 * @return {@code obj} 的 String 表示形式
	 * @see #nullSafeConciseToString(Object)
	 */
	public static String nullSafeToString(@Nullable Object obj) {
		if (obj == null) {
			return NULL_STRING;
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		if (obj instanceof Object[]) {
			return nullSafeToString((Object[]) obj);
		}
		if (obj instanceof boolean[]) {
			return nullSafeToString((boolean[]) obj);
		}
		if (obj instanceof byte[]) {
			return nullSafeToString((byte[]) obj);
		}
		if (obj instanceof char[]) {
			return nullSafeToString((char[]) obj);
		}
		if (obj instanceof double[]) {
			return nullSafeToString((double[]) obj);
		}
		if (obj instanceof float[]) {
			return nullSafeToString((float[]) obj);
		}
		if (obj instanceof int[]) {
			return nullSafeToString((int[]) obj);
		}
		if (obj instanceof long[]) {
			return nullSafeToString((long[]) obj);
		}
		if (obj instanceof short[]) {
			return nullSafeToString((short[]) obj);
		}
		String str = obj.toString();
		return (str != null ? str : EMPTY_STRING);
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable Object[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (Object o : array) {
			stringJoiner.add(String.valueOf(o));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable boolean[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (boolean b : array) {
			stringJoiner.add(String.valueOf(b));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable byte[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (byte b : array) {
			stringJoiner.add(String.valueOf(b));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable char[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (char c : array) {
			stringJoiner.add('\'' + String.valueOf(c) + '\'');
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable double[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (double d : array) {
			stringJoiner.add(String.valueOf(d));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable float[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (float f : array) {
			stringJoiner.add(String.valueOf(f));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable int[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (int i : array) {
			stringJoiner.add(String.valueOf(i));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable long[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (long l : array) {
			stringJoiner.add(String.valueOf(l));
		}
		return stringJoiner.toString();
	}

	/**
	 * 返回指定数组内容的 String 表示形式。 <p>该 String 表示形式由数组元素的列表组成，这些元素括在花括
	 * 号（{@code "{}"}）中。相邻元素由字符 {@code ", "}（逗号后跟一个空格）分隔。 如果 {@code array}
	 * 为 {@code null}，则返回 {@code "null"} String。
	 * @param array 要为其构建 String 表示形式的数组
	 * @return {@code array} 的 String 表示形式
	 */
	public static String nullSafeToString(@Nullable short[] array) {
		if (array == null) {
			return NULL_STRING;
		}
		int length = array.length;
		if (length == 0) {
			return EMPTY_ARRAY;
		}
		StringJoiner stringJoiner = new StringJoiner(ARRAY_ELEMENT_SEPARATOR, ARRAY_START, ARRAY_END);
		for (short s : array) {
			stringJoiner.add(String.valueOf(s));
		}
		return stringJoiner.toString();
	}

	/**
	 * 生成所提供对象的 null 安全且简洁的字符串表示形式，如下所述。
	 * <p>当你需要限制生成的字符串长度时，优先使用此方法而不是 {@link #nullSafeToString(Object)}。
	 * <p>返回：
	 * <ul>
	 * <li>如果 {@code obj} 为 {@code null}，则为 {@code "null"}</li>
	 * <li>如果 {@code obj} 是 {@link Class}，则为 {@linkplain Class#getName() 类名}</li>
	 * <li>如果 {@code obj} 是 {@link String} 或 {@link CharSequence}，则可能为 {@linkplain StringUtils#truncate(CharSequence) 截断后的字符串}</li>
	 * <li>如果 {@code obj} 是<em>简单类型</em>且其 {@code toString()} 方法返回非 null 值，则可能为 {@linkplain StringUtils#truncate(CharSequence) 截断后的字符串}。</li>
	 * <li>否则，为对象类型名与 {@code @} 以及对象标识哈希码的十六进制字符串形式拼接而成的字符串</li>
	 * </ul>
	 * <p>在此方法的上下文中，<em>简单类型</em>是以下任意一种：基本类型包装器（不包括 {@link Void}）、
	 * {@link Enum}、{@link Number}、{@link Date}、{@link Temporal}、{@link URI}、{@link URL} 或
	 * {@link Locale}。
	 * @param obj 要为其构建字符串表示形式的对象
	 * @return 所提供对象的简洁字符串表示形式
	 * @since 5.3.27
	 * @see #nullSafeToString(Object)
	 * @see StringUtils#truncate(CharSequence)
	 */
	public static String nullSafeConciseToString(@Nullable Object obj) {
		if (obj == null) {
			return "null";
		}
		if (obj instanceof Class<?>) {
			return ((Class<?>) obj).getName();
		}
		if (obj instanceof CharSequence) {
			return StringUtils.truncate((CharSequence) obj);
		}
		Class<?> type = obj.getClass();
		if (isSimpleValueType(type)) {
			String str = obj.toString();
			if (str != null) {
				return StringUtils.truncate(str);
			}
		}
		return type.getTypeName() + "@" + getIdentityHexString(obj);
	}

	/**
	 * {@link org.springframework.beans.BeanUtils#isSimpleValueType(Class)} 的副本。
	 * <p>检查给定类型是否表示 "简单" 值类型：基本类型或基本类型包装器、枚举、String 或其他
	 * CharSequence、Number、Date、Temporal、URI、URL、Locale 或 Class。
	 * <p>{@code Void} 和 {@code void} 不被视为简单值类型。
	 * @param type 要检查的类型
	 * @return 给定类型是否表示“简单”值类型
	 */
	private static boolean isSimpleValueType(Class<?> type) {
		return (Void.class != type && void.class != type &&
				(ClassUtils.isPrimitiveOrWrapper(type) ||
				Enum.class.isAssignableFrom(type) ||
				CharSequence.class.isAssignableFrom(type) ||
				Number.class.isAssignableFrom(type) ||
				Date.class.isAssignableFrom(type) ||
				Temporal.class.isAssignableFrom(type) ||
				URI.class == type ||
				URL.class == type ||
				Locale.class == type ||
				Class.class == type));
	}

}
