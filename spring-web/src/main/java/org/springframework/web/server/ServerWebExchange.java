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

package org.springframework.web.server;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * HTTP 请求-响应交互的契约。提供对 HTTP 请求和响应的访问，
 * 同时还暴露与服务器端处理相关的其他属性和特性，例如请求属性。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerWebExchange {

	/**
	 * {@link #getAttributes() 属性}的名称，其值可用于关联此交换的日志消息。
	 * 使用 {@link #getLogPrefix()} 可基于该属性获取格式一致的前缀。
	 * @since 5.1
	 * @see #getLogPrefix()
	 */
	String LOG_ID_ATTRIBUTE = ServerWebExchange.class.getName() + ".LOG_ID";


	/**
	 * 返回当前的 HTTP 请求。
	 */
	ServerHttpRequest getRequest();

	/**
	 * 返回当前的 HTTP 响应。
	 */
	ServerHttpResponse getResponse();

	/**
	 * 返回当前交换的可变请求属性 Map。
	 */
	Map<String, Object> getAttributes();

	/**
	 * 如果存在，返回请求属性值。
	 * @param name the attribute name
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T getAttribute(String name) {
		return (T) getAttributes().get(name);
	}

	/**
	 * 返回请求属性值，如果不存在则抛出
	 * {@link IllegalArgumentException}。
	 * @param name the attribute name
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	default <T> T getRequiredAttribute(String name) {
		T value = getAttribute(name);
		Assert.notNull(value, () -> "Required attribute '" + name + "' is missing");
		return value;
	}

	/**
	 * 返回请求属性值，或返回一个默认的后备值。
	 * @param name the attribute name
	 * @param defaultValue a default value to return instead
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	default <T> T getAttributeOrDefault(String name, T defaultValue) {
		return (T) getAttributes().getOrDefault(name, defaultValue);
	}

	/**
	 * 返回当前请求的 Web 会话。始终保证返回一个实例：要么与客户端请求的
	 * 会话 id 相匹配，要么使用新的会话 id（因为客户端未指定会话 id，
	 * 或者底层会话已过期）。使用此方法不会自动创建会话。
	 * 更多详情请参阅 {@link WebSession}。
	 */
	Mono<WebSession> getSession();

	/**
	 * 返回请求的已认证用户（如果有）。
	 */
	<T extends Principal> Mono<T> getPrincipal();

	/**
	 * 如果 Content-Type 为 {@code "application/x-www-form-urlencoded"}，
	 * 则返回请求体中的表单数据，否则返回空 Map。
	 * <p><strong>注意：</strong>调用此方法会导致请求体被完整读取和解析，
	 * 并且生成的 {@code MultiValueMap} 会被缓存，因此可以安全地多次调用此方法。
	 */
	Mono<MultiValueMap<String, String>> getFormData();

	/**
	 * 如果 Content-Type 为 {@code "multipart/form-data"}，
	 * 则返回 multipart 请求的各个部分，否则返回空 Map。
	 * <p><strong>注意：</strong>调用此方法会导致请求体被完整读取和解析，
	 * 并且生成的 {@code MultiValueMap} 会被缓存，因此可以安全地多次调用此方法。
	 * <p><strong>注意：</strong>每个部分的 {@linkplain Part#content() 内容}不会被缓存，
	 * 并且只能读取一次。
	 */
	Mono<MultiValueMap<String, Part>> getMultipartData();

	/**
	 * 使用已配置的 {@link org.springframework.web.server.i18n.LocaleContextResolver}
	 * 返回 {@link LocaleContext}。
	 */
	LocaleContext getLocaleContext();

	/**
	 * 返回与 Web 应用关联的 {@link ApplicationContext}（如果已通过
	 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)}
	 * 使用某个应用上下文进行了初始化）。
	 * @since 5.0.3
	 * @see org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)
	 */
	@Nullable
	ApplicationContext getApplicationContext();

	/**
	 * 如果使用了此契约中的某个 {@code checkNotModified} 方法并且它们返回了 true，
	 * 则返回 {@code true}。
	 */
	boolean isNotModified();

	/**
	 * {@link #checkNotModified(String, Instant)} 的重载变体，仅带有
	 * 最后修改时间戳。
	 * @param lastModified the last-modified time
	 * @return whether the request qualifies as not modified
	 */
	boolean checkNotModified(Instant lastModified);

	/**
	 * {@link #checkNotModified(String, Instant)} 的重载变体，仅带有
	 * {@code ETag}（实体标记）值。
	 * @param etag the entity tag for the underlying resource.
	 * @return true if the request does not require further processing.
	 */
	boolean checkNotModified(String etag);

	/**
	 * 根据应用确定的 {@code ETag}（实体标记）和最后修改时间戳，
	 * 检查所请求的资源是否已被修改。同时还会透明地准备响应：
	 * 设置 HTTP 状态，并在适用时添加 "ETag" 和 "Last-Modified" 头部。
	 * 此方法适用于条件 GET/HEAD 请求，也适用于条件 POST/PUT/DELETE 请求。
	 * <p><strong>注意：</strong>HTTP 规范建议同时设置 ETag 和 Last-Modified 值，
	 * 但你也可以使用 {@code #checkNotModified(String)} 或
	 * {@link #checkNotModified(Instant)}。
	 * @param etag the entity tag that the application determined for the
	 * underlying resource. This parameter will be padded with quotes (")
	 * if necessary.
	 * @param lastModified the last-modified timestamp that the application
	 * determined for the underlying resource
	 * @return true if the request does not require further processing.
	 */
	boolean checkNotModified(@Nullable String etag, Instant lastModified);

	/**
	 * 根据已注册的转换函数转换给定的 url。
	 * 默认情况下，此方法返回给定的 {@code url}，
	 * 不过可以通过 {@link #addUrlTransformer} 注册额外的转换函数。
	 * @param url the URL to transform
	 * @return the transformed URL
	 */
	String transformUrl(String url);

	/**
	 * 注册一个供 {@link #transformUrl} 使用的额外 URL 转换函数。
	 * 给定的函数可用于插入用于认证的 id、用于 CSRF 防护的 nonce 等。
	 * <p>请注意，给定的函数会在所有先前注册的函数之后应用。
	 * @param transformer a URL transformation function to add
	 */
	void addUrlTransformer(Function<String, String> transformer);

	/**
	 * 返回用于关联此交换的消息的日志消息前缀。
	 * 该前缀基于属性 {@link #LOG_ID_ATTRIBUTE} 的值，并附加一些额外的格式，
	 * 以便可以方便地直接前置该前缀，无需进一步格式化或分隔符。
	 * @return the log message prefix or an empty String if the
	 * {@link #LOG_ID_ATTRIBUTE} is not set.
	 * @since 5.1
	 */
	String getLogPrefix();

	/**
	 * 返回一个构建器，用于通过将交换包装为 {@link ServerWebExchangeDecorator}
	 * 来变更其属性，并返回变更后的值或委托回此实例。
	 */
	default Builder mutate() {
		return new DefaultServerWebExchangeBuilder(this);
	}


	/**
	 * 用于变更现有 {@link ServerWebExchange} 的构建器。
	 * 省去了相应的需要。
	 */
	interface Builder {

		/**
		 * 配置一个消费者，使用构建器修改当前请求。
		 * <p>其效果等同于：
		 * <pre>
		 * exchange.mutate().request(builder -&gt; builder.method(HttpMethod.PUT));
		 *
		 * // vs...
		 *
		 * ServerHttpRequest request = exchange.getRequest().mutate()
		 *     .method(HttpMethod.PUT)
		 *     .build();
		 *
		 * exchange.mutate().request(request);
		 * </pre>
		 * @see ServerHttpRequest#mutate()
		 */
		Builder request(Consumer<ServerHttpRequest.Builder> requestBuilderConsumer);

		/**
		 * 设置要使用的请求，特别是在需要覆盖 {@link ServerHttpRequest} 方法时。
		 * 如果只是想变更请求属性，请改用 {@link #request(Consumer)}。
		 * @see org.springframework.http.server.reactive.ServerHttpRequestDecorator
		 */
		Builder request(ServerHttpRequest request);

		/**
		 * 设置要使用的响应。
		 * @see org.springframework.http.server.reactive.ServerHttpResponseDecorator
		 */
		Builder response(ServerHttpResponse response);

		/**
		 * 设置此交换要返回的 {@code Mono<Principal>}。
		 */
		Builder principal(Mono<Principal> principalMono);

		/**
		 * 构建一个带有变更属性的 {@link ServerWebExchange} 装饰器。
		 */
		ServerWebExchange build();
	}

}
