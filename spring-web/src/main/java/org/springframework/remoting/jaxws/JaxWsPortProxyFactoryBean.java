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

package org.springframework.remoting.jaxws;

import javax.xml.ws.BindingProvider;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于 JAX-WS 服务特定端口的 {@link org.springframework.beans.factory.FactoryBean}。
 * 公开该端口的代理，以用于 bean 引用。
 * 从 {@link JaxWsPortClientInterceptor} 继承配置属性。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setServiceInterface
 * @see LocalJaxWsServiceFactoryBean
 */
public class JaxWsPortProxyFactoryBean extends JaxWsPortClientInterceptor implements FactoryBean<Object> {

	@Nullable
	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		Class<?> ifc = getServiceInterface();
		Assert.notNull(ifc, "Property 'serviceInterface' is required");

		// Build a proxy that also exposes the JAX-WS BindingProvider interface.
		ProxyFactory pf = new ProxyFactory();
		pf.addInterface(ifc);
		pf.addInterface(BindingProvider.class);
		pf.addAdvice(this);
		this.serviceProxy = pf.getProxy(getBeanClassLoader());
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
