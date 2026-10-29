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

package org.springframework.web.multipart.support;

import javax.servlet.ServletException;

import org.springframework.web.multipart.MultipartResolver;

/**
 * 当无法找到 "multipart/form-data" 请求中由名称标识的部分时抛出。
 *
 * <p>原因可能是该请求不是 multipart/form-data 请求、请求中不存在该部分，
 * 或者 Web 应用未正确配置以处理 multipart 请求，例如缺少 {@link MultipartResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MissingServletRequestPartException extends ServletException {

	private final String requestPartName;


	/**
	 * MissingServletRequestPartException 的构造函数。
	 * @param requestPartName the name of the missing part of the multipart request
	 */
	public MissingServletRequestPartException(String requestPartName) {
		super("Required request part '" + requestPartName + "' is not present");
		this.requestPartName = requestPartName;
	}


	/**
	 * 返回 multipart 请求中缺失的部分的名称。
	 */
	public String getRequestPartName() {
		return this.requestPartName;
	}

}
