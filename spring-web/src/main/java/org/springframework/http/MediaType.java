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

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * {@link MimeType} 的子类，增加了对 HTTP 规范中定义的质量参数的支持。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @since 3.0
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.1">
 *     HTTP 1.1: Semantics and Content, section 3.1.1.1</a>
 */
public class MediaType extends MimeType implements Serializable {

	private static final long serialVersionUID = 2069937152339670231L;

	/**
	 * 包含所有媒体范围（即 "&#42;/&#42;"）的公共常量媒体类型。
	 */
	public static final MediaType ALL;

	/**
	 * {@link MediaType#ALL} 的字符串等价形式。
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 * 用于 {@code application/atom+xml} 的公共常量媒体类型。
	 */
	public static final MediaType APPLICATION_ATOM_XML;

	/**
	 * {@link MediaType#APPLICATION_ATOM_XML} 的字符串等价形式。
	 */
	public static final String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	/**
	 * 用于 {@code application/cbor} 的公共常量媒体类型。
	 * @since 5.2
	 */
	public static final MediaType APPLICATION_CBOR;

	/**
	 * {@link MediaType#APPLICATION_CBOR} 的字符串等价形式。
	 * @since 5.2
	 */
	public static final String APPLICATION_CBOR_VALUE = "application/cbor";

	/**
	 * 用于 {@code application/x-www-form-urlencoded} 的公共常量媒体类型。
	 */
	public static final MediaType APPLICATION_FORM_URLENCODED;

	/**
	 * {@link MediaType#APPLICATION_FORM_URLENCODED} 的字符串等价形式。
	 */
	public static final String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	/**
	 * 用于 {@code application/graphql+json} 的公共常量媒体类型。
	 * @since 5.3.19
	 * @see <a href="https://github.com/graphql/graphql-over-http">GraphQL over HTTP spec</a>
	 */
	public static final MediaType APPLICATION_GRAPHQL;

	/**
	 * {@link MediaType#APPLICATION_GRAPHQL} 的字符串等价形式。
	 * @since 5.3.19
	 */
	public static final String APPLICATION_GRAPHQL_VALUE = "application/graphql+json";

	/**
	 * 用于 {@code application/json} 的公共常量媒体类型。
	 */
	public static final MediaType APPLICATION_JSON;

