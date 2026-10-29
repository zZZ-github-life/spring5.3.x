/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * {@link WebSocketConfigurationSupport} 的一种变体，它会检测 Spring 配置中的
 * {@link WebSocketConfigurer} 实现，并调用它们来配置 WebSocket 请求处理。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
public class DelegatingWebSocketConfiguration extends WebSocketConfigurationSupport {

	private final List<WebSocketConfigurer> configurers = new ArrayList<>();


	@Autowired(required = false)
	public void setConfigurers(List<WebSocketConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addAll(configurers);
		}
	}


	@Override
	protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		for (WebSocketConfigurer configurer : this.configurers) {
			configurer.registerWebSocketHandlers(registry);
		}
	}

}
