/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.function.Consumer;

import javax.servlet.http.Cookie;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

/**
 * {@link ServerResponse} 的实体特定子类型，暴露实体数据。
 *
 * @author Arjen Poutsma
 * @since 5.2
 * @param <T> 实体类型
 */
public interface EntityResponse<T> extends ServerResponse {

	/**
	 * 返回构成此响应的实体。
	 */
	T entity();


	// Static builder methods

	/**
	 * 使用给定对象创建构建器。
	 * @param t 表示响应正文的对象
	 * @param <T> 实体中包含的元素类型
	 * @return 创建的构建器
	 */
	static <T> Builder<T> fromObject(T t) {
		return DefaultEntityResponseBuilder.fromObject(t);
	}

	/**
	 * 使用给定对象和类型引用创建构建器。
	 * @param t 表示响应正文的对象
	 * @param entityType 实体的类型，用于捕获泛型信息
	 * @param <T> 实体中包含的元素类型
	 * @return 创建的构建器
	 */
	static <T> Builder<T> fromObject(T t, ParameterizedTypeReference<T> entityType) {
		return DefaultEntityResponseBuilder.fromObject(t, entityType);
	}


	/**
	 * 定义 {@code EntityResponse} 的构建器。
	 * @param <T> 实体类型
	 */
	interface Builder<T> {

		/**
		 * 在给定名称下添加给定的标头值。
		 * @param headerName 头名称
		 * @param headerValues 头值
		 * @return 此构建器
		 * @see HttpHeaders#add(String, String)
		 */
		Builder<T> header(String headerName, String... headerValues);

		/**
		 * 使用给定消费者操作此响应的标头。提供给消费者的标头是"实时"的，
		 * 因此消费者可以用它来{@linkplain HttpHeaders#set(String, String) 覆盖}现有标头值、
		 * {@linkplain HttpHeaders#remove(Object) 移除}值，或使用任何其他
		 * {@link HttpHeaders} 方法。
		 * @param headersConsumer 消费 {@code HttpHeaders} 的函数
		 * @return 此构建器
		 */
		Builder<T> headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 设置 HTTP 状态。
		 * @param status 响应状态
		 * @return 此构建器
		 */
		Builder<T> status(HttpStatus status);

		/**
		 * 设置 HTTP 状态。
		 * @param status 响应状态
		 * @return 此构建器
		 */
		Builder<T> status(int status);

		/**
		 * 将给定 cookie 添加到响应中。
		 * @param cookie 要添加的 Cookie
		 * @return 此构建器
		 */
		Builder<T> cookie(Cookie cookie);

		/**
		 * 使用给定消费者操作此响应的 cookie。提供给消费者的 cookie 是"实时"的，
		 * 因此消费者可以用它来{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有 cookie、
		 * {@linkplain MultiValueMap#remove(Object) 移除}cookie，或使用任何其他
		 * {@link MultiValueMap} 方法。
		 * @param cookiesConsumer 消费 Cookie 的函数
		 * @return 此构建器
		 */
		Builder<T> cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由 {@code Allow} 标头指定。
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(HttpMethod... allowedMethods);

		/**
		 * 设置允许的 {@link HttpMethod HTTP 方法}集合，由 {@code Allow} 标头指定。
		 * @param allowedMethods 允许的方法
		 * @return 此构建器
		 * @see HttpHeaders#setAllow(Set)
		 */
		Builder<T> allow(Set<HttpMethod> allowedMethods);

		/**
		 * 设置响应体的实体标签，由 {@code ETag} 标头指定。
		 * @param etag 新的实体标签
		 * @return 此构建器
		 * @see HttpHeaders#setETag(String)
		 */
		Builder<T> eTag(String etag);

		/**
		 * 设置资源最后更改的时间，由 {@code Last-Modified} 标头指定。
		 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(ZonedDateTime lastModified);

		/**
		 * 设置资源最后更改的时间，由 {@code Last-Modified} 标头指定。
		 * <p>日期应指定为自格林尼治标准时间 1970 年 1 月 1 日以来的毫秒数。
		 * @param lastModified 最后修改日期
		 * @return 此构建器
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(long)
		 */
		Builder<T> lastModified(Instant lastModified);
		/**
		 * 设置资源的位置，由 {@code Location} 标头指定。
		 * @param location 位置
		 * @return 此构建器
		 * @see HttpHeaders#setLocation(URI)
		 */
		Builder<T> location(URI location);

		/**
		 * 设置资源的缓存指令，由 HTTP 1.1 的 {@code Cache-Control} 标头指定。
		 * <p>{@code CacheControl} 实例可以像
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()} 这样构建。
		 * @param cacheControl 用于缓存相关 HTTP 响应头的构建器
		 * @return 此构建器
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		Builder<T> cacheControl(CacheControl cacheControl);

		/**
		 * 配置一个或多个请求标头名称（例如 "Accept-Language"），将其添加到
		 * "Vary" 响应标头中，以告知客户端响应内容需要经过内容协商，
		 * 并且会因给定请求标头的值而变化。只有在响应 "Vary" 标头中尚不存在时，
		 * 才会添加配置的请求标头名称。
		 * @param requestHeaders 请求头名称
		 * @return 此构建器
		 */
		Builder<T> varyBy(String... requestHeaders);

		/**
		 * 设置响应体的长度（以字节为单位），由 {@code Content-Length} 标头指定。
		 * @param contentLength 内容长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		Builder<T> contentLength(long contentLength);

		/**
		 * 设置响应体的{@linkplain MediaType 媒体类型}，由 {@code Content-Type} 标头指定。
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		Builder<T> contentType(MediaType contentType);

		/**
		 * 构建响应。
		 * @return 构建好的响应
		 */
		EntityResponse<T> build();
	}

}
