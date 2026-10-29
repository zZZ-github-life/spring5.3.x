/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * URI 模板的表示形式，可通过 {@link #expand(Map)}、{@link #expand(Object[])} 用 URI 变量扩展，或通过 {@link #match(String)} 与 URL 匹配。此类设计为线程安全且可复用，允许任意数量的 expand 或 match 调用。<p><strong>注意：</strong>此类内部使用 {@link UriComponentsBuilder} 扩展 URI 模板，只是已准备好 URI 模板的快捷方式。对于更动态的准备和额外灵活性（例如围绕 URI 编码），请考虑使用 {@code UriComponentsBuilder}，或在其之上添加了多种编码模式的更高级别 {@link DefaultUriBuilderFactory}。更多细节请参见<a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#mvc-uri-building">参考文档</a>。
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 */
@SuppressWarnings("serial")
public class UriTemplate implements Serializable {

	private final String uriTemplate;

	private final UriComponents uriComponents;

	private final List<String> variableNames;

	private final Pattern matchPattern;


	/**
	 * 使用给定 URI String 构造新的 {@code UriTemplate}。
	 * @param uriTemplate the URI template string
	 */
	public UriTemplate(String uriTemplate) {
		Assert.hasText(uriTemplate, "'uriTemplate' must not be null");
		this.uriTemplate = uriTemplate;
		this.uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).build();

		TemplateInfo info = TemplateInfo.parse(uriTemplate);
		this.variableNames = Collections.unmodifiableList(info.getVariableNames());
		this.matchPattern = info.getMatchPattern();
	}


	/**
	 * 按顺序返回模板中变量的名称。
	 * @return the template variable names
	 */
	public List<String> getVariableNames() {
		return this.variableNames;
	}

	/**
	 * 给定变量的 Map，将此模板扩展为 URI。Map 的键表示变量名，Map 的值表示变量值。变量的顺序无关紧要。
	 * <p>示例：
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("https://example.com/hotels/{hotel}/bookings/{booking}");
	 * Map&lt;String, String&gt; uriVariables = new HashMap&lt;String, String&gt;();
	 * uriVariables.put("booking", "42");
	 * uriVariables.put("hotel", "Rest &amp; Relax");
	 * System.out.println(template.expand(uriVariables));
	 * </pre>
	 * 将打印：<blockquote>{@code https://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
	 * @param uriVariables the map of URI variables
	 * @return the expanded URI
	 * @throws IllegalArgumentException if {@code uriVariables} is {@code null};
	 * or if it does not contain values for all the variable names
	 */
	public URI expand(Map<String, ?> uriVariables) {
		UriComponents expandedComponents = this.uriComponents.expand(uriVariables);
		UriComponents encodedComponents = expandedComponents.encode();
		return encodedComponents.toUri();
	}

	/**
	 * 给定变量数组，将此模板扩展为完整 URI。数组表示变量值。变量的顺序很重要。
	 * <p>示例：
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("https://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.expand("Rest &amp; Relax", 42));
	 * </pre>
	 * 将打印：<blockquote>{@code https://example.com/hotels/Rest%20%26%20Relax/bookings/42}</blockquote>
	 * @param uriVariableValues the array of URI variables
	 * @return the expanded URI
	 * @throws IllegalArgumentException if {@code uriVariables} is {@code null}
	 * or if it does not contain sufficient variables
	 */
	public URI expand(Object... uriVariableValues) {
		UriComponents expandedComponents = this.uriComponents.expand(uriVariableValues);
		UriComponents encodedComponents = expandedComponents.encode();
		return encodedComponents.toUri();
	}

	/**
	 * 指示给定 URI 是否匹配此模板。
	 * @param uri the URI to match to
	 * @return {@code true} if it matches; {@code false} otherwise
	 */
	public boolean matches(@Nullable String uri) {
		if (uri == null) {
			return false;
		}
		Matcher matcher = this.matchPattern.matcher(uri);
		return matcher.matches();
	}

	/**
	 * 将给定 URI 匹配到变量值的 Map。返回 Map 中的键为变量名，值为给定 URI 中出现的变量值。
	 * <p>示例：
	 * <pre class="code">
	 * UriTemplate template = new UriTemplate("https://example.com/hotels/{hotel}/bookings/{booking}");
	 * System.out.println(template.match("https://example.com/hotels/1/bookings/42"));
	 * </pre>
	 * 将打印：<blockquote>{@code {hotel=1, booking=42}}</blockquote>
	 * @param uri the URI to match to
	 * @return a map of variable values
	 */
	public Map<String, String> match(String uri) {
		Assert.notNull(uri, "'uri' must not be null");
		Map<String, String> result = CollectionUtils.newLinkedHashMap(this.variableNames.size());
		Matcher matcher = this.matchPattern.matcher(uri);
		if (matcher.find()) {
			for (int i = 1; i <= matcher.groupCount(); i++) {
				String name = this.variableNames.get(i - 1);
				String value = matcher.group(i);
				result.put(name, value);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return this.uriTemplate;
	}


	/**
	 * 用于提取变量名和正则表达式以便与实际 URL 匹配的辅助类。
	 */
	private static final class TemplateInfo {

		private final List<String> variableNames;

		private final Pattern pattern;

		private TemplateInfo(List<String> vars, Pattern pattern) {
			this.variableNames = vars;
			this.pattern = pattern;
		}

		public List<String> getVariableNames() {
			return this.variableNames;
		}

		public Pattern getMatchPattern() {
			return this.pattern;
		}

		public static TemplateInfo parse(String uriTemplate) {
			int level = 0;
			List<String> variableNames = new ArrayList<>();
			StringBuilder pattern = new StringBuilder();
			StringBuilder builder = new StringBuilder();
			for (int i = 0 ; i < uriTemplate.length(); i++) {
				char c = uriTemplate.charAt(i);
				if (c == '{') {
					level++;
					if (level == 1) {
						// start of URI variable
						pattern.append(quote(builder));
						builder = new StringBuilder();
						continue;
					}
				}
				else if (c == '}') {
					level--;
					if (level == 0) {
						// end of URI variable
						String variable = builder.toString();
						int idx = variable.indexOf(':');
						if (idx == -1) {
							pattern.append("([^/]*)");
							variableNames.add(variable);
						}
						else {
							if (idx + 1 == variable.length()) {
								throw new IllegalArgumentException(
										"No custom regular expression specified after ':' in \"" + variable + "\"");
							}
							String regex = variable.substring(idx + 1);
							pattern.append('(');
							pattern.append(regex);
							pattern.append(')');
							variableNames.add(variable.substring(0, idx));
						}
						builder = new StringBuilder();
						continue;
					}
				}
				builder.append(c);
			}
			if (builder.length() > 0) {
				pattern.append(quote(builder));
			}
			return new TemplateInfo(variableNames, Pattern.compile(pattern.toString()));
		}

		private static String quote(StringBuilder builder) {
			return (builder.length() > 0 ? Pattern.quote(builder.toString()) : "");
		}
	}

}
