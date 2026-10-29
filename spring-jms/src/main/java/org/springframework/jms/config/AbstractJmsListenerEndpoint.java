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

import javax.jms.MessageListener;

import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.endpoint.JmsActivationSpecConfig;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * JMS 监听器端点的基类模型。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see MethodJmsListenerEndpoint
 * @see SimpleJmsListenerEndpoint
 */
public abstract class AbstractJmsListenerEndpoint implements JmsListenerEndpoint {

	private String id = "";

	@Nullable
	private String destination;

	@Nullable
	private String subscription;

	@Nullable
	private String selector;

	@Nullable
	private String concurrency;


	/**
	 * 为此端点设置自定义 id。
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * 返回此端点的 id（可能是自动生成的）。
	 */
	@Override
	public String getId() {
		return this.id;
	}

	/**
	 * 设置此端点的目标名称。
	 */
	public void setDestination(@Nullable String destination) {
		this.destination = destination;
	}

	/**
	 * 返回此端点的目标名称。
	 */
	@Nullable
	public String getDestination() {
		return this.destination;
	}

	/**
	 * 设置持久订阅的名称。
	 */
	public void setSubscription(@Nullable String subscription) {
		this.subscription = subscription;
	}

	/**
	 * 返回持久订阅的名称（如果有）。
	 */
	@Nullable
	public String getSubscription() {
		return this.subscription;
	}

	/**
	 * 设置 JMS 消息选择器表达式。
	 * <p>有关选择器表达式的详细定义，请参见 JMS 规范。
	 */
	public void setSelector(@Nullable String selector) {
		this.selector = selector;
	}

	/**
	 * 返回 JMS 消息选择器表达式（如果有）。
	 */
	@Nullable
	public String getSelector() {
		return this.selector;
	}

	/**
	 * 设置监听器的并发（如果有）。
	 * <p>并发上限可以是 "lower-upper"（下限-上限）形式的字符串，例如 "5-10"，
	 * 也可以只是上限字符串，例如 "10"（此时下限为 1）。
	 * <p>底层容器可能支持也可能不支持所有特性。例如，它可能无法伸缩：
	 * 此时只使用上限值。
	 */
	public void setConcurrency(@Nullable String concurrency) {
		this.concurrency = concurrency;
	}

	/**
	 * 返回监听器的并发（如果有）。
	 */
	@Nullable
	public String getConcurrency() {
		return this.concurrency;
	}


	@Override
	public void setupListenerContainer(MessageListenerContainer listenerContainer) {
		if (listenerContainer instanceof AbstractMessageListenerContainer) {
			setupJmsListenerContainer((AbstractMessageListenerContainer) listenerContainer);
		}
		else {
			new JcaEndpointConfigurer().configureEndpoint(listenerContainer);
		}
	}

	private void setupJmsListenerContainer(AbstractMessageListenerContainer listenerContainer) {
		if (StringUtils.hasText(getId())) {
			listenerContainer.setBeanName(getId());
		}
		if (getDestination() != null) {
			listenerContainer.setDestinationName(getDestination());
		}
		if (getSubscription() != null) {
			listenerContainer.setSubscriptionName(getSubscription());
		}
		if (getSelector() != null) {
			listenerContainer.setMessageSelector(getSelector());
		}
		if (getConcurrency() != null) {
			listenerContainer.setConcurrency(getConcurrency());
		}
		setupMessageListener(listenerContainer);
	}

	/**
	 * 创建一个 {@link MessageListener}，使其能够为指定容器服务此端点。
	 */
	protected abstract MessageListener createMessageListener(MessageListenerContainer container);

	private void setupMessageListener(MessageListenerContainer container) {
		container.setupMessageListener(createMessageListener(container));
	}

	/**
	 * 返回此端点的描述。
	 * <p>可供子类使用，以便包含在其 {@code toString()} 结果中。
	 */
	protected StringBuilder getEndpointDescription() {
		StringBuilder result = new StringBuilder();
		return result.append(getClass().getSimpleName()).append('[').append(this.id).append("] destination=").
				append(this.destination).append("' | subscription='").append(this.subscription).
				append(" | selector='").append(this.selector).append('\'');
	}

	@Override
	public String toString() {
		return getEndpointDescription().toString();
	}


	/**
	 * 内部类，用于避免对 JCA API 的硬依赖。
	 */
	private class JcaEndpointConfigurer {

		public void configureEndpoint(Object listenerContainer) {
			if (listenerContainer instanceof JmsMessageEndpointManager) {
				setupJcaMessageContainer((JmsMessageEndpointManager) listenerContainer);
			}
			else {
				throw new IllegalArgumentException("Could not configure endpoint with the specified container '" +
						listenerContainer + "' Only JMS (" + AbstractMessageListenerContainer.class.getName() +
						" subclass) or JCA (" + JmsMessageEndpointManager.class.getName() + ") are supported.");
			}
		}

		private void setupJcaMessageContainer(JmsMessageEndpointManager container) {
			JmsActivationSpecConfig activationSpecConfig = container.getActivationSpecConfig();
			if (activationSpecConfig == null) {
				activationSpecConfig = new JmsActivationSpecConfig();
				container.setActivationSpecConfig(activationSpecConfig);
			}
			if (getDestination() != null) {
				activationSpecConfig.setDestinationName(getDestination());
			}
			if (getSubscription() != null) {
				activationSpecConfig.setSubscriptionName(getSubscription());
			}
			if (getSelector() != null) {
				activationSpecConfig.setMessageSelector(getSelector());
			}
			if (getConcurrency() != null) {
				activationSpecConfig.setConcurrency(getConcurrency());
			}
			setupMessageListener(container);
		}
	}

}
