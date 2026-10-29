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

package org.springframework.jms.config;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link JmsListenerEndpoint}，提供要调用来处理此端点传入消息的方法。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class MethodJmsListenerEndpoint extends AbstractJmsListenerEndpoint implements BeanFactoryAware {

	@Nullable
	private Object bean;

	@Nullable
	private Method method;

	@Nullable
	private Method mostSpecificMethod;

	@Nullable
	private MessageHandlerMethodFactory messageHandlerMethodFactory;

	@Nullable
	private StringValueResolver embeddedValueResolver;


	/**
	 * 设置要在其上调用此端点方法的实际 bean 实例。
	 */
	public void setBean(@Nullable Object bean) {
		this.bean = bean;
	}

	@Nullable
	public Object getBean() {
		return this.bean;
	}

	/**
	 * 设置处理此端点所管理消息时要调用的方法。
	 */
	public void setMethod(@Nullable Method method) {
		this.method = method;
	}

	@Nullable
	public Method getMethod() {
		return this.method;
	}

	/**
	 * 设置此端点声明已知的最具体方法。
	 * <p>在代理的情况下，这将是目标类上的方法
	 * （前提是它本身带有注解，即不仅仅是接口中带有注解）。
	 * @since 4.2.3
	 */
	public void setMostSpecificMethod(@Nullable Method mostSpecificMethod) {
		this.mostSpecificMethod = mostSpecificMethod;
	}

	@Nullable
	public Method getMostSpecificMethod() {
		if (this.mostSpecificMethod != null) {
			return this.mostSpecificMethod;
		}
		Method method = getMethod();
		if (method != null) {
			Object bean = getBean();
			if (AopUtils.isAopProxy(bean)) {
				Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
				method = AopUtils.getMostSpecificMethod(method, targetClass);
			}
		}
		return method;
	}

	/**
	 * 设置用于构建负责管理此端点调用的 {@link InvocableHandlerMethod}
	 * 的 {@link MessageHandlerMethodFactory}。
	 */
	public void setMessageHandlerMethodFactory(MessageHandlerMethodFactory messageHandlerMethodFactory) {
		this.messageHandlerMethodFactory = messageHandlerMethodFactory;
	}

	/**
	 * 设置用于内嵌占位符和表达式的值解析器。
	 */
	public void setEmbeddedValueResolver(@Nullable StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}

	/**
	 * 设置用于解析表达式的 {@link BeanFactory}（可以为 {@code null}）。
	 */
	@Override
	public void setBeanFactory(@Nullable BeanFactory beanFactory) {
		if (this.embeddedValueResolver == null && beanFactory instanceof ConfigurableBeanFactory) {
			this.embeddedValueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}


	@Override
	protected MessagingMessageListenerAdapter createMessageListener(MessageListenerContainer container) {
		Assert.state(this.messageHandlerMethodFactory != null,
				"Could not create message listener - MessageHandlerMethodFactory not set");
		MessagingMessageListenerAdapter messageListener = createMessageListenerInstance();
		Object bean = getBean();
		Method method = getMethod();
		Assert.state(bean != null && method != null, "No bean+method set on endpoint");
		InvocableHandlerMethod invocableHandlerMethod =
				this.messageHandlerMethodFactory.createInvocableHandlerMethod(bean, method);
		messageListener.setHandlerMethod(invocableHandlerMethod);
		String responseDestination = getDefaultResponseDestination();
		if (StringUtils.hasText(responseDestination)) {
			if (container.isReplyPubSubDomain()) {
				messageListener.setDefaultResponseTopicName(responseDestination);
			}
			else {
				messageListener.setDefaultResponseQueueName(responseDestination);
			}
		}
		QosSettings responseQosSettings = container.getReplyQosSettings();
		if (responseQosSettings != null) {
			messageListener.setResponseQosSettings(responseQosSettings);
		}
		MessageConverter messageConverter = container.getMessageConverter();
		if (messageConverter != null) {
			messageListener.setMessageConverter(messageConverter);
		}
		DestinationResolver destinationResolver = container.getDestinationResolver();
		if (destinationResolver != null) {
			messageListener.setDestinationResolver(destinationResolver);
		}
		return messageListener;
	}

	/**
	 * 创建一个空的 {@link MessagingMessageListenerAdapter} 实例。
	 * @return a new {@code MessagingMessageListenerAdapter} or subclass thereof
	 */
	protected MessagingMessageListenerAdapter createMessageListenerInstance() {
		return new MessagingMessageListenerAdapter();
	}

	/**
	 * 返回默认的响应目标（如果有）。
	 */
	@Nullable
	protected String getDefaultResponseDestination() {
		Method specificMethod = getMostSpecificMethod();
		if (specificMethod == null) {
			return null;
		}
		SendTo ann = getSendTo(specificMethod);
		if (ann != null) {
			Object[] destinations = ann.value();
			if (destinations.length != 1) {
				throw new IllegalStateException("Invalid @" + SendTo.class.getSimpleName() + " annotation on '" +
						specificMethod + "' one destination must be set (got " + Arrays.toString(destinations) + ")");
			}
			return resolve((String) destinations[0]);
		}
		return null;
	}

	@Nullable
	private SendTo getSendTo(Method specificMethod) {
		SendTo ann = AnnotatedElementUtils.findMergedAnnotation(specificMethod, SendTo.class);
		if (ann == null) {
			ann = AnnotatedElementUtils.findMergedAnnotation(specificMethod.getDeclaringClass(), SendTo.class);
		}
		return ann;
	}

	@Nullable
	private String resolve(String value) {
		return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
	}


	@Override
	protected StringBuilder getEndpointDescription() {
		return super.getEndpointDescription()
				.append(" | bean='").append(this.bean).append('\'')
				.append(" | method='").append(this.method).append('\'');
	}

}
