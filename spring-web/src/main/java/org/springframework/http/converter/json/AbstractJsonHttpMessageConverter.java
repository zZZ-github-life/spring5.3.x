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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;

/**
 * 纯 JSON 转换器（例如 Gson 和 JSON-B）的公共基类。
 *
 * <p>请注意，由于 Jackson 转换器支持多种格式，
 * 它们具有专门的类层次结构。
 *
 * @author Juergen Hoeller
 * @since 5.0
 * @see GsonHttpMessageConverter
 * @see JsonbHttpMessageConverter
 * @see #readInternal(Type, Reader)
 * @see #writeInternal(Object, Type, Writer)
 */
public abstract class AbstractJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	@Nullable
	private String jsonPrefix;


	public AbstractJsonHttpMessageConverter() {
		super(MediaType.APPLICATION_JSON, new MediaType("application", "*+json"));
		setDefaultCharset(DEFAULT_CHARSET);
	}


	/**
	 * 指定用于 JSON 输出的自定义前缀。默认为无。
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * 指示此视图输出的 JSON 是否应以 ")]}', " 为前缀。
	 * 默认为 {@code false}。
	 * <p>以这种方式为 JSON 字符串添加前缀有助于防止
	 * JSON 劫持。该前缀使字符串在语法上无法作为脚本，
	 * 从而无法被劫持。
	 * 在将字符串解析为 JSON 之前应去掉该前缀。
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? ")]}', " : null);
	}


	@Override
	public final Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readResolved(GenericTypeResolver.resolveType(type, contextClass), inputMessage);
	}

	@Override
	protected final Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		return readResolved(clazz, inputMessage);
	}

	private Object readResolved(Type resolvedType, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		Reader reader = getReader(inputMessage);
		try {
			return readInternal(resolvedType, reader);
		}
		catch (Exception ex) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex, inputMessage);
		}
	}

	@Override
	protected final void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		Writer writer = getWriter(outputMessage);
		if (this.jsonPrefix != null) {
			writer.append(this.jsonPrefix);
		}
		try {
			writeInternal(object, type, writer);
		}
		catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
		writer.flush();
	}


	/**
	 * 模板方法，从给定的 {@link Reader} 读取绑定到 JSON 的对象。
	 * @param resolvedType 解析后的泛型类型
	 * @param reader 要使用的 {@code} Reader
	 * @return 与 JSON 绑定的对象
	 * @throws Exception 发生读取/解析失败时抛出
	 */
	protected abstract Object readInternal(Type resolvedType, Reader reader) throws Exception;

	/**
	 * 模板方法，将绑定到 JSON 的对象写入给定的 {@link Writer}。
	 * @param object 要写入输出消息的对象
	 * @param type 要写入的对象的类型（可以为 {@code null}）
	 * @param writer 要使用的 {@code} Writer
	 * @throws Exception 发生写入失败时抛出
	 */
	protected abstract void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception;


	private static Reader getReader(HttpInputMessage inputMessage) throws IOException {
		return new InputStreamReader(inputMessage.getBody(), getCharset(inputMessage.getHeaders()));
	}

	private static Writer getWriter(HttpOutputMessage outputMessage) throws IOException {
		return new OutputStreamWriter(outputMessage.getBody(), getCharset(outputMessage.getHeaders()));
	}

	private static Charset getCharset(HttpHeaders headers) {
		Charset charset = (headers.getContentType() != null ? headers.getContentType().getCharset() : null);
		return (charset != null ? charset : DEFAULT_CHARSET);
	}

}
