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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * 表示 URI 组件的不可变集合，将组件类型映射到 String 值。包含所有组件的便捷 getter。效果类似于 {@link java.net.URI}，但具有更强大的编码选项和对 URI 模板变量的支持。
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see UriComponentsBuilder
 */
@SuppressWarnings("serial")
public abstract class UriComponents implements Serializable {

	/** 捕获 URI 模板变量名称。 */
	private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");


	@Nullable
	private final String scheme;

	@Nullable
	private final String fragment;


	protected UriComponents(@Nullable String scheme, @Nullable String fragment) {
		this.scheme = scheme;
		this.fragment = fragment;
	}


	// Component getters

	/**
	 * 返回 scheme。可能为 {@code null}。
	 */
	@Nullable
	public final String getScheme() {
		return this.scheme;
	}

	/**
	 * 返回片段。可能为 {@code null}。
	 */
	@Nullable
	public final String getFragment() {
		return this.fragment;
	}

	/**
	 * 返回 scheme 特定部分。可能为 {@code null}。
	 */
	@Nullable
	public abstract String getSchemeSpecificPart();

	/**
	 * 返回用户信息。可能为 {@code null}。
	 */
	@Nullable
	public abstract String getUserInfo();

	/**
	 * 返回主机。可能为 {@code null}。
	 */
	@Nullable
	public abstract String getHost();

	/**
	 * 返回端口。如果未设置端口则为 {@code -1}。
	 */
	public abstract int getPort();

	/**
	 * 返回路径。可能为 {@code null}。
	 */
	@Nullable
	public abstract String getPath();

	/**
	 * 返回路径段列表。如果未设置路径则为空。
	 */
	public abstract List<String> getPathSegments();

	/**
	 * 返回查询。可能为 {@code null}。
	 */
	@Nullable
	public abstract String getQuery();

	/**
	 * 返回查询参数的 Map。如果未设置查询则为空。
	 */
	public abstract MultiValueMap<String, String> getQueryParams();


	/**
	 * 在扩展 URI 变量<em>之后</em>调用此方法，以对生成的 URI 组件值进行编码。<p>与 {@link UriComponentsBuilder#encode()} 相比，此方法<em>仅</em>替换非 ASCII 字符和非法（在给定 URI 组件类型内）字符，而不替换具有保留含义的字符。大多数情况下，{@link UriComponentsBuilder#encode()} 更可能给出预期结果。
	 * @see UriComponentsBuilder#encode()
	 */
	public final UriComponents encode() {
		return encode(StandardCharsets.UTF_8);
	}

	/**
	 * 使用非 "UTF-8" 字符集的 {@link #encode()} 变体。
	 * @param charset the charset to use for encoding
	 * @see UriComponentsBuilder#encode(Charset)
	 */
	public abstract UriComponents encode(Charset charset);

	/**
	 * 用给定 Map 中的值替换所有 URI 模板变量。<p>给定 Map 的键表示变量名；对应的值表示变量值。变量的顺序无关紧要。
	 * @param uriVariables the map of URI variables
	 * @return the expanded URI components
	 */
	public final UriComponents expand(Map<String, ?> uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(new MapTemplateVariables(uriVariables));
	}

	/**
	 * 用给定数组中的值替换所有 URI 模板变量。<p>给定数组表示变量值。变量的顺序很重要。
	 * @param uriVariableValues the URI variable values
	 * @return the expanded URI components
	 */
	public final UriComponents expand(Object... uriVariableValues) {
		Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");
		return expandInternal(new VarArgsTemplateVariables(uriVariableValues));
	}

	/**
	 * 用给定 {@link UriTemplateVariables} 中的值替换所有 URI 模板变量。
	 * @param uriVariables the URI template values
	 * @return the expanded URI components
	 */
	public final UriComponents expand(UriTemplateVariables uriVariables) {
		Assert.notNull(uriVariables, "'uriVariables' must not be null");
		return expandInternal(uriVariables);
	}

	/**
	 * 用给定 {@link UriTemplateVariables} 中的值替换所有 URI 模板变量。
	 * @param uriVariables the URI template values
	 * @return the expanded URI components
	 */
	abstract UriComponents expandInternal(UriTemplateVariables uriVariables);

	/**
	 * 规范化路径，移除 "path/.." 之类的序列。请注意，规范化应用于完整路径，而不是各个路径段。
	 * @see org.springframework.util.StringUtils#cleanPath(String)
	 */
	public abstract UriComponents normalize();

	/**
	 * 连接所有 URI 组件以返回完整形成的 URI String。<p>此方法无非是对当前 URI 组件值进行简单的 String 连接，因此结果可能包含非法 URI 字符，例如如果 URI 变量尚未扩展，或尚未通过 {@link UriComponentsBuilder#encode()} 或 {@link #encode()} 应用编码。
	 */
	public abstract String toUriString();

