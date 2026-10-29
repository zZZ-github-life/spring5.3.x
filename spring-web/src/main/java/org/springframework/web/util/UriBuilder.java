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

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 用于准备并展开带有变量的 URI 模板的构建器风格方法。
 *
 * <p>实际上是对 {@link UriComponentsBuilder} 的泛化，但提供了直接展开为
 * {@link URI} 而非 {@link UriComponents} 的快捷方式，同时将编码偏好、
 * 基础 URI 等常见关注点留给实现去处理。
 *
 * <p>通常通过 {@link UriBuilderFactory} 获取，后者作为核心组件，只需配置一次
 * 即可用于创建多个 URL。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see UriBuilderFactory
 * @see UriComponentsBuilder
 */
public interface UriBuilder {

	/**
	 * 设置 URI scheme，其中可以包含 URI 模板变量；
	 * 也可以为 {@code null}，以清除此构建器的 scheme。
	 * @param scheme the URI scheme
	 */
	UriBuilder scheme(@Nullable String scheme);

	/**
	 * 设置 URI 用户信息，其中可以包含 URI 模板变量；
	 * 也可以为 {@code null}，以清除此构建器的用户信息。
	 * @param userInfo the URI user info
	 */
	UriBuilder userInfo(@Nullable String userInfo);

	/**
	 * 设置 URI 主机，其中可以包含 URI 模板变量；
	 * 也可以为 {@code null}，以清除此构建器的主机。
	 * @param host the URI host
	 */
	UriBuilder host(@Nullable String host);

	/**
	 * 设置 URI 端口。传入 {@code -1} 将清除此构建器的端口。
	 * @param port the URI port
	 */
	UriBuilder port(int port);

	/**
	 * 设置 URI 端口。仅当端口需要使用 URI 变量参数化时才使用此方法，
	 * 否则请使用 {@link #port(int)}。
	 * 传入 {@code null} 将清除此构建器的端口。
	 * @param port the URI port
	 */
	UriBuilder port(@Nullable String port);

	/**
	 * 追加到此构建器的路径。
	 * <p>给定值会原样追加到之前的 {@link #path(String) path} 值之后，
	 * 不插入任何额外的斜杠。例如：
	 * <pre class="code">
	 *
	 * builder.path("/first-").path("value/").path("/{id}").build("123")
	 *
	 * // 结果为 "/first-value/123"
	 * </pre>
	 * <p>相比之下，{@link #pathSegment(String...) pathSegment} 会在各个
	 * 路径段之间插入斜杠。例如：
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value").path("/")
	 *
	 * // 结果为 "/first-value/second-value/"
	 * </pre>
	 * <p>生成的完整路径会被规范化，以消除重复的斜杠。
	 * <p><strong>注意：</strong>当在 {@link #path(String) path} 中插入包含斜杠的
	 * URI 变量值时，这些斜杠是否会被编码取决于所配置的编码模式。有关详细信息，
	 * 请参阅 {@link UriComponentsBuilder#encode()}；如果通过 {@code WebClient}
	 * 或 {@code RestTemplate} 间接构建 URI，请参阅其
	 * {@link DefaultUriBuilderFactory#setEncodingMode encodingMode}。
	 * 另请参阅参考文档中的
	 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding">
	 * URI Encoding</a> 一节。
	 * @param path the URI path
	 */
	UriBuilder path(String path);

	/**
	 * 覆盖当前路径。
	 * @param path the URI path, or {@code null} for an empty path
	 */
	UriBuilder replacePath(@Nullable String path);

	/**
	 * 使用路径段追加到路径。例如：
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value", "{id}").build("123")
	 *
	 * // 结果为 "/first-value/second-value/123"
	 * </pre>
	 * <p>如果路径段中包含斜杠，则会对其进行编码：
	 * <pre class="code">
	 *
	 * builder.pathSegment("ba/z", "{id}").build("a/b")
	 *
	 * // 结果为 "/ba%2Fz/a%2Fb"
	 * </pre>
	 * 要插入末尾斜杠，请使用 {@link #path} 构建器方法：
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value").path("/")
	 *
	 * // 结果为 "/first-value/second-value/"
	 * </pre>
	 * <p>空的路径段会被忽略，因此生成的完整路径中不会出现重复的斜杠。
	 * @param pathSegments the URI path segments
	 */
	UriBuilder pathSegment(String... pathSegments) throws IllegalArgumentException;

	/**
	 * 将给定查询字符串解析为查询参数，参数之间以 {@code '&'} 分隔，
	 * 参数名与值（如果有）之间以 {@code '='} 分隔。
	 * 查询字符串中可以包含 URI 模板变量。
	 * <p><strong>注意：</strong>有关单个查询参数的处理和编码的更多说明，
	 * 请查阅 {@link #queryParam(String, Object...)} 的 Javadoc。
	 * @param query the query string
	 */
	UriBuilder query(String query);

