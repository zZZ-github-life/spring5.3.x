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

package org.springframework.test.context.jdbc;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlConfig.ErrorMode;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.transaction.TestContextTransactionUtils;
import org.springframework.test.context.util.TestContextResourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@code TestExecutionListener}，为执行通过 {@link Sql @Sql} 注解配置的 SQL {@link Sql#scripts 脚本}和内联 {@link Sql#statements 语句}提供支持。
 * <p>根据 {@link Sql#executionPhase executionPhase} 标志的配置值，脚本和内联语句将在相应{@linkplain java.lang.reflect.Method 测试方法}执行{@linkplain #beforeTestMethod(TestContext) 之前}或{@linkplain #afterTestMethod(TestContext) 之后}执行。
 * <p>根据 {@link SqlConfig#transactionMode} 的配置值以及是否存在事务管理器，脚本和内联语句将在无事务、现有 Spring 管理的事务内或隔离事务中执行。
 * <h3>脚本资源</h3>
 * <p>有关默认脚本检测以及如何解释脚本资源位置的详情，请参见 {@link Sql#scripts}。
 * <h3>必需的 Spring Bean</h3>
 * <p>必须为相应测试在 Spring {@link ApplicationContext} 中将 {@link PlatformTransactionManager} <em>和</em> {@link DataSource}、仅 {@link PlatformTransactionManager} 或仅 {@link DataSource} 定义为 bean。有关允许的配置组合以及用于定位这些 bean 的算法详情，请查阅 {@link SqlConfig#transactionMode}、{@link SqlConfig#transactionManager}、{@link SqlConfig#dataSource}、{@link TestContextTransactionUtils#retrieveDataSource} 和 {@link TestContextTransactionUtils#retrieveTransactionManager} 的 javadoc。
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @since 4.1
 * @see Sql
 * @see SqlConfig
 * @see SqlGroup
 * @see org.springframework.test.context.transaction.TestContextTransactionUtils
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 * @see org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
 * @see org.springframework.jdbc.datasource.init.ScriptUtils
 */
public class SqlScriptsTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(SqlScriptsTestExecutionListener.class);


	/**
	 * 返回 {@code 5000}。
	 */
	@Override
	public final int getOrder() {
		return 5000;
	}

	/**
	 * 在当前测试方法<em>之前</em>为所提供的 {@link TestContext} 执行通过 {@link Sql @Sql} 配置的 SQL 脚本。
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) {
		executeSqlScripts(testContext, ExecutionPhase.BEFORE_TEST_METHOD);
	}

	/**
	 * 在当前测试方法<em>之后</em>为所提供的 {@link TestContext} 执行通过 {@link Sql @Sql} 配置的 SQL 脚本。
	 */
	@Override
	public void afterTestMethod(TestContext testContext) {
		executeSqlScripts(testContext, ExecutionPhase.AFTER_TEST_METHOD);
	}

	/**
	 * 为所提供的 {@link TestContext} 和 {@link ExecutionPhase} 执行通过 {@link Sql @Sql} 配置的 SQL 脚本。
	 */
	private void executeSqlScripts(TestContext testContext, ExecutionPhase executionPhase) {
		Method testMethod = testContext.getTestMethod();
		Class<?> testClass = testContext.getTestClass();

		if (mergeSqlAnnotations(testContext)) {
			executeSqlScripts(getSqlAnnotationsFor(testClass), testContext, executionPhase, true);
			executeSqlScripts(getSqlAnnotationsFor(testMethod), testContext, executionPhase, false);
		}
		else {
			Set<Sql> methodLevelSqlAnnotations = getSqlAnnotationsFor(testMethod);
			if (!methodLevelSqlAnnotations.isEmpty()) {
				executeSqlScripts(methodLevelSqlAnnotations, testContext, executionPhase, false);
			}
			else {
				executeSqlScripts(getSqlAnnotationsFor(testClass), testContext, executionPhase, true);
			}
		}
	}

	/**
	 * 确定方法级 {@code @Sql} 注解是否应与类级 {@code @Sql} 注解合并。
	 */
	private boolean mergeSqlAnnotations(TestContext testContext) {
		SqlMergeMode sqlMergeMode = getSqlMergeModeFor(testContext.getTestMethod());
		if (sqlMergeMode == null) {
			sqlMergeMode = getSqlMergeModeFor(testContext.getTestClass());
		}
		return (sqlMergeMode != null && sqlMergeMode.value() == MergeMode.MERGE);
	}

	/**
	 * 获取在所提供的类上声明的 {@code @SqlMergeMode} 注解。
	 */
	@Nullable
	private SqlMergeMode getSqlMergeModeFor(Class<?> clazz) {
		return TestContextAnnotationUtils.findMergedAnnotation(clazz, SqlMergeMode.class);
	}

	/**
	 * 获取在所提供的方法上声明的 {@code @SqlMergeMode} 注解。
	 */
	@Nullable
	private SqlMergeMode getSqlMergeModeFor(Method method) {
		return AnnotatedElementUtils.findMergedAnnotation(method, SqlMergeMode.class);
	}

	/**
	 * 获取在所提供的类上声明的 {@code @Sql} 注解。
	 */
	private Set<Sql> getSqlAnnotationsFor(Class<?> clazz) {
		return TestContextAnnotationUtils.getMergedRepeatableAnnotations(clazz, Sql.class);
	}

	/**
	 * 获取在所提供的方法上声明的 {@code @Sql} 注解。
	 */
	private Set<Sql> getSqlAnnotationsFor(Method method) {
		return AnnotatedElementUtils.getMergedRepeatableAnnotations(method, Sql.class, SqlGroup.class);
	}

	/**
	 * 为所提供的 {@link Sql @Sql} 注解执行 SQL 脚本。
	 */
	private void executeSqlScripts(
			Set<Sql> sqlAnnotations, TestContext testContext, ExecutionPhase executionPhase, boolean classLevel) {

		sqlAnnotations.forEach(sql -> executeSqlScripts(sql, executionPhase, testContext, classLevel));
	}

	/**
	 * 为给定的 {@link ExecutionPhase} 和 {@link TestContext} 执行通过所提供的 {@link Sql @Sql} 注解配置的 SQL 脚本。
	 * <p>必须特别小心，以正确支持配置的 {@link SqlConfig#transactionMode}。
	 * @param sql the {@code @Sql} annotation to parse
	 * @param executionPhase the current execution phase
	 * @param testContext the current {@code TestContext}
	 * @param classLevel {@code true} if {@link Sql @Sql} was declared at the class level
	 */
	private void executeSqlScripts(
			Sql sql, ExecutionPhase executionPhase, TestContext testContext, boolean classLevel) {

		if (executionPhase != sql.executionPhase()) {
			return;
		}

		MergedSqlConfig mergedSqlConfig = new MergedSqlConfig(sql.config(), testContext.getTestClass());
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Processing %s for execution phase [%s] and test context %s.",
					mergedSqlConfig, executionPhase, testContext));
		}

		String[] scripts = getScripts(sql, testContext, classLevel);
		scripts = TestContextResourceUtils.convertToClasspathResourcePaths(testContext.getTestClass(), scripts);
		List<Resource> scriptResources = TestContextResourceUtils.convertToResourceList(
				testContext.getApplicationContext(), scripts);
		for (String stmt : sql.statements()) {
			if (StringUtils.hasText(stmt)) {
				stmt = stmt.trim();
				scriptResources.add(new ByteArrayResource(stmt.getBytes(), "from inlined SQL statement: " + stmt));
			}
		}

		ResourceDatabasePopulator populator = createDatabasePopulator(mergedSqlConfig);
		populator.setScripts(scriptResources.toArray(new Resource[0]));
		if (logger.isDebugEnabled()) {
			logger.debug("Executing SQL scripts: " + ObjectUtils.nullSafeToString(scriptResources));
		}

		String dsName = mergedSqlConfig.getDataSource();
		String tmName = mergedSqlConfig.getTransactionManager();
		DataSource dataSource = TestContextTransactionUtils.retrieveDataSource(testContext, dsName);
		PlatformTransactionManager txMgr = TestContextTransactionUtils.retrieveTransactionManager(testContext, tmName);
		boolean newTxRequired = (mergedSqlConfig.getTransactionMode() == TransactionMode.ISOLATED);

		if (txMgr == null) {
			Assert.state(!newTxRequired, () -> String.format("Failed to execute SQL scripts for test context %s: " +
					"cannot execute SQL scripts using Transaction Mode " +
					"[%s] without a PlatformTransactionManager.", testContext, TransactionMode.ISOLATED));
			Assert.state(dataSource != null, () -> String.format("Failed to execute SQL scripts for test context %s: " +
					"supply at least a DataSource or PlatformTransactionManager.", testContext));
			// Execute scripts directly against the DataSource
			populator.execute(dataSource);
		}
		else {
			DataSource dataSourceFromTxMgr = getDataSourceFromTransactionManager(txMgr);
			// Ensure user configured an appropriate DataSource/TransactionManager pair.
			if (dataSource != null && dataSourceFromTxMgr != null && !sameDataSource(dataSource, dataSourceFromTxMgr)) {
				throw new IllegalStateException(String.format("Failed to execute SQL scripts for test context %s: " +
						"the configured DataSource [%s] (named '%s') is not the one associated with " +
						"transaction manager [%s] (named '%s').", testContext, dataSource.getClass().getName(),
						dsName, txMgr.getClass().getName(), tmName));
			}
			if (dataSource == null) {
				dataSource = dataSourceFromTxMgr;
				Assert.state(dataSource != null, () -> String.format("Failed to execute SQL scripts for " +
						"test context %s: could not obtain DataSource from transaction manager [%s] (named '%s').",
						testContext, txMgr.getClass().getName(), tmName));
			}
			final DataSource finalDataSource = dataSource;
			int propagation = (newTxRequired ? TransactionDefinition.PROPAGATION_REQUIRES_NEW :
					TransactionDefinition.PROPAGATION_REQUIRED);
			TransactionAttribute txAttr = TestContextTransactionUtils.createDelegatingTransactionAttribute(
					testContext, new DefaultTransactionAttribute(propagation));
			new TransactionTemplate(txMgr, txAttr).executeWithoutResult(s -> populator.execute(finalDataSource));
		}
	}

	@NonNull
	private ResourceDatabasePopulator createDatabasePopulator(MergedSqlConfig mergedSqlConfig) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setSqlScriptEncoding(mergedSqlConfig.getEncoding());
		populator.setSeparator(mergedSqlConfig.getSeparator());
		populator.setCommentPrefixes(mergedSqlConfig.getCommentPrefixes());
		populator.setBlockCommentStartDelimiter(mergedSqlConfig.getBlockCommentStartDelimiter());
		populator.setBlockCommentEndDelimiter(mergedSqlConfig.getBlockCommentEndDelimiter());
		populator.setContinueOnError(mergedSqlConfig.getErrorMode() == ErrorMode.CONTINUE_ON_ERROR);
		populator.setIgnoreFailedDrops(mergedSqlConfig.getErrorMode() == ErrorMode.IGNORE_FAILED_DROPS);
		return populator;
	}

	/**
	 * 确定两个数据源是否实际上相同，必要时解包代理以比较目标实例。
	 * @since 5.3.4
	 * @see TransactionSynchronizationUtils#unwrapResourceIfNecessary(Object)
	 */
	private static boolean sameDataSource(DataSource ds1, DataSource ds2) {
		return TransactionSynchronizationUtils.unwrapResourceIfNecessary(ds1)
					.equals(TransactionSynchronizationUtils.unwrapResourceIfNecessary(ds2));
	}

	@Nullable
	private DataSource getDataSourceFromTransactionManager(PlatformTransactionManager transactionManager) {
		try {
			Method getDataSourceMethod = transactionManager.getClass().getMethod("getDataSource");
			Object obj = ReflectionUtils.invokeMethod(getDataSourceMethod, transactionManager);
			if (obj instanceof DataSource) {
				return (DataSource) obj;
			}
		}
		catch (Exception ex) {
			// ignore
		}
		return null;
	}

	private String[] getScripts(Sql sql, TestContext testContext, boolean classLevel) {
		String[] scripts = sql.scripts();
		if (ObjectUtils.isEmpty(scripts) && ObjectUtils.isEmpty(sql.statements())) {
			scripts = new String[] {detectDefaultScript(testContext, classLevel)};
		}
		return scripts;
	}

	/**
	 * 通过实现 {@link Sql#scripts} 中定义的算法检测默认 SQL 脚本。
	 */
	private String detectDefaultScript(TestContext testContext, boolean classLevel) {
		Class<?> clazz = testContext.getTestClass();
		Method method = testContext.getTestMethod();
		String elementType = (classLevel ? "class" : "method");
		String elementName = (classLevel ? clazz.getName() : method.toString());

		String resourcePath = ClassUtils.convertClassNameToResourcePath(clazz.getName());
		if (!classLevel) {
			resourcePath += "." + method.getName();
		}
		resourcePath += ".sql";

		String prefixedResourcePath = ResourceUtils.CLASSPATH_URL_PREFIX + resourcePath;
		ClassPathResource classPathResource = new ClassPathResource(resourcePath);

		if (classPathResource.exists()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Detected default SQL script \"%s\" for test %s [%s]",
						prefixedResourcePath, elementType, elementName));
			}
			return prefixedResourcePath;
		}
		else {
			String msg = String.format("Could not detect default SQL script for test %s [%s]: " +
					"%s does not exist. Either declare statements or scripts via @Sql or make the " +
					"default SQL script available.", elementType, elementName, classPathResource);
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
