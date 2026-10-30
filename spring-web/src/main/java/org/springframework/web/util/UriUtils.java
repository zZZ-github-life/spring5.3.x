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
	 * @param scheme 要编码的方案
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的方案
	 */
	public static String encodeScheme(String scheme, String encoding) {
		return encode(scheme, encoding, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * 使用给定编码对给定 URI scheme 进行编码。
	 * @param scheme 要编码的方案
	 * @param charset 要编码到的字符编码
	 * @return 编码后的 scheme
	 * @since 5.0
	 */
	public static String encodeScheme(String scheme, Charset charset) {
		return encode(scheme, charset, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * 使用给定编码对给定 URI 授权部分进行编码。
	 * @param authority 要编码的 authority
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的 authority
	 */
	public static String encodeAuthority(String authority, String encoding) {
		return encode(authority, encoding, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * 使用给定编码对给定 URI 授权部分进行编码。
	 * @param authority 要编码的 authority
	 * @param charset 要编码到的字符编码
	 * @return 编码后的 authority
	 * @since 5.0
	 */
	public static String encodeAuthority(String authority, Charset charset) {
		return encode(authority, charset, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * 使用给定编码对给定 URI 用户信息进行编码。
	 * @param userInfo 要编码的用户信息
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的用户信息
	 */
	public static String encodeUserInfo(String userInfo, String encoding) {
		return encode(userInfo, encoding, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * 使用给定编码对给定 URI 用户信息进行编码。
	 * @param userInfo 要编码的用户信息
	 * @param charset 要编码到的字符编码
	 * @return 编码后的用户信息
	 * @since 5.0
	 */
	public static String encodeUserInfo(String userInfo, Charset charset) {
		return encode(userInfo, charset, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * 使用给定编码对给定 URI 主机进行编码。
	 * @param host 要编码的主机
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的主机
	 */
	public static String encodeHost(String host, String encoding) {
		return encode(host, encoding, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * 使用给定编码对给定 URI 主机进行编码。
	 * @param host 要编码的主机
	 * @param charset 要编码到的字符编码
	 * @return 编码后的主机
	 * @since 5.0
	 */
	public static String encodeHost(String host, Charset charset) {
		return encode(host, charset, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * 使用给定编码对给定 URI 端口进行编码。
	 * @param port 要编码的端口
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的端口
	 */
	public static String encodePort(String port, String encoding) {
		return encode(port, encoding, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * 使用给定编码对给定 URI 端口进行编码。
	 * @param port 要编码的端口
	 * @param charset 要编码到的字符编码
	 * @return 编码后的端口
	 * @since 5.0
	 */
	public static String encodePort(String port, Charset charset) {
		return encode(port, charset, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * 使用给定编码对给定 URI 路径进行编码。
	 * @param path 要编码的路径
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的路径
	 */
	public static String encodePath(String path, String encoding) {
		return encode(path, encoding, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * 使用给定编码对给定 URI 路径进行编码。
	 * @param path 要编码的路径
	 * @param charset 要编码到的字符编码
	 * @return 编码后的路径
	 * @since 5.0
	 */
	public static String encodePath(String path, Charset charset) {
		return encode(path, charset, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * 使用给定编码对给定 URI 路径段进行编码。
	 * @param segment 要编码的路径段
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的路径段
	 */
	public static String encodePathSegment(String segment, String encoding) {
		return encode(segment, encoding, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * 使用给定编码对给定 URI 路径段进行编码。
	 * @param segment 要编码的路径段
	 * @param charset 要编码到的字符编码
	 * @return 编码后的路径段
	 * @since 5.0
	 */
	public static String encodePathSegment(String segment, Charset charset) {
		return encode(segment, charset, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * 使用给定编码对给定 URI 查询进行编码。
	 * @param query 要编码的查询
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的查询
	 */
	public static String encodeQuery(String query, String encoding) {
		return encode(query, encoding, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * 使用给定编码对给定 URI 查询进行编码。
	 * @param query 要编码的查询
	 * @param charset 要编码到的字符编码
	 * @return 编码后的查询
	 * @since 5.0
	 */
	public static String encodeQuery(String query, Charset charset) {
		return encode(query, charset, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * 使用给定编码对给定 URI 查询参数进行编码。
	 * @param queryParam 要编码的查询参数
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的查询参数
	 */
	public static String encodeQueryParam(String queryParam, String encoding) {
		return encode(queryParam, encoding, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * 使用给定编码对给定 URI 查询参数进行编码。
	 * @param queryParam 要编码的查询参数
	 * @param charset 要编码到的字符编码
	 * @return 编码后的查询参数
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
	 * @param params 要编码的参数
	 * @return 一个包含已编码名称和值的新的 {@code MultiValueMap}
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
	 * @param fragment 要编码的片段
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的片段
	 */
	public static String encodeFragment(String fragment, String encoding) {
		return encode(fragment, encoding, HierarchicalUriComponents.Type.FRAGMENT);
	}

	/**
	 * 使用给定编码对给定 URI 片段进行编码。
	 * @param fragment 要编码的片段
	 * @param charset 要编码到的字符编码
	 * @return 编码后的片段
	 * @since 5.0
	 */
	public static String encodeFragment(String fragment, Charset charset) {
		return encode(fragment, charset, HierarchicalUriComponents.Type.FRAGMENT);
	}


	/**
	 * 使用 String 字符集的 {@link #encode(String, Charset)} 变体。
	 * @param source 要编码的 String
	 * @param encoding 要编码到的字符编码
	 * @return 编码后的 String
	 */
	public static String encode(String source, String encoding) {
		return encode(source, encoding, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * 按照 <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> 的定义，对 URI 中任何位置属于非法或具有保留含义的所有字符进行编码。这有助于确保给定 String 按原样保留，不会对 URI 的结构或含义产生任何影响。
	 * @param source 要编码的 String
	 * @param charset 要编码到的字符编码
	 * @return 编码后的 String
	 * @since 5.0
	 */
	public static String encode(String source, Charset charset) {
		return encode(source, charset, HierarchicalUriComponents.Type.URI);
	}

	/**
	 * 便捷方法，将 {@link #encode(String, Charset)} 应用于所有给定 URI 变量值。
	 * @param uriVariables 要编码的 URI 变量值
	 * @return 编码后的 String
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
	 * @param uriVariables 要编码的 URI 变量值
	 * @return 编码后的 String
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
	 * @param source 编码后的 String
	 * @param encoding 要使用的字符编码
	 * @return 解码后的值
	 * @throws IllegalArgumentException 当给定的 source 包含无效的编码序列时
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, String encoding) {
		return StringUtils.uriDecode(source, Charset.forName(encoding));
	}

	/**
	 * 解码给定的已编码 URI 组件。<p>解码规则请参见 {@link StringUtils#uriDecode(String, Charset)}。
	 * @param source 编码后的 String
	 * @param charset 要使用的字符编码
	 * @return 解码后的值
	 * @throws IllegalArgumentException 当给定的 source 包含无效的编码序列时
	 * @since 5.0
	 * @see StringUtils#uriDecode(String, Charset)
	 * @see java.net.URLDecoder#decode(String, String)
	 */
	public static String decode(String source, Charset charset) {
		return StringUtils.uriDecode(source, charset);
	}

	/**
	 * 从给定 URI 路径中提取文件扩展名。
	 * @param path URI 路径（例如 "/products/index.html"）
	 * @return 提取出的文件扩展名（例如 "html"）
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
