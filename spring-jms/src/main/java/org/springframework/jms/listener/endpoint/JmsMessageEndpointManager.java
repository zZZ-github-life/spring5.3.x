/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.jms.MessageListener;
import javax.resource.ResourceException;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.jca.endpoint.GenericMessageEndpointManager;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;

/**
 * 通用 JCA 1.5
 * {@link org.springframework.jca.endpoint.GenericMessageEndpointManager} 的扩展，
 * 增加了对 ActivationSpec 配置的 JMS 特定支持。
 *
 * <p>允许定义一个通用的 {@link JmsActivationSpecConfig} 对象，
 * 该对象会被转换为特定于提供者的 JCA 1.5 ActivationSpec 对象，
 * 用于激活端点。
 *
 * <p><b>注意：</b>这个基于 JCA 的端点管理器仅支持标准 JMS
 * {@link javax.jms.MessageListener} 端点。它<i>不</i>支持
 * Spring 的 {@link org.springframework.jms.listener.SessionAwareMessageListener}
 * 变体，原因很简单：JCA 端点管理契约不允许获取当前的 JMS
 * {@link javax.jms.Session}。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 * @see javax.jms.MessageListener
 * @see #setActivationSpecConfig
 * @see JmsActivationSpecConfig
 * @see JmsActivationSpecFactory
 * @see JmsMessageEndpointFactory
 */
