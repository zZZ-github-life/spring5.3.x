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

package org.springframework.web.servlet.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.context.support.WebApplicationObjectSupport;

/**
 * 各种 Web 内容生成器的便捷超类，例如 {@link org.springframework.web.servlet.mvc.AbstractController} 和 {@link org.springframework.web.servlet.mvc.WebContentInterceptor}。也可用于拥有自己的 {@link org.springframework.web.servlet.HandlerAdapter} 的自定义处理器。
 *
 * <p>支持 HTTP 缓存控制选项。相应 HTTP 头的使用可通过 {@link #setCacheSeconds "cacheSeconds"} 和 {@link #setCacheControl "cacheControl"} 属性进行控制。
 *
 * <p><b>注意：</b>从 Spring 4.2 开始，当仅使用 {@link #setCacheSeconds} 时，此生成器的默认行为发生了改变，它会发送与当前浏览器和代理实现一致的 HTTP 响应头（即不再发送 HTTP 1.0 头）。通过使用新增的已弃用方法 {@link #setUseExpiresHeader}、{@link #setUseCacheControlHeader}、{@link #setUseCacheControlNoStore} 或 {@link #setAlwaysMustRevalidate} 之一，可以轻松恢复到之前的行为。
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see #setCacheSeconds
 * @see #setCacheControl
 * @see #setRequireSession
 */
public abstract class WebContentGenerator extends WebApplicationObjectSupport {

	/** HTTP 方法 "GET"。 */
	public static final String METHOD_GET = "GET";

	/** HTTP 方法 "HEAD"。 */
	public static final String METHOD_HEAD = "HEAD";

	/** HTTP 方法 "POST"。 */
	public static final String METHOD_POST = "POST";

	private static final String HEADER_PRAGMA = "Pragma";

	private static final String HEADER_EXPIRES = "Expires";

	protected static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/** 支持的 HTTP 方法集合。 */
	@Nullable
	private Set<String> supportedMethods;

	@Nullable
	private String allowHeader;

	private boolean requireSession = false;

	@Nullable
	private CacheControl cacheControl;

	private int cacheSeconds = -1;

	@Nullable
	private String[] varyByRequestHeaders;


	// deprecated fields

	/** 是否使用 HTTP 1.0 的 expires 头？ */
	private boolean useExpiresHeader = false;

	/** 是否使用 HTTP 1.1 的 cache-control 头？ */
	private boolean useCacheControlHeader = true;

	/** 是否使用 HTTP 1.1 的 cache-control 头值 "no-store"？ */
	private boolean useCacheControlNoStore = true;

	private boolean alwaysMustRevalidate = false;


	/**
	 * 创建一个新的 WebContentGenerator，默认支持
	 * HTTP 方法 GET、HEAD 和 POST。
	 */
	public WebContentGenerator() {
		this(true);
	}

	/**
	 * 创建一个新的 WebContentGenerator。
	 * @param restrictDefaultSupportedMethods {@code true} if this
	 * generator should support HTTP methods GET, HEAD and POST by default,
	 * or {@code false} if it should be unrestricted
	 */
	public WebContentGenerator(boolean restrictDefaultSupportedMethods) {
		if (restrictDefaultSupportedMethods) {
			this.supportedMethods = new LinkedHashSet<>(4);
			this.supportedMethods.add(METHOD_GET);
			this.supportedMethods.add(METHOD_HEAD);
			this.supportedMethods.add(METHOD_POST);
		}
		initAllowHeader();
	}

	/**
	 * 创建一个新的 WebContentGenerator。
	 * @param supportedMethods the supported HTTP methods for this content generator
	 */
	public WebContentGenerator(String... supportedMethods) {
		setSupportedMethods(supportedMethods);
	}


