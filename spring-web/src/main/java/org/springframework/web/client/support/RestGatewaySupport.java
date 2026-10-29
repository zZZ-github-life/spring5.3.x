/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.client.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * 需要 REST 访问的应用程序类的便捷父类。
 *
 * <p>需要设置一个 {@link ClientHttpRequestFactory} 或 {@link RestTemplate} 实例。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see #setRestTemplate
 * @see org.springframework.web.client.RestTemplate
 */
public class RestGatewaySupport {

	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private RestTemplate restTemplate;


	/**
	 * 使用默认参数构造一个 {@link RestGatewaySupport} 新实例。
	 */
	public RestGatewaySupport() {
		this.restTemplate = new RestTemplate();
	}

	/**
	 * 使用给定的 {@link ClientHttpRequestFactory} 构造一个 {@link RestGatewaySupport} 新实例。
	 * @see RestTemplate#RestTemplate(ClientHttpRequestFactory)
	 */
	public RestGatewaySupport(ClientHttpRequestFactory requestFactory) {
		Assert.notNull(requestFactory, "'requestFactory' must not be null");
		this.restTemplate = new RestTemplate(requestFactory);
	}


	/**
	 * 设置网关的 {@link RestTemplate}。
	 */
	public void setRestTemplate(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		this.restTemplate = restTemplate;
	}

	/**
	 * 返回网关的 {@link RestTemplate}。
	 */
	public RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

}
