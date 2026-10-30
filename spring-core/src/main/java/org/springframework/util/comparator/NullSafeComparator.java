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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 一个可安全地将 null 视为低于或高于其他对象进行比较的 Comparator。可以装饰给定的 Comparator，也可
 * 以直接在 Comparable 上工作。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @param <T> 该比较器可以比较的对象类型
 * @see Comparable
 */
public class NullSafeComparator<T> implements Comparator<T> {

	/**
	 * 此比较器的共享默认实例，将 null 视为低于非 null 对象。
	 * @see Comparators#nullsLow()
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_LOW = new NullSafeComparator<>(true);

	/**
	 * 此比较器的共享默认实例，将 null 视为高于非 null 对象。
	 * @see Comparators#nullsHigh()
	 */
	@SuppressWarnings("rawtypes")
	public static final NullSafeComparator NULLS_HIGH = new NullSafeComparator<>(false);


	private final Comparator<T> nonNullComparator;

	private final boolean nullsLow;


	/**
	 * 创建一个 NullSafeComparator，根据提供的标志对 {@code null} 排序，并在 Comparable 上工作。
	 *
	 * <p>比较两个非 null 对象时，将使用它们的 Comparable 实现：这意味着（此 Comparator 将应用于的）非
	 * null 元素需要实现 Comparable。
	 *
	 * <p>为方便起见，可以使用默认的共享实例：{@code NullSafeComparator.NULLS_LOW} 和
	 * {@code NullSafeComparator.NULLS_HIGH}。
	 * @param nullsLow 是否将 null 视为低于还是高于非 null 对象
	 * @see Comparable
	 * @see #NULLS_LOW
	 * @see #NULLS_HIGH
	 */
	@SuppressWarnings("unchecked")
	private NullSafeComparator(boolean nullsLow) {
		this.nonNullComparator = ComparableComparator.INSTANCE;
		this.nullsLow = nullsLow;
	}

	/**
	 * 创建一个 NullSafeComparator，根据提供的标志对 {@code null} 排序，并装饰给定的 Comparator。
	 *
	 * <p>比较两个非 null 对象时，将使用指定的 Comparator。给定的底层 Comparator 必须能够处理此
	 * Comparator 将应用于的元素。
	 * @param comparator 比较两个非 null 对象时使用的比较器
	 * @param nullsLow 是否将 null 视为低于还是高于非 null 对象
	 */
	public NullSafeComparator(Comparator<T> comparator, boolean nullsLow) {
		Assert.notNull(comparator, "Non-null Comparator is required");
		this.nonNullComparator = comparator;
		this.nullsLow = nullsLow;
	}


	@Override
	public int compare(@Nullable T o1, @Nullable T o2) {
		if (o1 == o2) {
			return 0;
		}
		if (o1 == null) {
			return (this.nullsLow ? -1 : 1);
		}
		if (o2 == null) {
			return (this.nullsLow ? 1 : -1);
		}
		return this.nonNullComparator.compare(o1, o2);
	}


	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NullSafeComparator)) {
			return false;
		}
		NullSafeComparator<T> otherComp = (NullSafeComparator<T>) other;
		return (this.nonNullComparator.equals(otherComp.nonNullComparator) && this.nullsLow == otherComp.nullsLow);
	}

	@Override
	public int hashCode() {
		return this.nonNullComparator.hashCode() * (this.nullsLow ? -1 : 1);
	}

	@Override
	public String toString() {
		return "NullSafeComparator: non-null comparator [" + this.nonNullComparator + "]; " +
				(this.nullsLow ? "nulls low" : "nulls high");
	}

}
