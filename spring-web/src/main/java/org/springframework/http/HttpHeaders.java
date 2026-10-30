/*
 * Copyright 2002-2021 the original author or authors.
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
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 表示 HTTP 请求或响应头的数据结构，将字符串头名称映射到字符串值列表，并为常见的应用级数据类型提供访问器。
 * <p>除了 {@link Map} 定义的常规方法外，此类还提供了许多常用的便捷方法，例如：
 * <ul>
 * <li>{@link #getFirst(String)} 返回与给定头名称关联的第一个值</li>
 * <li>{@link #add(String, String)} 将头值添加到某个头名称的值列表中</li>
 * <li>{@link #set(String, String)} 将头值设置为单个字符串值</li>
 * </ul>
 * <p>请注意，{@code HttpHeaders} 通常以不区分大小写的方式处理头名称。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Josh Long
 * @author Sam Brannen
 * @since 3.0
 */
public class HttpHeaders implements MultiValueMap<String, String>, Serializable {

	private static final long serialVersionUID = -8578554704772377436L;


	/**
	 * HTTP {@code Accept} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">Section 5.3.2 of RFC 7231</a>
	 */
	public static final String ACCEPT = "Accept";
	/**
	 * HTTP {@code Accept-Charset} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.3">Section 5.3.3 of RFC 7231</a>
	 */
	public static final String ACCEPT_CHARSET = "Accept-Charset";
	/**
	 * HTTP {@code Accept-Encoding} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.4">Section 5.3.4 of RFC 7231</a>
	 */
	public static final String ACCEPT_ENCODING = "Accept-Encoding";
	/**
	 * HTTP {@code Accept-Language} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.5">Section 5.3.5 of RFC 7231</a>
	 */
	public static final String ACCEPT_LANGUAGE = "Accept-Language";
	/**
	 * HTTP {@code Accept-Patch} 头字段名称。
	 * @since 5.3.6
	 * @see <a href="https://tools.ietf.org/html/rfc5789#section-3.1">Section 3.1 of RFC 5789</a>
	 */
	public static final String ACCEPT_PATCH = "Accept-Patch";
	/**
	 * HTTP {@code Accept-Ranges} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.3">Section 5.3.5 of RFC 7233</a>
	 */
	public static final String ACCEPT_RANGES = "Accept-Ranges";
	/**
	 * CORS {@code Access-Control-Allow-Credentials} 响应头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	/**
	 * CORS {@code Access-Control-Allow-Headers} 响应头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	/**
	 * CORS {@code Access-Control-Allow-Methods} 响应头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	/**
	 * CORS {@code Access-Control-Allow-Origin} 响应头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	/**
	 * CORS {@code Access-Control-Expose-Headers} 响应头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
	/**
	 * CORS {@code Access-Control-Max-Age} 响应头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	/**
	 * CORS {@code Access-Control-Request-Headers} 请求头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
	/**
	 * CORS {@code Access-Control-Request-Method} 请求头字段名称。
	 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
	 */
	public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
	/**
	 * HTTP {@code Age} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.1">Section 5.1 of RFC 7234</a>
	 */
	public static final String AGE = "Age";
	/**
	 * HTTP {@code Allow} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.1">Section 7.4.1 of RFC 7231</a>
	 */
	public static final String ALLOW = "Allow";
	/**
	 * HTTP {@code Authorization} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.2">Section 4.2 of RFC 7235</a>
	 */
	public static final String AUTHORIZATION = "Authorization";
	/**
	 * HTTP {@code Cache-Control} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">Section 5.2 of RFC 7234</a>
	 */
	public static final String CACHE_CONTROL = "Cache-Control";
	/**
	 * HTTP {@code Connection} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.1">Section 6.1 of RFC 7230</a>
	 */
	public static final String CONNECTION = "Connection";
	/**
	 * HTTP {@code Content-Encoding} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.2.2">Section 3.1.2.2 of RFC 7231</a>
	 */
	public static final String CONTENT_ENCODING = "Content-Encoding";
	/**
	 * HTTP {@code Content-Disposition} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
	 */
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	/**
	 * HTTP {@code Content-Language} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.3.2">Section 3.1.3.2 of RFC 7231</a>
	 */
	public static final String CONTENT_LANGUAGE = "Content-Language";
	/**
	 * HTTP {@code Content-Length} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.2">Section 3.3.2 of RFC 7230</a>
	 */
	public static final String CONTENT_LENGTH = "Content-Length";
	/**
	 * HTTP {@code Content-Location} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.4.2">Section 3.1.4.2 of RFC 7231</a>
	 */
	public static final String CONTENT_LOCATION = "Content-Location";
	/**
	 * HTTP {@code Content-Range} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.2">Section 4.2 of RFC 7233</a>
	 */
	public static final String CONTENT_RANGE = "Content-Range";
	/**
	 * HTTP {@code Content-Type} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
	 */
	public static final String CONTENT_TYPE = "Content-Type";
	/**
	 * HTTP {@code Cookie} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.3.4">Section 4.3.4 of RFC 2109</a>
	 */
	public static final String COOKIE = "Cookie";
	/**
	 * HTTP {@code Date} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">Section 7.1.1.2 of RFC 7231</a>
	 */
	public static final String DATE = "Date";
	/**
	 * HTTP {@code ETag} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
	 */
	public static final String ETAG = "ETag";
	/**
	 * HTTP {@code Expect} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.1">Section 5.1.1 of RFC 7231</a>
	 */
	public static final String EXPECT = "Expect";
	/**
	 * HTTP {@code Expires} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.3">Section 5.3 of RFC 7234</a>
	 */
	public static final String EXPIRES = "Expires";
	/**
	 * HTTP {@code From} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.1">Section 5.5.1 of RFC 7231</a>
	 */
	public static final String FROM = "From";
	/**
	 * HTTP {@code Host} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.4">Section 5.4 of RFC 7230</a>
	 */
	public static final String HOST = "Host";
	/**
	 * HTTP {@code If-Match} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.1">Section 3.1 of RFC 7232</a>
	 */
	public static final String IF_MATCH = "If-Match";
	/**
	 * HTTP {@code If-Modified-Since} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.3">Section 3.3 of RFC 7232</a>
	 */
	public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
	/**
	 * HTTP {@code If-None-Match} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.2">Section 3.2 of RFC 7232</a>
	 */
	public static final String IF_NONE_MATCH = "If-None-Match";
	/**
	 * HTTP {@code If-Range} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.2">Section 3.2 of RFC 7233</a>
	 */
	public static final String IF_RANGE = "If-Range";
	/**
	 * HTTP {@code If-Unmodified-Since} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-3.4">Section 3.4 of RFC 7232</a>
	 */
	public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
	/**
	 * HTTP {@code Last-Modified} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.2">Section 2.2 of RFC 7232</a>
	 */
	public static final String LAST_MODIFIED = "Last-Modified";
	/**
	 * HTTP {@code Link} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc5988">RFC 5988</a>
	 */
	public static final String LINK = "Link";
	/**
	 * HTTP {@code Location} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2">Section 7.1.2 of RFC 7231</a>
	 */
	public static final String LOCATION = "Location";
	/**
	 * HTTP {@code Max-Forwards} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.1.2">Section 5.1.2 of RFC 7231</a>
	 */
	public static final String MAX_FORWARDS = "Max-Forwards";
	/**
	 * HTTP {@code Origin} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
	 */
	public static final String ORIGIN = "Origin";
	/**
	 * HTTP {@code Pragma} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.4">Section 5.4 of RFC 7234</a>
	 */
	public static final String PRAGMA = "Pragma";
	/**
	 * HTTP {@code Proxy-Authenticate} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.3">Section 4.3 of RFC 7235</a>
	 */
	public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
	/**
	 * HTTP {@code Proxy-Authorization} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.4">Section 4.4 of RFC 7235</a>
	 */
	public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
	/**
	 * HTTP {@code Range} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-3.1">Section 3.1 of RFC 7233</a>
	 */
	public static final String RANGE = "Range";
	/**
	 * HTTP {@code Referer} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.2">Section 5.5.2 of RFC 7231</a>
	 */
	public static final String REFERER = "Referer";
	/**
	 * HTTP {@code Retry-After} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.3">Section 7.1.3 of RFC 7231</a>
	 */
	public static final String RETRY_AFTER = "Retry-After";
	/**
	 * HTTP {@code Server} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">Section 7.4.2 of RFC 7231</a>
	 */
	public static final String SERVER = "Server";
	/**
	 * HTTP {@code Set-Cookie} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc2109#section-4.2.2">Section 4.2.2 of RFC 2109</a>
	 */
	public static final String SET_COOKIE = "Set-Cookie";
	/**
	 * HTTP {@code Set-Cookie2} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc2965">RFC 2965</a>
	 */
	public static final String SET_COOKIE2 = "Set-Cookie2";
	/**
	 * HTTP {@code TE} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.3">Section 4.3 of RFC 7230</a>
	 */
	public static final String TE = "TE";
	/**
	 * HTTP {@code Trailer} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-4.4">Section 4.4 of RFC 7230</a>
	 */
	public static final String TRAILER = "Trailer";
	/**
	 * HTTP {@code Transfer-Encoding} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3.1">Section 3.3.1 of RFC 7230</a>
	 */
	public static final String TRANSFER_ENCODING = "Transfer-Encoding";
	/**
	 * HTTP {@code Upgrade} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-6.7">Section 6.7 of RFC 7230</a>
	 */
	public static final String UPGRADE = "Upgrade";
	/**
	 * HTTP {@code User-Agent} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.5.3">Section 5.5.3 of RFC 7231</a>
	 */
	public static final String USER_AGENT = "User-Agent";
	/**
	 * HTTP {@code Vary} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.4">Section 7.1.4 of RFC 7231</a>
	 */
	public static final String VARY = "Vary";
	/**
	 * HTTP {@code Via} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-5.7.1">Section 5.7.1 of RFC 7230</a>
	 */
	public static final String VIA = "Via";
	/**
	 * HTTP {@code Warning} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.5">Section 5.5 of RFC 7234</a>
	 */
	public static final String WARNING = "Warning";
	/**
	 * HTTP {@code WWW-Authenticate} 头字段名称。
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-4.1">Section 4.1 of RFC 7235</a>
	 */
	public static final String WWW_AUTHENTICATE = "WWW-Authenticate";


