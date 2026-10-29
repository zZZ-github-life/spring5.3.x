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

package org.springframework.test.context.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>测试注解，表示标注的 {@code void} 方法应在通过 Spring 的 {@code @Transactional} 注解配置为在事务中运行的测试方法的事务启动<em>之前</em>执行。
 * <p>在超类中或作为接口默认方法声明的 {@code @BeforeTransaction} 方法将在当前测试类的相应方法之前执行。
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>。
 * <p>从 Spring Framework 4.3 开始，{@code @BeforeTransaction} 也可以声明在基于 Java 8 的接口默认方法上。
 *
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.transaction.annotation.Transactional
 * @see AfterTransaction
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BeforeTransaction {
}
