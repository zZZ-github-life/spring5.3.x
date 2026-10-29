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

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executor;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.HandlerResolver;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于本地定义的 JAX-WS {@link javax.xml.ws.Service} 引用的工厂。
 * 底层使用 JAX-WS {@link javax.xml.ws.Service#create} 工厂 API。
 *
 * <p>作为 {@link LocalJaxWsServiceFactoryBean} 以及
 * {@link JaxWsPortClientInterceptor} 和 {@link JaxWsPortProxyFactoryBean} 的基类。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see javax.xml.ws.Service
 * @see LocalJaxWsServiceFactoryBean
 * @see JaxWsPortClientInterceptor
 * @see JaxWsPortProxyFactoryBean
 */
public class LocalJaxWsServiceFactory {

	@Nullable
	private URL wsdlDocumentUrl;

	@Nullable
	private String namespaceUri;

	@Nullable
	private String serviceName;

	@Nullable
	private WebServiceFeature[] serviceFeatures;

	@Nullable
	private Executor executor;

	@Nullable
	private HandlerResolver handlerResolver;


	/**
	 * 设置描述该服务的 WSDL 文档的 URL。
	 * @see #setWsdlDocumentResource(Resource)
	 */
	public void setWsdlDocumentUrl(@Nullable URL wsdlDocumentUrl) {
		this.wsdlDocumentUrl = wsdlDocumentUrl;
	}

	/**
	 * 以 {@link Resource} 形式设置 WSDL 文档 URL。
	 * @since 3.2
	 */
	public void setWsdlDocumentResource(Resource wsdlDocumentResource) throws IOException {
		Assert.notNull(wsdlDocumentResource, "WSDL Resource must not be null");
		this.wsdlDocumentUrl = wsdlDocumentResource.getURL();
	}

	/**
	 * 返回描述该服务的 WSDL 文档的 URL。
	 */
	@Nullable
	public URL getWsdlDocumentUrl() {
		return this.wsdlDocumentUrl;
	}

	/**
	 * 设置服务的命名空间 URI。
	 * 对应于 WSDL 的 "targetNamespace"。
	 */
	public void setNamespaceUri(@Nullable String namespaceUri) {
		this.namespaceUri = (namespaceUri != null ? namespaceUri.trim() : null);
	}

	/**
	 * 返回服务的命名空间 URI。
	 */
	@Nullable
	public String getNamespaceUri() {
		return this.namespaceUri;
	}

	/**
	 * 设置要查找的服务的名称。
	 * 对应于 "wsdl:service" 名称。
	 */
	public void setServiceName(@Nullable String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * 返回服务的名称。
	 */
	@Nullable
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * 指定要应用于 JAX-WS 服务创建的 WebServiceFeature 对象
	 * （例如作为内部 bean 定义）。
	 * @since 4.0
	 * @see Service#create(QName, WebServiceFeature...)
	 */
	public void setServiceFeatures(WebServiceFeature... serviceFeatures) {
		this.serviceFeatures = serviceFeatures;
	}

	/**
	 * 设置用于需要回调的异步执行的
	 * JDK 并发执行器。
	 * @see javax.xml.ws.Service#setExecutor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * 设置用于通过此工厂创建的所有代理和分派器的
	 * JAX-WS HandlerResolver。
	 * @see javax.xml.ws.Service#setHandlerResolver
	 */
	public void setHandlerResolver(HandlerResolver handlerResolver) {
		this.handlerResolver = handlerResolver;
	}


	/**
	 * 根据此工厂的参数创建 JAX-WS Service。
	 * @see #setServiceName
	 * @see #setWsdlDocumentUrl
	 */
	public Service createJaxWsService() {
		Assert.notNull(this.serviceName, "No service name specified");
		Service service;

		if (this.serviceFeatures != null) {
			service = (this.wsdlDocumentUrl != null ?
				Service.create(this.wsdlDocumentUrl, getQName(this.serviceName), this.serviceFeatures) :
				Service.create(getQName(this.serviceName), this.serviceFeatures));
		}
		else {
			service = (this.wsdlDocumentUrl != null ?
					Service.create(this.wsdlDocumentUrl, getQName(this.serviceName)) :
					Service.create(getQName(this.serviceName)));
		}

		if (this.executor != null) {
			service.setExecutor(this.executor);
		}
		if (this.handlerResolver != null) {
			service.setHandlerResolver(this.handlerResolver);
		}

		return service;
	}

	/**
	 * 为给定名称返回一个 QName，如果已指定，
	 * 则相对于此工厂的命名空间 URI。
	 * @see #setNamespaceUri
	 */
	protected QName getQName(String name) {
		return (getNamespaceUri() != null ? new QName(getNamespaceUri(), name) : new QName(name));
	}

}
