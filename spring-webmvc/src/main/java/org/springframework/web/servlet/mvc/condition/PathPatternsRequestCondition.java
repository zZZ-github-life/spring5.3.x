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

package org.springframework.web.servlet.mvc.condition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 逻辑析取（' || '）请求条件，用于将请求
 * 与一组 URL 路径模式进行匹配。
 *
 * <p>与 {@link PatternsRequestCondition} 不同，此条件使用
 * 解析后的 {@link PathPattern}，而不是通过
 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} 进行字符串模式匹配。
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public final class PathPatternsRequestCondition extends AbstractRequestCondition<PathPatternsRequestCondition> {

	private static final SortedSet<PathPattern> EMPTY_PATH_PATTERN =
			new TreeSet<>(Collections.singleton(new PathPatternParser().parse("")));

	private static final Set<String> EMPTY_PATH = Collections.singleton("");


	private final SortedSet<PathPattern> patterns;


	/**
	 * 默认构造函数，结果为 {@code ""}（空路径）映射。
	 */
	public PathPatternsRequestCondition() {
		this(EMPTY_PATH_PATTERN);
	}

	/**
	 * 使用指定模式的构造函数。
	 */
	public PathPatternsRequestCondition(PathPatternParser parser, String... patterns) {
		this(parse(parser, patterns));
	}

	private static SortedSet<PathPattern> parse(PathPatternParser parser, String... patterns) {
		if (patterns.length == 0 || (patterns.length == 1 && !StringUtils.hasText(patterns[0]))) {
			return EMPTY_PATH_PATTERN;
		}
		SortedSet<PathPattern> result = new TreeSet<>();
		for (String path : patterns) {
			if (StringUtils.hasText(path) && !path.startsWith("/")) {
				path = "/" + path;
			}
			result.add(parser.parse(path));
		}
		return result;
	}

	private PathPatternsRequestCondition(SortedSet<PathPattern> patterns) {
		this.patterns = patterns;
	}

	/**
	 * 返回此条件中的模式。如果只需要第一个（最上面的）模式，
	 * 请使用 {@link #getFirstPattern()}。
	 */
	public Set<PathPattern> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<PathPattern> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 返回第一个模式。
	 */
	public PathPattern getFirstPattern() {
		return this.patterns.first();
	}

	/**
	 * 此条件是否为 ""（空路径）映射。
	 */
	public boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}

	/**
	 * 返回不是模式的映射路径。
	 */
	public Set<String> getDirectPaths() {
		if (isEmptyPathMapping()) {
			return EMPTY_PATH;
		}
		Set<String> result = Collections.emptySet();
		for (PathPattern pattern : this.patterns) {
			if (!pattern.hasPatternSyntax()) {
				result = (result.isEmpty() ? new HashSet<>(1) : result);
				result.add(pattern.getPatternString());
			}
		}
		return result;
	}

	/**
	 * 返回映射为字符串的 {@link #getPatterns()}。
	 */
	public Set<String> getPatternValues() {
		return (isEmptyPathMapping() ? EMPTY_PATH :
				getPatterns().stream().map(PathPattern::getPatternString).collect(Collectors.toSet()));
	}

	/**
	 * 返回一个新实例，其中包含当前实例（"this"）和 "other" 实例的
	 * URL 模式，规则如下：
	 * <ul>
	 * <li>如果两个实例都有模式，则使用
	 * {@link PathPattern#combine(PathPattern)} 将 "this" 中的模式与
	 * "other" 中的模式组合起来。
	 * <li>如果只有一个实例有模式，则使用该实例的模式。
	 * <li>如果两个实例都没有模式，则使用空字符串（即 ""）。
	 * </ul>
	 */
	@Override
	public PathPatternsRequestCondition combine(PathPatternsRequestCondition other) {
		if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
			return this;
		}
		else if (other.isEmptyPathMapping()) {
			return this;
		}
		else if (isEmptyPathMapping()) {
			return other;
		}
		else {
			SortedSet<PathPattern> combined = new TreeSet<>();
			for (PathPattern pattern1 : this.patterns) {
				for (PathPattern pattern2 : other.patterns) {
					combined.add(pattern1.combine(pattern2));
				}
			}
			return new PathPatternsRequestCondition(combined);
		}
	}

	/**
	 * 检查是否有任何模式匹配给定请求，并返回一个
	 * 保证包含匹配模式且已排序的实例。
	 * @param request the current request
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} if no patterns match.
	 */
	@Override
	@Nullable
	public PathPatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
		SortedSet<PathPattern> matches = getMatchingPatterns(path);
		return (matches != null ? new PathPatternsRequestCondition(matches) : null);
	}

	@Nullable
	private SortedSet<PathPattern> getMatchingPatterns(PathContainer path) {
		TreeSet<PathPattern> result = null;
		for (PathPattern pattern : this.patterns) {
			if (pattern.matches(path)) {
				result = (result != null ? result : new TreeSet<>());
				result.add(pattern);
			}
		}
		return result;
	}

	/**
	 * 基于两个条件包含的 URL 模式进行比较。
	 * 模式从上到下逐一比较。如果所有比较的模式
	 * 都相同地匹配，但其中一个实例拥有更多模式，则认为
	 * 它更匹配。
	 * <p>假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，以确保它们
	 * 只包含匹配请求的模式，并按最匹配的排在前面
	 * 进行排序。
	 */
	@Override
	public int compareTo(PathPatternsRequestCondition other, HttpServletRequest request) {
		Iterator<PathPattern> iterator = this.patterns.iterator();
		Iterator<PathPattern> iteratorOther = other.getPatterns().iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = PathPattern.SPECIFICITY_COMPARATOR.compare(iterator.next(), iteratorOther.next());
			if (result != 0) {
				return result;
			}
		}
		if (iterator.hasNext()) {
			return -1;
		}
		else if (iteratorOther.hasNext()) {
			return 1;
		}
		else {
			return 0;
		}
	}

}
