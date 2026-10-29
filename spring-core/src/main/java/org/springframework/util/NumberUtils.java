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

package org.springframework.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * 用于数字转换和解析的各种工具方法。
 * <p>主要用于框架内部；如需更全面的数字工具套件，可考虑 Apache 的 Commons Lang。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1.2
 */
public abstract class NumberUtils {

	private static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

	private static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);

	/**
	 * 标准数字类型（均不可变）：
	 * Byte、Short、Integer、Long、BigInteger、Float、Double、BigDecimal。
	 */
	public static final Set<Class<?>> STANDARD_NUMBER_TYPES;

	static {
		Set<Class<?>> numberTypes = new HashSet<>(8);
		numberTypes.add(Byte.class);
		numberTypes.add(Short.class);
		numberTypes.add(Integer.class);
		numberTypes.add(Long.class);
		numberTypes.add(BigInteger.class);
		numberTypes.add(Float.class);
		numberTypes.add(Double.class);
		numberTypes.add(BigDecimal.class);
		STANDARD_NUMBER_TYPES = Collections.unmodifiableSet(numberTypes);
	}


	/**
	 * 将给定的数字转换为给定目标类的实例。
	 * @param number the number to convert
	 * @param targetClass the target class to convert to
	 * @return the converted number
	 * @throws IllegalArgumentException if the target class is not supported
	 * (i.e. not a standard Number subclass as included in the JDK)
	 * @see java.lang.Byte
	 * @see java.lang.Short
	 * @see java.lang.Integer
	 * @see java.lang.Long
	 * @see java.math.BigInteger
	 * @see java.lang.Float
	 * @see java.lang.Double
	 * @see java.math.BigDecimal
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T convertNumberToTargetClass(Number number, Class<T> targetClass)
			throws IllegalArgumentException {

		Assert.notNull(number, "Number must not be null");
		Assert.notNull(targetClass, "Target class must not be null");

		if (targetClass.isInstance(number)) {
			return (T) number;
		}
		else if (Byte.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return (T) Byte.valueOf(number.byteValue());
		}
		else if (Short.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return (T) Short.valueOf(number.shortValue());
		}
		else if (Integer.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
				raiseOverflowException(number, targetClass);
			}
			return (T) Integer.valueOf(number.intValue());
		}
		else if (Long.class == targetClass) {
			long value = checkedLongValue(number, targetClass);
			return (T) Long.valueOf(value);
		}
		else if (BigInteger.class == targetClass) {
			if (number instanceof BigDecimal) {
				// do not lose precision - use BigDecimal's own conversion
				return (T) ((BigDecimal) number).toBigInteger();
			}
			else {
				// original value is not a Big* number - use standard long conversion
				return (T) BigInteger.valueOf(number.longValue());
			}
		}
		else if (Float.class == targetClass) {
			return (T) Float.valueOf(number.floatValue());
		}
		else if (Double.class == targetClass) {
			return (T) Double.valueOf(number.doubleValue());
		}
		else if (BigDecimal.class == targetClass) {
			// always use BigDecimal(String) here to avoid unpredictability of BigDecimal(double)
			// (see BigDecimal javadoc for details)
			return (T) new BigDecimal(number.toString());
		}
		else {
			throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
					number.getClass().getName() + "] to unsupported target class [" + targetClass.getName() + "]");
		}
	}

	/**
	 * 在以 long 值返回给定数字之前，检查 {@code BigInteger}/{@code BigDecimal} 的 long 溢出。
	 * @param number the number to convert
	 * @param targetClass the target class to convert to
	 * @return the long value, if convertible without overflow
	 * @throws IllegalArgumentException if there is an overflow
	 * @see #raiseOverflowException
	 */
	private static long checkedLongValue(Number number, Class<? extends Number> targetClass) {
		BigInteger bigInt = null;
		if (number instanceof BigInteger) {
			bigInt = (BigInteger) number;
		}
		else if (number instanceof BigDecimal) {
			bigInt = ((BigDecimal) number).toBigInteger();
		}
		// Effectively analogous to JDK 8's BigInteger.longValueExact()
		if (bigInt != null && (bigInt.compareTo(LONG_MIN) < 0 || bigInt.compareTo(LONG_MAX) > 0)) {
			raiseOverflowException(number, targetClass);
		}
		return number.longValue();
	}

	/**
	 * 针对给定数字和目标类抛出<em>溢出</em>异常。
	 * @param number the number we tried to convert
	 * @param targetClass the target class we tried to convert to
	 * @throws IllegalArgumentException if there is an overflow
	 */
	private static void raiseOverflowException(Number number, Class<?> targetClass) {
		throw new IllegalArgumentException("Could not convert number [" + number + "] of type [" +
				number.getClass().getName() + "] to target class [" + targetClass.getName() + "]: overflow");
	}

	/**
	 * 将给定的 {@code text} 解析为给定目标类的 {@link Number} 实例，使用相应的 {@code decode} /
	 * {@code valueOf} 方法。
	 * <p>在尝试解析数字之前，去除输入 {@code String} 中的所有空白（前导、尾随以及字符之间的空白）。
	 * <p>也支持十六进制格式的数字（带有前导 "0x"、"0X" 或 "#"）。
	 * @param text the text to convert
	 * @param targetClass the target class to parse into
	 * @return the parsed number
	 * @throws IllegalArgumentException if the target class is not supported
	 * (i.e. not a standard Number subclass as included in the JDK)
	 * @see Byte#decode
	 * @see Short#decode
	 * @see Integer#decode
	 * @see Long#decode
	 * @see #decodeBigInteger(String)
	 * @see Float#valueOf
	 * @see Double#valueOf
	 * @see java.math.BigDecimal#BigDecimal(String)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T parseNumber(String text, Class<T> targetClass) {
		Assert.notNull(text, "Text must not be null");
		Assert.notNull(targetClass, "Target class must not be null");
		String trimmed = StringUtils.trimAllWhitespace(text);

		if (Byte.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Byte.decode(trimmed) : Byte.valueOf(trimmed));
		}
		else if (Short.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Short.decode(trimmed) : Short.valueOf(trimmed));
		}
		else if (Integer.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Integer.decode(trimmed) : Integer.valueOf(trimmed));
		}
		else if (Long.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? Long.decode(trimmed) : Long.valueOf(trimmed));
		}
		else if (BigInteger.class == targetClass) {
			return (T) (isHexNumber(trimmed) ? decodeBigInteger(trimmed) : new BigInteger(trimmed));
		}
		else if (Float.class == targetClass) {
			return (T) Float.valueOf(trimmed);
		}
		else if (Double.class == targetClass) {
			return (T) Double.valueOf(trimmed);
		}
		else if (BigDecimal.class == targetClass || Number.class == targetClass) {
			return (T) new BigDecimal(trimmed);
		}
		else {
			throw new IllegalArgumentException(
					"Cannot convert String [" + text + "] to target class [" + targetClass.getName() + "]");
		}
	}

	/**
	 * 将给定的 {@code text} 解析为给定目标类的 {@link Number} 实例，使用提供的 {@link NumberFormat}。
	 * <p>在尝试解析数字之前，去除输入 {@code String} 的空白。
	 * @param text the text to convert
	 * @param targetClass the target class to parse into
	 * @param numberFormat the {@code NumberFormat} to use for parsing (if
	 * {@code null}, this method falls back to {@link #parseNumber(String, Class)})
	 * @return the parsed number
	 * @throws IllegalArgumentException if the target class is not supported
	 * (i.e. not a standard Number subclass as included in the JDK)
	 * @see java.text.NumberFormat#parse
	 * @see #convertNumberToTargetClass
	 * @see #parseNumber(String, Class)
	 */
	public static <T extends Number> T parseNumber(
			String text, Class<T> targetClass, @Nullable NumberFormat numberFormat) {

		if (numberFormat != null) {
			Assert.notNull(text, "Text must not be null");
			Assert.notNull(targetClass, "Target class must not be null");
			DecimalFormat decimalFormat = null;
			boolean resetBigDecimal = false;
			if (numberFormat instanceof DecimalFormat) {
				decimalFormat = (DecimalFormat) numberFormat;
				if (BigDecimal.class == targetClass && !decimalFormat.isParseBigDecimal()) {
					decimalFormat.setParseBigDecimal(true);
					resetBigDecimal = true;
				}
			}
			try {
				Number number = numberFormat.parse(StringUtils.trimAllWhitespace(text));
				return convertNumberToTargetClass(number, targetClass);
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Could not parse number: " + ex.getMessage());
			}
			finally {
				if (resetBigDecimal) {
					decimalFormat.setParseBigDecimal(false);
				}
			}
		}
		else {
			return parseNumber(text, targetClass);
		}
	}

	/**
	 * 确定给定的 {@code value} String 是否表示十六进制数字，即是否需要传入 {@code Integer.decode} 而不
	 * 是 {@code Integer.valueOf}，等等。
	 */
	private static boolean isHexNumber(String value) {
		int index = (value.startsWith("-") ? 1 : 0);
		return (value.startsWith("0x", index) || value.startsWith("0X", index) || value.startsWith("#", index));
	}

	/**
	 * 从提供的 {@link String} 值中解码 {@link java.math.BigInteger}。
	 * <p>支持十进制、十六进制和八进制表示法。
	 * @see BigInteger#BigInteger(String, int)
	 */
	private static BigInteger decodeBigInteger(String value) {
		int radix = 10;
		int index = 0;
		boolean negative = false;

		// Handle minus sign, if present.
		if (value.startsWith("-")) {
			negative = true;
			index++;
		}

		// Handle radix specifier, if present.
		if (value.startsWith("0x", index) || value.startsWith("0X", index)) {
			index += 2;
			radix = 16;
		}
		else if (value.startsWith("#", index)) {
			index++;
			radix = 16;
		}
		else if (value.startsWith("0", index) && value.length() > 1 + index) {
			index++;
			radix = 8;
		}

		BigInteger result = new BigInteger(value.substring(index), radix);
		return (negative ? result.negate() : result);
	}

}
