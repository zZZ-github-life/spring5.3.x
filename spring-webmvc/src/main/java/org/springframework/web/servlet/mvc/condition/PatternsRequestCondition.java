/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 逻辑析取（' || '）请求条件，用于将请求
 * 与一组 URL 路径模式进行匹配。
 *
 * <p>与使用解析后 {@link PathPattern} 的 {@link PathPatternsRequestCondition} 不同，
 * 此条件通过
 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} 进行字符串模式匹配。
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	private final static Set<String> EMPTY_PATH_PATTERN = Collections.singleton("");


	private final Set<String> patterns;

	private final PathMatcher pathMatcher;

	private final boolean useSuffixPatternMatch;

	private final boolean useTrailingSlashMatch;

	private final List<String> fileExtensions = new ArrayList<>();


	/**
	 * 使用 URL 模式的构造函数，必要时会在模式前添加 "/"。
	 * @param patterns 0 个或多个 URL 模式；无模式会得到一个空路径 {@code ""} 映射，
	 * 可匹配所有请求。
	 */
	public PatternsRequestCondition(String... patterns) {
		this(patterns, true, null);
	}

	/**
	 * {@link #PatternsRequestCondition(String...)} 的变体，带有
	 * {@link PathMatcher} 以及是否匹配尾部斜杠的标志。
	 * @since 5.3
	 */
	public PatternsRequestCondition(String[] patterns,  boolean useTrailingSlashMatch,
			@Nullable PathMatcher pathMatcher) {

		this(patterns, null, pathMatcher, useTrailingSlashMatch);
	}

	/**
	 * {@link #PatternsRequestCondition(String...)} 的变体，带有
	 * {@link UrlPathHelper} 和 {@link PathMatcher}，以及是否匹配
	 * 尾部斜杠。
	 * <p>从 5.3 开始，路径通过静态方法
	 * {@link UrlPathHelper#getResolvedLookupPath} 获取，不再需要传入
	 * {@code UrlPathHelper}。
	 * @since 5.2.4
	 * @deprecated as of 5.3 in favor of
	 * {@link #PatternsRequestCondition(String[], boolean, PathMatcher)}.
	 */
	@Deprecated
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useTrailingSlashMatch) {

		this(patterns, urlPathHelper, pathMatcher, false, useTrailingSlashMatch);
	}

	/**
	 * {@link #PatternsRequestCondition(String...)} 的变体，带有
	 * {@link UrlPathHelper} 和 {@link PathMatcher}，以及是否匹配
	 * 后缀和尾部斜杠的标志。
	 * <p>从 5.3 开始，路径通过静态方法
	 * {@link UrlPathHelper#getResolvedLookupPath} 获取，不再需要传入
	 * {@code UrlPathHelper}。
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}
	 * on the deprecation of path extension config options.
	 */
	@Deprecated
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch, boolean useTrailingSlashMatch) {

		this(patterns, urlPathHelper, pathMatcher, useSuffixPatternMatch, useTrailingSlashMatch, null);
	}

	/**
	 * {@link #PatternsRequestCondition(String...)} 的变体，带有
	 * {@link UrlPathHelper} 和 {@link PathMatcher}，以及是否匹配
	 * 后缀和尾部斜杠的标志，并指定特定的扩展名。
	 * <p>从 5.3 开始，路径通过静态方法
	 * {@link UrlPathHelper#getResolvedLookupPath} 获取，不再需要传入
	 * {@code UrlPathHelper}。
	 * @deprecated as of 5.2.4. See class-level note in
	 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}
	 * on the deprecation of path extension config options.
	 */
	@Deprecated
	public PatternsRequestCondition(String[] patterns, @Nullable UrlPathHelper urlPathHelper,
			@Nullable PathMatcher pathMatcher, boolean useSuffixPatternMatch,
			boolean useTrailingSlashMatch, @Nullable List<String> fileExtensions) {

		this.patterns = initPatterns(patterns);
		this.pathMatcher = pathMatcher != null ? pathMatcher : new AntPathMatcher();
		this.useSuffixPatternMatch = useSuffixPatternMatch;
		this.useTrailingSlashMatch = useTrailingSlashMatch;

		if (fileExtensions != null) {
			for (String fileExtension : fileExtensions) {
				if (fileExtension.charAt(0) != '.') {
					fileExtension = "." + fileExtension;
				}
				this.fileExtensions.add(fileExtension);
			}
		}
	}

	private static Set<String> initPatterns(String[] patterns) {
		if (!hasPattern(patterns)) {
			return EMPTY_PATH_PATTERN;
		}
		Set<String> result = new LinkedHashSet<>(patterns.length);
		for (String pattern : patterns) {
			if (StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	private static boolean hasPattern(String[] patterns) {
		if (!ObjectUtils.isEmpty(patterns)) {
			for (String pattern : patterns) {
				if (StringUtils.hasText(pattern)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 用于组合和匹配的私有构造函数。
	 */
	private PatternsRequestCondition(Set<String> patterns, PatternsRequestCondition other) {
		this.patterns = patterns;
		this.pathMatcher = other.pathMatcher;
		this.useSuffixPatternMatch = other.useSuffixPatternMatch;
		this.useTrailingSlashMatch = other.useTrailingSlashMatch;
		this.fileExtensions.addAll(other.fileExtensions);
	}


	public Set<String> getPatterns() {
		return this.patterns;
	}

	@Override
	protected Collection<String> getContent() {
		return this.patterns;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 此条件是否为 ""（空路径）映射。
	 */
	public boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}

	/**
	 * 返回不是模式的映射路径。
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		if (isEmptyPathMapping()) {
			return EMPTY_PATH_PATTERN;
		}
		Set<String> result = Collections.emptySet();
		for (String pattern : this.patterns) {
			if (!this.pathMatcher.isPattern(pattern)) {
				result = (result.isEmpty() ? new HashSet<>(1) : result);
				result.add(pattern);
			}
		}
		return result;
	}

	/**
	 * 返回一个新实例，其中包含当前实例（"this"）和
	 * "other" 实例的 URL 模式，规则如下：
	 * <ul>
	 * <li>如果两个实例都有模式，则使用
	 * {@link PathMatcher#combine(String, String)} 将 "this" 中的模式与
	 * "other" 中的模式组合起来。
	 * <li>如果只有一个实例有模式，则使用该实例的模式。
	 * <li>如果两个实例都没有模式，则使用空字符串（即 ""）。
	 * </ul>
	 */
	@Override
	public PatternsRequestCondition combine(PatternsRequestCondition other) {
		if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
			return this;
		}
		else if (other.isEmptyPathMapping()) {
			return this;
		}
		else if (isEmptyPathMapping()) {
			return other;
		}
		Set<String> result = new LinkedHashSet<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.pathMatcher.combine(pattern1, pattern2));
				}
			}
		}
		return new PatternsRequestCondition(result, this);
	}

	/**
	 * 检查是否有任何模式匹配给定请求，并返回一个
	 * 保证包含匹配模式且已通过
	 * {@link PathMatcher#getPatternComparator(String)} 排序的实例。
	 * <p>匹配模式按以下顺序进行检查获得：
	 * <ul>
	 * <li>直接匹配
	 * <li>如果模式中尚不包含 "."，则追加 ".*" 后进行模式匹配
	 * <li>模式匹配
	 * <li>如果模式尚未以 "/" 结尾，则追加 "/" 后进行模式匹配
	 * </ul>
	 * @param request 当前请求
	 * @return 如果条件不包含模式，则为同一实例；或包含已排序匹配模式的新条件；
	 * 如果无模式匹配，则为 {@code null}。
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		List<String> matches = getMatchingPatterns(lookupPath);
		return !matches.isEmpty() ? new PatternsRequestCondition(new LinkedHashSet<>(matches), this) : null;
	}

	/**
	 * 查找匹配给定查找路径的模式。调用此方法应
	 * 产生与调用 {@link #getMatchingCondition} 等价的结果。
	 * 此方法作为替代方案提供，用于没有可用请求的场景
	 * （例如内省、工具等）。
	 * @param lookupPath 要与现有模式匹配的查找路径
	 * @return 匹配模式的集合，按最接近的匹配排在前面排序
	 */
	public List<String> getMatchingPatterns(String lookupPath) {
		List<String> matches = null;
		for (String pattern : this.patterns) {
			String match = getMatchingPattern(pattern, lookupPath);
			if (match != null) {
				matches = (matches != null ? matches : new ArrayList<>());
				matches.add(match);
			}
		}
		if (matches == null) {
			return Collections.emptyList();
		}
		if (matches.size() > 1) {
			matches.sort(this.pathMatcher.getPatternComparator(lookupPath));
		}
		return matches;
	}

	@Nullable
	private String getMatchingPattern(String pattern, String lookupPath) {
		if (pattern.equals(lookupPath)) {
			return pattern;
		}
		if (this.useSuffixPatternMatch) {
			if (!this.fileExtensions.isEmpty() && lookupPath.indexOf('.') != -1) {
				for (String extension : this.fileExtensions) {
					if (this.pathMatcher.match(pattern + extension, lookupPath)) {
						return pattern + extension;
					}
				}
			}
			else {
				boolean hasSuffix = pattern.indexOf('.') != -1;
				if (!hasSuffix && this.pathMatcher.match(pattern + ".*", lookupPath)) {
					return pattern + ".*";
				}
			}
		}
		if (this.pathMatcher.match(pattern, lookupPath)) {
			return pattern;
		}
		if (this.useTrailingSlashMatch) {
			if (!pattern.endsWith("/") && this.pathMatcher.match(pattern + "/", lookupPath)) {
				return pattern + "/";
			}
		}
		return null;
	}

	/**
	 * 基于两个条件包含的 URL 模式进行比较。
	 * 模式从上到下逐一通过
	 * {@link PathMatcher#getPatternComparator(String)} 进行比较。如果所有比较的
	 * 模式都相同地匹配，但其中一个实例拥有更多模式，则认为
	 * 它更匹配。
	 * <p>假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，以确保它们
	 * 只包含匹配请求的模式，并按最匹配的排在前面
	 * 进行排序。
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, HttpServletRequest request) {
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(lookupPath);
		Iterator<String> iterator = this.patterns.iterator();
		Iterator<String> iteratorOther = other.patterns.iterator();
		while (iterator.hasNext() && iteratorOther.hasNext()) {
			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
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
