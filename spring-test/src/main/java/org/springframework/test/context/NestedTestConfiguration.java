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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * {@code @NestedTestConfiguration} 是一个类型级注解，用于
 * 配置在封闭类层次结构中（即对于<em>内部</em>测试类）如何处理
 * Spring 测试配置注解。
 *
 * <p>如果 {@code @NestedTestConfiguration} 未<em>存在</em>或
 * <em>元存在</em>于测试类、其超类型层次结构或其
 * 封闭类层次结构中，则将使用默认的<em>封闭配置继承
 * 模式</em>。在封闭类上为嵌套接口声明的 {@code @NestedTestConfiguration}
 * 在实现该接口的类上搜索该注解时将被忽略。
 * 有关如何更改默认模式的详细信息，请参见
 * {@link #ENCLOSING_CONFIGURATION_PROPERTY_NAME}。
 *
 * <p>当使用 {@link EnclosingConfiguration#INHERIT INHERIT} 模式时，
 * 内部测试类将从封闭测试类继承配置，
 * 类似于测试类继承层次结构中的语义。
 * 当使用 {@link EnclosingConfiguration#OVERRIDE OVERRIDE} 模式时，
 * 内部测试类必须声明自己的 Spring 测试配置
 * 注解。如果希望显式配置模式，请使用
 * {@code @NestedTestConfiguration(...)} 注解内部测试类或其某个封闭类。
 * 请注意，{@code @NestedTestConfiguration(...)} 声明会在
 * 超类层次结构以及封闭类层次结构中继承。因此，
 * 除非希望切换模式，否则无需重新声明该注解。
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * <p>从 Spring Framework 5.3 开始，此注解通常只在
 * JUnit Jupiter 中与 {@link org.junit.jupiter.api.Nested @Nested} 测试
 * 类结合使用时才有意义；不过，其他支持嵌套测试类的
 * 测试框架也可能使用此注解。
 *
 * <h3>支持的注解</h3>
 * <p><em>Spring TestContext Framework</em> 对以下注解遵循 {@code @NestedTestConfiguration}
 * 语义。
 * <ul>
 * <li>{@link BootstrapWith @BootstrapWith}</li>
 * <li>{@link TestExecutionListeners @TestExecutionListeners}</li>
 * <li>{@link ContextConfiguration @ContextConfiguration}</li>
 * <li>{@link ContextHierarchy @ContextHierarchy}</li>
 * <li>{@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}</li>
 * <li>{@link ActiveProfiles @ActiveProfiles}</li>
 * <li>{@link TestPropertySource @TestPropertySource}</li>
 * <li>{@link DynamicPropertySource @DynamicPropertySource}</li>
 * <li>{@link org.springframework.test.annotation.DirtiesContext @DirtiesContext}</li>
 * <li>{@link org.springframework.transaction.annotation.Transactional @Transactional}</li>
 * <li>{@link org.springframework.test.annotation.Rollback @Rollback}</li>
 * <li>{@link org.springframework.test.annotation.Commit @Commit}</li>
 * <li>{@link org.springframework.test.context.jdbc.Sql @Sql}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlConfig @SqlConfig}</li>
 * <li>{@link org.springframework.test.context.jdbc.SqlMergeMode @SqlMergeMode}</li>
 * <li>{@link TestConstructor @TestConstructor}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 5.3
 * @see EnclosingConfiguration#INHERIT
 * @see EnclosingConfiguration#OVERRIDE
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface NestedTestConfiguration {

	/**
	 * 用于更改默认<em>封闭配置
	 * 继承模式</em>的 JVM 系统属性：{@value #ENCLOSING_CONFIGURATION_PROPERTY_NAME}。
	 * <p>支持的值包括 {@link EnclosingConfiguration} 中定义的枚举常量，
	 * 忽略大小写。例如，可以通过命令行提供
	 * 以下 JVM 系统属性，将默认值更改为 {@link EnclosingConfiguration#OVERRIDE}。
	 * <pre style="code">-Dspring.test.enclosing.configuration=override</pre>
	 * <p>如果该属性未设置为 {@code OVERRIDE}，则内部测试类的测试配置
	 * 默认将按照
	 * {@link EnclosingConfiguration#INHERIT} 语义<em>继承</em>。
	 * <p>也可以通过
	 * {@link org.springframework.core.SpringProperties SpringProperties}
	 * 机制配置。
	 * @see #value
	 */
	String ENCLOSING_CONFIGURATION_PROPERTY_NAME = "spring.test.enclosing.configuration";


	/**
	 * 配置 {@link EnclosingConfiguration} 模式。
	 * @see EnclosingConfiguration#INHERIT
	 * @see EnclosingConfiguration#OVERRIDE
	 */
	EnclosingConfiguration value();


	/**
	 * 枚举<em>模式</em>，决定内部测试类如何处理来自
	 * 封闭类的测试配置。
	 * @see #INHERIT
	 * @see #OVERRIDE
	 */
	enum EnclosingConfiguration {

		/**
		 * 指示内部测试类的测试配置应从其
		 * {@linkplain Class#getEnclosingClass() 封闭类}<em>继承</em>，
		 * 就像封闭类是超类一样。
		 */
		INHERIT,

		/**
		 * 指示内部测试类的测试配置应
		 * <em>覆盖</em>其
		 * {@linkplain Class#getEnclosingClass() 封闭类}的配置。
		 */
		OVERRIDE;


		/**
		 * 获取具有提供名称的 {@code EnclosingConfiguration} 枚举常量，
		 * 忽略大小写。
		 * @param name the name of the enum constant to retrieve
		 * @return the corresponding enum constant or {@code null} if not found
		 * @see EnclosingConfiguration#valueOf(String)
		 */
		@Nullable
		public static EnclosingConfiguration from(@Nullable String name) {
			if (name == null) {
				return null;
			}
			try {
				return EnclosingConfiguration.valueOf(name.trim().toUpperCase());
			}
			catch (IllegalArgumentException ex) {
				Log logger = LogFactory.getLog(EnclosingConfiguration.class);
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Failed to parse enclosing configuration mode from '%s': %s",
						name, ex.getMessage()));
				}
				return null;
			}
		}

	}

}
