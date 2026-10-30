/*
 * Copyright 2002-2021 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.lang.Nullable;

/**
 * 用于处理注解的回调接口。
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <C> 上下文类型
 * @param <R> 结果类型
 * @see AnnotationsScanner
 * @see TypeMappedAnnotations
 */
@FunctionalInterface
interface AnnotationsProcessor<C, R> {

	/**
	 * 在聚合对象即将被处理时调用。此方法可以返回 {@code non-null} 结果以短路任何进一步处理。
	 * @param context 与该处理器相关的上下文信息
	 * @param aggregateIndex 即将处理的聚合索引
	 * @return 如果无需进一步处理，则返回 {@code non-null} 的结果
	 */
	@Nullable
	default R doWithAggregate(C context, int aggregateIndex) {
		return null;
	}

	/**
	 * 在可以处理注解数组时调用。此方法可以返回 {@code non-null} 结果以短路任何进一步处理。
	 * @param context 与该处理器相关的上下文信息
	 * @param aggregateIndex 所提供注解的聚合索引
	 * @param source 注解的原始来源（如果已知）
	 * @param annotations 要处理的注解（该数组可能包含 {@code null} 元素）
	 * @return 如果无需进一步处理，则返回 {@code non-null} 的结果
	 */
	@Nullable
	R doWithAnnotations(C context, int aggregateIndex, @Nullable Object source, Annotation[] annotations);

	/**
	 * 获取要返回的最终结果。默认情况下，此方法返回最后一次处理结果。
	 * @param result 最后一次提前退出的结果，没有则为 {@code null}
	 * @return 要返回给调用方的最终结果
	 */
	@Nullable
	default R finish(@Nullable R result) {
		return result;
	}

}
