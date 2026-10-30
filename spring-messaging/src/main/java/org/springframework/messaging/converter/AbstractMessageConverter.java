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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * {@link SmartMessageConverter} 实现的抽象基类，包含对通用属性的支持以及
 * 转换方法的部分实现，主要用于根据载荷类和 MIME 类型检查转换器是否支持该转换。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractMessageConverter implements SmartMessageConverter {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<MimeType> supportedMimeTypes = new ArrayList<>(4);

	@Nullable
	private ContentTypeResolver contentTypeResolver = new DefaultContentTypeResolver();

	private boolean strictContentTypeMatch = false;

	private Class<?> serializedPayloadClass = byte[].class;


	/**
	 * 使用单个 MIME 类型的构造函数。
	 * @param supportedMimeType 支持的 MIME 类型
	 */
	protected AbstractMessageConverter(MimeType supportedMimeType) {
		this(Collections.singletonList(supportedMimeType));
	}

	/**
	 * 通过可变参数传入一个或多个 MIME 类型的构造函数。
	 * @param supportedMimeTypes 支持的 MIME 类型
	 * @since 5.2.2
	 */
	protected AbstractMessageConverter(MimeType... supportedMimeTypes) {
		this(Arrays.asList(supportedMimeTypes));
	}

	/**
	 * 使用 MIME 类型集合的构造函数。
	 * @param supportedMimeTypes 支持的 MIME 类型
	 */
	protected AbstractMessageConverter(Collection<MimeType> supportedMimeTypes) {
		this.supportedMimeTypes.addAll(supportedMimeTypes);
	}


	/**
	 * 返回支持的 MIME 类型。
	 */
	public List<MimeType> getSupportedMimeTypes() {
		return Collections.unmodifiableList(this.supportedMimeTypes);
	}

	/**
	 * 允许子类添加更多支持的 MIME 类型。
	 * @since 5.2.2
	 */
	protected void addSupportedMimeTypes(MimeType... supportedMimeTypes) {
		this.supportedMimeTypes.addAll(Arrays.asList(supportedMimeTypes));
	}

	/**
	 * 配置用于解析输入消息内容类型的 {@link ContentTypeResolver}。
	 * <p>注意，如果未配置解析器，则应将
	 * {@link #setStrictContentTypeMatch(boolean) strictContentTypeMatch} 保持为
	 * {@code false}（默认值），否则此转换器将忽略所有消息。
	 * <p>默认情况下，使用 {@code DefaultContentTypeResolver} 实例。
	 */
	public void setContentTypeResolver(@Nullable ContentTypeResolver resolver) {
		this.contentTypeResolver = resolver;
	}

	/**
	 * 返回已配置的 {@link ContentTypeResolver}。
	 */
	@Nullable
	public ContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	/**
	 * 是否应转换那些无法通过已配置的
	 * {@link org.springframework.messaging.converter.ContentTypeResolver} 解析内容类型的消息。
	 * <p>只有当配置了 {@link #setContentTypeResolver contentTypeResolver}
	 * 且 {@link #getSupportedMimeTypes() supportedMimeTypes} 列表不为空时，
	 * 才能将转换器配置为严格模式。
	 * <p>当此标志设置为 {@code true} 时，如果未定义
	 * {@link #setContentTypeResolver contentTypeResolver} 或不存在 content-type 头，
	 * {@link #supportsMimeType(MessageHeaders)} 将返回 {@code false}。
	 */
	public void setStrictContentTypeMatch(boolean strictContentTypeMatch) {
		if (strictContentTypeMatch) {
			Assert.notEmpty(getSupportedMimeTypes(), "Strict match requires non-empty list of supported mime types");
			Assert.notNull(getContentTypeResolver(), "Strict match requires ContentTypeResolver");
		}
		this.strictContentTypeMatch = strictContentTypeMatch;
	}

	/**
	 * 内容类型解析是否必须产生与某个受支持 MIME 类型匹配的值。
	 */
	public boolean isStrictContentTypeMatch() {
		return this.strictContentTypeMatch;
	}

	/**
	 * 配置将 Object 载荷转换为 {@link Message} 时使用的首选序列化类（byte[] 或 String）。
	 * <p>默认值为 byte[]。
	 * @param payloadClass 可以是 byte[] 或 String
	 */
	public void setSerializedPayloadClass(Class<?> payloadClass) {
		Assert.isTrue(byte[].class == payloadClass || String.class == payloadClass,
				() -> "Payload class must be byte[] or String: " + payloadClass);
		this.serializedPayloadClass = payloadClass;
	}

	/**
	 * 返回已配置的首选序列化载荷类。
	 */
	public Class<?> getSerializedPayloadClass() {
		return this.serializedPayloadClass;
	}


	@Override
	@Nullable
	public final Object fromMessage(Message<?> message, Class<?> targetClass) {
		return fromMessage(message, targetClass, null);
	}

	@Override
	@Nullable
	public final Object fromMessage(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
		if (!canConvertFrom(message, targetClass)) {
			return null;
		}
		return convertFromInternal(message, targetClass, conversionHint);
	}

	@Override
	@Nullable
	public final Message<?> toMessage(Object payload, @Nullable MessageHeaders headers) {
		return toMessage(payload, headers, null);
	}

	@Override
	@Nullable
	public final Message<?> toMessage(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {
		if (!canConvertTo(payload, headers)) {
			return null;
		}

		Object payloadToUse = convertToInternal(payload, headers, conversionHint);
		if (payloadToUse == null) {
			return null;
		}

		MimeType mimeType = getDefaultContentType(payloadToUse);
		if (headers != null) {
			MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(headers, MessageHeaderAccessor.class);
			if (accessor != null && accessor.isMutable()) {
				if (mimeType != null) {
					accessor.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, mimeType);
				}
				return MessageBuilder.createMessage(payloadToUse, accessor.getMessageHeaders());
			}
		}

		MessageBuilder<?> builder = MessageBuilder.withPayload(payloadToUse);
		if (headers != null) {
			builder.copyHeaders(headers);
		}
		if (mimeType != null) {
			builder.setHeaderIfAbsent(MessageHeaders.CONTENT_TYPE, mimeType);
		}
		return builder.build();
	}


	protected boolean canConvertFrom(Message<?> message, Class<?> targetClass) {
		return (supports(targetClass) && supportsMimeType(message.getHeaders()));
	}

	protected boolean canConvertTo(Object payload, @Nullable MessageHeaders headers) {
		return (supports(payload.getClass()) && supportsMimeType(headers));
	}

	protected boolean supportsMimeType(@Nullable MessageHeaders headers) {
		if (getSupportedMimeTypes().isEmpty()) {
			return true;
		}
		MimeType mimeType = getMimeType(headers);
		if (mimeType == null) {
			return !isStrictContentTypeMatch();
		}
		for (MimeType current : getSupportedMimeTypes()) {
			if (current.getType().equals(mimeType.getType()) && current.getSubtype().equals(mimeType.getSubtype())) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	protected MimeType getMimeType(@Nullable MessageHeaders headers) {
		return (headers != null && this.contentTypeResolver != null ? this.contentTypeResolver.resolve(headers) : null);
	}

	/**
	 * 返回载荷的默认内容类型。在未提供消息头或未提供 content-type 头的情况下
	 * 调用 {@link #toMessage(Object, MessageHeaders)} 时会调用此方法。
	 * <p>默认情况下，如果存在，则返回 {@link #getSupportedMimeTypes() supportedMimeTypes}
	 * 的第一个元素。可在子类中重写。
	 * @param payload 要转换为消息的载荷
	 * @return 内容类型；如果未知则返回 {@code null}
	 */
	@Nullable
	protected MimeType getDefaultContentType(Object payload) {
		List<MimeType> mimeTypes = getSupportedMimeTypes();
		return (!mimeTypes.isEmpty() ? mimeTypes.get(0) : null);
	}


	/**
	 * 给定类是否受此转换器支持。
	 * @param clazz 要检查是否受支持的类
	 * @return 如果受支持则返回 {@code true}；否则返回 {@code false}
	 */
	protected abstract boolean supports(Class<?> clazz);

	/**
	 * 将消息载荷从序列化形式转换为 Object。
	 * @param message 输入消息
	 * @param targetClass 转换的目标类
	 * @param conversionHint 传递给 {@link MessageConverter} 的额外对象，
	 * 例如关联的 {@code MethodParameter}（可以为 {@code null}）
	 * @return 转换结果；如果转换器无法执行转换则返回 {@code null}
	 * @since 4.2
	 */
	@Nullable
	protected Object convertFromInternal(
			Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {

		return null;
	}

	/**
	 * 将载荷对象转换为序列化形式。
	 * @param payload 要转换的对象
	 * @param headers 消息的可选消息头（可以为 {@code null}）
	 * @param conversionHint 传递给 {@link MessageConverter} 的额外对象，
	 * 例如关联的 {@code MethodParameter}（可以为 {@code null}）
	 * @return 生成的消息载荷；如果转换器无法执行转换则返回 {@code null}
	 * @since 4.2
	 */
	@Nullable
	protected Object convertToInternal(
			Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {

		return null;
	}


	static Type getResolvedType(Class<?> targetClass, @Nullable Object conversionHint) {
		if (conversionHint instanceof MethodParameter) {
			MethodParameter param = (MethodParameter) conversionHint;
			param = param.nestedIfOptional();
			if (Message.class.isAssignableFrom(param.getParameterType())) {
				param = param.nested();
			}
			Type genericParameterType = param.getNestedGenericParameterType();
			Class<?> contextClass = param.getContainingClass();
			return GenericTypeResolver.resolveType(genericParameterType, contextClass);
		}
		return targetClass;
	}

}
