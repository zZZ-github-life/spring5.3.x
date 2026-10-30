/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.multipart.support;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * MultipartHttpServletRequest 接口的抽象基类实现。
 * 提供对预生成的 MultipartFile 实例的管理。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 06.10.2003
 */
public abstract class AbstractMultipartHttpServletRequest extends HttpServletRequestWrapper
		implements MultipartHttpServletRequest {

	@Nullable
	private MultiValueMap<String, MultipartFile> multipartFiles;


	/**
	 * 将给定的 HttpServletRequest 包装为 MultipartHttpServletRequest。
	 * @param request 要包装的请求
	 */
	protected AbstractMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	public HttpServletRequest getRequest() {
		return (HttpServletRequest) super.getRequest();
	}

	@Override
	public HttpMethod getRequestMethod() {
		return HttpMethod.resolve(getRequest().getMethod());
	}

	@Override
	public HttpHeaders getRequestHeaders() {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<String> headerNames = getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			headers.put(headerName, Collections.list(getHeaders(headerName)));
		}
		return headers;
	}

	@Override
	public Iterator<String> getFileNames() {
		return getMultipartFiles().keySet().iterator();
	}

	@Override
	public MultipartFile getFile(String name) {
		return getMultipartFiles().getFirst(name);
	}

	@Override
	public List<MultipartFile> getFiles(String name) {
		List<MultipartFile> multipartFiles = getMultipartFiles().get(name);
		if (multipartFiles != null) {
			return multipartFiles;
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, MultipartFile> getFileMap() {
		return getMultipartFiles().toSingleValueMap();
	}

	@Override
	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		return getMultipartFiles();
	}

	/**
	 * 确定底层 multipart 请求是否已被解析。
	 * @return 已立即初始化或被延迟触发时返回 {@code true}；
	 * 如果延迟解析请求在任何参数或 multipart 文件被访问之前就已中止，
	 * 则返回 {@code false}
	 * @since 4.3.15
	 * @see #getMultipartFiles()
	 */
	public boolean isResolved() {
		return (this.multipartFiles != null);
	}


	/**
	 * 设置一个以参数名为键、以 MultipartFile 对象列表为值的 Map。
	 * 由子类在初始化时调用。
	 */
	protected final void setMultipartFiles(MultiValueMap<String, MultipartFile> multipartFiles) {
		this.multipartFiles =
				new LinkedMultiValueMap<>(Collections.unmodifiableMap(multipartFiles));
	}

	/**
	 * 获取用于检索的 MultipartFile Map，
	 * 必要时进行延迟初始化。
	 * @see #initializeMultipart()
	 */
	protected MultiValueMap<String, MultipartFile> getMultipartFiles() {
		if (this.multipartFiles == null) {
			initializeMultipart();
		}
		return this.multipartFiles;
	}

	/**
	 * 如果可能，延迟初始化 multipart 请求。
	 * 仅在尚未进行预先初始化时调用。
	 */
	protected void initializeMultipart() {
		throw new IllegalStateException("Multipart request not initialized");
	}

}
