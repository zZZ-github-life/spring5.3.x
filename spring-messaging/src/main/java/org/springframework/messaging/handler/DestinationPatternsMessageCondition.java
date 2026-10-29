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

package org.springframework.messaging.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.RouteMatcher;
import org.springframework.util.SimpleRouteMatcher;
import org.springframework.util.StringUtils;

/**
 * {@link MessageCondition}，用于通过 {@link RouteMatcher} 将 Message 的目标头
 * 与一个或多个模式进行匹配。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DestinationPatternsMessageCondition
		extends AbstractMessageCondition<DestinationPatternsMessageCondition> {

	/**
	 * “查找目标”头的名称。
	 */
	public static final String LOOKUP_DESTINATION_HEADER = "lookupDestination";


	private final Set<String> patterns;

	private final RouteMatcher routeMatcher;


	/**
	 * 仅使用模式的构造函数。创建并使用默认设置的 {@link AntPathMatcher} 实例。
	 * <p>不以 "/" 开头的非空模式会前置 "/"。
	 * @param patterns the URL patterns to match to, or if 0 then always match
	 */
	public DestinationPatternsMessageCondition(String... patterns) {
		this(patterns, (PathMatcher) null);
	}

	/**
	 * 使用模式和 {@code PathMatcher} 实例的构造函数。
	 * @param patterns the URL patterns to match to, or if 0 then always match
	 * @param matcher the {@code PathMatcher} to use
	 */
	public DestinationPatternsMessageCondition(String[] patterns, @Nullable PathMatcher matcher) {
		this(patterns, new SimpleRouteMatcher(matcher != null ? matcher : new AntPathMatcher()));
	}

	/**
	 * 使用模式和 {@code RouteMatcher} 实例的构造函数。
	 * @param patterns the URL patterns to match to, or if 0 then always match
	 * @param routeMatcher the {@code RouteMatcher} to use
	 * @since 5.2
	 */
	public DestinationPatternsMessageCondition(String[] patterns, RouteMatcher routeMatcher) {
		this(Collections.unmodifiableSet(prependLeadingSlash(patterns, routeMatcher)), routeMatcher);
	}

	private static Set<String> prependLeadingSlash(String[] patterns, RouteMatcher routeMatcher) {
		boolean slashSeparator = routeMatcher.combine("a", "a").equals("a/a");
		Set<String> result = new LinkedHashSet<>(patterns.length);
		for (String pattern : patterns) {
			if (slashSeparator && StringUtils.hasLength(pattern) && !pattern.startsWith("/")) {
				pattern = "/" + pattern;
			}
			result.add(pattern);
		}
		return result;
	}

	private DestinationPatternsMessageCondition(Set<String> patterns, RouteMatcher routeMatcher) {
		this.patterns = patterns;
		this.routeMatcher = routeMatcher;
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
	 * 返回一个新实例，其中的 URL 模式由当前实例（“this”）和“other”实例
	 * 按如下方式合并：
	 * <ul>
	 * <li>如果两个实例都有模式，使用
	 * {@link org.springframework.util.PathMatcher#combine(String, String)}
	 * 将“this”中的模式与“other”中的模式合并。
	 * <li>如果只有一个实例有模式，则使用它们。
	 * <li>如果两个实例都没有模式，则使用空字符串（即 ""）。
	 * </ul>
	 */
	@Override
	public DestinationPatternsMessageCondition combine(DestinationPatternsMessageCondition other) {
		Set<String> result = new LinkedHashSet<>();
		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
			for (String pattern1 : this.patterns) {
				for (String pattern2 : other.patterns) {
					result.add(this.routeMatcher.combine(pattern1, pattern2));
				}
			}
		}
		else if (!this.patterns.isEmpty()) {
			result.addAll(this.patterns);
		}
		else if (!other.patterns.isEmpty()) {
			result.addAll(other.patterns);
		}
		else {
			result.add("");
		}
		return new DestinationPatternsMessageCondition(result, this.routeMatcher);
	}

	/**
	 * 检查是否有任何模式与给定 Message 的目标匹配，并返回一个保证包含匹配模式的实例，
	 * 这些模式通过 {@link org.springframework.util.PathMatcher#getPatternComparator(String)}
	 * 排序。
	 * @param message the message to match to
	 * @return the same instance if the condition contains no patterns;
	 * or a new condition with sorted matching patterns;
	 * or {@code null} either if a destination can not be extracted or there is no match
	 */
	@Override
	@Nullable
	public DestinationPatternsMessageCondition getMatchingCondition(Message<?> message) {
		Object destination = message.getHeaders().get(LOOKUP_DESTINATION_HEADER);
		if (destination == null) {
			return null;
		}
		if (this.patterns.isEmpty()) {
			return this;
		}

		List<String> matches = null;
		for (String pattern : this.patterns) {
			if (pattern.equals(destination) || matchPattern(pattern, destination)) {
				if (matches == null) {
					matches = new ArrayList<>();
				}
				matches.add(pattern);
			}
		}
		if (CollectionUtils.isEmpty(matches)) {
			return null;
		}

		matches.sort(getPatternComparator(destination));
		return new DestinationPatternsMessageCondition(new LinkedHashSet<>(matches), this.routeMatcher);
	}

	private boolean matchPattern(String pattern, Object destination) {
		return destination instanceof RouteMatcher.Route ?
				this.routeMatcher.match(pattern, (RouteMatcher.Route) destination) :
				((SimpleRouteMatcher) this.routeMatcher).getPathMatcher().match(pattern, (String) destination);
	}

	private Comparator<String> getPatternComparator(Object destination) {
		return destination instanceof RouteMatcher.Route ?
			this.routeMatcher.getPatternComparator((RouteMatcher.Route) destination) :
			((SimpleRouteMatcher) this.routeMatcher).getPathMatcher().getPatternComparator((String) destination);
	}

	/**
	 * 根据两个条件包含的目标模式进行比较。
	 * 模式通过 {@link org.springframework.util.PathMatcher#getPatternComparator(String)}
	 * 从上到下逐个比较。如果所有比较的模式都同样匹配，但其中一个实例拥有更多模式，
	 * 则认为它更接近匹配。
	 * <p>假定两个实例都是通过 {@link #getMatchingCondition(Message)} 获取的，
	 * 以确保它们只包含与请求匹配的模式，并按最佳匹配在前的顺序排序。
	 */
	@Override
	public int compareTo(DestinationPatternsMessageCondition other, Message<?> message) {
		Object destination = message.getHeaders().get(LOOKUP_DESTINATION_HEADER);
		if (destination == null) {
			return 0;
		}

		Comparator<String> patternComparator = getPatternComparator(destination);
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
