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

package org.springframework.util.comparator;

import java.util.Comparator;

/**
 * 便捷的入口点，提供用于常见 Spring {@link Comparator} 变体的泛型工厂方法。
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
public abstract class Comparators {

	/**
	 * 返回一个 {@link Comparable} 适配器。
	 * @see ComparableComparator#INSTANCE
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> comparable() {
		return ComparableComparator.INSTANCE;
	}

	/**
	 * 返回一个 {@link Comparable} 适配器，它接受 null 值并将其排在非 null 值之前。
	 * @see NullSafeComparator#NULLS_LOW
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> nullsLow() {
		return NullSafeComparator.NULLS_LOW;
	}

	/**
	 * 返回给定比较器的装饰器，它接受 null 值并将其排在非 null 值之前。
	 * @see NullSafeComparator#NullSafeComparator(boolean)
	 */
	public static <T> Comparator<T> nullsLow(Comparator<T> comparator) {
		return new NullSafeComparator<>(comparator, true);
	}

	/**
	 * 返回一个 {@link Comparable} 适配器，它接受 null 值并将其排在非 null 值之后。
	 * @see NullSafeComparator#NULLS_HIGH
	 */
	@SuppressWarnings("unchecked")
	public static <T> Comparator<T> nullsHigh() {
		return NullSafeComparator.NULLS_HIGH;
	}

	/**
	 * 返回给定比较器的装饰器，它接受 null 值并将其排在非 null 值之后。
	 * @see NullSafeComparator#NullSafeComparator(boolean)
	 */
	public static <T> Comparator<T> nullsHigh(Comparator<T> comparator) {
		return new NullSafeComparator<>(comparator, false);
	}

}
