/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;

/**
 * {@code @SpringJUnitConfig} 是一个<em>组合注解</em>，将 JUnit Jupiter 的 {@link ExtendWith @ExtendWith(SpringExtension.class)} 与 <em>Spring TestContext Framework</em> 的 {@link ContextConfiguration @ContextConfiguration} 组合在一起。
 * <p>从 Spring Framework 5.3 开始，此注解实际上默认会从封闭测试类继承。有关详情，请参见 {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 5.0
 * @see ExtendWith
 * @see SpringExtension
 * @see ContextConfiguration
 * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SpringJUnitConfig {

	/**
	 * {@link ContextConfiguration#classes} 的别名。
	 */
	@AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
	Class<?>[] value() default {};

	/**
	 * {@link ContextConfiguration#classes} 的别名。
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	Class<?>[] classes() default {};

	/**
	 * {@link ContextConfiguration#locations} 的别名。
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	String[] locations() default {};

	/**
	 * {@link ContextConfiguration#initializers} 的别名。
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	Class<? extends ApplicationContextInitializer<?>>[] initializers() default {};

	/**
	 * {@link ContextConfiguration#inheritLocations} 的别名。
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	boolean inheritLocations() default true;

	/**
	 * {@link ContextConfiguration#inheritInitializers} 的别名。
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	boolean inheritInitializers() default true;

	/**
	 * {@link ContextConfiguration#name} 的别名。
	 */
	@AliasFor(annotation = ContextConfiguration.class)
	String name() default "";

}
