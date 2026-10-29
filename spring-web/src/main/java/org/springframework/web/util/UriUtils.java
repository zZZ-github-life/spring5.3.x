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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 基于 RFC 3986 的 URI 编码和解码工具方法。<p>有两类编码方法：<ul> <li>{@code "encodeXyz"} -- 这些方法通过百分号编码非法字符来编码特定的 URI 组件（例如路径、查询），非法字符包括非 US-ASCII 字符，以及 RFC 3986 中定义的、在给定 URI 组件类型内本来非法的字符。就编码而言，此方法的效果与使用 {@link URI} 的多参数构造函数相当。<li>{@code "encode"} 和 {@code "encodeUriVariables"} -- 这些方法可用于编码 URI 变量值，对 URI 中任何位置属于非法或具有保留含义的所有字符进行百分号编码。 </ul>
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
 */
public abstract class UriUtils {

	/**
	 * 使用给定编码对给定 URI scheme 进行编码。
	 * @param scheme the scheme to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded scheme
	 */
	public static String encodeScheme(String scheme, String encoding) {
		return encode(scheme, encoding, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * 使用给定编码对给定 URI scheme 进行编码。
	 * @param scheme the scheme to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded scheme
	 * @since 5.0
	 */
	public static String encodeScheme(String scheme, Charset charset) {
		return encode(scheme, charset, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * 使用给定编码对给定 URI 授权部分进行编码。
	 * @param authority the authority to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded authority
	 */
	public static String encodeAuthority(String authority, String encoding) {
		return encode(authority, encoding, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * 使用给定编码对给定 URI 授权部分进行编码。
	 * @param authority the authority to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded authority
	 * @since 5.0
	 */
	public static String encodeAuthority(String authority, Charset charset) {
		return encode(authority, charset, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * 使用给定编码对给定 URI 用户信息进行编码。
	 * @param userInfo the user info to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded user info
	 */
	public static String encodeUserInfo(String userInfo, String encoding) {
		return encode(userInfo, encoding, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * 使用给定编码对给定 URI 用户信息进行编码。
	 * @param userInfo the user info to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded user info
	 * @since 5.0
	 */
	public static String encodeUserInfo(String userInfo, Charset charset) {
		return encode(userInfo, charset, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * 使用给定编码对给定 URI 主机进行编码。
	 * @param host the host to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded host
	 */
	public static String encodeHost(String host, String encoding) {
		return encode(host, encoding, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * 使用给定编码对给定 URI 主机进行编码。
	 * @param host the host to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded host
	 * @since 5.0
	 */
	public static String encodeHost(String host, Charset charset) {
		return encode(host, charset, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * 使用给定编码对给定 URI 端口进行编码。
	 * @param port the port to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded port
	 */
	public static String encodePort(String port, String encoding) {
		return encode(port, encoding, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * 使用给定编码对给定 URI 端口进行编码。
	 * @param port the port to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded port
	 * @since 5.0
	 */
	public static String encodePort(String port, Charset charset) {
		return encode(port, charset, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * 使用给定编码对给定 URI 路径进行编码。
	 * @param path the path to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded path
	 */
	public static String encodePath(String path, String encoding) {
		return encode(path, encoding, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * 使用给定编码对给定 URI 路径进行编码。
	 * @param path the path to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded path
	 * @since 5.0
	 */
	public static String encodePath(String path, Charset charset) {
		return encode(path, charset, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * 使用给定编码对给定 URI 路径段进行编码。
	 * @param segment the segment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded segment
	 */
	public static String encodePathSegment(String segment, String encoding) {
		return encode(segment, encoding, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * 使用给定编码对给定 URI 路径段进行编码。
	 * @param segment the segment to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded segment
	 * @since 5.0
	 */
	public static String encodePathSegment(String segment, Charset charset) {
		return encode(segment, charset, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * 使用给定编码对给定 URI 查询进行编码。
	 * @param query the query to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query
	 */
	public static String encodeQuery(String query, String encoding) {
		return encode(query, encoding, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * 使用给定编码对给定 URI 查询进行编码。
	 * @param query the query to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded query
	 * @since 5.0
	 */
	public static String encodeQuery(String query, Charset charset) {
		return encode(query, charset, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * 使用给定编码对给定 URI 查询参数进行编码。
	 * @param queryParam the query parameter to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded query parameter
	 */
	public static String encodeQueryParam(String queryParam, String encoding) {
		return encode(queryParam, encoding, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * 使用给定编码对给定 URI 查询参数进行编码。
	 * @param queryParam the query parameter to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded query parameter
	 * @since 5.0
	 */
	public static String encodeQueryParam(String queryParam, Charset charset) {
		return encode(queryParam, charset, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * 使用 UTF-8 编码给定 {@code MultiValueMap} 中的查询参数。
	 * <p>当从已编码的模板构建 URI 时，可将其与 {@link UriComponentsBuilder#queryParams(MultiValueMap)} 一起使用。
	 * <pre class="code">{@code
	 * MultiValueMap<String, String> params = new LinkedMultiValueMap<>(2);
	 * // add to params...
	 *
	 * ServletUriComponentsBuilder.fromCurrentRequest()
	 *         .queryParams(UriUtils.encodeQueryParams(params))
	 *         .build(true)
	 *         .toUriString();
	 * }</pre>
	 * @param params the parameters to encode
	 * @return a new {@code MultiValueMap} with the encoded names and values
	 * @since 5.2.3
	 */
	public static MultiValueMap<String, String> encodeQueryParams(MultiValueMap<String, String> params) {
		Charset charset = StandardCharsets.UTF_8;
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>(params.size());
		for (Map.Entry<String, List<String>> entry : params.entrySet()) {
			for (String value : entry.getValue()) {
				result.add(encodeQueryParam(entry.getKey(), charset), encodeQueryParam(value, charset));
			}
		}
		return result;
	}

	/**
	 * 使用给定编码对给定 URI 片段进行编码。
	 * @param fragment the fragment to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded fragment
	 */
	public static String encodeFragment(String fragment, String encoding) {
		return encode(fragment, encoding, HierarchicalUriComponents.Type.FRAGMENT);
	}

	/**
	 * 使用给定编码对给定 URI 片段进行编码。
	 * @param fragment the fragment to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded fragment
	 * @since 5.0
	 */
	public static String encodeFragment(String fragment, Charset charset) {
		return encode(fragment, charset, HierarchicalUriComponents.Type.FRAGMENT);
	}


	/**
	 * 使用 String 字符集的 {@link #encode(String, Charset)} 变体。
	 * @param source the String to be encoded
	 * @param encoding the character encoding to encode to
	 * @return the encoded String
	 */
	public static String encode(String source, String encoding) {
		return encode(source, encoding, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * 按照 <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> 的定义，对 URI 中任何位置属于非法或具有保留含义的所有字符进行编码。这有助于确保给定 String 按原样保留，不会对 URI 的结构或含义产生任何影响。
	 * @param source the String to be encoded
	 * @param charset the character encoding to encode to
	 * @return the encoded String
	 * @since 5.0
	 */
	public static String encode(String source, Charset charset) {
		return encode(source, charset, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * 便捷方法，将 {@link #encode(String, Charset)} 应用于所有给定 URI 变量值。
	 * @param uriVariables the URI variable values to be encoded
	 * @return the encoded String
	 * @since 5.0
	 */
	public static Map<String, String> encodeUriVariables(Map<String, ?> uriVariables) {
		Map<String, String> result = CollectionUtils.newLinkedHashMap(uriVariables.size());
		uriVariables.forEach((key, value) -> {
			String stringValue = (value != null ? value.toString() : "");
			result.put(key, encode(stringValue, StandardCharsets.UTF_8));
		});
		return result;
	}

	/**
	 * 便捷方法，将 {@link #encode(String, Charset)} 应用于所有给定 URI 变量值。
	 * @param uriVariables the URI variable values to be encoded
	 * @return the encoded String
	 * @since 5.0
	 */
	public static Object[] encodeUriVariables(Object... uriVariables) {
		return Arrays.stream(uriVariables)
				.map(value -> {
					String stringValue = (value != null ? value.toString() : "");
					return encode(stringValue, StandardCharsets.UTF_8);
				})
				.toArray();
	}

	private static String encode(String scheme, String encoding, HierarchicalUriComponents.Type type) {
		return HierarchicalUriComponents.encodeUriComponent(scheme, encoding, type);
	}

	private static String encode(String scheme, Charset charset, HierarchicalUriComponents.Type type) {
		return HierarchicalUriComponents.encodeUriComponent(scheme, charset, type);
	}


	/**
	 * 解码给定的已编码 URI 组件。<p>解码规则请参见 {@link StringUtils#uriDecode(String, Charset)}。
	 * @param source the encoded String
	 * @param encoding the character encoding to use
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, String encoding) {
		return StringUtils.uriDecode(source, Charset.forName(encoding));
	}

	/**
	 * 解码给定的已编码 URI 组件。<p>解码规则请参见 {@link StringUtils#uriDecode(String, Charset)}。
	 * @param source the encoded String
	 * @param charset the character encoding to use
	 * @return the decoded value
	 * @throws IllegalArgumentException when the given source contains invalid encoded sequences
	 * @since 5.0
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, Charset charset) {
		return StringUtils.uriDecode(source, charset);
	}

	/**
	 * 从给定 URI 路径中提取文件扩展名。
	 * @param path the URI path (e.g. "/products/index.html")
	 * @return the extracted file extension (e.g. "html")
	 * @since 4.3.2
	 */
	@Nullable
	public static String extractFileExtension(String path) {
		int end = path.indexOf('?');
		int fragmentIndex = path.indexOf('#');
		if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
			end = fragmentIndex;
		}
		if (end == -1) {
			end = path.length();
		}
		int begin = path.lastIndexOf('/', end) + 1;
		int paramIndex = path.indexOf(';', begin);
		end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
		int extIndex = path.lastIndexOf('.', end);
		if (extIndex != -1 && extIndex >= begin) {
			return path.substring(extIndex + 1, end);
		}
		return null;
	}

}
