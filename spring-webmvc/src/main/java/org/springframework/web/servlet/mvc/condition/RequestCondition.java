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

package org.springframework.web.servlet.mvc.condition;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;

/**
 * 请求映射条件的约定。
 *
 * <p>请求条件可以通过 {@link #combine(Object)} 进行组合，
 * 通过 {@link #getMatchingCondition(HttpServletRequest)} 与请求进行匹配，
 * 并通过 {@link #compareTo(Object, HttpServletRequest)} 相互比较，
 * 以确定哪一个更匹配给定请求。
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 * @param <T> 该 RequestCondition 可与之组合和比较的对象类型
 */
public interface RequestCondition<T> {

	/**
	 * 将此条件与另一个条件组合，例如来自类型级别和方法级别
	 * {@code @RequestMapping} 注解的条件。
	 * @param other 要与之组合的条件。
	 * @return 两个条件实例组合后得到的请求条件实例。
	 */
	T combine(T other);

	/**
	 * 检查条件是否匹配请求，并返回一个可能为当前请求新建的实例。
	 * 例如，包含多个 URL 模式的条件可能仅返回一个
	 * 只包含匹配该请求的模式的实例。
	 * <p>对于 CORS 预检请求，条件应匹配实际将要发生的请求
	 * （例如 URL 模式、查询参数，以及来自
	 * "Access-Control-Request-Method" 头的 HTTP 方法）。如果某个条件无法
	 * 匹配预检请求，它应返回一个内容为空的实例，
	 * 从而不会导致匹配失败。
	 * @return 匹配时的条件实例，否则为 {@code null}。
	 */
	@Nullable
	T getMatchingCondition(HttpServletRequest request);

	/**
	 * 在特定请求的上下文中将此条件与另一个条件进行比较。
	 * 此方法假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，
	 * 以确保它们仅包含与当前请求相关的内容。
	 */
	int compareTo(T other, HttpServletRequest request);

}
