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

package org.springframework.messaging.handler.invocation.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * 通过委托给一组已注册的
 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers} 来解析方法参数。
 * 之前解析过的方法参数会被缓存，以加快查找速度。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class HandlerMethodArgumentResolverComposite implements HandlerMethodArgumentResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>();

	private final Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
			new ConcurrentHashMap<>(256);


	/**
	 * 添加给定的 {@link HandlerMethodArgumentResolver}。
	 */
	public HandlerMethodArgumentResolverComposite addResolver(HandlerMethodArgumentResolver resolver) {
		this.argumentResolvers.add(resolver);
		return this;
	}

	/**
	 * 添加给定的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 */
	public HandlerMethodArgumentResolverComposite addResolvers(@Nullable HandlerMethodArgumentResolver... resolvers) {
		if (resolvers != null) {
			Collections.addAll(this.argumentResolvers, resolvers);
		}
		return this;
	}

	/**
	 * 添加给定的 {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}。
	 */
	public HandlerMethodArgumentResolverComposite addResolvers(
			@Nullable List<? extends HandlerMethodArgumentResolver> resolvers) {

		if (resolvers != null) {
			this.argumentResolvers.addAll(resolvers);
		}
		return this;
	}

	/**
	 * 返回包含的解析器的只读列表，或空列表。
	 */
	public List<HandlerMethodArgumentResolver> getResolvers() {
		return Collections.unmodifiableList(this.argumentResolvers);
	}

	/**
	 * 清除已配置的解析器列表和解析器缓存。
	 */
	public void clear() {
		this.argumentResolvers.clear();
		this.argumentResolverCache.clear();
	}


	/**
	 * 给定{@linkplain MethodParameter 方法参数}是否受任何已注册的
	 * {@link HandlerMethodArgumentResolver} 支持。
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return getArgumentResolver(parameter) != null;
	}

	/**
	 * 遍历已注册的
	 * {@link HandlerMethodArgumentResolver HandlerMethodArgumentResolvers}，
	 * 并调用支持该参数的那个。
	 * @throws IllegalStateException if no suitable
	 * {@link HandlerMethodArgumentResolver} is found.
	 */
	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
		HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
		if (resolver == null) {
			throw new IllegalArgumentException("Unsupported parameter type [" +
					parameter.getParameterType().getName() + "]. supportsParameter should be called first.");
		}
		return resolver.resolveArgument(parameter, message);
	}

	/**
	 * 查找支持给定方法参数的已注册 {@link HandlerMethodArgumentResolver}。
	 */
	@Nullable
	public HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
		HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
		if (result == null) {
			for (HandlerMethodArgumentResolver methodArgumentResolver : this.argumentResolvers) {
				if (methodArgumentResolver.supportsParameter(parameter)) {
					result = methodArgumentResolver;
					this.argumentResolverCache.put(parameter, result);
					break;
				}
			}
		}
		return result;
	}

}
