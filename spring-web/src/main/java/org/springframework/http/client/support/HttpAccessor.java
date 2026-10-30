/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.client.support;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.web.client.RestTemplate}
 * 和其他 HTTP 访问网关辅助类的基类，定义要操作的
 * {@link ClientHttpRequestFactory} 等公共属性。
 *
 * <p>不打算直接使用。
 *
 * <p>有关入口点，请参阅 {@link org.springframework.web.client.RestTemplate}。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 * @see ClientHttpRequestFactory
 * @see org.springframework.web.client.RestTemplate
 */
public abstract class HttpAccessor {

	/** 供子类使用的 Logger。 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	private ClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

	private final List<ClientHttpRequestInitializer> clientHttpRequestInitializers = new ArrayList<>();


	/**
	 * 设置此访问器用于获取客户端请求句柄的请求工厂。
	 * <p>默认是基于 JDK 自带 HTTP 库（{@link java.net.HttpURLConnection}）的
	 * {@link SimpleClientHttpRequestFactory}。
	 * <p><b>请注意，标准 JDK HTTP 库不支持 HTTP PATCH 方法。
	 * 请配置 Apache HttpComponents 或 OkHttp 请求工厂以启用 PATCH。</b>
	 * @see #createRequest(URI, HttpMethod)
	 * @see SimpleClientHttpRequestFactory
	 * @see org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory
	 * @see org.springframework.http.client.OkHttp3ClientHttpRequestFactory
	 */
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "ClientHttpRequestFactory must not be null");
		this.requestFactory = requestFactory;
	}

	/**
	 * 返回此访问器用于获取客户端请求句柄的请求工厂。
	 */
	public ClientHttpRequestFactory getRequestFactory() {
		return this.requestFactory;
	}


	/**
	 * 设置此访问器应使用的请求初始化器。
	 * <p>初始化器将立即根据其
	 * {@linkplain AnnotationAwareOrderComparator#sort(List) 顺序}进行排序。
	 * @since 5.2
	 */
	public void setClientHttpRequestInitializers(
			List<ClientHttpRequestInitializer> clientHttpRequestInitializers) {

		if (this.clientHttpRequestInitializers != clientHttpRequestInitializers) {
			this.clientHttpRequestInitializers.clear();
			this.clientHttpRequestInitializers.addAll(clientHttpRequestInitializers);
			AnnotationAwareOrderComparator.sort(this.clientHttpRequestInitializers);
		}
	}

	/**
	 * 获取此访问器使用的请求初始化器。
	 * <p>返回的 {@link List} 是活动的，可以修改。但请注意，
	 * 在初始化 {@link ClientHttpRequest} 之前，初始化器
	 * 不会根据其
	 * {@linkplain AnnotationAwareOrderComparator#sort(List) 顺序}重新排序。
	 * @since 5.2
	 * @see #setClientHttpRequestInitializers(List)
	 */
	public List<ClientHttpRequestInitializer> getClientHttpRequestInitializers() {
		return this.clientHttpRequestInitializers;
	}

	/**
	 * 通过此模板的 {@link ClientHttpRequestFactory} 创建新的 {@link ClientHttpRequest}。
	 * @param url 要连接的 URL
	 * @param method 要执行的 HTTP 方法（GET、POST 等）
	 * @return 所创建的请求
	 * @throws IOException 如果发生 I/O 错误
	 * @see #getRequestFactory()
	 * @see ClientHttpRequestFactory#createRequest(URI, HttpMethod)
	 */
	protected ClientHttpRequest createRequest(URI url, HttpMethod method) throws IOException {
		ClientHttpRequest request = getRequestFactory().createRequest(url, method);
		initialize(request);
		if (logger.isDebugEnabled()) {
			logger.debug("HTTP " + method.name() + " " + url);
		}
		return request;
	}

	private void initialize(ClientHttpRequest request) {
		this.clientHttpRequestInitializers.forEach(initializer -> initializer.initialize(request));
	}

}
