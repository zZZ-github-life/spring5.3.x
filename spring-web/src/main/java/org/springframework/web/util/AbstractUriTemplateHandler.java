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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link UriTemplateHandler} 实现的抽象基类。
 *
 * <p>支持 {@link #setBaseUrl} 和 {@link #setDefaultUriVariables} 属性，
 * 无论子类使用何种 URI 模板展开和编码机制，这些属性都应适用。
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 * @deprecated as of 5.0 in favor of {@link DefaultUriBuilderFactory}
 */
@Deprecated
public abstract class AbstractUriTemplateHandler implements UriTemplateHandler {

	@Nullable
	private String baseUrl;

	private final Map<String, Object> defaultUriVariables = new HashMap<>();


	/**
	 * 配置要添加到 URI 模板前面的基础 URL。基础 URL 必须包含
	 * scheme 和 host，但可选地包含端口和路径。
	 * 基础 URL 必须完全展开并编码，这可以通过
	 * {@link UriComponentsBuilder} 完成。
	 * @param baseUrl the base URL.
	 */
	public void setBaseUrl(@Nullable String baseUrl) {
		if (baseUrl != null) {
			UriComponents uriComponents = UriComponentsBuilder.fromUriString(baseUrl).build();
			Assert.hasText(uriComponents.getScheme(), "'baseUrl' must have a scheme");
			Assert.hasText(uriComponents.getHost(), "'baseUrl' must have a host");
			Assert.isNull(uriComponents.getQuery(), "'baseUrl' cannot have a query");
			Assert.isNull(uriComponents.getFragment(), "'baseUrl' cannot have a fragment");
		}
		this.baseUrl = baseUrl;
	}

	/**
	 * 返回已配置的基础 URL。
	 */
	@Nullable
	public String getBaseUrl() {
		return this.baseUrl;
	}

	/**
	 * 配置用于每个展开 URI 模板的默认 URI 变量值。这些默认值
	 * 仅在通过 Map 展开时适用，而不适用于数组；使用 Map 时，
	 * 提供给 {@link #expand(String, Map)} 的 Map 可以覆盖默认值。
	 * @param defaultUriVariables the default URI variable values
	 * @since 4.3
	 */
	public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables.clear();
		if (defaultUriVariables != null) {
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * 返回已配置默认 URI 变量的只读副本。
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}


	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		if (!getDefaultUriVariables().isEmpty()) {
			Map<String, Object> map = new HashMap<>();
			map.putAll(getDefaultUriVariables());
			map.putAll(uriVariables);
			uriVariables = map;
		}
		URI url = expandInternal(uriTemplate, uriVariables);
		return insertBaseUrl(url);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVariables) {
		URI url = expandInternal(uriTemplate, uriVariables);
		return insertBaseUrl(url);
	}


	/**
	 * 实际展开并编码 URI 模板。
	 */
	protected abstract URI expandInternal(String uriTemplate, Map<String, ?> uriVariables);

	/**
	 * 实际展开并编码 URI 模板。
	 */
	protected abstract URI expandInternal(String uriTemplate, Object... uriVariables);


	/**
	 * 插入基础 URL（如果已配置），除非给定 URL 已包含 host。
	 */
	private URI insertBaseUrl(URI url) {
		try {
			String baseUrl = getBaseUrl();
			if (baseUrl != null && url.getHost() == null) {
				url = new URI(baseUrl + url.toString());
			}
			return url;
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("Invalid URL after inserting base URL: " + url, ex);
		}
	}

}
