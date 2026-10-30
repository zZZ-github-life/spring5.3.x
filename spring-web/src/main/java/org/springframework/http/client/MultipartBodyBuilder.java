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

package org.springframework.http.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 准备 multipart 请求的正文，结果为
 * {@code MultiValueMap<String, HttpEntity>}。部件可以是具体值，也可以
 * 通过异步类型（例如 Reactor 的 {@code Mono}、{@code Flux} 以及注册在
 * {@link org.springframework.core.ReactiveAdapterRegistry ReactiveAdapterRegistry} 中的其他类型）提供。
 *
 * <p>此构建器旨在与响应式
 * {@link org.springframework.web.reactive.function.client.WebClient WebClient} 一起使用。
 * 对于使用 {@code RestTemplate} 的 multipart 请求，只需创建并填充
 * 一个 {@code MultiValueMap<String, HttpEntity>}，如
 * {@link org.springframework.http.converter.FormHttpMessageConverter FormHttpMessageConverter}
 * 的 Javadoc 和
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#rest-template-multipart">参考文档</a>中所示。
 *
 * <p>以下是使用此构建器的示例：
 * <pre class="code">
 *
 * // Add form field
 * MultipartBodyBuilder builder = new MultipartBodyBuilder();
 * builder.part("form field", "form value").header("foo", "bar");
 *
 * // Add file part
 * Resource image = new ClassPathResource("image.jpg");
 * builder.part("image", image).header("foo", "bar");
 *
 * // Add content (e.g. JSON)
 * Account account = ...
 * builder.part("account", account).header("foo", "bar");
 *
 * // Add content from Publisher
 * Mono&lt;Account&gt; accountMono = ...
 * builder.asyncPart("account", accountMono).header("foo", "bar");
 *
 * // Build and use
 * MultiValueMap&lt;String, HttpEntity&lt;?&gt;&gt; multipartBody = builder.build();
 *
 * Mono&lt;Void&gt; result = webClient.post()
 *     .uri("...")
 *     .body(multipartBody)
 *     .retrieve()
 *     .bodyToMono(Void.class)
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0.2
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>
 */
public final class MultipartBodyBuilder {

	private final LinkedMultiValueMap<String, DefaultPartBuilder> parts = new LinkedMultiValueMap<>();


	/**
	 * 创建 {@code MultipartBodyBuilder} 的新空实例。
	 */
	public MultipartBodyBuilder() {
	}


	/**
	 * 添加一个部件，其中对象可以是：
	 * <ul>
	 * <li>String -- 表单字段
	 * <li>{@link org.springframework.core.io.Resource Resource} -- 文件部件
	 * <li>Object -- 要编码的内容（例如编码为 JSON）
	 * <li>{@link HttpEntity} -- 部件内容和头部，不过通常
	 * 通过返回的构建器添加头部更容易
	 * <li>{@link Part} -- 服务器请求中的部件
	 * </ul>
	 * @param name 要添加的部分的名称
	 * @param part 部分数据
	 * @return 允许进一步自定义部分头的构建器
	 */
	public PartBuilder part(String name, Object part) {
		return part(name, part, null);
	}

	/**
	 * 也接受 MediaType 的 {@link #part(String, Object)} 变体。
	 * @param name 要添加的部分的名称
	 * @param part 部分数据
	 * @param contentType 用于辅助编码该部分的媒体类型
	 * @return 允许进一步自定义部分头的构建器
	 */
	public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(part, "'part' must not be null");

		if (part instanceof Part) {
			Part partObject = (Part) part;
			PartBuilder builder = asyncPart(name, partObject.content(), DataBuffer.class);
			if (!partObject.headers().isEmpty()) {
				builder.headers(headers -> {
					headers.putAll(partObject.headers());
					String filename = headers.getContentDisposition().getFilename();
					// reset to parameter name
					headers.setContentDispositionFormData(name, filename);
				});
			}
			if (contentType != null) {
				builder.contentType(contentType);
			}
			return builder;
		}

		if (part instanceof PublisherEntity<?,?>) {
			PublisherPartBuilder<?, ?> builder = new PublisherPartBuilder<>(name, (PublisherEntity<?, ?>) part);
			if (contentType != null) {
				builder.contentType(contentType);
			}
			this.parts.add(name, builder);
			return builder;
		}

		Object partBody;
		HttpHeaders partHeaders = null;
		if (part instanceof HttpEntity) {
			partBody = ((HttpEntity<?>) part).getBody();
			partHeaders = new HttpHeaders();
			partHeaders.putAll(((HttpEntity<?>) part).getHeaders());
		}
		else {
			partBody = part;
		}

		if (partBody instanceof Publisher) {
			throw new IllegalArgumentException(
					"Use asyncPart(String, Publisher, Class)" +
							" or asyncPart(String, Publisher, ParameterizedTypeReference) or" +
							" or MultipartBodyBuilder.PublisherEntity");
		}

