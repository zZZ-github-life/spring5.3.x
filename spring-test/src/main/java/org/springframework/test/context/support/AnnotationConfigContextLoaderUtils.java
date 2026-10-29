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

package org.springframework.test.context.support;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 用于处理组件类（例如 {@link Configuration @Configuration} 类）的 {@link SmartContextLoader SmartContextLoaders} 的工具方法。
 *
 * @author Sam Brannen
 * @since 3.2
 */
public abstract class AnnotationConfigContextLoaderUtils {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoaderUtils.class);


	/**
	 * 为所提供的测试类检测默认配置类。
	 * <p>返回的类数组将包含所提供的类的所有静态嵌套类，这些类满足 {@link Configuration @Configuration} 文档中规定的 {@code @Configuration} 类实现要求。
	 * <p>此方法的实现遵循 {@link org.springframework.test.context.SmartContextLoader SmartContextLoader} SPI 中定义的契约。具体而言，此方法使用内省来检测符合 {@code @Configuration} 类实现所需约束的默认配置类。如果某个潜在候选配置类不满足这些要求，此方法将记录一条调试消息，并忽略该潜在候选类。
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but
	 * never {@code null}
	 */
	public static Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		Assert.notNull(declaringClass, "Declaring class must not be null");

		List<Class<?>> configClasses = new ArrayList<>();

		for (Class<?> candidate : declaringClass.getDeclaredClasses()) {
			if (isDefaultConfigurationClassCandidate(candidate)) {
				configClasses.add(candidate);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Ignoring class [%s]; it must be static, non-private, non-final, and annotated " +
								"with @Configuration to be considered a default configuration class.",
						candidate.getName()));
				}
			}
		}

		if (configClasses.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Could not detect default configuration classes for test class [%s]: " +
						"%s does not declare any static, non-private, non-final, nested classes " +
						"annotated with @Configuration.", declaringClass.getName(), declaringClass.getSimpleName()));
			}
		}

		return ClassUtils.toClassArray(configClasses);
	}

	/**
	 * 确定所提供的 {@link Class} 是否满足被视为<em>默认配置类</em>候选的条件。
	 * <p>具体而言，此类候选：
	 * <ul>
	 * <li>不得为 {@code null}</li>
	 * <li>不得为 {@code private}</li>
	 * <li>不得为 {@code final}</li>
	 * <li>必须为 {@code static}</li>
	 * <li>必须带有 {@code @Configuration} 注解或元注解</li>
	 * </ul>
	 * @param clazz the class to check
	 * @return {@code true} if the supplied class meets the candidate criteria
	 */
	private static boolean isDefaultConfigurationClassCandidate(@Nullable Class<?> clazz) {
		return (clazz != null && isStaticNonPrivateAndNonFinal(clazz) &&
				AnnotatedElementUtils.hasAnnotation(clazz, Configuration.class));
	}

	private static boolean isStaticNonPrivateAndNonFinal(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		int modifiers = clazz.getModifiers();
		return (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers));
	}

}
