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

package org.springframework.messaging.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;

/**
 * 基于 Jackson 2 的 {@link MessageConverter} 实现。
 *
 * <p>它使用以下属性定制 Jackson 的默认属性：
 * <ul>
 * <li>禁用 {@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁用 {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
 *
 * <p>截至 Spring 5.3，兼容 Jackson 2.9 至 2.12。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class MappingJackson2MessageConverter extends AbstractMessageConverter {

	private ObjectMapper objectMapper;

	@Nullable
	private Boolean prettyPrint;


	/**
	 * 构造一个支持 {@code application/json} MIME 类型并使用
	 * {@code UTF-8} 字符集的 {@code MappingJackson2MessageConverter}。
	 */
	public MappingJackson2MessageConverter() {
		super(new MimeType("application", "json"));
		this.objectMapper = initObjectMapper();
	}

	/**
	 * 构造一个支持一个或多个自定义 MIME 类型的
	 * {@code MappingJackson2MessageConverter}。
	 * @param supportedMimeTypes the supported MIME types
	 * @since 4.1.5
	 */
	public MappingJackson2MessageConverter(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
		this.objectMapper = initObjectMapper();
	}


	@SuppressWarnings("deprecation")  // on Jackson 2.13: configure(MapperFeature, boolean)
	private ObjectMapper initObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		return objectMapper;
	}

	/**
	 * 为此转换器设置 {@code ObjectMapper}。
	 * 如果未设置，将使用默认的 {@link ObjectMapper#ObjectMapper() ObjectMapper}。
	 * <p>设置自定义配置的 {@code ObjectMapper} 是进一步控制 JSON 序列化过程的一种方式。
	 * 例如，可以配置扩展的 {@link com.fasterxml.jackson.databind.ser.SerializerFactory}，
	 * 为特定类型提供自定义序列化器。优化序列化过程的另一种选择是在要序列化的类型上
	 * 使用 Jackson 提供的注解，这种情况下无需自定义配置的 ObjectMapper。
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
		configurePrettyPrint();
	}

	/**
	 * 返回此转换器底层使用的 {@code ObjectMapper}。
	 */
	public ObjectMapper getObjectMapper() {
		return this.objectMapper;
	}

	/**
	 * 写入 JSON 时是否使用 {@link DefaultPrettyPrinter}。
	 * 这是按如下方式配置 {@code ObjectMapper} 的快捷方式：
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * converter.setObjectMapper(mapper);
	 * </pre>
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}


	@Override
	protected boolean canConvertFrom(Message<?> message, @Nullable Class<?> targetClass) {
		if (targetClass == null || !supportsMimeType(message.getHeaders())) {
			return false;
		}
		JavaType javaType = this.objectMapper.constructType(targetClass);
		AtomicReference<Throwable> causeRef = new AtomicReference<>();
		if (this.objectMapper.canDeserialize(javaType, causeRef)) {
			return true;
		}
		logWarningIfNecessary(javaType, causeRef.get());
		return false;
	}

	@Override
	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		if (!supportsMimeType(headers)) {
			return false;
		}
		AtomicReference<Throwable> causeRef = new AtomicReference<>();
		if (this.objectMapper.canSerialize(payload.getClass(), causeRef)) {
			return true;
		}
		logWarningIfNecessary(payload.getClass(), causeRef.get());
		return false;
	}

	/**
	 * 确定是否记录来自 {@link ObjectMapper#canDeserialize} /
	 * {@link ObjectMapper#canSerialize} 检查的给定异常。
	 * @param type the class that Jackson tested for (de-)serializability
	 * @param cause the Jackson-thrown exception to evaluate
	 * (typically a {@link JsonMappingException})
	 * @since 4.3
	 */
	protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
		if (cause == null) {
			return;
		}

		// Do not log warning for serializer not found (note: different message wording on Jackson 2.9)
		boolean debugLevel = (cause instanceof JsonMappingException && cause.getMessage().startsWith("Cannot find"));

		if (debugLevel ? logger.isDebugEnabled() : logger.isWarnEnabled()) {
			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
					"serialization for type [" + type + "]";
			if (debugLevel) {
				logger.debug(msg, cause);
			}
			else if (logger.isDebugEnabled()) {
				logger.warn(msg, cause);
			}
			else {
				logger.warn(msg + ": " + cause);
			}
		}
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canConvertFrom/canConvertTo instead
		throw new UnsupportedOperationException();
	}

	@Override
	@Nullable
	protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		JavaType javaType = this.objectMapper.constructType(getResolvedType(targetClass, conversionHint));
		Object payload = message.getPayload();
		Class<?> view = getSerializationView(conversionHint);
		try {
			if (ClassUtils.isAssignableValue(targetClass, payload)) {
				return payload;
			}
			else if (payload instanceof byte[]) {
				if (view != null) {
					return this.objectMapper.readerWithView(view).forType(javaType).readValue((byte[]) payload);
				}
				else {
					return this.objectMapper.readValue((byte[]) payload, javaType);
				}
			}
			else {
				// Assuming a text-based source payload
				if (view != null) {
					return this.objectMapper.readerWithView(view).forType(javaType).readValue(payload.toString());
				}
				else {
					return this.objectMapper.readValue(payload.toString(), javaType);
				}
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	@Nullable
	protected Object convertToInternal(Object payload, @Nullable MessageHeaders headers,
			@Nullable Object conversionHint) {

		try {
			Class<?> view = getSerializationView(conversionHint);
			if (byte[].class == getSerializedPayloadClass()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
				JsonEncoding encoding = getJsonEncoding(getMimeType(headers));
				try (JsonGenerator generator = this.objectMapper.getFactory().createGenerator(out, encoding)) {
					if (view != null) {
						this.objectMapper.writerWithView(view).writeValue(generator, payload);
					}
					else {
						this.objectMapper.writeValue(generator, payload);
					}
					payload = out.toByteArray();
				}
			}
			else {
				// Assuming a text-based target payload
				Writer writer = new StringWriter(1024);
				if (view != null) {
					this.objectMapper.writerWithView(view).writeValue(writer, payload);
				}
				else {
					this.objectMapper.writeValue(writer, payload);
				}
				payload = writer.toString();
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
		}
		return payload;
	}

	/**
	 * 根据给定的转换提示确定 Jackson 序列化视图。
	 * @param conversionHint the conversion hint Object as passed into the
	 * converter for the current conversion attempt
	 * @return the serialization view class, or {@code null} if none
	 * @since 4.2
	 */
	@Nullable
	protected Class<?> getSerializationView(@Nullable Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter param = (MethodParameter) conversionHint;
			JsonView annotation = (param.getParameterIndex() >= 0 ?
					param.getParameterAnnotation(JsonView.class) : param.getMethodAnnotation(JsonView.class));
			if (annotation != null) {
				return extractViewClass(annotation, conversionHint);
			}
		}
		else if (conversionHint instanceof JsonView) {
			return extractViewClass((JsonView) conversionHint, conversionHint);
		}
		else if (conversionHint instanceof Class) {
			return (Class<?>) conversionHint;
		}

		// No JSON view specified...
		return null;
	}

	private Class<?> extractViewClass(JsonView annotation, Object conversionHint) {
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for handler methods with exactly 1 class argument: " + conversionHint);
		}
		return classes[0];
	}

	/**
	 * 确定用于给定内容类型的 JSON 编码。
	 * @param contentType the MIME type from the MessageHeaders, if any
	 * @return the JSON encoding to use (never {@code null})
	 */
	protected JsonEncoding getJsonEncoding(@Nullable MimeType contentType) {
		if (contentType != null && contentType.getCharset() != null) {
			Charset charset = contentType.getCharset();
			for (JsonEncoding encoding : JsonEncoding.values()) {
				if (charset.name().equals(encoding.getJavaName())) {
					return encoding;
				}
			}
		}
		return JsonEncoding.UTF8;
	}

}
