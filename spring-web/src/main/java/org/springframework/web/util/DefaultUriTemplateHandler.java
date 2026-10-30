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

package org.springframework.web.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * {@link UriTemplateHandler} 的默认实现，使用
 * {@link UriComponentsBuilder} 展开和编码变量。
 *
 * <p>还有若干属性可用于自定义 URI 模板的处理方式，包括用作所有
 * URI 模板前缀的 {@link #setBaseUrl baseUrl}，以及两个与编码相关的
 * 选项：{@link #setParsePath parsePath} 和
 * {@link #setStrictEncoding strictEncoding}。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 * @deprecated as of 5.0 in favor of {@link DefaultUriBuilderFactory}.
 * <p><strong>Note:</strong> {@link DefaultUriBuilderFactory} has a different
 * default for the {@link #setParsePath(boolean) parsePath} property (from
 * false to true).
 */
@Deprecated
public class DefaultUriTemplateHandler extends AbstractUriTemplateHandler {

	private boolean parsePath;

	private boolean strictEncoding;


	/**
	 * 是否将 URI 模板字符串的路径解析为路径段。
	 * <p>如果设置为 {@code true}，URI 模板路径会立即分解为路径段，
	 * 展开到其中的任何 URI 变量都会遵循路径段编码规则。实际上，
	 * 路径中的 URI 变量中的任何 "/" 字符都会被百分号编码。
	 * <p>默认设置为 {@code false}，此时路径保持为完整路径，
	 * 展开的 URI 变量会保留 "/" 字符。
	 * @param parsePath 是否将路径解析为路径段
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * 处理器是否配置为将路径解析为路径段。
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}

	/**
	 * 是否对
	 * <a href="https://tools.ietf.org/html/rfc3986#section-2">RFC 3986 第 2 节</a>
	 * 所定义的非保留字符集之外的字符进行编码。
	 * 这可确保 URI 变量值不包含任何具有保留用途的字符。
	 * <p>默认设置为 {@code false}，此时仅对给定 URI 组件非法的字符
	 * 进行编码。例如，将 URI 变量展开到路径段时，"/" 字符是非法的，
	 * 会被编码。而 ";" 字符虽然是保留用途，但它是合法的，
	 * 因此不会被编码。
	 * <p><strong>注意：</strong>此属性取代了同时设置
	 * {@link #setParsePath parsePath} 属性的需要。
	 * @param strictEncoding 是否执行严格编码
	 * @since 4.3
	 */
	public void setStrictEncoding(boolean strictEncoding) {
		this.strictEncoding = strictEncoding;
	}

	/**
	 * 是否对非保留字符集之外的任何字符进行严格编码。
	 */
	public boolean isStrictEncoding() {
		return this.strictEncoding;
	}


	@Override
	protected URI expandInternal(String uriTemplate, Map<String, ?> uriVariables) {
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		return createUri(uriComponents);
	}

	@Override
	protected URI expandInternal(String uriTemplate, Object... uriVariables) {
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		return createUri(uriComponents);
	}

	/**
	 * 根据 URI 模板字符串创建一个 {@code UriComponentsBuilder}。
	 * 此实现还会根据是否启用 {@link #setParsePath parsePath}
	 * 将路径拆分为路径段。
	 */
	protected UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
		if (shouldParsePath() && !isStrictEncoding()) {
			List<String> pathSegments = builder.build().getPathSegments();
			builder.replacePath(null);
			for (String pathSegment : pathSegments) {
				builder.pathSegment(pathSegment);
			}
		}
		return builder;
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		if (!isStrictEncoding()) {
			return builder.buildAndExpand(uriVariables).encode();
		}
		else {
			Map<String, ?> encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Object[] uriVariables) {
		if (!isStrictEncoding()) {
			return builder.buildAndExpand(uriVariables).encode();
		}
		else {
			Object[] encodedUriVars = UriUtils.encodeUriVariables(uriVariables);
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	private URI createUri(UriComponents uriComponents) {
		try {
			// Avoid further encoding (in the case of strictEncoding=true)
			return new URI(uriComponents.toUriString());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

}
