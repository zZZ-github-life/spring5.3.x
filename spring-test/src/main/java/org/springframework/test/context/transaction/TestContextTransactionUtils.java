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

import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import org.springframework.transaction.interceptor.DelegatingTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于在 <em>Spring TestContext Framework</em> 中处理事务和数据访问相关 bean 的工具方法。
 * <p>主要用于框架内部。
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class TestContextTransactionUtils {

	/**
	 * {@link DataSource} 的默认 bean 名称：{@code "dataSource"}。
	 */
	public static final String DEFAULT_DATA_SOURCE_NAME = "dataSource";

	/**
	 * {@link PlatformTransactionManager} 的默认 bean 名称：{@code "transactionManager"}。
	 */
	public static final String DEFAULT_TRANSACTION_MANAGER_NAME = "transactionManager";


	private static final Log logger = LogFactory.getLog(TestContextTransactionUtils.class);


	/**
	 * 获取要用于所提供的{@linkplain TestContext 测试上下文}的 {@link DataSource}。
	 * <p>使用以下算法从所提供测试上下文的 {@link org.springframework.context.ApplicationContext ApplicationContext} 中获取 {@code DataSource}：
	 * <ol>
	 * <li>如果提供的 {@code name} 非空，则按类型和名称查找 {@code DataSource}；如果指定名称的 {@code DataSource} 不存在，则抛出 {@link BeansException}。
	 * <li>尝试按类型查找单个 {@code DataSource}。
	 * <li>尝试按类型查找<em>主</em> {@code DataSource}。
	 * <li>尝试按类型和{@linkplain #DEFAULT_DATA_SOURCE_NAME 默认数据源名称}查找 {@code DataSource}。
	 * </ol>
	 * @param testContext the test context for which the {@code DataSource}
	 * should be retrieved; never {@code null}
	 * @param name the name of the {@code DataSource} to retrieve
	 * (may be {@code null} or <em>empty</em>)
	 * @return the {@code DataSource} to use, or {@code null} if not found
	 * @throws BeansException if an error occurs while retrieving an explicitly
	 * named {@code DataSource}
	 */
	@Nullable
	public static DataSource retrieveDataSource(TestContext testContext, @Nullable String name) {
		Assert.notNull(testContext, "TestContext must not be null");
		BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

		try {
			// Look up by type and explicit name
			if (StringUtils.hasText(name)) {
				return bf.getBean(name, DataSource.class);
			}
		}
		catch (BeansException ex) {
			logger.error(String.format("Failed to retrieve DataSource named '%s' for test context %s",
					name, testContext), ex);
			throw ex;
		}

		try {
			if (bf instanceof ListableBeanFactory) {
				ListableBeanFactory lbf = (ListableBeanFactory) bf;

				// Look up single bean by type
				Map<String, DataSource> dataSources =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, DataSource.class);
				if (dataSources.size() == 1) {
					return dataSources.values().iterator().next();
				}

				try {
					// look up single bean by type, with support for 'primary' beans
					return bf.getBean(DataSource.class);
				}
				catch (BeansException ex) {
					logBeansException(testContext, ex, PlatformTransactionManager.class);
				}
			}

			// look up by type and default name
			return bf.getBean(DEFAULT_DATA_SOURCE_NAME, DataSource.class);
		}
		catch (BeansException ex) {
			logBeansException(testContext, ex, DataSource.class);
			return null;
		}
	}

	/**
	 * 获取要用于所提供的{@linkplain TestContext 测试上下文}的{@linkplain PlatformTransactionManager 事务管理器}。
	 * <p>使用以下算法从所提供测试上下文的 {@link org.springframework.context.ApplicationContext ApplicationContext} 中获取事务管理器：
	 * <ol>
	 * <li>如果提供的 {@code name} 非空，则按类型和显式名称查找事务管理器；如果指定名称的事务管理器不存在，则抛出 {@link BeansException}。
	 * <li>如果存在 {@link TransactionManagementConfigurer}，则尝试通过它查找事务管理器。
	 * <li>尝试按类型查找单个事务管理器。
	 * <li>尝试按类型查找<em>主</em>事务管理器。
	 * <li>尝试按类型和{@linkplain #DEFAULT_TRANSACTION_MANAGER_NAME 默认事务管理器名称}查找事务管理器。
	 * </ol>
	 * @param testContext the test context for which the transaction manager
	 * should be retrieved; never {@code null}
	 * @param name the name of the transaction manager to retrieve
	 * (may be {@code null} or <em>empty</em>)
	 * @return the transaction manager to use, or {@code null} if not found
	 * @throws BeansException if an error occurs while retrieving an explicitly
	 * named transaction manager
	 * @throws IllegalStateException if more than one TransactionManagementConfigurer
	 * exists in the ApplicationContext
	 */
	@Nullable
	public static PlatformTransactionManager retrieveTransactionManager(TestContext testContext, @Nullable String name) {
		Assert.notNull(testContext, "TestContext must not be null");
		BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

		try {
			// Look up by type and explicit name
			if (StringUtils.hasText(name)) {
				return bf.getBean(name, PlatformTransactionManager.class);
			}
		}
		catch (BeansException ex) {
			logger.error(String.format("Failed to retrieve transaction manager named '%s' for test context %s",
					name, testContext), ex);
			throw ex;
		}

		try {
			if (bf instanceof ListableBeanFactory) {
				ListableBeanFactory lbf = (ListableBeanFactory) bf;

				// Look up single TransactionManagementConfigurer
				Map<String, TransactionManagementConfigurer> configurers =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, TransactionManagementConfigurer.class);
				Assert.state(configurers.size() <= 1,
						"Only one TransactionManagementConfigurer may exist in the ApplicationContext");
				if (configurers.size() == 1) {
					TransactionManager tm = configurers.values().iterator().next().annotationDrivenTransactionManager();
					Assert.state(tm instanceof PlatformTransactionManager, () ->
						"Transaction manager specified via TransactionManagementConfigurer " +
						"is not a PlatformTransactionManager: " + tm);
					return (PlatformTransactionManager) tm;
				}

				// Look up single bean by type
				Map<String, PlatformTransactionManager> txMgrs =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, PlatformTransactionManager.class);
				if (txMgrs.size() == 1) {
					return txMgrs.values().iterator().next();
				}

				try {
					// Look up single bean by type, with support for 'primary' beans
					return bf.getBean(PlatformTransactionManager.class);
				}
				catch (BeansException ex) {
					logBeansException(testContext, ex, PlatformTransactionManager.class);
				}
			}

			// look up by type and default name
			return bf.getBean(DEFAULT_TRANSACTION_MANAGER_NAME, PlatformTransactionManager.class);
		}
		catch (BeansException ex) {
			logBeansException(testContext, ex, PlatformTransactionManager.class);
			return null;
		}
	}

	private static void logBeansException(TestContext testContext, BeansException ex, Class<?> beanType) {
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Caught exception while retrieving %s for test context %s",
				beanType.getSimpleName(), testContext), ex);
		}
	}

	/**
	 * 为所提供的目标 {@link TransactionAttribute} 和 {@link TestContext} 创建一个委托 {@link TransactionAttribute}，并使用测试类和测试方法的名称构建事务名称。
	 * @param testContext the {@code TestContext} upon which to base the name
	 * @param targetAttribute the {@code TransactionAttribute} to delegate to
	 * @return the delegating {@code TransactionAttribute}
	 */
	public static TransactionAttribute createDelegatingTransactionAttribute(
			TestContext testContext, TransactionAttribute targetAttribute) {

		Assert.notNull(testContext, "TestContext must not be null");
		Assert.notNull(targetAttribute, "Target TransactionAttribute must not be null");
		return new TestContextTransactionAttribute(targetAttribute, testContext);
	}


	@SuppressWarnings("serial")
	private static class TestContextTransactionAttribute extends DelegatingTransactionAttribute {

		private final String name;

		public TestContextTransactionAttribute(TransactionAttribute targetAttribute, TestContext testContext) {
			super(targetAttribute);
			this.name = ClassUtils.getQualifiedMethodName(testContext.getTestMethod(), testContext.getTestClass());
		}

		@Override
		@Nullable
		public String getName() {
			return this.name;
		}
	}

}
