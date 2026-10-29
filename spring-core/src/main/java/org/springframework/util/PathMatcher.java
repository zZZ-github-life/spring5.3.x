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

/**
 * 基于 {@code String} 的路径匹配策略接口。
 *
 * <p>供 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}、
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping} 和
 * {@link org.springframework.web.servlet.mvc.WebContentInterceptor} 使用。
 *
 * <p>默认实现是 {@link AntPathMatcher}，支持 Ant 风格的模式语法。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AntPathMatcher
 */
public interface PathMatcher {

	/**
	 * 给定的 {@code path} 是否表示一个可由本接口实现进行匹配的模式？
	 * <p>如果返回值为 {@code false}，则无需使用 {@link #match} 方法，因为对静态路径字符串进行直接相等
	 * 比较会得到相同的结果。
	 * @param path the path to check
	 * @return {@code true} if the given {@code path} represents a pattern
	 */
	boolean isPattern(String path);

	/**
	 * 根据此 PathMatcher 的匹配策略，将给定的 {@code path} 与给定的 {@code pattern} 进行匹配。
	 * @param pattern the pattern to match against
	 * @param path the path to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	boolean match(String pattern, String path);

	/**
	 * 根据此 PathMatcher 的匹配策略，将给定的 {@code path} 与给定 {@code pattern} 的对应部分进行匹配。
	 * <p>判断模式至少在给定基础路径的范围内是否匹配，并假定完整路径随后也可能匹配。
	 * @param pattern the pattern to match against
	 * @param path the path to test
	 * @return {@code true} if the supplied {@code path} matched,
	 * {@code false} if it didn't
	 */
	boolean matchStart(String pattern, String path);

	/**
	 * 给定模式与完整路径，确定由模式映射的部分。
	 * <p>此方法应找出路径中通过实际模式动态匹配的部分，即从给定的完整路径中剥离静态定义的前导路径，仅
	 * 返回实际由模式匹配的部分。
	 * <p>例如：对于模式 "myroot/*.html" 和完整路径 "myroot/myfile.html"，此方法应返回 "myfile.html"。
	 * 详细的判定规则由该 PathMatcher 的匹配策略规定。
	 * <p>简单的实现可以在模式为实际模式时原样返回给定的完整路径，并在模式不包含任何动态部分（即
	 * {@code pattern} 参数是静态路径，不符合实际 {@link #isPattern pattern} 的条件）时返回空字符串。复
	 * 杂的实现会区分给定路径模式中的静态部分和动态部分。
	 * @param pattern the path pattern
	 * @param path the full path to introspect
	 * @return the pattern-mapped part of the given {@code path}
	 * (never {@code null})
	 */
	String extractPathWithinPattern(String pattern, String path);

	/**
	 * 给定模式与完整路径，提取 URI 模板变量。URI 模板变量通过花括号（'{' 和 '}'）表示。
	 * <p>例如：对于模式 "/hotels/{hotel}" 和路径 "/hotels/1"，此方法将返回包含 "hotel" &rarr; "1" 的映
	 * 射。
	 * @param pattern the path pattern, possibly containing URI templates
	 * @param path the full path to extract template variables from
	 * @return a map, containing variable names as keys; variables values as values
	 */
	Map<String, String> extractUriTemplateVariables(String pattern, String path);

	/**
	 * 给定完整路径，返回一个适合按该路径的明确程度对模式排序的 {@link Comparator}。
	 * <p>所使用的完整算法取决于底层实现，但通常返回的 {@code Comparator} 会通过
	 * {@linkplain java.util.List#sort(java.util.Comparator) sort} 对列表排序，使更具体的模式排在通用模
	 * 式之前。
	 * @param path the full path to use for comparison
	 * @return a comparator capable of sorting patterns in order of explicitness
	 */
	Comparator<String> getPatternComparator(String path);

	/**
	 * 将两个模式合并为一个返回的新模式。
	 * <p>用于合并两个模式的完整算法取决于底层实现。
	 * @param pattern1 the first pattern
	 * @param pattern2 the second pattern
	 * @return the combination of the two patterns
	 * @throws IllegalArgumentException when the two patterns cannot be combined
	 */
	String combine(String pattern1, String pattern2);

}
