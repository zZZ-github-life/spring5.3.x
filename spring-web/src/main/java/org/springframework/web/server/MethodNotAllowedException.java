/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.server;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * 适用于响应状态 405（方法不被允许）的异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class MethodNotAllowedException extends ResponseStatusException {

	private final String method;

	private final Set<HttpMethod> httpMethods;


	public MethodNotAllowedException(HttpMethod method, Collection<HttpMethod> supportedMethods) {
		this(method.name(), supportedMethods);
	}

	public MethodNotAllowedException(String method, @Nullable Collection<HttpMethod> supportedMethods) {
		super(HttpStatus.METHOD_NOT_ALLOWED, "Request method '" + method + "' not supported");
		Assert.notNull(method, "'method' is required");
		if (supportedMethods == null) {
			supportedMethods = Collections.emptySet();
		}
		this.method = method;
		this.httpMethods = Collections.unmodifiableSet(new LinkedHashSet<>(supportedMethods));
	}


	/**
	 * 返回包含 "Allow" 头部的 Map。
	 * @since 5.1.11
	 */
	@SuppressWarnings("deprecation")
	@Override
	public Map<String, String> getHeaders() {
		return getResponseHeaders().toSingleValueMap();
	}

	/**
	 * 返回包含 "Allow" 头部的 HttpHeaders。
	 * @since 5.1.13
	 */
	@Override
	public HttpHeaders getResponseHeaders() {
		if (CollectionUtils.isEmpty(this.httpMethods)) {
			return HttpHeaders.EMPTY;
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(this.httpMethods);
		return headers;
	}

	/**
	 * 返回失败请求所使用的 HTTP 方法。
	 */
	public String getHttpMethod() {
		return this.method;
	}

	/**
	 * 返回受支持的 HTTP 方法列表。
	 */
	public Set<HttpMethod> getSupportedMethods() {
		return this.httpMethods;
	}

}
