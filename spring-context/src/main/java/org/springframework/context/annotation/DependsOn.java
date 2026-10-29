/*
 * Copyright 2002-2018 the original author or authors.
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
 * 当前 Bean 所依赖的 Bean。所指定的任何 Bean 都保证由容器在此 Bean 之前创建。在 Bean 并非通过属性
 * 或构造器参数显式依赖另一个 Bean，而是依赖另一个 Bean 初始化的副作用的情况下偶尔使用。
 *
 * <p>depends-on 声明既可以指定初始化时依赖，也可以（仅对于单例 Bean）指定相应的销毁时依赖。与给定
 * Bean 定义了 depends-on 关系的依赖 Bean 会先被销毁，然后才销毁给定 Bean 本身。因此，depends-on 声
 * 明还可以控制关闭顺序。
 *
 * <p>可用于任何直接或间接标注了 {@link org.springframework.stereotype.Component} 的类，或标注了
 * {@link Bean} 的方法。
 *
 * <p>除非正在使用组件扫描，否则在类级别使用 {@link DependsOn} 不会产生效果。如果通过 XML 声明了一
 * 个标注了 {@link DependsOn} 的类，则会忽略 {@link DependsOn} 注解元数据，转而遵循
 * {@code <bean depends-on="..."/>}。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DependsOn {

	String[] value() default {};

}
