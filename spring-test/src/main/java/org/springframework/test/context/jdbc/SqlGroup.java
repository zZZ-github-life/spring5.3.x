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
 * 聚合多个 {@link Sql @Sql} 注解的容器注解。
 * <p>可以原生使用，声明多个嵌套的 {@code @Sql} 注解。也可以结合 Java 8 对可重复注解的支持使用，此时只需在同一类或方法上多次声明 {@code @Sql}，即可隐式生成此容器注解。
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>。
 * <p>从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see Sql
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlGroup {

	Sql[] value();

}
