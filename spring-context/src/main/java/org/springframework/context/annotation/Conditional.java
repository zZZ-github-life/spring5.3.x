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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示只有当所有 {@linkplain #value 指定的条件} 都匹配时，组件才具备注册资格。
 *
 * <p><em>条件</em>是指在 Bean 定义即将被注册之前可以通过编程方式确定的任何状态（详情参见
 * {@link Condition}）。
 *
 * <p>{@code @Conditional} 注解可以按以下任意方式使用：
 * <ul>
 * <li>作为类型级注解，用于任何直接或间接标注了 {@code @Component} 的类，包括 {@link Configuration @Configuration} 类</li>
 * <li>作为元注解，用于组合自定义构造型注解</li>
 * <li>作为方法级注解，用于任何 {@link Bean @Bean} 方法</li>
 * </ul>
 *
 * <p>如果某个 {@code @Configuration} 类标注了 {@code @Conditional}，那么与该类关联的所有
 * {@code @Bean} 方法、{@link Import @Import} 注解以及 {@link ComponentScan @ComponentScan} 注解都
 * 将受这些条件约束。
 *
 * <p><strong>注意</strong>：不支持 {@code @Conditional} 注解的继承；来自超类或来自被重写方法的任何
 * 条件都不会被考虑。为强制实施这一语义，{@code @Conditional} 本身未声明为
 * {@link java.lang.annotation.Inherited @Inherited}；此外，任何以 {@code @Conditional} 作为元注解
 * 的自定义<em>组合注解</em>都不得声明为 {@code @Inherited}。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see Condition
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

	/**
	 * 为使组件能够被注册而必须 {@linkplain Condition#matches 匹配}的所有 {@link Condition} 类。
	 */
	Class<? extends Condition>[] value();

}
