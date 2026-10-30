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

package org.springframework.remoting.jaxws;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceProvider;

/**
 * JAX-WS 服务的简单导出器，自动检测带注解的服务 bean
 * （通过 JAX-WS 的 {@link javax.jws.WebService} 注解），并使用
 * JAX-WS 提供者内置的发布支持，通过配置的基地址（默认为
 * "http://localhost:8080/"）导出它们。每个服务的完整地址
 * 由基地址加上服务名称组成
 * （例如 "http://localhost:8080/OrderService"）。
 *
 * <p>请注意，此导出器仅在 JAX-WS 运行时确实
 * 支持带地址参数发布时才能工作，即 JAX-WS 运行时
 * 内置了 HTTP 服务器。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.jws.WebService
 * @see javax.xml.ws.Endpoint#publish(String)
 */
public class SimpleJaxWsServiceExporter extends AbstractJaxWsServiceExporter {

	/**
	 * 默认基地址。
	 */
	public static final String DEFAULT_BASE_ADDRESS = "http://localhost:8080/";

	private String baseAddress = DEFAULT_BASE_ADDRESS;


	/**
	 * 设置所导出服务的基地址。
	 * 默认为 "http://localhost:8080/"。
	 * <p>对于每个实际发布地址，服务名称将
	 * 追加到此基地址之后。例如服务名称 "OrderService"
	 * &rarr; "http://localhost:8080/OrderService"。
	 * @see javax.xml.ws.Endpoint#publish(String)
	 * @see javax.jws.WebService#serviceName()
	 */
	public void setBaseAddress(String baseAddress) {
		this.baseAddress = baseAddress;
	}


	@Override
	protected void publishEndpoint(Endpoint endpoint, WebService annotation) {
		endpoint.publish(calculateEndpointAddress(endpoint, annotation.serviceName()));
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation) {
		endpoint.publish(calculateEndpointAddress(endpoint, annotation.serviceName()));
	}

	/**
	 * 计算给定端点的完整端点地址。
	 * @param endpoint JAX-WS 的 Provider Endpoint 对象
	 * @param serviceName 给定的服务名称
	 * @return 完整的端点地址
	 */
	protected String calculateEndpointAddress(Endpoint endpoint, String serviceName) {
		String fullAddress = this.baseAddress + serviceName;
		if (endpoint.getClass().getName().startsWith("weblogic.")) {
			// Workaround for WebLogic 10.3
			fullAddress = fullAddress + "/";
		}
		return fullAddress;
	}

}