	/**
	 * {@link MediaType#APPLICATION_JSON} 的字符串等价形式。
	 * @see #APPLICATION_JSON_UTF8_VALUE
	 */
	public static final String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * 用于 {@code application/json;charset=UTF-8} 的公共常量媒体类型。
	 * @deprecated as of 5.2 in favor of {@link #APPLICATION_JSON}
	 * since major browsers like Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * now comply with the specification</a> and interpret correctly UTF-8 special
	 * characters without requiring a {@code charset=UTF-8} parameter.
	 */
	@Deprecated
	public static final MediaType APPLICATION_JSON_UTF8;

	/**
	 * {@link MediaType#APPLICATION_JSON_UTF8} 的字符串等价形式。
	 * @deprecated as of 5.2 in favor of {@link #APPLICATION_JSON_VALUE}
	 * since major browsers like Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * now comply with the specification</a> and interpret correctly UTF-8 special
	 * characters without requiring a {@code charset=UTF-8} parameter.
	 */
	@Deprecated
	public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

	/**
	 * 用于 {@code application/octet-stream} 的公共常量媒体类型。
	 */
	public static final MediaType APPLICATION_OCTET_STREAM;

	/**
	 * {@link MediaType#APPLICATION_OCTET_STREAM} 的字符串等价形式。
	 */
	public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * 用于 {@code application/pdf} 的公共常量媒体类型。
	 * @since 4.3
	 */
	public static final MediaType APPLICATION_PDF;

	/**
	 * {@link MediaType#APPLICATION_PDF} 的字符串等价形式。
	 * @since 4.3
	 */
	public static final String APPLICATION_PDF_VALUE = "application/pdf";

	/**
	 * 用于 {@code application/problem+json} 的公共常量媒体类型。
	 * @since 5.0
	 * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1">
	 *     Problem Details for HTTP APIs, 6.1. application/problem+json</a>
	 */
	public static final MediaType APPLICATION_PROBLEM_JSON;

	/**
	 * {@link MediaType#APPLICATION_PROBLEM_JSON} 的字符串等价形式。
	 * @since 5.0
	 */
	public static final String APPLICATION_PROBLEM_JSON_VALUE = "application/problem+json";

	/**
	 * 用于 {@code application/problem+json} 的公共常量媒体类型。
	 * @since 5.0
	 * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.1">
	 *     Problem Details for HTTP APIs, 6.1. application/problem+json</a>
	 * @deprecated as of 5.2 in favor of {@link #APPLICATION_PROBLEM_JSON}
	 * since major browsers like Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * now comply with the specification</a> and interpret correctly UTF-8 special
	 * characters without requiring a {@code charset=UTF-8} parameter.
	 */
	@Deprecated
	public static final MediaType APPLICATION_PROBLEM_JSON_UTF8;

	/**
	 * {@link MediaType#APPLICATION_PROBLEM_JSON_UTF8} 的字符串等价形式。
	 * @since 5.0
	 * @deprecated as of 5.2 in favor of {@link #APPLICATION_PROBLEM_JSON_VALUE}
	 * since major browsers like Chrome
	 * <a href="https://bugs.chromium.org/p/chromium/issues/detail?id=438464">
	 * now comply with the specification</a> and interpret correctly UTF-8 special
	 * characters without requiring a {@code charset=UTF-8} parameter.
	 */
	@Deprecated
	public static final String APPLICATION_PROBLEM_JSON_UTF8_VALUE = "application/problem+json;charset=UTF-8";

	/**
	 * 用于 {@code application/problem+xml} 的公共常量媒体类型。
	 * @since 5.0
	 * @see <a href="https://tools.ietf.org/html/rfc7807#section-6.2">
	 *     Problem Details for HTTP APIs, 6.2. application/problem+xml</a>
	 */
	public static final MediaType APPLICATION_PROBLEM_XML;

	/**
	 * {@link MediaType#APPLICATION_PROBLEM_XML} 的字符串等价形式。
	 * @since 5.0
	 */
	public static final String APPLICATION_PROBLEM_XML_VALUE = "application/problem+xml";

	/**
	 * 用于 {@code application/rss+xml} 的公共常量媒体类型。
	 * @since 4.3.6
	 */
	public static final MediaType APPLICATION_RSS_XML;

	/**
	 * {@link MediaType#APPLICATION_RSS_XML} 的字符串等价形式。
	 * @since 4.3.6
	 */
	public static final String APPLICATION_RSS_XML_VALUE = "application/rss+xml";

	/**
	 * 用于 {@code application/x-ndjson} 的公共常量媒体类型。
	 * @since 5.3
	 */
	public static final MediaType APPLICATION_NDJSON;

	/**
	 * {@link MediaType#APPLICATION_NDJSON} 的字符串等价形式。
	 * @since 5.3
	 */
	public static final String APPLICATION_NDJSON_VALUE = "application/x-ndjson";

	/**
	 * 用于 {@code application/stream+json} 的公共常量媒体类型。
	 * @since 5.0
	 * @deprecated as of 5.3, see notice on {@link #APPLICATION_STREAM_JSON_VALUE}.
	 */
	@Deprecated
	public static final MediaType APPLICATION_STREAM_JSON;

	/**
	 * {@link MediaType#APPLICATION_STREAM_JSON} 的字符串等价形式。
	 * @since 5.0
	 * @deprecated as of 5.3 since it originates from the W3C Activity Streams
	 * specification which has a more specific purpose and has been since
	 * replaced with a different mime type. Use {@link #APPLICATION_NDJSON} as
	 * a replacement or any other line-delimited JSON format (e.g. JSON Lines,
	 * JSON Text Sequences).
	 */
	@Deprecated
	public static final String APPLICATION_STREAM_JSON_VALUE = "application/stream+json";

	/**
	 * 用于 {@code application/xhtml+xml} 的公共常量媒体类型。
	 */
	public static final MediaType APPLICATION_XHTML_XML;

	/**
	 * {@link MediaType#APPLICATION_XHTML_XML} 的字符串等价形式。
	 */
	public static final String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	/**
	 * 用于 {@code application/xml} 的公共常量媒体类型。
	 */
	public static final MediaType APPLICATION_XML;

	/**
	 * {@link MediaType#APPLICATION_XML} 的字符串等价形式。
	 */
	public static final String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * 用于 {@code image/gif} 的公共常量媒体类型。
	 */
	public static final MediaType IMAGE_GIF;

	/**
	 * {@link MediaType#IMAGE_GIF} 的字符串等价形式。
	 */
	public static final String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * 用于 {@code image/jpeg} 的公共常量媒体类型。
	 */
	public static final MediaType IMAGE_JPEG;

	/**
	 * {@link MediaType#IMAGE_JPEG} 的字符串等价形式。
	 */
	public static final String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * 用于 {@code image/png} 的公共常量媒体类型。
	 */
	public static final MediaType IMAGE_PNG;

	/**
	 * {@link MediaType#IMAGE_PNG} 的字符串等价形式。
	 */
	public static final String IMAGE_PNG_VALUE = "image/png";

	/**
	 * 用于 {@code multipart/form-data} 的公共常量媒体类型。
	 */
	public static final MediaType MULTIPART_FORM_DATA;

	/**
	 * {@link MediaType#MULTIPART_FORM_DATA} 的字符串等价形式。
	 */
	public static final String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	/**
	 * 用于 {@code multipart/mixed} 的公共常量媒体类型。
	 * @since 5.2
	 */
	public static final MediaType MULTIPART_MIXED;

	/**
	 * {@link MediaType#MULTIPART_MIXED} 的字符串等价形式。
	 * @since 5.2
	 */
	public static final String MULTIPART_MIXED_VALUE = "multipart/mixed";

	/**
	 * 用于 {@code multipart/related} 的公共常量媒体类型。
	 * @since 5.2.5
	 */
	public static final MediaType MULTIPART_RELATED;

	/**
	 * {@link MediaType#MULTIPART_RELATED} 的字符串等价形式。
	 * @since 5.2.5
	 */
	public static final String MULTIPART_RELATED_VALUE = "multipart/related";

	/**
	 * 用于 {@code text/event-stream} 的公共常量媒体类型。
	 * @since 4.3.6
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	public static final MediaType TEXT_EVENT_STREAM;

	/**
	 * {@link MediaType#TEXT_EVENT_STREAM} 的字符串等价形式。
	 * @since 4.3.6
	 */
	public static final String TEXT_EVENT_STREAM_VALUE = "text/event-stream";

	/**
	 * 用于 {@code text/html} 的公共常量媒体类型。
	 */
	public static final MediaType TEXT_HTML;

	/**
	 * {@link MediaType#TEXT_HTML} 的字符串等价形式。
	 */
	public static final String TEXT_HTML_VALUE = "text/html";

	/**
	 * 用于 {@code text/markdown} 的公共常量媒体类型。
	 * @since 4.3
	 */
	public static final MediaType TEXT_MARKDOWN;

	/**
	 * {@link MediaType#TEXT_MARKDOWN} 的字符串等价形式。
	 * @since 4.3
	 */
	public static final String TEXT_MARKDOWN_VALUE = "text/markdown";

	/**
	 * 用于 {@code text/plain} 的公共常量媒体类型。
	 */
	public static final MediaType TEXT_PLAIN;

	/**
	 * {@link MediaType#TEXT_PLAIN} 的字符串等价形式。
	 */
	public static final String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * 用于 {@code text/xml} 的公共常量媒体类型。
	 */
	public static final MediaType TEXT_XML;

	/**
	 * {@link MediaType#TEXT_XML} 的字符串等价形式。
	 */
	public static final String TEXT_XML_VALUE = "text/xml";

	private static final String PARAM_QUALITY_FACTOR = "q";


	static {
		// Not using "valueOf' to avoid static init cost
		ALL = new MediaType("*", "*");
		APPLICATION_ATOM_XML = new MediaType("application", "atom+xml");
		APPLICATION_CBOR = new MediaType("application", "cbor");
		APPLICATION_FORM_URLENCODED = new MediaType("application", "x-www-form-urlencoded");
		APPLICATION_GRAPHQL = new MediaType("application", "graphql+json");
		APPLICATION_JSON = new MediaType("application", "json");
		APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);
		APPLICATION_NDJSON = new MediaType("application", "x-ndjson");
		APPLICATION_OCTET_STREAM = new MediaType("application", "octet-stream");
		APPLICATION_PDF = new MediaType("application", "pdf");
		APPLICATION_PROBLEM_JSON = new MediaType("application", "problem+json");
		APPLICATION_PROBLEM_JSON_UTF8 = new MediaType("application", "problem+json", StandardCharsets.UTF_8);
		APPLICATION_PROBLEM_XML = new MediaType("application", "problem+xml");
		APPLICATION_RSS_XML = new MediaType("application", "rss+xml");
		APPLICATION_STREAM_JSON = new MediaType("application", "stream+json");
		APPLICATION_XHTML_XML = new MediaType("application", "xhtml+xml");
		APPLICATION_XML = new MediaType("application", "xml");
		IMAGE_GIF = new MediaType("image", "gif");
		IMAGE_JPEG = new MediaType("image", "jpeg");
		IMAGE_PNG = new MediaType("image", "png");
		MULTIPART_FORM_DATA = new MediaType("multipart", "form-data");
		MULTIPART_MIXED = new MediaType("multipart", "mixed");
		MULTIPART_RELATED = new MediaType("multipart", "related");
		TEXT_EVENT_STREAM = new MediaType("text", "event-stream");
		TEXT_HTML = new MediaType("text", "html");
		TEXT_MARKDOWN = new MediaType("text", "markdown");
		TEXT_PLAIN = new MediaType("text", "plain");
		TEXT_XML = new MediaType("text", "xml");
	}


