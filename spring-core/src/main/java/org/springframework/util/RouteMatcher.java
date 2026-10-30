/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.util;

import java.util.Comparator;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * 将路由与模式匹配的契约。
 *
 * <p>相当于 {@link PathMatcher}，但在来自传入消息的路由需要持续与大量消息处理器模式进行匹配的场景
 * 中，出于效率考虑，允许使用路由和模式的已解析表示。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see PathMatcher
 */
public interface RouteMatcher {

	/**
	 * 返回给定路由的已解析表示。
	 * @param routeValue 要解析的路由
	 * @return 路由的解析后表示形式
	 */
	Route parseRoute(String routeValue);

	/**
	 * 给定的 {@code route} 是否包含需要 {@link #match(String, Route)} 方法的模式语法，或者它是否是可直
	 * 接与其他字符串比较的常规字符串。
	 * @param route 要检查的路由
	 * @return 如果给定的 {@code route} 表示模式，则返回 {@code true}
	 */
	boolean isPattern(String route);

	/**
	 * 将两个模式合并为单个模式。
	 * @param pattern1 第一个模式
	 * @param pattern2 第二个模式
	 * @return 两个模式的组合
	 * @throws IllegalArgumentException 当两个模式无法组合时
	 */
	String combine(String pattern1, String pattern2);

	/**
	 * 将给定的路由与给定的模式进行匹配。
	 * @param pattern 要尝试匹配的模式
	 * @param route 要与之测试的路由
	 * @return 如果匹配则返回 {@code true}，否则返回 {@code false}
	 */
	boolean match(String pattern, Route route);

	/**
	 * 将模式与路由进行匹配并提取模板变量。
	 * @param pattern 模式，可能包含模板变量
	 * @param route 要从中提取模板变量的路由
	 * @return 包含模板变量及其值的映射
	 */
	@Nullable
	Map<String, String> matchAndExtract(String pattern, Route route);

	/**
	 * 给定路由，返回一个适合按该路由的明确程度对模式排序的 {@link Comparator}，使更具体的模式排在更通
	 * 用的模式之前。
	 * @param route 用于比较的完整路径
	 * @return 一个能够按明确程度对模式排序的比较器
	 */
	Comparator<String> getPatternComparator(Route route);


	/**
	 * 路由的已解析表示。
 	 */
	interface Route {

		/**
		 * 原始路由值。
		 */
		String value();
	}

}
