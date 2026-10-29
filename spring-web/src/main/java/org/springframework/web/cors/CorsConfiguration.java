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

package org.springframework.web.cors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * CORS 配置的容器，同时提供了针对给定请求的实际 origin、HTTP 方法和请求头
 * 进行校验的方法。
 *
 * <p>默认情况下，新建的 {@code CorsConfiguration} 不允许任何跨源请求，
 * 必须显式进行配置以指明允许的内容。可使用
 * {@link #applyPermitDefaultValues()} 将初始化模型切换为开放的默认值，
 * 即允许 GET、HEAD 和 POST 请求的所有跨源请求。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Ruslan Akhundov
 * @since 4.2
 * @see <a href="https://www.w3.org/TR/cors/">CORS spec</a>
 */
public class CorsConfiguration {

	/** 表示<em>所有</em> origin、方法或请求头的通配符。 */
	public static final String ALL = "*";

	private static final List<String> ALL_LIST = Collections.singletonList(ALL);

	private static final OriginPattern ALL_PATTERN = new OriginPattern("*");

	private static final List<OriginPattern> ALL_PATTERN_LIST = Collections.singletonList(ALL_PATTERN);

	private static final List<String> DEFAULT_PERMIT_ALL = Collections.singletonList(ALL);

	private static final List<HttpMethod> DEFAULT_METHODS = Collections.unmodifiableList(
			Arrays.asList(HttpMethod.GET, HttpMethod.HEAD));

	private static final List<String> DEFAULT_PERMIT_METHODS = Collections.unmodifiableList(
			Arrays.asList(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name()));


	@Nullable
	private List<String> allowedOrigins;

	@Nullable
	private List<OriginPattern> allowedOriginPatterns;

	@Nullable
	private List<String> allowedMethods;

	@Nullable
	private List<HttpMethod> resolvedMethods = DEFAULT_METHODS;

	@Nullable
	private List<String> allowedHeaders;

	@Nullable
	private List<String> exposedHeaders;

	@Nullable
	private Boolean allowCredentials;

	@Nullable
	private Long maxAge;


	/**
	 * 构造一个新的 {@code CorsConfiguration} 实例，默认不允许任何 origin 的跨源请求。
	 * @see #applyPermitDefaultValues()
	 */
	public CorsConfiguration() {
	}

	/**
	 * 通过复制所提供的 {@code CorsConfiguration} 中的所有值，
	 * 构造一个新的 {@code CorsConfiguration} 实例。
	 */
	public CorsConfiguration(CorsConfiguration other) {
		this.allowedOrigins = other.allowedOrigins;
		this.allowedOriginPatterns = other.allowedOriginPatterns;
		this.allowedMethods = other.allowedMethods;
		this.resolvedMethods = other.resolvedMethods;
		this.allowedHeaders = other.allowedHeaders;
		this.exposedHeaders = other.exposedHeaders;
		this.allowCredentials = other.allowCredentials;
		this.maxAge = other.maxAge;
	}


	/**
	 * 允许跨源请求的 origin 列表。值可以是具体域名，例如
	 * {@code "https://domain1.com"}，也可以是 CORS 定义的特殊值 {@code "*"}，
	 * 表示所有 origin。
	 * <p>对于匹配的预检请求和实际请求，{@code Access-Control-Allow-Origin}
	 * 响应头会被设置为匹配的域名值或 {@code "*"}。但请注意，当
	 * {@link #setAllowCredentials allowCredentials} 设置为 {@code true} 时，
	 * CORS 规范不允许使用 {@code "*"}，并且从 5.3 开始，这种组合会被拒绝，
	 * 应改用 {@link #setAllowedOriginPatterns allowedOriginPatterns}。
	 * <p>默认情况下不设置此属性，意味着不允许任何 origin。
	 * 不过，此类的实例通常会被进一步初始化，例如对于 {@code @CrossOrigin}，
	 * 会通过 {@link #applyPermitDefaultValues()} 进行初始化。
	 */
	public void setAllowedOrigins(@Nullable List<String> origins) {
		this.allowedOrigins = (origins == null ? null :
				origins.stream().filter(Objects::nonNull).map(this::trimTrailingSlash).collect(Collectors.toList()));
	}

	private String trimTrailingSlash(String origin) {
		return (origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin);
	}

	/**
	 * 返回配置的允许 origin，如果没有则为 {@code null}。
	 */
	@Nullable
	public List<String> getAllowedOrigins() {
		return this.allowedOrigins;
	}

	/**
	 * {@link #setAllowedOrigins} 的变体，用于一次添加一个 origin。
	 */
	public void addAllowedOrigin(@Nullable String origin) {
		if (origin == null) {
			return;
		}
		if (this.allowedOrigins == null) {
			this.allowedOrigins = new ArrayList<>(4);
		}
		else if (this.allowedOrigins == DEFAULT_PERMIT_ALL && CollectionUtils.isEmpty(this.allowedOriginPatterns)) {
			setAllowedOrigins(DEFAULT_PERMIT_ALL);
		}
		origin = trimTrailingSlash(origin);
		this.allowedOrigins.add(origin);
	}

	/**
	 * {@link #setAllowedOrigins} 的替代方案，支持更灵活的 origin 模式，
	 * 可在主机名中的任意位置使用 "*"，此外还支持端口列表。示例：
	 * <ul>
	 * <li>{@literal https://*.domain1.com} -- 以 domain1.com 结尾的域名
	 * <li>{@literal https://*.domain1.com:[8080,8081]} -- 以
	 * domain1.com 结尾、端口为 8080 或 8081 的域名
	 * <li>{@literal https://*.domain1.com:[*]} -- 以 domain1.com 结尾、
	 * 使用任意端口（包括默认端口）的域名
	 * </ul>
	 * <p>与仅支持 "*" 且不能与 {@code allowCredentials} 一起使用的
	 * {@link #setAllowedOrigins(List) allowedOrigins} 不同，当匹配到
	 * allowedOriginPattern 时，{@code Access-Control-Allow-Origin}
	 * 响应头会被设置为匹配的 origin，而不是 {@code "*"}，也不是该模式本身。
	 * 因此，allowedOriginPatterns 可以与设置为 {@code true} 的
	 * {@link #setAllowCredentials} 组合使用。
	 * <p>默认情况下不设置此属性。
	 * @since 5.3
	 */
	public CorsConfiguration setAllowedOriginPatterns(@Nullable List<String> allowedOriginPatterns) {
		if (allowedOriginPatterns == null) {
			this.allowedOriginPatterns = null;
		}
		else {
			this.allowedOriginPatterns = new ArrayList<>(allowedOriginPatterns.size());
			for (String patternValue : allowedOriginPatterns) {
				addAllowedOriginPattern(patternValue);
			}
		}
		return this;
	}

	/**
	 * 返回配置的允许 origin 模式，如果没有则为 {@code null}。
	 * @since 5.3
	 */
	@Nullable
	public List<String> getAllowedOriginPatterns() {
		if (this.allowedOriginPatterns == null) {
			return null;
		}
		return this.allowedOriginPatterns.stream()
				.map(OriginPattern::getDeclaredPattern)
				.collect(Collectors.toList());
	}

	/**
	 * {@link #setAllowedOriginPatterns} 的变体，用于一次添加一个 origin。
	 * @since 5.3
	 */
	public void addAllowedOriginPattern(@Nullable String originPattern) {
		if (originPattern == null) {
			return;
		}
		if (this.allowedOriginPatterns == null) {
			this.allowedOriginPatterns = new ArrayList<>(4);
		}
		originPattern = trimTrailingSlash(originPattern);
		this.allowedOriginPatterns.add(new OriginPattern(originPattern));
		if (this.allowedOrigins == DEFAULT_PERMIT_ALL) {
			this.allowedOrigins = null;
		}
	}

	/**
	 * 设置允许的 HTTP 方法，例如 {@code "GET"}、{@code "POST"}、
	 * {@code "PUT"} 等。
	 * <p>特殊值 {@code "*"} 表示允许所有方法。
	 * <p>如果未设置，则只允许 {@code "GET"} 和 {@code "HEAD"}。
	 * <p>默认情况下不设置此属性。
	 * <p><strong>注意：</strong>CORS 检查会使用 "Forwarded"
	 * （<a href="https://tools.ietf.org/html/rfc7239">RFC 7239</a>）、
	 * "X-Forwarded-Host"、"X-Forwarded-Port" 和 "X-Forwarded-Proto" 头中的值
	 * （如果存在），以反映客户端发起的地址。可以考虑使用
	 * {@code ForwardedHeaderFilter}，以便在一个集中位置选择是提取并使用
	 * 还是丢弃此类头。有关此过滤器的更多信息，请参阅 Spring Framework 参考文档。
	 */
	public void setAllowedMethods(@Nullable List<String> allowedMethods) {
		this.allowedMethods = (allowedMethods != null ? new ArrayList<>(allowedMethods) : null);
		if (!CollectionUtils.isEmpty(allowedMethods)) {
			this.resolvedMethods = new ArrayList<>(allowedMethods.size());
			for (String method : allowedMethods) {
				if (ALL.equals(method)) {
					this.resolvedMethods = null;
					break;
				}
				this.resolvedMethods.add(HttpMethod.resolve(method));
			}
		}
		else {
			this.resolvedMethods = DEFAULT_METHODS;
		}
	}

	/**
	 * 返回允许的 HTTP 方法；如果为 {@code null}，则只允许
	 * {@code "GET"} 和 {@code "HEAD"}。
	 * @see #addAllowedMethod(HttpMethod)
	 * @see #addAllowedMethod(String)
	 * @see #setAllowedMethods(List)
	 */
	@Nullable
	public List<String> getAllowedMethods() {
		return this.allowedMethods;
	}

	/**
	 * 添加允许的 HTTP 方法。
	 */
	public void addAllowedMethod(HttpMethod method) {
		addAllowedMethod(method.name());
	}

	/**
	 * 添加允许的 HTTP 方法。
	 */
	public void addAllowedMethod(String method) {
		if (StringUtils.hasText(method)) {
			if (this.allowedMethods == null) {
				this.allowedMethods = new ArrayList<>(4);
				this.resolvedMethods = new ArrayList<>(4);
			}
			else if (this.allowedMethods == DEFAULT_PERMIT_METHODS) {
				setAllowedMethods(DEFAULT_PERMIT_METHODS);
			}
			this.allowedMethods.add(method);
			if (ALL.equals(method)) {
				this.resolvedMethods = null;
			}
			else if (this.resolvedMethods != null) {
				this.resolvedMethods.add(HttpMethod.resolve(method));
			}
		}
	}

	/**
	 * 设置预检请求可以列为允许在实际请求期间使用的请求头列表。
	 * <p>特殊值 {@code "*"} 允许实际请求发送任意请求头。
	 * <p>如果请求头属于以下之一，则无需列出：
	 * {@code Cache-Control}、{@code Content-Language}、{@code Expires}、
	 * {@code Last-Modified} 或 {@code Pragma}。
	 * <p>默认情况下不设置此属性。
	 */
	public void setAllowedHeaders(@Nullable List<String> allowedHeaders) {
		this.allowedHeaders = (allowedHeaders != null ? new ArrayList<>(allowedHeaders) : null);
	}

	/**
	 * 返回允许的实际请求头，如果没有则为 {@code null}。
	 * @see #addAllowedHeader(String)
	 * @see #setAllowedHeaders(List)
	 */
	@Nullable
	public List<String> getAllowedHeaders() {
		return this.allowedHeaders;
	}

	/**
	 * 添加允许的实际请求头。
	 */
	public void addAllowedHeader(String allowedHeader) {
		if (this.allowedHeaders == null) {
			this.allowedHeaders = new ArrayList<>(4);
		}
		else if (this.allowedHeaders == DEFAULT_PERMIT_ALL) {
			setAllowedHeaders(DEFAULT_PERMIT_ALL);
		}
		this.allowedHeaders.add(allowedHeader);
	}

	/**
	 * 设置可暴露的响应头列表，即除简单头（{@code Cache-Control}、
	 * {@code Content-Language}、{@code Content-Type}、{@code Expires}、
	 * {@code Last-Modified} 或 {@code Pragma}）之外，实际响应可能具有
	 * 并可以被暴露的响应头。
	 * <p>特殊值 {@code "*"} 允许对非凭据请求暴露所有响应头。
	 * <p>默认情况下不设置此属性。
	 */
	public void setExposedHeaders(@Nullable List<String> exposedHeaders) {
		this.exposedHeaders = (exposedHeaders != null ? new ArrayList<>(exposedHeaders) : null);
	}

	/**
	 * 返回配置的待暴露响应头，如果没有则为 {@code null}。
	 * @see #addExposedHeader(String)
	 * @see #setExposedHeaders(List)
	 */
	@Nullable
	public List<String> getExposedHeaders() {
		return this.exposedHeaders;
	}

	/**
	 * 添加要暴露的响应头。
	 * <p>特殊值 {@code "*"} 允许对非凭据请求暴露所有响应头。
	 */
	public void addExposedHeader(String exposedHeader) {
		if (this.exposedHeaders == null) {
			this.exposedHeaders = new ArrayList<>(4);
		}
		this.exposedHeaders.add(exposedHeader);
	}

	/**
	 * 是否支持用户凭据。
	 * <p>默认情况下不设置此属性（即不支持用户凭据）。
	 */
	public void setAllowCredentials(@Nullable Boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}

	/**
	 * 返回配置的 {@code allowCredentials} 标志，如果没有则为 {@code null}。
	 * @see #setAllowCredentials(Boolean)
	 */
	@Nullable
	public Boolean getAllowCredentials() {
		return this.allowCredentials;
	}

	/**
	 * 以时长形式配置客户端可以缓存预检请求响应的时长。
	 * @since 5.2
	 * @see #setMaxAge(Long)
	 */
	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge.getSeconds();
	}

	/**
	 * 以秒为单位配置客户端可以缓存预检请求响应的时长。
	 * <p>默认情况下不设置此属性。
	 */
	public void setMaxAge(@Nullable Long maxAge) {
		this.maxAge = maxAge;
	}

	/**
	 * 返回配置的 {@code maxAge} 值，如果没有则为 {@code null}。
	 * @see #setMaxAge(Long)
	 */
	@Nullable
	public Long getMaxAge() {
		return this.maxAge;
	}


	/**
	 * 默认情况下 {@code CorsConfiguration} 不允许任何跨源请求，必须显式配置。
	 * 使用此方法可切换到默认值，即允许 GET、HEAD 和 POST 的所有跨源请求，
	 * 但不会覆盖已经设置的值。
	 * <p>对于未设置的值，会应用以下默认值：
	 * <ul>
	 * <li>使用 CORS 规范中定义的特殊值 {@code "*"} 允许所有 origin。
	 * 仅当既未设置 {@link #setAllowedOrigins origins}，也未设置
	 * {@link #setAllowedOriginPatterns originPatterns} 时才会应用。</li>
	 * <li>允许“简单”方法 {@code GET}、{@code HEAD} 和 {@code POST}。</li>
	 * <li>允许所有请求头。</li>
	 * <li>将最大缓存时长设置为 1800 秒（30 分钟）。</li>
	 * </ul>
	 */
	public CorsConfiguration applyPermitDefaultValues() {
		if (this.allowedOrigins == null && this.allowedOriginPatterns == null) {
			this.allowedOrigins = DEFAULT_PERMIT_ALL;
		}
		if (this.allowedMethods == null) {
			this.allowedMethods = DEFAULT_PERMIT_METHODS;
			this.resolvedMethods = DEFAULT_PERMIT_METHODS
					.stream().map(HttpMethod::resolve).collect(Collectors.toList());
		}
		if (this.allowedHeaders == null) {
			this.allowedHeaders = DEFAULT_PERMIT_ALL;
		}
		if (this.maxAge == null) {
			this.maxAge = 1800L;
		}
		return this;
	}

	/**
	 * 校验当 {@link #setAllowCredentials allowCredentials} 为 {@code true} 时，
	 * {@link #setAllowedOrigins allowedOrigins} 不包含特殊值 {@code "*"}，
	 * 因为在这种情况下 "Access-Control-Allow-Origin" 无法设置为 {@code "*"}。
	 * @throws IllegalArgumentException if the validation fails
	 * @since 5.3
	 */
	public void validateAllowCredentials() {
		if (this.allowCredentials == Boolean.TRUE &&
				this.allowedOrigins != null && this.allowedOrigins.contains(ALL)) {

			throw new IllegalArgumentException(
					"When allowCredentials is true, allowedOrigins cannot contain the special value \"*\" " +
							"since that cannot be set on the \"Access-Control-Allow-Origin\" response header. " +
							"To allow credentials to a set of origins, list them explicitly " +
							"or consider using \"allowedOriginPatterns\" instead.");
		}
	}

	/**
	 * 将所提供的 {@code CorsConfiguration} 的非 null 属性与此配置合并。
	 * <p>在合并 {@code allowCredentials} 或 {@code maxAge} 等单值属性时，
	 * 如果 {@code other} 中的属性非 null，则会覆盖 {@code this} 中的属性。
	 * <p>合并 {@code allowedOrigins}、{@code allowedMethods}、
	 * {@code allowedHeaders} 或 {@code exposedHeaders} 等列表时采用累加方式。
	 * 例如，将 {@code ["GET", "POST"]} 与 {@code ["PATCH"]} 合并会得到
	 * {@code ["GET", "POST", "PATCH"]}。不过，将 {@code ["GET", "POST"]}
	 * 与 {@code ["*"]} 合并会得到 {@code ["*"]}。另请注意，任何显式定义的值
	 * 都会覆盖 {@link CorsConfiguration#applyPermitDefaultValues()} 设置的默认许可值。
	 * @return the combined {@code CorsConfiguration}, or {@code this}
	 * configuration if the supplied configuration is {@code null}
	 */
	public CorsConfiguration combine(@Nullable CorsConfiguration other) {
		if (other == null) {
			return this;
		}
		// Bypass setAllowedOrigins to avoid re-compiling patterns
		CorsConfiguration config = new CorsConfiguration(this);
		List<String> origins = combine(getAllowedOrigins(), other.getAllowedOrigins());
		List<OriginPattern> patterns = combinePatterns(this.allowedOriginPatterns, other.allowedOriginPatterns);
		config.allowedOrigins = (origins == DEFAULT_PERMIT_ALL && !CollectionUtils.isEmpty(patterns) ? null : origins);
		config.allowedOriginPatterns = patterns;
		config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));
		config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));
		config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));
		Boolean allowCredentials = other.getAllowCredentials();
		if (allowCredentials != null) {
			config.setAllowCredentials(allowCredentials);
		}
		Long maxAge = other.getMaxAge();
		if (maxAge != null) {
			config.setMaxAge(maxAge);
		}
		return config;
	}

	private List<String> combine(@Nullable List<String> source, @Nullable List<String> other) {
		if (other == null) {
			return (source != null ? source : Collections.emptyList());
		}
		if (source == null) {
			return other;
		}
		if (source == DEFAULT_PERMIT_ALL || source == DEFAULT_PERMIT_METHODS) {
			return other;
		}
		if (other == DEFAULT_PERMIT_ALL || other == DEFAULT_PERMIT_METHODS) {
			return source;
		}
		if (source.contains(ALL) || other.contains(ALL)) {
			return ALL_LIST;
		}
		Set<String> combined = new LinkedHashSet<>(source.size() + other.size());
		combined.addAll(source);
		combined.addAll(other);
		return new ArrayList<>(combined);
	}

	private List<OriginPattern> combinePatterns(
			@Nullable List<OriginPattern> source, @Nullable List<OriginPattern> other) {

		if (other == null) {
			return (source != null ? source : Collections.emptyList());
		}
		if (source == null) {
			return other;
		}
		if (source.contains(ALL_PATTERN) || other.contains(ALL_PATTERN)) {
			return ALL_PATTERN_LIST;
		}
		Set<OriginPattern> combined = new LinkedHashSet<>(source.size() + other.size());
		combined.addAll(source);
		combined.addAll(other);
		return new ArrayList<>(combined);
	}


	/**
	 * 根据配置的允许 origin 检查请求的 origin。
	 * @param origin the origin to check
	 * @return the origin to use for the response, or {@code null} which
	 * means the request origin is not allowed
	 */
	@Nullable
	public String checkOrigin(@Nullable String origin) {
		if (!StringUtils.hasText(origin)) {
			return null;
		}
		String originToCheck = trimTrailingSlash(origin);
		if (!ObjectUtils.isEmpty(this.allowedOrigins)) {
			if (this.allowedOrigins.contains(ALL)) {
				validateAllowCredentials();
				return ALL;
			}
			for (String allowedOrigin : this.allowedOrigins) {
				if (originToCheck.equalsIgnoreCase(allowedOrigin)) {
					return origin;
				}
			}
		}
		if (!ObjectUtils.isEmpty(this.allowedOriginPatterns)) {
			for (OriginPattern p : this.allowedOriginPatterns) {
				if (p.getDeclaredPattern().equals(ALL) || p.getPattern().matcher(originToCheck).matches()) {
					return origin;
				}
			}
		}
		return null;
	}

	/**
	 * 根据配置的允许方法检查 HTTP 请求方法（对于预检请求，则为
	 * {@code Access-Control-Request-Method} 头中的方法）。
	 * @param requestMethod the HTTP request method to check
	 * @return the list of HTTP methods to list in the response of a pre-flight
	 * request, or {@code null} if the supplied {@code requestMethod} is not allowed
	 */
	@Nullable
	public List<HttpMethod> checkHttpMethod(@Nullable HttpMethod requestMethod) {
		if (requestMethod == null) {
			return null;
		}
		if (this.resolvedMethods == null) {
			return Collections.singletonList(requestMethod);
		}
		return (this.resolvedMethods.contains(requestMethod) ? this.resolvedMethods : null);
	}

	/**
	 * 根据配置的允许请求头检查所提供的请求头（或预检请求的
	 * {@code Access-Control-Request-Headers} 中列出的请求头）。
	 * @param requestHeaders the request headers to check
	 * @return the list of allowed headers to list in the response of a pre-flight
	 * request, or {@code null} if none of the supplied request headers is allowed
	 */
	@Nullable
	public List<String> checkHeaders(@Nullable List<String> requestHeaders) {
		if (requestHeaders == null) {
			return null;
		}
		if (requestHeaders.isEmpty()) {
			return Collections.emptyList();
		}
		if (ObjectUtils.isEmpty(this.allowedHeaders)) {
			return null;
		}

		boolean allowAnyHeader = this.allowedHeaders.contains(ALL);
		List<String> result = new ArrayList<>(requestHeaders.size());
		for (String requestHeader : requestHeaders) {
			if (StringUtils.hasText(requestHeader)) {
				requestHeader = requestHeader.trim();
				if (allowAnyHeader) {
					result.add(requestHeader);
				}
				else {
					for (String allowedHeader : this.allowedHeaders) {
						if (requestHeader.equalsIgnoreCase(allowedHeader)) {
							result.add(requestHeader);
							break;
						}
					}
				}
			}
		}
		return (result.isEmpty() ? null : result);
	}


	/**
	 * 同时包含用户声明的模式（例如 "https://*.domain.com"）以及
	 * 由该模式派生得到的正则表达式 {@link Pattern}。
	 */
	private static class OriginPattern {

		private static final Pattern PORTS_PATTERN = Pattern.compile("(.*):\\[(\\*|\\d+(,\\d+)*)]");

		private final String declaredPattern;

		private final Pattern pattern;

		OriginPattern(String declaredPattern) {
			this.declaredPattern = declaredPattern;
			this.pattern = initPattern(declaredPattern);
		}

		private static Pattern initPattern(String patternValue) {
			String portList = null;
			Matcher matcher = PORTS_PATTERN.matcher(patternValue);
			if (matcher.matches()) {
				patternValue = matcher.group(1);
				portList = matcher.group(2);
			}

			patternValue = "\\Q" + patternValue + "\\E";
			patternValue = patternValue.replace("*", "\\E.*\\Q");

			if (portList != null) {
				patternValue += (portList.equals(ALL) ? "(:\\d+)?" : ":(" + portList.replace(',', '|') + ")");
			}

			return Pattern.compile(patternValue);
		}

		public String getDeclaredPattern() {
			return this.declaredPattern;
		}

		public Pattern getPattern() {
			return this.pattern;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || !getClass().equals(other.getClass())) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(
					this.declaredPattern, ((OriginPattern) other).declaredPattern);
		}

		@Override
		public int hashCode() {
			return this.declaredPattern.hashCode();
		}

		@Override
		public String toString() {
			return this.declaredPattern;
		}
	}

}