		DefaultPartBuilder builder = new DefaultPartBuilder(name, partHeaders, partBody);
		if (contentType != null) {
			builder.contentType(contentType);
		}
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * 从 {@link Publisher} 内容添加部件。
	 * @param name 要添加的部分的名称
	 * @param publisher 该部分内容的 Publisher
	 * @param elementClass Publisher 中包含的元素类型
	 * @return 允许进一步自定义部分头的构建器
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher, Class<T> elementClass) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");

		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, elementClass);
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * 使用 {@link ParameterizedTypeReference} 提供元素类型信息的
	 * {@link #asyncPart(String, Publisher, Class)} 变体。
	 * @param name 要添加的部分的名称
	 * @param publisher 部分内容
	 * @param typeReference Publisher 中包含的元素类型
	 * @return 允许进一步自定义部分头的构建器
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(
			String name, P publisher, ParameterizedTypeReference<T> typeReference) {

		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(typeReference, "'typeReference' must not be null");

		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, typeReference);
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * 返回包含已配置部件的 {@code MultiValueMap}。
	 */
	public MultiValueMap<String, HttpEntity<?>> build() {
		MultiValueMap<String, HttpEntity<?>> result = new LinkedMultiValueMap<>(this.parts.size());
		for (Map.Entry<String, List<DefaultPartBuilder>> entry : this.parts.entrySet()) {
			for (DefaultPartBuilder builder : entry.getValue()) {
				HttpEntity<?> entity = builder.build();
				result.add(entry.getKey(), entity);
			}
		}
		return result;
	}


	/**
	 * 允许进一步自定义部件头部的构建器。
	 */
	public interface PartBuilder {

		/**
		 * 设置部件的 {@linkplain MediaType 媒体类型}。
		 * @param contentType 内容类型
		 * @since 5.2
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		PartBuilder contentType(MediaType contentType);

		/**
		 * 设置文件部件的 filename 参数。对于公开文件名的
		 * 基于 {@link org.springframework.core.io.Resource Resource} 的部件，这应该不是必需的，
		 * 但对于 {@link Publisher} 部件可能有用。
		 * @param filename 要在 Content-Disposition 上设置的文件名
		 * @since 5.2
		 */
		PartBuilder filename(String filename);

		/**
		 * 添加部件头部值。
		 * @param headerName 部分头名称
		 * @param headerValues 部分头的值
		 * @return 此构建器
		 * @see HttpHeaders#addAll(String, List)
		 */
		PartBuilder header(String headerName, String... headerValues);

		/**
		 * 通过给定的 consumer 操作部件头部。
		 * @param headersConsumer 用于操作部分头的 consumer
		 * @return 此构建器
		 */
		PartBuilder headers(Consumer<HttpHeaders> headersConsumer);
	}


	private static class DefaultPartBuilder implements PartBuilder {

		private final String name;

		@Nullable
		protected HttpHeaders headers;

		@Nullable
		protected final Object body;

		public DefaultPartBuilder(String name, @Nullable HttpHeaders headers, @Nullable Object body) {
			this.name = name;
			this.headers = headers;
			this.body = body;
		}

		@Override
		public PartBuilder contentType(MediaType contentType) {
			initHeadersIfNecessary().setContentType(contentType);
			return this;
		}

		@Override
		public PartBuilder filename(String filename) {
			initHeadersIfNecessary().setContentDispositionFormData(this.name, filename);
			return this;
		}

		@Override
		public PartBuilder header(String headerName, String... headerValues) {
			initHeadersIfNecessary().addAll(headerName, Arrays.asList(headerValues));
			return this;
		}

		@Override
		public PartBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(initHeadersIfNecessary());
			return this;
		}

		private HttpHeaders initHeadersIfNecessary() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
			}
			return this.headers;
		}

		public HttpEntity<?> build() {
			return new HttpEntity<>(this.body, this.headers);
		}
	}


	private static class PublisherPartBuilder<S, P extends Publisher<S>> extends DefaultPartBuilder {

		private final ResolvableType resolvableType;

		public PublisherPartBuilder(String name, @Nullable HttpHeaders headers, P body, Class<S> elementClass) {
			super(name, headers, body);
			this.resolvableType = ResolvableType.forClass(elementClass);
		}

		public PublisherPartBuilder(String name, @Nullable HttpHeaders headers, P body,
				ParameterizedTypeReference<S> typeRef) {

			super(name, headers, body);
			this.resolvableType = ResolvableType.forType(typeRef);
		}

		public PublisherPartBuilder(String name, PublisherEntity<S, P> other) {
			super(name, other.getHeaders(), other.getBody());
			this.resolvableType = other.getResolvableType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public HttpEntity<?> build() {
			P publisher = (P) this.body;
			Assert.state(publisher != null, "Publisher must not be null");
			return new PublisherEntity<>(this.headers, publisher, this.resolvableType);
		}
	}


	/**
	 * 用于基于 {@link Publisher} 的正文的 {@link HttpEntity} 特化，
	 * 对此我们还需要跟踪元素类型。
	 * @param <T> Publisher 中包含的类型
	 * @param <P> 该 Publisher
	 */
	static final class PublisherEntity<T, P extends Publisher<T>> extends HttpEntity<P>
			implements ResolvableTypeProvider  {

		private final ResolvableType resolvableType;

		PublisherEntity(
				@Nullable MultiValueMap<String, String> headers, P publisher, ResolvableType resolvableType) {

			super(publisher, headers);
			Assert.notNull(publisher, "'publisher' must not be null");
			Assert.notNull(resolvableType, "'resolvableType' must not be null");
			this.resolvableType = resolvableType;
		}

		/**
		 * 返回 {@code Publisher} 正文的元素类型。
		 */
		@Override
		@NonNull
		public ResolvableType getResolvableType() {
			return this.resolvableType;
		}
	}

}
