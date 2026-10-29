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
 * {@code @SqlConfig} 定义了用于确定如何解析和执行通过 {@link Sql @Sql} 注解配置的 SQL 脚本的元数据。
 * <h3>配置范围</h3>
 * <p>当在集成测试类上作为类级注解声明时，{@code @SqlConfig} 充当测试类层次结构中所有 SQL 脚本的<strong><em>全局</em></strong>配置。当通过 {@code @Sql} 注解的 {@link Sql#config config} 属性直接声明时，{@code @SqlConfig} 充当封闭 {@code @Sql} 注解内声明的 SQL 脚本的<strong><em>本地</em></strong>配置。
 * <h3>默认值</h3>
 * <p>{@code @SqlConfig} 中的每个属性都有一个<em>隐式</em>默认值，该默认值记录在相应属性的 javadoc 中。由于 Java 语言规范中为注解属性定义的规则，遗憾的是无法为注解属性赋 {@code null} 值。因此，为了支持覆盖<em>继承</em>的全局配置，{@code @SqlConfig} 属性具有<em>显式</em>的 {@code default} 值：String 为 {@code ""}，数组为 <code>{}</code>，枚举为 {@code DEFAULT}。这种方式允许 {@code @SqlConfig} 的本地声明通过提供 {@code ""}、<code>{}</code> 或 {@code DEFAULT} 以外的值，有选择地覆盖全局 {@code @SqlConfig} 声明中的各个属性。
 * <h3>继承与覆盖</h3>
 * <p>只要本地 {@code @SqlConfig} 属性未提供 {@code ""}、<code>{}</code> 或 {@code DEFAULT} 以外的显式值，就会<em>继承</em>全局 {@code @SqlConfig} 属性。因此，显式的本地配置会<em>覆盖</em>全局配置。
 * <p>从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @since 4.1
 * @see Sql
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SqlConfig {

	/**
	 * 应针对其执行脚本的 {@link javax.sql.DataSource} 的 bean 名称。
	 * <p>仅当测试的 {@code ApplicationContext} 中存在多个 {@code DataSource} 类型的 bean 时才需要该名称。如果只有一个此类 bean，则无需指定 bean 名称。
	 * <p>默认为空字符串，此时要求以下条件之一成立：
	 * <ol>
	 * <li>在 {@code @SqlConfig} 的全局声明中定义了显式 bean 名称。</li>
	 * <li>可以通过反射在事务管理器上调用名为 {@code getDataSource()} 的公共方法，从事务管理器获取数据源。</li>
	 * <li>测试的 {@code ApplicationContext} 中只有一个 {@code DataSource} 类型的 bean。</li>
	 * <li>要使用的 {@code DataSource} 名为 {@code "dataSource"}。</li>
	 * </ol>
	 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveDataSource
	 */
	String dataSource() default "";

	/**
	 * 应用于驱动事务的 {@link org.springframework.transaction.PlatformTransactionManager PlatformTransactionManager} 的 bean 名称。
	 * <p>仅当测试的 {@code ApplicationContext} 中存在多个 {@code PlatformTransactionManager} 类型的 bean 时才使用该名称。如果只有一个此类 bean，则无需指定 bean 名称。
	 * <p>默认为空字符串，此时要求以下条件之一成立：
	 * <ol>
	 * <li>在 {@code @SqlConfig} 的全局声明中定义了显式 bean 名称。</li>
	 * <li>测试的 {@code ApplicationContext} 中只有一个 {@code PlatformTransactionManager} 类型的 bean。</li>
	 * <li>已实现 {@link org.springframework.transaction.annotation.TransactionManagementConfigurer TransactionManagementConfigurer} 以指定哪个 {@code PlatformTransactionManager} bean 应用于注解驱动的事务管理。</li>
	 * <li>要使用的 {@code PlatformTransactionManager} 名为 {@code "transactionManager"}。</li>
	 * </ol>
	 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveTransactionManager
	 */
	String transactionManager() default "";

	/**
	 * 确定是否应在事务中执行 SQL 脚本时要使用的<em>模式</em>。
	 * <p>默认为 {@link TransactionMode#DEFAULT DEFAULT}。
	 * <p>可以设置为 {@link TransactionMode#ISOLATED}，以确保 SQL 脚本在将立即提交的新的隔离事务中执行。
	 * @see TransactionMode
	 */
	TransactionMode transactionMode() default TransactionMode.DEFAULT;

	/**
	 * 提供的 SQL 脚本的编码（如果与平台编码不同）。
	 * <p>空字符串表示应使用平台编码。
	 */
	String encoding() default "";

	/**
	 * 用于分隔 SQL 脚本中各个语句的字符串。
	 * <p>如果未指定，则隐式默认为 {@code ";"}，最后回退到 {@code "\n"}。
	 * <p>可以设置为 {@link org.springframework.jdbc.datasource.init.ScriptUtils#EOF_STATEMENT_SEPARATOR}，以表示每个脚本包含单个语句且没有分隔符。
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_STATEMENT_SEPARATOR
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#EOF_STATEMENT_SEPARATOR
	 */
	String separator() default "";

	/**
	 * 用于标识 SQL 脚本中单行注释的前缀。
	 * <p>隐式默认为 {@code "--"}。
	 * <p>此属性<strong>不得</strong>与 {@link #commentPrefixes commentPrefixes} 一起使用，但可以代替 {@link #commentPrefixes commentPrefixes} 使用。
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_COMMENT_PREFIX
	 * @see #commentPrefixes
	 */
	String commentPrefix() default "";

	/**
	 * 用于标识 SQL 脚本中单行注释的多个前缀。
	 * <p>隐式默认为 {@code ["--"]}。
	 * <p>此属性<strong>不得</strong>与 {@link #commentPrefix commentPrefix} 一起使用，但可以代替 {@link #commentPrefix commentPrefix} 使用。
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_COMMENT_PREFIXES
	 * @see #commentPrefix
	 * @since 5.2
	 */
	String[] commentPrefixes() default {};

	/**
	 * 用于标识 SQL 脚本中块注释的起始分隔符。
	 * <p>隐式默认为 {@code "/*"}。
	 * @see #blockCommentEndDelimiter
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_BLOCK_COMMENT_START_DELIMITER
	 */
	String blockCommentStartDelimiter() default "";

	/**
	 * 用于标识 SQL 脚本中块注释的结束分隔符。
	 * <p>隐式默认为 <code>"*&#47;"</code>。
	 * @see #blockCommentStartDelimiter
	 * @see org.springframework.jdbc.datasource.init.ScriptUtils#DEFAULT_BLOCK_COMMENT_END_DELIMITER
	 */
	String blockCommentEndDelimiter() default "";

	/**
	 * 执行 SQL 语句时遇到错误而要使用的<em>模式</em>。
	 * <p>默认为 {@link ErrorMode#DEFAULT DEFAULT}。
	 * @see ErrorMode
	 */
	ErrorMode errorMode() default ErrorMode.DEFAULT;


	/**
	 * 枚举<em>模式</em>，用于规定是否应在事务中执行 SQL 脚本以及事务传播行为应是什么。
	 */
	enum TransactionMode {

		/**
		 * 表示应使用<em>默认</em>事务模式。
		 * <p><em>默认</em>的含义取决于声明 {@code @SqlConfig} 的上下文：
		 * <ul>
		 * <li>如果 {@code @SqlConfig} <strong>仅</strong>在本地声明，则默认事务模式为 {@link #INFERRED}。</li>
		 * <li>如果在全局声明 {@code @SqlConfig}，则默认事务模式为 {@link #INFERRED}。</li>
		 * <li>如果同时在全局<strong>和</strong>本地声明 {@code @SqlConfig}，则本地声明的默认事务模式继承自全局声明。</li>
		 * </ul>
		 */
		DEFAULT,

		/**
		 * 表示应使用以下规则<em>推断</em>执行 SQL 脚本时使用的事务模式。在这些规则的上下文中，“<em>可用</em>”是指数据源或事务管理器的 bean 可以通过 {@code @SqlConfig} 中相应的注解属性显式指定，或可通过约定发现。有关用于在 {@code ApplicationContext} 中发现此类 bean 的约定详情，请参见 {@link org.springframework.test.context.transaction.TestContextTransactionUtils TestContextTransactionUtils}。
		 * <h4>推断规则</h4>
		 * <ol>
		 * <li>如果事务管理器和数据源都不可用，将抛出异常。</li>
		 * <li>如果事务管理器不可用但数据源可用，则 SQL 脚本将直接针对数据源执行，而不使用事务。</li>
		 * <li>如果事务管理器可用：
		 * <ul>
		 * <li>如果数据源不可用，将尝试通过反射在事务管理器上调用名为 {@code getDataSource()} 的公共方法，从事务管理器获取数据源。如果尝试失败，将抛出异常。</li>
		 * <li>使用解析出的事务管理器和数据源，如果存在现有事务，则在该事务中执行 SQL 脚本；否则，脚本将在将立即提交的新事务中执行。<em>现有</em>事务通常由 {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener TransactionalTestExecutionListener} 管理。</li>
		 * </ul>
		 * </li>
		 * </ol>
		 * @see #ISOLATED
		 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveDataSource
		 * @see org.springframework.test.context.transaction.TestContextTransactionUtils#retrieveTransactionManager
		 */
		INFERRED,

		/**
		 * 表示 SQL 脚本应始终在将立即提交的新的<em>隔离</em>事务中执行。
		 * <p>与 {@link #INFERRED} 不同，此模式要求存在事务管理器<strong>和</strong>数据源。
		 */
		ISOLATED
	}


	/**
	 * 枚举<em>模式</em>，用于规定执行 SQL 语句时如何处理错误。
	 */
	enum ErrorMode {

		/**
		 * 表示应使用<em>默认</em>错误模式。
		 * <p><em>默认</em>的含义取决于声明 {@code @SqlConfig} 的上下文：
		 * <ul>
		 * <li>如果 {@code @SqlConfig} <strong>仅</strong>在本地声明，则默认错误模式为 {@link #FAIL_ON_ERROR}。</li>
		 * <li>如果在全局声明 {@code @SqlConfig}，则默认错误模式为 {@link #FAIL_ON_ERROR}。</li>
		 * <li>如果同时在全局<strong>和</strong>本地声明 {@code @SqlConfig}，则本地声明的默认错误模式继承自全局声明。</li>
		 * </ul>
		 */
		DEFAULT,

		/**
		 * 表示遇到错误时脚本执行将失败。换言之，不应忽略任何错误。
		 * <p>这实际上是默认错误模式，以便在脚本被意外执行时，如果脚本中的任何 SQL 语句导致错误，就会快速失败。
		 * @see #CONTINUE_ON_ERROR
		 */
		FAIL_ON_ERROR,

		/**
		 * 表示应记录 SQL 脚本中的所有错误，但不将其作为异常传播。
		 * <p>{@code CONTINUE_ON_ERROR} 是 {@code FAIL_ON_ERROR} 的逻辑<em>对立面</em>，也是 {@code IGNORE_FAILED_DROPS} 的<em>超集</em>。
		 * @see #FAIL_ON_ERROR
		 * @see #IGNORE_FAILED_DROPS
		 */
		CONTINUE_ON_ERROR,

		/**
		 * 表示可以忽略失败的 SQL {@code DROP} 语句。
		 * <p>这对于 SQL 方言不支持在 {@code DROP} 语句中使用 {@code IF EXISTS} 子句的非嵌入式数据库很有用。
		 * @see #CONTINUE_ON_ERROR
		 */
		IGNORE_FAILED_DROPS
	}

}
