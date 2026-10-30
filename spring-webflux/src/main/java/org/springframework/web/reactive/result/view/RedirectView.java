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

package org.springframework.web.reactive.result.view;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * 重定向到绝对 URL 或相对于上下文的 URL 的视图。该 URL 可以是 URI 模板，此时 URI 模板变量将替换为模型中的值或当前请求中的 URI 变量。
 *
 * <p>默认使用 {@link HttpStatus#SEE_OTHER}，但可以通过构造函数或 setter 参数提供其他状态代码。
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RedirectView extends AbstractUrlBasedView {

	private static final Pattern URI_TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


	private HttpStatus statusCode = HttpStatus.SEE_OTHER;

	private boolean contextRelative = true;

	private boolean propagateQuery = false;

	@Nullable
	private String[] hosts;


	/**
	 * 用作 Bean 的构造函数。
	 */
	public RedirectView() {
	}

	/**
	 * 使用给定重定向 URL 创建新的 {@code RedirectView}。
	 * <p>默认使用状态代码 {@link HttpStatus#SEE_OTHER}。
	 */
	public RedirectView(String redirectUrl) {
		super(redirectUrl);
	}

	/**
	 * 使用给定 URL 和替代重定向状态代码（例如 {@link HttpStatus#TEMPORARY_REDIRECT} 或 {@link HttpStatus#PERMANENT_REDIRECT}）创建新的 {@code RedirectView}。
	 */
	public RedirectView(String redirectUrl, HttpStatus statusCode) {
		super(redirectUrl);
		setStatusCode(statusCode);
	}


	/**
	 * 设置替代重定向状态代码，例如 {@link HttpStatus#TEMPORARY_REDIRECT} 或 {@link HttpStatus#PERMANENT_REDIRECT}。
	 */
	public void setStatusCode(HttpStatus statusCode) {
		Assert.isTrue(statusCode.is3xxRedirection(), () -> "Not a redirect status code: " + statusCode);
		this.statusCode = statusCode;
	}

	/**
	 * 获取要使用的重定向状态代码。
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

	/**
	 * 是否将以斜杠（"/"）开头的重定向 URL 解释为相对于当前上下文路径（{@code true}，默认值）还是相对于 Web 服务器根路径（{@code false}）。
	 */
	public void setContextRelative(boolean contextRelative) {
		this.contextRelative = contextRelative;
	}

	/**
	 * 是否将 URL 解释为相对于当前上下文路径。
	 */
	public boolean isContextRelative() {
		return this.contextRelative;
	}

	/**
	 * 是否将当前 URL 的查询字符串追加到重定向 URL（{@code true}），或不追加（{@code false}，默认值）。
	 */
	public void setPropagateQuery(boolean propagateQuery) {
		this.propagateQuery = propagateQuery;
	}

	/**
	 * 当前 URL 的查询字符串是否追加到重定向 URL。
	 */
	public boolean isPropagateQuery() {
		return this.propagateQuery;
	}

	/**
	 * 配置与应用程序关联的一个或多个主机。
	 * <p>所有其他主机将被视为外部主机。
	 * <p>实际上，这提供了一种方式：对于具有主机且该主机未列为已知主机的 URL，关闭编码。
	 * <p>如果未设置（默认），则对所有重定向 URL 进行编码。
	 * @param hosts 一个或多个应用主机
	 */
	public void setHosts(@Nullable String... hosts) {
		this.hosts = hosts;
	}

	/**
	 * 返回配置的应用程序主机。
	 */
	@Nullable
	public String[] getHosts() {
		return this.hosts;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}


	@Override
	public boolean isRedirectView() {
		return true;
	}

	@Override
	public boolean checkResourceExists(Locale locale) throws Exception {
		return true;
	}

	/**
	 * 将模型转换为请求参数并重定向到给定 URL。
	 */
	@Override
	protected Mono<Void> renderInternal(
			Map<String, Object> model, @Nullable MediaType contentType, ServerWebExchange exchange) {

		String targetUrl = createTargetUrl(model, exchange);
		return sendRedirect(targetUrl, exchange);
	}

	/**
	 * 创建目标 URL，并在必要时在前面加上 contextPath、展开 URI 模板变量、追加当前请求查询，并应用配置的 {@link #getRequestDataValueProcessor() RequestDataValueProcessor}。
	 */
	protected final String createTargetUrl(Map<String, Object> model, ServerWebExchange exchange) {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		ServerHttpRequest request = exchange.getRequest();

		StringBuilder targetUrl = new StringBuilder();
		if (isContextRelative() && url.startsWith("/")) {
			targetUrl.append(request.getPath().contextPath().value());
		}
		targetUrl.append(url);

		if (StringUtils.hasText(targetUrl)) {
			Map<String, String> uriVars = getCurrentUriVariables(exchange);
			targetUrl = expandTargetUrlTemplate(targetUrl.toString(), model, uriVars);
		}

		if (isPropagateQuery()) {
			targetUrl = appendCurrentRequestQuery(targetUrl.toString(), request);
		}

		String result = targetUrl.toString();

		RequestDataValueProcessor processor = getRequestDataValueProcessor();
		return (processor != null ? processor.processUrl(exchange, result) : result);
	}

	private Map<String, String> getCurrentUriVariables(ServerWebExchange exchange) {
		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return exchange.getAttributeOrDefault(name, Collections.emptyMap());
	}

	/**
	 * 使用模型属性值或当前请求中的 URI 变量值（作为回退）展开目标 URL 中的 URI 模板变量。值会被编码。
	 */
	protected StringBuilder expandTargetUrlTemplate(String targetUrl,
			Map<String, Object> model, Map<String, String> uriVariables) {

		Matcher matcher = URI_TEMPLATE_VARIABLE_PATTERN.matcher(targetUrl);
		boolean found = matcher.find();
		if (!found) {
			return new StringBuilder(targetUrl);
		}
		StringBuilder result = new StringBuilder();
		int endLastMatch = 0;
		while (found) {
			String name = matcher.group(1);
			Object value = (model.containsKey(name) ? model.get(name) : uriVariables.get(name));
			Assert.notNull(value, () -> "No value for URI variable '" + name + "'");
			result.append(targetUrl, endLastMatch, matcher.start());
			result.append(encodeUriVariable(value.toString()));
			endLastMatch = matcher.end();
			found = matcher.find();
		}
		result.append(targetUrl, endLastMatch, targetUrl.length());
		return result;
	}

	private String encodeUriVariable(String text) {
		// Strict encoding of all reserved URI characters
		return UriUtils.encode(text, StandardCharsets.UTF_8);
	}

	/**
	 * 将当前请求的查询追加到目标重定向 URL。
	 */
	protected StringBuilder appendCurrentRequestQuery(String targetUrl, ServerHttpRequest request) {
		String query = request.getURI().getRawQuery();
		if (!StringUtils.hasText(query)) {
			return new StringBuilder(targetUrl);
		}

		int index = targetUrl.indexOf('#');
		String fragment = (index > -1 ? targetUrl.substring(index) : null);

		StringBuilder result = new StringBuilder();
		result.append(index != -1 ? targetUrl.substring(0, index) : targetUrl);
		result.append(targetUrl.indexOf('?') < 0 ? '?' : '&').append(query);

		if (fragment != null) {
			result.append(fragment);
		}

		return result;
	}

	/**
	 * 向 HTTP 客户端发送重定向。
	 * @param targetUrl 要重定向到的目标 URL
	 * @param exchange 当前交换
	 */
	protected Mono<Void> sendRedirect(String targetUrl, ServerWebExchange exchange) {
		String transformedUrl = (isRemoteHost(targetUrl) ? targetUrl : exchange.transformUrl(targetUrl));
		ServerHttpResponse response = exchange.getResponse();
		response.getHeaders().setLocation(URI.create(transformedUrl));
		response.setStatusCode(getStatusCode());
		return Mono.empty();
	}

	/**
	 * 给定 targetUrl 是否具有“外部”系统主机，在这种情况下不会应用 {@link javax.servlet.http.HttpServletResponse#encodeRedirectURL}。
	 * <p>如果配置了 {@link #setHosts(String[])} 属性且目标 URL 的主机不匹配，则此方法返回 {@code true}。
	 * @param targetUrl 目标重定向 URL
	 * @return 如果目标 URL 具有远程主机则为 {@code true}；
	 * 如果该 URL 没有主机或未配置 "host" 属性则为 {@code false}
	 */
	protected boolean isRemoteHost(String targetUrl) {
		if (ObjectUtils.isEmpty(this.hosts)) {
			return false;
		}
		String targetHost = UriComponentsBuilder.fromUriString(targetUrl).build().getHost();
		if (!StringUtils.hasLength(targetHost)) {
			return false;
		}
		for (String host : this.hosts) {
			if (targetHost.equals(host)) {
				return false;
			}
		}
		return true;
	}

}
