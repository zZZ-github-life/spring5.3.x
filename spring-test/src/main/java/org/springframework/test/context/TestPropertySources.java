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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @TestPropertySources} 是一个或多个 {@link TestPropertySource @TestPropertySource} 声明的容器。
 * <p>但请注意，由于 {@code @TestPropertySource} 是{@linkplain java.lang.annotation.Repeatable 可重复}注解，因此 {@code @TestPropertySources} 容器的使用完全是可选的。
 * <p>从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Anatoliy Korovin
 * @author Sam Brannen
 * @since 5.2
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestPropertySources {

	/**
	 * 一个或多个 {@link TestPropertySource @TestPropertySource} 声明的数组。
	 *
	 */
	TestPropertySource[] value();

}
