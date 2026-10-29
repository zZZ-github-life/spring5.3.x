/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.util.unit;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 数据大小，例如 '12MB'。
 *
 * <p>此类以字节为单位对数据大小建模，并且是不可变且线程安全的。
 *
 * <p>此类中使用的术语和单位基于<a href="https://en.wikipedia.org/wiki/Binary_prefix">二进制前缀
 * </a>，表示按 2 的幂进行乘法运算。详情请参阅下表以及 {@link DataUnit} 的 Javadoc。
 *
 * <p>
 * <table border="1">
 * <tr><th>术语</th><th>数据大小</th><th>字节数</th></tr>
 * <tr><td>字节</td><td>1B</td><td>1</td></tr>
 * <tr><td>千字节</td><td>1KB</td><td>1,024</td></tr>
 * <tr><td>兆字节</td><td>1MB</td><td>1,048,576</td></tr>
 * <tr><td>吉字节</td><td>1GB</td><td>1,073,741,824</td></tr>
 * <tr><td>太字节</td><td>1TB</td><td>1,099,511,627,776</td></tr>
 * </table>
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 5.1
 * @see DataUnit
 */
@SuppressWarnings("serial")
public final class DataSize implements Comparable<DataSize>, Serializable {

	/**
	 * 每千字节的字节数。
	 */
	private static final long BYTES_PER_KB = 1024;

	/**
	 * 每兆字节的字节数。
	 */
	private static final long BYTES_PER_MB = BYTES_PER_KB * 1024;

	/**
	 * 每吉字节的字节数。
	 */
	private static final long BYTES_PER_GB = BYTES_PER_MB * 1024;

	/**
	 * 每太字节的字节数。
	 */
	private static final long BYTES_PER_TB = BYTES_PER_GB * 1024;


	private final long bytes;


	private DataSize(long bytes) {
		this.bytes = bytes;
	}


	/**
	 * 获取一个表示指定字节数的 {@link DataSize}。
	 * @param bytes the number of bytes, positive or negative
	 * @return a {@link DataSize}
	 */
	public static DataSize ofBytes(long bytes) {
		return new DataSize(bytes);
	}

	/**
	 * 获取一个表示指定千字节数的 {@link DataSize}。
	 * @param kilobytes the number of kilobytes, positive or negative
	 * @return a {@link DataSize}
	 */
	public static DataSize ofKilobytes(long kilobytes) {
		return new DataSize(Math.multiplyExact(kilobytes, BYTES_PER_KB));
	}

	/**
	 * 获取一个表示指定兆字节数的 {@link DataSize}。
	 * @param megabytes the number of megabytes, positive or negative
	 * @return a {@link DataSize}
	 */
	public static DataSize ofMegabytes(long megabytes) {
		return new DataSize(Math.multiplyExact(megabytes, BYTES_PER_MB));
	}

	/**
	 * 获取一个表示指定吉字节数的 {@link DataSize}。
	 * @param gigabytes the number of gigabytes, positive or negative
	 * @return a {@link DataSize}
	 */
	public static DataSize ofGigabytes(long gigabytes) {
		return new DataSize(Math.multiplyExact(gigabytes, BYTES_PER_GB));
	}

	/**
	 * 获取一个表示指定太字节数的 {@link DataSize}。
	 * @param terabytes the number of terabytes, positive or negative
	 * @return a {@link DataSize}
	 */
	public static DataSize ofTerabytes(long terabytes) {
		return new DataSize(Math.multiplyExact(terabytes, BYTES_PER_TB));
	}

	/**
	 * 获取一个表示指定 {@link DataUnit} 中数量的 {@link DataSize}。
	 * @param amount the amount of the size, measured in terms of the unit,
	 * positive or negative
	 * @return a corresponding {@link DataSize}
	 */
	public static DataSize of(long amount, DataUnit unit) {
		Assert.notNull(unit, "Unit must not be null");
		return new DataSize(Math.multiplyExact(amount, unit.size().toBytes()));
	}

