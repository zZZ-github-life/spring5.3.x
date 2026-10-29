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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @Sql} 用于标注测试类或测试方法，以配置在集成测试期间针对给定数据库执行的 SQL {@link #scripts} 和 {@link #statements}。
 * <p>默认情况下，方法级声明会覆盖类级声明，但可以通过 {@link SqlMergeMode @SqlMergeMode} 配置此行为。
 * <p>脚本执行由 {@link SqlScriptsTestExecutionListener} 执行，该监听器默认启用。
 * <p>此注解和 {@link SqlConfig @SqlConfig} 提供的配置选项与 {@link org.springframework.jdbc.datasource.init.ScriptUtils ScriptUtils} 和 {@link org.springframework.jdbc.datasource.init.ResourceDatabasePopulator ResourceDatabasePopulator} 支持的选项相同，但比 {@code <jdbc:initialize-database/>} XML 命名空间元素提供的选项更多。有关详情，请查阅此注解和 {@link SqlConfig @SqlConfig} 中各个属性的 javadoc。
 * <p>从 Java 8 开始，{@code @Sql} 可用作<em>{@linkplain Repeatable 可重复}</em>注解。否则，可以使用 {@link SqlGroup @SqlGroup} 作为显式容器来声明多个 {@code @Sql} 实例。
 * <p>此注解可用作<em>元注解</em>以创建带属性覆盖的自定义<em>组合注解</em>。
 * <p>从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see SqlConfig
 * @see SqlMergeMode
 * @see SqlGroup
 * @see SqlScriptsTestExecutionListener
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(SqlGroup.class)
public @interface Sql {

	/**
	 * 作为 {@link #scripts} 的别名。
	 * <p>此属性<strong>不得</strong>与 {@link #scripts} 一起使用，但可以代替 {@link #scripts} 使用。
	 * @see #scripts
	 * @see #statements
	 */
	@AliasFor("scripts")
	String[] value() default {};

	/**
	 * 要执行的 SQL 脚本的路径。
	 * <p>此属性<strong>不得</strong>与 {@link #value} 一起使用，但可以代替 {@link #value} 使用。同样，此属性可以与 {@link #statements} 结合使用或代替 {@link #statements} 使用。
	 * <h3>路径资源语义</h3>
	 * <p>每个路径都将被解释为 Spring 的 {@link org.springframework.core.io.Resource Resource}。普通路径——例如 {@code "schema.sql"}——将被视为相对于定义该测试类所在包的类路径资源。以斜杠开头的路径将被视为<em>绝对</em>类路径资源，例如：{@code "/org/example/schema.sql"}。引用 URL 的路径（例如以 {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:}、{@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:}、{@code http:} 等为前缀的路径）将使用指定的资源协议加载。
	 * <h3>默认脚本检测</h3>
	 * <p>如果未指定 SQL 脚本或 {@link #statements}，将根据声明此注解的位置尝试检测<em>默认</em>脚本。如果无法检测到默认值，将抛出 {@link IllegalStateException}。
	 * <ul>
	 * <li><strong>类级声明</strong>：如果带注解的测试类是 {@code com.example.MyTest}，则对应的默认脚本为 {@code "classpath:com/example/MyTest.sql"}。</li>
	 * <li><strong>方法级声明</strong>：如果带注解的测试方法名为 {@code testMethod()} 并且定义在类 {@code com.example.MyTest} 中，则对应的默认脚本为 {@code "classpath:com/example/MyTest.testMethod.sql"}。</li>
	 * </ul>
	 * @see #value
	 * @see #statements
	 */
	@AliasFor("value")
	String[] scripts() default {};

	/**
	 * 要执行的<em>内联 SQL 语句</em>。
	 * <p>此属性可以与 {@link #scripts} 结合使用，也可以代替 {@link #scripts} 使用。
	 * <h3>顺序</h3>
	 * <p>通过此属性声明的语句将在从资源 {@link #scripts} 加载的语句之后执行。如果希望内联语句在脚本之前执行，只需在同一类或方法上声明多个 {@code @Sql} 实例。
	 * @since 4.2
	 * @see #scripts
	 */
	String[] statements() default {};

	/**
	 * 何时应执行 SQL 脚本和语句。
	 * <p>默认为 {@link ExecutionPhase#BEFORE_TEST_METHOD BEFORE_TEST_METHOD}。
	 */
	ExecutionPhase executionPhase() default ExecutionPhase.BEFORE_TEST_METHOD;

	/**
	 * 为此 {@code @Sql} 注解中声明的 SQL 脚本和语句提供的本地配置。
	 * <p>有关本地配置与全局配置、继承、覆盖等说明，请参见 {@link SqlConfig} 的类级 javadoc。
	 * <p>默认为空的 {@link SqlConfig @SqlConfig} 实例。
	 */
	SqlConfig config() default @SqlConfig;


	/**
	 * 枚举<em>阶段</em>，用于规定何时执行 SQL 脚本。
	 */
	enum ExecutionPhase {

		/**
		 * 配置的 SQL 脚本和语句将在相应测试方法<em>之前</em>执行。
		 */
		BEFORE_TEST_METHOD,

		/**
		 * 配置的 SQL 脚本和语句将在相应测试方法<em>之后</em>执行。
		 */
		AFTER_TEST_METHOD
	}

}
