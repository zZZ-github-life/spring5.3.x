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

package org.springframework.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 表示可用于 HTTP {@code "Range"} 头的 HTTP（字节）范围。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.2
 * @see <a href="https://tools.ietf.org/html/rfc7233">HTTP/1.1: Range Requests</a>
 * @see HttpHeaders#setRange(List)
 * @see HttpHeaders#getRange()
 */
public abstract class HttpRange {

	/** 每个请求允许的最大范围数。 */
	private static final int MAX_RANGES = 100;

	private static final String BYTE_RANGE_PREFIX = "bytes=";


	/**
	 * 使用当前 {@code HttpRange} 中包含的范围信息将 {@code Resource} 转换为 {@link ResourceRegion}。
	 * @param resource the {@code Resource} to select the region from
	 * @return the selected region of the given {@code Resource}
	 * @since 4.3
	 */
	public ResourceRegion toResourceRegion(Resource resource) {
		// Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
		// Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
		Assert.isTrue(resource.getClass() != InputStreamResource.class,
				"Cannot convert an InputStreamResource to a ResourceRegion");
		long contentLength = getLengthFor(resource);
		long start = getRangeStart(contentLength);
		long end = getRangeEnd(contentLength);
		Assert.isTrue(start < contentLength, () -> "'position' exceeds the resource length " + contentLength);
		return new ResourceRegion(resource, start, end - start + 1);
	}

	/**
	 * 在给定表示形式总长度的情况下，返回范围的起始位置。
	 * @param length the length of the representation
	 * @return the start of this range for the representation
	 */
	public abstract long getRangeStart(long length);

	/**
	 * 在给定表示形式总长度的情况下，返回范围的结束位置（包含）。
	 * @param length the length of the representation
	 * @return the end of the range for the representation
	 */
	public abstract long getRangeEnd(long length);


	/**
	 * 创建从给定位置到末尾的 {@code HttpRange}。
	 * @param firstBytePos the first byte position
	 * @return a byte range that ranges from {@code firstPos} till the end
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos) {
		return new ByteRange(firstBytePos, null);
	}

	/**
	 * 创建从给定的第一个位置到最后一个位置的 {@code HttpRange}。
	 * @param firstBytePos the first byte position
	 * @param lastBytePos the last byte position
	 * @return a byte range that ranges from {@code firstPos} till {@code lastPos}
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos, long lastBytePos) {
		return new ByteRange(firstBytePos, lastBytePos);
	}

	/**
	 * 创建一个覆盖最后给定字节数的 {@code HttpRange}。
	 * @param suffixLength the number of bytes for the range
	 * @return a byte range that ranges over the last {@code suffixLength} number of bytes
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createSuffixRange(long suffixLength) {
		return new SuffixByteRange(suffixLength);
	}

	/**
	 * 将给定的逗号分隔字符串解析为 {@code HttpRange} 对象列表。
	 * <p>此方法可用于解析 {@code Range} 头。
	 * @param ranges the string to parse
	 * @return the list of ranges
	 * @throws IllegalArgumentException if the string cannot be parsed
	 * or if the number of ranges is greater than 100
	 */
	public static List<HttpRange> parseRanges(@Nullable String ranges) {
		if (!StringUtils.hasLength(ranges)) {
			return Collections.emptyList();
		}
		if (!ranges.startsWith(BYTE_RANGE_PREFIX)) {
			throw new IllegalArgumentException("Range '" + ranges + "' does not start with 'bytes='");
		}
		ranges = ranges.substring(BYTE_RANGE_PREFIX.length());

		String[] tokens = StringUtils.tokenizeToStringArray(ranges, ",");
		if (tokens.length > MAX_RANGES) {
			throw new IllegalArgumentException("Too many ranges: " + tokens.length);
		}
		List<HttpRange> result = new ArrayList<>(tokens.length);
		for (String token : tokens) {
			result.add(parseRange(token));
		}
		return result;
	}

	private static HttpRange parseRange(String range) {
		Assert.hasLength(range, "Range String must not be empty");
		int dashIdx = range.indexOf('-');
		if (dashIdx > 0) {
			long firstPos = Long.parseLong(range.substring(0, dashIdx));
			if (dashIdx < range.length() - 1) {
				Long lastPos = Long.parseLong(range.substring(dashIdx + 1));
				return new ByteRange(firstPos, lastPos);
			}
			else {
				return new ByteRange(firstPos, null);
			}
		}
		else if (dashIdx == 0) {
			long suffixLength = Long.parseLong(range.substring(1));
			return new SuffixByteRange(suffixLength);
		}
		else {
			throw new IllegalArgumentException("Range '" + range + "' does not contain \"-\"");
		}
	}

