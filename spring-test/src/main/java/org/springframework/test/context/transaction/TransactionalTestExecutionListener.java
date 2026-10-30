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

package org.springframework.test.context.transaction;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;

/**
 * {@code TestExecutionListener}，通过遵循 Spring 的 {@link Transactional @Transactional} 注解，为在<em>测试管理的事务</em>中执行测试提供支持。
 * <h3>测试管理的事务</h3>
 * <p><em>测试管理的事务</em>是通过此监听器以声明方式或以编程方式通过 {@link TestTransaction} 管理的事务。此类事务不应与<em>Spring 管理的事务</em>（即在为测试加载的 {@code ApplicationContext} 中由 Spring 直接管理的事务）或<em>应用程序管理的事务</em>（即在通过测试调用的应用程序代码中以编程方式管理的事务）混淆。Spring 管理的事务和应用程序管理的事务通常会加入测试管理的事务；但是，如果 Spring 管理或应用程序管理的事务配置为 {@link org.springframework.transaction.annotation.Propagation#REQUIRED REQUIRED} 或 {@link org.springframework.transaction.annotation.Propagation#SUPPORTS SUPPORTS} 以外的任何传播类型，则应谨慎行事。
 * <h3>启用和禁用事务</h3>
 * <p>使用 {@code @Transactional} 标注测试方法会使测试在事务中运行，默认情况下该事务将在测试完成后自动<em>回滚</em>。如果测试类标注了 {@code @Transactional}，则该类层次结构或嵌套类层次结构中的每个测试方法都将在事务中运行。<em>未</em>标注 {@code @Transactional}（在类或方法级别）的测试方法将不在事务中运行。此外，<em>标注了</em> {@code @Transactional} 但将 {@link Transactional#propagation propagation} 类型设置为 {@link org.springframework.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED} 或 {@link org.springframework.transaction.annotation.Propagation#NEVER NEVER} 的测试将不在事务中运行。
 * <h3>声明式回滚和提交行为</h3>
 * <p>默认情况下，测试事务将在测试完成后自动<em>回滚</em>；但是，可以通过类级别和方法级别的 {@link Commit @Commit} 和 {@link Rollback @Rollback} 注解以声明方式配置事务提交和回滚行为。
 * <h3>编程式事务管理</h3>
 * <p>从 Spring Framework 4.1 开始，可以通过 {@link TestTransaction} 中的静态方法以编程方式与测试管理的事务交互。{@code TestTransaction} 可以在<em>测试</em>方法、<em>before</em> 方法和 <em>after</em> 方法中使用。
 * <h3>在事务外执行代码</h3>
 * <p>执行事务性测试时，有时能够在事务外执行某些<em>设置</em>或<em>清理</em>代码很有用。{@code TransactionalTestExecutionListener} 为标注了 {@link BeforeTransaction @BeforeTransaction} 或 {@link AfterTransaction @AfterTransaction} 的方法提供此类支持。从 Spring Framework 4.3 开始，{@code @BeforeTransaction} 和 {@code @AfterTransaction} 也可以声明在基于 Java 8 的接口默认方法上。
 * <h3>配置事务管理器</h3>
 * <p>{@code TransactionalTestExecutionListener} 期望在测试的 Spring {@code ApplicationContext} 中定义 {@link PlatformTransactionManager} bean。如果测试的 {@code ApplicationContext} 中存在多个 {@code PlatformTransactionManager} 实例，可以通过 {@link Transactional @Transactional} 声明<em>限定符</em>（例如 {@code @Transactional("myTxMgr")} 或 {@code @Transactional(transactionManager = "myTxMgr")}），或者由 {@link org.springframework.context.annotation.Configuration @Configuration} 类实现 {@link org.springframework.transaction.annotation.TransactionManagementConfigurer TransactionManagementConfigurer}。有关在测试的 {@code ApplicationContext} 中查找事务管理器所用算法的详情，请参见 {@link TestContextTransactionUtils#retrieveTransactionManager}。
 * <h3>{@code @Transactional} 属性支持</h3>
 * <table border="1">
 * <tr><th>属性</th><th>是否支持测试管理的事务</th></tr>
 * <tr><td>{@link Transactional#value value} 和 {@link Transactional#transactionManager transactionManager}</td><td>是</td></tr>
 * <tr><td>{@link Transactional#propagation propagation}</td>
 * <td>仅支持 {@link org.springframework.transaction.annotation.Propagation#NOT_SUPPORTED NOT_SUPPORTED} 和 {@link org.springframework.transaction.annotation.Propagation#NEVER NEVER}</td></tr>
 * <tr><td>{@link Transactional#isolation isolation}</td><td>否</td></tr>
 * <tr><td>{@link Transactional#timeout timeout}</td><td>否</td></tr>
 * <tr><td>{@link Transactional#readOnly readOnly}</td><td>否</td></tr>
 * <tr><td>{@link Transactional#rollbackFor rollbackFor} 和 {@link Transactional#rollbackForClassName rollbackForClassName}</td>
 * <td>否：请改用 {@link TestTransaction#flagForRollback()}</td></tr>
 * <tr><td>{@link Transactional#noRollbackFor noRollbackFor} 和 {@link Transactional#noRollbackForClassName noRollbackForClassName}</td>
 * <td>否：请改用 {@link TestTransaction#flagForCommit()}</td></tr>
 * </table>
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.transaction.annotation.TransactionManagementConfigurer
 * @see org.springframework.transaction.annotation.Transactional
 * @see org.springframework.test.annotation.Commit
 * @see org.springframework.test.annotation.Rollback
 * @see BeforeTransaction
 * @see AfterTransaction
 * @see TestTransaction
 */