	/**
	 * 按如下方式从此实例创建 {@link URI}：<p>如果当前实例已{@link #encode() 编码}，则通过 {@link #toUriString()} 形成完整 URI String，然后将其传递给保留百分号编码的单参数 {@link URI} 构造函数。<p>如果尚未编码，则将各个 URI 组件值传递给多参数 {@link URI} 构造函数，该构造函数会对其各自 URI 组件中不能出现的非法字符进行转义。
	 */
	public abstract URI toUri();

	/**
	 * 对 {@link #toUriString()} 的简单传递。
	 */
	@Override
	public final String toString() {
		return toUriString();
	}

	/**
	 * 设置给定 UriComponentsBuilder 的所有组件。
	 * @since 4.2
	 */
	protected abstract void copyToUriComponentsBuilder(UriComponentsBuilder builder);


	// Static expansion helpers

	@Nullable
	static String expandUriComponent(@Nullable String source, UriTemplateVariables uriVariables) {
		return expandUriComponent(source, uriVariables, null);
	}

	@Nullable
	static String expandUriComponent(@Nullable String source, UriTemplateVariables uriVariables,
			@Nullable UnaryOperator<String> encoder) {

		if (source == null) {
			return null;
		}
		if (source.indexOf('{') == -1) {
			return source;
		}
		if (source.indexOf(':') != -1) {
			source = sanitizeSource(source);
		}
		Matcher matcher = NAMES_PATTERN.matcher(source);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			String match = matcher.group(1);
			String varName = getVariableName(match);
			Object varValue = uriVariables.getValue(varName);
			if (UriTemplateVariables.SKIP_VALUE.equals(varValue)) {
				continue;
			}
			String formatted = getVariableValueAsString(varValue);
			formatted = encoder != null ? encoder.apply(formatted) : Matcher.quoteReplacement(formatted);
			matcher.appendReplacement(sb, formatted);
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * 移除嵌套的 "{}"，例如带正则表达式的 URI 变量中的嵌套。
	 */
	private static String sanitizeSource(String source) {
		int level = 0;
		int lastCharIndex = 0;
		char[] chars = new char[source.length()];
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '{') {
				level++;
			}
			if (c == '}') {
				level--;
			}
			if (level > 1 || (level == 1 && c == '}')) {
				continue;
			}
			chars[lastCharIndex++] = c;
		}
		return new String(chars, 0, lastCharIndex);
	}

	private static String getVariableName(String match) {
		int colonIdx = match.indexOf(':');
		return (colonIdx != -1 ? match.substring(0, colonIdx) : match);
	}

	private static String getVariableValueAsString(@Nullable Object variableValue) {
		return (variableValue != null ? variableValue.toString() : "");
	}


	/**
	 * 定义 URI 模板变量的约定。
	 * @see HierarchicalUriComponents#expand
	 */
	public interface UriTemplateVariables {

		/**
		 * 常量，表示应忽略某个 URI 变量名并保持原样。这对只扩展部分（而非全部）URI 变量很有用。
		 */
		Object SKIP_VALUE = UriTemplateVariables.class;

		/**
		 * 获取给定 URI 变量名的值。如果值为 {@code null}，则扩展为空 String。如果值为 {@link #SKIP_VALUE}，则不扩展该 URI 变量。
		 * @param name the variable name
		 * @return the variable value, possibly {@code null} or {@link #SKIP_VALUE}
		 */
		@Nullable
		Object getValue(@Nullable String name);
	}


	/**
	 * 由 Map 支持的 URI 模板变量。
	 */
	private static class MapTemplateVariables implements UriTemplateVariables {

		private final Map<String, ?> uriVariables;

		public MapTemplateVariables(Map<String, ?> uriVariables) {
			this.uriVariables = uriVariables;
		}

		@Override
		@Nullable
		public Object getValue(@Nullable String name) {
			if (!this.uriVariables.containsKey(name)) {
				throw new IllegalArgumentException("Map has no value for '" + name + "'");
			}
			return this.uriVariables.get(name);
		}
	}


	/**
	 * 由可变参数数组支持的 URI 模板变量。
	 */
	private static class VarArgsTemplateVariables implements UriTemplateVariables {

		private final Iterator<Object> valueIterator;

		public VarArgsTemplateVariables(Object... uriVariableValues) {
			this.valueIterator = Arrays.asList(uriVariableValues).iterator();
		}

		@Override
		@Nullable
		public Object getValue(@Nullable String name) {
			if (!this.valueIterator.hasNext()) {
				throw new IllegalArgumentException("Not enough variable values available to expand '" + name + "'");
			}
			return this.valueIterator.next();
		}
	}

}
