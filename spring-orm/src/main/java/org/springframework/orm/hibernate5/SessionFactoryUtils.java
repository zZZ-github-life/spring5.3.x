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

package org.springframework.orm.hibernate5;

import java.lang.reflect.Method;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.PersistentObjectException;
import org.hibernate.PessimisticLockException;
import org.hibernate.PropertyValueException;
import org.hibernate.QueryException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.WrongClassException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.dialect.lock.PessimisticEntityLockException;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.service.UnknownServiceException;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 提供 Hibernate Session 处理方法的辅助类。
 * 还提供异常转换支持。
 *
 * <p>供 {@link HibernateTransactionManager} 内部使用。
 * 也可以直接在应用程序代码中使用。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see HibernateExceptionTranslator
 * @see HibernateTransactionManager
 */
public abstract class SessionFactoryUtils {

	/**
	 * 用于清理 Hibernate Session 的 TransactionSynchronization 对象的顺序值。
	 * 返回 {@code DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100}，
	 * 以便在 JDBC Connection 清理（如有）之前执行 Session 清理。
	 * @see DataSourceUtils#CONNECTION_SYNCHRONIZATION_ORDER
	 */
	public static final int SESSION_SYNCHRONIZATION_ORDER =
			DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER - 100;

	static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);


	/**
	 * 在给定的 Hibernate Session 上触发刷新，并相应地转换常规的
	 * {@link HibernateException} 实例以及 Hibernate 5.2 的
	 * {@link PersistenceException} 包装器。
	 * @param session 要刷新的 Hibernate Session
	 * @param synch 此刷新是否由事务同步触发
	 * @throws DataAccessException 如果刷新失败
	 * @since 4.3.2
	 */
	static void flush(Session session, boolean synch) throws DataAccessException {
		if (synch) {
			logger.debug("Flushing Hibernate Session on transaction synchronization");
		}
		else {
			logger.debug("Flushing Hibernate Session on explicit request");
		}
		try {
			session.flush();
		}
		catch (HibernateException ex) {
			throw convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}

	}

	/**
	 * 执行 Hibernate Session 的实际关闭，
	 * 捕获并记录抛出的任何清理异常。
	 * @param session 要关闭的 Hibernate Session（可以为 {@code null}）
	 * @see Session#close()
	 */
	public static void closeSession(@Nullable Session session) {
		if (session != null) {
			try {
				if (session.isOpen()) {
					session.close();
				}
			}
			catch (Throwable ex) {
				logger.error("Failed to release Hibernate Session", ex);
			}
		}
	}

	/**
	 * 确定给定 SessionFactory 的 DataSource。
	 * @param sessionFactory 要检查的 SessionFactory
	 * @return DataSource；如果未找到则返回 {@code null}
	 * @see ConnectionProvider
	 */
	@Nullable
	public static DataSource getDataSource(SessionFactory sessionFactory) {
		Method getProperties = ClassUtils.getMethodIfAvailable(sessionFactory.getClass(), "getProperties");
		if (getProperties != null) {
			Map<?, ?> props = (Map<?, ?>) ReflectionUtils.invokeMethod(getProperties, sessionFactory);
			if (props != null) {
				Object dataSourceValue = props.get(Environment.DATASOURCE);
				if (dataSourceValue instanceof DataSource) {
					return (DataSource) dataSourceValue;
				}
			}
		}
		if (sessionFactory instanceof SessionFactoryImplementor) {
			SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;
			try {
				ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);
				if (cp != null) {
					return cp.unwrap(DataSource.class);
				}
			}
			catch (UnknownServiceException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No ConnectionProvider found - cannot determine DataSource for SessionFactory: " + ex);
				}
			}
		}
		return null;
	}

	/**
	 * 将给定的 HibernateException 转换为 {@code org.springframework.dao} 层次结构中
	 * 相应的异常。
	 * @param ex 发生的 HibernateException
	 * @return 相应的 DataAccessException 实例
	 * @see HibernateExceptionTranslator#convertHibernateAccessException
	 * @see HibernateTransactionManager#convertHibernateAccessException
	 */
	public static DataAccessException convertHibernateAccessException(HibernateException ex) {
		if (ex instanceof JDBCConnectionException) {
			return new DataAccessResourceFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof SQLGrammarException) {
			SQLGrammarException jdbcEx = (SQLGrammarException) ex;
			return new InvalidDataAccessResourceUsageException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof QueryTimeoutException) {
			QueryTimeoutException jdbcEx = (QueryTimeoutException) ex;
			return new org.springframework.dao.QueryTimeoutException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof LockAcquisitionException) {
			LockAcquisitionException jdbcEx = (LockAcquisitionException) ex;
			return new CannotAcquireLockException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof PessimisticLockException) {
			PessimisticLockException jdbcEx = (PessimisticLockException) ex;
			return new PessimisticLockingFailureException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof ConstraintViolationException) {
			ConstraintViolationException jdbcEx = (ConstraintViolationException) ex;
			return new DataIntegrityViolationException(ex.getMessage()  + "; SQL [" + jdbcEx.getSQL() +
					"]; constraint [" + jdbcEx.getConstraintName() + "]", ex);
		}
		if (ex instanceof DataException) {
			DataException jdbcEx = (DataException) ex;
			return new DataIntegrityViolationException(ex.getMessage() + "; SQL [" + jdbcEx.getSQL() + "]", ex);
		}
		if (ex instanceof JDBCException) {
			return new HibernateJdbcException((JDBCException) ex);
		}
		// end of JDBCException (subclass) handling

		if (ex instanceof QueryException) {
			return new HibernateQueryException((QueryException) ex);
		}
		if (ex instanceof NonUniqueResultException) {
			return new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
		if (ex instanceof NonUniqueObjectException) {
			return new DuplicateKeyException(ex.getMessage(), ex);
		}
		if (ex instanceof PropertyValueException) {
			return new DataIntegrityViolationException(ex.getMessage(), ex);
		}
		if (ex instanceof PersistentObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof TransientObjectException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof ObjectDeletedException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof UnresolvableObjectException) {
			return new HibernateObjectRetrievalFailureException((UnresolvableObjectException) ex);
		}
		if (ex instanceof WrongClassException) {
			return new HibernateObjectRetrievalFailureException((WrongClassException) ex);
		}
		if (ex instanceof StaleObjectStateException) {
			return new HibernateOptimisticLockingFailureException((StaleObjectStateException) ex);
		}
		if (ex instanceof StaleStateException) {
			return new HibernateOptimisticLockingFailureException((StaleStateException) ex);
		}
		if (ex instanceof OptimisticEntityLockException) {
			return new HibernateOptimisticLockingFailureException((OptimisticEntityLockException) ex);
		}
		if (ex instanceof PessimisticEntityLockException) {
			if (ex.getCause() instanceof LockAcquisitionException) {
				return new CannotAcquireLockException(ex.getMessage(), ex.getCause());
			}
			return new PessimisticLockingFailureException(ex.getMessage(), ex);
		}

		// fallback
		return new HibernateSystemException(ex);
	}

}