	/**
	 * 设置此内容生成器应支持的 HTTP 方法。
	 * <p>对于简单的表单控制器类型，默认是 GET、HEAD 和 POST；对于通用控制器和拦截器则不受限制。
	 */
	public final void setSupportedMethods(@Nullable String... methods) {
		if (!ObjectUtils.isEmpty(methods)) {
			this.supportedMethods = new LinkedHashSet<>(Arrays.asList(methods));
		}
		else {
			this.supportedMethods = null;
		}
		initAllowHeader();
	}

	/**
	 * 返回此内容生成器支持的 HTTP 方法。
	 */
	@Nullable
	public final String[] getSupportedMethods() {
		return (this.supportedMethods != null ? StringUtils.toStringArray(this.supportedMethods) : null);
	}

	private void initAllowHeader() {
		Collection<String> allowedMethods;
		if (this.supportedMethods == null) {
			allowedMethods = new ArrayList<>(HttpMethod.values().length - 1);
			for (HttpMethod method : HttpMethod.values()) {
				if (method != HttpMethod.TRACE) {
					allowedMethods.add(method.name());
				}
			}
		}
		else if (this.supportedMethods.contains(HttpMethod.OPTIONS.name())) {
			allowedMethods = this.supportedMethods;
		}
		else {
			allowedMethods = new ArrayList<>(this.supportedMethods);
			allowedMethods.add(HttpMethod.OPTIONS.name());

		}
		this.allowHeader = StringUtils.collectionToCommaDelimitedString(allowedMethods);
	}

	/**
	 * 返回响应 HTTP OPTIONS 请求时要使用的 "Allow" 头值，该值基于所配置的 {@link #setSupportedMethods 支持的方法}，即使 "OPTIONS" 不在受支持的方法之列也会自动将其添加到列表中。这意味着只要在调用 {@link #checkRequest(HttpServletRequest)} 之前处理 HTTP OPTIONS 请求，子类就无需显式将 "OPTIONS" 列为受支持的方法。
	 * @since 4.3
	 */
	@Nullable
	protected String getAllowHeader() {
		return this.allowHeader;
	}

	/**
	 * 设置处理请求是否要求存在会话。
	 */
	public final void setRequireSession(boolean requireSession) {
		this.requireSession = requireSession;
	}

	/**
	 * 返回处理请求是否要求存在会话。
	 */
	public final boolean isRequireSession() {
		return this.requireSession;
	}

