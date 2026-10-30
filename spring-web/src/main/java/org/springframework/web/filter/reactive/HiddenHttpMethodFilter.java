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

package org.springframework.web.filter.reactive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * 响应式 {@link WebFilter}，将 POST 请求中的方法参数转换为 HTTP 方法，
 * 可通过 {@link ServerHttpRequest#getMethod()} 获取。由于浏览器目前只支持
 * GET 和 POST，一种常见技术是使用普通的 POST 请求并附加一个隐藏表单字段
 * （{@code _method}）来传递“真正的”HTTP 方法。
 * 此过滤器读取该参数，并使用 {@link ServerWebExchange#mutate()} 修改
 * {@link ServerHttpRequest#getMethod()} 的返回值。
 *
 * <p>请求参数名默认为 {@code _method}，但可以通过
 * {@link #setMethodParamName(String) methodParamName} 属性进行调整。
 *
 * @author Greg Turnquist
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HiddenHttpMethodFilter implements WebFilter {

	private static final List<HttpMethod> ALLOWED_METHODS =
			Collections.unmodifiableList(Arrays.asList(HttpMethod.PUT,
					HttpMethod.DELETE, HttpMethod.PATCH));

	/** 携带要使用的 HTTP 方法的表单参数的默认名称。 */
	public static final String DEFAULT_METHOD_PARAMETER_NAME = "_method";


	private String methodParamName = DEFAULT_METHOD_PARAMETER_NAME;


	/**
	 * 设置携带要使用的 HTTP 方法的表单参数名称。
	 * <p>默认设置为 {@code "_method"}。
	 */
	public void setMethodParamName(String methodParamName) {
		Assert.hasText(methodParamName, "'methodParamName' must not be empty");
		this.methodParamName = methodParamName;
	}


	/**
	 * 根据 {@code methodParamName} 将 HTTP POST 转换为另一种方法。
	 * @param exchange 当前服务器交换
	 * @param chain 提供委托给下一个过滤器的方式
	 * @return 表示请求处理何时完成的 {@code Mono<Void>}
	 */
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		if (exchange.getRequest().getMethod() != HttpMethod.POST) {
			return chain.filter(exchange);
		}

		return exchange.getFormData()
				.map(formData -> {
					String method = formData.getFirst(this.methodParamName);
					return StringUtils.hasLength(method) ? mapExchange(exchange, method) : exchange;
				})
				.flatMap(chain::filter);
	}

	private ServerWebExchange mapExchange(ServerWebExchange exchange, String methodParamValue) {
		HttpMethod httpMethod = HttpMethod.resolve(methodParamValue.toUpperCase(Locale.ENGLISH));
		Assert.notNull(httpMethod, () -> "HttpMethod '" + methodParamValue + "' not supported");
		if (ALLOWED_METHODS.contains(httpMethod)) {
			return exchange.mutate().request(builder -> builder.method(httpMethod)).build();
		}
		else {
			return exchange;
		}
	}

}