public class TransactionalTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(TransactionalTestExecutionListener.class);

	// Do not require @Transactional test methods to be public.
	@SuppressWarnings("serial")
	protected final TransactionAttributeSource attributeSource = new AnnotationTransactionAttributeSource(false) {

		@Override
		protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
			// @Transactional present in inheritance hierarchy?
			TransactionAttribute result = super.findTransactionAttribute(clazz);
			if (result != null) {
				return result;
			}
			// @Transactional present in enclosing class hierarchy?
			return findTransactionAttributeInEnclosingClassHierarchy(clazz);
		}

		@Nullable
		private TransactionAttribute findTransactionAttributeInEnclosingClassHierarchy(Class<?> clazz) {
			if (TestContextAnnotationUtils.searchEnclosingClass(clazz)) {
				return findTransactionAttribute(clazz.getEnclosingClass());
			}
			return null;
		}
	};


	/**
	 * 返回 {@code 4000}。
	 */
	@Override
	public final int getOrder() {
		return 4000;
	}

	/**
	 * 如果所提供的{@linkplain TestContext 测试上下文}的测试方法配置为在事务中运行，则此方法将运行 {@link BeforeTransaction @BeforeTransaction} 方法并启动一个新事务。
	 * <p>请注意，如果某个 {@code @BeforeTransaction} 方法失败，则不会调用任何剩余的 {@code @BeforeTransaction} 方法，也不会启动事务。
	 * @see Transactional @Transactional
	 * @see #getTransactionManager(TestContext, String)
	 */
	@Override
	public void beforeTestMethod(final TestContext testContext) throws Exception {
		Method testMethod = testContext.getTestMethod();
		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testMethod, "Test method of supplied TestContext must not be null");

		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
		Assert.state(txContext == null, "Cannot start new transaction without ending existing transaction");

		PlatformTransactionManager tm = null;
		TransactionAttribute transactionAttribute = this.attributeSource.getTransactionAttribute(testMethod, testClass);

		if (transactionAttribute != null) {
			transactionAttribute = TestContextTransactionUtils.createDelegatingTransactionAttribute(testContext,
				transactionAttribute);

			if (logger.isDebugEnabled()) {
				logger.debug("Explicit transaction definition [" + transactionAttribute +
						"] found for test context " + testContext);
			}

			if (transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED ||
					transactionAttribute.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {
				return;
			}

			tm = getTransactionManager(testContext, transactionAttribute.getQualifier());
			Assert.state(tm != null,
					() -> "Failed to retrieve PlatformTransactionManager for @Transactional test: " + testContext);
		}

		if (tm != null) {
			txContext = new TransactionContext(testContext, tm, transactionAttribute, isRollback(testContext));
			runBeforeTransactionMethods(testContext);
			txContext.startTransaction();
			TransactionContextHolder.setCurrentTransactionContext(txContext);
		}
	}

	/**
	 * 如果所提供的{@linkplain TestContext 测试上下文}当前有活动事务，则此方法将结束该事务并运行 {@link AfterTransaction @AfterTransaction} 方法。
	 * <p>即使结束事务时发生错误，也保证会调用 {@code @AfterTransaction} 方法。
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		TransactionContext txContext = TransactionContextHolder.removeCurrentTransactionContext();
		// If there was (or perhaps still is) a transaction...
		if (txContext != null) {
			TransactionStatus transactionStatus = txContext.getTransactionStatus();
			try {
				// If the transaction is still active...
				if (transactionStatus != null && !transactionStatus.isCompleted()) {
					txContext.endTransaction();
				}
			}
			finally {
				runAfterTransactionMethods(testContext);
			}
		}
	}

	/**
	 * 为指定的{@linkplain TestContext 测试上下文}运行所有 {@link BeforeTransaction @BeforeTransaction} 方法。但是，如果其中一个方法失败，捕获的异常将以包装的 {@link RuntimeException} 形式重新抛出，其余方法<strong>不会</strong>有机会执行。
	 * @param testContext 当前的测试上下文
	 */
	protected void runBeforeTransactionMethods(TestContext testContext) throws Exception {
		try {
			List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), BeforeTransaction.class);
			Collections.reverse(methods);
			for (Method method : methods) {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing @BeforeTransaction method [" + method + "] for test context " + testContext);
				}
				ReflectionUtils.makeAccessible(method);
				method.invoke(testContext.getTestInstance());
			}
		}
		catch (InvocationTargetException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Exception encountered while executing @BeforeTransaction methods for test context " +
						testContext + ".", ex.getTargetException());
			}
			ReflectionUtils.rethrowException(ex.getTargetException());
		}
	}

	/**
	 * 为指定的{@linkplain TestContext 测试上下文}运行所有 {@link AfterTransaction @AfterTransaction} 方法。如果其中一个方法失败，捕获的异常将作为错误记录，其余方法仍将有机会执行。在所有方法执行完毕后，将重新抛出第一个捕获的异常（如果有）。
	 * @param testContext 当前的测试上下文
	 */
	protected void runAfterTransactionMethods(TestContext testContext) throws Exception {
		Throwable afterTransactionException = null;

		List<Method> methods = getAnnotatedMethods(testContext.getTestClass(), AfterTransaction.class);
		for (Method method : methods) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing @AfterTransaction method [" + method + "] for test context " + testContext);
				}
				ReflectionUtils.makeAccessible(method);
				method.invoke(testContext.getTestInstance());
			}
			catch (InvocationTargetException ex) {
				Throwable targetException = ex.getTargetException();
				if (afterTransactionException == null) {
					afterTransactionException = targetException;
				}
				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
						"] for test context " + testContext, targetException);
			}
			catch (Exception ex) {
				if (afterTransactionException == null) {
					afterTransactionException = ex;
				}
				logger.error("Exception encountered while executing @AfterTransaction method [" + method +
						"] for test context " + testContext, ex);
			}
		}

		if (afterTransactionException != null) {
			ReflectionUtils.rethrowException(afterTransactionException);
		}
	}

	/**
	 * 获取要用于所提供的{@linkplain TestContext 测试上下文}和 {@code qualifier} 的{@linkplain PlatformTransactionManager 事务管理器}。
	 * <p>如果提供的 {@code qualifier} 为 {@code null} 或空，则委托给 {@link #getTransactionManager(TestContext)}。
	 * @param testContext 应为其检索事务管理器的测试上下文
	 * @param qualifier 用于在多个 Bean 匹配之间进行选择的限定符；
	 * 可以为 {@code null} 或空
	 * @return 要使用的事务管理器；如果未找到，则为 {@code null}
	 * @throws BeansException 如果检索事务管理器时发生错误
	 * @see #getTransactionManager(TestContext)
	 */
	@Nullable
	protected PlatformTransactionManager getTransactionManager(TestContext testContext, @Nullable String qualifier) {
		// Look up by type and qualifier from @Transactional
		if (StringUtils.hasText(qualifier)) {
			try {
				// Use autowire-capable factory in order to support extended qualifier matching
				// (only exposed on the internal BeanFactory, not on the ApplicationContext).
				BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

				return BeanFactoryAnnotationUtils.qualifiedBeanOfType(bf, PlatformTransactionManager.class, qualifier);
			}
			catch (RuntimeException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn(String.format(
							"Caught exception while retrieving transaction manager with qualifier '%s' for test context %s",
							qualifier, testContext), ex);
				}
				throw ex;
			}
		}

		// else
		return getTransactionManager(testContext);
	}

	/**
	 * 获取要用于所提供的{@linkplain TestContext 测试上下文}的{@linkplain PlatformTransactionManager 事务管理器}。
	 * <p>默认实现仅委托给 {@link TestContextTransactionUtils#retrieveTransactionManager}。
	 * @param testContext 应为其检索事务管理器的测试上下文
	 * @return 要使用的事务管理器；如果未找到，则为 {@code null}
	 * @throws BeansException 如果检索显式命名的事务管理器时发生错误
	 * @throws IllegalStateException 如果 ApplicationContext 中存在多个 TransactionManagementConfigurer
	 * @see #getTransactionManager(TestContext, String)
	 */
	@Nullable
	protected PlatformTransactionManager getTransactionManager(TestContext testContext) {
		return TestContextTransactionUtils.retrieveTransactionManager(testContext, null);
	}

	/**
	 * 确定对于所提供的{@linkplain TestContext 测试上下文}，默认是否应回滚事务。
	 * <p>支持类级别的 {@link Rollback @Rollback} 或 {@link Commit @Commit}。
	 * @param testContext 应为其检索默认回滚标志的测试上下文
	 * @return 所提供测试上下文的<em>默认回滚</em>标志
	 * @throws Exception 如果确定默认回滚标志时发生错误
	 */
	protected final boolean isDefaultRollback(TestContext testContext) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		Rollback rollback = TestContextAnnotationUtils.findMergedAnnotation(testClass, Rollback.class);
		boolean rollbackPresent = (rollback != null);

		if (rollbackPresent) {
			boolean defaultRollback = rollback.value();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Retrieved default @Rollback(%s) for test class [%s].",
						defaultRollback, testClass.getName()));
			}
			return defaultRollback;
		}

		// else
		return true;
	}

	/**
	 * 通过考虑{@linkplain #isDefaultRollback(TestContext) 默认回滚}标志以及通过 {@link Rollback @Rollback} 注解在方法级别可能的覆盖，确定对于所提供的{@linkplain TestContext 测试上下文}是否应回滚事务。
	 * @param testContext 应为其检索回滚标志的测试上下文
	 * @return 所提供测试上下文的<em>回滚</em>标志
	 * @throws Exception 如果确定回滚标志时发生错误
	 */
	protected final boolean isRollback(TestContext testContext) throws Exception {
		boolean rollback = isDefaultRollback(testContext);
		Rollback rollbackAnnotation =
				AnnotatedElementUtils.findMergedAnnotation(testContext.getTestMethod(), Rollback.class);
		if (rollbackAnnotation != null) {
			boolean rollbackOverride = rollbackAnnotation.value();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"Method-level @Rollback(%s) overrides default rollback [%s] for test context %s.",
						rollbackOverride, rollback, testContext));
			}
			rollback = rollbackOverride;
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format(
						"No method-level @Rollback override: using default rollback [%s] for test context %s.",
						rollback, testContext));
			}
		}
		return rollback;
	}

	/**
	 * 获取所提供的 {@link Class 类}及其超类中所有标注了所提供的 {@code annotationType} 但未被子类中重写方法<em>遮蔽</em>的方法。
	 * <p>接口上的默认方法也会被检测到。
	 * @param clazz 要为其检索注解方法的类
	 * @param annotationType 要搜索的注解类型
	 * @return 所提供类及其超类中的所有注解方法，
	 * 以及注解的接口默认方法
	 */
	private List<Method> getAnnotatedMethods(Class<?> clazz, Class<? extends Annotation> annotationType) {
		MethodFilter methodFilter = ReflectionUtils.USER_DECLARED_METHODS
				.and(method -> AnnotatedElementUtils.hasAnnotation(method, annotationType));
		return Arrays.asList(ReflectionUtils.getUniqueDeclaredMethods(clazz, methodFilter));
	}

}
