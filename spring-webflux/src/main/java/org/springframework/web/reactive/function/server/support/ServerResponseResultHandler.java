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

package org.springframework.web.reactive.function.server.support;

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * 支持 {@link ServerResponse ServerResponses} 的 {@code HandlerResultHandler} 实现。
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ServerResponseResultHandler implements HandlerResultHandler, InitializingBean, Ordered {

	private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

	private List<ViewResolver> viewResolvers = Collections.emptyList();

	private int order = 0;


	/**
	 * 配置用于序列化请求正文的 HTTP 消息写出器。
	 * <p>默认设置为 {@link ServerCodecConfigurer} 的默认写出器。
	 */
	public void setMessageWriters(List<HttpMessageWriter<?>> configurer) {
		this.messageWriters = configurer;
	}

	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	/**
	 * 设置此结果处理器相对于其他处理器的顺序。
	 * <p>默认设置为 0。由于它会查找具体返回类型，通常可以安全地将其放在较靠前的位置。
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(this.messageWriters)) {
			throw new IllegalArgumentException("Property 'messageWriters' is required");
		}
	}

	@Override
	public boolean supports(HandlerResult result) {
		return (result.getReturnValue() instanceof ServerResponse);
	}

	@Override
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		ServerResponse response = (ServerResponse) result.getReturnValue();
		Assert.state(response != null, "No ServerResponse");
		return response.writeTo(exchange, new ServerResponse.Context() {
			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				return messageWriters;
			}
			@Override
			public List<ViewResolver> viewResolvers() {
				return viewResolvers;
			}
		});
	}

}
