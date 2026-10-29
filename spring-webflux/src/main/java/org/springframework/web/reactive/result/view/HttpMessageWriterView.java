/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@code View}，使用 {@link HttpMessageWriter} 写入模型属性。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpMessageWriterView implements View {

	private final HttpMessageWriter<?> writer;

	private final Set<String> modelKeys = new HashSet<>(4);

	private final boolean canWriteMap;


	/**
	 * 使用 {@code Encoder} 的构造函数。
	 */
	public HttpMessageWriterView(Encoder<?> encoder) {
		this(new EncoderHttpMessageWriter<>(encoder));
	}

	/**
	 * 使用完全初始化的 {@link HttpMessageWriter} 的构造函数。
	 */
	public HttpMessageWriterView(HttpMessageWriter<?> writer) {
		Assert.notNull(writer, "HttpMessageWriter is required");
		this.writer = writer;
		this.canWriteMap = writer.canWrite(ResolvableType.forClass(Map.class), null);
	}


	/**
	 * 返回配置的消息写入器。
	 */
	public HttpMessageWriter<?> getMessageWriter() {
		return this.writer;
	}

	/**
	 * {@inheritDoc}
	 * <p>此方法针对 {@link HttpMessageWriterView} 的实现委托给 {@link HttpMessageWriter#getWritableMediaTypes()}。
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.writer.getWritableMediaTypes();
	}

	/**
	 * 设置应由此视图渲染的模型属性。设置后，所有其他模型属性将被忽略。匹配的属性会通过 {@link HttpMessageWriter#canWrite} 进一步缩小范围。匹配属性的处理方式如下：
	 * <ul>
	 * <li>0：不向响应正文写入任何内容。
	 * <li>1：将匹配的属性传递给写入器。
	 * <li>2..N：如果写入器支持 {@link Map}，则写入所有匹配项；否则抛出 {@link IllegalStateException}。
	 * </ul>
	 */
	public void setModelKeys(@Nullable Set<String> modelKeys) {
		this.modelKeys.clear();
		if (modelKeys != null) {
			this.modelKeys.addAll(modelKeys);
		}
	}

	/**
	 * 返回配置的模型键。
	 */
	public final Set<String> getModelKeys() {
		return this.modelKeys;
	}


	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> render(
			@Nullable Map<String, ?> model, @Nullable MediaType contentType, ServerWebExchange exchange) {

		Object value = getObjectToRender(model);
		return (value != null ? write(value, contentType, exchange) : exchange.getResponse().setComplete());
	}

	@Nullable
	private Object getObjectToRender(@Nullable Map<String, ?> model) {
		if (model == null) {
			return null;
		}

		Map<String, ?> result = model.entrySet().stream()
				.filter(this::isMatch)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		if (result.isEmpty()) {
			return null;
		}
		else if (result.size() == 1) {
			return result.values().iterator().next();
		}
		else if (this.canWriteMap) {
			return result;
		}
		else {
			throw new IllegalStateException("Multiple matches found: " + result + " but " +
					"Map rendering is not supported by " + getMessageWriter().getClass().getName());
		}
	}

	private boolean isMatch(Map.Entry<String, ?> entry) {
		if (entry.getValue() == null) {
			return false;
		}
		if (!getModelKeys().isEmpty() && !getModelKeys().contains(entry.getKey())) {
			return false;
		}
		ResolvableType type = ResolvableType.forInstance(entry.getValue());
		return getMessageWriter().canWrite(type, null);
	}

	@SuppressWarnings("unchecked")
	private <T> Mono<Void> write(T value, @Nullable MediaType contentType, ServerWebExchange exchange) {
		Publisher<T> input = Mono.justOrEmpty(value);
		ResolvableType elementType = ResolvableType.forClass(value.getClass());
		return ((HttpMessageWriter<T>) this.writer).write(
				input, elementType, contentType, exchange.getResponse(),
				Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix()));
	}

}
