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
import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * 用于从给定源对象中提取注解属性值的策略 API，该源对象通常是 {@link Annotation}、{@link Map} 或
 * {@link TypeMappedAnnotation}。
 *
 * @author Sam Brannen
 * @since 5.2.4
 */
@FunctionalInterface
interface ValueExtractor {

	/**
	 * 从所提供的源 {@link Object} 中提取由所提供的 {@link Method} 表示的注解属性。
	 */
	@Nullable
	Object extract(Method attribute, @Nullable Object object);

}
