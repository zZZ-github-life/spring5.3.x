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

package org.springframework.web.util;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HierarchicalUriComponents.PathComponent;
import org.springframework.web.util.UriComponents.UriTemplateVariables;

/**
 * 用于 {@link UriComponents} 的构建器。<p>典型用法包括：<ol> <li>使用某个静态工厂方法（如 {@link #fromPath(String)} 或 {@link #fromUri(URI)}）创建一个 {@code UriComponentsBuilder}</li> <li>通过相应方法（{@link #scheme(String)}、{@link #userInfo(String)}、{@link #host(String)}、{@link #port(int)}、{@link #path(String)}、{@link #pathSegment(String...)}、{@link #queryParam(String, Object...)} 和 {@link #fragment(String)}）设置各个 URI 组件。</li> <li>使用 {@link #build()} 方法构建 {@link UriComponents} 实例。</li> </ol>
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Oliver Gierke
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 3.1
 * @see #newInstance()
 * @see #fromPath(String)
 * @see #fromUri(URI)
 */
public class UriComponentsBuilder implements UriBuilder, Cloneable {

	private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

	private static final String SCHEME_PATTERN = "([^:/?#]+):";

	private static final String HTTP_PATTERN = "(?i)(http|https):";

	private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

	private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

	private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}:.]*[%\\p{Alnum}]*]";

	private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

	private static final String PORT_PATTERN = "(\\{[^}]+\\}?|[^/?#]*)";

	private static final String PATH_PATTERN = "([^?#]*)";

	private static final String QUERY_PATTERN = "([^#]*)";

	private static final String LAST_PATTERN = "(.*)";

	// Regex patterns that matches URIs. See RFC 3986, appendix B
	private static final Pattern URI_PATTERN = Pattern.compile(
			"^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN +
					")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	private static final Pattern HTTP_URL_PATTERN = Pattern.compile(
			"^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" +
					PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

	private static final String FORWARDED_VALUE = "\"?([^;,\"]+)\"?";

	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("(?i:host)=" + FORWARDED_VALUE);

	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("(?i:proto)=" + FORWARDED_VALUE);

	private static final Pattern FORWARDED_FOR_PATTERN = Pattern.compile("(?i:for)=" + FORWARDED_VALUE);

	private static final Object[] EMPTY_VALUES = new Object[0];


	@Nullable
	private String scheme;

	@Nullable
	private String ssp;

	@Nullable
	private String userInfo;

	@Nullable
	private String host;

	@Nullable
	private String port;

	private CompositePathComponentBuilder pathBuilder;

	private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

	@Nullable
	private String fragment;

	private final Map<String, Object> uriVariables = new HashMap<>(4);

	private boolean encodeTemplate;

	private Charset charset = StandardCharsets.UTF_8;


	/**
	 * 默认构造函数。声明为 protected 以防止直接实例化。
	 * @see #newInstance()
	 * @see #fromPath(String)
	 * @see #fromUri(URI)
	 */
	protected UriComponentsBuilder() {
		this.pathBuilder = new CompositePathComponentBuilder();
	}

	/**
	 * 创建给定 UriComponentsBuilder 的深拷贝。
	 * @param other 要从中复制的另一个构建器
	 * @since 4.1.3
	 */
	protected UriComponentsBuilder(UriComponentsBuilder other) {
		this.scheme = other.scheme;
		this.ssp = other.ssp;
		this.userInfo = other.userInfo;
		this.host = other.host;
		this.port = other.port;
		this.pathBuilder = other.pathBuilder.cloneBuilder();
		this.uriVariables.putAll(other.uriVariables);
		this.queryParams.addAll(other.queryParams);
		this.fragment = other.fragment;
		this.encodeTemplate = other.encodeTemplate;
		this.charset = other.charset;
	}


	// Factory methods

	/**
	 * 创建一个新的空构建器。
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder newInstance() {
		return new UriComponentsBuilder();
	}

	/**
	 * 创建一个用给定路径初始化的构建器。
	 * @param path 用于初始化的路径
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromPath(String path) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.path(path);
		return builder;
	}

	/**
	 * 创建一个从给定 {@code URI} 初始化的构建器。<p><strong>注意：</strong>结果构建器中的组件将采用完全编码（原始）形式，后续更改也必须提供完全编码的值，例如通过 {@link UriUtils} 中的方法。此外，请使用值为 "true" 的 {@link #build(boolean)} 来构建 {@link UriComponents} 实例，以指示组件已编码。
	 * @param uri 用于初始化的 URI
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUri(URI uri) {
		UriComponentsBuilder builder = new UriComponentsBuilder();
		builder.uri(uri);
		return builder;
	}

	/**
	 * 创建一个用给定 URI 字符串初始化的构建器。
	 * <p><strong>注意：</strong>保留字符的存在可能会妨碍 URI 字符串的正确解析。例如，如果查询参数包含 {@code '='} 或 {@code '&'} 字符，则查询字符串无法被无歧义地解析。此类值应替换为 URI 变量以实现正确解析：
	 * <pre class="code">
	 * String uriString = &quot;/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromUriString(uriString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * @param uri 用于初始化的 URI 字符串
	 * @return 新的 {@code UriComponentsBuilder}
	 */
	public static UriComponentsBuilder fromUriString(String uri) {
		Assert.notNull(uri, "URI must not be null");
		Matcher matcher = URI_PATTERN.matcher(uri);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(2);
			String userInfo = matcher.group(5);
			String host = matcher.group(6);
			String port = matcher.group(8);
			String path = matcher.group(9);
			String query = matcher.group(11);
			String fragment = matcher.group(13);
			boolean opaque = false;
			if (StringUtils.hasLength(scheme)) {
				String rest = uri.substring(scheme.length());
				if (!rest.startsWith(":/")) {
					opaque = true;
				}
			}
			builder.scheme(scheme);
			if (opaque) {
				String ssp = uri.substring(scheme.length() + 1);
				if (StringUtils.hasLength(fragment)) {
					ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
				}
				builder.schemeSpecificPart(ssp);
			}
			else {
				if (StringUtils.hasLength(scheme) && scheme.startsWith("http") && !StringUtils.hasLength(host)) {
					throw new IllegalArgumentException("[" + uri + "] is not a valid HTTP URL");
				}
				builder.userInfo(userInfo);
				builder.host(host);
				if (StringUtils.hasLength(port)) {
					builder.port(port);
				}
				builder.path(path);
				builder.query(query);
			}
			if (StringUtils.hasText(fragment)) {
				builder.fragment(fragment);
			}
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
		}
	}

	/**
	 * 从给定 HTTP URL 字符串创建 URI 组件构建器。
	 * <p><strong>注意：</strong>保留字符的存在可能会妨碍 URI 字符串的正确解析。例如，如果查询参数包含 {@code '='} 或 {@code '&'} 字符，则查询字符串无法被无歧义地解析。此类值应替换为 URI 变量以实现正确解析：
	 * <pre class="code">
	 * String urlString = &quot;https://example.com/hotels/42?filter={value}&quot;;
	 * UriComponentsBuilder.fromHttpUrl(urlString).buildAndExpand(&quot;hot&amp;cold&quot;);
	 * </pre>
	 * @param httpUrl 源 URI
	 * @return 该 URI 的 URI 组件
	 */
	public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
		Assert.notNull(httpUrl, "HTTP URL must not be null");
		Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(1);
			builder.scheme(scheme != null ? scheme.toLowerCase() : null);
			builder.userInfo(matcher.group(4));
			String host = matcher.group(5);
			if (StringUtils.hasLength(scheme) && !StringUtils.hasLength(host)) {
				throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
			}
			builder.host(host);
			String port = matcher.group(7);
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			builder.path(matcher.group(8));
			builder.query(matcher.group(10));
			String fragment = matcher.group(12);
			if (StringUtils.hasText(fragment)) {
				builder.fragment(fragment);
			}
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
		}
	}

	/**
	 * 从与给定 HttpRequest 关联的 URI 创建新的 {@code UriComponents} 对象，同时用 "Forwarded"（<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>）头中的值进行覆盖；如果未找到 "Forwarded"，则使用 "X-Forwarded-Host"、"X-Forwarded-Port" 和 "X-Forwarded-Proto"。
	 * @param request 源请求
	 * @return 该 URI 的 URI 组件
	 * @since 4.1.5
	 * @see #parseForwardedFor(HttpRequest, InetSocketAddress)
	 */
	public static UriComponentsBuilder fromHttpRequest(HttpRequest request) {
		return fromUri(request.getURI()).adaptFromForwardedHeaders(request.getHeaders());
	}

	/**
	 * 将第一个 "Forwarded: for=..." 或 "X-Forwarded-For" 头值解析为表示客户端地址的 {@code InetSocketAddress}。
	 * @param request 其头部可能包含转发头的请求
	 * @param remoteAddress 当前 remoteAddress
	 * @return 包含提取出的主机和端口的 {@code InetSocketAddress}；
	 * 如果这些头部不存在则为 {@code null}。
	 * @since 5.3
	 * @see <a href="https://tools.ietf.org/html/rfc7239#section-5.2">RFC 7239, Section 5.2</a>
	 */
	@Nullable
	public static InetSocketAddress parseForwardedFor(
			HttpRequest request, @Nullable InetSocketAddress remoteAddress) {

		int port = (remoteAddress != null ?
				remoteAddress.getPort() : "https".equals(request.getURI().getScheme()) ? 443 : 80);

		String forwardedHeader = request.getHeaders().getFirst("Forwarded");
		if (StringUtils.hasText(forwardedHeader)) {
			String forwardedToUse = StringUtils.tokenizeToStringArray(forwardedHeader, ",")[0];
			Matcher matcher = FORWARDED_FOR_PATTERN.matcher(forwardedToUse);
			if (matcher.find()) {
				String value = matcher.group(1).trim();
				String host = value;
				int portSeparatorIdx = value.lastIndexOf(':');
				int squareBracketIdx = value.lastIndexOf(']');
				if (portSeparatorIdx > squareBracketIdx) {
					if (squareBracketIdx == -1 && value.indexOf(':') != portSeparatorIdx) {
						throw new IllegalArgumentException("Invalid IPv4 address: " + value);
					}
					host = value.substring(0, portSeparatorIdx);
					try {
						port = Integer.parseInt(value.substring(portSeparatorIdx + 1));
					}
					catch (NumberFormatException ex) {
						throw new IllegalArgumentException(
								"Failed to parse a port from \"forwarded\"-type header value: " + value);
					}
				}
				return InetSocketAddress.createUnresolved(host, port);
			}
		}

		String forHeader = request.getHeaders().getFirst("X-Forwarded-For");
		if (StringUtils.hasText(forHeader)) {
			String host = StringUtils.tokenizeToStringArray(forHeader, ",")[0];
			return InetSocketAddress.createUnresolved(host, port);
		}

		return null;
	}

	/**
	 * 通过解析 HTTP 请求的 "Origin" 头创建实例。
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454</a>
	 */
	public static UriComponentsBuilder fromOriginHeader(String origin) {
		Matcher matcher = URI_PATTERN.matcher(origin);
		if (matcher.matches()) {
			UriComponentsBuilder builder = new UriComponentsBuilder();
			String scheme = matcher.group(2);
			String host = matcher.group(6);
			String port = matcher.group(8);
			if (StringUtils.hasLength(scheme)) {
				builder.scheme(scheme);
			}
			builder.host(host);
			if (StringUtils.hasLength(port)) {
				builder.port(port);
			}
			return builder;
		}
		else {
			throw new IllegalArgumentException("[" + origin + "] is not a valid \"Origin\" header value");
		}
	}


	// Encode methods

	/**
	 * 要求在构建时预编码 URI 模板，并在扩展时单独编码 URI 变量。<p>与 {@link UriComponents#encode()} 相比，此方法对 URI 模板的效果相同，即通过将非 ASCII 字符和非法（在 URI 组件类型内）字符替换为转义八位组来编码每个 URI 组件。不过，URI 变量的编码更严格，还会转义具有保留含义的字符。<p>大多数情况下，此方法更可能给出预期结果，因为它将 URI 变量视为需要完全编码的不透明数据；而当你有意扩展包含保留字符的 URI 变量时，{@link UriComponents#encode()} 则很有用。<p>例如，';' 在路径中是合法的，但具有保留含义。此方法会将 URI 变量中的 ";" 替换为 "%3B"，但不会替换 URI 模板中的 ";"。相反，{@link UriComponents#encode()} 从不替换 ";"，因为它是路径中的合法字符。<p>如果完全不扩展 URI 变量，请优先使用 {@link UriComponents#encode()}，因为它还会编码任何偶然看起来像 URI 变量的内容。
	 * @since 5.0.8
	 */
	public final UriComponentsBuilder encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * 使用非 "UTF-8" 字符集的 {@link #encode()} 变体。
	 * @param charset 用于编码的字符集
	 * @since 5.0.8
	 */
	public UriComponentsBuilder encode(Charset charset) {
		this.encodeTemplate = true;
		this.charset = charset;
		return this;
	}


	// Build methods

	/**
	 * 根据此构建器中包含的各个组件构建 {@code UriComponents} 实例。
	 * @return URI 组件
	 */
	public UriComponents build() {
		return build(false);
	}

	/**
	 * 当组件已完全编码时，用于创建 {@link UriComponents} 实例的 {@link #build()} 变体。例如，如果构建器是通过 {@link UriComponentsBuilder#fromUri(URI)} 创建的，这将很有用。
	 * @param encoded 此构建器中的组件是否已编码
	 * @return URI 组件
	 * @throws IllegalArgumentException 如果任何组件包含本应已编码的非法字符。
	 */
	public UriComponents build(boolean encoded) {
		return buildInternal(encoded ? EncodingHint.FULLY_ENCODED :
				(this.encodeTemplate ? EncodingHint.ENCODE_TEMPLATE : EncodingHint.NONE));
	}

	private UriComponents buildInternal(EncodingHint hint) {
		UriComponents result;
		if (this.ssp != null) {
			result = new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
		}
		else {
			HierarchicalUriComponents uric = new HierarchicalUriComponents(this.scheme, this.fragment,
					this.userInfo, this.host, this.port, this.pathBuilder.build(), this.queryParams,
					hint == EncodingHint.FULLY_ENCODED);
			result = (hint == EncodingHint.ENCODE_TEMPLATE ? uric.encodeTemplate(this.charset) : uric);
		}
		if (!this.uriVariables.isEmpty()) {
			result = result.expand(name -> this.uriVariables.getOrDefault(name, UriTemplateVariables.SKIP_VALUE));
		}
		return result;
	}

	/**
	 * 构建 {@code UriComponents} 实例，并用 Map 中的值替换 URI 模板变量。此便捷方法组合了对 {@link #build()} 和 {@link UriComponents#expand(Map)} 的调用。
	 * @param uriVariables URI 变量映射
	 * @return 展开了值的 URI 组件
	 */
	public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
		return build().expand(uriVariables);
	}

	/**
	 * 构建 {@code UriComponents} 实例，并用数组中的值替换 URI 模板变量。此便捷方法组合了对 {@link #build()} 和 {@link UriComponents#expand(Object...)} 的调用。
	 * @param uriVariableValues URI 变量值
	 * @return 展开了值的 URI 组件
	 */
	public UriComponents buildAndExpand(Object... uriVariableValues) {
		return build().expand(uriVariableValues);
	}

	@Override
	public URI build(Object... uriVariables) {
		return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
	}

	@Override
	public URI build(Map<String, ?> uriVariables) {
		return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
	}

	/**
	 * 构建 URI String。
	 * <p>实际上，这是构建、编码并返回 String 表示形式的快捷方式：
	 * <pre class="code">
	 * String uri = builder.build().encode().toUriString()
	 * </pre>
	 * <p>不过，如果已提供 {@link #uriVariables(Map) URI 变量}，则 URI 模板会与 URI 变量分开预编码（详情请参见
	 * {@link #encode()}），即等价于：
	 * <pre>
	 * String uri = builder.encode().build().toUriString()
	 * </pre>
	 * @since 4.1
	 * @see UriComponents#toUriString()
	 */
	public String toUriString() {
		return (this.uriVariables.isEmpty() ? build().encode().toUriString() :
				buildInternal(EncodingHint.ENCODE_TEMPLATE).toUriString());
	}


	// Instance methods

	/**
	 * 从给定 URI 的组件初始化此构建器的组件。
	 * @param uri 该 URI
	 * @return 当前 UriComponentsBuilder
	 */
	public UriComponentsBuilder uri(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.scheme = uri.getScheme();
		if (uri.isOpaque()) {
			this.ssp = uri.getRawSchemeSpecificPart();
			resetHierarchicalComponents();
		}
		else {
			if (uri.getRawUserInfo() != null) {
				this.userInfo = uri.getRawUserInfo();
			}
			if (uri.getHost() != null) {
				this.host = uri.getHost();
			}
			if (uri.getPort() != -1) {
				this.port = String.valueOf(uri.getPort());
			}
			if (StringUtils.hasLength(uri.getRawPath())) {
				this.pathBuilder = new CompositePathComponentBuilder();
				this.pathBuilder.addPath(uri.getRawPath());
			}
			if (StringUtils.hasLength(uri.getRawQuery())) {
				this.queryParams.clear();
				query(uri.getRawQuery());
			}
			resetSchemeSpecificPart();
		}
		if (uri.getRawFragment() != null) {
			this.fragment = uri.getRawFragment();
		}
		return this;
	}

	/**
	 * 用给定 {@link UriComponents} 实例的值设置或追加此构建器的各个 URI 组件。<p>关于每个组件的语义（即设置还是追加），请查看此类上的构建器方法。例如 {@link #host(String)} 是设置，而 {@link #path(String)} 是追加。
	 * @param uriComponents 要从中复制的 UriComponents
	 * @return 当前 UriComponentsBuilder
	 */
	public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
		Assert.notNull(uriComponents, "UriComponents must not be null");
		uriComponents.copyToUriComponentsBuilder(this);
		return this;
	}

	@Override
	public UriComponentsBuilder scheme(@Nullable String scheme) {
		this.scheme = scheme;
		return this;
	}

	/**
	 * 设置 URI 的 scheme 特定部分。调用时，此方法会覆盖 {@linkplain #userInfo(String) user-info}、{@linkplain #host(String) host}、{@linkplain #port(int) port}、{@linkplain #path(String) path} 和 {@link #query(String) query}。
	 * @param ssp URI 方案特定部分（scheme-specific-part），
	 * 可能包含 URI 模板参数
	 * @return 当前 UriComponentsBuilder
	 */
	public UriComponentsBuilder schemeSpecificPart(String ssp) {
		this.ssp = ssp;
		resetHierarchicalComponents();
		return this;
	}

	@Override
	public UriComponentsBuilder userInfo(@Nullable String userInfo) {
		this.userInfo = userInfo;
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder host(@Nullable String host) {
		this.host = host;
		if (host != null) {
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder port(int port) {
		Assert.isTrue(port >= -1, "Port must be >= -1");
		this.port = String.valueOf(port);
		if (port > -1) {
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder port(@Nullable String port) {
		this.port = port;
		if (port != null) {
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder path(String path) {
		this.pathBuilder.addPath(path);
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
		this.pathBuilder.addPathSegments(pathSegments);
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder replacePath(@Nullable String path) {
		this.pathBuilder = new CompositePathComponentBuilder();
		if (path != null) {
			this.pathBuilder.addPath(path);
		}
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder query(@Nullable String query) {
		if (query != null) {
			Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = matcher.group(1);
				String eq = matcher.group(2);
				String value = matcher.group(3);
				queryParam(name, (value != null ? value : (StringUtils.hasLength(eq) ? "" : null)));
			}
			resetSchemeSpecificPart();
		}
		else {
			this.queryParams.clear();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder replaceQuery(@Nullable String query) {
		this.queryParams.clear();
		if (query != null) {
			query(query);
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder queryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		if (!ObjectUtils.isEmpty(values)) {
			for (Object value : values) {
				String valueAsString = getQueryParamValue(value);
				this.queryParams.add(name, valueAsString);
			}
		}
		else {
			this.queryParams.add(name, null);
		}
		resetSchemeSpecificPart();
		return this;
	}

	@Nullable
	private String getQueryParamValue(@Nullable Object value) {
		if (value != null) {
			return (value instanceof Optional ?
					((Optional<?>) value).map(Object::toString).orElse(null) :
					value.toString());
		}
		return null;
	}

	@Override
	public UriComponentsBuilder queryParam(String name, @Nullable Collection<?> values) {
		return queryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
	}

	@Override
	public UriComponentsBuilder queryParamIfPresent(String name, Optional<?> value) {
		value.ifPresent(o -> {
			if (o instanceof Collection) {
				queryParam(name, (Collection<?>) o);
			}
			else {
				queryParam(name, o);
			}
		});
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @since 4.0
	 */
	@Override
	public UriComponentsBuilder queryParams(@Nullable MultiValueMap<String, String> params) {
		if (params != null) {
			this.queryParams.addAll(params);
			resetSchemeSpecificPart();
		}
		return this;
	}

	@Override
	public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
		Assert.notNull(name, "Name must not be null");
		this.queryParams.remove(name);
		if (!ObjectUtils.isEmpty(values)) {
			queryParam(name, values);
		}
		resetSchemeSpecificPart();
		return this;
	}

	@Override
	public UriComponentsBuilder replaceQueryParam(String name, @Nullable Collection<?> values) {
		return replaceQueryParam(name, (CollectionUtils.isEmpty(values) ? EMPTY_VALUES : values.toArray()));
	}

	/**
	 * {@inheritDoc}
	 * @since 4.2
	 */
	@Override
	public UriComponentsBuilder replaceQueryParams(@Nullable MultiValueMap<String, String> params) {
		this.queryParams.clear();
		if (params != null) {
			this.queryParams.putAll(params);
		}
		return this;
	}

	@Override
	public UriComponentsBuilder fragment(@Nullable String fragment) {
		if (fragment != null) {
			Assert.hasLength(fragment, "Fragment must not be empty");
			this.fragment = fragment;
		}
		else {
			this.fragment = null;
		}
		return this;
	}

	/**
	 * 配置在构建时要扩展的 URI 变量。<p>提供的变量可以是所有必需变量的子集。构建时，会扩展可用的变量，而未解析的 URI 占位符会保留原样，之后仍可扩展。<p>与 {@link UriComponents#expand(Map)} 或 {@link #buildAndExpand(Map)} 不同，当你需要提供 URI 变量但还不想构建 {@link UriComponents} 实例时，或者也许想预先扩展某些共享默认值（如主机和端口）时，此方法很有用。
	 * @param uriVariables 要使用的 URI 变量
	 * @return 当前 UriComponentsBuilder
	 * @since 5.0.8
	 */
	public UriComponentsBuilder uriVariables(Map<String, Object> uriVariables) {
		this.uriVariables.putAll(uriVariables);
		return this;
	}

	/**
	 * 根据给定头（特别是 "Forwarded"（<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>）；如果未找到 "Forwarded"，则使用 "X-Forwarded-Host"、"X-Forwarded-Port" 和 "X-Forwarded-Proto"）调整此构建器的 scheme+host+port。<p><strong>注意：</strong>此方法会使用转发头中的值（如果存在），以反映客户端发起的协议和地址。可考虑使用 {@code ForwardedHeaderFilter}，以便在一个中心位置选择是提取并使用还是丢弃这些头。有关此过滤器的更多信息，请参见 Spring Framework 参考文档。
	 * @param headers 要考虑的 HTTP 头部
	 * @return 当前 UriComponentsBuilder
	 * @since 4.2.7
	 */
	UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
		try {
			String forwardedHeader = headers.getFirst("Forwarded");
			if (StringUtils.hasText(forwardedHeader)) {
				Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedHeader);
				if (matcher.find()) {
					scheme(matcher.group(1).trim());
					port(null);
				}
				else if (isForwardedSslOn(headers)) {
					scheme("https");
					port(null);
				}
				matcher = FORWARDED_HOST_PATTERN.matcher(forwardedHeader);
				if (matcher.find()) {
					adaptForwardedHost(matcher.group(1).trim());
				}
			}
			else {
				String protocolHeader = headers.getFirst("X-Forwarded-Proto");
				if (StringUtils.hasText(protocolHeader)) {
					scheme(StringUtils.tokenizeToStringArray(protocolHeader, ",")[0]);
					port(null);
				}
				else if (isForwardedSslOn(headers)) {
					scheme("https");
					port(null);
				}
				String hostHeader = headers.getFirst("X-Forwarded-Host");
				if (StringUtils.hasText(hostHeader)) {
					adaptForwardedHost(StringUtils.tokenizeToStringArray(hostHeader, ",")[0]);
				}
				String portHeader = headers.getFirst("X-Forwarded-Port");
				if (StringUtils.hasText(portHeader)) {
					port(Integer.parseInt(StringUtils.tokenizeToStringArray(portHeader, ",")[0]));
				}
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Failed to parse a port from \"forwarded\"-type headers. " +
					"If not behind a trusted proxy, consider using ForwardedHeaderFilter " +
					"with the removeOnly=true. Request headers: " + headers);
		}

		if (this.scheme != null &&
				(((this.scheme.equals("http") || this.scheme.equals("ws")) && "80".equals(this.port)) ||
				((this.scheme.equals("https") || this.scheme.equals("wss")) && "443".equals(this.port)))) {
			port(null);
		}

		return this;
	}

	private boolean isForwardedSslOn(HttpHeaders headers) {
		String forwardedSsl = headers.getFirst("X-Forwarded-Ssl");
		return StringUtils.hasText(forwardedSsl) && forwardedSsl.equalsIgnoreCase("on");
	}

	private void adaptForwardedHost(String rawValue) {
		int portSeparatorIdx = rawValue.lastIndexOf(':');
		int squareBracketIdx = rawValue.lastIndexOf(']');
		if (portSeparatorIdx > squareBracketIdx) {
			if (squareBracketIdx == -1 && rawValue.indexOf(':') != portSeparatorIdx) {
				throw new IllegalArgumentException("Invalid IPv4 address: " + rawValue);
			}
			host(rawValue.substring(0, portSeparatorIdx));
			port(Integer.parseInt(rawValue.substring(portSeparatorIdx + 1)));
		}
		else {
			host(rawValue);
			port(null);
		}
	}

	private void resetHierarchicalComponents() {
		this.userInfo = null;
		this.host = null;
		this.port = null;
		this.pathBuilder = new CompositePathComponentBuilder();
		this.queryParams.clear();
	}

	private void resetSchemeSpecificPart() {
		this.ssp = null;
	}


	/**
	 * Object 的 {@code clone()} 方法的公共声明。委托给 {@link #cloneBuilder()}。
	 */
	@Override
	public Object clone() {
		return cloneBuilder();
	}

	/**
	 * 克隆此 {@code UriComponentsBuilder}。
	 * @return 克隆的 {@code UriComponentsBuilder} 对象
	 * @since 4.2.7
	 */
	public UriComponentsBuilder cloneBuilder() {
		return new UriComponentsBuilder(this);
	}


	private interface PathComponentBuilder {

		@Nullable
		PathComponent build();

		PathComponentBuilder cloneBuilder();
	}


	private static class CompositePathComponentBuilder implements PathComponentBuilder {

		private final Deque<PathComponentBuilder> builders = new ArrayDeque<>();

		public void addPathSegments(String... pathSegments) {
			if (!ObjectUtils.isEmpty(pathSegments)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				if (psBuilder == null) {
					psBuilder = new PathSegmentComponentBuilder();
					this.builders.add(psBuilder);
					if (fpBuilder != null) {
						fpBuilder.removeTrailingSlash();
					}
				}
				psBuilder.append(pathSegments);
			}
		}

		public void addPath(String path) {
			if (StringUtils.hasText(path)) {
				PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
				FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
				if (psBuilder != null) {
					path = (path.startsWith("/") ? path : "/" + path);
				}
				if (fpBuilder == null) {
					fpBuilder = new FullPathComponentBuilder();
					this.builders.add(fpBuilder);
				}
				fpBuilder.append(path);
			}
		}

		@SuppressWarnings("unchecked")
		@Nullable
		private <T> T getLastBuilder(Class<T> builderClass) {
			if (!this.builders.isEmpty()) {
				PathComponentBuilder last = this.builders.getLast();
				if (builderClass.isInstance(last)) {
					return (T) last;
				}
			}
			return null;
		}

		@Override
		public PathComponent build() {
			int size = this.builders.size();
			List<PathComponent> components = new ArrayList<>(size);
			for (PathComponentBuilder componentBuilder : this.builders) {
				PathComponent pathComponent = componentBuilder.build();
				if (pathComponent != null) {
					components.add(pathComponent);
				}
			}
			if (components.isEmpty()) {
				return HierarchicalUriComponents.NULL_PATH_COMPONENT;
			}
			if (components.size() == 1) {
				return components.get(0);
			}
			return new HierarchicalUriComponents.PathComponentComposite(components);
		}

		@Override
		public CompositePathComponentBuilder cloneBuilder() {
			CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
			for (PathComponentBuilder builder : this.builders) {
				compositeBuilder.builders.add(builder.cloneBuilder());
			}
			return compositeBuilder;
		}
	}


	private static class FullPathComponentBuilder implements PathComponentBuilder {

		private final StringBuilder path = new StringBuilder();

		public void append(String path) {
			this.path.append(path);
		}

		@Override
		public PathComponent build() {
			if (this.path.length() == 0) {
				return null;
			}
			String sanitized = getSanitizedPath(this.path);
			return new HierarchicalUriComponents.FullPathComponent(sanitized);
		}

		private static String getSanitizedPath(final StringBuilder path) {
			int index = path.indexOf("//");
			if (index >= 0) {
				StringBuilder sanitized = new StringBuilder(path);
				while (index != -1) {
					sanitized.deleteCharAt(index);
					index = sanitized.indexOf("//", index);
				}
				return sanitized.toString();
			}
			return path.toString();
		}

		public void removeTrailingSlash() {
			int index = this.path.length() - 1;
			if (this.path.charAt(index) == '/') {
				this.path.deleteCharAt(index);
			}
		}

		@Override
		public FullPathComponentBuilder cloneBuilder() {
			FullPathComponentBuilder builder = new FullPathComponentBuilder();
			builder.append(this.path.toString());
			return builder;
		}
	}


	private static class PathSegmentComponentBuilder implements PathComponentBuilder {

		private final List<String> pathSegments = new ArrayList<>();

		public void append(String... pathSegments) {
			for (String pathSegment : pathSegments) {
				if (StringUtils.hasText(pathSegment)) {
					this.pathSegments.add(pathSegment);
				}
			}
		}

		@Override
		public PathComponent build() {
			return (this.pathSegments.isEmpty() ? null :
					new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
		}

		@Override
		public PathSegmentComponentBuilder cloneBuilder() {
			PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
			builder.pathSegments.addAll(this.pathSegments);
			return builder;
		}
	}


	private enum EncodingHint { ENCODE_TEMPLATE, FULLY_ENCODED, NONE }

}