	/**
	 * 清除现有的查询参数，然后委托给 {@link #query(String)}。
	 * <p><strong>注意：</strong>有关单个查询参数的处理和编码的更多说明，
	 * 请查阅 {@link #queryParam(String, Object...)} 的 Javadoc。
	 * @param query the query string; a {@code null} value removes all query parameters.
	 */
	UriBuilder replaceQuery(@Nullable String query);

	/**
	 * 追加给定查询参数。参数名和值都可以包含稍后从值中展开的 URI 模板变量。
	 * 如果未提供值，生成的 URI 将仅包含查询参数名，
	 * 例如 {@code "?foo"} 而不是 {@code "?foo=bar"}。
	 * <p><strong>注意：</strong>如果应用编码，则只会编码在查询参数名或值中非法的字符，
	 * 例如 {@code "="} 或 {@code "&"}。其他所有符合
	 * <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> 语法规则的字符
	 * 都不会被编码。这包括 {@code "+"}，它有时需要被编码，以避免被解释为编码后的空格。
	 * 可以通过使用 URI 模板变量并对变量值采用更严格的编码来应用更严格的编码。
	 * 有关详细信息，请阅读 Spring Framework 参考文档中的
	 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding">"URI Encoding"</a>
	 * 一节。
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @see #queryParam(String, Collection)
	 */
	UriBuilder queryParam(String name, Object... values);

	/**
	 * {@link #queryParam(String, Object...)} 的变体，接受 Collection。
	 * <p><strong>注意：</strong>有关单个查询参数的处理和编码的更多说明，
	 * 请查阅 {@link #queryParam(String, Object...)} 的 Javadoc。
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @since 5.2
	 * @see #queryParam(String, Object...)
	 */
	UriBuilder queryParam(String name, @Nullable Collection<?> values);

	/**
	 * 如果给定 {@link Optional} 有值，则委托给
	 * {@link #queryParam(String, Object...)} 或
	 * {@link #queryParam(String, Collection)}；如果为空，则完全不添加查询参数。
	 * @param name the query parameter name
	 * @param value an Optional, either empty or holding the query parameter value.
	 * @since 5.3
	 */
	UriBuilder queryParamIfPresent(String name, Optional<?> value);

	/**
	 * 添加多个查询参数和值。
	 * <p><strong>注意：</strong>有关单个查询参数的处理和编码的更多说明，
	 * 请查阅 {@link #queryParam(String, Object...)} 的 Javadoc。
	 * @param params the params
	 */
	UriBuilder queryParams(MultiValueMap<String, String> params);

	/**
	 * 设置查询参数值以替换现有值；如果未提供值，则移除该查询参数。
	 * <p><strong>注意：</strong>有关单个查询参数的处理和编码的更多说明，
	 * 请查阅 {@link #queryParam(String, Object...)} 的 Javadoc。
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @see #replaceQueryParam(String, Collection)
	 */
	UriBuilder replaceQueryParam(String name, Object... values);

	/**
	 * {@link #replaceQueryParam(String, Object...)} 的变体，接受 Collection。
	 * <p><strong>注意：</strong>有关单个查询参数的处理和编码的更多说明，
	 * 请查阅 {@link #queryParam(String, Object...)} 的 Javadoc。
	 * @param name the query parameter name
	 * @param values the query parameter values
	 * @since 5.2
	 * @see #replaceQueryParam(String, Object...)
	 */
	UriBuilder replaceQueryParam(String name, @Nullable Collection<?> values);

	/**
	 * 在移除所有现有查询参数值后设置查询参数值。
	 * <p><strong>注意：</strong>有关单个查询参数的处理和编码的更多说明，
	 * 请查阅 {@link #queryParam(String, Object...)} 的 Javadoc。
	 * @param params the query parameter name
	 */
	UriBuilder replaceQueryParams(MultiValueMap<String, String> params);

	/**
	 * 设置 URI 片段。给定片段可以包含 URI 模板变量，
	 * 也可以为 {@code null}，以清除此构建器的片段。
	 * @param fragment the URI fragment
	 */
	UriBuilder fragment(@Nullable String fragment);

	/**
	 * 构建 {@link URI} 实例，并使用数组中的值替换 URI 模板变量。
	 * @param uriVariables the map of URI variables
	 * @return the URI
	 */
	URI build(Object... uriVariables);

	/**
	 * 构建 {@link URI} 实例，并使用 Map 中的值替换 URI 模板变量。
	 * @param uriVariables the map of URI variables
	 * @return the URI
	 */
	URI build(Map<String, ?> uriVariables);

}
