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

package org.springframework.web.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.Filter}，将 POST 请求中的方法参数转换为 HTTP 方法，
 * 可通过 {@link HttpServletRequest#getMethod()} 获取。由于浏览器目前只支持
 * GET 和 POST，一种常见技术（例如 Prototype 库所采用的技术）是使用普通的 POST
 * 请求并附加一个隐藏表单字段（{@code _method}）来传递“真正的”HTTP 方法。
 * 此过滤器读取该参数，并相应地修改
 * {@link HttpServletRequestWrapper#getMethod()} 的返回值。
 * 仅允许 {@code "PUT"}、{@code "DELETE"} 和 {@code "PATCH"} HTTP 方法。
 *
 * <p>请求参数名默认为 {@code _method}，但可以通过
 * {@link #setMethodParam(String) methodParam} 属性进行调整。
 *
 * <p><b>注意：对于 multipart POST 请求，此过滤器需要在 multipart 处理之后运行，
 * 因为它需要检查 POST 请求体参数。</b>
 * 因此，通常应在 {@code web.xml} 过滤器链中将 Spring 的
 * {@link org.springframework.web.multipart.support.MultipartFilter} 放在此
 * HiddenHttpMethodFilter <i>之前</i>。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class HiddenHttpMethodFilter extends OncePerRequestFilter {

	private static final List<String> ALLOWED_METHODS =
			Collections.unmodifiableList(Arrays.asList(HttpMethod.PUT.name(),
					HttpMethod.DELETE.name(), HttpMethod.PATCH.name()));

	/** 默认方法参数名：{@code _method}。 */
	public static final String DEFAULT_METHOD_PARAM = "_method";

	private String methodParam = DEFAULT_METHOD_PARAM;


	/**
	 * 设置用于查找 HTTP 方法的参数名。
	 * @see #DEFAULT_METHOD_PARAM
	 */
	public void setMethodParam(String methodParam) {
		Assert.hasText(methodParam, "'methodParam' must not be empty");
		this.methodParam = methodParam;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		HttpServletRequest requestToUse = request;

		if ("POST".equals(request.getMethod()) && request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
			String paramValue = request.getParameter(this.methodParam);
			if (StringUtils.hasLength(paramValue)) {
				String method = paramValue.toUpperCase(Locale.ENGLISH);
				if (ALLOWED_METHODS.contains(method)) {
					requestToUse = new HttpMethodRequestWrapper(request, method);
				}
			}
		}

		filterChain.doFilter(requestToUse, response);
	}


	/**
	 * 简单的 {@link HttpServletRequest} 包装器，为
	 * {@link HttpServletRequest#getMethod()} 返回所提供的方法。
	 */
	private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {

		private final String method;

		public HttpMethodRequestWrapper(HttpServletRequest request, String method) {
			super(request);
			this.method = method;
		}

		@Override
		public String getMethod() {
			return this.method;
		}
	}

}
