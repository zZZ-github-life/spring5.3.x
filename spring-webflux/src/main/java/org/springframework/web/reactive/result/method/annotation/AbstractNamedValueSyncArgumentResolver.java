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

package org.springframework.web.reactive.result.method.annotation;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.SyncHandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link AbstractNamedValueArgumentResolver} 的扩展，用于同步且非阻塞的命名值解析器。子类实现同步的 {@link #resolveNamedValue}，默认情况下异步的 {@link #resolveName} 会委托给它。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractNamedValueSyncArgumentResolver extends AbstractNamedValueArgumentResolver
		implements SyncHandlerMethodArgumentResolver {

	/**
	 * 创建新的 {@link AbstractNamedValueSyncArgumentResolver}。
	 * @param factory 用于解析默认值中的 {@code ${...}} 占位符
	 * 和 {@code #{...}} SpEL 表达式的 Bean 工厂；
	 * 如果默认值预计没有表达式则为 {@code null}
	 * @param registry 用于检查响应式类型包装
	 */
	protected AbstractNamedValueSyncArgumentResolver(
			@Nullable ConfigurableBeanFactory factory, ReactiveAdapterRegistry registry) {

		super(factory, registry);
	}


	@Override
	public Mono<Object> resolveArgument(
			MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {

		// Flip the default implementation from SyncHandlerMethodArgumentResolver:
		// instead of delegating to (sync) resolveArgumentValue,
		// call (async) super.resolveArgument shared with non-blocking resolvers;
		// actual resolution below still sync...

		return super.resolveArgument(parameter, bindingContext, exchange);
	}

	@Override
	public Object resolveArgumentValue(
			MethodParameter parameter, BindingContext context, ServerWebExchange exchange) {

		// This won't block since resolveName below doesn't
		return resolveArgument(parameter, context, exchange).block();
	}

	@Override
	protected final Mono<Object> resolveName(String name, MethodParameter param, ServerWebExchange exchange) {
		return Mono.justOrEmpty(resolveNamedValue(name, param, exchange));
	}

	/**
	 * 实际同步解析值。
	 */
	@Nullable
	protected abstract Object resolveNamedValue(String name, MethodParameter param, ServerWebExchange exchange);

}
