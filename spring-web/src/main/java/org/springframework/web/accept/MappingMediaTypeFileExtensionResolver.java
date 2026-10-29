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

package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * {@code MediaTypeFileExtensionResolver} 的实现，双向维护文件扩展名与
 * MediaType 之间的查找关系。
 *
 * <p>最初使用文件扩展名与媒体类型的映射创建。
 * 之后子类可以使用 {@link #addMapping} 添加更多映射。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.2
 */
public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {

	private final ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<>(64);

	private final ConcurrentMap<MediaType, List<String>> fileExtensions = new ConcurrentHashMap<>(64);

	private final List<String> allFileExtensions = new CopyOnWriteArrayList<>();


	/**
	 * 使用给定的文件扩展名与媒体类型映射创建实例。
	 */
	public MappingMediaTypeFileExtensionResolver(@Nullable Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			Set<String> allFileExtensions = new HashSet<>(mediaTypes.size());
			mediaTypes.forEach((extension, mediaType) -> {
				String lowerCaseExtension = extension.toLowerCase(Locale.ENGLISH);
				this.mediaTypes.put(lowerCaseExtension, mediaType);
				addFileExtension(mediaType, lowerCaseExtension);
				allFileExtensions.add(lowerCaseExtension);
			});
			this.allFileExtensions.addAll(allFileExtensions);
		}
	}


	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	protected List<MediaType> getAllMediaTypes() {
		return new ArrayList<>(this.mediaTypes.values());
	}

	/**
	 * 将扩展名映射到 MediaType。如果扩展名已有映射则忽略。
	 */
	protected void addMapping(String extension, MediaType mediaType) {
		MediaType previous = this.mediaTypes.putIfAbsent(extension, mediaType);
		if (previous == null) {
			addFileExtension(mediaType, extension);
			this.allFileExtensions.add(extension);
		}
	}

	private void addFileExtension(MediaType mediaType, String extension) {
		this.fileExtensions.computeIfAbsent(mediaType, key -> new CopyOnWriteArrayList<>())
				.add(extension);
	}


	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		List<String> fileExtensions = this.fileExtensions.get(mediaType);
		return (fileExtensions != null ? fileExtensions : Collections.emptyList());
	}

	@Override
	public List<String> getAllFileExtensions() {
		return Collections.unmodifiableList(this.allFileExtensions);
	}

	/**
	 * 使用此方法进行从扩展名到 MediaType 的反向查找。
	 * @return a MediaType for the extension, or {@code null} if none found
	 */
	@Nullable
	protected MediaType lookupMediaType(String extension) {
		return this.mediaTypes.get(extension.toLowerCase(Locale.ENGLISH));
	}

}
