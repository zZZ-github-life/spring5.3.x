/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.adapter.standard;

import java.nio.ByteBuffer;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ContextLoader;

/**
 * 可用于实现标准 {@link javax.websocket.Encoder}
 * 和/或 {@link javax.websocket.Decoder} 的基类。它提供了委托给 Spring
 * {@link ConversionService} 的编码和解码方法实现。
 *
 * <p>默认情况下，此类会在
 * {@link #getApplicationContext() 当前 ApplicationContext} 中查找以
 * {@code 'webSocketConversionService'} 名称注册的 {@link ConversionService}。
 * 在 Servlet 容器环境中，这对客户端端点和服务器端点都适用。
 * 如果不在 Servlet 容器中运行，子类需要覆盖
 * {@link #getConversionService()} 方法以提供替代的查找策略。
 *
 * <p>子类可以继承此类，并且还应实现
 * {@link javax.websocket.Encoder} 和 {@link javax.websocket.Decoder}
 * 中的一个或两个。为方便起见，提供了
 * {@link ConvertingEncoderDecoderSupport.BinaryEncoder}、
 * {@link ConvertingEncoderDecoderSupport.BinaryDecoder}、
 * {@link ConvertingEncoderDecoderSupport.TextEncoder} 和
 * {@link ConvertingEncoderDecoderSupport.TextDecoder} 子类。
 *
 * <p>由于 JSR-356 只允许按类型注册 Encoder/Decoder，
 * 因此此类的实例由 WebSocket 运行时管理，无需注册为 Spring Bean。
 * 不过，它们可以通过 {@link Autowired @Autowire} 注入由 Spring 管理的依赖。
 *
 * <p>应注册用于在 {@link #getType() 类型} 与 {@code String} 或
 * {@code ByteBuffer} 之间进行转换的转换器。
 *
 * @author Phillip Webb
 * @since 4.0
 * @param <T> the type being converted to (for Encoder) or from (for Decoder)
 * @param <M> the WebSocket message type ({@link String} or {@link ByteBuffer})
 * @see ConvertingEncoderDecoderSupport.BinaryEncoder
 * @see ConvertingEncoderDecoderSupport.BinaryDecoder
 * @see ConvertingEncoderDecoderSupport.TextEncoder
 * @see ConvertingEncoderDecoderSupport.TextDecoder
 */
public abstract class ConvertingEncoderDecoderSupport<T, M> {

	private static final String CONVERSION_SERVICE_BEAN_NAME = "webSocketConversionService";


	/**
	 * 调用以初始化编码器/解码器。
	 * @see javax.websocket.Encoder#init(EndpointConfig)
	 * @see javax.websocket.Decoder#init(EndpointConfig)
	 */
	public void init(EndpointConfig config) {
		ApplicationContext applicationContext = getApplicationContext();
		if (applicationContext instanceof ConfigurableApplicationContext) {
			ConfigurableListableBeanFactory beanFactory =
					((ConfigurableApplicationContext) applicationContext).getBeanFactory();
			beanFactory.autowireBean(this);
		}
	}

	/**
	 * 调用以销毁编码器/解码器。
	 * @see javax.websocket.Encoder#destroy()
	 * @see javax.websocket.Decoder#destroy()
	 */
	public void destroy() {
	}