	/**
	 * 设置用于构建 Cache-Control HTTP 响应头的 {@link org.springframework.http.CacheControl} 实例。
	 * @since 4.2
	 */
	public final void setCacheControl(@Nullable CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * 获取用于构建 Cache-Control HTTP 响应头的 {@link org.springframework.http.CacheControl} 实例。
	 * @since 4.2
	 */
	@Nullable
	public final CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * 通过向响应写入与缓存相关的 HTTP 头，将内容缓存指定的秒数：
	 * <ul>
	 * <li>seconds == -1（默认值）：不生成与缓存相关的头</li>
	 * <li>seconds == 0："Cache-Control: no-store" 将阻止缓存</li>
	 * <li>seconds &gt; 0："Cache-Control: max-age=seconds" 将要求缓存内容</li>
	 * </ul>
	 * <p>对于更具体的需求，应使用自定义的 {@link org.springframework.http.CacheControl}。
	 * @see #setCacheControl
	 */
	public final void setCacheSeconds(int seconds) {
		this.cacheSeconds = seconds;
	}

	/**
	 * 返回内容被缓存的秒数。
	 */
	public final int getCacheSeconds() {
		return this.cacheSeconds;
	}

	/**
	 * 配置一个或多个请求头名称（例如 "Accept-Language"），将其添加到 "Vary" 响应头，以告知客户端该响应会进行内容协商，并会根据给定请求头的值发生变化。只有在响应的 "Vary" 头中尚不存在时，才会添加所配置的请求头名称。
	 * @param varyByRequestHeaders one or more request header names
	 * @since 4.3
	 */
	public final void setVaryByRequestHeaders(@Nullable String... varyByRequestHeaders) {
		this.varyByRequestHeaders = varyByRequestHeaders;
	}

	/**
	 * 返回为 "Vary" 响应头配置的请求头名称。
	 * @since 4.3
	 */
	@Nullable
	public final String[] getVaryByRequestHeaders() {
		return this.varyByRequestHeaders;
	}

	/**
	 * 设置是否使用 HTTP 1.0 的 expires 头。从 4.2 开始默认值为 "false"。
	 * <p>注意：只有在为当前请求启用了缓存（或显式阻止缓存）时，缓存头才会被应用。
	 * @deprecated as of 4.2, since going forward, the HTTP 1.1 cache-control
	 * header will be required, with the HTTP 1.0 headers disappearing
	 */
	@Deprecated
	public final void setUseExpiresHeader(boolean useExpiresHeader) {
		this.useExpiresHeader = useExpiresHeader;
	}

	/**
	 * 返回是否使用 HTTP 1.0 的 expires 头。
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseExpiresHeader() {
		return this.useExpiresHeader;
	}

	/**
	 * 设置是否使用 HTTP 1.1 的 cache-control 头。默认值为 "true"。
	 * <p>注意：只有在为当前请求启用了缓存（或显式阻止缓存）时，缓存头才会被应用。
	 * @deprecated as of 4.2, since going forward, the HTTP 1.1 cache-control
	 * header will be required, with the HTTP 1.0 headers disappearing
	 */
	@Deprecated
	public final void setUseCacheControlHeader(boolean useCacheControlHeader) {
		this.useCacheControlHeader = useCacheControlHeader;
	}

	/**
	 * 返回是否使用 HTTP 1.1 的 cache-control 头。
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseCacheControlHeader() {
		return this.useCacheControlHeader;
	}

	/**
	 * 设置阻止缓存时是否使用 HTTP 1.1 的 cache-control 头值 "no-store"。默认值为 "true"。
	 * @deprecated as of 4.2, in favor of {@link #setCacheControl}
	 */
	@Deprecated
	public final void setUseCacheControlNoStore(boolean useCacheControlNoStore) {
		this.useCacheControlNoStore = useCacheControlNoStore;
	}

	/**
	 * 返回是否使用 HTTP 1.1 的 cache-control 头值 "no-store"。
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseCacheControlNoStore() {
		return this.useCacheControlNoStore;
	}

	/**
	 * 一个用于向每个 Cache-Control 头添加 'must-revalidate' 的选项。这对于注解式控制器方法可能很有用，此类方法可以按 {@link org.springframework.web.context.request.WebRequest#checkNotModified(long)} 中所述以编程方式进行最后修改时间的计算。
	 * <p>默认值为 "false"。
	 * @deprecated as of 4.2, in favor of {@link #setCacheControl}
	 */
	@Deprecated
	public final void setAlwaysMustRevalidate(boolean mustRevalidate) {
		this.alwaysMustRevalidate = mustRevalidate;
	}

	/**
	 * 返回是否向每个 Cache-Control 头添加 'must-revalidate'。
	 * @deprecated as of 4.2, in favor of {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isAlwaysMustRevalidate() {
		return this.alwaysMustRevalidate;
	}


	/**
	 * 检查给定请求是否使用了受支持的方法，以及是否存在所需的会话（如果有）。
	 * @param request current HTTP request
	 * @throws ServletException if the request cannot be handled because a check failed
	 * @since 4.2
	 */
	protected final void checkRequest(HttpServletRequest request) throws ServletException {
		// Check whether we should support the request method.
		String method = request.getMethod();
		if (this.supportedMethods != null && !this.supportedMethods.contains(method)) {
			throw new HttpRequestMethodNotSupportedException(method, this.supportedMethods);
		}

		// Check whether a session is required.
		if (this.requireSession && request.getSession(false) == null) {
			throw new HttpSessionRequiredException("Pre-existing session required but none found");
		}
	}

	/**
	 * 根据此生成器的设置准备给定的响应。
	 * 会应用为此生成器指定的缓存秒数。
	 * @param response current HTTP response
	 * @since 4.2
	 */
	protected final void prepareResponse(HttpServletResponse response) {
		if (this.cacheControl != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying default " + getCacheControl());
			}
			applyCacheControl(response, this.cacheControl);
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Applying default cacheSeconds=" + this.cacheSeconds);
			}
			applyCacheSeconds(response, this.cacheSeconds);
		}
		if (this.varyByRequestHeaders != null) {
			for (String value : getVaryRequestHeadersToAdd(response, this.varyByRequestHeaders)) {
				response.addHeader("Vary", value);
			}
		}
	}

	/**
	 * 根据给定设置设置 HTTP Cache-Control 头。
	 * @param response current HTTP response
	 * @param cacheControl the pre-configured cache control settings
	 * @since 4.2
	 */
	protected final void applyCacheControl(HttpServletResponse response, CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			// Set computed HTTP 1.1 Cache-Control header
			response.setHeader(HEADER_CACHE_CONTROL, ccValue);

			if (response.containsHeader(HEADER_PRAGMA)) {
				// Reset HTTP 1.0 Pragma header if present
				response.setHeader(HEADER_PRAGMA, "");
			}
			if (response.containsHeader(HEADER_EXPIRES)) {
				// Reset HTTP 1.0 Expires header if present
				response.setHeader(HEADER_EXPIRES, "");
			}
		}
	}

	/**
	 * 应用给定的缓存秒数并生成相应的 HTTP 头，即：如果为正值，则允许在给定秒数内缓存；如果为 0，则阻止缓存；否则不做任何操作。
	 * 不会通知浏览器重新验证资源。
	 * @param response current HTTP response
	 * @param cacheSeconds positive number of seconds into the future that the
	 * response should be cacheable for, 0 to prevent caching
	 */
	@SuppressWarnings("deprecation")
	protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds) {
		if (this.useExpiresHeader || !this.useCacheControlHeader) {
			// Deprecated HTTP 1.0 cache behavior, as in previous Spring versions
			if (cacheSeconds > 0) {
				cacheForSeconds(response, cacheSeconds);
			}
			else if (cacheSeconds == 0) {
				preventCaching(response);
			}
		}
		else {
			CacheControl cControl;
			if (cacheSeconds > 0) {
				cControl = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS);
				if (this.alwaysMustRevalidate) {
					cControl = cControl.mustRevalidate();
				}
			}
			else if (cacheSeconds == 0) {
				cControl = (this.useCacheControlNoStore ? CacheControl.noStore() : CacheControl.noCache());
			}
			else {
				cControl = CacheControl.empty();
			}
			applyCacheControl(response, cControl);
		}
	}


	/**
	 * 根据此生成器的设置检查并准备给定的请求和响应。
	 * @see #checkRequest(HttpServletRequest)
	 * @see #prepareResponse(HttpServletResponse)
	 * @deprecated as of 4.2, since the {@code lastModified} flag is effectively ignored,
	 * with a must-revalidate header only generated if explicitly configured
	 */
	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, boolean lastModified) throws ServletException {

		checkRequest(request);
		prepareResponse(response);
	}

	/**
	 * 根据此生成器的设置检查并准备给定的请求和响应。
	 * @see #checkRequest(HttpServletRequest)
	 * @see #applyCacheSeconds(HttpServletResponse, int)
	 * @deprecated as of 4.2, since the {@code lastModified} flag is effectively ignored,
	 * with a must-revalidate header only generated if explicitly configured
	 */
	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, int cacheSeconds, boolean lastModified)
			throws ServletException {

		checkRequest(request);
		applyCacheSeconds(response, cacheSeconds);
	}

	/**
	 * 应用给定的缓存秒数并生成相应的 HTTP 头。
	 * <p>即：如果为正值，则允许在给定秒数内缓存；如果为 0，则阻止缓存；否则不做任何操作（即将缓存交给客户端决定）。
	 * @param response the current HTTP response
	 * @param cacheSeconds the (positive) number of seconds into the future
	 * that the response should be cacheable for; 0 to prevent caching; and
	 * a negative value to leave caching to the client.
	 * @param mustRevalidate whether the client should revalidate the resource
	 * (typically only necessary for controllers with last-modified support)
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds, boolean mustRevalidate) {
		if (cacheSeconds > 0) {
			cacheForSeconds(response, cacheSeconds, mustRevalidate);
		}
		else if (cacheSeconds == 0) {
			preventCaching(response);
		}
	}

	/**
	 * 设置 HTTP 头以允许在给定秒数内缓存。
	 * 不会通知浏览器重新验证资源。
	 * @param response current HTTP response
	 * @param seconds number of seconds into the future that the response
	 * should be cacheable for
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void cacheForSeconds(HttpServletResponse response, int seconds) {
		cacheForSeconds(response, seconds, false);
	}

	/**
	 * 设置 HTTP 头以允许在给定秒数内缓存。
	 * 如果 mustRevalidate 为 {@code true}，则通知浏览器重新验证资源。
	 * @param response the current HTTP response
	 * @param seconds number of seconds into the future that the response
	 * should be cacheable for
	 * @param mustRevalidate whether the client should revalidate the resource
	 * (typically only necessary for controllers with last-modified support)
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void cacheForSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (this.useExpiresHeader) {
			// HTTP 1.0 header
			response.setDateHeader(HEADER_EXPIRES, System.currentTimeMillis() + seconds * 1000L);
		}
		else if (response.containsHeader(HEADER_EXPIRES)) {
			// Reset HTTP 1.0 Expires header if present
			response.setHeader(HEADER_EXPIRES, "");
		}

		if (this.useCacheControlHeader) {
			// HTTP 1.1 header
			String headerValue = "max-age=" + seconds;
			if (mustRevalidate || this.alwaysMustRevalidate) {
				headerValue += ", must-revalidate";
			}
			response.setHeader(HEADER_CACHE_CONTROL, headerValue);
		}

		if (response.containsHeader(HEADER_PRAGMA)) {
			// Reset HTTP 1.0 Pragma header if present
			response.setHeader(HEADER_PRAGMA, "");
		}
	}

	/**
	 * 阻止响应被缓存。
	 * 仅在 HTTP 1.0 兼容模式下调用。
	 * <p>参见 {@code https://www.mnot.net/cache_docs}。
	 * @deprecated as of 4.2, in favor of {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void preventCaching(HttpServletResponse response) {
		response.setHeader(HEADER_PRAGMA, "no-cache");

		if (this.useExpiresHeader) {
			// HTTP 1.0 Expires header
			response.setDateHeader(HEADER_EXPIRES, 1L);
		}

		if (this.useCacheControlHeader) {
			// HTTP 1.1 Cache-Control header: "no-cache" is the standard value,
			// "no-store" is necessary to prevent caching on Firefox.
			response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
			if (this.useCacheControlNoStore) {
				response.addHeader(HEADER_CACHE_CONTROL, "no-store");
			}
		}
	}


	private Collection<String> getVaryRequestHeadersToAdd(HttpServletResponse response, String[] varyByRequestHeaders) {
		if (!response.containsHeader(HttpHeaders.VARY)) {
			return Arrays.asList(varyByRequestHeaders);
		}
		Collection<String> result = new ArrayList<>(varyByRequestHeaders.length);
		Collections.addAll(result, varyByRequestHeaders);
		for (String header : response.getHeaders(HttpHeaders.VARY)) {
			for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
				if ("*".equals(existing)) {
					return Collections.emptyList();
				}
				for (String value : varyByRequestHeaders) {
					if (value.equalsIgnoreCase(existing)) {
						result.remove(value);
					}
				}
			}
		}
		return result;
	}

}
