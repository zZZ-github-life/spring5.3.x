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

package org.springframework.web.reactive.result.condition;

import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 请求映射条件的契约。
 *
 * <p>请求条件可以通过 {@link #combine(Object)} 组合，通过 {@link #getMatchingCondition(ServerWebExchange)} 与请求匹配，并通过 {@link #compareTo(Object, ServerWebExchange)} 相互比较，以确定哪个条件对给定请求更接近匹配。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 此 RequestCondition 可以与之组合
 * 并比较的对象类型
 */
public interface RequestCondition<T> {

	/**
	 * 将此条件与另一个条件组合，例如来自类型级和方法级 {@code @RequestMapping} 注解的条件。
	 * @param other 要与之组合的条件。
	 * @return 组合两个条件实例后
	 * 得到的请求条件实例。
	 */
	T combine(T other);

	/**
	 * 检查条件是否匹配请求，并返回为当前请求创建的潜在新实例。例如，包含多个 URL 模式的条件可能仅返回与请求匹配的那些模式的新实例。
	 * <p>对于 CORS 预检请求，条件应匹配将要执行的实际请求（例如 URL 模式、查询参数以及来自 "Access-Control-Request-Method" 头的 HTTP 方法）。如果条件无法匹配预检请求，则应返回内容为空的实例，从而不会导致匹配失败。
	 * @return 匹配时的条件实例，否则为 {@code null}。
	 */
	@Nullable
	T getMatchingCondition(ServerWebExchange exchange);

	/**
	 * 在特定请求的上下文中将此条件与另一个条件进行比较。此方法假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，以确保它们仅包含与当前请求相关的内容。
	 */
	int compareTo(T other, ServerWebExchange exchange);

}
