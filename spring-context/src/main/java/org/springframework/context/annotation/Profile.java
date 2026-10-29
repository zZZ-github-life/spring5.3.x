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

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/**
 * 指示当一个或多个{@linkplain #value 指定的 profile} 处于活动状态时，组件才有资格注册。
 *
 * <p><em>profile</em> 是一种具名的逻辑分组，可以通过
 * {@link ConfigurableEnvironment#setActiveProfiles} 以编程方式激活，也可以通过以 JVM 系统属性、环
 * 境变量或（对于 Web 应用）{@code web.xml} 中的 Servlet 上下文参数的形式设置
 * {@link AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME spring.profiles.active} 属性来以声明方
 * 式激活。在集成测试中，还可以通过 {@code @ActiveProfiles} 注解以声明方式激活 profile。
 *
 * <p>{@code @Profile} 注解可以通过以下任意方式使用：
 * <ul>
 * <li>作为类型级注解，用于任何直接或间接标注了 {@code @Component} 的类，包括 {@link Configuration @Configuration} 类</li>
 * <li>作为元注解，用于组合自定义构造型注解</li>
 * <li>作为方法级注解，用于任何 {@link Bean @Bean} 方法</li>
 * </ul>
 *
 * <p>如果 {@code @Configuration} 类标注了 {@code @Profile}，除非一个或多个指定的 profile 处于活动
 * 状态，否则与该类关联的所有 {@code @Bean} 方法和 {@link Import @Import} 注解都将被跳过。profile
 * 字符串可以包含简单的 profile 名称（例如 {@code "p1"}），也可以是 profile 表达式。profile 表达式
 * 允许表达更复杂的 profile 逻辑，例如 {@code "p1 & p2"}。有关受支持格式的更多详细信息，请参见
 * {@link Profiles#of(String...)}。
 *
 * <p>这类似于 Spring XML 中的行为：如果提供了 {@code beans} 元素的 {@code profile} 属性，例如
 * {@code <beans profile="p1,p2">}，则除非至少激活了 profile 'p1' 或 'p2'，否则不会解析该
 * {@code beans} 元素。同样，如果 {@code @Component} 或 {@code @Configuration} 类标注了 {@code
 * &#64;Profile({"p1", "p2"})}，则除非至少激活了 profile 'p1' 或 'p2'，否则该类不会被注册或处理。
 *
 * <p>如果给定 profile 带有 NOT 运算符（{@code !}）前缀，则当该 profile <em>不</em>处于活动状态时，
 * 标注的组件将被注册 &mdash; 例如，对于 {@code @Profile({"p1", "!p2"})}，如果 profile 'p1' 处于活
 * 动状态或 profile 'p2' <em>不</em>处于活动状态，就会发生注册。
 *
 * <p>如果省略了 {@code @Profile} 注解，则无论哪些 profile 处于活动状态（如果有），都会发生注册。
 *
 * <p><b>注意：</b>在 {@code @Bean} 方法上使用 {@code @Profile} 时，可能适用一种特殊情况：对于具有
 * 相同 Java 方法名的重载 {@code @Bean} 方法（类似于构造器重载），需要在所有重载方法上一致地声明
 * {@code @Profile} 条件。如果条件不一致，则只有重载方法中第一个声明上的条件会起作用。因此，
 * {@code @Profile} 不能用于在具有特定参数签名的重载方法之间进行选择；同一 Bean 的所有工厂方法之间
 * 的解析在创建时遵循 Spring 的构造器解析算法。<b>如果希望定义具有不同 profile 条件的替代 Bean，请
 * 使用指向同一 {@link Bean#name Bean 名称} 的不同 Java 方法名</b>；请参见
 * {@link Configuration @Configuration} 的 Javadoc 中的 {@code ProfileDatabaseConfig}。
 *
 * <p>通过 XML 定义 Spring Bean 时，可以使用 {@code <beans>} 元素的 {@code "profile"} 属性。有关详
 * 细信息，请参见 {@code spring-beans} XSD（3.1 或更高版本）中的文档。
 *
 * @author Chris Beams
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see ConfigurableEnvironment#setActiveProfiles
 * @see ConfigurableEnvironment#setDefaultProfiles
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
 * @see Conditional
 * @see org.springframework.test.context.ActiveProfiles
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {

	/**
	 * 应注册标注组件的 profile 集合。
	 */
	String[] value();

}
