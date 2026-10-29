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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 测试注解，指示与测试关联的
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * 处于<em>脏</em>状态，因此应关闭
 * 并从上下文缓存中移除。
 *
 * <p>如果测试修改了上下文（例如修改单例 Bean 的状态、
 * 修改嵌入式数据库的状态等），请使用此注解。
 * 后续请求同一上下文的测试将获得
 * 一个新的上下文。
 *
 * <p>{@code @DirtiesContext} 可以在同一个类或类层次结构中
 * 作为类级和方法级注解使用。在此类场景中，
 * {@code ApplicationContext} 将在任何此类被注解的方法之前或
 * 之后，以及当前测试类之前或之后被标记为<em>脏</em>，
 * 具体取决于配置的 {@link #methodMode} 和 {@link #classMode}。
 *
 * <p>此注解可用作<em>元注解</em>来创建自定义的
 * <em>组合注解</em>。
 *
 * <p>从 Spring Framework 5.3 开始，此注解默认将从
 * 外层测试类继承。详情请参见
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * <h3>支持的测试阶段</h3>
 * <ul>
 * <li><strong>当前测试类之前</strong>：在类级别声明
 * 且类模式设置为 {@link ClassMode#BEFORE_CLASS BEFORE_CLASS} 时</li>
 * <li><strong>当前测试类中的每个测试方法之前</strong>：在
 * 类级别声明且类模式设置为
 * {@link ClassMode#BEFORE_EACH_TEST_METHOD BEFORE_EACH_TEST_METHOD} 时</li>
 * <li><strong>当前测试方法之前</strong>：在方法级别声明
 * 且方法模式设置为
 * {@link MethodMode#BEFORE_METHOD BEFORE_METHOD} 时</li>
 * <li><strong>当前测试方法之后</strong>：在方法级别声明
 * 且方法模式设置为
 * {@link MethodMode#AFTER_METHOD AFTER_METHOD} 时</li>
 * <li><strong>当前测试类中的每个测试方法之后</strong>：在
 * 类级别声明且类模式设置为
 * {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD} 时</li>
 * <li><strong>当前测试类之后</strong>：在
 * 类级别声明且类模式设置为
 * {@link ClassMode#AFTER_CLASS AFTER_CLASS} 时</li>
 * </ul>
 *
 * <p>{@code BEFORE_*} 模式由
 * {@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener DirtiesContextBeforeModesTestExecutionListener} 支持；
 * {@code AFTER_*} 模式由
 * {@link org.springframework.test.context.support.DirtiesContextTestExecutionListener DirtiesContextTestExecutionListener} 支持。
 *
 * @author Sam Brannen
 * @author Rod Johnson
 * @since 2.0
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
 * @see org.springframework.test.context.support.DirtiesContextTestExecutionListener
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DirtiesContext {

	/**
	 * 当测试方法使用
	 * {@code @DirtiesContext} 注解时要使用的<i>模式</i>。
	 * <p>默认为 {@link MethodMode#AFTER_METHOD AFTER_METHOD}。
	 * <p>在带注解的测试类上设置方法模式没有意义。
	 * 若要进行类级控制，请改用 {@link #classMode}。
	 * @since 4.2
	 */
	MethodMode methodMode() default MethodMode.AFTER_METHOD;

	/**
	 * 当测试类使用
	 * {@code @DirtiesContext} 注解时要使用的<i>模式</i>。
	 * <p>默认为 {@link ClassMode#AFTER_CLASS AFTER_CLASS}。
	 * <p>在带注解的测试方法上设置类模式没有意义。
	 * 若要进行方法级控制，请改用 {@link #methodMode}。
	 * @since 3.0
	 */
	ClassMode classMode() default ClassMode.AFTER_CLASS;

	/**
	 * 当上下文作为层次结构的一部分通过
	 * {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy} 配置时，
	 * 要使用的上下文缓存清除<em>模式</em>。
	 * <p>默认为 {@link HierarchyMode#EXHAUSTIVE EXHAUSTIVE}。
	 * @since 3.2.2
	 */
	HierarchyMode hierarchyMode() default HierarchyMode.EXHAUSTIVE;


	/**
	 * 定义<i>模式</i>，用于确定 {@code @DirtiesContext}
	 * 在注解测试方法时的解释方式。
	 * @since 4.2
	 */
	enum MethodMode {

		/**
		 * 关联的 {@code ApplicationContext} 将在
		 * 相应测试方法之前被标记为<em>脏</em>。
		 */
		BEFORE_METHOD,

		/**
		 * 关联的 {@code ApplicationContext} 将在
		 * 相应测试方法之后被标记为<em>脏</em>。
		 */
		AFTER_METHOD;
	}


	/**
	 * 定义<i>模式</i>，用于确定 {@code @DirtiesContext}
	 * 在注解测试类时的解释方式。
	 * @since 3.0
	 */
	enum ClassMode {

		/**
		 * 关联的 {@code ApplicationContext} 将在
		 * 测试类之前被标记为<em>脏</em>。
		 *
		 * @since 4.2
		 */
		BEFORE_CLASS,

		/**
		 * 关联的 {@code ApplicationContext} 将在
		 * 类中的每个测试方法之前被标记为<em>脏</em>。
		 *
		 * @since 4.2
		 */
		BEFORE_EACH_TEST_METHOD,

		/**
		 * 关联的 {@code ApplicationContext} 将在
		 * 类中的每个测试方法之后被标记为<em>脏</em>。
		 */
		AFTER_EACH_TEST_METHOD,

		/**
		 * 关联的 {@code ApplicationContext} 将在
		 * 测试类之后被标记为<em>脏</em>。
		 */
		AFTER_CLASS;
	}


	/**
	 * 定义<i>模式</i>，用于确定当测试的上下文
	 * 作为层次结构的一部分通过
	 * {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy} 配置时，
	 * 使用 {@code @DirtiesContext} 的上下文缓存清除方式。
	 * @since 3.2.2
	 */
	enum HierarchyMode {

		/**
		 * 上下文缓存将使用<em>穷尽性</em>算法清除，
		 * 该算法不仅包括 {@linkplain HierarchyMode#CURRENT_LEVEL 当前层级}，
		 * 还包括与当前测试共享共同祖先上下文
		 * 的所有其他上下文层次结构。
		 *
		 * <p>位于共同祖先上下文子层次结构中的所有 {@code ApplicationContext}
		 * 都将从上下文缓存中移除并关闭。
		 */
		EXHAUSTIVE,

		/**
		 * <em>当前层级</em>的 {@code ApplicationContext} 以及当前
		 * 层级子层次结构中的所有上下文都将从上下文缓存中移除并关闭。
		 *
		 * <p><em>当前层级</em>指的是从当前测试可见的
		 * 上下文层次结构中最底层的 {@code ApplicationContext}。
		 */
		CURRENT_LEVEL;
	}

}