public class JmsMessageEndpointManager extends GenericMessageEndpointManager
		implements BeanNameAware, MessageListenerContainer {

	private final JmsMessageEndpointFactory endpointFactory = new JmsMessageEndpointFactory();

	private boolean messageListenerSet = false;

	private JmsActivationSpecFactory activationSpecFactory = new DefaultJmsActivationSpecFactory();

	@Nullable
	private JmsActivationSpecConfig activationSpecConfig;


	/**
	 * 设置此端点的 JMS MessageListener。
	 * <p>这是配置专用 JmsMessageEndpointFactory 的快捷方式。
	 * @see JmsMessageEndpointFactory#setMessageListener
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.endpointFactory.setMessageListener(messageListener);
		this.messageListenerSet = true;
	}

	/**
	 * 返回此端点的 JMS MessageListener。
	 */
	public MessageListener getMessageListener() {
		return this.endpointFactory.getMessageListener();
	}

	/**
	 * 设置用于包装端点调用的 XA 事务管理器，
	 * 在每次此类事务中登记端点资源。
	 * <p>传入的对象可以是实现了 Spring 的
	 * {@link org.springframework.transaction.jta.TransactionFactory} 接口的事务管理器，
	 * 也可以是普通的 {@link javax.transaction.TransactionManager}。
	 * <p>如果未指定事务管理器，端点调用将不会被包装在 XA 事务中。
	 * 有关特定提供者的本地事务选项，请查阅资源提供者的
	 * ActivationSpec 文档。
	 * <p>这是配置专用 JmsMessageEndpointFactory 的快捷方式。
	 * @see JmsMessageEndpointFactory#setTransactionManager
	 */
	public void setTransactionManager(Object transactionManager) {
		this.endpointFactory.setTransactionManager(transactionManager);
	}

	/**
	 * 设置用于创建具体 JCA 1.5 ActivationSpec 对象的工厂，
	 * 基于 {@link #setActivationSpecConfig JmsActivationSpecConfig} 对象创建 JCA ActivationSpec。
	 * <p>此工厂依赖于具体的 JMS 提供者，例如 ActiveMQ。
	 * 默认实现只是根据提供者的类名猜测 ActivationSpec 类名
	 * （例如同一包中的 "ActiveMQResourceAdapter" &rarr;
	 * "ActiveMQActivationSpec"），并按照 JCA 1.5 规范的建议
	 * 填充 ActivationSpec 属性（外加几个自动检测到的特定于厂商的属性）。
	 * @see DefaultJmsActivationSpecFactory
	 */
	public void setActivationSpecFactory(@Nullable JmsActivationSpecFactory activationSpecFactory) {
		this.activationSpecFactory =
				(activationSpecFactory != null ? activationSpecFactory : new DefaultJmsActivationSpecFactory());
	}

	/**
	 * 设置用于将目的地名称解析到 JCA 1.5 ActivationSpec
	 * "destination" 属性的 DestinationResolver。
	 * <p>如果未指定，目的地名称将直接作为字符串传入。
	 * 如果指定，目的地名称会先被解析为 Destination 对象。
	 * <p>请注意，DestinationResolver 通常是在 JmsActivationSpecFactory 上指定的
	 * （参见 {@link StandardJmsActivationSpecFactory#setDestinationResolver}）。
	 * 这里只是为默认 JmsActivationSpecFactory 提供参数的快捷方式；
	 * 它会替换之前可能设置的任何自定义 JmsActivationSpecFactory。
	 * @see StandardJmsActivationSpecFactory#setDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		DefaultJmsActivationSpecFactory factory = new DefaultJmsActivationSpecFactory();
		factory.setDestinationResolver(destinationResolver);
		this.activationSpecFactory = factory;
	}

	/**
	 * 指定此端点管理器应用于激活其监听器的
	 * {@link JmsActivationSpecConfig} 对象。
	 * <p>此配置对象将通过 {@link #setActivationSpecFactory JmsActivationSpecFactory}
	 * 转换为具体的 JCA 1.5 ActivationSpec 对象。
	 */
	public void setActivationSpecConfig(@Nullable JmsActivationSpecConfig activationSpecConfig) {
		this.activationSpecConfig = activationSpecConfig;
	}

	/**
	 * 返回此端点管理器应用于激活其监听器的
	 * {@link JmsActivationSpecConfig} 对象。如果未设置，则返回 {@code null}。
	 */
	@Nullable
	public JmsActivationSpecConfig getActivationSpecConfig() {
		return this.activationSpecConfig;
	}

	/**
	 * 设置此消息端点的名称。当在 Spring 的 Bean 工厂中定义时，
	 * 会自动填充为 Bean 名称。
	 */
	@Override
	public void setBeanName(String beanName) {
		this.endpointFactory.setBeanName(beanName);
	}


	@Override
	public void afterPropertiesSet() throws ResourceException {
		if (getResourceAdapter() == null) {
			throw new IllegalArgumentException("Property 'resourceAdapter' is required");
		}
		if (this.messageListenerSet) {
			setMessageEndpointFactory(this.endpointFactory);
		}
		if (this.activationSpecConfig != null) {
			setActivationSpec(
					this.activationSpecFactory.createActivationSpec(getResourceAdapter(), this.activationSpecConfig));
		}

		super.afterPropertiesSet();
	}


	@Override
	public void setupMessageListener(Object messageListener) {
		if (messageListener instanceof MessageListener) {
			setMessageListener((MessageListener) messageListener);
		}
		else {
			throw new IllegalArgumentException("Unsupported message listener '" +
					messageListener.getClass().getName() + "': only '" + MessageListener.class.getName() +
					"' type is supported");
		}
	}

	@Override
	@Nullable
	public MessageConverter getMessageConverter() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.getMessageConverter();
		}
		return null;
	}

	@Override
	@Nullable
	public DestinationResolver getDestinationResolver() {
		if (this.activationSpecFactory instanceof StandardJmsActivationSpecFactory) {
			return ((StandardJmsActivationSpecFactory) this.activationSpecFactory).getDestinationResolver();
		}
		return null;
	}

	@Override
	public boolean isPubSubDomain() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.isPubSubDomain();
		}
		throw new IllegalStateException("Could not determine pubSubDomain - no activation spec config is set");
	}

	@Override
	public boolean isReplyPubSubDomain() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.isReplyPubSubDomain();
		}
		throw new IllegalStateException("Could not determine reply pubSubDomain - no activation spec config is set");
	}

	@Override
	@Nullable
	public QosSettings getReplyQosSettings() {
		JmsActivationSpecConfig config = getActivationSpecConfig();
		if (config != null) {
			return config.getReplyQosSettings();
		}
		throw new IllegalStateException("Could not determine reply qosSettings - no activation spec config is set");
	}
}
