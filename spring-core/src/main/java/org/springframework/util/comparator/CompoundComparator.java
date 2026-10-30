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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 将一系列（一个或多个）比较器串联起来的比较器。
 *
 * <p>复合比较器按顺序调用每个比较器，直到某个比较器返回非零结果，或者所有比较器都已调用完毕并返回
 * 零。
 *
 * <p>这便于实现类似 SQL 中多列排序的内存排序。列表中任何单个比较器的顺序也可以反转。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @param <T> 该比较器可以比较的对象类型
 * @deprecated as of Spring Framework 5.0, in favor of the standard JDK 8
 * {@link Comparator#thenComparing(Comparator)}
 */
@Deprecated
@SuppressWarnings({"serial", "rawtypes"})
public class CompoundComparator<T> implements Comparator<T>, Serializable {

	private final List<InvertibleComparator> comparators;


	/**
	 * 构造一个初始不包含任何比较器的 CompoundComparator。客户端在调用 compare 方法之前必须至少添加一个
	 * 比较器，否则会抛出 IllegalStateException。
	 */
	public CompoundComparator() {
		this.comparators = new ArrayList<>();
	}

	/**
	 * 根据提供的数组中的比较器构造一个 CompoundComparator。
	 *
	 * <p>所有比较器默认采用升序排序，除非它们是 InvertibleComparator。
	 * @param comparators 要构建到复合比较器中的比较器
	 * @see InvertibleComparator
	 */
	@SuppressWarnings("unchecked")
	public CompoundComparator(Comparator... comparators) {
		Assert.notNull(comparators, "Comparators must not be null");
		this.comparators = new ArrayList<>(comparators.length);
		for (Comparator comparator : comparators) {
			addComparator(comparator);
		}
	}


	/**
	 * 将一个比较器添加到链的末尾。
	 *
	 * <p>该比较器默认采用升序排序，除非它是 InvertibleComparator。
	 * @param comparator 要添加到链末尾的 Comparator
	 * @see InvertibleComparator
	 */
	@SuppressWarnings("unchecked")
	public void addComparator(Comparator<? extends T> comparator) {
		if (comparator instanceof InvertibleComparator) {
			this.comparators.add((InvertibleComparator) comparator);
		}
		else {
			this.comparators.add(new InvertibleComparator(comparator));
		}
	}

	/**
	 * 使用提供的排序顺序将一个比较器添加到链的末尾。
	 * @param comparator 要添加到链末尾的 Comparator
	 * @param ascending 排序顺序：升序（true）或降序（false）
	 */
	@SuppressWarnings("unchecked")
	public void addComparator(Comparator<? extends T> comparator, boolean ascending) {
		this.comparators.add(new InvertibleComparator(comparator, ascending));
	}

	/**
	 * 替换给定索引处的比较器。
	 *
	 * <p>该比较器默认采用升序排序，除非它是 InvertibleComparator。
	 * @param index 要替换的 Comparator 的索引
	 * @param comparator 要放置在给定索引处的 Comparator
	 * @see InvertibleComparator
	 */
	@SuppressWarnings("unchecked")
	public void setComparator(int index, Comparator<? extends T> comparator) {
		if (comparator instanceof InvertibleComparator) {
			this.comparators.set(index, (InvertibleComparator) comparator);
		}
		else {
			this.comparators.set(index, new InvertibleComparator(comparator));
		}
	}

	/**
	 * 使用给定的排序顺序替换给定索引处的比较器。
	 * @param index 要替换的 Comparator 的索引
	 * @param comparator 要放置在给定索引处的 Comparator
	 * @param ascending 排序顺序：升序（true）或降序（false）
	 */
	public void setComparator(int index, Comparator<T> comparator, boolean ascending) {
		this.comparators.set(index, new InvertibleComparator<>(comparator, ascending));
	}

	/**
	 * 反转此复合比较器包含的每个排序定义的排序顺序。
	 */
	public void invertOrder() {
		for (InvertibleComparator comparator : this.comparators) {
			comparator.invertOrder();
		}
	}

	/**
	 * 反转指定索引处排序定义的排序顺序。
	 * @param index 要反转顺序的比较器的索引
	 */
	public void invertOrder(int index) {
		this.comparators.get(index).invertOrder();
	}

	/**
	 * 将给定索引处的排序顺序改为升序。
	 * @param index 要更改的比较器的索引
	 */
	public void setAscendingOrder(int index) {
		this.comparators.get(index).setAscending(true);
	}

	/**
	 * 将给定索引处的排序顺序改为降序。
	 * @param index 要更改的比较器的索引
	 */
	public void setDescendingOrder(int index) {
		this.comparators.get(index).setAscending(false);
	}

	/**
	 * 返回聚合的比较器数量。
	 */
	public int getComparatorCount() {
		return this.comparators.size();
	}


	@Override
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) {
		Assert.state(!this.comparators.isEmpty(),
				"No sort definitions have been added to this CompoundComparator to compare");
		for (InvertibleComparator comparator : this.comparators) {
			int result = comparator.compare(o1, o2);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}


	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof CompoundComparator &&
				this.comparators.equals(((CompoundComparator<T>) other).comparators)));
	}

	@Override
	public int hashCode() {
		return this.comparators.hashCode();
	}

	@Override
	public String toString() {
		return "CompoundComparator: " + this.comparators;
	}

}
