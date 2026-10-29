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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * {@link javax.servlet.Filter}，使表单编码数据在 HTTP PUT 或 PATCH 请求期间
 * 可以通过 {@code ServletRequest.getParameter*()} 系列方法获取。
 *
 * <p>Servlet 规范要求表单数据在 HTTP POST 请求中可用，但在 HTTP PUT 或 PATCH
 * 请求中不要求。此过滤器拦截内容类型为
 * {@code 'application/x-www-form-urlencoded'} 的 HTTP PUT 和 PATCH 请求，
 * 从请求体中读取表单编码内容，并包装 ServletRequest，
 * 使表单数据可以像 HTTP POST 请求中那样作为请求参数获取。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @deprecated as of 5.1 in favor of {@link FormContentFilter} which is the same
 * but also handles DELETE.
 */
@Deprecated
public class HttpPutFormContentFilter extends OncePerRequestFilter {

	private FormHttpMessageConverter formConverter = new AllEncompassingFormHttpMessageConverter();


	/**
	 * 设置用于解析表单内容的转换器。
	 * <p>默认情况下这是一个 {@link AllEncompassingFormHttpMessageConverter} 实例。
	 */
	public void setFormConverter(FormHttpMessageConverter converter) {
		Assert.notNull(converter, "FormHttpMessageConverter is required.");
		this.formConverter = converter;
	}

	public FormHttpMessageConverter getFormConverter() {
		return this.formConverter;
	}

	/**
	 * 用于读取表单数据的默认字符集。
	 * 这是以下调用的快捷方式：<br>
	 * {@code getFormConverter.setCharset(charset)}。
	 */
	public void setCharset(Charset charset) {
		this.formConverter.setCharset(charset);
	}


	@Override
	protected void doFilterInternal(final HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		if (("PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) && isFormContentType(request)) {
			HttpInputMessage inputMessage = new ServletServerHttpRequest(request) {
				@Override
				public InputStream getBody() throws IOException {
					return request.getInputStream();
				}
			};
			MultiValueMap<String, String> formParameters = this.formConverter.read(null, inputMessage);
			if (!formParameters.isEmpty()) {
				HttpServletRequest wrapper = new HttpPutFormContentRequestWrapper(request, formParameters);
				filterChain.doFilter(wrapper, response);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private boolean isFormContentType(HttpServletRequest request) {
		String contentType = request.getContentType();
		if (contentType != null) {
			try {
				MediaType mediaType = MediaType.parseMediaType(contentType);
				return (MediaType.APPLICATION_FORM_URLENCODED.includes(mediaType));
			}
			catch (IllegalArgumentException ex) {
				return false;
			}
		}
		else {
			return false;
		}
	}


	private static class HttpPutFormContentRequestWrapper extends HttpServletRequestWrapper {

		private MultiValueMap<String, String> formParameters;

		public HttpPutFormContentRequestWrapper(HttpServletRequest request, MultiValueMap<String, String> parameters) {
			super(request);
			this.formParameters = parameters;
		}

		@Override
		@Nullable
		public String getParameter(String name) {
			String queryStringValue = super.getParameter(name);
			String formValue = this.formParameters.getFirst(name);
			return (queryStringValue != null ? queryStringValue : formValue);
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			Map<String, String[]> result = new LinkedHashMap<>();
			Enumeration<String> names = getParameterNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				result.put(name, getParameterValues(name));
			}
			return result;
		}

		@Override
		public Enumeration<String> getParameterNames() {
			Set<String> names = new LinkedHashSet<>();
			names.addAll(Collections.list(super.getParameterNames()));
			names.addAll(this.formParameters.keySet());
			return Collections.enumeration(names);
		}

		@Override
		@Nullable
		public String[] getParameterValues(String name) {
			String[] parameterValues = super.getParameterValues(name);
			List<String> formParam = this.formParameters.get(name);
			if (formParam == null) {
				return parameterValues;
			}
			if (parameterValues == null || getQueryString() == null) {
				return StringUtils.toStringArray(formParam);
			}
			else {
				List<String> result = new ArrayList<>(parameterValues.length + formParam.size());
				result.addAll(Arrays.asList(parameterValues));
				result.addAll(formParam);
				return StringUtils.toStringArray(result);
			}
		}
	}

}
