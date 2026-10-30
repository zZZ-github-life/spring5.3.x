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

package org.springframework.http.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 大多数 {@link HttpMessageConverter} 实现的抽象基类。
 *
 * <p>此基类增加了对通过 {@link #setSupportedMediaTypes(List) supportedMediaTypes}
 * bean 属性设置受支持 {@code MediaTypes} 的支持。它还增加了在写入输出消息时
 * 对 {@code Content-Type} 和 {@code Content-Length} 的支持。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 3.0
 * @param <T> 所转换的对象类型
 */
public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

	/** 供子类使用的 Logger。 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	private List<MediaType> supportedMediaTypes = Collections.emptyList();

	@Nullable
	private Charset defaultCharset;


	/**
	 * 构造一个不支持任何媒体类型的 {@code AbstractHttpMessageConverter}。
	 * @see #setSupportedMediaTypes
	 */
	protected AbstractHttpMessageConverter() {
	}

	/**
	 * 构造一个支持一种媒体类型的 {@code AbstractHttpMessageConverter}。
	 * @param supportedMediaType 支持的媒体类型
	 */
	protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	/**
	 * 构造一个支持多种媒体类型的 {@code AbstractHttpMessageConverter}。
	 * @param supportedMediaTypes 支持的媒体类型
	 */
	protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}

	/**
	 * 构造一个具有默认字符集并支持多种媒体类型的
	 * {@code AbstractHttpMessageConverter}。
	 * @param defaultCharset 默认字符集
	 * @param supportedMediaTypes 支持的媒体类型
	 * @since 4.3
	 */
	protected AbstractHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
		this.defaultCharset = defaultCharset;
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}


	/**
	 * 设置此转换器支持的 {@link MediaType} 对象列表。
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	/**
	 * 设置默认字符集（如果有）。
	 * @since 4.3
	 */
	public void setDefaultCharset(@Nullable Charset defaultCharset) {
		this.defaultCharset = defaultCharset;
	}

	/**
	 * 返回默认字符集（如果有）。
	 * @since 4.3
	 */
	@Nullable
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}


	/**
	 * 此实现检查给定类是否{@linkplain #supports(Class) 受支持}，
	 * 以及{@linkplain #getSupportedMediaTypes() 受支持的媒体类型}
	 * 是否{@linkplain MediaType#includes(MediaType) 包含}给定媒体类型。
	 */
	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return supports(clazz) && canRead(mediaType);
	}

	/**
	 * 如果{@linkplain #setSupportedMediaTypes(List) 受支持的}媒体类型中
	 * 有任何一个{@link MediaType#includes(MediaType) 包含}给定媒体类型，
	 * 则返回 {@code true}。
	 * @param mediaType 要读取的媒体类型，如果未指定可以为 {@code null}。
	 * 通常是 {@code Content-Type} 头的值。
	 * @return 如果支持的媒体类型包含该媒体类型，
	 * 或该媒体类型为 {@code null}，则返回 {@code true}
	 */
	protected boolean canRead(@Nullable MediaType mediaType) {
		if (mediaType == null) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 此实现检查给定类是否{@linkplain #supports(Class) 受支持}，
	 * 以及{@linkplain #getSupportedMediaTypes() 受支持的}媒体类型
	 * 是否{@linkplain MediaType#includes(MediaType) 包含}给定媒体类型。
	 */
	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return supports(clazz) && canWrite(mediaType);
	}

	/**
	 * 如果给定媒体类型包含{@linkplain #setSupportedMediaTypes(List) 受支持的媒体类型}
	 * 中的任何一个，则返回 {@code true}。
	 * @param mediaType 要写入的媒体类型，如果未指定可以为 {@code null}。
	 * 通常是 {@code Accept} 头的值。
	 * @return 如果支持的媒体类型与该媒体类型兼容，
	 * 或该媒体类型为 {@code null}，则返回 {@code true}
	 */
	protected boolean canWrite(@Nullable MediaType mediaType) {
		if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 此实现简单地委托给 {@link #readInternal(Class, HttpInputMessage)}。
	 * 不过，未来的实现可能会添加一些默认行为。
	 */
	@Override
	public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readInternal(clazz, inputMessage);
	}

	/**
	 * 此实现通过调用 {@link #addDefaultHeaders} 设置默认头，
	 * 然后调用 {@link #writeInternal}。
	 */
	@Override
	public final void write(final T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		final HttpHeaders headers = outputMessage.getHeaders();
		addDefaultHeaders(headers, t, contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(outputStream -> writeInternal(t, new HttpOutputMessage() {
				@Override
				public OutputStream getBody() {
					return outputStream;
				}
				@Override
				public HttpHeaders getHeaders() {
					return headers;
				}
			}));
		}
		else {
			writeInternal(t, outputMessage);
			outputMessage.getBody().flush();
		}
	}

	/**
	 * 向输出消息添加默认头。
	 * <p>此实现会在未提供内容类型时委托给 {@link #getDefaultContentType(Object)}，
	 * 在必要时设置默认字符集，调用 {@link #getContentLength}，
	 * 并设置相应的头。
	 * @since 4.2
	 */
	protected void addDefaultHeaders(HttpHeaders headers, T t, @Nullable MediaType contentType) throws IOException {
		if (headers.getContentType() == null) {
			MediaType contentTypeToUse = contentType;
			if (contentType == null || !contentType.isConcrete()) {
				contentTypeToUse = getDefaultContentType(t);
			}
			else if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
				MediaType mediaType = getDefaultContentType(t);
				contentTypeToUse = (mediaType != null ? mediaType : contentTypeToUse);
			}
			if (contentTypeToUse != null) {
				if (contentTypeToUse.getCharset() == null) {
					Charset defaultCharset = getDefaultCharset();
					if (defaultCharset != null) {
						contentTypeToUse = new MediaType(contentTypeToUse, defaultCharset);
					}
				}
				headers.setContentType(contentTypeToUse);
			}
		}
		if (headers.getContentLength() < 0 && !headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
			Long contentLength = getContentLength(t, headers.getContentType());
			if (contentLength != null) {
				headers.setContentLength(contentLength);
			}
		}
	}

	/**
	 * 返回给定类型的默认内容类型。当调用 {@link #write}
	 * 而未指定内容类型参数时调用。
	 * <p>默认情况下，如果存在，返回
	 * {@link #setSupportedMediaTypes(List) supportedMediaTypes} 属性的第一个元素。
	 * 可在子类中重写。
	 * @param t 要返回其内容类型的类型
	 * @return 内容类型，如果未知则为 {@code null}
	 */
	@Nullable
	protected MediaType getDefaultContentType(T t) throws IOException {
		List<MediaType> mediaTypes = getSupportedMediaTypes();
		return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
	}

	/**
	 * 返回给定类型的内容长度。
	 * <p>默认返回 {@code null}，表示内容长度未知。
	 * 可在子类中重写。
	 * @param t 要返回其内容长度的类型
	 * @return 内容长度，如果未知则为 {@code null}
	 */
	@Nullable
	protected Long getContentLength(T t, @Nullable MediaType contentType) throws IOException {
		return null;
	}


	/**
	 * 指示给定类是否受此转换器支持。
	 * @param clazz 要测试是否支持的类
	 * @return 如果支持则返回 {@code true}；否则返回 {@code false}
	 */
	protected abstract boolean supports(Class<?> clazz);

	/**
	 * 读取实际对象的抽象模板方法。由 {@link #read} 调用。
	 * @param clazz 要返回的对象类型
	 * @param inputMessage 要从中读取的 HTTP 输入消息
	 * @return 转换后的对象
	 * @throws IOException 如果发生 I/O 错误
	 * @throws HttpMessageNotReadableException 如果发生转换错误
	 */
	protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException;

	/**
	 * 写入实际正文的抽象模板方法。由 {@link #write} 调用。
	 * @param t 要写入输出消息的对象
	 * @param outputMessage 要写入到的 HTTP 输出消息
	 * @throws IOException 如果发生 I/O 错误
	 * @throws HttpMessageNotWritableException 如果发生转换错误
	 */
	protected abstract void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException;

}
