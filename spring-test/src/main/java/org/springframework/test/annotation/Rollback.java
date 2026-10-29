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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Rollback} 是一个测试注解，用于指示
 * <em>测试管理的事务</em>在测试方法完成后是否应<em>回滚</em>。
 *
 * <p>有关<em>测试管理的事务</em>的说明，请参阅
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * 的类级 Javadoc。
 *
 * <p>当声明为类级注解时，{@code @Rollback} 为测试类
 * 层次结构或嵌套类层次结构中的所有测试方法定义默认的回滚语义。当声明为方法级注解时，
 * {@code @Rollback} 为特定测试方法定义回滚语义，
 * 可能覆盖类级默认的提交或回滚语义。
 *
 * <p>从 Spring Framework 4.2 开始，{@code @Commit} 可直接
 * 替代 {@code @Rollback(false)}。
 *
 * <p><strong>警告</strong>：在同一测试方法或同一测试类上同时声明
 * {@code @Commit} 和 {@code @Rollback} 是不受支持的，
 * 可能导致不可预测的结果。
 *
 * <p>此注解可用作<em>元注解</em>以创建
 * 自定义的<em>组合注解</em>。具体示例请参阅
 * {@link Commit @Commit} 的源代码。
 *
 * <p>从 Spring Framework 5.3 开始，此注解默认将从
 * 外层测试类继承。详情请参见
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 2.5
 * @see Commit
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Rollback {

	/**
	 * 测试方法完成后是否应回滚<em>测试管理的事务</em>。
	 * <p>如果为 {@code true}，事务将被回滚；否则，
	 * 事务将被提交。
	 * <p>默认为 {@code true}。
	 */
	boolean value() default true;

}
