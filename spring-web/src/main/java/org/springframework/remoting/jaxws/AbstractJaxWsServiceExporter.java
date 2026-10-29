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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.WebServiceProvider;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JAX-WS 服务的抽象导出器，自动检测带注解的服务 bean
 * （通过 JAX-WS 的 {@link javax.jws.WebService} 注解）。
 *
 * <p>子类需要实现 {@link #publishEndpoint} 模板方法
 * 以实际公开端点。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 * @see javax.jws.WebService
 * @see javax.xml.ws.Endpoint
 * @see SimpleJaxWsServiceExporter
 */
public abstract class AbstractJaxWsServiceExporter implements BeanFactoryAware, InitializingBean, DisposableBean {

	@Nullable
	private Map<String, Object> endpointProperties;

	@Nullable
	private Executor executor;

	@Nullable
	private String bindingType;

	@Nullable
	private WebServiceFeature[] endpointFeatures;

	@Nullable
	private ListableBeanFactory beanFactory;

	private final Set<Endpoint> publishedEndpoints = new LinkedHashSet<>();


	/**
	 * 设置端点的属性包，包括 "javax.xml.ws.wsdl.service"
	 * 或 "javax.xml.ws.wsdl.port" 等属性。
	 * @see javax.xml.ws.Endpoint#setProperties
	 * @see javax.xml.ws.Endpoint#WSDL_SERVICE
	 * @see javax.xml.ws.Endpoint#WSDL_PORT
	 */
	public void setEndpointProperties(Map<String, Object> endpointProperties) {
		this.endpointProperties = endpointProperties;
	}

	/**
	 * 设置用于将传入请求分派到已导出服务实例的
	 * JDK 并发执行器。
	 * @see javax.xml.ws.Endpoint#setExecutor
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * 指定要使用的绑定类型，覆盖
	 * JAX-WS {@link javax.xml.ws.BindingType} 注解的值。
	 */
	public void setBindingType(String bindingType) {
		this.bindingType = bindingType;
	}

	/**
	 * 指定要应用于 JAX-WS 端点创建的 WebServiceFeature 对象
	 * （例如作为内部 bean 定义）。
	 * @since 4.0
	 */
	public void setEndpointFeatures(WebServiceFeature... endpointFeatures) {
		this.endpointFeatures = endpointFeatures;
	}

	/**
	 * 获取所有 Web 服务 bean 并将它们发布为 JAX-WS 端点。
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new IllegalStateException(getClass().getSimpleName() + " requires a ListableBeanFactory");
		}
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}


	/**
	 * 在完全配置后立即发布所有端点。
	 * @see #publishEndpoints()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		publishEndpoints();
	}

	/**
	 * 发布所在 BeanFactory 中所有
	 * 标注 {@link javax.jws.WebService} 的 bean。
	 * @see #publishEndpoint
	 */
	public void publishEndpoints() {
		Assert.state(this.beanFactory != null, "No BeanFactory set");

		Set<String> beanNames = new LinkedHashSet<>(this.beanFactory.getBeanDefinitionCount());
		Collections.addAll(beanNames, this.beanFactory.getBeanDefinitionNames());
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			Collections.addAll(beanNames, ((ConfigurableBeanFactory) this.beanFactory).getSingletonNames());
		}

		for (String beanName : beanNames) {
			try {
				Class<?> type = this.beanFactory.getType(beanName);
				if (type != null && !type.isInterface()) {
					WebService wsAnnotation = type.getAnnotation(WebService.class);
					WebServiceProvider wsProviderAnnotation = type.getAnnotation(WebServiceProvider.class);
					if (wsAnnotation != null || wsProviderAnnotation != null) {
						Endpoint endpoint = createEndpoint(this.beanFactory.getBean(beanName));
						if (this.endpointProperties != null) {
							endpoint.setProperties(this.endpointProperties);
						}
						if (this.executor != null) {
							endpoint.setExecutor(this.executor);
						}
						if (wsAnnotation != null) {
							publishEndpoint(endpoint, wsAnnotation);
						}
						else {
							publishEndpoint(endpoint, wsProviderAnnotation);
						}
						this.publishedEndpoints.add(endpoint);
					}
				}
			}
			catch (CannotLoadBeanClassException ex) {
				// ignore beans where the class is not resolvable
			}
		}
	}

	/**
	 * 创建实际的 Endpoint 实例。
	 * @param bean the service object to wrap
	 * @return the Endpoint instance
	 * @see Endpoint#create(Object)
	 * @see Endpoint#create(String, Object)
	 */
	protected Endpoint createEndpoint(Object bean) {
		return (this.endpointFeatures != null ?
				Endpoint.create(this.bindingType, bean, this.endpointFeatures) :
				Endpoint.create(this.bindingType, bean));
	}


	/**
	 * 实际发布给定的端点。由子类实现。
	 * @param endpoint the JAX-WS Endpoint object
	 * @param annotation the service bean's WebService annotation
	 */
	protected abstract void publishEndpoint(Endpoint endpoint, WebService annotation);

	/**
	 * 实际发布给定的提供者端点。由子类实现。
	 * @param endpoint the JAX-WS Provider Endpoint object
	 * @param annotation the service bean's WebServiceProvider annotation
	 */
	protected abstract void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation);


	/**
	 * 停止所有已发布的端点，使这些 Web 服务离线。
	 */
	@Override
	public void destroy() {
		for (Endpoint endpoint : this.publishedEndpoints) {
			endpoint.stop();
		}
	}

}