	/**
	 * 从诸如 {@code 12MB} 这样的文本字符串获取 {@link DataSize}，如果未指定单位，则使用
	 * {@link DataUnit#BYTES}。
	 * <p>
	 * 示例：
	 * <pre>
	 * "12KB" -- parses as "12 kilobytes"
	 * "5MB"  -- parses as "5 megabytes"
	 * "20"   -- parses as "20 bytes"
	 * </pre>
	 * @param text the text to parse
	 * @return the parsed {@link DataSize}
	 * @see #parse(CharSequence, DataUnit)
	 */
	public static DataSize parse(CharSequence text) {
		return parse(text, null);
	}

	/**
	 * 从诸如 {@code 12MB} 这样的文本字符串获取 {@link DataSize}，如果未指定单位，则使用指定的默认
	 * {@link DataUnit}。
	 * <p>
	 * 该字符串以数字开头，后面可选地跟一个与受支持的 {@linkplain DataUnit 后缀}之一匹配的单位。
	 * <p>
	 * 示例：
	 * <pre>
	 * "12KB" -- parses as "12 kilobytes"
	 * "5MB"  -- parses as "5 megabytes"
	 * "20"   -- parses as "20 kilobytes" (where the {@code defaultUnit} is {@link DataUnit#KILOBYTES})
	 * </pre>
	 * @param text the text to parse
	 * @return the parsed {@link DataSize}
	 */
	public static DataSize parse(CharSequence text, @Nullable DataUnit defaultUnit) {
		Assert.notNull(text, "Text must not be null");
		try {
			Matcher matcher = DataSizeUtils.PATTERN.matcher(StringUtils.trimAllWhitespace(text));
			Assert.state(matcher.matches(), "Does not match data size pattern");
			DataUnit unit = DataSizeUtils.determineDataUnit(matcher.group(2), defaultUnit);
			long amount = Long.parseLong(matcher.group(1));
			return DataSize.of(amount, unit);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("'" + text + "' is not a valid data size", ex);
		}
	}

	/**
	 * 检查此大小是否为负数，不包括零。
	 * @return true if this size has a size less than zero bytes
	 */
	public boolean isNegative() {
		return this.bytes < 0;
	}

	/**
	 * 返回此实例中的字节数。
	 * @return the number of bytes
	 */
	public long toBytes() {
		return this.bytes;
	}

	/**
	 * 返回此实例中的千字节数。
	 * @return the number of kilobytes
	 */
	public long toKilobytes() {
		return this.bytes / BYTES_PER_KB;
	}

	/**
	 * 返回此实例中的兆字节数。
	 * @return the number of megabytes
	 */
	public long toMegabytes() {
		return this.bytes / BYTES_PER_MB;
	}

	/**
	 * 返回此实例中的吉字节数。
	 * @return the number of gigabytes
	 */
	public long toGigabytes() {
		return this.bytes / BYTES_PER_GB;
	}

	/**
	 * 返回此实例中的太字节数。
	 * @return the number of terabytes
	 */
	public long toTerabytes() {
		return this.bytes / BYTES_PER_TB;
	}

	@Override
	public int compareTo(DataSize other) {
		return Long.compare(this.bytes, other.bytes);
	}

	@Override
	public String toString() {
		return String.format("%dB", this.bytes);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		DataSize otherSize = (DataSize) other;
		return (this.bytes == otherSize.bytes);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.bytes);
	}


	/**
	 * 用于支持 {@link #PATTERN} 延迟加载的静态嵌套类。
	 * @since 5.3.21
	 */
	private static class DataSizeUtils {

		/**
		 * 用于解析的模式。
		 */
		private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");

		private static DataUnit determineDataUnit(String suffix, @Nullable DataUnit defaultUnit) {
			DataUnit defaultUnitToUse = (defaultUnit != null ? defaultUnit : DataUnit.BYTES);
			return (StringUtils.hasLength(suffix) ? DataUnit.fromSuffix(suffix) : defaultUnitToUse);
		}

	}

}
