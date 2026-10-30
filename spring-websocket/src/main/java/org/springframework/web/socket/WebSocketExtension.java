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

package org.springframework.web.socket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * 表示 RFC 6455 中定义的 WebSocket 扩展。
 * WebSocket 扩展为 WebSocket 协议添加协议特性。会话中使用的扩展
 * 在握手阶段协商确定，具体如下：
 * <ul>
 * <li>客户端可以在 HTTP 握手请求中请求特定的扩展</li>
 * <li>服务器响应当前会话中最终使用的扩展列表</li>
 * </ul>
 *
 * <p>WebSocket 扩展 HTTP 头可以包含参数，并遵循
 * <a href="https://tools.ietf.org/html/rfc7230#section-3.2">RFC 7230 第 3.2 节</a></p>
 *
 * <p>请注意，HTTP 头中扩展的顺序定义了它们的执行顺序，
 * 例如扩展 "foo, bar" 将按 "bar(foo(message))" 的顺序执行。</p>
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.0
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-9">WebSocket Protocol Extensions, RFC 6455 - Section 9</a>
 */
public class WebSocketExtension {

	private final String name;

	private final Map<String, String> parameters;


	/**
	 * 使用给定名称创建一个 WebSocketExtension。
	 * @param name 扩展的名称
	 */
	public WebSocketExtension(String name) {
		this(name, null);
	}

	/**
	 * 使用给定名称和参数创建一个 WebSocketExtension。
	 * @param name 扩展的名称
	 * @param parameters 参数
	 */
	public WebSocketExtension(String name, @Nullable Map<String, String> parameters) {
		Assert.hasLength(name, "Extension name must not be empty");
		this.name = name;
		if (!CollectionUtils.isEmpty(parameters)) {
			Map<String, String> map = new LinkedCaseInsensitiveMap<>(parameters.size(), Locale.ENGLISH);
			map.putAll(parameters);
			this.parameters = Collections.unmodifiableMap(map);
		}
		else {
			this.parameters = Collections.emptyMap();
		}
	}


	/**
	 * 返回扩展的名称（永远不会是 {@code null} 或空）。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回扩展的参数（永远不会是 {@code null}）。
	 */
	public Map<String, String> getParameters() {
		return this.parameters;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || !WebSocketExtension.class.isAssignableFrom(other.getClass())) {
			return false;
		}
		WebSocketExtension otherExt = (WebSocketExtension) other;
		return (this.name.equals(otherExt.name) && this.parameters.equals(otherExt.parameters));
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() * 31 + this.parameters.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append(this.name);
		this.parameters.forEach((key, value) -> str.append(';').append(key).append('=').append(value));
		return str.toString();
	}


	/**
	 * 将给定的逗号分隔字符串解析为 {@code WebSocketExtension} 对象列表。
	 * <p>此方法可用于解析 "Sec-WebSocket-Extension" 头。
	 * @param extensions 要解析的字符串
	 * @return 扩展列表
	 * @throws IllegalArgumentException 如果无法解析该字符串
	 */
	public static List<WebSocketExtension> parseExtensions(String extensions) {
		if (StringUtils.hasText(extensions)) {
			String[] tokens = StringUtils.tokenizeToStringArray(extensions, ",");
			List<WebSocketExtension> result = new ArrayList<>(tokens.length);
			for (String token : tokens) {
				result.add(parseExtension(token));
			}
			return result;
		}
		else {
			return Collections.emptyList();
		}
	}

	private static WebSocketExtension parseExtension(String extension) {
		if (extension.contains(",")) {
			throw new IllegalArgumentException("Expected single extension value: [" + extension + "]");
		}
		String[] parts = StringUtils.tokenizeToStringArray(extension, ";");
		String name = parts[0].trim();

		Map<String, String> parameters = null;
		if (parts.length > 1) {
			parameters = CollectionUtils.newLinkedHashMap(parts.length - 1);
			for (int i = 1; i < parts.length; i++) {
				String parameter = parts[i];
				int eqIndex = parameter.indexOf('=');
				if (eqIndex != -1) {
					String attribute = parameter.substring(0, eqIndex);
					String value = parameter.substring(eqIndex + 1);
					parameters.put(attribute, value);
				}
			}
		}

		return new WebSocketExtension(name, parameters);
	}

}
