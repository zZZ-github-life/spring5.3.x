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

package org.springframework.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 用于从 {@link Resource} 句柄或文件名解析 {@link MediaType} 对象的工厂委托。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 5.0
 */
public final class MediaTypeFactory {

	private static final String MIME_TYPES_FILE_NAME = "/org/springframework/http/mime.types";

	private static final MultiValueMap<String, MediaType> fileExtensionToMediaTypes = parseMimeTypes();


	private MediaTypeFactory() {
	}


	/**
	 * 解析资源中的 {@code mime.types} 文件。格式为：
	 * <code>
	 * # 注释以 '#' 开头<br>
	 * # 格式为 &lt;mime type> &lt;space separated file extensions><br>
	 * # 例如：<br>
	 * text/plain    txt text<br>
	 * # 这会将 file.txt 和 file.text 映射到<br>
	 * # MIME 类型 "text/plain"<br>
	 * </code>
	 * @return 多值映射，将媒体类型映射到文件扩展名。
	 */
	private static MultiValueMap<String, MediaType> parseMimeTypes() {
		InputStream is = MediaTypeFactory.class.getResourceAsStream(MIME_TYPES_FILE_NAME);
		Assert.state(is != null, MIME_TYPES_FILE_NAME + " not found in classpath");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.US_ASCII))) {
			MultiValueMap<String, MediaType> result = new LinkedMultiValueMap<>();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty() || line.charAt(0) == '#') {
					continue;
				}
				String[] tokens = StringUtils.tokenizeToStringArray(line, " \t\n\r\f");
				MediaType mediaType = MediaType.parseMediaType(tokens[0]);
				for (int i = 1; i < tokens.length; i++) {
					String fileExtension = tokens[i].toLowerCase(Locale.ENGLISH);
					result.add(fileExtension, mediaType);
				}
			}
			return result;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not read " + MIME_TYPES_FILE_NAME, ex);
		}
	}

	/**
	 * 如果可能，确定为给定资源确定的媒体类型。
	 * @param resource 要检查的资源
	 * @return 对应的媒体类型，如果未找到则为 {@code null}
	 */
	public static Optional<MediaType> getMediaType(@Nullable Resource resource) {
		return Optional.ofNullable(resource)
				.map(Resource::getFilename)
				.flatMap(MediaTypeFactory::getMediaType);
	}

	/**
	 * 如果可能，确定为给定文件名确定的媒体类型。
	 * @param filename 文件名加扩展名
	 * @return 对应的媒体类型，如果未找到则为 {@code null}
	 */
	public static Optional<MediaType> getMediaType(@Nullable String filename) {
		return getMediaTypes(filename).stream().findFirst();
	}

	/**
	 * 如果可能，确定为给定文件名确定的媒体类型列表。
	 * @param filename 文件名加扩展名
	 * @return 对应的媒体类型，如果未找到则为空列表
	 */
	public static List<MediaType> getMediaTypes(@Nullable String filename) {
		List<MediaType> mediaTypes = null;
		String ext = StringUtils.getFilenameExtension(filename);
		if (ext != null) {
			mediaTypes = fileExtensionToMediaTypes.get(ext.toLowerCase(Locale.ENGLISH));
		}
		return (mediaTypes != null ? mediaTypes : Collections.emptyList());
	}

}
