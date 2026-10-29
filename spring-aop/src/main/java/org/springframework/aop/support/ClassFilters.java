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

package org.springframework.aop.support;

import java.io.Serializable;
import java.util.Arrays;

import org.springframework.aop.ClassFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 用于组合 {@link ClassFilter ClassFilters} 的静态工具方法。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 11.11.2003
 * @see MethodMatchers
 * @see Pointcuts
 */
public abstract class ClassFilters {

	/**
	 * 匹配给定 ClassFilters 中<i>任一</i>（或两者）都能匹配的所有类。
	 * @param cf1 the first ClassFilter
	 * @param cf2 the second ClassFilter
	 * @return a distinct ClassFilter that matches all classes that either
	 * of the given ClassFilter matches
	 */
	public static ClassFilter union(ClassFilter cf1, ClassFilter cf2) {
		Assert.notNull(cf1, "First ClassFilter must not be null");
		Assert.notNull(cf2, "Second ClassFilter must not be null");
		return new UnionClassFilter(new ClassFilter[] {cf1, cf2});
	}

	/**
	 * 匹配给定 ClassFilters 中<i>任一</i>（或全部）都能匹配的所有类。
	 * @param classFilters the ClassFilters to match
	 * @return a distinct ClassFilter that matches all classes that either
	 * of the given ClassFilter matches
	 */
	public static ClassFilter union(ClassFilter[] classFilters) {
		Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
		return new UnionClassFilter(classFilters);
	}

	/**
	 * 匹配给定两个 ClassFilters 都匹配的所有类。
	 * @param cf1 the first ClassFilter
	 * @param cf2 the second ClassFilter
	 * @return a distinct ClassFilter that matches all classes that both
	 * of the given ClassFilter match
	 */
	public static ClassFilter intersection(ClassFilter cf1, ClassFilter cf2) {
		Assert.notNull(cf1, "First ClassFilter must not be null");
		Assert.notNull(cf2, "Second ClassFilter must not be null");
		return new IntersectionClassFilter(new ClassFilter[] {cf1, cf2});
	}

	/**
	 * 匹配给定 ClassFilters 中<i>全部</i>都能匹配的所有类。
	 * @param classFilters the ClassFilters to match
	 * @return a distinct ClassFilter that matches all classes that both
	 * of the given ClassFilter match
	 */
	public static ClassFilter intersection(ClassFilter[] classFilters) {
		Assert.notEmpty(classFilters, "ClassFilter array must not be empty");
		return new IntersectionClassFilter(classFilters);
	}


	/**
	 * 用于给定 ClassFilters 并集的 ClassFilter 实现。
	 */
	@SuppressWarnings("serial")
	private static class UnionClassFilter implements ClassFilter, Serializable {

		private final ClassFilter[] filters;

		UnionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			for (ClassFilter filter : this.filters) {
				if (filter.matches(clazz)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof UnionClassFilter &&
					ObjectUtils.nullSafeEquals(this.filters, ((UnionClassFilter) other).filters)));
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.filters);
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + Arrays.toString(this.filters);
		}

	}


	/**
	 * 用于给定 ClassFilters 交集的 ClassFilter 实现。
	 */
	@SuppressWarnings("serial")
	private static class IntersectionClassFilter implements ClassFilter, Serializable {

		private final ClassFilter[] filters;

		IntersectionClassFilter(ClassFilter[] filters) {
			this.filters = filters;
		}

		@Override
		public boolean matches(Class<?> clazz) {
			for (ClassFilter filter : this.filters) {
				if (!filter.matches(clazz)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof IntersectionClassFilter &&
					ObjectUtils.nullSafeEquals(this.filters, ((IntersectionClassFilter) other).filters)));
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.filters);
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + Arrays.toString(this.filters);
		}

	}

}
