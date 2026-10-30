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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}
 * 接口的默认实现。提供对预生成参数值的管理。
 *
 * <p>由 {@link org.springframework.web.multipart.commons.CommonsMultipartResolver} 使用。
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 29.09.2003
 * @see org.springframework.web.multipart.MultipartResolver
 */
public class DefaultMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

	private static final String CONTENT_TYPE = "Content-Type";

	@Nullable
	private Map<String, String[]> multipartParameters;

	@Nullable
	private Map<String, String> multipartParameterContentTypes;


	/**
	 * 将给定的 HttpServletRequest 包装为 MultipartHttpServletRequest。
	 * @param request 要包装的 Servlet 请求
	 * @param mpFiles multipart 文件的映射
	 * @param mpParams 要暴露的参数的映射，
	 * 以 String 为键、以 String 数组为值
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request, MultiValueMap<String, MultipartFile> mpFiles,
			Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

		super(request);
		setMultipartFiles(mpFiles);
		setMultipartParameters(mpParams);
		setMultipartParameterContentTypes(mpParamContentTypes);
	}

	/**
	 * 将给定的 HttpServletRequest 包装为 MultipartHttpServletRequest。
	 * @param request 要包装的 Servlet 请求
	 */
	public DefaultMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	@Nullable
	public String getParameter(String name) {
		String[] values = getMultipartParameters().get(name);
		if (values != null) {
			return (values.length > 0 ? values[0] : null);
		}
		return super.getParameter(name);
	}

	@Override
	public String[] getParameterValues(String name) {
		String[] parameterValues = super.getParameterValues(name);
		String[] mpValues = getMultipartParameters().get(name);
		if (mpValues == null) {
			return parameterValues;
		}
		if (parameterValues == null || getQueryString() == null) {
			return mpValues;
		}
		else {
			String[] result = new String[mpValues.length + parameterValues.length];
			System.arraycopy(mpValues, 0, result, 0, mpValues.length);
			System.arraycopy(parameterValues, 0, result, mpValues.length, parameterValues.length);
			return result;
		}
	}

	@Override
	public Enumeration<String> getParameterNames() {
		Map<String, String[]> multipartParameters = getMultipartParameters();
		if (multipartParameters.isEmpty()) {
			return super.getParameterNames();
		}

		Set<String> paramNames = new LinkedHashSet<>();
		paramNames.addAll(Collections.list(super.getParameterNames()));
		paramNames.addAll(multipartParameters.keySet());
		return Collections.enumeration(paramNames);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		Map<String, String[]> result = new LinkedHashMap<>();
		Enumeration<String> names = getParameterNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			result.put(name, getParameterValues(name));
		}
		return result;
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		MultipartFile file = getFile(paramOrFileName);
		if (file != null) {
			return file.getContentType();
		}
		else {
			return getMultipartParameterContentTypes().get(paramOrFileName);
		}
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		String contentType = getMultipartContentType(paramOrFileName);
		if (contentType != null) {
			HttpHeaders headers = new HttpHeaders();
			headers.add(CONTENT_TYPE, contentType);
			return headers;
		}
		else {
			return null;
		}
	}


	/**
	 * 设置一个以参数名为键、以 String 数组对象为值的 Map。
	 * 由子类在初始化时调用。
	 */
	protected final void setMultipartParameters(Map<String, String[]> multipartParameters) {
		this.multipartParameters = multipartParameters;
	}

	/**
	 * 获取用于检索的 multipart 参数 Map，
	 * 必要时进行延迟初始化。
	 * @see #initializeMultipart()
	 */
	protected Map<String, String[]> getMultipartParameters() {
		if (this.multipartParameters == null) {
			initializeMultipart();
		}
		return this.multipartParameters;
	}

	/**
	 * 设置一个以参数名为键、以内容类型 String 为值的 Map。
	 * 由子类在初始化时调用。
	 */
	protected final void setMultipartParameterContentTypes(Map<String, String> multipartParameterContentTypes) {
		this.multipartParameterContentTypes = multipartParameterContentTypes;
	}

	/**
	 * 获取用于检索的 multipart 参数内容类型 Map，
	 * 必要时进行延迟初始化。
	 * @see #initializeMultipart()
	 */
	protected Map<String, String> getMultipartParameterContentTypes() {
		if (this.multipartParameterContentTypes == null) {
			initializeMultipart();
		}
		return this.multipartParameterContentTypes;
	}

}