	/**
	 * 空的 {@code HttpHeaders} 实例（不可变）。
	 * @since 5.0
	 */
	public static final HttpHeaders EMPTY = new ReadOnlyHttpHeaders(new LinkedMultiValueMap<>());

	/**
	 * 匹配 "If-Match"、"If-None-Match" 等头中多个 ETag 字段值的模式。
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">Section 2.3 of RFC 7232</a>
	 */
	private static final Pattern ETAG_HEADER_VALUE_PATTERN = Pattern.compile("\\*|\\s*((W\\/)?(\"[^\"]*\"))\\s*,?");

	private static final DecimalFormatSymbols DECIMAL_FORMAT_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);

	private static final ZoneId GMT = ZoneId.of("GMT");

	/**
	 * 用于格式化的日期格式（带时区），按 HTTP RFC 规定。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).withZone(GMT);

	/**
	 * 用于解析的日期格式（带时区），按 HTTP RFC 规定。
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final DateTimeFormatter[] DATE_PARSERS = new DateTimeFormatter[] {
			DateTimeFormatter.RFC_1123_DATE_TIME,
			DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
			DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.US).withZone(GMT)
	};


	final MultiValueMap<String, String> headers;


	/**
	 * 构造一个新的空 {@code HttpHeaders} 对象实例。
	 * <p>这是常用构造函数，使用不区分大小写的映射结构。
	 */
	public HttpHeaders() {
		this(CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH)));
	}

	/**
	 * 构造一个由现有映射支持的新的 {@code HttpHeaders} 实例。
	 * <p>此构造函数作为优化提供，用于适配现有的头映射结构，主要供框架内部使用。
	 * @param headers 标头映射（应使用不区分大小写的键）
	 * @since 5.1
	 */
	public HttpHeaders(MultiValueMap<String, String> headers) {
		Assert.notNull(headers, "MultiValueMap must not be null");
		this.headers = headers;
	}


	/**
	 * 获取给定头名称的头值列表（如果有）。
	 * @param headerName 标头名称
	 * @return 标头值列表，或空列表
	 * @since 5.2
	 */
	public List<String> getOrEmpty(Object headerName) {
		List<String> values = get(headerName);
		return (values != null ? values : Collections.emptyList());
	}

	/**
	 * 设置可接受的 {@linkplain MediaType media types} 列表，由 {@code Accept} 头指定。
	 */
	public void setAccept(List<MediaType> acceptableMediaTypes) {
		set(ACCEPT, MediaType.toString(acceptableMediaTypes));
	}

	/**
	 * 返回可接受的 {@linkplain MediaType media types} 列表，由 {@code Accept} 头指定。
	 * <p>如果未指定可接受的媒体类型，则返回空列表。
	 */
	public List<MediaType> getAccept() {
		return MediaType.parseMediaTypes(get(ACCEPT));
	}

	/**
	 * 设置可接受的语言范围，由 {@literal Accept-Language} 头指定。
	 * @since 5.0
	 */
	public void setAcceptLanguage(List<Locale.LanguageRange> languages) {
		Assert.notNull(languages, "LanguageRange List must not be null");
		DecimalFormat decimal = new DecimalFormat("0.0", DECIMAL_FORMAT_SYMBOLS);
		List<String> values = languages.stream()
				.map(range ->
						range.getWeight() == Locale.LanguageRange.MAX_WEIGHT ?
								range.getRange() :
								range.getRange() + ";q=" + decimal.format(range.getWeight()))
				.collect(Collectors.toList());
		set(ACCEPT_LANGUAGE, toCommaDelimitedString(values));
	}

	/**
	 * 从 {@literal "Accept-Language"} 头返回语言范围。
	 * <p>如果你只需要排序后的首选区域设置，请使用 {@link #getAcceptLanguageAsLocales()}；如果需要根据受支持的区域设置列表进行过滤，可以将返回的列表传递给 {@link Locale#filter(List, Collection)}。
	 * @throws IllegalArgumentException 如果该值无法转换为语言范围
	 * @since 5.0
	 */
	public List<Locale.LanguageRange> getAcceptLanguage() {
		String value = getFirst(ACCEPT_LANGUAGE);
		return (StringUtils.hasText(value) ? Locale.LanguageRange.parse(value) : Collections.emptyList());
	}

	/**
	 * 使用 {@link Locale} 的 {@link #setAcceptLanguage(List)} 变体。
	 * @since 5.0
	 */
	public void setAcceptLanguageAsLocales(List<Locale> locales) {
		setAcceptLanguage(locales.stream()
				.map(locale -> new Locale.LanguageRange(locale.toLanguageTag()))
				.collect(Collectors.toList()));
	}

	/**
	 * {@link #getAcceptLanguage()} 的变体，将每个 {@link java.util.Locale.LanguageRange} 转换为 {@link Locale}。
	 * @return 区域设置列表，或空列表
	 * @throws IllegalArgumentException 如果该值无法转换为区域设置
	 * @since 5.0
	 */
	public List<Locale> getAcceptLanguageAsLocales() {
		List<Locale.LanguageRange> ranges = getAcceptLanguage();
		if (ranges.isEmpty()) {
			return Collections.emptyList();
		}
		return ranges.stream()
				.map(range -> Locale.forLanguageTag(range.getRange()))
				.filter(locale -> StringUtils.hasText(locale.getDisplayName()))
				.collect(Collectors.toList());
	}

	/**
	 * 设置 {@code PATCH} 方法的可接受 {@linkplain MediaType media types} 列表，由 {@code Accept-Patch} 头指定。
	 * @since 5.3.6
	 */
	public void setAcceptPatch(List<MediaType> mediaTypes) {
		set(ACCEPT_PATCH, MediaType.toString(mediaTypes));
	}

	/**
	 * 返回 {@code PATCH} 方法的可接受 {@linkplain MediaType media types} 列表，由 {@code Accept-Patch} 头指定。
	 * <p>如果未指定可接受的媒体类型，则返回空列表。
	 * @since 5.3.6
	 */
	public List<MediaType> getAcceptPatch() {
		return MediaType.parseMediaTypes(get(ACCEPT_PATCH));
	}

	/**
	 * 设置 {@code Access-Control-Allow-Credentials} 响应头的（新）值。
	 */
	public void setAccessControlAllowCredentials(boolean allowCredentials) {
		set(ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.toString(allowCredentials));
	}

	/**
	 * 返回 {@code Access-Control-Allow-Credentials} 响应头的值。
	 */
	public boolean getAccessControlAllowCredentials() {
		return Boolean.parseBoolean(getFirst(ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	/**
	 * 设置 {@code Access-Control-Allow-Headers} 响应头的（新）值。
	 */
	public void setAccessControlAllowHeaders(List<String> allowedHeaders) {
		set(ACCESS_CONTROL_ALLOW_HEADERS, toCommaDelimitedString(allowedHeaders));
	}

	/**
	 * 返回 {@code Access-Control-Allow-Headers} 响应头的值。
	 */
	public List<String> getAccessControlAllowHeaders() {
		return getValuesAsList(ACCESS_CONTROL_ALLOW_HEADERS);
	}

	/**
	 * 设置 {@code Access-Control-Allow-Methods} 响应头的（新）值。
	 */
	public void setAccessControlAllowMethods(List<HttpMethod> allowedMethods) {
		set(ACCESS_CONTROL_ALLOW_METHODS, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * 返回 {@code Access-Control-Allow-Methods} 响应头的值。
	 */
	public List<HttpMethod> getAccessControlAllowMethods() {
		List<HttpMethod> result = new ArrayList<>();
		String value = getFirst(ACCESS_CONTROL_ALLOW_METHODS);
		if (value != null) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			for (String token : tokens) {
				HttpMethod resolved = HttpMethod.resolve(token);
				if (resolved != null) {
					result.add(resolved);
				}
			}
		}
		return result;
	}

	/**
	 * 设置 {@code Access-Control-Allow-Origin} 响应头的（新）值。
	 */
	public void setAccessControlAllowOrigin(@Nullable String allowedOrigin) {
		setOrRemove(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
	}

	/**
	 * 返回 {@code Access-Control-Allow-Origin} 响应头的值。
	 */
	@Nullable
	public String getAccessControlAllowOrigin() {
		return getFieldValues(ACCESS_CONTROL_ALLOW_ORIGIN);
	}

	/**
	 * 设置 {@code Access-Control-Expose-Headers} 响应头的（新）值。
	 */
	public void setAccessControlExposeHeaders(List<String> exposedHeaders) {
		set(ACCESS_CONTROL_EXPOSE_HEADERS, toCommaDelimitedString(exposedHeaders));
	}

	/**
	 * 返回 {@code Access-Control-Expose-Headers} 响应头的值。
	 */
	public List<String> getAccessControlExposeHeaders() {
		return getValuesAsList(ACCESS_CONTROL_EXPOSE_HEADERS);
	}

	/**
	 * 设置 {@code Access-Control-Max-Age} 响应头的（新）值。
	 * @since 5.2
	 */
	public void setAccessControlMaxAge(Duration maxAge) {
		set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge.getSeconds()));
	}

	/**
	 * 设置 {@code Access-Control-Max-Age} 响应头的（新）值。
	 */
	public void setAccessControlMaxAge(long maxAge) {
		set(ACCESS_CONTROL_MAX_AGE, Long.toString(maxAge));
	}

	/**
	 * 返回 {@code Access-Control-Max-Age} 响应头的值。
	 * <p>当最大年龄未知时返回 -1。
	 */
	public long getAccessControlMaxAge() {
		String value = getFirst(ACCESS_CONTROL_MAX_AGE);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * 设置 {@code Access-Control-Request-Headers} 请求头的（新）值。
	 */
	public void setAccessControlRequestHeaders(List<String> requestHeaders) {
		set(ACCESS_CONTROL_REQUEST_HEADERS, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * 返回 {@code Access-Control-Request-Headers} 请求头的值。
	 */
	public List<String> getAccessControlRequestHeaders() {
		return getValuesAsList(ACCESS_CONTROL_REQUEST_HEADERS);
	}

	/**
	 * 设置 {@code Access-Control-Request-Method} 请求头的（新）值。
	 */
	public void setAccessControlRequestMethod(@Nullable HttpMethod requestMethod) {
		setOrRemove(ACCESS_CONTROL_REQUEST_METHOD, (requestMethod != null ? requestMethod.name() : null));
	}

	/**
	 * 返回 {@code Access-Control-Request-Method} 请求头的值。
	 */
	@Nullable
	public HttpMethod getAccessControlRequestMethod() {
		return HttpMethod.resolve(getFirst(ACCESS_CONTROL_REQUEST_METHOD));
	}

	/**
	 * 设置可接受的 {@linkplain Charset charsets} 列表，由 {@code Accept-Charset} 头指定。
	 */
	public void setAcceptCharset(List<Charset> acceptableCharsets) {
		StringJoiner joiner = new StringJoiner(", ");
		for (Charset charset : acceptableCharsets) {
			joiner.add(charset.name().toLowerCase(Locale.ENGLISH));
		}
		set(ACCEPT_CHARSET, joiner.toString());
	}

	/**
	 * 返回可接受的 {@linkplain Charset charsets} 列表，由 {@code Accept-Charset} 头指定。
	 */
	public List<Charset> getAcceptCharset() {
		String value = getFirst(ACCEPT_CHARSET);
		if (value != null) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			List<Charset> result = new ArrayList<>(tokens.length);
			for (String token : tokens) {
				int paramIdx = token.indexOf(';');
				String charsetName;
				if (paramIdx == -1) {
					charsetName = token;
				}
				else {
					charsetName = token.substring(0, paramIdx);
				}
				if (!charsetName.equals("*")) {
					result.add(Charset.forName(charsetName));
				}
			}
			return result;
		}
		else {
			return Collections.emptyList();
		}
	}

	/**
	 * 设置允许的 {@link HttpMethod HTTP methods} 集合，由 {@code Allow} 头指定。
	 */
	public void setAllow(Set<HttpMethod> allowedMethods) {
		set(ALLOW, StringUtils.collectionToCommaDelimitedString(allowedMethods));
	}

	/**
	 * 返回允许的 {@link HttpMethod HTTP methods} 集合，由 {@code Allow} 头指定。
	 * <p>如果未指定允许的方法，则返回空集合。
	 */
	public Set<HttpMethod> getAllow() {
		String value = getFirst(ALLOW);
		if (StringUtils.hasLength(value)) {
			String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
			List<HttpMethod> result = new ArrayList<>(tokens.length);
			for (String token : tokens) {
				HttpMethod resolved = HttpMethod.resolve(token);
				if (resolved != null) {
					result.add(resolved);
				}
			}
			return EnumSet.copyOf(result);
		}
		else {
			return EnumSet.noneOf(HttpMethod.class);
		}
	}

	/**
	 * 根据给定的用户名和密码，将 {@linkplain #AUTHORIZATION Authorization} 头的值设置为基本认证。
	 * <p>请注意，此方法仅支持 {@link StandardCharsets#ISO_8859_1 ISO-8859-1} 字符集中的字符。
	 * @param username 用户名
	 * @param password 密码
	 * @throws IllegalArgumentException 如果 {@code user} 或 {@code password} 包含无法编码为 ISO-8859-1 的字符
	 * @since 5.1
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public void setBasicAuth(String username, String password) {
		setBasicAuth(username, password, null);
	}

	/**
	 * 根据给定的用户名和密码，将 {@linkplain #AUTHORIZATION Authorization} 头的值设置为基本认证。
	 * @param username 用户名
	 * @param password 密码
	 * @param charset 用于将凭证转换为八位字节序列的字符集。
	 * 默认为 {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}。
	 * @throws IllegalArgumentException 如果 {@code username} 或 {@code password} 包含无法编码为给定字符集的字符
	 * @since 5.1
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public void setBasicAuth(String username, String password, @Nullable Charset charset) {
		setBasicAuth(encodeBasicAuth(username, password, charset));
	}

	/**
	 * 根据给定的 {@linkplain #encodeBasicAuth 编码凭据}，将 {@linkplain #AUTHORIZATION Authorization} 头的值设置为基本认证。
	 * <p>如果希望缓存编码后的凭据，请优先使用此方法而不是 {@link #setBasicAuth(String, String)} 和 {@link #setBasicAuth(String, String, Charset)}。
	 * @param encodedCredentials 编码后的凭证
	 * @throws IllegalArgumentException 如果提供的凭证字符串为 {@code null} 或空白
	 * @since 5.2
	 * @see #setBasicAuth(String, String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see #encodeBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public void setBasicAuth(String encodedCredentials) {
		Assert.hasText(encodedCredentials, "'encodedCredentials' must not be null or blank");
		set(AUTHORIZATION, "Basic " + encodedCredentials);
	}

	/**
	 * 将 {@linkplain #AUTHORIZATION Authorization} 头的值设置为给定的 Bearer 令牌。
	 * @param token Base64 编码的令牌
	 * @since 5.1
	 * @see <a href="https://tools.ietf.org/html/rfc6750">RFC 6750</a>
	 */
	public void setBearerAuth(String token) {
		set(AUTHORIZATION, "Bearer " + token);
	}

	/**
	 * 将配置的 {@link CacheControl} 实例设置为 {@code Cache-Control} 头的新值。
	 * @since 5.0.5
	 */
	public void setCacheControl(CacheControl cacheControl) {
		setOrRemove(CACHE_CONTROL, cacheControl.getHeaderValue());
	}

	/**
	 * 设置 {@code Cache-Control} 头的（新）值。
	 */
	public void setCacheControl(@Nullable String cacheControl) {
		setOrRemove(CACHE_CONTROL, cacheControl);
	}

	/**
	 * 返回 {@code Cache-Control} 头的值。
	 */
	@Nullable
	public String getCacheControl() {
		return getFieldValues(CACHE_CONTROL);
	}

	/**
	 * 设置 {@code Connection} 头的（新）值。
	 */
	public void setConnection(String connection) {
		set(CONNECTION, connection);
	}

	/**
	 * 设置 {@code Connection} 头的（新）值。
	 */
	public void setConnection(List<String> connection) {
		set(CONNECTION, toCommaDelimitedString(connection));
	}

	/**
	 * 返回 {@code Connection} 头的值。
	 */
	public List<String> getConnection() {
		return getValuesAsList(CONNECTION);
	}

	/**
	 * 在创建 {@code "multipart/form-data"} 请求时设置 {@code Content-Disposition} 头。
	 * <p>应用程序通常不会直接设置此头，而是准备一个 {@code MultiValueMap<String, Object>}，为每个部分包含一个 Object 或 {@link org.springframework.core.io.Resource}，然后将其传递给 {@code RestTemplate} 或 {@code WebClient}。
	 * @param name 控件名称
	 * @param filename 文件名（可能为 {@code null}）
	 * @see #getContentDisposition()
	 */
	public void setContentDispositionFormData(String name, @Nullable String filename) {
		Assert.notNull(name, "Name must not be null");
		ContentDisposition.Builder disposition = ContentDisposition.formData().name(name);
		if (StringUtils.hasText(filename)) {
			disposition.filename(filename);
		}
		setContentDisposition(disposition.build());
	}

	/**
	 * 设置 {@literal Content-Disposition} 头。
	 * <p>这可用于响应，以指示内容是预期在浏览器中内联显示，还是作为附件在本地保存。
	 * <p>它也可用于 {@code "multipart/form-data"} 请求。有关更多详细信息，请参见 {@link #setContentDispositionFormData} 的说明。
	 * @since 5.0
	 * @see #getContentDisposition()
	 */
	public void setContentDisposition(ContentDisposition contentDisposition) {
		set(CONTENT_DISPOSITION, contentDisposition.toString());
	}

	/**
	 * 返回 {@literal Content-Disposition} 头的解析表示形式。
	 * @since 5.0
	 * @see #setContentDisposition(ContentDisposition)
	 */
	public ContentDisposition getContentDisposition() {
		String contentDisposition = getFirst(CONTENT_DISPOSITION);
		if (StringUtils.hasText(contentDisposition)) {
			return ContentDisposition.parse(contentDisposition);
		}
		return ContentDisposition.empty();
	}

	/**
	 * 设置内容语言的 {@link Locale}，由 {@literal Content-Language} 头指定。
	 * <p>如果需要设置多个内容语言，请使用 {@code put(CONTENT_LANGUAGE, list)}。</p>
	 * @since 5.0
	 */
	public void setContentLanguage(@Nullable Locale locale) {
		setOrRemove(CONTENT_LANGUAGE, (locale != null ? locale.toLanguageTag() : null));
	}

	/**
	 * 获取内容语言的第一个 {@link Locale}，由 {@code Content-Language} 头指定。
	 * <p>如果需要获取多个内容语言，请使用 {@link #getValuesAsList(String)}。
	 * @return 内容语言的第一个 {@code Locale}，如果未知则为 {@code null}
	 * @since 5.0
	 */
	@Nullable
	public Locale getContentLanguage() {
		return getValuesAsList(CONTENT_LANGUAGE)
				.stream()
				.findFirst()
				.map(Locale::forLanguageTag)
				.orElse(null);
	}

	/**
	 * 设置正文的字节长度，由 {@code Content-Length} 头指定。
	 */
	public void setContentLength(long contentLength) {
		set(CONTENT_LENGTH, Long.toString(contentLength));
	}

	/**
	 * 返回正文的字节长度，由 {@code Content-Length} 头指定。
	 * <p>当内容长度未知时返回 -1。
	 */
	public long getContentLength() {
		String value = getFirst(CONTENT_LENGTH);
		return (value != null ? Long.parseLong(value) : -1);
	}

	/**
	 * 设置正文的 {@linkplain MediaType media type}，由 {@code Content-Type} 头指定。
	 */
	public void setContentType(@Nullable MediaType mediaType) {
		if (mediaType != null) {
			Assert.isTrue(!mediaType.isWildcardType(), "Content-Type cannot contain wildcard type '*'");
			Assert.isTrue(!mediaType.isWildcardSubtype(), "Content-Type cannot contain wildcard subtype '*'");
			set(CONTENT_TYPE, mediaType.toString());
		}
		else {
			remove(CONTENT_TYPE);
		}
	}

	/**
	 * 返回正文的 {@linkplain MediaType media type}，由 {@code Content-Type} 头指定。
	 * <p>当未设置 {@code Content-Type} 头时返回 {@code null}。
	 * @throws InvalidMediaTypeException 如果媒体类型值无法解析
	 */
	@Nullable
	public MediaType getContentType() {
		String value = getFirst(CONTENT_TYPE);
		return (StringUtils.hasLength(value) ? MediaType.parseMediaType(value) : null);
	}

	/**
	 * 设置消息创建时的日期和时间，由 {@code Date} 头指定。
	 * @since 5.2
	 */
	public void setDate(ZonedDateTime date) {
		setZonedDateTime(DATE, date);
	}

	/**
	 * 设置消息创建时的日期和时间，由 {@code Date} 头指定。
	 * @since 5.2
	 */
	public void setDate(Instant date) {
		setInstant(DATE, date);
	}

	/**
	 * 设置消息创建时的日期和时间，由 {@code Date} 头指定。
	 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
	 */
	public void setDate(long date) {
		setDate(DATE, date);
	}

	/**
	 * 返回消息创建时的日期和时间，由 {@code Date} 头指定。
	 * <p>日期以自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数返回。当日期未知时返回 -1。
	 * @throws IllegalArgumentException 如果该值无法转换为日期
	 */
	public long getDate() {
		return getFirstDate(DATE);
	}

	/**
	 * 设置正文的（新）实体标签，由 {@code ETag} 头指定。
	 */
	public void setETag(@Nullable String etag) {
		if (etag != null) {
			Assert.isTrue(etag.startsWith("\"") || etag.startsWith("W/"),
					"Invalid ETag: does not start with W/ or \"");
			Assert.isTrue(etag.endsWith("\""), "Invalid ETag: does not end with \"");
			set(ETAG, etag);
		}
		else {
			remove(ETAG);
		}
	}

	/**
	 * 返回正文的实体标签，由 {@code ETag} 头指定。
	 */
	@Nullable
	public String getETag() {
		return getFirst(ETAG);
	}

	/**
	 * 设置消息不再有效之前的持续时间，由 {@code Expires} 头指定。
	 * @since 5.0.5
	 */
	public void setExpires(ZonedDateTime expires) {
		setZonedDateTime(EXPIRES, expires);
	}

	/**
	 * 设置消息不再有效的日期和时间，由 {@code Expires} 头指定。
	 * @since 5.2
	 */
	public void setExpires(Instant expires) {
		setInstant(EXPIRES, expires);
	}

	/**
	 * 设置消息不再有效的日期和时间，由 {@code Expires} 头指定。
	 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
	 */
	public void setExpires(long expires) {
		setDate(EXPIRES, expires);
	}

	/**
	 * 返回消息不再有效的日期和时间，由 {@code Expires} 头指定。
	 * <p>日期以自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数返回。当日期未知时返回 -1。
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getExpires() {
		return getFirstDate(EXPIRES, false);
	}

	/**
	 * 设置 {@code Host} 头的（新）值。
	 * <p>如果给定 {@linkplain InetSocketAddress#getPort() port} 为 {@code 0}，则 Host 头将仅包含 {@linkplain InetSocketAddress#getHostString() host name}。
	 * @since 5.0
	 */
	public void setHost(@Nullable InetSocketAddress host) {
		if (host != null) {
			String value = host.getHostString();
			int port = host.getPort();
			if (port != 0) {
				value = value + ":" + port;
			}
			set(HOST, value);
		}
		else {
			remove(HOST, null);
		}
	}

	/**
	 * 返回 {@code Host} 头的值（如果可用）。
	 * <p>如果头值不包含端口，则返回地址中的 {@linkplain InetSocketAddress#getPort() port} 将为 {@code 0}。
	 * @since 5.0
	 */
	@Nullable
	public InetSocketAddress getHost() {
		String value = getFirst(HOST);
		if (value == null) {
			return null;
		}

		String host = null;
		int port = 0;
		int separator = (value.startsWith("[") ? value.indexOf(':', value.indexOf(']')) : value.lastIndexOf(':'));
		if (separator != -1) {
			host = value.substring(0, separator);
			String portString = value.substring(separator + 1);
			try {
				port = Integer.parseInt(portString);
			}
			catch (NumberFormatException ex) {
				// ignore
			}
		}

		if (host == null) {
			host = value;
		}
		return InetSocketAddress.createUnresolved(host, port);
	}

	/**
	 * 设置 {@code If-Match} 头的（新）值。
	 * @since 4.3
	 */
	public void setIfMatch(String ifMatch) {
		set(IF_MATCH, ifMatch);
	}

	/**
	 * 设置 {@code If-Match} 头的（新）值。
	 * @since 4.3
	 */
	public void setIfMatch(List<String> ifMatchList) {
		set(IF_MATCH, toCommaDelimitedString(ifMatchList));
	}

	/**
	 * 返回 {@code If-Match} 头的值。
	 * @throws IllegalArgumentException 如果解析失败
	 * @since 4.3
	 */
	public List<String> getIfMatch() {
		return getETagValuesAsList(IF_MATCH);
	}

	/**
	 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * @since 5.1.4
	 */
	public void setIfModifiedSince(ZonedDateTime ifModifiedSince) {
		setZonedDateTime(IF_MODIFIED_SINCE, ifModifiedSince.withZoneSameInstant(GMT));
	}

	/**
	 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * @since 5.1.4
	 */
	public void setIfModifiedSince(Instant ifModifiedSince) {
		setInstant(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * 设置 {@code If-Modified-Since} 头的（新）值。
	 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
	 */
	public void setIfModifiedSince(long ifModifiedSince) {
		setDate(IF_MODIFIED_SINCE, ifModifiedSince);
	}

	/**
	 * 返回 {@code If-Modified-Since} 头的值。
	 * <p>日期以自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数返回。当日期未知时返回 -1。
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getIfModifiedSince() {
		return getFirstDate(IF_MODIFIED_SINCE, false);
	}

	/**
	 * 设置 {@code If-None-Match} 头的（新）值。
	 */
	public void setIfNoneMatch(String ifNoneMatch) {
		set(IF_NONE_MATCH, ifNoneMatch);
	}

	/**
	 * 设置 {@code If-None-Match} 头的（新）值。
	 */
	public void setIfNoneMatch(List<String> ifNoneMatchList) {
		set(IF_NONE_MATCH, toCommaDelimitedString(ifNoneMatchList));
	}

	/**
	 * 返回 {@code If-None-Match} 头的值。
	 * @throws IllegalArgumentException 如果解析失败
	 */
	public List<String> getIfNoneMatch() {
		return getETagValuesAsList(IF_NONE_MATCH);
	}

	/**
	 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * @since 5.1.4
	 */
	public void setIfUnmodifiedSince(ZonedDateTime ifUnmodifiedSince) {
		setZonedDateTime(IF_UNMODIFIED_SINCE, ifUnmodifiedSince.withZoneSameInstant(GMT));
	}

	/**
	 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * @since 5.1.4
	 */
	public void setIfUnmodifiedSince(Instant ifUnmodifiedSince) {
		setInstant(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
	}

	/**
	 * 设置 {@code If-Unmodified-Since} 头的（新）值。
	 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
	 * @since 4.3
	 */
	public void setIfUnmodifiedSince(long ifUnmodifiedSince) {
		setDate(IF_UNMODIFIED_SINCE, ifUnmodifiedSince);
	}

	/**
	 * 返回 {@code If-Unmodified-Since} 头的值。
	 * <p>日期以自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数返回。当日期未知时返回 -1。
	 * @since 4.3
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getIfUnmodifiedSince() {
		return getFirstDate(IF_UNMODIFIED_SINCE, false);
	}

	/**
	 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * @since 5.1.4
	 */
	public void setLastModified(ZonedDateTime lastModified) {
		setZonedDateTime(LAST_MODIFIED, lastModified.withZoneSameInstant(GMT));
	}

	/**
	 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * @since 5.1.4
	 */
	public void setLastModified(Instant lastModified) {
		setInstant(LAST_MODIFIED, lastModified);
	}

	/**
	 * 设置资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
	 */
	public void setLastModified(long lastModified) {
		setDate(LAST_MODIFIED, lastModified);
	}

	/**
	 * 返回资源最后更改的时间，由 {@code Last-Modified} 头指定。
	 * <p>日期以自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数返回。当日期未知时返回 -1。
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getLastModified() {
		return getFirstDate(LAST_MODIFIED, false);
	}

	/**
	 * 设置资源的（新）位置，由 {@code Location} 头指定。
	 */
	public void setLocation(@Nullable URI location) {
		setOrRemove(LOCATION, (location != null ? location.toASCIIString() : null));
	}

	/**
	 * 返回资源的（新）位置，由 {@code Location} 头指定。
	 * <p>当位置未知时返回 {@code null}。
	 */
	@Nullable
	public URI getLocation() {
		String value = getFirst(LOCATION);
		return (value != null ? URI.create(value) : null);
	}

	/**
	 * 设置 {@code Origin} 头的（新）值。
	 */
	public void setOrigin(@Nullable String origin) {
		setOrRemove(ORIGIN, origin);
	}

	/**
	 * 返回 {@code Origin} 头的值。
	 */
	@Nullable
	public String getOrigin() {
		return getFirst(ORIGIN);
	}

	/**
	 * 设置 {@code Pragma} 头的（新）值。
	 */
	public void setPragma(@Nullable String pragma) {
		setOrRemove(PRAGMA, pragma);
	}

	/**
	 * 返回 {@code Pragma} 头的值。
	 */
	@Nullable
	public String getPragma() {
		return getFirst(PRAGMA);
	}

	/**
	 * 设置 {@code Range} 头的（新）值。
	 */
	public void setRange(List<HttpRange> ranges) {
		String value = HttpRange.toString(ranges);
		set(RANGE, value);
	}

	/**
	 * 返回 {@code Range} 头的值。
	 * <p>当范围未知时返回空列表。
	 */
	public List<HttpRange> getRange() {
		String value = getFirst(RANGE);
		return HttpRange.parseRanges(value);
	}

	/**
	 * 设置 {@code Upgrade} 头的（新）值。
	 */
	public void setUpgrade(@Nullable String upgrade) {
		setOrRemove(UPGRADE, upgrade);
	}

	/**
	 * 返回 {@code Upgrade} 头的值。
	 */
	@Nullable
	public String getUpgrade() {
		return getFirst(UPGRADE);
	}

	/**
	 * 设置请求头名称（例如 "Accept-Language"），响应将根据这些请求头的值进行内容协商并产生差异。
	 * @param requestHeaders 请求标头名称
	 * @since 4.3
	 */
	public void setVary(List<String> requestHeaders) {
		set(VARY, toCommaDelimitedString(requestHeaders));
	}

	/**
	 * 返回参与内容协商的请求头名称。
	 * @since 4.3
	 */
	public List<String> getVary() {
		return getValuesAsList(VARY);
	}

	/**
	 * 使用 RFC-1123 日期时间格式化器将给定日期格式化为字符串后，将其设置到给定头名称下。等同于 {@link #set(String, String)}，但用于日期头。
	 * @since 5.0
	 */
	public void setZonedDateTime(String headerName, ZonedDateTime date) {
		set(headerName, DATE_FORMATTER.format(date));
	}

	/**
	 * 使用 RFC-1123 日期时间格式化器将给定日期格式化为字符串后，将其设置到给定头名称下。等同于 {@link #set(String, String)}，但用于日期头。
	 * @since 5.1.4
	 */
	public void setInstant(String headerName, Instant date) {
		setZonedDateTime(headerName, ZonedDateTime.ofInstant(date, GMT));
	}

	/**
	 * 使用 RFC-1123 日期时间格式化器将给定日期格式化为字符串后，将其设置到给定头名称下。等同于 {@link #set(String, String)}，但用于日期头。
	 * @since 3.2.4
	 * @see #setZonedDateTime(String, ZonedDateTime)
	 */
	public void setDate(String headerName, long date) {
		setInstant(headerName, Instant.ofEpochMilli(date));
	}

	/**
	 * 将给定头名称的第一个头值解析为日期；如果没有值，则返回 -1；如果该值无法解析为日期，则抛出 {@link IllegalArgumentException}。
	 * @param headerName 标头名称
	 * @return 解析后的日期标头，若不存在则为 -1
	 * @since 3.2.4
	 * @see #getFirstZonedDateTime(String)
	 */
	public long getFirstDate(String headerName) {
		return getFirstDate(headerName, true);
	}

	/**
	 * 将给定头名称的第一个头值解析为日期；如果没有值，或在值无效时（如果 {@code rejectInvalid=false}），返回 -1；如果该值无法解析为日期，则抛出 {@link IllegalArgumentException}。
	 * @param headerName 标头名称
	 * @param rejectInvalid 是否对无效值抛出 {@link IllegalArgumentException}（{@code true}），
	 * 还是在这种情况下返回 -1（{@code false}）
	 * @return 解析后的日期标头，若不存在（或无效）则为 -1
	 * @see #getFirstZonedDateTime(String, boolean)
	 */
	private long getFirstDate(String headerName, boolean rejectInvalid) {
		ZonedDateTime zonedDateTime = getFirstZonedDateTime(headerName, rejectInvalid);
		return (zonedDateTime != null ? zonedDateTime.toInstant().toEpochMilli() : -1);
	}

	/**
	 * 将给定头名称的第一个头值解析为日期；如果没有值，则返回 {@code null}；如果该值无法解析为日期，则抛出 {@link IllegalArgumentException}。
	 * @param headerName 标头名称
	 * @return 解析后的日期标头，若不存在则为 {@code null}
	 * @since 5.0
	 */
	@Nullable
	public ZonedDateTime getFirstZonedDateTime(String headerName) {
		return getFirstZonedDateTime(headerName, true);
	}

	/**
	 * 将给定头名称的第一个头值解析为日期；如果没有值，或在值无效时（如果 {@code rejectInvalid=false}），返回 {@code null}；如果该值无法解析为日期，则抛出 {@link IllegalArgumentException}。
	 * @param headerName 标头名称
	 * @param rejectInvalid 是否对无效值抛出 {@link IllegalArgumentException}（{@code true}），
	 * 还是在这种情况下返回 {@code null}（{@code false}）
	 * @return 解析后的日期标头，若不存在（或无效）则为 {@code null}
	 */
	@Nullable
	private ZonedDateTime getFirstZonedDateTime(String headerName, boolean rejectInvalid) {
		String headerValue = getFirst(headerName);
		if (headerValue == null) {
			// No header value sent at all
			return null;
		}
		if (headerValue.length() >= 3) {
			// Short "0" or "-1" like values are never valid HTTP date headers...
			// Let's only bother with DateTimeFormatter parsing for long enough values.

			// See https://stackoverflow.com/questions/12626699/if-modified-since-http-header-passed-by-ie9-includes-length
			int parametersIndex = headerValue.indexOf(';');
			if (parametersIndex != -1) {
				headerValue = headerValue.substring(0, parametersIndex);
			}

			for (DateTimeFormatter dateFormatter : DATE_PARSERS) {
				try {
					return ZonedDateTime.parse(headerValue, dateFormatter);
				}
				catch (DateTimeParseException ex) {
					// ignore
				}
			}

		}
		if (rejectInvalid) {
			throw new IllegalArgumentException("Cannot parse date value \"" + headerValue +
					"\" for \"" + headerName + "\" header");
		}
		return null;
	}

	/**
	 * 返回给定头名称的所有值，即使该头被多次设置。
	 * @param headerName 标头名称
	 * @return 所有关联的值
	 * @since 4.3
	 */
	public List<String> getValuesAsList(String headerName) {
		List<String> values = get(headerName);
		if (values != null) {
			List<String> result = new ArrayList<>();
			for (String value : values) {
				if (value != null) {
					Collections.addAll(result, StringUtils.tokenizeToStringArray(value, ","));
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * 移除已知的 {@code "Content-*"} HTTP 头。
	 * <p>如果预期的正文因错误而无法写入，则应从响应中清除此类头。
	 * @since 5.2.3
	 */
	public void clearContentHeaders() {
		this.headers.remove(HttpHeaders.CONTENT_DISPOSITION);
		this.headers.remove(HttpHeaders.CONTENT_ENCODING);
		this.headers.remove(HttpHeaders.CONTENT_LANGUAGE);
		this.headers.remove(HttpHeaders.CONTENT_LENGTH);
		this.headers.remove(HttpHeaders.CONTENT_LOCATION);
		this.headers.remove(HttpHeaders.CONTENT_RANGE);
		this.headers.remove(HttpHeaders.CONTENT_TYPE);
	}

	/**
	 * 从 ETag 头的字段值中检索合并后的结果。
	 * @param headerName 标头名称
	 * @return 合并后的结果
	 * @throws IllegalArgumentException 如果解析失败
	 * @since 4.3
	 */
	protected List<String> getETagValuesAsList(String headerName) {
		List<String> values = get(headerName);
		if (values != null) {
			List<String> result = new ArrayList<>();
			for (String value : values) {
				if (value != null) {
					Matcher matcher = ETAG_HEADER_VALUE_PATTERN.matcher(value);
					while (matcher.find()) {
						if ("*".equals(matcher.group())) {
							result.add(matcher.group());
						}
						else {
							result.add(matcher.group(1));
						}
					}
					if (result.isEmpty()) {
						throw new IllegalArgumentException(
								"Could not parse header '" + headerName + "' with value '" + value + "'");
					}
				}
			}
			return result;
		}
		return Collections.emptyList();
	}

	/**
	 * 从多值头的字段值中检索合并后的结果。
	 * @param headerName 标头名称
	 * @return 合并后的结果
	 * @since 4.3
	 */
	@Nullable
	protected String getFieldValues(String headerName) {
		List<String> headerValues = get(headerName);
		return (headerValues != null ? toCommaDelimitedString(headerValues) : null);
	}

	/**
	 * 将给定的头值列表转换为逗号分隔的结果。
	 * @param headerValues 标头值列表
	 * @return 以逗号分隔的合并结果
	 */
	protected String toCommaDelimitedString(List<String> headerValues) {
		StringJoiner joiner = new StringJoiner(", ");
		for (String val : headerValues) {
			if (val != null) {
				joiner.add(val);
			}
		}
		return joiner.toString();
	}

	/**
	 * 设置给定的头值；如果为 {@code null}，则移除该头。
	 * @param headerName 标头名称
	 * @param headerValue 标头值，若无则为 {@code null}
	 */
	private void setOrRemove(String headerName, @Nullable String headerValue) {
		if (headerValue != null) {
			set(headerName, headerValue);
		}
		else {
			remove(headerName);
		}
	}


	// MultiValueMap implementation

	/**
	 * 返回给定头名称的第一个头值（如果有）。
	 * @param headerName 标头名称
	 * @return 第一个标头值，若无则为 {@code null}
	 */
	@Override
	@Nullable
	public String getFirst(String headerName) {
		return this.headers.getFirst(headerName);
	}

	/**
	 * 在给定名称下添加给定的单个头值。
	 * @param headerName 标头名称
	 * @param headerValue 标头值
	 * @throws UnsupportedOperationException 如果不支持添加标头
	 * @see #put(String, List)
	 * @see #set(String, String)
	 */
	@Override
	public void add(String headerName, @Nullable String headerValue) {
		this.headers.add(headerName, headerValue);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		this.headers.addAll(key, values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		this.headers.addAll(values);
	}

	/**
	 * 在给定名称下设置给定的单个头值。
	 * @param headerName 标头名称
	 * @param headerValue 标头值
	 * @throws UnsupportedOperationException 如果不支持添加标头
	 * @see #put(String, List)
	 * @see #add(String, String)
	 */
	@Override
	public void set(String headerName, @Nullable String headerValue) {
		this.headers.set(headerName, headerValue);
	}

	@Override
	public void setAll(Map<String, String> values) {
		this.headers.setAll(values);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		return this.headers.toSingleValueMap();
	}


	// Map implementation

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	@Nullable
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HttpHeaders)) {
			return false;
		}
		return unwrap(this).equals(unwrap((HttpHeaders) other));
	}

	private static MultiValueMap<String, String> unwrap(HttpHeaders headers) {
		while (headers.headers instanceof HttpHeaders) {
			headers = (HttpHeaders) headers.headers;
		}
		return headers.headers;
	}

	@Override
	public int hashCode() {
		return this.headers.hashCode();
	}

	@Override
	public String toString() {
		return formatHeaders(this.headers);
	}


	/**
	 * 如有必要，对给定的头应用只读 {@code HttpHeaders} 包装器。
	 * <p>还会缓存 "Accept" 和 "Content-Type" 头的解析表示形式。
	 * @param headers 要暴露的标头
	 * @return 标头的只读变体，或原样返回原始标头
	 * （如果它已经是只读的 {@code HttpHeaders} 实例）
	 * @since 5.3
	 */
	public static HttpHeaders readOnlyHttpHeaders(MultiValueMap<String, String> headers) {
		return (headers instanceof HttpHeaders ?
				readOnlyHttpHeaders((HttpHeaders) headers) : new ReadOnlyHttpHeaders(headers));
	}

	/**
	 * 如有必要，对给定的头应用只读 {@code HttpHeaders} 包装器。
	 * <p>还会缓存 "Accept" 和 "Content-Type" 头的解析表示形式。
	 * @param headers 要暴露的标头
	 * @return 标头的只读变体，或原样返回原始标头
	 */
	public static HttpHeaders readOnlyHttpHeaders(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		return (headers instanceof ReadOnlyHttpHeaders ? headers : new ReadOnlyHttpHeaders(headers.headers));
	}

	/**
	 * 移除之前可能通过 {@link #readOnlyHttpHeaders(HttpHeaders)} 应用于给定头的任何只读包装器。
	 * @param headers 要暴露的标头
	 * @return 标头的可写变体，或原样返回原始标头
	 * @since 5.1.1
	 */
	public static HttpHeaders writableHttpHeaders(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		if (headers == EMPTY) {
			return new HttpHeaders();
		}
		return (headers instanceof ReadOnlyHttpHeaders ? new HttpHeaders(headers.headers) : headers);
	}

	/**
	 * 帮助格式化 HTTP 头值，因为 HTTP 头值本身可以包含逗号分隔的值，使用同样在条目之间使用逗号的常规 {@link Map} 格式化方式可能会造成混淆。
	 * @param headers 要格式化的标头
	 * @return 标头的 String 表示
	 * @since 5.1.4
	 */
	public static String formatHeaders(MultiValueMap<String, String> headers) {
		return headers.entrySet().stream()
				.map(entry -> {
					List<String> values = entry.getValue();
					return entry.getKey() + ":" + (values.size() == 1 ?
							"\"" + values.get(0) + "\"" :
							values.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ")));
				})
				.collect(Collectors.joining(", ", "[", "]"));
	}

	/**
	 * 将给定的用户名和密码编码为基本认证凭据。
	 * <p>此方法返回的编码凭据可以提供给 {@link #setBasicAuth(String)} 以设置基本认证头。
	 * @param username 用户名
	 * @param password 密码
	 * @param charset 用于将凭证转换为八位字节序列的字符集。
	 * 默认为 {@linkplain StandardCharsets#ISO_8859_1 ISO-8859-1}。
	 * @throws IllegalArgumentException 如果 {@code username} 或 {@code password} 包含无法编码为给定字符集的字符
	 * @since 5.2
	 * @see #setBasicAuth(String)
	 * @see #setBasicAuth(String, String)
	 * @see #setBasicAuth(String, String, Charset)
	 * @see <a href="https://tools.ietf.org/html/rfc7617">RFC 7617</a>
	 */
	public static String encodeBasicAuth(String username, String password, @Nullable Charset charset) {
		Assert.notNull(username, "Username must not be null");
		Assert.doesNotContain(username, ":", "Username must not contain a colon");
		Assert.notNull(password, "Password must not be null");
		if (charset == null) {
			charset = StandardCharsets.ISO_8859_1;
		}

		CharsetEncoder encoder = charset.newEncoder();
		if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to " + charset.displayName());
		}

		String credentialsString = username + ":" + password;
		byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(charset));
		return new String(encodedBytes, charset);
	}

	// Package-private: used in ResponseCookie
	static String formatDate(long date) {
		Instant instant = Instant.ofEpochMilli(date);
		ZonedDateTime time = ZonedDateTime.ofInstant(instant, GMT);
		return DATE_FORMATTER.format(time);
	}

}
