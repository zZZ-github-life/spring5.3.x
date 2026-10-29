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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code ActiveProfiles} 是一个类级注解，用于声明
 * 在加载测试类的 {@link org.springframework.context.ApplicationContext ApplicationContext} 时应使用哪些<em>活动 Bean 定义配置</em>。
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * <p>从 Spring Framework 5.3 开始，此注解默认将从
 * 外层测试类继承。详情请参见
 * {@link NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SmartContextLoader
 * @see MergedContextConfiguration
 * @see ContextConfiguration
 * @see ActiveProfilesResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.annotation.Profile
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ActiveProfiles {

	/**
	 * {@link #profiles} 的别名。
	 * <p>此属性<strong>不得</strong>与
	 * {@link #profiles} 一起使用，但可以<em>代替</em> {@link #profiles} 使用。
	 */
	@AliasFor("profiles")
	String[] value() default {};

	/**
	 * 要激活的 Bean 定义配置。
	 * <p>此属性<strong>不得</strong>与
	 * {@link #value} 一起使用，但可以<em>代替</em> {@link #value} 使用。
	 */
	@AliasFor("value")
	String[] profiles() default {};

	/**
	 * 用于以编程方式解析活动
	 * Bean 定义配置的 {@link ActiveProfilesResolver} 类型。
	 * @since 4.0
	 * @see ActiveProfilesResolver
	 */
	Class<? extends ActiveProfilesResolver> resolver() default ActiveProfilesResolver.class;

	/**
	 * 是否应<em>继承</em>超类和封闭类中的
	 * Bean 定义配置。
	 * <p>默认值为 {@code true}，表示测试类将
	 * <em>继承</em>测试超类或封闭类定义的 Bean 定义配置。具体而言，
	 * 测试类的 Bean 定义配置将追加到测试超类或封闭类定义的
	 * Bean 定义配置列表中。因此，子类和嵌套类
	 * 可以选择<em>扩展</em>该 Bean 定义配置列表。
	 * <p>如果 {@code inheritProfiles} 设置为 {@code false}，测试类的 Bean 定义
	 * 配置将<em>遮蔽</em>并实际替换
	 * 超类或封闭类定义的任何 Bean 定义配置。
	 * <p>在下面的示例中，{@code BaseTest} 的
	 * {@code ApplicationContext} 将仅使用 "base"
	 * Bean 定义配置加载；因此，"extended" 配置中定义的 Bean
	 * 不会被加载。相反，{@code ExtendedTest} 的 {@code ApplicationContext}
	 * 将使用 "base"
	 * <strong>和</strong> "extended" Bean 定义配置加载。
	 * <pre class="code">
	 * &#064;ActiveProfiles(&quot;base&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 * // ...
	 * }
	 *
	 * &#064;ActiveProfiles(&quot;extended&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 * // ...
	 * }
	 * </pre>
	 * <p>注意：{@code @ActiveProfiles} 可用于从基于路径的资源位置或
	 * 注解类加载 {@code ApplicationContext}。
	 * @see ContextConfiguration#locations
	 * @see ContextConfiguration#classes
	 * @see ContextConfiguration#inheritLocations
	 */
	boolean inheritProfiles() default true;

}
