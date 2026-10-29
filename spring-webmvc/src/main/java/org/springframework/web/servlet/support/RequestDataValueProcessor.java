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

package org.springframework.web.servlet.support;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * 用于在视图渲染之前或在重定向之前检查并可能修改请求数据值（例如 URL 查询参数或表单字段值）的契约。
 *
 * <p>实现可以将此契约用于例如提供数据完整性、机密性、跨站请求伪造（CSRF）防护等解决方案的一部分，或用于其他任务，例如自动向所有表单和 URL 添加隐藏字段。
 *
 * <p>支持此契约的视图技术可以通过 {@link RequestContext#getRequestDataValueProcessor()} 获取要委托的实例。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface RequestDataValueProcessor {

	/**
	 * 在渲染新的表单 action 时调用。
	 * @param request the current request
	 * @param action the form action
	 * @param httpMethod the form HTTP method
	 * @return the action to use, possibly modified
	 */
	String processAction(HttpServletRequest request, String action, String httpMethod);

	/**
	 * 在渲染表单字段值时调用。
	 * @param request the current request
	 * @param name the form field name (if any)
	 * @param value the form field value
	 * @param type the form field type ("text", "hidden", etc.)
	 * @return the form field value to use, possibly modified
	 */
	String processFormFieldValue(HttpServletRequest request, @Nullable String name, String value, String type);

	/**
	 * 在所有表单字段渲染完成后调用。
	 * @param request the current request
	 * @return additional hidden form fields to be added, or {@code null} if none
	 */
	@Nullable
	Map<String, String> getExtraHiddenFields(HttpServletRequest request);

	/**
	 * 在即将渲染 URL 或重定向到 URL 时调用。
	 * @param request the current request
	 * @param url the URL value
	 * @return the URL to use, possibly modified
	 */
	String processUrl(HttpServletRequest request, String url);

}