	/**
	 * 为给定的主类型创建一个新的 {@code MediaType}。
	 * <p>{@linkplain #getSubtype() subtype} 设置为 "&#42;"，参数为空。
	 * @param type 主类型
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type) {
		super(type);
	}

	/**
	 * 为给定的主类型和子类型创建一个新的 {@code MediaType}。
	 * <p>参数为空。
	 * @param type 主类型
	 * @param subtype 子类型
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype) {
		super(type, subtype, Collections.emptyMap());
	}

	/**
	 * 为给定的类型、子类型和字符集创建一个新的 {@code MediaType}。
	 * @param type 主类型
	 * @param subtype 子类型
	 * @param charset 字符集
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype, Charset charset) {
		super(type, subtype, charset);
	}

	/**
	 * 为给定的类型、子类型和质量值创建一个新的 {@code MediaType}。
	 * @param type 主类型
	 * @param subtype 子类型
	 * @param qualityValue 质量值
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype, double qualityValue) {
		this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
	}

	/**
	 * 复制构造函数，复制给定 {@code MediaType} 的类型、子类型和参数，并允许设置指定的字符集。
	 * @param other 另一个媒体类型
	 * @param charset 字符集
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 * @since 4.3
	 */
	public MediaType(MediaType other, Charset charset) {
		super(other, charset);
	}

