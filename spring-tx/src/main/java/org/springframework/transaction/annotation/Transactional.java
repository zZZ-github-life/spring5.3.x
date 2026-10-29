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

package org.springframework.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.TransactionDefinition;

/**
 * 描述单个方法或类上的事务属性。
 *
 * <p>当此注解在类级别声明时, 它将作为声明类及其子类所有方法的默认值。
 * 请注意, 它不适用于类层次结构中的祖先类; 继承的方法需要在子类中本地重新声明,
 * 以参与子类级别的注解。有关方法可见性约束的详情, 请参阅参考手册的
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction">事务管理</a>章节。
 *
 * <p>此注解通常直接与Spring的{@link org.springframework.transaction.interceptor.RuleBasedTransactionAttribute}
 * 类进行比较, 实际上{@link AnnotationTransactionAttributeSource}会直接将此注解的属性转换为
 * {@code RuleBasedTransactionAttribute}中的属性, 这样Spring的事务支持代码就无需了解注解。
 *
 * <h3>属性语义</h3>
 *
 * <p>如果此注解中未配置自定义回滚规则, 事务将在{@link RuntimeException}和{@link Error}时回滚,
 * 但不会在受检异常时回滚。
 *
 * <p>回滚规则决定在抛出给定异常时是否应回滚事务, 规则基于模式。模式可以是异常类型的完全限定类名
 * 或完全限定类名的子字符串(必须是{@code Throwable}的子类), 目前不支持通配符。例如,
 * {@code "javax.servlet.ServletException"}或{@code "ServletException"}的值将匹配
 * {@code javax.servlet.ServletException}及其子类。
 *
 * <p>回滚规则可通过{@link #rollbackFor}/{@link #noRollbackFor}和
 * {@link #rollbackForClassName}/{@link #noRollbackForClassName}配置,
 * 分别允许将模式指定为{@link Class}引用或{@linkplain String 字符串}。
 * 当异常类型被指定为类引用时, 其完全限定名将用作模式。因此,
 * {@code @Transactional(rollbackFor = example.CustomException.class)}等价于
 * {@code @Transactional(rollbackForClassName = "example.CustomException")}。
 *
 * <p><strong>警告:</strong> 必须仔细考虑模式的特异性以及是否包含包信息(这不是必须的)。
 * 例如, {@code "Exception"}将匹配几乎所有内容, 可能会隐藏其他规则。{@code "java.lang.Exception"}
 * 在{@code "Exception"}意为定义所有受检异常规则时才是正确的。对于更独特的异常名称如
 * {@code "BaseBusinessException"}, 可能不需要使用完全限定类名作为异常模式。此外,
 * 回滚规则可能导致对类似命名的异常和嵌套类的意外匹配。这是因为如果被抛出异常的名称
 * 包含为回滚规则配置的异常模式, 则该异常被视为匹配该回滚规则。例如, 给定一个配置为匹配
 * {@code com.example.CustomException}的规则, 该规则将匹配名为{@code com.example.CustomExceptionV2}
 * 的异常(与{@code CustomException}在同一包中但带有额外后缀的异常)或名为
 * {@code com.example.CustomException$AnotherException}的异常(在{@code CustomException}中声明为嵌套类的异常)。
 *
 * <p>有关此注解中其他属性的语义信息, 请参阅{@link org.springframework.transaction.TransactionDefinition}
 * 和{@link org.springframework.transaction.interceptor.TransactionAttribute}的javadoc。
 *
 * <h3>事务管理</h3>
 *
 * <p>此注解通常与由{@link org.springframework.transaction.PlatformTransactionManager}管理的
 * 线程绑定事务一起工作, 将事务暴露给当前执行线程内的所有数据访问操作。
 * <b>注意: 这不会传播到方法内新启动的线程。</b>
 *
 * <p>或者, 此注解可以界定由{@link org.springframework.transaction.ReactiveTransactionManager}管理的
 * 响应式事务, 它使用Reactor上下文而非线程局部变量。因此, 所有参与的数据访问操作需要在同一响应式管道中
 * 的同一Reactor上下文中执行。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Paluch
 * @since 1.2
 * @see org.springframework.transaction.interceptor.TransactionAttribute
 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute
 * @see org.springframework.transaction.interceptor.RuleBasedTransactionAttribute
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Transactional {

	/**
	 * {@link #transactionManager}的别名。
	 * @see #transactionManager
	 */
	@AliasFor("transactionManager")
	String value() default "";

	/**
	 * 指定事务的<em>限定符</em>值。
	 * <p>可用于确定目标事务管理器, 匹配特定{@link org.springframework.transaction.TransactionManager TransactionManager}
	 * bean定义的限定符值(或bean名称)。
	 * @since 4.2
	 * @see #value
	 * @see org.springframework.transaction.PlatformTransactionManager
	 * @see org.springframework.transaction.ReactiveTransactionManager
	 */
	@AliasFor("value")
	String transactionManager() default "";

	/**
	 * 定义零个或多个事务标签。
	 * <p>标签可用于描述事务, 并由各个事务管理器进行求值。标签可以仅用于描述目的,
	 * 也可以映射到预定义的事务管理器特定选项。
	 * <p>有关事务管理器如何求值事务标签的详情, 请参阅实际事务管理器实现的文档。
	 * @since 5.3
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#getLabels()
	 */
	String[] label() default {};

	/**
	 * 事务传播类型。
	 * <p>默认值为{@link Propagation#REQUIRED}。
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getPropagationBehavior()
	 */
	Propagation propagation() default Propagation.REQUIRED;

	/**
	 * 事务隔离级别。
	 * <p>默认值为{@link Isolation#DEFAULT}。
	 * <p>专用于{@link Propagation#REQUIRED}或{@link Propagation#REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务。如果您希望在参与具有不同隔离级别的现有事务时拒绝隔离级别声明,
	 * 请考虑在事务管理器上将"validateExistingTransactions"标志切换为"true"。
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getIsolationLevel()
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
	 */
	Isolation isolation() default Isolation.DEFAULT;

	/**
	 * 此事务的超时时间(以秒为单位)。
	 * <p>默认使用底层事务系统的默认超时时间。
	 * <p>专用于{@link Propagation#REQUIRED}或{@link Propagation#REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务。
	 * @return the timeout in seconds
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
	 */
	int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;

	/**
	 * 此事务的超时时间(以秒为单位)。
	 * <p>默认使用底层事务系统的默认超时时间。
	 * <p>专用于{@link Propagation#REQUIRED}或{@link Propagation#REQUIRES_NEW},
	 * 因为它仅适用于新启动的事务。
	 * @return the timeout in seconds as a String value, e.g. a placeholder
	 * @since 5.3
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#getTimeout()
	 */
	String timeoutString() default "";

	/**
	 * 布尔标志, 如果事务实际上是只读的, 可以设置为{@code true}, 以允许在运行时进行相应的优化。
	 * <p>默认值为{@code false}。
	 * <p>这仅作为对实际事务子系统的提示; 它<i>不一定</i>会导致写访问尝试失败。
	 * 无法解读只读提示的事务管理器在被要求只读事务时<i>不会</i>抛出异常, 而是默默地忽略该提示。
	 * @see org.springframework.transaction.interceptor.TransactionAttribute#isReadOnly()
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
	 */
	boolean readOnly() default false;

	/**
	 * 定义零个或多个异常{@linkplain Class 类}, 必须是{@link Throwable}的子类,
	 * 指示哪些异常类型必须导致事务回滚。
	 * <p>默认情况下, 事务将在{@link RuntimeException}和{@link Error}时回滚,
	 * 但不会在受检异常(业务异常)时回滚。详见
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)}。
	 * <p>这是构建回滚规则的首选方式(与{@link #rollbackForClassName}相比),
	 * 匹配异常类型、其子类和嵌套类。有关回滚规则语义和可能意外匹配的警告,
	 * 请参阅{@linkplain Transactional 类级别javadoc}。
	 * @see #rollbackForClassName
	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(Class)
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	Class<? extends Throwable>[] rollbackFor() default {};

	/**
	 * 定义零个或多个异常名称模式(异常必须是{@link Throwable}的子类),
	 * 指示哪些异常类型必须导致事务回滚。
	 * <p>有关回滚规则语义、模式和可能意外匹配的警告,
	 * 请参阅{@linkplain Transactional 类级别javadoc}。
	 * @see #rollbackFor
	 * @see org.springframework.transaction.interceptor.RollbackRuleAttribute#RollbackRuleAttribute(String)
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	String[] rollbackForClassName() default {};

	/**
	 * 定义零个或多个异常{@link Class 类}, 必须是{@link Throwable}的子类,
	 * 指示哪些异常类型<b>不应</b>导致事务回滚。
	 * <p>这是构建回滚规则的首选方式(与{@link #noRollbackForClassName}相比),
	 * 匹配异常类型、其子类和嵌套类。有关回滚规则语义和可能意外匹配的警告,
	 * 请参阅{@linkplain Transactional 类级别javadoc}。
	 * @see #noRollbackForClassName
	 * @see org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(Class)
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	Class<? extends Throwable>[] noRollbackFor() default {};

	/**
	 * 定义零个或多个异常名称模式(异常必须是{@link Throwable}的子类),
	 * 指示哪些异常类型<b>不应</b>导致事务回滚。
	 * <p>有关回滚规则语义、模式和可能意外匹配的警告,
	 * 请参阅{@linkplain Transactional 类级别javadoc}。
	 * @see #noRollbackFor
	 * @see org.springframework.transaction.interceptor.NoRollbackRuleAttribute#NoRollbackRuleAttribute(String)
	 * @see org.springframework.transaction.interceptor.DefaultTransactionAttribute#rollbackOn(Throwable)
	 */
	String[] noRollbackForClassName() default {};

}
