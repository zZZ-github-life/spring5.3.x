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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 将构造器、字段、setter 方法或配置方法标记为由 Spring 的依赖注入设施自动装配。它是 JSR-330 的
 * {@link javax.inject.Inject} 注解的替代方案，增加了必需与可选的语义。
 *
 * <h3>自动装配的构造器</h3>
 * <p>对于任意给定的 Bean 类，只能有一个构造器将该注解的 {@link #required} 属性设为 {@code true}，
 * 表示作为 Spring Bean 使用时<i>该</i>构造器进行自动装配。此外，如果 {@code required} 属性设为
 * {@code true}，则只能有一个构造器被标注 {@code @Autowired}。如果有多个<i>非必需</i>构造器声明了该
 * 注解，它们将被视为自动装配的候选。将选择依赖数量最多且能由 Spring 容器中匹配的 Bean 满足的构造器。
 * 如果没有任何候选能够被满足，则使用主/默认构造器（如果存在）。同样，如果一个类声明了多个构造器
 * 但没有一个标注 {@code @Autowired}，则使用主/默认构造器（如果存在）。如果一个类从一开始就只声明了
 * 一个构造器，则始终使用它，即使未标注注解。带注解的构造器不必是 public。
 *
 * <h3>自动装配的字段</h3>
 * <p>字段在 Bean 构造完成后、任何配置方法调用之前立即注入。此类配置字段不必是 public。
 *
 * <h3>自动装配的方法</h3>
 * <p>配置方法可以具有任意名称和任意数量的参数；其中每个参数都将与 Spring 容器中匹配的 Bean 进行自
 * 动装配。Bean 属性 setter 方法实际上只是此类通用配置方法的一个特例。此类配置方法不必是 public。
 *
 * <h3>自动装配的参数</h3>
 * <p>尽管从 Spring Framework 5.0 开始，{@code @Autowired} 在技术上可以声明在单个方法或构造器参数上，
 * 但框架的大多数部分会忽略此类声明。核心 Spring Framework 中唯一主动支持自动装配参数的部分是
 * {@code spring-test} 模块中的 JUnit Jupiter 支持（详情参见
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-junit-jupiter-di">
 * TestContext framework</a> 参考文档）。
 *
 * <h3>多个参数与 'required' 语义</h3>
 * <p>对于多参数构造器或方法，{@link #required} 属性适用于所有参数。单个参数可以声明为 Java 8 风格
 * 的 {@link java.util.Optional}，或者从 Spring Framework 5.0 开始，也可以声明为 {@code @Nullable}
 * 或 Kotlin 中的非空参数类型，从而覆盖基础的 'required' 语义。
 *
 * <h3>自动装配数组、集合和映射</h3>
 * <p>对于数组、{@link java.util.Collection} 或 {@link java.util.Map} 依赖类型，容器会自动装配所有
 * 与声明值类型匹配的 Bean。为此，映射的键必须声明为 {@code String} 类型，它们将被解析为相应的 Bean
 * 名称。此类由容器提供的集合将是有序的，会考虑目标组件的
 * {@link org.springframework.core.Ordered Ordered} 和
 * {@link org.springframework.core.annotation.Order @Order} 值，否则遵循它们在容器中的注册顺序。另
 * 外，单个匹配的目标 Bean 本身也可以是一个泛型化的 {@code Collection} 或 {@code Map}，并以此形式被
 * 注入。
 *
 * <h3>在 {@code BeanPostProcessor} 或 {@code BeanFactoryPostProcessor} 中不受支持</h3>
 * <p>请注意，实际的注入是通过
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 执行的，这
 * 意味着你<em>不能</em>使用 {@code @Autowired} 向
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessor} 或
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * 类型注入引用。请参阅 {@link AutowiredAnnotationBeanPostProcessor} 类的 javadoc（默认情况下，它会
 * 检查该注解是否存在）。
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 * @since 2.5
 * @see AutowiredAnnotationBeanPostProcessor
 * @see Qualifier
 * @see Value
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {

	/**
	 * 声明被注解的依赖是否为必需。
	 * <p>默认值为 {@code true}。
	 */
	boolean required() default true;

}
