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

package org.springframework.messaging.handler.invocation.reactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

/**
 * 协助配置处理器方法参数解析器。
 * 目前仅支持提供自定义解析器列表。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class ArgumentResolverConfigurer {

	private final List<HandlerMethodArgumentResolver> customResolvers = new ArrayList<>(8);


	/**
	 * 为自定义处理器方法参数配置解析器。
	 * @param resolver 要添加的解析器
	 */
	public void addCustomResolver(HandlerMethodArgumentResolver... resolver) {
		Assert.notNull(resolver, "'resolvers' must not be null");
		this.customResolvers.addAll(Arrays.asList(resolver));
	}


	public List<HandlerMethodArgumentResolver> getCustomResolvers() {
		return this.customResolvers;
	}

}
