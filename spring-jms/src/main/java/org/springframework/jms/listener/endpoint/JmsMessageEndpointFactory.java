/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;

import org.springframework.jca.endpoint.AbstractMessageEndpointFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JCA 1.7 {@link javax.resource.spi.endpoint.MessageEndpointFactory} 接口的
 * JMS 特定实现，为 JMS 监听器对象（例如 {@link javax.jms.MessageListener} 对象）
 * 提供事务管理能力。
 *
 * <p>使用静态端点实现，只是包装指定的消息监听器对象，
 * 并在端点实例上暴露其实现的所有接口。
 *
 * <p>通常与 Spring 的 {@link JmsMessageEndpointManager} 一起使用，
 * 但并非必须绑定于它。因此，此端点工厂也可以用于
 * 原生 {@link javax.resource.spi.ResourceAdapter} 实例上的编程式端点管理。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.5
 * @see #setMessageListener
 * @see #setTransactionManager
 * @see JmsMessageEndpointManager
 */
public class JmsMessageEndpointFactory extends AbstractMessageEndpointFactory  {

	@Nullable
	private MessageListener messageListener;


	/**
	 * 设置此端点的 JMS MessageListener。
	 */
	public void setMessageListener(MessageListener messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * 返回此端点的 JMS MessageListener。
	 */
	protected MessageListener getMessageListener() {
		Assert.state(this.messageListener != null, "No MessageListener set");
		return this.messageListener;
	}

	/**
	 * 创建一个具体的 JMS 消息端点，为此工厂内部使用。
	 */
	@Override
	protected AbstractMessageEndpoint createEndpointInternal() throws UnavailableException {
		return new JmsMessageEndpoint();
	}


	/**
	 * 实现具体 JMS 消息端点的私有内部类。
	 */
	private class JmsMessageEndpoint extends AbstractMessageEndpoint implements MessageListener {

		@Override
		public void onMessage(Message message) {
			Throwable endpointEx = null;
			boolean applyDeliveryCalls = !hasBeforeDeliveryBeenCalled();
			if (applyDeliveryCalls) {
				try {
					beforeDelivery(null);
				}
				catch (ResourceException ex) {
					throw new JmsResourceException(ex);
				}
			}
			try {
				getMessageListener().onMessage(message);
			}
			catch (RuntimeException | Error ex) {
				endpointEx = ex;
				onEndpointException(ex);
				throw ex;
			}
			finally {
				if (applyDeliveryCalls) {
					try {
						afterDelivery();
					}
					catch (ResourceException ex) {
						if (endpointEx == null) {
							throw new JmsResourceException(ex);
						}
					}
				}
			}
		}

		@Override
		protected ClassLoader getEndpointClassLoader() {
			return getMessageListener().getClass().getClassLoader();
		}
	}


	/**
	 * 端点调用期间遇到 ResourceException 时抛出的内部异常。
	 * <p>仅当 ResourceAdapter 不直接调用端点的
	 * {@code beforeDelivery} 和 {@code afterDelivery} 时使用，
	 * 此时由具体端点负责应用这些方法，
	 * 并处理它们抛出的任何 ResourceException。
	 */
	@SuppressWarnings("serial")
	public static class JmsResourceException extends RuntimeException {

		public JmsResourceException(ResourceException cause) {
			super(cause);
		}
	}

}
