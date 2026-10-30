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

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * 按照 RFC 6266 定义的 Content-Disposition 类型和参数的表示形式。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sergey Tsypanov
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
 */
public final class ContentDisposition {

	private final static Pattern BASE64_ENCODED_PATTERN =
			Pattern.compile("=\\?([0-9a-zA-Z-_]+)\\?B\\?([+/0-9a-zA-Z]+=*)\\?=");

	private static final String INVALID_HEADER_FIELD_PARAMETER_FORMAT =
			"Invalid header field parameter format (as defined in RFC 5987)";


	@Nullable
	private final String type;

	@Nullable
	private final String name;

	@Nullable
	private final String filename;

	@Nullable
	private final Charset charset;

	@Nullable
	private final Long size;

	@Nullable
	private final ZonedDateTime creationDate;

	@Nullable
	private final ZonedDateTime modificationDate;

	@Nullable
	private final ZonedDateTime readDate;


	/**
	 * 私有构造函数。请参见此类中的静态工厂方法。
	 */
	private ContentDisposition(@Nullable String type, @Nullable String name, @Nullable String filename,
			@Nullable Charset charset, @Nullable Long size, @Nullable ZonedDateTime creationDate,
			@Nullable ZonedDateTime modificationDate, @Nullable ZonedDateTime readDate) {

		this.type = type;
		this.name = name;
		this.filename = filename;
		this.charset = charset;
		this.size = size;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
		this.readDate = readDate;
	}


	/**
	 * 返回 {@link #getType() type} 是否为 {@literal "attachment"}。
	 * @since 5.3
	 */
	public boolean isAttachment() {
		return (this.type != null && this.type.equalsIgnoreCase("attachment"));
	}

	/**
	 * 返回 {@link #getType() type} 是否为 {@literal "form-data"}。
	 * @since 5.3
	 */
	public boolean isFormData() {
		return (this.type != null && this.type.equalsIgnoreCase("form-data"));
	}

	/**
	 * 返回 {@link #getType() type} 是否为 {@literal "inline"}。
	 * @since 5.3
	 */
	public boolean isInline() {
		return (this.type != null && this.type.equalsIgnoreCase("inline"));
	}

	/**
	 * 返回处置类型。
	 * @see #isAttachment()
	 * @see #isFormData()
	 * @see #isInline()
	 */
	@Nullable
	public String getType() {
		return this.type;
	}

	/**
	 * 返回 {@literal name} 参数的值；如果未定义则返回 {@code null}。
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 返回 {@literal filename} 参数的值（可能已根据 RFC 2047 从 BASE64 编码解码），或 {@literal filename*} 参数的值（可能已按 RFC 5987 中的定义解码）。
	 */
	@Nullable
	public String getFilename() {
		return this.filename;
	}

	/**
	 * 返回 {@literal filename*} 参数中定义的字符集；如果未定义则返回 {@code null}。
	 */
	@Nullable
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * 返回 {@literal size} 参数的值；如果未定义则返回 {@code null}。
	 * @deprecated since 5.2.3 as per
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
	 * to be removed in a future release.
	 */
	@Deprecated
	@Nullable
	public Long getSize() {
		return this.size;
	}

	/**
	 * 返回 {@literal creation-date} 参数的值；如果未定义则返回 {@code null}。
	 * @deprecated since 5.2.3 as per
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
	 * to be removed in a future release.
	 */
	@Deprecated
	@Nullable
	public ZonedDateTime getCreationDate() {
		return this.creationDate;
	}

	/**
	 * 返回 {@literal modification-date} 参数的值；如果未定义则返回 {@code null}。
	 * @deprecated since 5.2.3 as per
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
	 * to be removed in a future release.
	 */
	@Deprecated
	@Nullable
	public ZonedDateTime getModificationDate() {
		return this.modificationDate;
	}

