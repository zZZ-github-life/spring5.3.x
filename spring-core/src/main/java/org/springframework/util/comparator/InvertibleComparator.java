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
import java.util.Comparator;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 比较器的装饰器，带有一个 "ascending" 标志，用于指明比较结果应按正向（标准升序）顺序处理，还是翻
 * 转后按反向（降序）顺序处理。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @param <T> 该比较器可以比较的对象类型
 * @deprecated as of Spring Framework 5.0, in favor of the standard JDK 8
 * {@link Comparator#reversed()}
 */
@Deprecated
@SuppressWarnings("serial")
public class InvertibleComparator<T> implements Comparator<T>, Serializable {

	private final Comparator<T> comparator;

	private boolean ascending = true;


	/**
	 * 创建一个默认按升序排序的 InvertibleComparator。实际比较时将使用指定的 Comparator。
	 * @param comparator 要装饰的比较器
	 */
	public InvertibleComparator(Comparator<T> comparator) {
		Assert.notNull(comparator, "Comparator must not be null");
		this.comparator = comparator;
	}

	/**
	 * 创建一个根据提供的顺序排序的 InvertibleComparator。实际比较时将使用指定的 Comparator。
	 * @param comparator 要装饰的比较器
	 * @param ascending 排序顺序：升序（true）或降序（false）
	 */
	public InvertibleComparator(Comparator<T> comparator, boolean ascending) {
		Assert.notNull(comparator, "Comparator must not be null");
		this.comparator = comparator;
		setAscending(ascending);
	}


	/**
	 * 指定排序顺序：升序 (true) 或降序 (false)。
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}

	/**
	 * 返回排序顺序：升序 (true) 或降序 (false)。
	 */
	public boolean isAscending() {
		return this.ascending;
	}

	/**
	 * 反转排序顺序：升序 &rarr; 降序或降序 &rarr; 升序。
	 */
	public void invertOrder() {
		this.ascending = !this.ascending;
	}


	@Override
	public int compare(T o1, T o2) {
		int result = this.comparator.compare(o1, o2);
		if (result != 0) {
			// Invert the order if it is a reverse sort.
			if (!this.ascending) {
				if (Integer.MIN_VALUE == result) {
					result = Integer.MAX_VALUE;
				}
				else {
					result *= -1;
				}
			}
			return result;
		}
		return 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof InvertibleComparator)) {
			return false;
		}
		InvertibleComparator<T> otherComp = (InvertibleComparator<T>) other;
		return (this.comparator.equals(otherComp.comparator) && this.ascending == otherComp.ascending);
	}

	@Override
	public int hashCode() {
		return this.comparator.hashCode();
	}

	@Override
	public String toString() {
		return "InvertibleComparator: [" + this.comparator + "]; ascending=" + this.ascending;
	}

}
