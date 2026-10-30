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

package org.springframework.util;

import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.lang.Nullable;

/**
 * 各种 {@link MimeType} 工具方法。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Dimitrios Liapis
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class MimeTypeUtils {

	private static final byte[] BOUNDARY_CHARS =
			new byte[] {'-', '_', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
					'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A',
					'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U',
					'V', 'W', 'X', 'Y', 'Z'};

	/**
	 * 由 {@link #sortBySpecificity(List)} 使用的比较器。
	 */
	public static final Comparator<MimeType> SPECIFICITY_COMPARATOR = new MimeType.SpecificityComparator<>();

	/**
	 * 包含所有媒体范围的公共常量 MIME 类型（即 "&#42;/&#42;"）。
	 */
	public static final MimeType ALL;

	/**
	 * {@link MimeTypeUtils#ALL} 的 String 等价形式。
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 * {@code application/graphql+json} 的公共常量 MIME 类型。
	 * @since 5.3.19
	 * @see <a href="https://github.com/graphql/graphql-over-http">GraphQL over HTTP spec</a>
	 */
	public static final MimeType APPLICATION_GRAPHQL;

	/**
	 * {@link MimeTypeUtils#APPLICATION_GRAPHQL} 的 String 等价形式。
	 * @since 5.3.19
	 */
	public static final String APPLICATION_GRAPHQL_VALUE = "application/graphql+json";

	/**
	 * {@code application/json} 的公共常量 MIME 类型。
	 */
	public static final MimeType APPLICATION_JSON;

	/**
	 * {@link MimeTypeUtils#APPLICATION_JSON} 的 String 等价形式。
	 */
	public static final String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * {@code application/octet-stream} 的公共常量 MIME 类型。
	 *  */
	public static final MimeType APPLICATION_OCTET_STREAM;

	/**
	 * {@link MimeTypeUtils#APPLICATION_OCTET_STREAM} 的 String 等价形式。
	 */
	public static final String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * {@code application/xml} 的公共常量 MIME 类型。
	 */
	public static final MimeType APPLICATION_XML;

	/**
	 * {@link MimeTypeUtils#APPLICATION_XML} 的 String 等价形式。
	 */
	public static final String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * {@code image/gif} 的公共常量 MIME 类型。
	 */
	public static final MimeType IMAGE_GIF;

	/**
	 * {@link MimeTypeUtils#IMAGE_GIF} 的 String 等价形式。
	 */
	public static final String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * {@code image/jpeg} 的公共常量 MIME 类型。
	 */
	public static final MimeType IMAGE_JPEG;

	/**
	 * {@link MimeTypeUtils#IMAGE_JPEG} 的 String 等价形式。
	 */
	public static final String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * {@code image/png} 的公共常量 MIME 类型。
	 */
	public static final MimeType IMAGE_PNG;

	/**
	 * {@link MimeTypeUtils#IMAGE_PNG} 的 String 等价形式。
	 */
	public static final String IMAGE_PNG_VALUE = "image/png";

	/**
	 * {@code text/html} 的公共常量 MIME 类型。
	 *  */
	public static final MimeType TEXT_HTML;

	/**
	 * {@link MimeTypeUtils#TEXT_HTML} 的 String 等价形式。
	 */
	public static final String TEXT_HTML_VALUE = "text/html";

	/**
	 * {@code text/plain} 的公共常量 MIME 类型。
	 *  */
	public static final MimeType TEXT_PLAIN;

	/**
	 * {@link MimeTypeUtils#TEXT_PLAIN} 的 String 等价形式。
	 */
	public static final String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * {@code text/xml} 的公共常量 MIME 类型。
	 *  */
	public static final MimeType TEXT_XML;

	/**
	 * {@link MimeTypeUtils#TEXT_XML} 的 String 等价形式。
	 */
	public static final String TEXT_XML_VALUE = "text/xml";


	private static final ConcurrentLruCache<String, MimeType> cachedMimeTypes =
			new ConcurrentLruCache<>(64, MimeTypeUtils::parseMimeTypeInternal);

	@Nullable
	private static volatile Random random;

	static {
		// Not using "parseMimeType" to avoid static init cost
		ALL = new MimeType("*", "*");
		APPLICATION_GRAPHQL = new MimeType("application", "graphql+json");
		APPLICATION_JSON = new MimeType("application", "json");
		APPLICATION_OCTET_STREAM = new MimeType("application", "octet-stream");
		APPLICATION_XML = new MimeType("application", "xml");
		IMAGE_GIF = new MimeType("image", "gif");
		IMAGE_JPEG = new MimeType("image", "jpeg");
		IMAGE_PNG = new MimeType("image", "png");
		TEXT_HTML = new MimeType("text", "html");
		TEXT_PLAIN = new MimeType("text", "plain");
		TEXT_XML = new MimeType("text", "xml");
	}


	/**
	 * 将给定的 String 解析为单个 {@code MimeType}。
	 * 最近解析过的 {@code MimeType} 会被缓存以便后续获取。
	 * @param mimeType 要解析的字符串
	 * @return MIME 类型
	 * @throws InvalidMimeTypeException 如果字符串无法解析
	 */
	public static MimeType parseMimeType(String mimeType) {
		if (!StringUtils.hasLength(mimeType)) {
			throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
		}
		// do not cache multipart mime types with random boundaries
		if (mimeType.startsWith("multipart")) {
			return parseMimeTypeInternal(mimeType);
		}
		return cachedMimeTypes.get(mimeType);
	}

	private static MimeType parseMimeTypeInternal(String mimeType) {
		int index = mimeType.indexOf(';');
		String fullType = (index >= 0 ? mimeType.substring(0, index) : mimeType).trim();
		if (fullType.isEmpty()) {
			throw new InvalidMimeTypeException(mimeType, "'mimeType' must not be empty");
		}

		// java.net.HttpURLConnection returns a *; q=.2 Accept header
		if (MimeType.WILDCARD_TYPE.equals(fullType)) {
			fullType = "*/*";
		}
		int subIndex = fullType.indexOf('/');
		if (subIndex == -1) {
			throw new InvalidMimeTypeException(mimeType, "does not contain '/'");
		}
		if (subIndex == fullType.length() - 1) {
			throw new InvalidMimeTypeException(mimeType, "does not contain subtype after '/'");
		}
		String type = fullType.substring(0, subIndex);
		String subtype = fullType.substring(subIndex + 1);
		if (MimeType.WILDCARD_TYPE.equals(type) && !MimeType.WILDCARD_TYPE.equals(subtype)) {
			throw new InvalidMimeTypeException(mimeType, "wildcard type is legal only in '*/*' (all mime types)");
		}

		Map<String, String> parameters = null;
		do {
			int nextIndex = index + 1;
			boolean quoted = false;
			while (nextIndex < mimeType.length()) {
				char ch = mimeType.charAt(nextIndex);
				if (ch == ';') {
					if (!quoted) {
						break;
					}
				}
				else if (ch == '"') {
					quoted = !quoted;
				}
				nextIndex++;
			}
			String parameter = mimeType.substring(index + 1, nextIndex).trim();
			if (parameter.length() > 0) {
				if (parameters == null) {
					parameters = new LinkedHashMap<>(4);
				}
				int eqIndex = parameter.indexOf('=');
				if (eqIndex >= 0) {
					String attribute = parameter.substring(0, eqIndex).trim();
					String value = parameter.substring(eqIndex + 1).trim();
					parameters.put(attribute, value);
				}
			}
			index = nextIndex;
		}
		while (index < mimeType.length());

		try {
			return new MimeType(type, subtype, parameters);
		}
		catch (UnsupportedCharsetException ex) {
			throw new InvalidMimeTypeException(mimeType, "unsupported charset '" + ex.getCharsetName() + "'");
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidMimeTypeException(mimeType, ex.getMessage());
		}
	}

	/**
	 * 将以逗号分隔的字符串解析为 {@code MimeType} 对象的列表。
	 * @param mimeTypes 要解析的字符串
	 * @return MIME 类型列表
	 * @throws InvalidMimeTypeException 如果字符串无法解析
	 */
	public static List<MimeType> parseMimeTypes(String mimeTypes) {
		if (!StringUtils.hasLength(mimeTypes)) {
			return Collections.emptyList();
		}
		return tokenize(mimeTypes).stream()
				.filter(StringUtils::hasText)
				.map(MimeTypeUtils::parseMimeType)
				.collect(Collectors.toList());
	}

	/**
	 * 将给定的逗号分隔的 {@code MimeType} 对象字符串切分为 {@code List<String>}。与按 "," 进行简单切分
	 * 不同，此方法会考虑带引号的参数。
	 * @param mimeTypes 要标记化的字符串
	 * @return 标记列表
	 * @since 5.1.3
	 */
	public static List<String> tokenize(String mimeTypes) {
		if (!StringUtils.hasLength(mimeTypes)) {
			return Collections.emptyList();
		}
		List<String> tokens = new ArrayList<>();
		boolean inQuotes = false;
		int startIndex = 0;
		int i = 0;
		while (i < mimeTypes.length()) {
			switch (mimeTypes.charAt(i)) {
				case '"':
					inQuotes = !inQuotes;
					break;
				case ',':
					if (!inQuotes) {
						tokens.add(mimeTypes.substring(startIndex, i));
						startIndex = i + 1;
					}
					break;
				case '\\':
					i++;
					break;
			}
			i++;
		}
		tokens.add(mimeTypes.substring(startIndex));
		return tokens;
	}

	/**
	 * 返回给定 {@code MimeType} 对象列表的字符串表示形式。
	 * @param mimeTypes 要解析的字符串
	 * @return MIME 类型列表
	 * @throws IllegalArgumentException 如果该 String 无法解析
	 */
	public static String toString(Collection<? extends MimeType> mimeTypes) {
		StringBuilder builder = new StringBuilder();
		for (Iterator<? extends MimeType> iterator = mimeTypes.iterator(); iterator.hasNext();) {
			MimeType mimeType = iterator.next();
			mimeType.appendTo(builder);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}

	/**
	 * 按具体程度对给定的 {@code MimeType} 对象列表进行排序。
	 * <p>给定两个 MIME 类型：
	 * <ol>
	 * <li>如果任一 MIME 类型具有 {@linkplain MimeType#isWildcardType() 通配类型}，则不带通配符的 MIME 类型排在前。</li>
	 * <li>如果两个 MIME 类型具有不同的 {@linkplain MimeType#getType() 类型}，则认为二者相等，并保持当前顺序。</li>
	 * <li>如果任一 MIME 类型具有 {@linkplain MimeType#isWildcardSubtype() 通配子类型}，则不带通配符的 MIME 类型排在前。</li>
	 * <li>如果两个 MIME 类型具有不同的 {@linkplain MimeType#getSubtype() 子类型}，则认为二者相等，并保持当前顺序。</li>
	 * <li>如果两个 MIME 类型的 {@linkplain MimeType#getParameter(String) 参数}数量不同，则参数最多的 MIME 类型排在前。</li>
	 * </ol>
	 * <p>例如：
	 * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
	 * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
	 * <blockquote>audio/basic == text/html</blockquote>
	 * <blockquote>audio/basic == audio/wave</blockquote>
	 * @param mimeTypes 要排序的 MIME 类型列表
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-5.3.2">HTTP 1.1: Semantics
	 * and Content, section 5.3.2</a>
	 */
	public static void sortBySpecificity(List<MimeType> mimeTypes) {
		Assert.notNull(mimeTypes, "'mimeTypes' must not be null");
		if (mimeTypes.size() > 1) {
			mimeTypes.sort(SPECIFICITY_COMPARATOR);
		}
	}


	/**
	 * 为 {@link #generateMultipartBoundary()} 延迟初始化 {@link SecureRandom}。
	 */
	private static Random initRandom() {
		Random randomToUse = random;
		if (randomToUse == null) {
			synchronized (MimeTypeUtils.class) {
				randomToUse = random;
				if (randomToUse == null) {
					randomToUse = new SecureRandom();
					random = randomToUse;
				}
			}
		}
		return randomToUse;
	}

	/**
	 * 以字节形式生成随机 MIME 边界，常用于多部分 MIME 类型。
	 */
	public static byte[] generateMultipartBoundary() {
		Random randomToUse = initRandom();
		byte[] boundary = new byte[randomToUse.nextInt(11) + 30];
		for (int i = 0; i < boundary.length; i++) {
			boundary[i] = BOUNDARY_CHARS[randomToUse.nextInt(BOUNDARY_CHARS.length)];
		}
		return boundary;
	}

	/**
	 * 以 String 形式生成随机 MIME 边界，常用于多部分 MIME 类型。
	 */
	public static String generateMultipartBoundaryString() {
		return new String(generateMultipartBoundary(), StandardCharsets.US_ASCII);
	}

}
