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

package org.springframework.messaging.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.TypeMismatchException;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * 使用 Spring 的 {@link Marshaller} 和 {@link Unmarshaller} 抽象读写 XML 的
 * {@link MessageConverter} 实现。
 *
 * <p>此转换器在使用前需要 {@code Marshaller} 和 {@code Unmarshaller}。
 * 可以通过 {@linkplain #MarshallingMessageConverter(Marshaller) 构造函数}
 * 或 {@linkplain #setMarshaller(Marshaller) Bean 属性}注入它们。
 *
 * @author Arjen Poutsma
 * @since 4.2
 * @see Marshaller
 * @see Unmarshaller
 */
public class MarshallingMessageConverter extends AbstractMessageConverter {

	@Nullable
	private Marshaller marshaller;

	@Nullable
	private Unmarshaller unmarshaller;


	/**
	 * 默认构造函数，允许分别调用 {@link #setMarshaller(Marshaller)} 和/或
	 * {@link #setUnmarshaller(Unmarshaller)}。
	 */
	public MarshallingMessageConverter() {
		this(new MimeType("application", "xml"), new MimeType("text", "xml"),
				new MimeType("application", "*+xml"));
	}

	/**
	 * 使用给定的受支持 MIME 类型列表的构造函数。
	 * @param supportedMimeTypes the MIME types
	 */
	public MarshallingMessageConverter(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}

	/**
	 * 使用 {@link Marshaller} 的构造函数。如果给定的 {@link Marshaller} 同时
	 * 实现了 {@link Unmarshaller}，则它也会用于解组。
	 * <p>注意，Spring 中的所有 {@code Marshaller} 实现也都实现了
	 * {@code Unmarshaller}，因此可以安全地使用此构造函数。
	 * @param marshaller object used as marshaller and unmarshaller
	 */
	public MarshallingMessageConverter(Marshaller marshaller) {
		this();
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
		if (marshaller instanceof Unmarshaller) {
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}


	/**
	 * 设置此消息转换器要使用的 {@link Marshaller}。
	 */
	public void setMarshaller(@Nullable Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 返回已配置的 Marshaller。
	 */
	@Nullable
	public Marshaller getMarshaller() {
		return this.marshaller;
	}

	/**
	 * 设置此消息转换器要使用的 {@link Unmarshaller}。
	 */
	public void setUnmarshaller(@Nullable Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/**
	 * 返回已配置的 Unmarshaller。
	 */
	@Nullable
	public Unmarshaller getUnmarshaller() {
		return this.unmarshaller;
	}


	@Override
	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return (supportsMimeType(message.getHeaders()) && this.unmarshaller != null &&
				this.unmarshaller.supports(targetClass));
	}

	@Override
	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		return (supportsMimeType(headers) && this.marshaller != null &&
				this.marshaller.supports(payload.getClass()));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canConvertFrom/canConvertTo instead
		throw new UnsupportedOperationException();
	}

	@Override
	@Nullable
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
		try {
			Source source = getSource(message.getPayload());
			Object result = this.unmarshaller.unmarshal(source);
			if (!targetClass.isInstance(result)) {
				throw new TypeMismatchException(result, targetClass);
			}
			return result;
		}
		catch (Exception ex) {
			throw new MessageConversionException(message, "Could not unmarshal XML: " + ex.getMessage(), ex);
		}
	}

	private Source getSource(Object payload) {
		if (payload instanceof byte[]) {
			return new StreamSource(new ByteArrayInputStream((byte[]) payload));
		}
		else {
			return new StreamSource(new StringReader(payload.toString()));
		}
	}

	@Override
	@Nullable
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers,
			@Nullable Object conversionHint) {

		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		try {
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
				Result result = new StreamResult(out);
				this.marshaller.marshal(payload, result);
				payload = out.toByteArray();
			}
			else {
				Writer writer = new StringWriter(1024);
				Result result = new StreamResult(writer);
				this.marshaller.marshal(payload, result);
				payload = writer.toString();
			}
		}
		catch (Throwable ex) {
			throw new MessageConversionException("Could not marshal XML: " + ex.getMessage(), ex);
		}
		return payload;
	}

}