	/**
	 * 复制构造函数，复制给定 {@code MediaType} 的类型和子类型，并允许使用不同的参数。
	 * @param other 另一个媒体类型
	 * @param parameters 参数，可能为 {@code null}
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(MediaType other, @Nullable Map<String, String> parameters) {
		super(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * 为给定的类型、子类型和参数创建一个新的 {@code MediaType}。
	 * @param type 主类型
	 * @param subtype 子类型
	 * @param parameters 参数，可能为 {@code null}
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type, String subtype, @Nullable Map<String, String> parameters) {
		super(type, subtype, parameters);
	}

	/**
	 * 为给定的 {@link MimeType} 创建一个新的 {@code MediaType}。类型、子类型和参数信息会被复制，并执行 {@code MediaType} 特有的参数检查。
	 * @param mimeType MIME 类型
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 * @since 5.3
	 */
	public MediaType(MimeType mimeType) {
		super(mimeType);
		getParameters().forEach(this::checkParameters);
	}


	@Override
	protected void checkParameters(String parameter, String value) {
		super.checkParameters(parameter, value);
		if (PARAM_QUALITY_FACTOR.equals(parameter)) {
			String unquotedValue = unquote(value);
			double d = Double.parseDouble(unquotedValue);
			Assert.isTrue(d >= 0D && d <= 1D,
					() -> "Invalid quality value \"" + unquotedValue + "\": should be between 0.0 and 1.0");
		}
	}