	/**
	 * 返回 {@literal read-date} 参数的值；如果未定义则返回 {@code null}。
	 * @deprecated since 5.2.3 as per
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
	 * to be removed in a future release.
	 */
	@Deprecated
	@Nullable
	public ZonedDateTime getReadDate() {
		return this.readDate;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ContentDisposition)) {
			return false;
		}
		ContentDisposition otherCd = (ContentDisposition) other;
		return (ObjectUtils.nullSafeEquals(this.type, otherCd.type) &&
				ObjectUtils.nullSafeEquals(this.name, otherCd.name) &&
				ObjectUtils.nullSafeEquals(this.filename, otherCd.filename) &&
				ObjectUtils.nullSafeEquals(this.charset, otherCd.charset) &&
				ObjectUtils.nullSafeEquals(this.size, otherCd.size) &&
				ObjectUtils.nullSafeEquals(this.creationDate, otherCd.creationDate)&&
				ObjectUtils.nullSafeEquals(this.modificationDate, otherCd.modificationDate)&&
				ObjectUtils.nullSafeEquals(this.readDate, otherCd.readDate));
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.type);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.filename);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.charset);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.size);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.creationDate);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.modificationDate);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.readDate);
		return result;
	}

	/**
	 * 返回此内容处置按 RFC 6266 定义的头值。
	 * @see #parse(String)
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.type != null) {
			sb.append(this.type);
		}
		if (this.name != null) {
			sb.append("; name=\"");
			sb.append(this.name).append('\"');
		}
		if (this.filename != null) {
			if (this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
				sb.append("; filename=\"");
				sb.append(escapeQuotationsInFilename(this.filename)).append('\"');
			}
			else {
				sb.append("; filename*=");
				sb.append(encodeFilename(this.filename, this.charset));
			}
		}
		if (this.size != null) {
			sb.append("; size=");
			sb.append(this.size);
		}
		if (this.creationDate != null) {
			sb.append("; creation-date=\"");
			sb.append(RFC_1123_DATE_TIME.format(this.creationDate));
			sb.append('\"');
		}
		if (this.modificationDate != null) {
			sb.append("; modification-date=\"");
			sb.append(RFC_1123_DATE_TIME.format(this.modificationDate));
			sb.append('\"');
		}
		if (this.readDate != null) {
			sb.append("; read-date=\"");
			sb.append(RFC_1123_DATE_TIME.format(this.readDate));
			sb.append('\"');
		}
		return sb.toString();
	}


	/**
	 * 返回类型为 {@literal "attachment"} 的 {@code ContentDisposition} 构建器。
	 * @since 5.3
	 */
	public static Builder attachment() {
		return builder("attachment");
	}

	/**
	 * 返回类型为 {@literal "form-data"} 的 {@code ContentDisposition} 构建器。
	 * @since 5.3
	 */
	public static Builder formData() {
		return builder("form-data");
	}

	/**
	 * 返回类型为 {@literal "inline"} 的 {@code ContentDisposition} 构建器。
	 * @since 5.3
	 */
	public static Builder inline() {
		return builder("inline");
	}

	/**
	 * 返回 {@code ContentDisposition} 的构建器。
	 * @param type 处置类型，例如 {@literal inline}、{@literal attachment} 或 {@literal form-data}
	 * @return 该构建器
	 */
	public static Builder builder(String type) {
		return new BuilderImpl(type);
	}

	/**
	 * 返回空的内容处置。
	 */
	public static ContentDisposition empty() {
		return new ContentDisposition("", null, null, null, null, null, null, null);
	}

	/**
	 * 解析按 RFC 2183 定义的 {@literal Content-Disposition} 头值。
	 * @param contentDisposition {@literal Content-Disposition} 标头值
	 * @return 解析后的内容处置
	 * @see #toString()
	 */
	public static ContentDisposition parse(String contentDisposition) {
		List<String> parts = tokenize(contentDisposition);
		String type = parts.get(0);
		String name = null;
		String filename = null;
		Charset charset = null;
		Long size = null;
		ZonedDateTime creationDate = null;
		ZonedDateTime modificationDate = null;
		ZonedDateTime readDate = null;
		for (int i = 1; i < parts.size(); i++) {
			String part = parts.get(i);
			int eqIndex = part.indexOf('=');
			if (eqIndex != -1) {
				String attribute = part.substring(0, eqIndex);
				String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"") ?
						part.substring(eqIndex + 2, part.length() - 1) :
						part.substring(eqIndex + 1));
				if (attribute.equals("name") ) {
					name = value;
				}
				else if (attribute.equals("filename*") ) {
					int idx1 = value.indexOf('\'');
					int idx2 = value.indexOf('\'', idx1 + 1);
					if (idx1 != -1 && idx2 != -1) {
						charset = Charset.forName(value.substring(0, idx1).trim());
						Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
								"Charset must be UTF-8 or ISO-8859-1");
						filename = decodeFilename(value.substring(idx2 + 1), charset);
					}
					else {
						// US ASCII
						filename = decodeFilename(value, StandardCharsets.US_ASCII);
					}
				}
				else if (attribute.equals("filename") && (filename == null)) {
					if (value.startsWith("=?") ) {
						Matcher matcher = BASE64_ENCODED_PATTERN.matcher(value);
						if (matcher.find()) {
							String match1 = matcher.group(1);
							String match2 = matcher.group(2);
							filename = new String(Base64.getDecoder().decode(match2), Charset.forName(match1));
						}
						else {
							filename = value;
						}
					}
					else {
						filename = value;
					}
				}
				else if (attribute.equals("size") ) {
					size = Long.parseLong(value);
				}
				else if (attribute.equals("creation-date")) {
					try {
						creationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
					}
					catch (DateTimeParseException ex) {
						// ignore
					}
				}
				else if (attribute.equals("modification-date")) {
					try {
						modificationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
					}
					catch (DateTimeParseException ex) {
						// ignore
					}
				}
				else if (attribute.equals("read-date")) {
					try {
						readDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
					}
					catch (DateTimeParseException ex) {
						// ignore
					}
				}
			}
			else {
				throw new IllegalArgumentException("Invalid content disposition format");
			}
		}
		return new ContentDisposition(type, name, filename, charset, size, creationDate, modificationDate, readDate);
	}

	private static List<String> tokenize(String headerValue) {
		int index = headerValue.indexOf(';');
		String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();
		if (type.isEmpty()) {
			throw new IllegalArgumentException("Content-Disposition header must not be empty");
		}
		List<String> parts = new ArrayList<>();
		parts.add(type);
		if (index >= 0) {
			do {
				int nextIndex = index + 1;
				boolean quoted = false;
				boolean escaped = false;
				while (nextIndex < headerValue.length()) {
					char ch = headerValue.charAt(nextIndex);
					if (ch == ';') {
						if (!quoted) {
							break;
						}
					}
					else if (!escaped && ch == '"') {
						quoted = !quoted;
					}
					escaped = (!escaped && ch == '\\');
					nextIndex++;
				}
				String part = headerValue.substring(index + 1, nextIndex).trim();
				if (!part.isEmpty()) {
					parts.add(part);
				}
				index = nextIndex;
			}
			while (index < headerValue.length());
		}
		return parts;
	}

	/**
	 * 按照 RFC 5987 的描述解码给定的头字段参数。
	 * <p>仅支持 US-ASCII、UTF-8 和 ISO-8859-1 字符集。
	 * @param filename 文件名
	 * @param charset 文件名的字符集
	 * @return 编码后的标头字段参数
	 * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
	 */
	private static String decodeFilename(String filename, Charset charset) {
		Assert.notNull(filename, "'filename' must not be null");
		Assert.notNull(charset, "'charset' must not be null");

		byte[] value = filename.getBytes(charset);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int index = 0;
		while (index < value.length) {
			byte b = value[index];
			if (isRFC5987AttrChar(b)) {
				baos.write((char) b);
				index++;
			}
			else if (b == '%' && index < value.length - 2) {
				char[] array = new char[]{(char) value[index + 1], (char) value[index + 2]};
				try {
					baos.write(Integer.parseInt(String.valueOf(array), 16));
				}
				catch (NumberFormatException ex) {
					throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT, ex);
				}
				index+=3;
			}
			else {
				throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT);
			}
		}
		return StreamUtils.copyToString(baos, charset);
	}

	private static boolean isRFC5987AttrChar(byte c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
				c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
				c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
	}

	private static String escapeQuotationsInFilename(String filename) {
		if (filename.indexOf('"') == -1 && filename.indexOf('\\') == -1) {
			return filename;
		}
		boolean escaped = false;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < filename.length() ; i++) {
			char c = filename.charAt(i);
			if (!escaped && c == '"') {
				sb.append("\\\"");
			}
			else {
				sb.append(c);
			}
			escaped = (!escaped && c == '\\');
		}
		// Remove backslash at the end.
		if (escaped) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * 按照 RFC 5987 的描述编码给定的头字段参数。
	 * @param input 标头字段参数
	 * @param charset 标头字段参数字符串的字符集，仅支持 US-ASCII、UTF-8 和 ISO-8859-1 字符集
	 * @return 编码后的标头字段参数
	 * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
	 */
	private static String encodeFilename(String input, Charset charset) {
		Assert.notNull(input, "'input' must not be null");
		Assert.notNull(charset, "'charset' must not be null");
		Assert.isTrue(!StandardCharsets.US_ASCII.equals(charset), "ASCII does not require encoding");
		Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset), "Only UTF-8 and ISO-8859-1 are supported");

		byte[] source = input.getBytes(charset);
		int len = source.length;
		StringBuilder sb = new StringBuilder(len << 1);
		sb.append(charset.name());
		sb.append("''");
		for (byte b : source) {
			if (isRFC5987AttrChar(b)) {
				sb.append((char) b);
			}
			else {
				sb.append('%');
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				sb.append(hex1);
				sb.append(hex2);
			}
		}
		return sb.toString();
	}


	/**
	 * 用于 {@code ContentDisposition} 的可变构建器。
	 */
	public interface Builder {

		/**
		 * 设置 {@literal name} 参数的值。
		 */
		Builder name(String name);

		/**
		 * 设置 {@literal filename} 参数的值。给定的文件名将格式化为 RFC 2616 第 2.2 节定义的带引号字符串，文件名值中的任何引号字符都会用反斜杠转义，例如 {@code "foo\"bar.txt"} 会变成 {@code "foo\\\"bar.txt"}。
		 */
		Builder filename(String filename);

		/**
		 * 设置 {@code filename} 的值，该值将按 RFC 5987 的定义进行编码。仅支持 US-ASCII、UTF-8 和 ISO-8859-1 字符集。
		 * <p><strong>注意：</strong>请勿将其用于 {@code "multipart/form-data"} 请求，因为 <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, Section 4.2</a> 以及 RFC 5987 都提到它不适用于 multipart 请求。
		 */
		Builder filename(String filename, Charset charset);

		/**
		 * 设置 {@literal size} 参数的值。
		 * @deprecated since 5.2.3 as per
		 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
		 * to be removed in a future release.
		 */
		@Deprecated
		Builder size(Long size);

		/**
		 * 设置 {@literal creation-date} 参数的值。
		 * @deprecated since 5.2.3 as per
		 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
		 * to be removed in a future release.
		 */
		@Deprecated
		Builder creationDate(ZonedDateTime creationDate);

		/**
		 * 设置 {@literal modification-date} 参数的值。
		 * @deprecated since 5.2.3 as per
		 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
		 * to be removed in a future release.
		 */
		@Deprecated
		Builder modificationDate(ZonedDateTime modificationDate);

		/**
		 * 设置 {@literal read-date} 参数的值。
		 * @deprecated since 5.2.3 as per
		 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266, Appendix B</a>,
		 * to be removed in a future release.
		 */
		@Deprecated
		Builder readDate(ZonedDateTime readDate);

		/**
		 * 构建内容处置。
		 */
		ContentDisposition build();
	}


	private static class BuilderImpl implements Builder {

		private final String type;

		@Nullable
		private String name;

		@Nullable
		private String filename;

		@Nullable
		private Charset charset;

		@Nullable
		private Long size;

		@Nullable
		private ZonedDateTime creationDate;

		@Nullable
		private ZonedDateTime modificationDate;

		@Nullable
		private ZonedDateTime readDate;

		public BuilderImpl(String type) {
			Assert.hasText(type, "'type' must not be not empty");
			this.type = type;
		}

		@Override
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder filename(String filename) {
			Assert.hasText(filename, "No filename");
			this.filename = filename;
			return this;
		}

		@Override
		public Builder filename(String filename, Charset charset) {
			Assert.hasText(filename, "No filename");
			this.filename = filename;
			this.charset = charset;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder size(Long size) {
			this.size = size;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder creationDate(ZonedDateTime creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder modificationDate(ZonedDateTime modificationDate) {
			this.modificationDate = modificationDate;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder readDate(ZonedDateTime readDate) {
			this.readDate = readDate;
			return this;
		}

		@Override
		public ContentDisposition build() {
			return new ContentDisposition(this.type, this.name, this.filename, this.charset,
					this.size, this.creationDate, this.modificationDate, this.readDate);
		}
	}

}
