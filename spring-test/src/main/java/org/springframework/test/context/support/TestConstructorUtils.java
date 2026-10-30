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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestConstructor.AutowireMode;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * 用于处理 {@link TestConstructor @TestConstructor} 的工具方法。
 *
 * <p>主要供框架内部使用。
 *
 * @author Sam Brannen
 * @since 5.2
 * @see TestConstructor
 */
public abstract class TestConstructorUtils {

	private TestConstructorUtils() {
	}

	/**
	 * 确定给定测试类的所提供可执行对象是否是可自动装配的构造器。
	 * <p>此方法以 {@code null} 作为回退 {@link PropertyProvider} 的值委托给 {@link #isAutowirableConstructor(Executable, Class, PropertyProvider)}。
	 * @param executable 测试类的可执行对象
	 * @param testClass 测试类
	 * @return 如果该可执行对象是可自动装配的构造函数，则为 {@code true}
	 * @see #isAutowirableConstructor(Executable, Class, PropertyProvider)
	 */
	public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass) {
		return isAutowirableConstructor(executable, testClass, null);
	}

	/**
	 * 确定给定测试类的所提供构造器是否可自动装配。
	 * <p>此方法以 {@code null} 作为回退 {@link PropertyProvider} 的值委托给 {@link #isAutowirableConstructor(Constructor, Class, PropertyProvider)}。
	 * @param constructor 测试类的构造函数
	 * @param testClass 测试类
	 * @return 如果该构造函数可自动装配，则为 {@code true}
	 * @see #isAutowirableConstructor(Constructor, Class, PropertyProvider)
	 */
	public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass) {
		return isAutowirableConstructor(constructor, testClass, null);
	}

	/**
	 * 确定给定测试类的所提供可执行对象是否是可自动装配的构造器。
	 * <p>如果所提供的可执行对象是构造器，则此方法委托给 {@link #isAutowirableConstructor(Constructor, Class, PropertyProvider)}，否则返回 {@code false}。
	 * @param executable 测试类的可执行对象
	 * @param testClass 测试类
	 * @param fallbackPropertyProvider 备用属性提供者，用于在 {@link SpringProperties} 中未找到
	 * {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME} 的值时
	 * 查找该值
	 * @return 如果该可执行对象是可自动装配的构造函数，则为 {@code true}
	 * @since 5.3
	 * @see #isAutowirableConstructor(Constructor, Class, PropertyProvider)
	 */
	public static boolean isAutowirableConstructor(Executable executable, Class<?> testClass,
			@Nullable PropertyProvider fallbackPropertyProvider) {

		return (executable instanceof Constructor &&
				isAutowirableConstructor((Constructor<?>) executable, testClass, fallbackPropertyProvider));
	}

	/**
	 * 确定给定测试类的所提供构造器是否可自动装配。
	 *
	 * <p>如果满足以下条件之一，则构造器被视为可自动装配。
	 *
	 * <ol>
	 * <li>构造器带有 {@link Autowired @Autowired} 注解。</li>
	 * <li>测试类上<em>存在</em>或<em>元存在</em> {@link TestConstructor @TestConstructor}，且 {@link TestConstructor#autowireMode() autowireMode} 设置为 {@link AutowireMode#ALL ALL}。</li>
	 * <li>在 {@link SpringProperties} 或所提供的回退 {@link PropertyProvider} 中，默认的<em>测试构造器自动装配模式</em>已设置为 {@code ALL}（参见 {@link TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}）。</li>
	 * </ol>
	 * @param constructor 测试类的构造函数
	 * @param testClass 测试类
	 * @param fallbackPropertyProvider 备用属性提供者，用于在 {@link SpringProperties} 中未找到
	 * 默认<em>测试构造函数自动装配模式</em>的值时查找该值
	 * @return 如果该构造函数可自动装配，则为 {@code true}
	 * @since 5.3
	 */
	public static boolean isAutowirableConstructor(Constructor<?> constructor, Class<?> testClass,
			@Nullable PropertyProvider fallbackPropertyProvider) {

		// Is the constructor annotated with @Autowired?
		if (AnnotatedElementUtils.hasAnnotation(constructor, Autowired.class)) {
			return true;
		}

		AutowireMode autowireMode = null;

		// Is the test class annotated with @TestConstructor?
		TestConstructor testConstructor = TestContextAnnotationUtils.findMergedAnnotation(testClass, TestConstructor.class);
		if (testConstructor != null) {
			autowireMode = testConstructor.autowireMode();
		}
		else {
			// Custom global default from SpringProperties?
			String value = SpringProperties.getProperty(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
			autowireMode = AutowireMode.from(value);

			// Use fallback provider?
			if (autowireMode == null && fallbackPropertyProvider != null) {
				value = fallbackPropertyProvider.get(TestConstructor.TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME);
				autowireMode = AutowireMode.from(value);
			}
		}

		return (autowireMode == AutowireMode.ALL);
	}

}
