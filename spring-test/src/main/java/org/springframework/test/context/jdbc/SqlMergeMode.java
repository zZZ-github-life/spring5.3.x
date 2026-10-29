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

package org.springframework.test.context.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SqlMergeMode} 用于标注测试类或测试方法，以配置方法级 {@code @Sql} 声明是否与类级 {@code @Sql} 声明合并。
 * <p>方法级 {@code @SqlMergeMode} 声明会覆盖类级声明。
 * <p>如果未在测试类或测试方法上声明 {@code @SqlMergeMode}，则默认使用 {@link MergeMode#OVERRIDE}。
 * <p>此注解可用作<em>元注解</em>以创建带属性覆盖的自定义<em>组合注解</em>。
 * <p>从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @since 5.2
 * @see Sql
 * @see MergeMode#MERGE
 * @see MergeMode#OVERRIDE
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlMergeMode {

	/**
	 * 指示方法级 {@code @Sql} 注解应与类级 {@code @Sql} 注解合并还是覆盖它们。
	 */
	MergeMode value();


	/**
	 * 枚举<em>模式</em>，用于规定方法级 {@code @Sql} 声明是否与类级 {@code @Sql} 声明合并。
	 */
	enum MergeMode {

		/**
		 * 表示方法级 {@code @Sql} 声明应与类级 {@code @Sql} 声明合并，其中类级 SQL 脚本和语句在方法级脚本和语句之前执行。
		 */
		MERGE,

		/**
		 * 表示方法级 {@code @Sql} 声明应覆盖类级 {@code @Sql} 声明。
		 */
		OVERRIDE

	}

}
