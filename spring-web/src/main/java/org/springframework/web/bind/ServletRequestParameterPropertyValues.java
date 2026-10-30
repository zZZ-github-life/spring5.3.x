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

package org.springframework.web.bind;

import javax.servlet.ServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.lang.Nullable;
import org.springframework.web.util.WebUtils;

/**
 * 由 ServletRequest 中的参数创建的 PropertyValues 实现。
 * 可以查找以某个前缀和前缀分隔符（默认为 "_"）开头的所有属性值。
 *
 * <p>例如，前缀为 "spring" 时，"spring_param1" 和 "spring_param2"
 * 将生成以 "param1" 和 "param2" 为键的 Map。
 *
 * <p>此类不是不可变的，以便能够高效地移除在绑定时应被忽略的属性值。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.util.WebUtils#getParametersStartingWith
 */
@SuppressWarnings("serial")
public class ServletRequestParameterPropertyValues extends MutablePropertyValues {

	/** 默认前缀分隔符。 */
	public static final String DEFAULT_PREFIX_SEPARATOR = "_";


	/**
	 * 使用无前缀（因此也没有前缀分隔符）创建新的 ServletRequestPropertyValues。
	 * @param request HTTP 请求
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request) {
		this(request, null, null);
	}

	/**
	 * 使用给定前缀和默认前缀分隔符（下划线 "_"）创建新的
	 * ServletRequestPropertyValues。
	 * @param request HTTP 请求
	 * @param prefix 参数前缀（完整前缀由此前缀加分隔符构成）
	 * @see #DEFAULT_PREFIX_SEPARATOR
	 */
	public ServletRequestParameterPropertyValues(ServletRequest request, @Nullable String prefix) {
		this(request, prefix, DEFAULT_PREFIX_SEPARATOR);
	}

	/**
	 * 同时提供前缀和前缀分隔符创建新的 ServletRequestPropertyValues。
	 * @param request HTTP 请求
	 * @param prefix 参数前缀（完整前缀由此前缀加分隔符构成）
	 * @param prefixSeparator 分隔前缀（例如 "spring"）与参数名其余部分（"param1"、"param2"）的分隔符
	 */
	public ServletRequestParameterPropertyValues(
			ServletRequest request, @Nullable String prefix, @Nullable String prefixSeparator) {

		super(WebUtils.getParametersStartingWith(
				request, (prefix != null ? prefix + prefixSeparator : null)));
	}

}
