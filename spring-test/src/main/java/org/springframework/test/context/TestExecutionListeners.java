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
 * {@code TestExecutionListeners} 定义了类级元数据，用于配置应向 {@link TestContextManager} 注册哪些 {@link TestExecutionListener TestExecutionListeners}。
 * <p>{@code @TestExecutionListeners} 用于为特定测试类、其子类及其嵌套类注册监听器。如果希望全局注册监听器，应通过 {@link TestExecutionListener} 中描述的自动发现机制进行注册。
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>。从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link NestedTestConfiguration @NestedTestConfiguration}。
 * <h3>切换到默认 {@code TestExecutionListener} 实现</h3>
 * <p>如果扩展了标注有 {@code @TestExecutionListeners} 的类，并且需要切换到<em>默认</em>监听器集合，可以按如下方式为类添加注解。
 * <pre class="code">
 * // Switch to default listeners
 * &#064;TestExecutionListeners(listeners = {}, inheritListeners = false, mergeMode = MERGE_WITH_DEFAULTS)
 * class MyTests extends BaseTests {
 * // ...
 * }
 * </pre>
 *
 * @author Sam Brannen
 * @since 2.5
 * @see TestExecutionListener
 * @see TestContextManager
 * @see ContextConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestExecutionListeners {

	/**
	 * 作为 {@link #listeners} 的别名。
	 * <p>此属性<strong>不得</strong>与 {@link #listeners} 一起使用，但可以代替 {@link #listeners} 使用。
	 */
	@AliasFor("listeners")
	Class<? extends TestExecutionListener>[] value() default {};

	/**
	 * 要注册到 {@link TestContextManager} 的 {@link TestExecutionListener TestExecutionListeners}。
	 * <p>此属性<strong>不得</strong>与 {@link #value} 一起使用，但可以代替 {@link #value} 使用。
	 * @see org.springframework.test.context.web.ServletTestExecutionListener
	 * @see org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
	 * @see org.springframework.test.context.event.ApplicationEventsTestExecutionListener
	 * @see org.springframework.test.context.support.DependencyInjectionTestExecutionListener
	 * @see org.springframework.test.context.support.DirtiesContextTestExecutionListener
	 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
	 * @see org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
	 * @see org.springframework.test.context.event.EventPublishingTestExecutionListener
	 */
	@AliasFor("value")
	Class<? extends TestExecutionListener>[] listeners() default {};

	/**
	 * 是否应<em>继承</em>超类和封闭类中的 {@link #listeners TestExecutionListeners}。
	 * <p>默认值为 {@code true}，这意味着带注解的类将<em>继承</em>带注解的超类或封闭类所定义的监听器。具体而言，带注解类的监听器将追加到带注解的超类或封闭类所定义的监听器列表末尾。因此，子类和嵌套类可以选择<em>扩展</em>监听器列表。在以下示例中，{@code AbstractBaseTest} 将配置 {@code DependencyInjectionTestExecutionListener} 和 {@code DirtiesContextTestExecutionListener}；而 {@code TransactionalTest} 将按该顺序配置 {@code DependencyInjectionTestExecutionListener}、{@code DirtiesContextTestExecutionListener} <strong>以及</strong> {@code TransactionalTestExecutionListener}。
	 * <pre class="code">
	 * &#064;TestExecutionListeners({
	 *     DependencyInjectionTestExecutionListener.class,
	 *     DirtiesContextTestExecutionListener.class
	 * })
	 * public abstract class AbstractBaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;TestExecutionListeners(TransactionalTestExecutionListener.class)
	 * public class TransactionalTest extends AbstractBaseTest {
	 *     // ...
	 * }</pre>
	 * <p>如果将 {@code inheritListeners} 设置为 {@code false}，则带注解类的监听器将<em>遮蔽</em>并实际上替换超类或封闭类定义的任何监听器。
	 */
	boolean inheritListeners() default true;

	/**
	 * 当 {@code @TestExecutionListeners} 声明在<strong>不</strong>从超类或封闭类继承监听器的类上时要使用的<em>合并模式</em>。
	 * <p>可以设置为 {@link MergeMode#MERGE_WITH_DEFAULTS MERGE_WITH_DEFAULTS}，以便将本地声明的监听器与默认监听器<em>合并</em>。
	 * <p>如果监听器继承自超类或封闭类，则忽略此模式。
	 * <p>为了向后兼容，默认为 {@link MergeMode#REPLACE_DEFAULTS REPLACE_DEFAULTS}。
	 * @see MergeMode
	 * @since 4.1
	 */
	MergeMode mergeMode() default MergeMode.REPLACE_DEFAULTS;


	/**
	 * 枚举<em>模式</em>，用于规定当 {@code @TestExecutionListeners} 声明在<strong>不</strong>从超类或封闭类继承监听器的类上时，显式声明的监听器是否与默认监听器合并。
	 * @since 4.1
	 */
	enum MergeMode {

		/**
		 * 表示本地声明的监听器应替换默认监听器。
		 */
		REPLACE_DEFAULTS,

		/**
		 * 表示本地声明的监听器应与默认监听器合并。
		 * <p>合并算法会确保从列表中移除重复项，并确保合并后的监听器集合按 {@link org.springframework.core.annotation.AnnotationAwareOrderComparator AnnotationAwareOrderComparator} 的语义进行排序。如果监听器实现了 {@link org.springframework.core.Ordered Ordered} 或标注了 {@link org.springframework.core.annotation.Order @Order}，则可以影响其与默认监听器合并的位置；否则，本地声明的监听器在合并时只会追加到默认监听器列表末尾。
		 *
		 */
		MERGE_WITH_DEFAULTS
	}

}
