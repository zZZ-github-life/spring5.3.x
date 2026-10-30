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

package org.springframework.validation.annotation;

import java.lang.annotation.Annotation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

/**
 * 用于处理校验注解的工具类。
 * 主要供框架内部使用。
 *
 * @author Christoph Dreis
 * @since 5.3.7
 */
public abstract class ValidationAnnotationUtils {

	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	/**
	 * 根据给定注解确定任何校验提示。
	 *
	 * <p>此实现会检查 {@code @javax.validation.Valid}、Spring 的
	 * {@link org.springframework.validation.annotation.Validated}，以及名称以 "Valid" 开头的自定义注解。
	 * @param ann 该注解（可能是一个校验注解）。
	 * @return 要应用的校验提示（可能为空数组），
	 * 如果该注解不触发任何校验，则为 {@code null}。
	 */
	@Nullable
	public static Object[] determineValidationHints(Annotation ann) {
		Class<? extends Annotation> annotationType = ann.annotationType();
		String annotationName = annotationType.getName();
		if ("javax.validation.Valid".equals(annotationName)) {
			return EMPTY_OBJECT_ARRAY;
		}
		Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
		if (validatedAnn != null) {
			Object hints = validatedAnn.value();
			return convertValidationHints(hints);
		}
		if (annotationType.getSimpleName().startsWith("Valid")) {
			Object hints = AnnotationUtils.getValue(ann);
			return convertValidationHints(hints);
		}
		return null;
	}

	private static Object[] convertValidationHints(@Nullable Object hints) {
		if (hints == null) {
			return EMPTY_OBJECT_ARRAY;
		}
		return (hints instanceof Object[] ? (Object[]) hints : new Object[]{hints});
	}

}
