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

package org.springframework.core.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.Map;

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * 用于根据类型声明确定对象顺序的通用工具。同时处理 Spring 的 {@link Order} 注解以及
 * {@link javax.annotation.Priority}。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see Order
 * @see javax.annotation.Priority
 */
public abstract class OrderUtils {

	/** 用于非注解 Class 的缓存标记。 */
	private static final Object NOT_ANNOTATED = new Object();

	private static final String JAVAX_PRIORITY_ANNOTATION = "javax.annotation.Priority";

	/** 每个 Class 的 @Order 值（或 NOT_ANNOTATED 标记）缓存。 */
	private static final Map<AnnotatedElement, Object> orderCache = new ConcurrentReferenceHashMap<>(64);


	/**
	 * 返回指定 {@code type} 上的顺序值，如果找不到，则返回指定的默认值。<p>会处理 {@link Order @Order}
	 * 和 {@code @javax.annotation.Priority}。
	 * @param type 要处理的类型
	 * @return 优先级值；如果找不到，则为指定的默认顺序
	 * @since 5.0
	 * @see #getPriority(Class)
	 */
	public static int getOrder(Class<?> type, int defaultOrder) {
		Integer order = getOrder(type);
		return (order != null ? order : defaultOrder);
	}

	/**
	 * 返回指定 {@code type} 上的顺序值，如果找不到，则返回指定的默认值。<p>会处理 {@link Order @Order}
	 * 和 {@code @javax.annotation.Priority}。
	 * @param type 要处理的类型
	 * @return 优先级值；如果找不到，则为指定的默认顺序
	 * @see #getPriority(Class)
	 */
	@Nullable
	public static Integer getOrder(Class<?> type, @Nullable Integer defaultOrder) {
		Integer order = getOrder(type);
		return (order != null ? order : defaultOrder);
	}

	/**
	 * 返回指定 {@code type} 上的顺序值。<p>会处理 {@link Order @Order} 和
	 * {@code @javax.annotation.Priority}。
	 * @param type 要处理的类型
	 * @return 顺序值；如果找不到，则为 {@code null}
	 * @see #getPriority(Class)
	 */
	@Nullable
	public static Integer getOrder(Class<?> type) {
		return getOrder((AnnotatedElement) type);
	}

	/**
	 * 返回指定 {@code element} 上声明的顺序值。<p>会处理 {@link Order @Order} 和
	 * {@code @javax.annotation.Priority}。
	 * @param element 被注解的元素（例如类型或方法）
	 * @return 顺序值；如果找不到，则为 {@code null}
	 * @since 5.3
	 */
	@Nullable
	public static Integer getOrder(AnnotatedElement element) {
		return getOrderFromAnnotations(element, MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY));
	}

	/**
	 * 从指定的注解集合中返回顺序值。<p>会处理 {@link Order @Order} 和
	 * {@code @javax.annotation.Priority}。
	 * @param element 源元素
	 * @param annotations 要考虑的注解
	 * @return 顺序值；如果找不到，则为 {@code null}
	 */
	@Nullable
	static Integer getOrderFromAnnotations(AnnotatedElement element, MergedAnnotations annotations) {
		if (!(element instanceof Class)) {
			return findOrder(annotations);
		}
		Object cached = orderCache.get(element);
		if (cached != null) {
			return (cached instanceof Integer ? (Integer) cached : null);
		}
		Integer result = findOrder(annotations);
		orderCache.put(element, result != null ? result : NOT_ANNOTATED);
		return result;
	}

	@Nullable
	private static Integer findOrder(MergedAnnotations annotations) {
		MergedAnnotation<Order> orderAnnotation = annotations.get(Order.class);
		if (orderAnnotation.isPresent()) {
			return orderAnnotation.getInt(MergedAnnotation.VALUE);
		}
		MergedAnnotation<?> priorityAnnotation = annotations.get(JAVAX_PRIORITY_ANNOTATION);
		if (priorityAnnotation.isPresent()) {
			return priorityAnnotation.getInt(MergedAnnotation.VALUE);
		}
		return null;
	}

	/**
	 * 返回指定类型上声明的 {@code javax.annotation.Priority} 注解的值，如果不存在则返回 {@code null}。
	 * @param type 要处理的类型
	 * @return 如果声明了该注解则为优先级值，否则为 {@code null}
	 */
	@Nullable
	public static Integer getPriority(Class<?> type) {
		return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY).get(JAVAX_PRIORITY_ANNOTATION)
				.getValue(MergedAnnotation.VALUE, Integer.class).orElse(null);
	}

}
