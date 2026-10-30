/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.multipart;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * 此接口定义了针对实际 multipart 请求所暴露的 multipart 请求访问操作。
 * 由 {@link MultipartHttpServletRequest} 扩展。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 2.5.2
 */
public interface MultipartRequest {

	/**
	 * 返回一个包含此请求中 multipart 文件参数名称的 String 迭代器。
	 * 这些名称是表单的字段名（与普通参数类似），而不是原始文件名。
	 * @return 文件的名称
	 */
	Iterator<String> getFileNames();

	/**
	 * 返回此请求中某个上传文件的内容及描述，如果该文件不存在则返回 {@code null}。
	 * @param name 指定 multipart 文件参数名称的 String
	 * @return 以 {@link MultipartFile} 对象形式表示的上传内容
	 */
	@Nullable
	MultipartFile getFile(String name);

	/**
	 * 返回此请求中多个上传文件的内容及描述，如果不存在则返回空列表。
	 * @param name 指定 multipart 文件参数名称的 String
	 * @return 以 {@link MultipartFile} 列表形式表示的上传内容
	 * @since 3.0
	 */
	List<MultipartFile> getFiles(String name);

	/**
	 * 返回此请求中所含 multipart 文件的 {@link java.util.Map}。
	 * @return 以参数名称为键、以 {@link MultipartFile} 对象为值的映射
	 */
	Map<String, MultipartFile> getFileMap();

	/**
	 * 返回此请求中所含 multipart 文件的 {@link MultiValueMap}。
	 * @return 以参数名称为键、以 {@link MultipartFile} 对象列表为值的映射
	 * @since 3.0
	 */
	MultiValueMap<String, MultipartFile> getMultiFileMap();

	/**
	 * 确定指定请求部分（part）的内容类型。
	 * @param paramOrFileName part 的名称
	 * @return 关联的内容类型；如果未定义则返回 {@code null}
	 * @since 3.1
	 */
	@Nullable
	String getMultipartContentType(String paramOrFileName);

}
