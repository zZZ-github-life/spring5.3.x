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

package org.springframework.jms.remoting;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * JMS 调用器代理的 FactoryBean。使用指定的服务接口暴露
 * 被代理的服务，以便作为 Bean 引用使用。
 *
 * <p>序列化远程调用对象并反序列化远程调用结果对象。
 * 与 RMI 一样使用 Java 序列化，但以 JMS 提供者为通信基础设施。
 *
 * <p>需使用 {@link javax.jms.QueueConnectionFactory} 和目标队列
 * （可以是 {@link javax.jms.Queue} 引用或队列名称）进行配置。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setConnectionFactory
 * @see #setQueueName
 * @see #setServiceInterface
 * @see org.springframework.jms.remoting.JmsInvokerClientInterceptor
 * @see org.springframework.jms.remoting.JmsInvokerServiceExporter
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class JmsInvokerProxyFactoryBean extends JmsInvokerClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware {

	@Nullable
	private Class<?> serviceInterface;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private Object serviceProxy;


	/**
	 * 设置代理必须实现的接口。
	 * @param serviceInterface the interface that the proxy must implement
	 * @throws IllegalArgumentException if the supplied {@code serviceInterface}
	 * is not an interface type
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		Assert.notNull(serviceInterface, "'serviceInterface' must not be null");
		Assert.isTrue(serviceInterface.isInterface(), "'serviceInterface' must be an interface");
		this.serviceInterface = serviceInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Assert.notNull(this.serviceInterface, "Property 'serviceInterface' is required");
		this.serviceProxy = new ProxyFactory(this.serviceInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
