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

package org.springframework.test.annotation;

import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 用于处理 Spring 核心测试注解的工具方法集合。
 *
 * @author Sam Brannen
 * @since 4.2
 */
public abstract class TestAnnotationUtils {

	/**
	 * 获取通过提供的 {@code method} 上的 {@link Timed @Timed}
	 * 注解配置的 {@code timeout}。
	 * <p>配置的负值将转换为 {@code 0}。
	 * @return 配置的超时时间，
	 * 如果该方法未标注 {@code @Timed}，则为 {@code 0}
	 */
	public static long getTimeout(Method method) {
		Timed timed = AnnotatedElementUtils.findMergedAnnotation(method, Timed.class);
		return (timed == null ? 0 : Math.max(0, timed.millis()));
	}

	/**
	 * 获取通过提供的 {@code method} 上的 {@link Repeat @Repeat}
	 * 注解配置的重复次数。
	 * <p>配置的非负值将转换为 {@code 1}。
	 * @return 配置的重复次数，
	 * 如果该方法未标注 {@code @Repeat}，则为 {@code 1}
	 */
	public static int getRepeatCount(Method method) {
		Repeat repeat = AnnotatedElementUtils.findMergedAnnotation(method, Repeat.class);
		if (repeat == null) {
			return 1;
		}
		return Math.max(1, repeat.value());
	}

}
