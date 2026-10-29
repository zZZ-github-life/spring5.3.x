/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 通用的组合 Servlet {@link Filter}，仅将其行为委托给由用户提供的过滤器组成的
 * 链（列表），从而实现 {@link FilterChain} 的功能，但可以方便地只使用
 * {@link Filter} 实例。
 *
 * <p>这对于需要依赖注入、因而可以在 Spring 应用上下文中设置的过滤器非常有用。
 * 通常，此组合过滤器会与 {@link DelegatingFilterProxy} 配合使用，
 * 以便它可以在 Spring 中声明，但应用于 Servlet 上下文。
 *
 * @author Dave Syer
 * @since 3.1
 */
public class CompositeFilter implements Filter {

	private List<? extends Filter> filters = new ArrayList<>();


	public void setFilters(List<? extends Filter> filters) {
		this.filters = new ArrayList<>(filters);
	}


	/**
	 * 初始化所有过滤器，按提供的顺序依次调用每个过滤器的 init 方法。
	 * @see Filter#init(FilterConfig)
	 */
	@Override
	public void init(FilterConfig config) throws ServletException {
		for (Filter filter : this.filters) {
			filter.init(config);
		}
	}

	/**
	 * 由所提供的委托过滤器列表（{@link #setFilters}）构成一个临时链，并按顺序执行它们。
	 * 每个过滤器委托给列表中的下一个过滤器，从而实现 {@link FilterChain} 的常规行为，
	 * 尽管此类型本身只是一个 {@link Filter}。
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		new VirtualFilterChain(chain, this.filters).doFilter(request, response);
	}

	/**
	 * 清理所有提供的过滤器，依次调用每个过滤器的 destroy 方法，但按相反顺序。
	 * @see Filter#init(FilterConfig)
	 */
	@Override
	public void destroy() {
		for (int i = this.filters.size(); i-- > 0;) {
			Filter filter = this.filters.get(i);
			filter.destroy();
		}
	}


	private static class VirtualFilterChain implements FilterChain {

		private final FilterChain originalChain;

		private final List<? extends Filter> additionalFilters;

		private int currentPosition = 0;

		public VirtualFilterChain(FilterChain chain, List<? extends Filter> additionalFilters) {
			this.originalChain = chain;
			this.additionalFilters = additionalFilters;
		}

		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response)
				throws IOException, ServletException {

			if (this.currentPosition == this.additionalFilters.size()) {
				this.originalChain.doFilter(request, response);
			}
			else {
				this.currentPosition++;
				Filter nextFilter = this.additionalFilters.get(this.currentPosition - 1);
				nextFilter.doFilter(request, response, this);
			}
		}
	}

}
