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

package org.springframework.core;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * {@link Ordered} 对象的 {@link Comparator} 实现，按 order 值升序排序，相应地按优先级降序排序。
 *
 * <h3>{@code PriorityOrdered} 对象</h3>
 *
 * <p>{@link PriorityOrdered} 对象的排序优先级高于<em>普通</em> {@code Ordered} 对象。
 *
 * <h3>Order 值相同的对象</h3>
 *
 * <p>具有相同 order 值的对象，相对于其他具有相同 order 值的对象，将以任意顺序排序。
 *
 * <h3>非有序对象</h3>
 *
 * <p>任何未提供自身 order 值的对象都会被隐式赋予 {@link Ordered#LOWEST_PRECEDENCE} 值，因此最终会
 * 以任意顺序位于排序集合的末尾（相对于其他具有相同 order 值的对象）。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 07.04.2003
 * @see Ordered
 * @see PriorityOrdered
 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
 * @see java.util.List#sort(java.util.Comparator)
 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
 */
public class OrderComparator implements Comparator<Object> {

	/**
	 * {@code OrderComparator} 的共享默认实例。
	 */
	public static final OrderComparator INSTANCE = new OrderComparator();


	/**
	 * 使用给定的源提供者构建一个适配的 order 比较器。
	 * @param sourceProvider the order source provider to use
	 * @return the adapted comparator
	 * @since 4.1
	 */
	public Comparator<Object> withSourceProvider(OrderSourceProvider sourceProvider) {
		return (o1, o2) -> doCompare(o1, o2, sourceProvider);
	}

	@Override
	public int compare(@Nullable Object o1, @Nullable Object o2) {
		return doCompare(o1, o2, null);
	}

	private int doCompare(@Nullable Object o1, @Nullable Object o2, @Nullable OrderSourceProvider sourceProvider) {
		boolean p1 = (o1 instanceof PriorityOrdered);
		boolean p2 = (o2 instanceof PriorityOrdered);
		if (p1 && !p2) {
			return -1;
		}
		else if (p2 && !p1) {
			return 1;
		}

		int i1 = getOrder(o1, sourceProvider);
		int i2 = getOrder(o2, sourceProvider);
		return Integer.compare(i1, i2);
	}

	/**
	 * 确定给定对象的 order 值。
	 *
	 * <p>默认实现使用 {@link #findOrder} 检查给定的 {@link OrderSourceProvider}，并回退到常规的
	 * {@link #getOrder(Object)} 调用。
	 * @param obj the object to check
	 * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback
	 */
	private int getOrder(@Nullable Object obj, @Nullable OrderSourceProvider sourceProvider) {
		Integer order = null;
		if (obj != null && sourceProvider != null) {
			Object orderSource = sourceProvider.getOrderSource(obj);
			if (orderSource != null) {
				if (orderSource.getClass().isArray()) {
					for (Object source : ObjectUtils.toObjectArray(orderSource)) {
						order = findOrder(source);
						if (order != null) {
							break;
						}
					}
				}
				else {
					order = findOrder(orderSource);
				}
			}
		}
		return (order != null ? order : getOrder(obj));
	}

	/**
	 * 确定给定对象的 order 值。
	 *
	 * <p>默认实现通过委托给 {@link #findOrder} 检查 {@link Ordered} 接口。可在子类中重写。
	 * @param obj the object to check
	 * @return the order value, or {@code Ordered.LOWEST_PRECEDENCE} as fallback
	 */
	protected int getOrder(@Nullable Object obj) {
		if (obj != null) {
			Integer order = findOrder(obj);
			if (order != null) {
				return order;
			}
		}
		return Ordered.LOWEST_PRECEDENCE;
	}

	/**
	 * 查找由给定对象指示的 order 值。
	 *
	 * <p>默认实现检查 {@link Ordered} 接口。可在子类中重写。
	 * @param obj the object to check
	 * @return the order value, or {@code null} if none found
	 */
	@Nullable
	protected Integer findOrder(Object obj) {
		return (obj instanceof Ordered ? ((Ordered) obj).getOrder() : null);
	}

	/**
	 * 确定给定对象的优先级值（如果有）。
	 *
	 * <p>默认实现总是返回 {@code null}。子类可以重写此方法，为特定类型的值赋予“优先级”特性，作为其
	 * “order”语义的补充。优先级表示除了用于列表/数组中的排序之外，还可用于在对象之间选择一个对象。
	 * @param obj the object to check
	 * @return the priority value, or {@code null} if none
	 * @since 4.1
	 */
	@Nullable
	public Integer getPriority(Object obj) {
		return null;
	}


	/**
	 * 使用默认 OrderComparator 对给定 List 进行排序。
	 *
	 * <p>针对大小为 0 或 1 的列表优化为跳过排序，以避免不必要的数组提取。
	 * @param list the List to sort
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}

	/**
	 * 使用默认 OrderComparator 对给定数组进行排序。
	 *
	 * <p>针对大小为 0 或 1 的列表优化为跳过排序，以避免不必要的数组提取。
	 * @param array the array to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * 必要时使用默认 OrderComparator 对给定数组或 List 进行排序。给定任何其他值时直接跳过排序。
	 *
	 * <p>针对大小为 0 或 1 的列表优化为跳过排序，以避免不必要的数组提取。
	 * @param value the array or List to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sortIfNecessary(Object value) {
		if (value instanceof Object[]) {
			sort((Object[]) value);
		}
		else if (value instanceof List) {
			sort((List<?>) value);
		}
	}


	/**
	 * 用于为给定对象提供 order 源的策略接口。
	 * @since 4.1
	 */
	@FunctionalInterface
	public interface OrderSourceProvider {

		/**
		 * 返回指定对象的 order 源，即应作为给定对象替代来检查 order 值的对象。
		 *
		 * <p>也可以是 order 源对象数组。
		 *
		 * <p>如果返回的对象未指示任何 order，则比较器将回退到检查原始对象。
		 * @param obj the object to find an order source for
		 * @return the order source for that object, or {@code null} if none found
		 */
		@Nullable
		Object getOrderSource(Object obj);
	}

}