	/**
	 * 返回质量因子（由 {@code q} 参数表示，如果有）。默认为 {@code 1.0}。
	 * @return 以 double 值表示的质量因子
	 */
	public double getQualityValue() {
		String qualityFactor = getParameter(PARAM_QUALITY_FACTOR);
		return (qualityFactor != null ? Double.parseDouble(unquote(qualityFactor)) : 1D);
	}

	/**
	 * 指示此 {@code MediaType} 是否包含给定的媒体类型。
	 * <p>例如，{@code text/*} 包含 {@code text/plain} 和 {@code text/html}，{@code application/*+xml} 包含 {@code application/soap+xml} 等。此方法<b>不</b>对称。
	 * <p>此方法仅调用 {@link MimeType#includes(MimeType)}，但为了二进制向后兼容而声明为 {@code MediaType} 参数。
	 * @param other 用于比较的参考媒体类型
	 * @return 如果此媒体类型包含给定媒体类型，则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean includes(@Nullable MediaType other) {
		return super.includes(other);
	}

	/**
	 * 指示此 {@code MediaType} 是否与给定的媒体类型兼容。
	 * <p>例如，{@code text/*} 与 {@code text/plain}、{@code text/html} 兼容，反之亦然。实际上，此方法类似于 {@link #includes}，区别在于它<b>是</b>对称的。
	 * <p>此方法仅调用 {@link MimeType#isCompatibleWith(MimeType)}，但为了二进制向后兼容而声明为 {@code MediaType} 参数。
	 * @param other 用于比较的参考媒体类型
	 * @return 如果此媒体类型与给定媒体类型兼容，则返回 {@code true}；否则返回 {@code false}
	 */
	public boolean isCompatibleWith(@Nullable MediaType other) {
		return super.isCompatibleWith(other);
	}

