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

package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * 表示被注解的类是一个“Service”，该术语最初由领域驱动设计（Evans，2003）定义为“以接口形式提供、在
 * 模型中独立存在且不封装状态的操作”。
 *
 * <p>也可以表示某个类是“Business Service Facade”（按照 Core J2EE 模式的含义）或类似角色。此注解是
 * 一种通用构造型，各个团队可以酌情收窄其语义和用法。
 *
 * <p>此注解充当 {@link Component @Component} 的特化，允许通过类路径扫描自动检测实现类。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see Component
 * @see Repository
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Service {

	/**
	 * 该值可以表示逻辑组件名称的建议，以便在自动检测到组件时将其转换为 Spring Bean。
	 * @return 建议的组件名称（如果有），否则为空 String
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
