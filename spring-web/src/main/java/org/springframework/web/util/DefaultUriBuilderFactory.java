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

package org.springframework.web.util;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@code UriBuilderFactory}，依赖于 {@link UriComponentsBuilder} 完成
 * URI 的实际构建。
 *
 * <p>提供多种选项，例如创建带有公共基础 URI 的 {@link UriBuilder} 实例、
 * 可选的编码模式策略等。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see UriComponentsBuilder
 */
public class DefaultUriBuilderFactory implements UriBuilderFactory {

	@Nullable
	private final UriComponentsBuilder baseUri;

	private EncodingMode encodingMode = EncodingMode.TEMPLATE_AND_VALUES;

	private final Map<String, Object> defaultUriVariables = new HashMap<>();

	private boolean parsePath = true;


	/**
	 * 不带基础 URI 的默认构造函数。
	 * <p>必须在每个 UriBuilder 上指定目标地址。
	 */
	public DefaultUriBuilderFactory() {
		this.baseUri = null;
	}

	/**
	 * 带有基础 URI 的构造函数。
	 * <p>给定的 URI 模板通过 {@link UriComponentsBuilder#fromUriString} 解析，
	 * 然后通过 {@link UriComponentsBuilder#uriComponents} 应用为每个 UriBuilder
	 * 的基础 URI，除非该 UriBuilder 本身是使用已包含目标地址的 URI 模板创建的。
	 * @param baseUriTemplate the URI template to use a base URL
	 */
	public DefaultUriBuilderFactory(String baseUriTemplate) {
		this.baseUri = UriComponentsBuilder.fromUriString(baseUriTemplate);
	}

	/**
	 * {@link #DefaultUriBuilderFactory(String)} 的变体，接受
	 * {@code UriComponentsBuilder}。
	 */
	public DefaultUriBuilderFactory(UriComponentsBuilder baseUri) {
		this.baseUri = baseUri;
	}


	/**
	 * 设置要使用的 {@link EncodingMode 编码模式}。
	 * <p>默认设置为 {@link EncodingMode#TEMPLATE_AND_VALUES
	 * EncodingMode.TEMPLATE_AND_VALUES}。
	 * <p><strong>注意：</strong>在 5.1 之前，默认值为
	 * {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}，
	 * 因此 {@code WebClient} 和 {@code RestTemplate} 已改用新的默认行为。
	 * @param encodingMode the encoding mode to use
	 */
	public void setEncodingMode(EncodingMode encodingMode) {
		this.encodingMode = encodingMode;
	}

	/**
	 * 返回已配置的编码模式。
	 */
	public EncodingMode getEncodingMode() {
		return this.encodingMode;
	}