	/**
	 * 返回此实例的副本，其中质量值取自给定的 {@code MediaType}。
	 * @return 如果给定 MediaType 没有质量值，则返回同一实例，否则返回新实例
	 */
	public MediaType copyQualityValue(MediaType mediaType) {
		if (!mediaType.getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<>(getParameters());
		params.put(PARAM_QUALITY_FACTOR, mediaType.getParameters().get(PARAM_QUALITY_FACTOR));
		return new MediaType(this, params);
	}

	/**
	 * 返回移除了质量值的此实例副本。
	 * @return 如果媒体类型不包含质量值，则返回同一实例，否则返回新实例
	 */
	public MediaType removeQualityValue() {
		if (!getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<>(getParameters());
		params.remove(PARAM_QUALITY_FACTOR);
		return new MediaType(this, params);
	}


	/**
	 * 将给定的字符串值解析为 {@code MediaType} 对象，方法名遵循 'valueOf' 命名约定（受 {@link org.springframework.core.convert.ConversionService} 支持）。
	 * @param value 要解析的字符串
	 * @throws InvalidMediaTypeException 如果媒体类型值无法解析
	 * @see #parseMediaType(String)
	 */
	public static MediaType valueOf(String value) {
		return parseMediaType(value);
	}

	/**
	 * 将给定字符串解析为单个 {@code MediaType}。
	 * @param mediaType 要解析的字符串
	 * @return 该媒体类型
	 * @throws InvalidMediaTypeException 如果媒体类型值无法解析
	 */
	public static MediaType parseMediaType(String mediaType) {
		MimeType type;
		try {
			type = MimeTypeUtils.parseMimeType(mediaType);
		}
		catch (InvalidMimeTypeException ex) {
			throw new InvalidMediaTypeException(ex);
		}
		try {
			return new MediaType(type);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidMediaTypeException(mediaType, ex.getMessage());
		}
	}

	/**
	 * 将逗号分隔的字符串解析为 {@code MediaType} 对象列表。
	 * <p>此方法可用于解析 Accept 或 Content-Type 头。
	 * @param mediaTypes 要解析的字符串
	 * @return 媒体类型列表
	 * @throws InvalidMediaTypeException 如果媒体类型值无法解析
	 */
	public static List<MediaType> parseMediaTypes(@Nullable String mediaTypes) {
		if (!StringUtils.hasLength(mediaTypes)) {
			return Collections.emptyList();
		}
		// Avoid using java.util.stream.Stream in hot paths
		List<String> tokenizedTypes = MimeTypeUtils.tokenize(mediaTypes);
		List<MediaType> result = new ArrayList<>(tokenizedTypes.size());
		for (String type : tokenizedTypes) {
			if (StringUtils.hasText(type)) {
				result.add(parseMediaType(type));
			}
		}
		return result;
	}

	/**
	 * 将给定的（可能以逗号分隔的）字符串列表解析为 {@code MediaType} 对象列表。
	 * <p>此方法可用于解析 Accept 或 Content-Type 头。
	 * @param mediaTypes 要解析的字符串
	 * @return 媒体类型列表
	 * @throws InvalidMediaTypeException 如果媒体类型值无法解析
	 * @since 4.3.2
	 */
	public static List<MediaType> parseMediaTypes(@Nullable List<String> mediaTypes) {
		if (CollectionUtils.isEmpty(mediaTypes)) {
			return Collections.emptyList();
		}
		else if (mediaTypes.size() == 1) {
			return parseMediaTypes(mediaTypes.get(0));
		}
		else {
			List<MediaType> result = new ArrayList<>(8);
			for (String mediaType : mediaTypes) {
				result.addAll(parseMediaTypes(mediaType));
			}
			return result;
		}
	}

	/**
	 * 将给定的 MIME 类型重新创建为媒体类型。
	 * @since 5.0
	 */
	public static List<MediaType> asMediaTypes(List<MimeType> mimeTypes) {
		List<MediaType> mediaTypes = new ArrayList<>(mimeTypes.size());
		for (MimeType mimeType : mimeTypes) {
			mediaTypes.add(MediaType.asMediaType(mimeType));
		}
		return mediaTypes;
	}

	/**
	 * 将给定的 MIME 类型重新创建为媒体类型。
	 * @since 5.0
	 */
	public static MediaType asMediaType(MimeType mimeType) {
		if (mimeType instanceof MediaType) {
			return (MediaType) mimeType;
		}
		return new MediaType(mimeType.getType(), mimeType.getSubtype(), mimeType.getParameters());
	}

	/**
	 * 返回给定 {@code MediaType} 对象列表的字符串表示形式。
	 * <p>此方法可用于生成 {@code Accept} 或 {@code Content-Type} 头。
	 * @param mediaTypes 要为其创建字符串表示的媒体类型
	 * @return 字符串表示
	 */
	public static String toString(Collection<MediaType> mediaTypes) {
		return MimeTypeUtils.toString(mediaTypes);
	}

	/**
	 * 按特定性对给定的 {@code MediaType} 对象列表进行排序。
	 * <p>给定两个媒体类型：
	 * <ol>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardType() wildcard type}，则不带通配符的媒体类型排在另一个之前。</li>
	 * <li>如果两个媒体类型具有不同的 {@linkplain #getType() types}，则认为它们相等，并保持当前顺序。</li>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardSubtype() wildcard subtype}，则不带通配符的媒体类型排在另一个之前。</li>
	 * <li>如果两个媒体类型具有不同的 {@linkplain #getSubtype() subtypes}，则认为它们相等，并保持当前顺序。</li>
	 * <li>如果两个媒体类型具有不同的 {@linkplain #getQualityValue() quality value}，则质量值最高的媒体类型排在另一个之前。</li>
	 * <li>如果两个媒体类型具有不同数量的 {@linkplain #getParameter(String) parameters}，则参数最多的媒体类型排在另一个之前。</li>
	 * </ol>
	 * <p>例如：
	 * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
	 * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
	 * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
	 * <blockquote>audio/basic == text/html</blockquote>
	 * <blockquote>audio/basic == audio/wave</blockquote>
	 * @param mediaTypes 要排序的媒体类型列表
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
	 * and Content, section 5.3.2</a>
	 */
	public static void sortBySpecificity(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			mediaTypes.sort(SPECIFICITY_COMPARATOR);
		}
	}

	/**
	 * 按质量值对给定的 {@code MediaType} 对象列表进行排序。
	 * <p>给定两个媒体类型：
	 * <ol>
	 * <li>如果两个媒体类型具有不同的 {@linkplain #getQualityValue() quality value}，则质量值最高的媒体类型排在另一个之前。</li>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardType() wildcard type}，则不带通配符的媒体类型排在另一个之前。</li>
	 * <li>如果两个媒体类型具有不同的 {@linkplain #getType() types}，则认为它们相等，并保持当前顺序。</li>
	 * <li>如果任一媒体类型具有 {@linkplain #isWildcardSubtype() wildcard subtype}，则不带通配符的媒体类型排在另一个之前。</li>
	 * <li>如果两个媒体类型具有不同的 {@linkplain #getSubtype() subtypes}，则认为它们相等，并保持当前顺序。</li>
	 * <li>如果两个媒体类型具有不同数量的 {@linkplain #getParameter(String) parameters}，则参数最多的媒体类型排在另一个之前。</li>
	 * </ol>
	 * @param mediaTypes 要排序的媒体类型列表
	 * @see #getQualityValue()
	 */
	public static void sortByQualityValue(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			mediaTypes.sort(QUALITY_VALUE_COMPARATOR);
		}
	}

