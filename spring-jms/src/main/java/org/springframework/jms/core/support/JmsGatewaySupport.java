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

package org.springframework.jms.core.support;

import javax.jms.ConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.lang.Nullable;

/**
 * 供需要 JMS 访问的应用类使用的便捷基类。
 *
 * <p>需要设置 ConnectionFactory 或 JmsTemplate 实例。如果传入的是
 * ConnectionFactory，它将创建自己的 JmsTemplate。可以通过重写
 * {@link #createJmsTemplate} 方法为给定的 ConnectionFactory 创建自定义的
 * JmsTemplate 实例。
 *
 * @author Mark Pollack
 * @since 1.1.1
 * @see #setConnectionFactory
 * @see #setJmsTemplate
 * @see #createJmsTemplate
 * @see org.springframework.jms.core.JmsTemplate
 */
public abstract class JmsGatewaySupport implements InitializingBean {

	/** 供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private JmsTemplate jmsTemplate;


	/**
	 * 设置网关使用的 JMS 连接工厂。
	 * 将自动为给定的 ConnectionFactory 创建 JmsTemplate。
	 * @see #createJmsTemplate
	 * @see #setConnectionFactory(javax.jms.ConnectionFactory)
	 */
	public final void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.jmsTemplate = createJmsTemplate(connectionFactory);
	}

	/**
	 * 为给定的 ConnectionFactory 创建 JmsTemplate。
	 * 仅在为网关填充 ConnectionFactory 引用时调用。
	 * <p>可在子类中重写，以提供具有不同配置的 JmsTemplate 实例。
	 * @param connectionFactory 要为其创建 JmsTemplate 的 JMS ConnectionFactory
	 * @return 新的 JmsTemplate 实例
	 * @see #setConnectionFactory
	 */
	protected JmsTemplate createJmsTemplate(ConnectionFactory connectionFactory) {
		return new JmsTemplate(connectionFactory);
	}

	/**
	 * 返回网关使用的 JMS ConnectionFactory。
	 */
	@Nullable
	public final ConnectionFactory getConnectionFactory() {
		return (this.jmsTemplate != null ? this.jmsTemplate.getConnectionFactory() : null);
	}

	/**
	 * 设置网关使用的 JmsTemplate。
	 * @see #setConnectionFactory(javax.jms.ConnectionFactory)
	 */
	public final void setJmsTemplate(@Nullable JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	/**
	 * 返回网关使用的 JmsTemplate。
	 */
	@Nullable
	public final JmsTemplate getJmsTemplate() {
		return this.jmsTemplate;
	}

	@Override
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
		if (this.jmsTemplate == null) {
			throw new IllegalArgumentException("'connectionFactory' or 'jmsTemplate' is required");
		}
		try {
			initGateway();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Initialization of JMS gateway failed: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 子类可以重写此方法以实现自定义初始化行为。
	 * 在填充此实例的 bean 属性之后调用。
	 * @throws java.lang.Exception 如果初始化失败
	 */
	protected void initGateway() throws Exception {
	}

}
