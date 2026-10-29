/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.client.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

/**
 * 基于 {@link AsyncHttpAccessor} 并添加请求拦截功能的
 * HTTP 访问器。
 *
 * @author Jakub Narloch
 * @author Rossen Stoyanchev
 * @since 4.3
 * @deprecated as of Spring 5.0, with no direct replacement
 */
@Deprecated
public abstract class InterceptingAsyncHttpAccessor extends AsyncHttpAccessor {

	private List<org.springframework.http.client.AsyncClientHttpRequestInterceptor> interceptors =
			new ArrayList<>();


	/**
	 * 设置此访问器应使用的请求拦截器。
	 * @param interceptors the list of interceptors
	 */
	public void setInterceptors(List<org.springframework.http.client.AsyncClientHttpRequestInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	/**
	 * 返回此访问器使用的请求拦截器。
	 */
	public List<org.springframework.http.client.AsyncClientHttpRequestInterceptor> getInterceptors() {
		return this.interceptors;
	}


	@Override
	public org.springframework.http.client.AsyncClientHttpRequestFactory getAsyncRequestFactory() {
		org.springframework.http.client.AsyncClientHttpRequestFactory delegate = super.getAsyncRequestFactory();
		if (!CollectionUtils.isEmpty(getInterceptors())) {
			return new org.springframework.http.client.InterceptingAsyncClientHttpRequestFactory(delegate, getInterceptors());
		}
		else {
			return delegate;
		}
	}

}
