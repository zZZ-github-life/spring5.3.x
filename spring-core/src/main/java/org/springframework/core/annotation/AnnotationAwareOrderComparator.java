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

package org.springframework.core.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.List;

import org.springframework.core.DecoratingProxy;
import org.springframework.core.OrderComparator;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;

/**
 * {@code AnnotationAwareOrderComparator} 是 {@link OrderComparator} 的扩展，支持 Spring 的
 * {@link org.springframework.core.Ordered} 接口以及 {@link Order @Order} 和
 * {@link javax.annotation.Priority @Priority} 注解，其中由 {@code Ordered} 实例提供的 order 值会覆
 * 盖静态定义的注解值（如果有）。
 *
 * <p>有关非有序对象的排序语义的详细信息，请参阅 {@link OrderComparator} 的 Javadoc。
 *
 * @author Juergen Hoeller
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @since 2.0.1
 * @see org.springframework.core.Ordered
 * @see org.springframework.core.annotation.Order
 * @see javax.annotation.Priority
 */
public class AnnotationAwareOrderComparator extends OrderComparator {

	/**
	 * {@code AnnotationAwareOrderComparator} 的共享默认实例。
	 */
	public static final AnnotationAwareOrderComparator INSTANCE = new AnnotationAwareOrderComparator();


	/**
	 * 此实现除了执行超类中的 {@link org.springframework.core.Ordered} 检查之外，还会检查各种类型元素上
	 * 的 {@link Order @Order} 或 {@link javax.annotation.Priority @Priority}。
	 */
	@Override
	@Nullable
	protected Integer findOrder(Object obj) {
		Integer order = super.findOrder(obj);
		if (order != null) {
			return order;
		}
		return findOrderFromAnnotation(obj);
	}

	@Nullable
	private Integer findOrderFromAnnotation(Object obj) {
		AnnotatedElement element = (obj instanceof AnnotatedElement ? (AnnotatedElement) obj : obj.getClass());
		MergedAnnotations annotations = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY);
		Integer order = OrderUtils.getOrderFromAnnotations(element, annotations);
		if (order == null && obj instanceof DecoratingProxy) {
			return findOrderFromAnnotation(((DecoratingProxy) obj).getDecoratedClass());
		}
		return order;
	}

	/**
	 * 此实现获取 @{@link javax.annotation.Priority} 值，相比常规的 @{@link Order} 注解允许附加语义：通
	 * 常是在存在多个匹配项但只需返回一个对象时，选择其中一个对象。
	 */
	@Override
	@Nullable
	public Integer getPriority(Object obj) {
		if (obj instanceof Class) {
			return OrderUtils.getPriority((Class<?>) obj);
		}
		Integer priority = OrderUtils.getPriority(obj.getClass());
		if (priority == null  && obj instanceof DecoratingProxy) {
			return getPriority(((DecoratingProxy) obj).getDecoratedClass());
		}
		return priority;
	}


	/**
	 * 使用默认的 {@link AnnotationAwareOrderComparator} 对给定列表排序。
	 * <p>已优化为对大小为 0 或 1 的列表跳过排序，以避免不必要的数组提取。
	 * @param list the List to sort
	 * @see java.util.List#sort(java.util.Comparator)
	 */
	public static void sort(List<?> list) {
		if (list.size() > 1) {
			list.sort(INSTANCE);
		}
	}

	/**
	 * 使用默认的 AnnotationAwareOrderComparator 对给定数组排序。
	 * <p>已优化为对大小为 0 或 1 的列表跳过排序，以避免不必要的数组提取。
	 * @param array the array to sort
	 * @see java.util.Arrays#sort(Object[], java.util.Comparator)
	 */
	public static void sort(Object[] array) {
		if (array.length > 1) {
			Arrays.sort(array, INSTANCE);
		}
	}

	/**
	 * 如有必要，使用默认的 AnnotationAwareOrderComparator 对给定数组或 List 排序。给定其他任何值时，将
	 * 直接跳过排序。
	 * <p>已优化为对大小为 0 或 1 的列表跳过排序，以避免不必要的数组提取。
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

}
