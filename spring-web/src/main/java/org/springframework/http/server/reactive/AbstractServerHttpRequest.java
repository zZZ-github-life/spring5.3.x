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

package org.springframework.http.server.reactive;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ServerHttpRequest} 实现的公共基类。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractServerHttpRequest implements ServerHttpRequest {

	private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");


	private final URI uri;

	private final RequestPath path;

	private final HttpHeaders headers;

	@Nullable
	private MultiValueMap<String, String> queryParams;

	@Nullable
	private MultiValueMap<String, HttpCookie> cookies;

	@Nullable
	private SslInfo sslInfo;

	@Nullable
	private String id;

	@Nullable
	private String logPrefix;


	/**
	 * 使用请求的 URI 和头构造。
	 * @param uri 请求的 URI
	 * @param contextPath 请求的上下文路径
	 * @param headers 请求的头（作为 {@link MultiValueMap}）
	 * @since 5.3
	 */
	public AbstractServerHttpRequest(URI uri, @Nullable String contextPath, MultiValueMap<String, String> headers) {
		this.uri = uri;
		this.path = RequestPath.parse(uri, contextPath);
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}

	/**
	 * 使用请求的 URI 和头构造。
	 * @param uri 请求的 URI
	 * @param contextPath 请求的上下文路径
	 * @param headers 请求的头（作为 {@link HttpHeaders}）
	 */
	public AbstractServerHttpRequest(URI uri, @Nullable String contextPath, HttpHeaders headers) {
		this.uri = uri;
		this.path = RequestPath.parse(uri, contextPath);
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}


	@Override
	public String getId() {
		if (this.id == null) {
			this.id = initId();
			if (this.id == null) {
				this.id = ObjectUtils.getIdentityHexString(this);
			}
		}
		return this.id;
	}

	/**
	 * 获取要使用的请求 id，如果返回 {@code null}，则使用此请求实例的
	 * 对象标识。
	 * @since 5.1
	 */
	@Nullable
	protected String initId() {
		return null;
	}

	@Override
	public URI getURI() {
		return this.uri;
	}

	@Override
	public RequestPath getPath() {
		return this.path;
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, String> getQueryParams() {
		if (this.queryParams == null) {
			this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
		}
		return this.queryParams;
	}

	/**
	 * 将查询解析为名称-值对的方法。返回值会被转换为不可变 Map
	 * 并缓存。
	 * <p>请注意，此方法在首次访问 {@link #getQueryParams()} 时延迟调用。
	 * 该调用不是同步的，但解析过程仍然是线程安全的。
	 */
	protected MultiValueMap<String, String> initQueryParams() {
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		String query = getURI().getRawQuery();
		if (query != null) {
			Matcher matcher = QUERY_PATTERN.matcher(query);
			while (matcher.find()) {
				String name = decodeQueryParam(matcher.group(1));
				String eq = matcher.group(2);
				String value = matcher.group(3);
				value = (value != null ? decodeQueryParam(value) : (StringUtils.hasLength(eq) ? "" : null));
				queryParams.add(name, value);
			}
		}
		return queryParams;
	}

	@SuppressWarnings("deprecation")
	private String decodeQueryParam(String value) {
		try {
			return URLDecoder.decode(value, "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			// Should never happen but we got a platform default fallback anyway.
			return URLDecoder.decode(value);
		}
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (this.cookies == null) {
			this.cookies = CollectionUtils.unmodifiableMultiValueMap(initCookies());
		}
		return this.cookies;
	}

	/**
	 * 从底层"原生"请求获取 cookie，并将其适配为
	 * {@link HttpCookie} Map。返回值会被转换为不可变 Map 并缓存。
	 * <p>请注意，此方法在访问 {@link #getCookies()} 时延迟调用。
	 * 如果底层"原生"请求不提供对 cookie 数据的线程安全访问，
	 * 子类应同步 cookie 的初始化。
	 */
	protected abstract MultiValueMap<String, HttpCookie> initCookies();

	@Nullable
	@Override
	public SslInfo getSslInfo() {
		if (this.sslInfo == null) {
			this.sslInfo = initSslInfo();
		}
		return this.sslInfo;
	}

	/**
	 * 从底层"原生"请求获取 SSL 会话信息。
	 * @return 会话信息，如果没有可用会话则为 {@code null}
	 * @since 5.0.2
	 */
	@Nullable
	protected abstract SslInfo initSslInfo();

	/**
	 * 返回底层的服务器响应。
	 * <p><strong>注意：</strong>主要公开此方法供框架内部使用，
	 * 例如 spring-webflux 模块中的 WebSocket 升级。
	 */
	public abstract <T> T getNativeRequest();

	/**
	 * 供 HTTP 适配器层记录日志时内部使用。
	 * @since 5.1
	 */
	String getLogPrefix() {
		if (this.logPrefix == null) {
			this.logPrefix = "[" + initLogPrefix() + "] ";
		}
		return this.logPrefix;
	}

	/**
	 * 子类可以重写此方法，以提供日志消息使用的前缀。
	 * <p>默认情况下，该前缀为 {@link #getId()}。
	 * @since 5.3.15
	 */
	protected String initLogPrefix() {
		return getId();
	}

}
