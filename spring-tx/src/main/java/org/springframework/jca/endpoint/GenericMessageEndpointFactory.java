/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jca.endpoint;

import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * JCA 1.7 {@link javax.resource.spi.endpoint.MessageEndpointFactory} 接口的通用实现，
 * 为任何类型的消息监听器对象（例如 {@link javax.jms.MessageListener} 对象或
 * {@link javax.resource.cci.MessageListener} 对象）提供事务管理能力。
 *
 * <p>使用 AOP 代理包装具体的端点实例，简单地包装指定的消息监听器对象，
 * 并在端点实例上暴露其所有实现的接口。
 *
 * <p>通常与 Spring 的 {@link GenericMessageEndpointManager} 一起使用，
 * 但不绑定于此。因此，此端点工厂也可以与原生
 * {@link javax.resource.spi.ResourceAdapter} 实例上的编程式端点管理一起使用。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setMessageListener
 * @see #setTransactionManager
 * @see GenericMessageEndpointManager
 */
public class GenericMessageEndpointFactory extends AbstractMessageEndpointFactory {

	@Nullable
	private Object messageListener;


	/**
	 * 指定端点应暴露的消息监听器对象
	 * （例如 {@link javax.jms.MessageListener} 对象或
	 * {@link javax.resource.cci.MessageListener} 实现）。
	 */
	public void setMessageListener(Object messageListener) {
		this.messageListener = messageListener;
	}

	/**
	 * 返回此端点的消息监听器对象。
	 * @since 5.0
	 */
	protected Object getMessageListener() {
		Assert.state(this.messageListener != null, "No message listener set");
		return this.messageListener;
	}

	/**
	 * 使用 AOP 代理包装每个具体端点实例，
	 * 通过 AOP 引入暴露消息监听器的接口以及端点 SPI。
	 */
	@Override
	public MessageEndpoint createEndpoint(XAResource xaResource) throws UnavailableException {
		GenericMessageEndpoint endpoint = (GenericMessageEndpoint) super.createEndpoint(xaResource);
		Object target = getMessageListener();
		ProxyFactory proxyFactory = new ProxyFactory(target);
		DelegatingIntroductionInterceptor introduction = new DelegatingIntroductionInterceptor(endpoint);
		introduction.suppressInterface(MethodInterceptor.class);
		proxyFactory.addAdvice(introduction);
		return (MessageEndpoint) proxyFactory.getProxy(target.getClass().getClassLoader());
	}

	/**
	 * 创建此工厂内部的具体通用消息端点。
	 */
	@Override
	protected AbstractMessageEndpoint createEndpointInternal() throws UnavailableException {
		return new GenericMessageEndpoint();
	}


	/**
	 * 实现具体通用消息端点的私有内部类，
	 * 作为将由代理调用的 AOP Alliance MethodInterceptor。
	 */
	private class GenericMessageEndpoint extends AbstractMessageEndpoint implements MethodInterceptor {

		@Override
		@Nullable
		public Object invoke(MethodInvocation methodInvocation) throws Throwable {
			Throwable endpointEx = null;
			boolean applyDeliveryCalls = !hasBeforeDeliveryBeenCalled();
			if (applyDeliveryCalls) {
				try {
					beforeDelivery(null);
				}
				catch (ResourceException ex) {
					throw adaptExceptionIfNecessary(methodInvocation, ex);
				}
			}
			try {
				return methodInvocation.proceed();
			}
			catch (Throwable ex) {
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
							throw adaptExceptionIfNecessary(methodInvocation, ex);
						}
					}
				}
			}
		}

		private Exception adaptExceptionIfNecessary(MethodInvocation methodInvocation, ResourceException ex) {
			if (ReflectionUtils.declaresException(methodInvocation.getMethod(), ex.getClass())) {
				return ex;
			}
			else {
				return new InternalResourceException(ex);
			}
		}

		@Override
		protected ClassLoader getEndpointClassLoader() {
			return getMessageListener().getClass().getClassLoader();
		}
	}


	/**
	 * 在端点调用期间遇到 ResourceException 时抛出的内部异常。
	 * <p>仅在 ResourceAdapter 不直接调用端点的 {@code beforeDelivery} 和
	 * {@code afterDelivery} 时使用，将应用这些调用以及处理它们抛出的任何
	 * ResourceException 的责任留给具体端点。
	 */
	@SuppressWarnings("serial")
	public static class InternalResourceException extends RuntimeException {

		public InternalResourceException(ResourceException cause) {
			super(cause);
		}
	}

}
