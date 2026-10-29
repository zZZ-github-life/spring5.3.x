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

package org.springframework.http.client.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.web.client.RestTemplate}
 * 和其他 HTTP 访问网关辅助类的基类，在 {@link HttpAccessor}
 * 的公共属性基础上添加与拦截器相关的属性。
 *
 * <p>不打算直接使用。
 * 有关入口点，请参阅 {@link org.springframework.web.client.RestTemplate}。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see ClientHttpRequestInterceptor
 * @see InterceptingClientHttpRequestFactory
 * @see org.springframework.web.client.RestTemplate
 */
public abstract class InterceptingHttpAccessor extends HttpAccessor {

	private final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

	@Nullable
	private volatile ClientHttpRequestFactory interceptingRequestFactory;


	/**
	 * 设置此访问器应使用的请求拦截器。
	 * <p>拦截器将立即根据其
	 * {@linkplain AnnotationAwareOrderComparator#sort(List) 顺序}进行排序。
	 * <p><strong>注意：</strong>此方法不支持并发更改，
	 * 在大多数情况下不应在启动初始化之后调用。
	 * 另请参阅 {@link org.springframework.web.client.RestTemplate}
	 * 中关于并发配置更改的相关说明。
	 * @see #getRequestFactory()
	 * @see AnnotationAwareOrderComparator
	 */
	public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
		Assert.noNullElements(interceptors, "'interceptors' must not contain null elements");
		// Take getInterceptors() List as-is when passed in here
		if (this.interceptors != interceptors) {
			this.interceptors.clear();
			this.interceptors.addAll(interceptors);
			AnnotationAwareOrderComparator.sort(this.interceptors);
		}
	}

	/**
	 * 获取此访问器使用的请求拦截器。
	 * <p>返回的 {@link List} 是活动的，可以修改。但请注意，
	 * 在构建 {@link ClientHttpRequestFactory} 之前，拦截器
	 * 不会根据其
	 * {@linkplain AnnotationAwareOrderComparator#sort(List) 顺序}重新排序。
	 */
	public List<ClientHttpRequestInterceptor> getInterceptors() {
		return this.interceptors;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		super.setRequestFactory(requestFactory);
		this.interceptingRequestFactory = null;
	}

	/**
	 * 重写此方法以便在必要时公开
	 * {@link InterceptingClientHttpRequestFactory}。
	 * @see #getInterceptors()
	 */
	@Override
	public ClientHttpRequestFactory getRequestFactory() {
		List<ClientHttpRequestInterceptor> interceptors = getInterceptors();
		if (!CollectionUtils.isEmpty(interceptors)) {
			ClientHttpRequestFactory factory = this.interceptingRequestFactory;
			if (factory == null) {
				factory = new InterceptingClientHttpRequestFactory(super.getRequestFactory(), interceptors);
				this.interceptingRequestFactory = factory;
			}
			return factory;
		}
		else {
			return super.getRequestFactory();
		}
	}

}
