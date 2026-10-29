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

package org.springframework.remoting.caucho;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * 用于 Hessian 代理的 {@link FactoryBean}。使用指定的服务接口，
 * 将所代理的服务公开为 bean 引用。
 *
 * <p>Hessian 是一种轻量级二进制 RPC 协议。
 * 有关 Hessian 的信息，请参阅
 * <a href="http://hessian.caucho.com">Hessian 网站</a>
 * <b>注意：从 Spring 4.0 起，此代理工厂要求 Hessian 4.0 或更高版本。</b>
 *
 * <p>服务 URL 必须是指向 Hessian 服务的 HTTP URL。
 * 有关详细信息，请参阅 {@link HessianClientInterceptor} 的 javadoc。
 *
 * @author Juergen Hoeller
 * @since 13.05.2003
 * @see #setServiceInterface
 * @see #setServiceUrl
 * @see HessianClientInterceptor
 * @see HessianServiceExporter
 * @see org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean
 * @see org.springframework.remoting.rmi.RmiProxyFactoryBean
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HessianProxyFactoryBean extends HessianClientInterceptor implements FactoryBean<Object> {

	@Nullable
	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		this.serviceProxy = new ProxyFactory(getServiceInterface(), this).getProxy(getBeanClassLoader());
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
