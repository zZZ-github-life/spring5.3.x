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

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;
import org.springframework.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import org.springframework.util.Assert;

import static org.springframework.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;

/**
 * {@link ActiveProfilesResolver} 策略的默认实现，仅根据通过 {@link ActiveProfiles#profiles} 或 {@link ActiveProfiles#value} 以声明方式配置的配置文件来解析<em>活动 Bean 定义配置文件</em>。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see ActiveProfiles
 * @see ActiveProfilesResolver
 */
public class DefaultActiveProfilesResolver implements ActiveProfilesResolver {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private static final Log logger = LogFactory.getLog(DefaultActiveProfilesResolver.class);


	/**
	 * 根据通过 {@link ActiveProfiles#profiles} 或 {@link ActiveProfiles#value} 以声明方式配置的配置文件，为给定的 {@linkplain Class 测试类}解析<em>Bean 定义配置文件</em>。
	 * @param testClass 应为其解析 profile 的测试类；从不为 {@code null}
	 * @return 加载 {@code ApplicationContext} 时要使用的 Bean 定义 profile 列表；
	 * 从不为 {@code null}
	 */
	@Override
	public String[] resolve(Class<?> testClass) {
		Assert.notNull(testClass, "Class must not be null");
		AnnotationDescriptor<ActiveProfiles> descriptor = findAnnotationDescriptor(testClass, ActiveProfiles.class);

		if (descriptor == null) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
					"Could not find an 'annotation declaring class' for annotation type [%s] and class [%s]",
					ActiveProfiles.class.getName(), testClass.getName()));
			}
			return EMPTY_STRING_ARRAY;
		}
		else {
			ActiveProfiles annotation = descriptor.getAnnotation();
			if (logger.isTraceEnabled()) {
				logger.trace(String.format("Retrieved @ActiveProfiles [%s] for declaring class [%s].", annotation,
					descriptor.getDeclaringClass().getName()));
			}
			return annotation.profiles();
		}
	}

}