	/**
	 * 提供在使用变量 Map 展开 URI 模板时要使用的默认 URI 变量值。
	 * @param defaultUriVariables default URI variable values
	 */
	public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
		this.defaultUriVariables.clear();
		if (defaultUriVariables != null) {
			this.defaultUriVariables.putAll(defaultUriVariables);
		}
	}

	/**
	 * 返回已配置的默认 URI 变量值。
	 */
	public Map<String, ?> getDefaultUriVariables() {
		return Collections.unmodifiableMap(this.defaultUriVariables);
	}

	/**
	 * 当编码模式设置为 {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}
	 * 时，是否将输入路径解析为路径段，以此确保路径中的 URI 变量按照路径段规则进行编码，
	 * 例如 '/' 会被编码。
	 * <p>默认设置为 {@code true}。
	 * @param parsePath whether to parse the path into path segments
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * 当编码模式设置为 {@link EncodingMode#URI_COMPONENT EncodingMode.URI_COMPONENT}
	 * 时，是否将路径解析为路径段。
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}


	// UriTemplateHandler

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	@Override
	public URI expand(String uriTemplate, Object... uriVars) {
		return uriString(uriTemplate).build(uriVars);
	}

	// UriBuilderFactory

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return new DefaultUriBuilder(uriTemplate);
	}

	@Override
	public UriBuilder builder() {
		return new DefaultUriBuilder("");
	}


	/**
	 * 表示多种 URI 编码策略的枚举。可用值如下：
	 * <ul>
	 * <li>{@link #TEMPLATE_AND_VALUES}
	 * <li>{@link #VALUES_ONLY}
	 * <li>{@link #URI_COMPONENT}
	 * <li>{@link #NONE}
	 * </ul>
	 * @see #setEncodingMode
	 */
	public enum EncodingMode {

		/**
		 * 先对 URI 模板进行预编码，然后在展开时对 URI 变量进行严格编码，
		 * 规则如下：
		 * <ul>
		 * <li>对于 URI 模板，<em>仅</em>将非 ASCII 字符和非法字符
		 * （在给定 URI 组件类型范围内）替换为转义八位字节。
		 * <li>对于 URI 变量，执行相同的替换，并且还会替换具有保留含义的字符。
		 * </ul>
		 * <p>在大多数情况下，此模式最有可能给出预期结果，因为它将 URI 变量视为
		 * 需要完全编码的不透明数据；相比之下，{@link #URI_COMPONENT} 仅在
		 * 有意使用保留字符展开 URI 变量时才有用。
		 * @since 5.0.8
		 * @see UriComponentsBuilder#encode()
		 */
		TEMPLATE_AND_VALUES,

		/**
		 * 不对 URI 模板进行编码，而是在将 URI 变量展开到模板之前，
		 * 通过 {@link UriUtils#encodeUriVariables} 对其进行严格编码。
		 * @see UriUtils#encodeUriVariables(Object...)
		 * @see UriUtils#encodeUriVariables(Map)
		 */
		VALUES_ONLY,

		/**
		 * 先展开 URI 变量，然后对生成的 URI 组件值进行编码，
		 * <em>仅</em>替换非 ASCII 字符和非法字符（在给定 URI 组件类型范围内），
		 * 而不替换具有保留含义的字符。
		 * @see UriComponents#encode()
		 */
		URI_COMPONENT,

		/**
		 * 不应应用任何编码。
		 */
		NONE
	}


	/**
	 * {@link DefaultUriBuilderFactory} 特有的 UriBuilder 实现。
	 */
	private class DefaultUriBuilder implements UriBuilder {

		private final UriComponentsBuilder uriComponentsBuilder;

		public DefaultUriBuilder(String uriTemplate) {
			this.uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		}

		private UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
			UriComponentsBuilder result;
			if (!StringUtils.hasLength(uriTemplate)) {
				result = (baseUri != null ? baseUri.cloneBuilder() : UriComponentsBuilder.newInstance());
			}
			else if (baseUri != null) {
				UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
				UriComponents uri = builder.build();
				result = (uri.getHost() == null ? baseUri.cloneBuilder().uriComponents(uri) : builder);
			}
			else {
				result = UriComponentsBuilder.fromUriString(uriTemplate);
			}
			if (encodingMode.equals(EncodingMode.TEMPLATE_AND_VALUES)) {
				result.encode();
			}
			parsePathIfNecessary(result);
			return result;
		}

		private void parsePathIfNecessary(UriComponentsBuilder result) {
			if (parsePath && encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				UriComponents uric = result.build();
				String path = uric.getPath();
				result.replacePath(null);
				for (String segment : uric.getPathSegments()) {
					result.pathSegment(segment);
				}
				if (path != null && path.endsWith("/")) {
					result.path("/");
				}
			}
		}


		@Override
		public DefaultUriBuilder scheme(@Nullable String scheme) {
			this.uriComponentsBuilder.scheme(scheme);
			return this;
		}

		@Override
		public DefaultUriBuilder userInfo(@Nullable String userInfo) {
			this.uriComponentsBuilder.userInfo(userInfo);
			return this;
		}

		@Override
		public DefaultUriBuilder host(@Nullable String host) {
			this.uriComponentsBuilder.host(host);
			return this;
		}

		@Override
		public DefaultUriBuilder port(int port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder port(@Nullable String port) {
			this.uriComponentsBuilder.port(port);
			return this;
		}

		@Override
		public DefaultUriBuilder path(String path) {
			this.uriComponentsBuilder.path(path);
			return this;
		}

		@Override
		public DefaultUriBuilder replacePath(@Nullable String path) {
			this.uriComponentsBuilder.replacePath(path);
			return this;
		}

		@Override
		public DefaultUriBuilder pathSegment(String... pathSegments) {
			this.uriComponentsBuilder.pathSegment(pathSegments);
			return this;
		}

		@Override
		public DefaultUriBuilder query(String query) {
			this.uriComponentsBuilder.query(query);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQuery(@Nullable String query) {
			this.uriComponentsBuilder.replaceQuery(query);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParam(String name, Object... values) {
			this.uriComponentsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParam(String name, @Nullable Collection<?> values) {
			this.uriComponentsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParamIfPresent(String name, Optional<?> value) {
			this.uriComponentsBuilder.queryParamIfPresent(name, value);
			return this;
		}

		@Override
		public DefaultUriBuilder queryParams(MultiValueMap<String, String> params) {
			this.uriComponentsBuilder.queryParams(params);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParam(String name, Object... values) {
			this.uriComponentsBuilder.replaceQueryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParam(String name, @Nullable Collection<?> values) {
			this.uriComponentsBuilder.replaceQueryParam(name, values);
			return this;
		}

		@Override
		public DefaultUriBuilder replaceQueryParams(MultiValueMap<String, String> params) {
			this.uriComponentsBuilder.replaceQueryParams(params);
			return this;
		}

		@Override
		public DefaultUriBuilder fragment(@Nullable String fragment) {
			this.uriComponentsBuilder.fragment(fragment);
			return this;
		}

		@Override
		public URI build(Map<String, ?> uriVars) {
			if (!defaultUriVariables.isEmpty()) {
				Map<String, Object> map = new HashMap<>();
				map.putAll(defaultUriVariables);
				map.putAll(uriVars);
				uriVars = map;
			}
			if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
				uriVars = UriUtils.encodeUriVariables(uriVars);
			}
			UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
			return createUri(uric);
		}

		@Override
		public URI build(Object... uriVars) {
			if (ObjectUtils.isEmpty(uriVars) && !defaultUriVariables.isEmpty()) {
				return build(Collections.emptyMap());
			}
			if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
				uriVars = UriUtils.encodeUriVariables(uriVars);
			}
			UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
			return createUri(uric);
		}

		private URI createUri(UriComponents uric) {
			if (encodingMode.equals(EncodingMode.URI_COMPONENT)) {
				uric = uric.encode();
			}
			return URI.create(uric.toString());
		}
	}

}
