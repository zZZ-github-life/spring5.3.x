/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.transaction.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;

/**
 * 根据{@link TransactionPhase}进行调用的{@link EventListener}。
 * 这是{@link TransactionalApplicationListener}的基于注解的等价物。
 *
 * <p>如果事件不是在活动事务中发布的, 则除非显式设置{@link #fallbackExecution}标志,
 * 否则该事件将被丢弃。如果事务正在运行, 则根据其{@code TransactionPhase}处理事件。
 *
 * <p>在标注方法上添加{@link org.springframework.core.annotation.Order @Order},
 * 允许在事务完成之前或之后运行的其他监听器中对该监听器进行优先级排序。
 *
 * <p><b>注意: 事务性事件监听器仅适用于由{@link org.springframework.transaction.PlatformTransactionManager
 * PlatformTransactionManager}管理的线程绑定事务。</b>由
 * {@link org.springframework.transaction.ReactiveTransactionManager ReactiveTransactionManager}管理的
 * 响应式事务使用Reactor上下文而非线程局部变量, 因此从事件监听器的角度来看,
 * 没有它可以参与的兼容活动事务。
 *
 * <p><strong>警告:</strong> 如果{@code TransactionPhase}设置为
 * {@link TransactionPhase#AFTER_COMMIT AFTER_COMMIT}(默认值)、
 * {@link TransactionPhase#AFTER_ROLLBACK AFTER_ROLLBACK}或
 * {@link TransactionPhase#AFTER_COMPLETION AFTER_COMPLETION}, 事务将已经被提交或回滚,
 * 但事务资源可能仍然活跃且可访问。因此, 在此时触发的任何数据访问代码仍将"参与"原始事务,
 * 但更改不会提交到事务资源。详见
 * {@link org.springframework.transaction.support.TransactionSynchronization#afterCompletion(int)
 * TransactionSynchronization.afterCompletion(int)}。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 4.2
 * @see TransactionalApplicationListener
 * @see TransactionalApplicationListenerMethodAdapter
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EventListener
public @interface TransactionalEventListener {

	/**
	 * 绑定事件处理的阶段。
	 * <p>默认阶段为{@link TransactionPhase#AFTER_COMMIT}。
	 * <p>如果没有事务正在运行, 除非显式启用了{@link #fallbackExecution}, 否则事件不会被处理。
	 */
	TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;

	/**
	 * 在没有事务运行时是否仍应处理事件。
	 */
	boolean fallbackExecution() default false;

	/**
	 * {@link #classes}的别名。
	 */
	@AliasFor(annotation = EventListener.class, attribute = "classes")
	Class<?>[] value() default {};

	/**
	 * 此监听器处理的事件类。
	 * <p>如果此属性指定了单个值, 则标注方法可以选择性地接受单个参数。
	 * 但是, 如果此属性指定了多个值, 则标注方法<b>不应</b>声明任何参数。
	 */
	@AliasFor(annotation = EventListener.class, attribute = "classes")
	Class<?>[] classes() default {};

	/**
	 * Spring表达式语言(SpEL)属性, 用于使事件处理条件化。
	 * <p>默认值为{@code ""}, 表示始终处理事件。
	 * @see EventListener#condition
	 */
	@AliasFor(annotation = EventListener.class, attribute = "condition")
	String condition() default "";

	/**
	 * 监听器的可选标识符, 默认为声明方法的完全限定签名
	 * (例如 "mypackage.MyClass.myMethod()")。
	 * @since 5.3
	 * @see EventListener#id
	 * @see TransactionalApplicationListener#getListenerId()
	 */
	@AliasFor(annotation = EventListener.class, attribute = "id")
	String id() default "";

}
