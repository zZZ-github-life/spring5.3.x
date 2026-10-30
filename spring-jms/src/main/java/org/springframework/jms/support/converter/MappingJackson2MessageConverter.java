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

package org.springframework.jms.support.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 使用 Jackson 2.x 在消息与 JSON 之间进行转换的消息转换器。
 * 将对象映射为 {@link BytesMessage}；如果 {@link #setTargetType targetType}
 * 设置为 {@link MessageType#TEXT}，则映射为 {@link TextMessage}。
 * 可将 {@link TextMessage} 或 {@link BytesMessage} 转换为对象。
 *
 * <p>它通过以下设置自定义 Jackson 的默认属性：
 * <ul>
 * <li>禁用 {@link MapperFeature#DEFAULT_VIEW_INCLUSION}</li>
 * <li>禁用 {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}</li>
 * </ul>
 *
 * <p>截至 Spring 5.3，兼容 Jackson 2.9 至 2.12。
 *
 * @author Mark Pollack
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1.4
 */
public class MappingJackson2MessageConverter implements SmartMessageConverter, BeanClassLoaderAware {

	/**
	 * 写入文本消息时使用的默认编码：UTF-8。
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";


	private ObjectMapper objectMapper;

	private MessageType targetType = MessageType.BYTES;

	@Nullable
	private String encoding;

	@Nullable
	private String encodingPropertyName;

	@Nullable
	private String typeIdPropertyName;

	private Map<String, Class<?>> idClassMappings = new HashMap<>();

	private Map<Class<?>, String> classIdMappings = new HashMap<>();

	@Nullable
	private ClassLoader beanClassLoader;


	@SuppressWarnings("deprecation")  // on Jackson 2.13: configure(MapperFeature, boolean)
	public MappingJackson2MessageConverter() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * 指定要使用的 {@link ObjectMapper}，而不是使用默认的。
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.objectMapper = objectMapper;
	}

	/**
	 * 指定 {@link #toMessage(Object, Session)} 应编组为
	 * {@link BytesMessage} 还是 {@link TextMessage}。
	 * <p>默认为 {@link MessageType#BYTES}，即此转换器编组为
	 * {@link BytesMessage}。请注意，此转换器的默认版本仅支持
	 * {@link MessageType#BYTES} 和 {@link MessageType#TEXT}。
	 * @see MessageType#BYTES
	 * @see MessageType#TEXT
	 */
	public void setTargetType(MessageType targetType) {
		Assert.notNull(targetType, "MessageType must not be null");
		this.targetType = targetType;
	}

	/**
	 * 指定在文本形式的消息体内容之间转换时使用的编码。默认编码为 "UTF-8"。
	 * <p>从文本消息读取时，可能已通过特殊的 JMS 属性建议了编码，
	 * 该编码将优先于此 MessageConverter 实例上设置的编码。
	 * @see #setEncodingPropertyName
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 指定 JMS 消息属性的名称，当转换过程中使用 BytesMessage 时，
	 * 该属性携带从字节到 String（以及反向）转换所用的编码。
	 * <p>默认为无。设置此属性是可选的；如果未设置，将对任何入站字节消息
	 * 使用 UTF-8 进行解码。
	 * @see #setEncoding
	 */
	public void setEncodingPropertyName(String encodingPropertyName) {
		this.encodingPropertyName = encodingPropertyName;
	}

	/**
	 * 指定 JMS 消息属性的名称，该属性携带所包含对象的类型 ID：
	 * 要么是映射后的 ID 值，要么是原始 Java 类名。
	 * <p>默认为无。<b>注意：为了能够将入站消息转换为 Java 对象，
	 * 必须设置此属性。</b>
	 * @see #setTypeIdMappings
	 */
	public void setTypeIdPropertyName(String typeIdPropertyName) {
		this.typeIdPropertyName = typeIdPropertyName;
	}

	/**
	 * 如有需要，指定从类型 ID 到 Java 类的映射。
	 * 这样可以在类型 ID 消息属性中使用合成的 ID，
	 * 而不必传输 Java 类名。
	 * <p>默认没有自定义映射，即传输原始 Java 类名。
	 * @param typeIdMappings 以类型 id 值为键、以 Java 类为值的 Map
	 */
	public void setTypeIdMappings(Map<String, Class<?>> typeIdMappings) {
		this.idClassMappings = new HashMap<>();
		typeIdMappings.forEach((id, clazz) -> {
			this.idClassMappings.put(id, clazz);
			this.classIdMappings.put(clazz, id);
		});
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
		Message message;
		try {
			switch (this.targetType) {
				case TEXT:
					message = mapToTextMessage(object, session, this.objectMapper.writer());
					break;
				case BYTES:
					message = mapToBytesMessage(object, session, this.objectMapper.writer());
					break;
				default:
					message = mapToMessage(object, session, this.objectMapper.writer(), this.targetType);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not map JSON object [" + object + "]", ex);
		}
		setTypeIdOnMessage(object, message);
		return message;
	}

	@Override
	public Message toMessage(Object object, Session session, @Nullable Object conversionHint)
			throws JMSException, MessageConversionException {

		return toMessage(object, session, getSerializationView(conversionHint));
	}

	/**
	 * 使用指定的 JSON 视图和给定的会话将 Java 对象转换为 JMS Message，
	 * 由该会话创建消息对象。
	 * @param object 要转换的对象
	 * @param session 用于创建 JMS Message 的 Session
	 * @param jsonView 用于过滤内容的视图
	 * @return 该 JMS Message
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 * @throws MessageConversionException 转换失败时
	 * @since 4.3
	 */
	public Message toMessage(Object object, Session session, @Nullable Class<?> jsonView)
			throws JMSException, MessageConversionException {

		if (jsonView != null) {
			return toMessage(object, session, this.objectMapper.writerWithView(jsonView));
		}
		else {
			return toMessage(object, session, this.objectMapper.writer());
		}
	}

	@Override
	public Object fromMessage(Message message) throws JMSException, MessageConversionException {
		try {
			JavaType targetJavaType = getJavaTypeForMessage(message);
			return convertToObject(message, targetJavaType);
		}
		catch (IOException ex) {
			throw new MessageConversionException("Failed to convert JSON message content", ex);
		}
	}

	protected Message toMessage(Object object, Session session, ObjectWriter objectWriter)
			throws JMSException, MessageConversionException {

		Message message;
		try {
			switch (this.targetType) {
				case TEXT:
					message = mapToTextMessage(object, session, objectWriter);
					break;
				case BYTES:
					message = mapToBytesMessage(object, session, objectWriter);
					break;
				default:
					message = mapToMessage(object, session, objectWriter, this.targetType);
			}
		}
		catch (IOException ex) {
			throw new MessageConversionException("Could not map JSON object [" + object + "]", ex);
		}
		setTypeIdOnMessage(object, message);
		return message;
	}


	/**
	 * 将给定对象映射为 {@link TextMessage}。
	 * @param object 要映射的对象
	 * @param session 当前的 JMS Session
	 * @param objectWriter 要使用的写入器
	 * @return 生成的消息
	 * @throws JMSException 如果由 JMS 方法抛出
	 * @throws IOException 发生 I/O 错误时
	 * @since 4.3
	 * @see Session#createBytesMessage
	 */
	protected TextMessage mapToTextMessage(Object object, Session session, ObjectWriter objectWriter)
			throws JMSException, IOException {

		StringWriter writer = new StringWriter(1024);
		objectWriter.writeValue(writer, object);
		return session.createTextMessage(writer.toString());
	}

	/**
	 * 将给定对象映射为 {@link BytesMessage}。
	 * @param object 要映射的对象
	 * @param session 当前的 JMS Session
	 * @param objectWriter 要使用的写入器
	 * @return 生成的消息
	 * @throws JMSException 如果由 JMS 方法抛出
	 * @throws IOException 发生 I/O 错误时
	 * @since 4.3
	 * @see Session#createBytesMessage
	 */
	protected BytesMessage mapToBytesMessage(Object object, Session session, ObjectWriter objectWriter)
			throws JMSException, IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
		if (this.encoding != null) {
			OutputStreamWriter writer = new OutputStreamWriter(bos, this.encoding);
			objectWriter.writeValue(writer, object);
		}
		else {
			// Jackson usually defaults to UTF-8 but can also go straight to bytes, e.g. for Smile.
			// We use a direct byte array argument for the latter case to work as well.
			objectWriter.writeValue(bos, object);
		}

		BytesMessage message = session.createBytesMessage();
		message.writeBytes(bos.toByteArray());
		if (this.encodingPropertyName != null) {
			message.setStringProperty(this.encodingPropertyName,
					(this.encoding != null ? this.encoding : DEFAULT_ENCODING));
		}
		return message;
	}

	/**
	 * 允许自定义消息映射的模板方法。
	 * 当 {@link #setTargetType} 不是 {@link MessageType#TEXT} 或
	 * {@link MessageType#BYTES} 时调用。
	 * <p>默认实现抛出 {@link IllegalArgumentException}。
	 * @param object 要编组的对象
	 * @param session 该 JMS Session
	 * @param objectWriter 要使用的写入器
	 * @param targetType 目标消息类型（TEXT 或 BYTES 之外）
	 * @return 生成的消息
	 * @throws JMSException 如果由 JMS 方法抛出
	 * @throws IOException 发生 I/O 错误时
	 */
	protected Message mapToMessage(Object object, Session session, ObjectWriter objectWriter, MessageType targetType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + targetType +
				"]. MappingJackson2MessageConverter by default only supports TextMessages and BytesMessages.");
	}

	/**
	 * 在给定的 JMS Message 上为给定负载对象设置类型 ID。
	 * <p>默认实现会查询已配置的类型 ID 映射，并将结果值
	 * （映射后的 ID 或原始 Java 类名）设置到已配置的类型 ID 消息属性中。
	 * @param object 要为其设置类型 id 的有效负载对象
	 * @param message 要在其上设置 type id 属性的 JMS Message
	 * @throws JMSException 如果由 JMS 方法抛出
	 * @see #getJavaTypeForMessage(javax.jms.Message)
	 * @see #setTypeIdPropertyName(String)
	 * @see #setTypeIdMappings(java.util.Map)
	 */
	protected void setTypeIdOnMessage(Object object, Message message) throws JMSException {
		if (this.typeIdPropertyName != null) {
			String typeId = this.classIdMappings.get(object.getClass());
			if (typeId == null) {
				typeId = object.getClass().getName();
			}
			message.setStringProperty(this.typeIdPropertyName, typeId);
		}
	}

	/**
	 * 便捷方法，将处理分派给针对各个消息类型的转换方法。
	 */
	private Object convertToObject(Message message, JavaType targetJavaType) throws JMSException, IOException {
		if (message instanceof TextMessage) {
			return convertFromTextMessage((TextMessage) message, targetJavaType);
		}
		else if (message instanceof BytesMessage) {
			return convertFromBytesMessage((BytesMessage) message, targetJavaType);
		}
		else {
			return convertFromMessage(message, targetJavaType);
		}
	}

	/**
	 * 将 TextMessage 转换为指定类型的 Java 对象。
	 * @param message 输入消息
	 * @param targetJavaType 目标类型
	 * @return 转换为对象后的消息
	 * @throws JMSException 如果由 JMS 抛出
	 * @throws IOException 发生 I/O 错误时
	 */
	protected Object convertFromTextMessage(TextMessage message, JavaType targetJavaType)
			throws JMSException, IOException {

		String body = message.getText();
		return this.objectMapper.readValue(body, targetJavaType);
	}

	/**
	 * 将 BytesMessage 转换为指定类型的 Java 对象。
	 * @param message 输入消息
	 * @param targetJavaType 目标类型
	 * @return 转换为对象后的消息
	 * @throws JMSException 如果由 JMS 抛出
	 * @throws IOException 发生 I/O 错误时
	 */
	protected Object convertFromBytesMessage(BytesMessage message, JavaType targetJavaType)
			throws JMSException, IOException {

		String encoding = this.encoding;
		if (this.encodingPropertyName != null && message.propertyExists(this.encodingPropertyName)) {
			encoding = message.getStringProperty(this.encodingPropertyName);
		}
		byte[] bytes = new byte[(int) message.getBodyLength()];
		message.readBytes(bytes);
		if (encoding != null) {
			try {
				String body = new String(bytes, encoding);
				return this.objectMapper.readValue(body, targetJavaType);
			}
			catch (UnsupportedEncodingException ex) {
				throw new MessageConversionException("Cannot convert bytes to String", ex);
			}
		}
		else {
			// Jackson internally performs encoding detection, falling back to UTF-8.
			return this.objectMapper.readValue(bytes, targetJavaType);
		}
	}

	/**
	 * 允许自定义消息映射的模板方法。
	 * 当 {@link #setTargetType} 不是 {@link MessageType#TEXT} 或
	 * {@link MessageType#BYTES} 时调用。
	 * <p>默认实现抛出 {@link IllegalArgumentException}。
	 * @param message 输入消息
	 * @param targetJavaType 目标类型
	 * @return 转换为对象后的消息
	 * @throws JMSException 如果由 JMS 抛出
	 * @throws IOException 发生 I/O 错误时
	 */
	protected Object convertFromMessage(Message message, JavaType targetJavaType)
			throws JMSException, IOException {

		throw new IllegalArgumentException("Unsupported message type [" + message.getClass() +
				"]. MappingJacksonMessageConverter by default only supports TextMessages and BytesMessages.");
	}

	/**
	 * 为给定的 JMS Message 确定 Jackson JavaType，
	 * 通常通过解析类型 ID 消息属性实现。
	 * <p>默认实现解析已配置的类型 ID 属性名称并查询已配置的类型 ID 映射。
	 * 可以用不同的策略覆盖此方法，例如根据消息来源做一些启发式判断。
	 * @param message 要从中获取 type id 属性的 JMS Message
	 * @throws JMSException 如果由 JMS 方法抛出
	 * @see #setTypeIdOnMessage(Object, javax.jms.Message)
	 * @see #setTypeIdPropertyName(String)
	 * @see #setTypeIdMappings(java.util.Map)
	 */
	protected JavaType getJavaTypeForMessage(Message message) throws JMSException {
		String typeId = message.getStringProperty(this.typeIdPropertyName);
		if (typeId == null) {
			throw new MessageConversionException(
					"Could not find type id property [" + this.typeIdPropertyName + "] on message [" +
					message.getJMSMessageID() + "] from destination [" + message.getJMSDestination() + "]");
		}
		Class<?> mappedClass = this.idClassMappings.get(typeId);
		if (mappedClass != null) {
			return this.objectMapper.constructType(mappedClass);
		}
		try {
			Class<?> typeClass = ClassUtils.forName(typeId, this.beanClassLoader);
			return this.objectMapper.constructType(typeClass);
		}
		catch (Throwable ex) {
			throw new MessageConversionException("Failed to resolve type id [" + typeId + "]", ex);
		}
	}

	/**
	 * 根据给定的转换提示确定 Jackson 序列化视图。
	 * @param conversionHint 为当前转换尝试传入转换器的转换提示对象
	 * @return 序列化视图类；如果没有，则为 {@code null}
	 */
	@Nullable
	protected Class<?> getSerializationView(@Nullable Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter methodParam = (MethodParameter) conversionHint;
			JsonView annotation = methodParam.getParameterAnnotation(JsonView.class);
			if (annotation == null) {
				annotation = methodParam.getMethodAnnotation(JsonView.class);
				if (annotation == null) {
					return null;
				}
			}
			return extractViewClass(annotation, conversionHint);
		}
		else if (conversionHint instanceof JsonView) {
			return extractViewClass((JsonView) conversionHint, conversionHint);
		}
		else if (conversionHint instanceof Class) {
			return (Class<?>) conversionHint;
		}
		else {
			return null;
		}
	}

	private Class<?> extractViewClass(JsonView annotation, Object conversionHint) {
		Class<?>[] classes = annotation.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for handler methods with exactly 1 class argument: " + conversionHint);
		}
		return classes[0];
	}

}
