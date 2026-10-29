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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于 JUnit 4 的测试注解，指示测试在
 * 特定测试配置下是启用还是禁用。
 *
 * <p>在此注解的上下文中，术语<em>配置</em>默认指
 * Java 系统属性；不过，可以通过实现自定义的 {@link ProfileValueSource}
 * 来更改其语义。如果配置的
 * {@code ProfileValueSource} 为声明的 {@link #name} 返回匹配的 {@link #value}，
 * 则测试将被启用。否则，测试
 * 将被禁用，并实际上被<em>忽略</em>。
 *
 * <p>{@code @IfProfileValue} 可以应用于类级别、方法
 * 级别，或两者兼用。对于该类或其子类中的任何方法，
 * 类级使用 {@code @IfProfileValue} 优先于方法级使用。具体而言，
 * 只有当测试在类级别<em>和</em>方法级别
 * 都启用时，它才会被启用；缺少
 * {@code @IfProfileValue} 意味着测试隐式启用。这
 * 类似于 JUnit 的 {@link org.junit.Ignore @Ignore}
 * 注解的语义，不同之处在于 {@code @Ignore} 的存在总是会禁用
 * 测试。
 *
 * <h3>示例</h3>
 * 使用 {@link SystemProfileValueSource} 作为 {@code ProfileValueSource}
 * 实现（这是默认配置）时，可以按如下方式配置测试
 * 方法，使其仅在 Oracle 的 Java VM 上运行：
 *
 * <pre class="code">
 * &#064;IfProfileValue(name = &quot;java.vendor&quot;, value = &quot;Oracle Corporation&quot;)
 * public void testSomething() {
 *     // ...
 * }</pre>
 *
 * <h3>'OR' 语义</h3>
 * <p>或者，可以为多个 {@link #values} 配置具有<em>或</em>
 * 语义的 {@code @IfProfileValue}。如果
 * 为 {@code "test-groups"} 配置项适当配置了 {@code ProfileValueSource}，
 * 其值为 {@code unit-tests}
 * <em>或</em> {@code integration-tests}，则以下测试将被启用。此功能类似于
 * TestNG 对测试<em>组</em>的支持以及 JUnit 对测试
 * <em>类别</em>的实验性支持。
 *
 * <pre class="code">
 * &#064;IfProfileValue(name = &quot;test-groups&quot;, values = { &quot;unit-tests&quot;, &quot;integration-tests&quot; })
 * public void testWhichRunsForUnitOrIntegrationTestGroups() {
 *     // ...
 * }</pre>
 *
 * <h3>{@code @IfProfileValue} 与 {@code @Profile} 的对比</h3>
 * <p>尽管 {@code @IfProfileValue} 和
 * {@link org.springframework.context.annotation.Profile @Profile} 注解
 * 都涉及<em>配置</em>（profiles），但它们并不直接相关。{@code @Profile}
 * 涉及在
 * {@link org.springframework.core.env.Environment Environment} 中配置的 Bean 定义配置；而
 * {@code @IfProfileValue} 用于启用或禁用测试。
 *
 * <h3>元注解支持</h3>
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see ProfileValueSource
 * @see SystemProfileValueSource
 * @see ProfileValueSourceConfiguration
 * @see ProfileValueUtils
 * @see org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * @see org.springframework.test.context.junit4.statements.ProfileValueChecker
 * @see org.springframework.context.annotation.Profile
 * @see org.springframework.test.context.ActiveProfiles
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface IfProfileValue {

	/**
	 * 要针对其进行测试的<em>配置值</em>的 {@code name}。
	 */
	String name();

	/**
	 * 给定 {@link #name} 的<em>配置值</em>的单个允许 {@code value}。
	 * <p>注意：同时为 {@code #value} 和 {@link #values}
	 * 赋值将导致配置冲突。
	 */
	String value() default "";

	/**
	 * 给定 {@link #name} 的<em>配置值</em>的所有允许 {@code values}
	 * 的列表。
	 * <p>注意：同时为 {@link #value} 和 {@code #values}
	 * 赋值将导致配置冲突。
	 */
	String[] values() default {};

}
