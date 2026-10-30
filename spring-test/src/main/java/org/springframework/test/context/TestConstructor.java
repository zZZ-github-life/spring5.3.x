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
 * {@code @TestConstructor} 是一个类型级注解，用于配置
 * 测试类构造函数的参数如何从测试的
 * {@link org.springframework.context.ApplicationContext
 * ApplicationContext} 中的组件自动装配。
 *
 * <p>如果测试类上未<em>存在</em>或<em>元存在</em> {@code @TestConstructor}，
 * 将使用默认的<em>测试构造函数自动装配模式</em>。
 * 有关如何更改默认模式的详细信息，请参见 {@link #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}。
 * 但请注意，在构造函数上本地声明
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 优先于
 * {@code @TestConstructor} 和默认
 * 模式。
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * <p>从 Spring Framework 5.2 开始，此注解仅支持与
 * 用于 JUnit Jupiter 的 {@link org.springframework.test.context.junit.jupiter.SpringExtension
 * SpringExtension} 结合使用。请注意，{@code SpringExtension}
 * 通常会为你自动注册 —— 例如，在使用
 * {@link org.springframework.test.context.junit.jupiter.SpringJUnitConfig @SpringJUnitConfig} 和
 * {@link org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig @SpringJUnitWebConfig}
 * 或 Spring Boot Test 中各种测试相关注解时。
 *
 * <p>从 Spring Framework 5.3 开始，此注解默认将从
 * 外层测试类继承。详情请参见
 * {@link NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 5.2
 * @see org.springframework.beans.factory.annotation.Autowired @Autowired
 * @see org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension
 * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig @SpringJUnitConfig
 * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig @SpringJUnitWebConfig
 * @see ContextConfiguration @ContextConfiguration
 * @see ContextHierarchy @ContextHierarchy
 * @see ActiveProfiles @ActiveProfiles
 * @see TestPropertySource @TestPropertySource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestConstructor {

	/**
	 * 用于更改默认<em>测试构造函数
	 * 自动装配模式</em>的 JVM 系统属性：{@value #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}。
	 * <p>可接受的值包括 {@link AutowireMode} 中定义的枚举常量，
	 * 忽略大小写。例如，可以通过命令行提供
	 * 以下 JVM 系统属性，将默认值更改为 {@link AutowireMode#ALL}。
	 * <pre style="code">-Dspring.test.constructor.autowire.mode=all</pre>
	 * <p>如果该属性未设置为 {@code ALL}，则测试类
	 * 构造函数的参数默认将按照 {@link AutowireMode#ANNOTATED}
	 * 语义自动装配。
	 * <p>也可以通过
	 * {@link org.springframework.core.SpringProperties SpringProperties}
	 * 机制配置。
	 * <p>从 Spring Framework 5.3 开始，此属性还可以配置为
	 * <a href="https://junit.org/junit5/docs/current/user-guide/#running-tests-config-params">JUnit
	 * Platform 配置参数</a>。
	 * @see #autowireMode
	 */
	String TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME = "spring.test.constructor.autowire.mode";


	/**
	 * 用于设置当前测试类的<em>测试构造函数 {@linkplain AutowireMode 自动装配
	 * 模式}</em>的标志。
	 * <p>设置此标志将覆盖全局默认值。有关
	 * 如何更改全局默认值的详细信息，请参见
	 * {@link #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME}。
	 * @return 优先于全局默认值的 {@link AutowireMode}
	 * @see #TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
	 * @see org.springframework.beans.factory.annotation.Autowired @Autowired
	 * @see AutowireMode#ALL
	 * @see AutowireMode#ANNOTATED
	 */
	AutowireMode autowireMode();


	/**
	 * 定义测试构造函数中参数的自动装配模式。
	 * @see #ALL
	 * @see #ANNOTATED
	 */
	enum AutowireMode {

		/**
		 * 所有测试构造函数参数都将被自动装配，就像构造函数
		 * 本身使用
		 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 注解一样。
		 * @see #ANNOTATED
		 */
		ALL,

		/**
		 * 每个单独的测试构造函数参数仅在使用
		 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired}、
		 * {@link org.springframework.beans.factory.annotation.Qualifier @Qualifier}
		 * 或 {@link org.springframework.beans.factory.annotation.Value @Value} 注解时，
		 * 或构造函数本身使用 {@code @Autowired} 注解时，才会
		 * 被自动装配。
		 * @see #ALL
		 */
		ANNOTATED;


		/**
		 * 获取具有提供名称的 {@code AutowireMode} 枚举常量，
		 * 忽略大小写。
		 * @param name 要检索的枚举常量的名称
		 * @return 对应的枚举常量，如果未找到则为 {@code null}
		 * @since 5.3
		 * @see AutowireMode#valueOf(String)
		 */
		@Nullable
		public static AutowireMode from(@Nullable String name) {
			if (name == null) {
				return null;
			}
			try {
				return AutowireMode.valueOf(name.trim().toUpperCase());
			}
			catch (IllegalArgumentException ex) {
				Log logger = LogFactory.getLog(AutowireMode.class);
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Failed to parse autowire mode from '%s': %s", name, ex.getMessage()));
				}
				return null;
			}
		}
	}

}
