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

package org.springframework.mock.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link javax.servlet.FilterConfig} 接口的实现，
 * 将调用简单地传递给给定的 Filter/FilterChain 组合
 * （表示链中的下一个 Filter 及其
 * 应处理的 FilterChain），或传递给给定的 Servlet（表示链的末端）。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see javax.servlet.Filter
 * @see javax.servlet.Servlet
 * @see MockFilterChain
 */
public class PassThroughFilterChain implements FilterChain {

	@Nullable
	private Filter filter;

	@Nullable
	private FilterChain nextFilterChain;

	@Nullable
	private Servlet servlet;


	/**
	 * 创建一个新的 PassThroughFilterChain，它委托给给定的 Filter，
	 * 并使用给定的 FilterChain 调用它。
	 * @param filter 要委托给的 Filter
	 * @param nextFilterChain 用于该下一个 Filter 的 FilterChain
	 */
	public PassThroughFilterChain(Filter filter, FilterChain nextFilterChain) {
		Assert.notNull(filter, "Filter must not be null");
		Assert.notNull(nextFilterChain, "'FilterChain must not be null");
		this.filter = filter;
		this.nextFilterChain = nextFilterChain;
	}

	/**
	 * 创建一个新的 PassThroughFilterChain，它委托给给定的 Servlet。
	 * @param servlet 要委托给的 Servlet
	 */
	public PassThroughFilterChain(Servlet servlet) {
		Assert.notNull(servlet, "Servlet must not be null");
		this.servlet = servlet;
	}


	/**
	 * 将调用传递给 Filter/Servlet。
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response) throws ServletException, IOException {
		if (this.filter != null) {
			this.filter.doFilter(request, response, this.nextFilterChain);
		}
		else {
			Assert.state(this.servlet != null, "Neither a Filter nor a Servlet has been set");
			this.servlet.service(request, response);
		}
	}

}
