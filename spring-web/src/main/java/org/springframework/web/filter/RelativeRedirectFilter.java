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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * 重写 {@link HttpServletResponse#sendRedirect(String)}，并通过设置 HTTP 状态和
 * "Location" 头来处理重定向，从而避免 Servlet 容器将相对重定向 URL 重写为绝对 URL。
 * Servlet 容器被要求这样做，但这违背了
 * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2"> RFC 7231 第 7.1.2 节</a>
 * 的建议，而且不一定会考虑 "X-Forwarded" 头。
 *
 * <p><strong>注意：</strong>虽然 RFC 推荐使用相对重定向，
 * 但在某些使用反向代理的配置下，它们可能无法正常工作。
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @since 4.3.10
 */
public class RelativeRedirectFilter extends OncePerRequestFilter {

	private HttpStatus redirectStatus = HttpStatus.SEE_OTHER;


	/**
	 * 设置用于重定向的默认 HTTP 状态。
	 * <p>默认值为 {@link HttpStatus#SEE_OTHER}。
	 * @param status 要使用的 3xx 重定向状态
	 */
	public void setRedirectStatus(HttpStatus status) {
		Assert.notNull(status, "Property 'redirectStatus' is required");
		Assert.isTrue(status.is3xxRedirection(), () -> "Not a redirect status code: " + status);
		this.redirectStatus = status;
	}

	/**
	 * 返回配置的重定向状态。
	 */
	public HttpStatus getRedirectStatus() {
		return this.redirectStatus;
	}


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		response = RelativeRedirectResponseWrapper.wrapIfNecessary(response, this.redirectStatus);
		filterChain.doFilter(request, response);
	}

}