	/**
	 * 将每个 {@code HttpRange} 转换为 {@code ResourceRegion}，利用 HTTP Range 信息选择给定 {@code Resource} 的相应片段。
	 * @param ranges the list of ranges
	 * @param resource the resource to select the regions from
	 * @return the list of regions for the given resource
	 * @throws IllegalArgumentException if the sum of all ranges exceeds the resource length
	 * @since 4.3
	 */
	public static List<ResourceRegion> toResourceRegions(List<HttpRange> ranges, Resource resource) {
		if (CollectionUtils.isEmpty(ranges)) {
			return Collections.emptyList();
		}
		List<ResourceRegion> regions = new ArrayList<>(ranges.size());
		for (HttpRange range : ranges) {
			regions.add(range.toResourceRegion(resource));
		}
		if (ranges.size() > 1) {
			long length = getLengthFor(resource);
			long total = 0;
			for (ResourceRegion region : regions) {
				total += region.getCount();
			}
			if (total >= length) {
				throw new IllegalArgumentException("The sum of all ranges (" + total +
						") should be less than the resource length (" + length + ")");
			}
		}
		return regions;
	}

	private static long getLengthFor(Resource resource) {
		try {
			long contentLength = resource.contentLength();
			Assert.isTrue(contentLength > 0, "Resource content length should be > 0");
			return contentLength;
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to obtain Resource content length", ex);
		}
	}

	/**
	 * 返回给定 {@code HttpRange} 对象列表的字符串表示形式。
	 * <p>此方法可用于生成 {@code Range} 头。
	 * @param ranges the ranges to create a string of
	 * @return the string representation
	 */
	public static String toString(Collection<HttpRange> ranges) {
		Assert.notEmpty(ranges, "Ranges Collection must not be empty");
		StringJoiner builder = new StringJoiner(", ", BYTE_RANGE_PREFIX, "");
		for (HttpRange range : ranges) {
			builder.add(range.toString());
		}
		return builder.toString();
	}


	/**
	 * 表示 HTTP/1.1 字节范围，包含第一个位置和可选的最后一个位置。
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 * @see HttpRange#createByteRange(long)
	 * @see HttpRange#createByteRange(long, long)
	 */
	private static class ByteRange extends HttpRange {

		private final long firstPos;

		@Nullable
		private final Long lastPos;

		public ByteRange(long firstPos, @Nullable Long lastPos) {
			assertPositions(firstPos, lastPos);
			this.firstPos = firstPos;
			this.lastPos = lastPos;
		}

		private void assertPositions(long firstBytePos, @Nullable Long lastBytePos) {
			if (firstBytePos < 0) {
				throw new IllegalArgumentException("Invalid first byte position: " + firstBytePos);
			}
			if (lastBytePos != null && lastBytePos < firstBytePos) {
				throw new IllegalArgumentException("firstBytePosition=" + firstBytePos +
						" should be less then or equal to lastBytePosition=" + lastBytePos);
			}
		}

		@Override
		public long getRangeStart(long length) {
			return this.firstPos;
		}

		@Override
		public long getRangeEnd(long length) {
			if (this.lastPos != null && this.lastPos < length) {
				return this.lastPos;
			}
			else {
				return length - 1;
			}
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ByteRange)) {
				return false;
			}
			ByteRange otherRange = (ByteRange) other;
			return (this.firstPos == otherRange.firstPos &&
					ObjectUtils.nullSafeEquals(this.lastPos, otherRange.lastPos));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.firstPos) * 31 +
					ObjectUtils.nullSafeHashCode(this.lastPos));
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.firstPos);
			builder.append('-');
			if (this.lastPos != null) {
				builder.append(this.lastPos);
			}
			return builder.toString();
		}
	}


	/**
	 * 表示 HTTP/1.1 后缀字节范围，包含若干后缀字节。
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 * @see HttpRange#createSuffixRange(long)
	 */
	private static class SuffixByteRange extends HttpRange {

		private final long suffixLength;

		public SuffixByteRange(long suffixLength) {
			if (suffixLength < 0) {
				throw new IllegalArgumentException("Invalid suffix length: " + suffixLength);
			}
			this.suffixLength = suffixLength;
		}

		@Override
		public long getRangeStart(long length) {
			if (this.suffixLength < length) {
				return length - this.suffixLength;
			}
			else {
				return 0;
			}
		}

		@Override
		public long getRangeEnd(long length) {
			return length - 1;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof SuffixByteRange)) {
				return false;
			}
			SuffixByteRange otherRange = (SuffixByteRange) other;
			return (this.suffixLength == otherRange.suffixLength);
		}

		@Override
		public int hashCode() {
			return Long.hashCode(this.suffixLength);
		}

		@Override
		public String toString() {
			return "-" + this.suffixLength;
		}
	}

}
