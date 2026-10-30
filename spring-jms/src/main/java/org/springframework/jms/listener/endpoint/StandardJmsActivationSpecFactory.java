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

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jms.support.destination.DestinationResolutionException;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;

/**
 * {@link JmsActivationSpecFactory} 接口的标准实现。
 * 支持 JMS 1.5 规范（附录 B）定义的标准 JMS 属性；
 * 忽略 Spring 的 "maxConcurrency" 和 "prefetchSize" 设置。
 *
 * <p>'activationSpecClass' 属性是必需的，显式定义提供者
 * ActivationSpec 类的完全限定类名
 * （例如 "org.apache.activemq.ra.ActiveMQActivationSpec"）。
 *
 * <p>请查看 {@link DefaultJmsActivationSpecFactory}，了解此类的扩展变体，
 * 它支持普通 JMS 1.5 规范之外的一些额外默认约定。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setActivationSpecClass
 * @see DefaultJmsActivationSpecFactory
 */
public class StandardJmsActivationSpecFactory implements JmsActivationSpecFactory {

	@Nullable
	private Class<?> activationSpecClass;

	@Nullable
	private Map<String, String> defaultProperties;

	@Nullable
	private DestinationResolver destinationResolver;


	/**
	 * 为目标提供者指定完全限定的 ActivationSpec 类名
	 * （例如 "org.apache.activemq.ra.ActiveMQActivationSpec"）。
	 */
	public void setActivationSpecClass(Class<?> activationSpecClass) {
		this.activationSpecClass = activationSpecClass;
	}

	/**
	 * 指定自定义的默认属性，键和值均为 String。
	 * <p>在填充监听器特定设置之前，应用于每个 ActivationSpec 对象。
	 * 允许配置 {@link JmsActivationSpecConfig} 中 Spring 定义设置之外的
	 * 特定于厂商的属性。
	 */
	public void setDefaultProperties(Map<String, String> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}

	/**
	 * 设置用于将目的地名称解析到 JCA 1.5 ActivationSpec
	 * "destination" 属性的 DestinationResolver。
	 * <p>如果未指定，目的地名称将直接作为字符串传入。
	 * 如果指定，目的地名称会先被解析为 Destination 对象。
	 * <p>请注意，与此工厂一起使用的 DestinationResolver 必须能够在<i>没有</i>
	 * 活动 JMS Session 的情况下工作：例如
	 * {@link org.springframework.jms.support.destination.JndiDestinationResolver}
	 * 或 {@link org.springframework.jms.support.destination.BeanFactoryDestinationResolver}，
	 * 但不能是 {@link org.springframework.jms.support.destination.DynamicDestinationResolver}。
	 */
	public void setDestinationResolver(@Nullable DestinationResolver destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * 返回用于解析目的地名称的 {@link DestinationResolver}。
	 */
	@Nullable
	public DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}


