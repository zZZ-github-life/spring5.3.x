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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * 可读写字符串的 {@link HttpMessageConverter} 实现。
 *
 * <p>默认情况下，此转换器支持所有媒体类型（<code>&#42;/&#42;</code>），
 * 并使用 {@code text/plain} 作为 {@code Content-Type} 进行写入。
 * 可以通过设置 {@link #setSupportedMediaTypes supportedMediaTypes} 属性来覆盖。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {

	private static final MediaType APPLICATION_PLUS_JSON = new MediaType("application", "*+json");

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;


	@Nullable
	private volatile List<Charset> availableCharsets;

	private boolean writeAcceptCharset = false;


	/**
	 * 使用 {@code "ISO-8859-1"} 作为默认字符集的默认构造函数。
	 * @see #StringHttpMessageConverter(Charset)
	 */
	public StringHttpMessageConverter() {
		this(DEFAULT_CHARSET);
	}

	/**
	 * 接受默认字符集的构造函数，在请求的内容类型未指定字符集时使用。
	 */
	public StringHttpMessageConverter(Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN, MediaType.ALL);
	}


	/**
	 * 是否应将 {@code Accept-Charset} 头写入任何出站请求，
	 * 其值来源于 {@link Charset#availableCharsets()}。
	 * 如果该头已设置，则此行为会被抑制。
	 * <p>从 5.2 起，默认设置为 {@code false}。
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.writeAcceptCharset = writeAcceptCharset;
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return String.class == clazz;
	}

	@Override
	protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
		Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
		return StreamUtils.copyToString(inputMessage.getBody(), charset);
	}

	@Override
	protected Long getContentLength(String str, @Nullable MediaType contentType) {
		Charset charset = getContentTypeCharset(contentType);
		return (long) str.getBytes(charset).length;
	}


	@Override
	protected void addDefaultHeaders(HttpHeaders headers, String s, @Nullable MediaType type) throws IOException {
		if (headers.getContentType() == null ) {
			if (type != null && type.isConcrete() &&
					(type.isCompatibleWith(MediaType.APPLICATION_JSON) ||
					type.isCompatibleWith(APPLICATION_PLUS_JSON))) {
				// Prevent charset parameter for JSON..
				headers.setContentType(type);
			}
		}
		super.addDefaultHeaders(headers, s, type);
	}

	@Override
	protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
		HttpHeaders headers = outputMessage.getHeaders();
		if (this.writeAcceptCharset && headers.get(HttpHeaders.ACCEPT_CHARSET) == null) {
			headers.setAcceptCharset(getAcceptedCharsets());
		}
		Charset charset = getContentTypeCharset(headers.getContentType());
		StreamUtils.copy(str, charset, outputMessage.getBody());
	}


	/**
	 * 返回受支持的 {@link Charset Charset} 列表。
	 * <p>默认返回 {@link Charset#availableCharsets()}。
	 * 可在子类中重写。
	 * @return 可接受的字符集列表
	 */
	protected List<Charset> getAcceptedCharsets() {
		List<Charset> charsets = this.availableCharsets;
		if (charsets == null) {
			charsets = new ArrayList<>(Charset.availableCharsets().values());
			this.availableCharsets = charsets;
		}
		return charsets;
	}

	private Charset getContentTypeCharset(@Nullable MediaType contentType) {
		if (contentType != null) {
			Charset charset = contentType.getCharset();
			if (charset != null) {
				return charset;
			}
			else if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON) ||
					contentType.isCompatibleWith(APPLICATION_PLUS_JSON)) {
				// Matching to AbstractJackson2HttpMessageConverter#DEFAULT_CHARSET
				return StandardCharsets.UTF_8;
			}
		}
		Charset charset = getDefaultCharset();
		Assert.state(charset != null, "No default charset");
		return charset;
	}

}
