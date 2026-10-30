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

package org.springframework.http.converter;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@code HttpMessageConverter}，使用 {@link StringHttpMessageConverter}
 * 读写内容，并使用 {@link ConversionService} 在 String 内容与目标对象类型
 * 之间进行转换。
 *
 * <p>默认情况下，此转换器仅支持媒体类型 {@code text/plain}。
 * 可以通过 {@link #setSupportedMediaTypes supportedMediaTypes}
 * 属性覆盖。
 *
 * <p>用法示例：
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.ObjectToStringHttpMessageConverter"&gt;
 *   &lt;constructor-arg&gt;
 *     &lt;bean class="org.springframework.context.support.ConversionServiceFactoryBean"/&gt;
 *   &lt;/constructor-arg&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

	private final ConversionService conversionService;

	private final StringHttpMessageConverter stringHttpMessageConverter;


	/**
	 * 接受一个 {@code ConversionService} 的构造函数，用于将
	 * （String）消息体与目标类类型相互转换。此构造函数使用
	 * {@link StringHttpMessageConverter#DEFAULT_CHARSET} 作为默认字符集。
	 * @param conversionService 转换服务
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService) {
		this(conversionService, StringHttpMessageConverter.DEFAULT_CHARSET);
	}

	/**
	 * 同时接受 {@code ConversionService} 和默认字符集的构造函数。
	 * @param conversionService 转换服务
	 * @param defaultCharset 默认字符集
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService, Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN);

		Assert.notNull(conversionService, "ConversionService is required");
		this.conversionService = conversionService;
		this.stringHttpMessageConverter = new StringHttpMessageConverter(defaultCharset);
	}


	/**
	 * 委托给 {@link StringHttpMessageConverter#setWriteAcceptCharset(boolean)}。
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.stringHttpMessageConverter.setWriteAcceptCharset(writeAcceptCharset);
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return canRead(mediaType) && this.conversionService.canConvert(String.class, clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(mediaType) && this.conversionService.canConvert(clazz, String.class);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		String value = this.stringHttpMessageConverter.readInternal(String.class, inputMessage);
		Object result = this.conversionService.convert(value, clazz);
		if (result == null) {
			throw new HttpMessageNotReadableException(
					"Unexpected null conversion result for '" + value + "' to " + clazz,
					inputMessage);
		}
		return result;
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
		String value = this.conversionService.convert(obj, String.class);
		if (value != null) {
			this.stringHttpMessageConverter.writeInternal(value, outputMessage);
		}
	}

	@Override
	protected Long getContentLength(Object obj, @Nullable MediaType contentType) {
		String value = this.conversionService.convert(obj, String.class);
		if (value == null) {
			return 0L;
		}
		return this.stringHttpMessageConverter.getContentLength(value, contentType);
	}

}
