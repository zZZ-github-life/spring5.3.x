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

package org.springframework.http.codec.json;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.codec.HttpMessageDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Jackson 2.9 解码的抽象基类，利用非阻塞解析。
 *
 * <p>兼容 Jackson 2.9.7 及更高版本。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 5.0
 * @see <a href="https://github.com/FasterXML/jackson-core/issues/57" target="_blank">Add support for non-blocking ("async") JSON parsing</a>
 */
public abstract class AbstractJackson2Decoder extends Jackson2CodecSupport implements HttpMessageDecoder<Object> {

	private int maxInMemorySize = 256 * 1024;


	/**
	 * 使用要采用的 Jackson {@link ObjectMapper} 的构造函数。
	 */
	protected AbstractJackson2Decoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	/**
	 * 设置此解码器可缓冲的最大字节数。这可以是整体解码时整个
	 * 输入的大小，也可以是 JSON 流中单个顶层 JSON 对象的大小。
	 * 超过限制时会抛出 {@link DataBufferLimitException}。
	 * <p>默认设置为 256K。
	 * @param byteCount 要缓冲的最大字节数，-1 表示不限
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 返回{@link #setMaxInMemorySize 已配置}的字节数限制。
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
		if (mapper == null) {
			return false;
		}
		JavaType javaType = mapper.constructType(elementType.getType());
		// Skip String: CharSequenceDecoder + "*/*" comes after
		if (CharSequence.class.isAssignableFrom(elementType.toClass()) || !supportsMimeType(mimeType)) {
			return false;
		}
		if (!logger.isDebugEnabled()) {
			return mapper.canDeserialize(javaType);
		}
		else {
			AtomicReference<Throwable> causeRef = new AtomicReference<>();
			if (mapper.canDeserialize(javaType, causeRef)) {
				return true;
			}
			logWarningIfNecessary(javaType, causeRef.get());
			return false;
		}
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
		if (mapper == null) {
			throw new IllegalStateException("No ObjectMapper for " + elementType);
		}

		boolean forceUseOfBigDecimal = mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		if (BigDecimal.class.equals(elementType.getType())) {
			forceUseOfBigDecimal = true;
		}

		Flux<DataBuffer> processed = processInput(input, elementType, mimeType, hints);
		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(processed, mapper.getFactory(), mapper,
				true, forceUseOfBigDecimal, getMaxInMemorySize());

		ObjectReader reader = getObjectReader(mapper, elementType, hints);

		return tokens.handle((tokenBuffer, sink) -> {
			try {
				Object value = reader.readValue(tokenBuffer.asParser(mapper));
				logValue(value, hints);
				if (value != null) {
					sink.next(value);
				}
			}
			catch (IOException ex) {
				sink.error(processException(ex));
			}
		});
	}

	/**
	 * 将输入的发布者处理为 Flux。默认实现返回
	 * {@link Flux#from(Publisher)}，但子类可以选择自定义
	 * 此行为。
	 * @param input 要处理的 {@code DataBuffer} 输入流
	 * @param elementType 输出流中元素的预期类型
	 * @param mimeType 与输入流关联的 MIME 类型（可选）
	 * @param hints 关于如何编码的附加信息
	 * @return 处理后的 Flux
	 * @since 5.1.14
	 */
	protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
				@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(input);
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(input, this.maxInMemorySize)
				.flatMap(dataBuffer -> Mono.justOrEmpty(decode(dataBuffer, elementType, mimeType, hints)));
	}

	@Override
	public Object decode(DataBuffer dataBuffer, ResolvableType targetType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		ObjectMapper mapper = selectObjectMapper(targetType, mimeType);
		if (mapper == null) {
			throw new IllegalStateException("No ObjectMapper for " + targetType);
		}

		try {
			ObjectReader objectReader = getObjectReader(mapper, targetType, hints);
			Object value = objectReader.readValue(dataBuffer.asInputStream());
			logValue(value, hints);
			return value;
		}
		catch (IOException ex) {
			throw processException(ex);
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
	}

	private ObjectReader getObjectReader(
			ObjectMapper mapper, ResolvableType elementType, @Nullable Map<String, Object> hints) {

		Assert.notNull(elementType, "'elementType' must not be null");
		Class<?> contextClass = getContextClass(elementType);
		if (contextClass == null && hints != null) {
			contextClass = getContextClass((ResolvableType) hints.get(ACTUAL_TYPE_HINT));
		}
		JavaType javaType = getJavaType(elementType.getType(), contextClass);
		Class<?> jsonView = (hints != null ? (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT) : null);
		return jsonView != null ?
				mapper.readerWithView(jsonView).forType(javaType) :
				mapper.readerFor(javaType);
	}

	@Nullable
	private Class<?> getContextClass(@Nullable ResolvableType elementType) {
		MethodParameter param = (elementType != null ? getParameter(elementType)  : null);
		return (param != null ? param.getContainingClass() : null);
	}

	private void logValue(@Nullable Object value, @Nullable Map<String, Object> hints) {
		if (!Hints.isLoggingSuppressed(hints)) {
			LogFormatUtils.traceDebug(logger, traceOn -> {
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
			});
		}
	}

	private CodecException processException(IOException ex) {
		if (ex instanceof InvalidDefinitionException) {
			JavaType type = ((InvalidDefinitionException) ex).getType();
			return new CodecException("Type definition error: " + type, ex);
		}
		if (ex instanceof JsonProcessingException) {
			String originalMessage = ((JsonProcessingException) ex).getOriginalMessage();
			return new DecodingException("JSON decoding error: " + originalMessage, ex);
		}
		return new DecodingException("I/O error while parsing input stream", ex);
	}


	// HttpMessageDecoder

	@Override
	public Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response) {

		return getHints(actualType);
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return getMimeTypes();
	}

	@Override
	public List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
		return getMimeTypes(targetType);
	}

	// Jackson2CodecSupport

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getParameterAnnotation(annotType);
	}

}
