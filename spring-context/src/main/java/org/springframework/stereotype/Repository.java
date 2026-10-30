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
 * 表示被注解的类是一个“Repository”，该术语最初由领域驱动设计（Evans，2003）定义为“一种封装存储、检
 * 索和搜索行为并模拟对象集合的机制”。
 *
 * <p>实现“Data Access Object”等传统 Java EE 模式的团队也可以将此构造型应用于 DAO 类，但在这样做之
 * 前，应注意理解 Data Access Object 与 DDD 风格 Repository 之间的区别。此注解是一种通用构造型，各
 * 个团队可以酌情收窄其语义和用法。
 *
 * <p>如此注解的类在与
 * {@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor PersistenceExceptionTranslationPostProcessor}
 * 结合使用时，有资格获得 Spring 的
 * {@link org.springframework.dao.DataAccessException DataAccessException} 转换。出于工具、切面等目
 * 的，被注解类在整体应用架构中的角色也会得到明确。
 *
 * <p>从 Spring 2.5 开始，此注解还充当 {@link Component @Component} 的特化，允许通过类路径扫描自动
 * 检测实现类。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see Component
 * @see Service
 * @see org.springframework.dao.DataAccessException
 * @see org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Repository {

	/**
	 * 该值可以表示逻辑组件名称的建议，以便在自动检测到组件时将其转换为 Spring Bean。
	 * @return 建议的组件名称（如果有），否则为空 String
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

}
