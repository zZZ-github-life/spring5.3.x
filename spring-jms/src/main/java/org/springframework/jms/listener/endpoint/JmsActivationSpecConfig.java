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

package org.springframework.jms.listener.endpoint;

import javax.jms.Session;

import org.springframework.core.Constants;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.lang.Nullable;

/**
 * 用于激活 JMS 消息端点的通用配置对象。
 * 会被转换为特定于提供者的 JCA 1.5 ActivationSpec 对象，
 * 用于激活端点。
 *
 * <p>通常与 {@link JmsMessageEndpointManager} 结合使用，
 * 但并非必须绑定于它。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 * @see JmsActivationSpecFactory
 * @see JmsMessageEndpointManager#setActivationSpecConfig
 * @see javax.resource.spi.ResourceAdapter#endpointActivation
 */
public class JmsActivationSpecConfig {

	/** {@code javax.jms.Session} 的 Constants 实例。 */
	private static final Constants sessionConstants = new Constants(Session.class);


	@Nullable
	private String destinationName;

	private boolean pubSubDomain = false;

	@Nullable
	private Boolean replyPubSubDomain;

	@Nullable
	private QosSettings replyQosSettings;

	private boolean subscriptionDurable = false;

	private boolean subscriptionShared = false;

	@Nullable
	private String subscriptionName;

	@Nullable
	private String clientId;

	@Nullable
	private String messageSelector;

	private int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;

	private int maxConcurrency = -1;

	private int prefetchSize = -1;

	@Nullable
	private MessageConverter messageConverter;


	public void setDestinationName(@Nullable String destinationName) {
		this.destinationName = destinationName;
	}

	@Nullable
	public String getDestinationName() {
		return this.destinationName;
	}

	public void setPubSubDomain(boolean pubSubDomain) {
		this.pubSubDomain = pubSubDomain;
	}

	public boolean isPubSubDomain() {
		return this.pubSubDomain;
	}

	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	public boolean isReplyPubSubDomain() {
		if (this.replyPubSubDomain != null) {
			return this.replyPubSubDomain;
		}
		else {
			return isPubSubDomain();
		}
	}

	public void setReplyQosSettings(@Nullable QosSettings replyQosSettings) {
		this.replyQosSettings = replyQosSettings;
	}

	@Nullable
	public QosSettings getReplyQosSettings() {
		return this.replyQosSettings;
	}

	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
		if (subscriptionDurable) {
			this.pubSubDomain = true;
		}
	}

	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	public void setSubscriptionShared(boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
		if (subscriptionShared) {
			this.pubSubDomain = true;
		}
	}

	public boolean isSubscriptionShared() {
		return this.subscriptionShared;
	}

	public void setSubscriptionName(@Nullable String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	@Nullable
	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	public void setDurableSubscriptionName(@Nullable String durableSubscriptionName) {
		this.subscriptionName = durableSubscriptionName;
		this.subscriptionDurable = (durableSubscriptionName != null);
	}

	@Nullable
	public String getDurableSubscriptionName() {
		return (this.subscriptionDurable ? this.subscriptionName : null);
	}

	public void setClientId(@Nullable String clientId) {
		this.clientId = clientId;
	}

	@Nullable
	public String getClientId() {
		return this.clientId;
	}

	public void setMessageSelector(@Nullable String messageSelector) {
		this.messageSelector = messageSelector;
	}

	@Nullable
	public String getMessageSelector() {
		return this.messageSelector;
	}

	/**
	 * 通过 JMS {@link Session} 接口中相应常量的名称设置 JMS 确认模式，
	 * 例如 "CLIENT_ACKNOWLEDGE"。
	 * <p>请注意，JCA 资源适配器通常只支持 auto 和 dups-ok
	 * （参见 Spring 的 {@link StandardJmsActivationSpecFactory}）。ActiveMQ 还
	 * 以 RA 管理事务的形式支持 "SESSION_TRANSACTED"
	 * （由 Spring 的 {@link DefaultJmsActivationSpecFactory} 自动转换）。
	 * @param constantName the name of the {@link Session} acknowledge mode constant
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Session#SESSION_TRANSACTED
	 * @see StandardJmsActivationSpecFactory
	 * @see DefaultJmsActivationSpecFactory
	 */
	public void setAcknowledgeModeName(String constantName) {
		setAcknowledgeMode(sessionConstants.asNumber(constantName).intValue());
	}

	/**
	 * 设置要使用的 JMS 确认模式。
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Session#SESSION_TRANSACTED
	 */
	public void setAcknowledgeMode(int acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	/**
	 * 返回要使用的 JMS 确认模式。
	 */
	public int getAcknowledgeMode() {
		return this.acknowledgeMode;
	}

	/**
	 * 通过 "lower-upper" 形式的字符串（例如 "5-10"）或简单的上限字符串
	 * （例如 "10"）指定并发限制。
	 * <p>JCA 监听器容器始终从零扩展到给定的上限。
	 * 指定的下限实际上会被忽略。
	 * <p>支持此属性主要是为了与
	 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer} 的配置兼容。
	 * 对于此激活配置，通常请改用 {@link #setMaxConcurrency}。
	 */
	public void setConcurrency(String concurrency) {
		try {
			int separatorIndex = concurrency.indexOf('-');
			if (separatorIndex != -1) {
				setMaxConcurrency(Integer.parseInt(concurrency.substring(separatorIndex + 1)));
			}
			else {
				setMaxConcurrency(Integer.parseInt(concurrency));
			}
		}
		catch (NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid concurrency value [" + concurrency + "]: only " +
					"single maximum integer (e.g. \"5\") and minimum-maximum combo (e.g. \"3-5\") supported. " +
					"Note that JmsActivationSpecConfig will effectively ignore the minimum value and " +
					"scale from zero up to the number of consumers according to the maximum value.");
		}
	}

	/**
	 * 指定要使用的最大消费者/会话数量，从而有效控制
	 * 对目标监听器的并发调用数。
	 */
	public void setMaxConcurrency(int maxConcurrency) {
		this.maxConcurrency = maxConcurrency;
	}

	/**
	 * 返回要使用的最大消费者/会话数量。
	 */
	public int getMaxConcurrency() {
		return this.maxConcurrency;
	}

	/**
	 * 指定要加载到单个会话中的最大消息数
	 * （一种批处理大小）。
	 */
	public void setPrefetchSize(int prefetchSize) {
		this.prefetchSize = prefetchSize;
	}

	/**
	 * 返回要加载到单个会话中的最大消息数。
	 */
	public int getPrefetchSize() {
		return this.prefetchSize;
	}

	/**
	 * 设置用于转换 JMS 消息的 {@link MessageConverter} 策略。
	 * @param messageConverter the message converter to use
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * 返回要使用的 {@link MessageConverter}（如果有）。
	 */
	@Nullable
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

}
