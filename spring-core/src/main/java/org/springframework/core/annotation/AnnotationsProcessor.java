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
 * @param <C> the context type
 * @param <R> the result type
 * @see AnnotationsScanner
 * @see TypeMappedAnnotations
 */
@FunctionalInterface
interface AnnotationsProcessor<C, R> {

	/**
	 * 在聚合对象即将被处理时调用。此方法可以返回 {@code non-null} 结果以短路任何进一步处理。
	 * @param context the context information relevant to the processor
	 * @param aggregateIndex the aggregate index about to be processed
	 * @return a {@code non-null} result if no further processing is required
	 */
	@Nullable
	default R doWithAggregate(C context, int aggregateIndex) {
		return null;
	}

	/**
	 * 在可以处理注解数组时调用。此方法可以返回 {@code non-null} 结果以短路任何进一步处理。
	 * @param context the context information relevant to the processor
	 * @param aggregateIndex the aggregate index of the provided annotations
	 * @param source the original source of the annotations, if known
	 * @param annotations the annotations to process (this array may contain
	 * {@code null} elements)
	 * @return a {@code non-null} result if no further processing is required
	 */
	@Nullable
	R doWithAnnotations(C context, int aggregateIndex, @Nullable Object source, Annotation[] annotations);

	/**
	 * 获取要返回的最终结果。默认情况下，此方法返回最后一次处理结果。
	 * @param result the last early exit result, or {@code null} if none
	 * @return the final result to be returned to the caller
	 */
	@Nullable
	default R finish(@Nullable R result) {
		return result;
	}

}
