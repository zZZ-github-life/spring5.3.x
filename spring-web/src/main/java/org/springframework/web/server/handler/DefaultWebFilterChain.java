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

package org.springframework.web.server.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;

/**
 * {@link WebFilterChain} 的默认实现。
 *
 * <p>此类的每个实例代表链中的一个环节。公共构造函数
 * {@link #DefaultWebFilterChain(WebHandler, List)} 会初始化整条链，
 * 并代表链的第一个环节。
 *
 * <p>此类是不可变的，并且线程安全。它可以创建一次，然后被复用于并发处理请求。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultWebFilterChain implements WebFilterChain {

	private final List<WebFilter> allFilters;

	private final WebHandler handler;

	@Nullable
	private final WebFilter currentFilter;

	@Nullable
	private final DefaultWebFilterChain chain;


	/**
	 * 带有过滤器列表和要使用的目标处理器的公共构造函数。
	 * @param handler the target handler
	 * @param filters the filters ahead of the handler
	 * @since 5.1
	 */
	public DefaultWebFilterChain(WebHandler handler, List<WebFilter> filters) {
		Assert.notNull(handler, "WebHandler is required");
		this.allFilters = Collections.unmodifiableList(filters);
		this.handler = handler;
		DefaultWebFilterChain chain = initChain(filters, handler);
		this.currentFilter = chain.currentFilter;
		this.chain = chain.chain;
	}

	private static DefaultWebFilterChain initChain(List<WebFilter> filters, WebHandler handler) {
		DefaultWebFilterChain chain = new DefaultWebFilterChain(filters, handler, null, null);
		ListIterator<? extends WebFilter> iterator = filters.listIterator(filters.size());
		while (iterator.hasPrevious()) {
			chain = new DefaultWebFilterChain(filters, handler, iterator.previous(), chain);
		}
		return chain;
	}

	/**
	 * 表示链中某一环节的私有构造函数。
	 */
	private DefaultWebFilterChain(List<WebFilter> allFilters, WebHandler handler,
			@Nullable WebFilter currentFilter, @Nullable DefaultWebFilterChain chain) {

		this.allFilters = allFilters;
		this.currentFilter = currentFilter;
		this.handler = handler;
		this.chain = chain;
	}

	/**
	 * 带有过滤器列表和要使用的目标处理器的公共构造函数。
	 * @param handler the target handler
	 * @param filters the filters ahead of the handler
	 * @deprecated as of 5.1 this constructor is deprecated in favor of
	 * {@link #DefaultWebFilterChain(WebHandler, List)}.
	 */
	@Deprecated
	public DefaultWebFilterChain(WebHandler handler, WebFilter... filters) {
		this(handler, Arrays.asList(filters));
	}


	public List<WebFilter> getFilters() {
		return this.allFilters;
	}

	public WebHandler getHandler() {
		return this.handler;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange) {
		return Mono.defer(() ->
				this.currentFilter != null && this.chain != null ?
						invokeFilter(this.currentFilter, this.chain, exchange) :
						this.handler.handle(exchange));
	}

	private Mono<Void> invokeFilter(WebFilter current, DefaultWebFilterChain chain, ServerWebExchange exchange) {
		String currentName = current.getClass().getName();
		return current.filter(exchange, chain).checkpoint(currentName + " [DefaultWebFilterChain]");
	}

}
