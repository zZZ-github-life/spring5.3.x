/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;

import org.springframework.lang.Nullable;

/**
 * 一个简单的实例过滤器，根据 includes 和 excludes 元素集合检查给定实例是否匹配。
 *
 * <p>子类可能希望重写 {@link #match(Object, Object)} 以提供自定义匹配算法。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <T> the instance type
 */
public class InstanceFilter<T> {

	private final Collection<? extends T> includes;

	private final Collection<? extends T> excludes;

	private final boolean matchIfEmpty;


	/**
	 * 基于 includes/excludes 集合创建一个新实例。
	 *
	 * <p>如果某个元素与 includes 列表中的某个元素"匹配"，并且不与 excludes 列表中的任何元素匹配，则该
	 * 元素将匹配。
	 *
	 * <p>子类可以重新定义匹配的含义。默认情况下，如果一个元素根据 {@link Object#equals(Object)} 与另一
	 * 个元素相等，则二者匹配。
	 *
	 * <p>如果两个集合都为空，则由 {@code matchIfEmpty} 决定元素是否匹配。
	 * @param includes the collection of includes
	 * @param excludes the collection of excludes
	 * @param matchIfEmpty the matching result if both the includes and the excludes
	 * collections are empty
	 */
	public InstanceFilter(@Nullable Collection<? extends T> includes,
			@Nullable Collection<? extends T> excludes, boolean matchIfEmpty) {

		this.includes = (includes != null ? includes : Collections.emptyList());
		this.excludes = (excludes != null ? excludes : Collections.emptyList());
		this.matchIfEmpty = matchIfEmpty;
	}


	/**
	 * 确定指定的 {@code instance} 是否匹配此过滤器。
	 */
	public boolean match(T instance) {
		Assert.notNull(instance, "Instance to match must not be null");

		boolean includesSet = !this.includes.isEmpty();
		boolean excludesSet = !this.excludes.isEmpty();
		if (!includesSet && !excludesSet) {
			return this.matchIfEmpty;
		}

		boolean matchIncludes = match(instance, this.includes);
		boolean matchExcludes = match(instance, this.excludes);
		if (!includesSet) {
			return !matchExcludes;
		}
		if (!excludesSet) {
			return matchIncludes;
		}
		return matchIncludes && !matchExcludes;
	}

	/**
	 * 确定指定的 {@code instance} 是否等于指定的 {@code candidate}。
	 * @param instance the instance to handle
	 * @param candidate a candidate defined by this filter
	 * @return {@code true} if the instance matches the candidate
	 */
	protected boolean match(T instance, T candidate) {
		return instance.equals(candidate);
	}

	/**
	 * 确定指定的 {@code instance} 是否匹配其中一个候选。
	 *
	 * <p>如果候选集合为 {@code null}，则返回 {@code false}。
	 * @param instance the instance to check
	 * @param candidates a list of candidates
	 * @return {@code true} if the instance match or the candidates collection is null
	 */
	protected boolean match(T instance, Collection<? extends T> candidates) {
		for (T candidate : candidates) {
			if (match(instance, candidate)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(": includes=").append(this.includes);
		sb.append(", excludes=").append(this.excludes);
		sb.append(", matchIfEmpty=").append(this.matchIfEmpty);
		return sb.toString();
	}

}