	@Override
	public ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config) {
		Class<?> activationSpecClassToUse = this.activationSpecClass;
		if (activationSpecClassToUse == null) {
			activationSpecClassToUse = determineActivationSpecClass(adapter);
			if (activationSpecClassToUse == null) {
				throw new IllegalStateException("Property 'activationSpecClass' is required");
			}
		}

		ActivationSpec spec = (ActivationSpec) BeanUtils.instantiateClass(activationSpecClassToUse);
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(spec);
		if (this.defaultProperties != null) {
			bw.setPropertyValues(this.defaultProperties);
		}
		populateActivationSpecProperties(bw, config);
		return spec;
	}

	/**
	 * 如果可能，为给定的 ResourceAdapter 确定 ActivationSpec 类。
	 * 在未显式设置 'activationSpecClass' 时调用。
	 * @param adapter 要检查的 ResourceAdapter
	 * @return 相应的 ActivationSpec 类；如果无法确定，则为 {@code null}
	 * @see #setActivationSpecClass
	 */
	@Nullable
	protected Class<?> determineActivationSpecClass(ResourceAdapter adapter) {
		return null;
	}

	/**
	 * 使用给定配置对象中定义的设置填充给定的 ApplicationSpec 对象。
	 * <p>此实现应用所有标准 JMS 设置，但忽略
	 * "maxConcurrency" 和 "prefetchSize"——标准 JCA 1.5 不支持它们。
	 * @param bw 包装 ActivationSpec 对象的 BeanWrapper
	 * @param config 保存通用 JMS 设置的已配置对象
	 */
	protected void populateActivationSpecProperties(BeanWrapper bw, JmsActivationSpecConfig config) {
		String destinationName = config.getDestinationName();
		if (destinationName != null) {
			boolean pubSubDomain = config.isPubSubDomain();
			Object destination = destinationName;
			if (this.destinationResolver != null) {
				try {
					destination = this.destinationResolver.resolveDestinationName(null, destinationName, pubSubDomain);
				}
				catch (JMSException ex) {
					throw new DestinationResolutionException(
							"Cannot resolve destination name [" + destinationName + "]", ex);
				}
			}
			bw.setPropertyValue("destination", destination);
			bw.setPropertyValue("destinationType", pubSubDomain ? Topic.class.getName() : Queue.class.getName());
		}

		if (bw.isWritableProperty("subscriptionDurability")) {
			bw.setPropertyValue("subscriptionDurability", config.isSubscriptionDurable() ? "Durable" : "NonDurable");
		}
		else if (config.isSubscriptionDurable()) {
			// Standard JCA 1.5 "subscriptionDurability" apparently not supported...
			throw new IllegalArgumentException("Durable subscriptions not supported by underlying provider");
		}
		if (config.isSubscriptionShared()) {
			throw new IllegalArgumentException("Shared subscriptions not supported for JCA-driven endpoints");
		}

		if (config.getSubscriptionName() != null) {
			bw.setPropertyValue("subscriptionName", config.getSubscriptionName());
		}
		if (config.getClientId() != null) {
			bw.setPropertyValue("clientId", config.getClientId());
		}
		if (config.getMessageSelector() != null) {
			bw.setPropertyValue("messageSelector", config.getMessageSelector());
		}
		applyAcknowledgeMode(bw, config.getAcknowledgeMode());
	}

	/**
	 * 将指定的确认模式应用到 ActivationSpec 对象。
	 * <p>此实现应用标准 JCA 1.5 确认模式
	 * "Auto-acknowledge" 和 "Dups-ok-acknowledge"。
	 * 如果请求了 {@code CLIENT_ACKNOWLEDGE} 或 {@code SESSION_TRANSACTED}，
	 * 则抛出异常。
	 * @param bw 包装 ActivationSpec 对象的 BeanWrapper
	 * @param ackMode 配置的确认模式（依据 {@link javax.jms.Session} 中的常量）
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE
	 * @see javax.jms.Session#DUPS_OK_ACKNOWLEDGE
	 * @see javax.jms.Session#CLIENT_ACKNOWLEDGE
	 * @see javax.jms.Session#SESSION_TRANSACTED
	 */
	protected void applyAcknowledgeMode(BeanWrapper bw, int ackMode) {
		if (ackMode == Session.SESSION_TRANSACTED) {
			throw new IllegalArgumentException("No support for SESSION_TRANSACTED: Only \"Auto-acknowledge\" " +
					"and \"Dups-ok-acknowledge\" supported in standard JCA 1.5");
		}
		else if (ackMode == Session.CLIENT_ACKNOWLEDGE) {
			throw new IllegalArgumentException("No support for CLIENT_ACKNOWLEDGE: Only \"Auto-acknowledge\" " +
					"and \"Dups-ok-acknowledge\" supported in standard JCA 1.5");
		}
		else if (bw.isWritableProperty("acknowledgeMode")) {
			bw.setPropertyValue("acknowledgeMode",
					ackMode == Session.DUPS_OK_ACKNOWLEDGE ? "Dups-ok-acknowledge" : "Auto-acknowledge");
		}
		else if (ackMode == Session.DUPS_OK_ACKNOWLEDGE) {
			// Standard JCA 1.5 "acknowledgeMode" apparently not supported (e.g. WebSphere MQ 6.0.2.1)
			throw new IllegalArgumentException("Dups-ok-acknowledge not supported by underlying provider");
		}
	}

}
