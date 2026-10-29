/*
 * Copyright 2002-2021 the original author or authors.
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.RequestPath;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * 一个 {@code Filter}，用于 {@link ServletRequestPathUtils#parseAndCache parse}
 * 并缓存 {@link org.springframework.http.server.RequestPath}，
 * 以便在整个过滤器链中后续
 * {@link ServletRequestPathUtils#getParsedRequestPath access} 使用。
 * 当应用程序中任何位置使用已解析的
 * {@link org.springframework.web.util.pattern.PathPattern}，而不是使用
 * {@link org.springframework.util.PathMatcher} 进行 String 模式匹配时，
 * 此过滤器非常有用。
 * <p>请注意，在 Spring MVC 中，如果 {@code DispatcherServlet} 检测到任何
 * {@code HandlerMapping} 启用了已解析的 {@code PathPattern}，它也会解析并缓存
 * {@code RequestPath}，但如果发现 {@link ServletRequestPathUtils#PATH_ATTRIBUTE}
 * 已存在，则会跳过该操作。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class ServletRequestPathFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		RequestPath previousRequestPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
		ServletRequestPathUtils.parseAndCache((HttpServletRequest) request);
		try {
			chain.doFilter(request, response);
		}
		finally {
			ServletRequestPathUtils.setParsedRequestPath(previousRequestPath, request);
		}
	}

}
