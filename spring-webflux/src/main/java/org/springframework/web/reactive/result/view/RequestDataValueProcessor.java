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

package org.springframework.web.reactive.result.view;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于在视图渲染请求数据值（例如 URL 查询参数或表单字段值）之前，或在重定向之前，检查并可能修改这些值的契约。
 *
 * <p>实现可以例如将此契约用作提供数据完整性、机密性、跨站请求伪造（CSRF）防护等解决方案的一部分，或用于其他任务，例如自动向所有表单和 URL 添加隐藏字段。
 *
 * <p>支持此契约的视图技术可以通过 {@link RequestContext#getRequestDataValueProcessor()} 获取要委托的实例。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestDataValueProcessor {

	/**
	 * 在渲染新表单 action 时调用。
	 * @param exchange 当前交换
	 * @param action 表单的 action
	 * @param httpMethod 表单的 HTTP 方法
	 * @return 要使用的 action，可能已被修改
	 */
	String processAction(ServerWebExchange exchange, String action, String httpMethod);

	/**
	 * 在渲染表单字段值时调用。
	 * @param exchange 当前交换
	 * @param name 表单字段名称
	 * @param value 表单字段值
	 * @param type 表单字段类型（"text"、"hidden" 等）
	 * @return 要使用的表单字段值，可能已被修改
	 */
	String processFormFieldValue(ServerWebExchange exchange, String name, String value, String type);

	/**
	 * 在所有表单字段渲染后调用。
	 * @param exchange 当前交换
	 * @return 要添加的额外隐藏表单字段，或 {@code null}
	 */
	@Nullable
	Map<String, String> getExtraHiddenFields(ServerWebExchange exchange);

	/**
	 * 在即将渲染或重定向到 URL 时调用。
	 * @param exchange 当前交换
	 * @param url URL 值
	 * @return 要使用的 URL，可能已被修改
	 */
	String processUrl(ServerWebExchange exchange, String url);

}