	/**
	 * 按特定性作为主要标准、质量值作为次要标准对给定的 {@code MediaType} 对象列表进行排序。
	 * @see MediaType#sortBySpecificity(List)
	 * @see MediaType#sortByQualityValue(List)
	 */
	public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			mediaTypes.sort(MediaType.SPECIFICITY_COMPARATOR.thenComparing(MediaType.QUALITY_VALUE_COMPARATOR));
		}
	}


	/**
	 * 由 {@link #sortByQualityValue(List)} 使用的比较器。
	 */
	public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = (mediaType1, mediaType2) -> {
		double quality1 = mediaType1.getQualityValue();
		double quality2 = mediaType2.getQualityValue();
		int qualityComparison = Double.compare(quality2, quality1);
		if (qualityComparison != 0) {
			return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
		}
		else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) {  // */* < audio/*
			return 1;
		}
		else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) {  // audio/* > */*
			return -1;
		}
		else if (!mediaType1.getType().equals(mediaType2.getType())) {  // audio/basic == text/html
			return 0;
		}
		else {  // mediaType1.getType().equals(mediaType2.getType())
			if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) {  // audio/* < audio/basic
				return 1;
			}
			else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) {  // audio/basic > audio/*
				return -1;
			}
			else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) {  // audio/basic == audio/wave
				return 0;
			}
			else {
				int paramsSize1 = mediaType1.getParameters().size();
				int paramsSize2 = mediaType2.getParameters().size();
				return Integer.compare(paramsSize2, paramsSize1);  // audio/basic;level=1 < audio/basic
			}
		}
	};


	/**
	 * 由 {@link #sortBySpecificity(List)} 使用的比较器。
	 */
	public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new SpecificityComparator<MediaType>() {

		@Override
		protected int compareParameters(MediaType mediaType1, MediaType mediaType2) {
			double quality1 = mediaType1.getQualityValue();
			double quality2 = mediaType2.getQualityValue();
			int qualityComparison = Double.compare(quality2, quality1);
			if (qualityComparison != 0) {
				return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
			}
			return super.compareParameters(mediaType1, mediaType2);
		}
	};

}
