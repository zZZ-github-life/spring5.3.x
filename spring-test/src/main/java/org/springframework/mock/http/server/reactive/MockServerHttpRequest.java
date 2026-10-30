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

package org.springframework.mock.http.server.reactive;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link AbstractServerHttpRequest} 的 mock 扩展，用于在没有
 * 实际服务器的情况下进行测试。使用静态方法获取构建器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class MockServerHttpRequest extends AbstractServerHttpRequest {

	/**
	 * 某个 {@link HttpMethod} 的字符串表示形式，或非空的自定义方法（例如 <i>CONNECT</i>）。
	 */
	private final String httpMethod;

	private final MultiValueMap<String, HttpCookie> cookies;

	@Nullable
	private final InetSocketAddress localAddress;

	@Nullable
	private final InetSocketAddress remoteAddress;

	@Nullable
	private final SslInfo sslInfo;

	private final Flux<DataBuffer> body;

	private MockServerHttpRequest(String httpMethod,
			URI uri, @Nullable String contextPath, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies,
			@Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress,
			@Nullable SslInfo sslInfo, Publisher<? extends DataBuffer> body) {

		super(uri, contextPath, headers);
		Assert.isTrue(StringUtils.hasText(httpMethod), "HTTP method is required.");
		this.httpMethod = httpMethod;
		this.cookies = cookies;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.sslInfo = sslInfo;
		this.body = Flux.from(body);
	}


	@Override
	@Nullable
	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.httpMethod);
	}

	@Override
	public String getMethodValue() {
		return this.httpMethod;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	@Nullable
	protected SslInfo initSslInfo() {
		return this.sslInfo;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		return this.cookies;
	}

	@Override
	public <T> T getNativeRequest() {
		throw new IllegalStateException("This is a mock. No running server, no native request.");
	}


	// Static builder methods

	/**
	 * 使用给定的 URI 模板创建一个 HTTP GET 构建器。给定的 URI 可以
	 * 包含查询参数，也可以稍后通过
	 * {@link BaseBuilder#queryParam queryParam} 构建器方法添加。
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars 零个或多个 URI 变量
	 * @return 所创建的构建器
	 */
	public static BaseBuilder<?> get(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.GET, urlTemplate, uriVars);
	}

	/**
	 * HTTP HEAD 变体。一般信息请参见 {@link #get(String, Object...)}。
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars 零个或多个 URI 变量
	 * @return 所创建的构建器
	 */
	public static BaseBuilder<?> head(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.HEAD, urlTemplate, uriVars);
	}

	/**
	 * HTTP POST 变体。一般信息请参见 {@link #get(String, Object...)}。
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars 零个或多个 URI 变量
	 * @return 所创建的构建器
	 */
	public static BodyBuilder post(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.POST, urlTemplate, uriVars);
	}

	/**
	 * HTTP PUT 变体。一般信息请参见 {@link #get(String, Object...)}。
	 * {@link BaseBuilder#queryParam queryParam} 构建器方法。
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars 零个或多个 URI 变量
	 * @return 所创建的构建器
	 */
	public static BodyBuilder put(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PUT, urlTemplate, uriVars);
	}

	/**
	 * HTTP PATCH 变体。一般信息请参见 {@link #get(String, Object...)}。
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars 零个或多个 URI 变量
	 * @return 所创建的构建器
	 */
	public static BodyBuilder patch(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PATCH, urlTemplate, uriVars);
	}

	/**
	 * HTTP DELETE 变体。一般信息请参见 {@link #get(String, Object...)}。
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars 零个或多个 URI 变量
	 * @return 所创建的构建器
	 */
	public static BaseBuilder<?> delete(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.DELETE, urlTemplate, uriVars);
	}

	/**
	 * HTTP OPTIONS 变体。一般信息请参见 {@link #get(String, Object...)}。
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars 零个或多个 URI 变量
	 * @return 所创建的构建器
	 */
	public static BaseBuilder<?> options(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.OPTIONS, urlTemplate, uriVars);
	}

	/**
	 * 使用给定的 HTTP 方法和 {@link URI} 创建构建器。
	 * @param method HTTP 方法（GET、POST 等）
	 * @param url 该 URL
	 * @return 所创建的构建器
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		Assert.notNull(method, "HTTP method is required. " +
				"For a custom HTTP method, please provide a String HTTP method value.");
		return new DefaultBodyBuilder(method.name(), url);
	}

	/**
	 * {@link #method(HttpMethod, URI)} 的替代方法，接受 URI 模板。
	 * 给定的 URI 可以包含查询参数，也可以稍后通过
	 * {@link BaseBuilder#queryParam queryParam} 构建器方法添加。
	 * @param method HTTP 方法（GET、POST 等）
	 * @param uri 目标 URL 的 URI 模板
	 * @param vars 要展开到模板中的变量
	 * @return 所创建的构建器
	 */
	public static BodyBuilder method(HttpMethod method, String uri, Object... vars) {
		return method(method, toUri(uri, vars));
	}

	/**
	 * 使用超出 {@link HttpMethod} 枚举值范围的
	 * 原始 HTTP 方法值创建构建器。
	 * @param httpMethod HTTP methodValue 值
	 * @param uri 目标 URL 的 URI 模板
	 * @param vars 要展开到模板中的变量
	 * @return 所创建的构建器
	 * @since 5.2.7
	 */
	public static BodyBuilder method(String httpMethod, String uri, Object... vars) {
		return new DefaultBodyBuilder(httpMethod, toUri(uri, vars));
	}

	private static URI toUri(String uri, Object[] vars) {
		return UriComponentsBuilder.fromUriString(uri).buildAndExpand(vars).encode().toUri();
	}


	/**
	 * 请求构建器，暴露与正文无关的属性。
	 * @param <B> 构建器子类
	 */
	public interface BaseBuilder<B extends BaseBuilder<B>> {

		/**
		 * 设置要返回的 contextPath。
		 */
		B contextPath(String contextPath);

		/**
		 * 将给定的查询参数追加到现有查询参数中。
		 * 如果未提供值，生成的 URI 将仅包含查询参数名称
		 * （即 {@code ?foo} 而不是 {@code ?foo=bar}）。
		 * <p>提供的查询名称和值将被编码。
		 * @param name 查询参数名称
		 * @param values 查询参数值
		 * @return 该 UriComponentsBuilder
		 */
		B queryParam(String name, Object... values);

		/**
		 * 添加给定的查询参数及其值。提供的查询名称
		 * 和对应的值将被编码。
		 * @param params 参数
		 * @return 该 UriComponentsBuilder
		 */
		B queryParams(MultiValueMap<String, String> params);

		/**
		 * 设置要返回的远程地址。
		 */
		B remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * 设置要返回的本地地址。
		 * @since 5.2.3
		 */
		B localAddress(InetSocketAddress localAddress);

		/**
		 * 设置 SSL 会话信息和证书。
		 */
		void sslInfo(SslInfo sslInfo);

		/**
		 * 添加一个或多个 Cookie。
		 */
		B cookie(HttpCookie... cookie);

		/**
		 * 添加给定的 Cookie。
		 * @param cookies 这些 Cookie。
		 */
		B cookies(MultiValueMap<String, HttpCookie> cookies);

		/**
		 * 在给定名称下添加给定的单个头值。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 添加给定的头值。
		 * @param headers 头值
		 */
		B headers(MultiValueMap<String, String> headers);

		/**
		 * 设置可接受的 {@linkplain MediaType 媒体类型} 列表，
		 * 由 {@code Accept} 头指定。
		 * @param acceptableMediaTypes 可接受的媒体类型
		 */
		B accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置可接受的 {@linkplain Charset 字符集} 列表，
		 * 由 {@code Accept-Charset} 头指定。
		 * @param acceptableCharsets 可接受的字符集
		 */
		B acceptCharset(Charset... acceptableCharsets);

		/**
		 * 设置可接受的 {@linkplain Locale 区域设置} 列表，
		 * 由 {@code Accept-Languages} 头指定。
		 * @param acceptableLocales 可接受的语言环境
		 */
		B acceptLanguageAsLocales(Locale... acceptableLocales);

		/**
		 * 设置 {@code If-Modified-Since} 头的值。
		 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
		 * @param ifModifiedSince 头的新值
		 */
		B ifModifiedSince(long ifModifiedSince);

		/**
		 * 设置 {@code If-Unmodified-Since} 头的（新）值。
		 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
		 * @param ifUnmodifiedSince 头的新值
		 * @see HttpHeaders#setIfUnmodifiedSince(long)
		 */
		B ifUnmodifiedSince(long ifUnmodifiedSince);

		/**
		 * 设置 {@code If-None-Match} 头的值。
		 * @param ifNoneMatches 头的新值
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * 设置 Range 头的（新）值。
		 * @param ranges HTTP 范围
		 * @see HttpHeaders#setRange(List)
		 */
		B range(HttpRange... ranges);

		/**
		 * 构建不带正文的请求。
		 * @return 请求
		 * @see BodyBuilder#body(Publisher)
		 * @see BodyBuilder#body(String)
		 */
		MockServerHttpRequest build();
	}


	/**
	 * 向请求添加正文的构建器。
	 */
	public interface BodyBuilder extends BaseBuilder<BodyBuilder> {

		/**
		 * 设置正文的长度（以字节为单位），
		 * 由 {@code Content-Length} 头指定。
		 * @param contentLength 内容长度
		 * @return 该构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置正文的 {@linkplain MediaType 媒体类型}，
		 * 由 {@code Content-Type} 头指定。
		 * @param contentType 内容类型
		 * @return 该构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 设置请求的正文并构建请求。
		 * @param body 正文
		 * @return 构建出的请求实体
		 */
		MockServerHttpRequest body(Publisher<? extends DataBuffer> body);

		/**
		 * 设置请求的正文并构建请求。
		 * <p>除非请求具有带 charset 属性的 "content-type" 头，
		 * 否则假定该 String 采用 UTF-8 编码。
		 * @param body 正文文本
		 * @return 构建出的请求实体
		 */
		MockServerHttpRequest body(String body);
	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		private final String methodValue;

		private final URI url;

		@Nullable
		private String contextPath;

		private final UriComponentsBuilder queryParamsBuilder = UriComponentsBuilder.newInstance();

		private final HttpHeaders headers = new HttpHeaders();

		private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

		@Nullable
		private InetSocketAddress remoteAddress;

		@Nullable
		private InetSocketAddress localAddress;

		@Nullable
		private SslInfo sslInfo;

		DefaultBodyBuilder(String method, URI url) {
			this.methodValue = method;
			this.url = url;
		}

		@Override
		public BodyBuilder contextPath(String contextPath) {
			this.contextPath = contextPath;
			return this;
		}

		@Override
		public BodyBuilder queryParam(String name, Object... values) {
			this.queryParamsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public BodyBuilder queryParams(MultiValueMap<String, String> params) {
			this.queryParamsBuilder.queryParams(params);
			return this;
		}

		@Override
		public BodyBuilder remoteAddress(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
			return this;
		}

		@Override
		public BodyBuilder localAddress(InetSocketAddress localAddress) {
			this.localAddress = localAddress;
			return this;
		}

		@Override
		public void sslInfo(SslInfo sslInfo) {
			this.sslInfo = sslInfo;
		}

		@Override
		public BodyBuilder cookie(HttpCookie... cookies) {
			Arrays.stream(cookies).forEach(cookie -> this.cookies.add(cookie.getName(), cookie));
			return this;
		}

		@Override
		public BodyBuilder cookies(MultiValueMap<String, HttpCookie> cookies) {
			this.cookies.putAll(cookies);
			return this;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				this.headers.add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public BodyBuilder headers(MultiValueMap<String, String> headers) {
			this.headers.putAll(headers);
			return this;
		}

		@Override
		public BodyBuilder accept(MediaType... acceptableMediaTypes) {
			this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
			this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public BodyBuilder acceptLanguageAsLocales(Locale... acceptableLocales) {
			this.headers.setAcceptLanguageAsLocales(Arrays.asList(acceptableLocales));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(long ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifUnmodifiedSince(long ifUnmodifiedSince) {
			this.headers.setIfUnmodifiedSince(ifUnmodifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public BodyBuilder range(HttpRange... ranges) {
			this.headers.setRange(Arrays.asList(ranges));
			return this;
		}

		@Override
		public MockServerHttpRequest build() {
			return body(Flux.empty());
		}

		@Override
		public MockServerHttpRequest body(String body) {
			byte[] bytes = body.getBytes(getCharset());
			DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
			return body(Flux.just(buffer));
		}

		private Charset getCharset() {
			return Optional.ofNullable(this.headers.getContentType())
					.map(MimeType::getCharset).orElse(StandardCharsets.UTF_8);
		}

		@Override
		public MockServerHttpRequest body(Publisher<? extends DataBuffer> body) {
			applyCookiesIfNecessary();
			return new MockServerHttpRequest(this.methodValue, getUrlToUse(), this.contextPath,
					this.headers, this.cookies, this.localAddress, this.remoteAddress, this.sslInfo, body);
		}

		private void applyCookiesIfNecessary() {
			if (this.headers.get(HttpHeaders.COOKIE) == null) {
				this.cookies.values().stream().flatMap(Collection::stream)
						.forEach(cookie -> this.headers.add(HttpHeaders.COOKIE, cookie.toString()));
			}
		}

		private URI getUrlToUse() {
			MultiValueMap<String, String> params =
					this.queryParamsBuilder.buildAndExpand().encode().getQueryParams();
			if (!params.isEmpty()) {
				return UriComponentsBuilder.fromUri(this.url).queryParams(params).build(true).toUri();
			}
			return this.url;
		}
	}

}
