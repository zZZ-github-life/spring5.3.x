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

package org.springframework.remoting.httpinvoker;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于 HTTP invoker 代理的 {@link FactoryBean}。使用指定的服务接口，
 * 将所代理的服务公开为 bean 引用。
 *
 * <p>服务 URL 必须是指向 HTTP invoker 服务的 HTTP URL。
 * 此外，还可以指定 codebase URL，以便从远程位置按需动态下载类。
 * 有关详细信息，请参阅 HttpInvokerClientInterceptor 的文档。
 *
 * <p>序列化远程调用对象，并反序列化远程调用结果对象。
 * 与 RMI 一样使用 Java 序列化，但提供了与 Caucho 基于 HTTP 的
 * Hessian 协议同样简单的配置方式。
 *
 * <p><b>HTTP invoker 是 Java 到 Java 远程调用的推荐协议。</b>
 * 它比 Hessian 更强大、更具扩展性，代价是与 Java 绑定。
 * 不过，它的配置与 Hessian 一样简单，这是它相对于 RMI 的主要优势。
 *
 * <p><b>警告：请注意不安全的 Java 反序列化所带来的漏洞：
 * 被篡改的输入流可能导致在反序列化步骤中于服务器上执行非预期代码。
 * 因此，不要将 HTTP invoker 端点暴露给不受信任的客户端，
 * 而应仅在自己内部的服务之间使用。</b>
 * 一般而言，我们强烈建议改用其他任何消息格式（例如 JSON）。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setServiceInterface
 * @see #setServiceUrl
 * @see #setCodebaseUrl
 * @see HttpInvokerClientInterceptor
 * @see HttpInvokerServiceExporter
 * @see org.springframework.remoting.rmi.RmiProxyFactoryBean
 * @see org.springframework.remoting.caucho.HessianProxyFactoryBean
 * @deprecated as of 5.3 (phasing out serialization-based remoting)
 */
@Deprecated
public class HttpInvokerProxyFactoryBean extends HttpInvokerClientInterceptor implements FactoryBean<Object> {

	@Nullable
	private Object serviceProxy;


	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		Class<?> ifc = getServiceInterface();
		Assert.notNull(ifc, "Property 'serviceInterface' is required");
		this.serviceProxy = new ProxyFactory(ifc, this).getProxy(getBeanClassLoader());
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
