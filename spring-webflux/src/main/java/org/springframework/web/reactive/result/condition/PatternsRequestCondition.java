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

package org.springframework.web.reactive.result.condition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 逻辑析取（' || '）请求条件，用于将请求与一组 URL 路径模式进行匹配。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public final class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {

	private static final SortedSet<PathPattern> EMPTY_PATH_PATTERN =
			new TreeSet<>(Collections.singleton(PathPatternParser.defaultInstance.parse("")));

	private static final Set<String> EMPTY_PATH = Collections.singleton("");


	private final SortedSet<PathPattern> patterns;


	/**
	 * 使用给定的 URL 模式创建新实例。
	 * @param patterns 0 个或多个 URL 模式；如果为 0 个，
	 * 则该条件将匹配所有请求。
	 */
	public PatternsRequestCondition(PathPattern... patterns) {
		this(ObjectUtils.isEmpty(patterns) ? Collections.emptyList() : Arrays.asList(patterns));
	}

	/**
	 * 使用给定的 URL 模式创建新实例。
	 */
	public PatternsRequestCondition(List<PathPattern> patterns) {
		this.patterns = (patterns.isEmpty() ? EMPTY_PATH_PATTERN : new TreeSet<>(patterns));
	}

	private PatternsRequestCondition(SortedSet<PathPattern> patterns) {
		this.patterns = patterns;
	}


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

	private boolean isEmptyPathMapping() {
		return this.patterns == EMPTY_PATH_PATTERN;
	}

	/**
	 * 返回不是模式的映射路径。
	 * @since 5.3
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
	 * 返回一个新实例，其中包含当前实例（"this"）和 "other" 实例的 URL 模式，规则如下：
	 * <ul>
	 * <li>如果两个实例都包含模式，则使用 {@link PathPattern#combine(PathPattern)} 将 "this" 中的模式与 "other" 中的模式组合。
	 * <li>如果只有一个实例包含模式，则使用这些模式。
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
		else {
			SortedSet<PathPattern> combined = new TreeSet<>();
			for (PathPattern pattern1 : this.patterns) {
				for (PathPattern pattern2 : other.patterns) {
					combined.add(pattern1.combine(pattern2));
				}
			}
			return new PatternsRequestCondition(combined);
		}
	}

	/**
	 * 检查是否有任何模式匹配给定请求，并返回一个保证包含匹配模式的实例，结果已排序。
	 * @param exchange 当前交换
	 * @return 如果该条件不包含任何模式，则返回同一实例；
	 * 否则返回包含已排序匹配模式的新条件；
	 * 如果没有模式匹配，则返回 {@code null}。
	 */
	@Override
	@Nullable
	public PatternsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		SortedSet<PathPattern> matches = getMatchingPatterns(exchange);
		return (matches != null ? new PatternsRequestCondition(matches) : null);
	}

	@Nullable
	private SortedSet<PathPattern> getMatchingPatterns(ServerWebExchange exchange) {
		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
		TreeSet<PathPattern> result = null;
		for (PathPattern pattern : this.patterns) {
			if (pattern.matches(lookupPath)) {
				result = (result != null ? result : new TreeSet<>());
				result.add(pattern);
			}
		}
		return result;
	}

	/**
	 * 根据所包含的 URL 模式比较两个条件。模式从上到下逐一比较。如果所有比较的模式都同样匹配，但一个实例具有更多模式，则认为它更接近匹配。
	 * <p>假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，以确保它们仅包含匹配请求的模式，并且最佳匹配排在前面。
	 */
	@Override
	public int compareTo(PatternsRequestCondition other, ServerWebExchange exchange) {
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
