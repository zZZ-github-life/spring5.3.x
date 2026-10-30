/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;

import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.Assert;

/**
 * {@code MockMvcWebConnection} 使 {@link MockMvc} 能够将
 * {@link WebRequest} 转换为 {@link WebResponse}。
 * <p>这是与 <a href="https://htmlunit.sourceforge.io/">HtmlUnit</a> 的核心集成。
 * <p>下面是一个用法示例。
 *
 * <pre class="code">
 * WebClient webClient = new WebClient();
 * MockMvc mockMvc = ...
 * MockMvcWebConnection webConnection = new MockMvcWebConnection(mockMvc, webClient);
 * webClient.setWebConnection(webConnection);
 *
 * // Use webClient as normal ...
 * </pre>
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see org.springframework.test.web.servlet.htmlunit.webdriver.WebConnectionHtmlUnitDriver
 */
public final class MockMvcWebConnection implements WebConnection {

	private final Map<String, MockHttpSession> sessions = new HashMap<>();

	private final MockMvc mockMvc;

	@Nullable
	private final String contextPath;

	private WebClient webClient;

	private static int MAX_FORWARDS = 100;


	/**
	 * 创建一个新实例，假定应用程序的上下文路径
	 * 为 {@code ""}（即根上下文）。
	 * <p>例如，URL {@code http://localhost/test/this} 将使用
	 * {@code ""} 作为上下文路径。
	 * @param mockMvc 要使用的 {@code MockMvc} 实例；永不为 {@code null}
	 * @param webClient 要使用的 {@link WebClient}。永不为 {@code null}
	 */
	public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient) {
		this(mockMvc, webClient, "");
	}

	/**
	 * 使用指定的上下文路径创建新实例。
	 * <p>该路径可以为 {@code null}，此时 URL 的第一个路径段
	 * 会转换为 contextPath。否则它必须符合
	 * {@link javax.servlet.http.HttpServletRequest#getContextPath()}
	 * 的要求，即可以是空字符串，否则必须以“/”字符开头且
	 * 不以“/”字符结尾。
	 * @param mockMvc 要使用的 {@code MockMvc} 实例（永不为 {@code null}）
	 * @param webClient 要使用的 {@link WebClient}（永不为 {@code null}）
	 * @param contextPath 要使用的 contextPath
	 */
	public MockMvcWebConnection(MockMvc mockMvc, WebClient webClient, @Nullable String contextPath) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		Assert.notNull(webClient, "WebClient must not be null");
		validateContextPath(contextPath);

		this.webClient = webClient;
		this.mockMvc = mockMvc;
		this.contextPath = contextPath;
	}

	/**
	 * 验证提供的 {@code contextPath}。
	 * <p>如果该值不为 {@code null}，它必须符合
	 * {@link javax.servlet.http.HttpServletRequest#getContextPath()} 的要求，
	 * 即可以是空字符串，否则必须以“/”字符开头且不以“/”字符结尾。
	 * @param contextPath 要验证的路径
	 */
	static void validateContextPath(@Nullable String contextPath) {
		if (contextPath == null || contextPath.isEmpty()) {
			return;
		}
		Assert.isTrue(contextPath.startsWith("/"), () -> "contextPath '" + contextPath + "' must start with '/'.");
		Assert.isTrue(!contextPath.endsWith("/"), () -> "contextPath '" + contextPath + "' must not end with '/'.");
	}


	public void setWebClient(WebClient webClient) {
		Assert.notNull(webClient, "WebClient must not be null");
		this.webClient = webClient;
	}


	@Override
	public WebResponse getResponse(WebRequest webRequest) throws IOException {
		long startTime = System.currentTimeMillis();
		HtmlUnitRequestBuilder requestBuilder = new HtmlUnitRequestBuilder(this.sessions, this.webClient, webRequest);
		requestBuilder.setContextPath(this.contextPath);

		MockHttpServletResponse httpServletResponse = getResponse(requestBuilder);
		String forwardedUrl = httpServletResponse.getForwardedUrl();
		int forwards = 0;
		while (forwardedUrl != null && forwards < MAX_FORWARDS) {
			requestBuilder.setForwardPostProcessor(new ForwardRequestPostProcessor(forwardedUrl));
			httpServletResponse = getResponse(requestBuilder);
			forwardedUrl = httpServletResponse.getForwardedUrl();
			forwards += 1;
		}
		if (forwards == MAX_FORWARDS) {
			throw new IllegalStateException("Forwarded more than " + forwards + " times in a row, potential infinite forward loop");
		}
		storeCookies(webRequest, httpServletResponse.getCookies());

		return new MockWebResponseBuilder(startTime, webRequest, httpServletResponse).build();
	}

	private MockHttpServletResponse getResponse(RequestBuilder requestBuilder) throws IOException {
		ResultActions resultActions;
		try {
			resultActions = this.mockMvc.perform(requestBuilder);
		}
		catch (Exception ex) {
			throw new IOException(ex);
		}

		return resultActions.andReturn().getResponse();
	}

	private void storeCookies(WebRequest webRequest, javax.servlet.http.Cookie[] cookies) {
		Date now = new Date();
		CookieManager cookieManager = this.webClient.getCookieManager();
		for (javax.servlet.http.Cookie cookie : cookies) {
			if (cookie.getDomain() == null) {
				cookie.setDomain(webRequest.getUrl().getHost());
			}
			Cookie toManage = createCookie(cookie);
			Date expires = toManage.getExpires();
			if (expires == null || expires.after(now)) {
				cookieManager.addCookie(toManage);
			}
			else {
				cookieManager.removeCookie(toManage);
			}
		}
	}

	private static com.gargoylesoftware.htmlunit.util.Cookie createCookie(javax.servlet.http.Cookie cookie) {
		Date expires = null;
		if (cookie.getMaxAge() > -1) {
			expires = new Date(System.currentTimeMillis() + cookie.getMaxAge() * 1000);
		}
		BasicClientCookie result = new BasicClientCookie(cookie.getName(), cookie.getValue());
		result.setDomain(cookie.getDomain());
		result.setComment(cookie.getComment());
		result.setExpiryDate(expires);
		result.setPath(cookie.getPath());
		result.setSecure(cookie.getSecure());
		if (cookie.isHttpOnly()) {
			result.setAttribute("httponly", "true");
		}
		return new com.gargoylesoftware.htmlunit.util.Cookie(result);
	}

	@Override
	public void close() {
	}

}