	/**
	 * 用于获取 {@link ConversionService} 的策略方法。默认情况下，此方法
	 * 期望在 {@link #getApplicationContext() 当前 ApplicationContext} 中
	 * 存在一个名为 {@code 'webSocketConversionService'} 的 Bean。
	 * @return the {@link ConversionService} (never null)
	 */
	protected ConversionService getConversionService() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "Unable to locate the Spring ApplicationContext");
		try {
			return applicationContext.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
		}
		catch (BeansException ex) {
			throw new IllegalStateException("Unable to find ConversionService: please configure a '" +
					CONVERSION_SERVICE_BEAN_NAME + "' or override the getConversionService() method", ex);
		}
	}

	/**
	 * 返回当前活动的 {@link ApplicationContext}。默认情况下，此方法通过
	 * {@link ContextLoader#getCurrentWebApplicationContext()} 获取上下文，
	 * 该方法通常会在 Servlet 容器环境中找到通过 {@link ContextLoader}
	 * 加载的 ApplicationContext。如果不在 Servlet 容器中运行，
	 * 并且未使用 {@link ContextLoader}，则应覆盖此方法。
	 * @return the {@link ApplicationContext} or {@code null}
	 */
	@Nullable
	protected ApplicationContext getApplicationContext() {
		return ContextLoader.getCurrentWebApplicationContext();
	}

	/**
	 * 返回要转换的类型。默认情况下，该类型通过
	 * 类的泛型参数解析。
	 */
	protected TypeDescriptor getType() {
		return TypeDescriptor.valueOf(resolveTypeArguments()[0]);
	}

	/**
	 * 返回 WebSocket 消息类型。默认情况下，该类型通过
	 * 类的泛型参数解析。
	 */
	protected TypeDescriptor getMessageType() {
		return TypeDescriptor.valueOf(resolveTypeArguments()[1]);
	}

	private Class<?>[] resolveTypeArguments() {
		Class<?>[] resolved = GenericTypeResolver.resolveTypeArguments(getClass(), ConvertingEncoderDecoderSupport.class);
		if (resolved == null) {
			throw new IllegalStateException("ConvertingEncoderDecoderSupport's generic types T and M " +
					"need to be substituted in subclass: " + getClass());
		}
		return resolved;
	}

	/**
	 * 将对象编码为消息。
	 * @see javax.websocket.Encoder.Text#encode(Object)
	 * @see javax.websocket.Encoder.Binary#encode(Object)
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public M encode(T object) throws EncodeException {
		try {
			return (M) getConversionService().convert(object, getType(), getMessageType());
		}
		catch (ConversionException ex) {
			throw new EncodeException(object, "Unable to encode websocket message using ConversionService", ex);
		}
	}

	/**
	 * 判断给定消息是否可以被解码。
	 * @see #decode(Object)
	 * @see javax.websocket.Decoder.Text#willDecode(String)
	 * @see javax.websocket.Decoder.Binary#willDecode(ByteBuffer)
	 */
	public boolean willDecode(M bytes) {
		return getConversionService().canConvert(getType(), getMessageType());
	}

	/**
	 * 将消息解码为对象。
	 * @see javax.websocket.Decoder.Text#decode(String)
	 * @see javax.websocket.Decoder.Binary#decode(ByteBuffer)
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public T decode(M message) throws DecodeException {
		try {
			return (T) getConversionService().convert(message, getMessageType(), getType());
		}
		catch (ConversionException ex) {
			if (message instanceof String) {
				throw new DecodeException((String) message,
						"Unable to decode websocket message using ConversionService", ex);
			}
			if (message instanceof ByteBuffer) {
				throw new DecodeException((ByteBuffer) message,
						"Unable to decode websocket message using ConversionService", ex);
			}
			throw ex;
		}
	}


	/**
	 * 一个委托给 Spring 转换服务的二进制
	 * {@link javax.websocket.Encoder.Binary javax.websocket.Encoder}。
	 * 详情请参见 {@link ConvertingEncoderDecoderSupport}。
	 * @param <T> the type that this Encoder can convert to
	 */
	public abstract static class BinaryEncoder<T> extends ConvertingEncoderDecoderSupport<T, ByteBuffer>
			implements Encoder.Binary<T> {
	}


	/**
	 * 一个委托给 Spring 转换服务的二进制
	 * {@link javax.websocket.Encoder.Binary javax.websocket.Encoder}。
	 * 详情请参见 {@link ConvertingEncoderDecoderSupport}。
	 * @param <T> the type that this Decoder can convert from
	 */
	public abstract static class BinaryDecoder<T> extends ConvertingEncoderDecoderSupport<T, ByteBuffer>
			implements Decoder.Binary<T> {
	}


	/**
	 * 一个委托给 Spring 转换服务的文本
	 * {@link javax.websocket.Encoder.Text javax.websocket.Encoder}。
	 * 详情请参见 {@link ConvertingEncoderDecoderSupport}。
	 * @param <T> the type that this Encoder can convert to
	 */
	public abstract static class TextEncoder<T> extends ConvertingEncoderDecoderSupport<T, String>
			implements Encoder.Text<T> {
	}


	/**
	 * 一个委托给 Spring 转换服务的文本
	 * {@link javax.websocket.Encoder.Text javax.websocket.Encoder}。
	 * 详情请参见 {@link ConvertingEncoderDecoderSupport}。
	 * @param <T> the type that this Decoder can convert from
	 */
	public abstract static class TextDecoder<T> extends ConvertingEncoderDecoderSupport<T, String>
			implements Decoder.Text<T> {
	}

}
