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

package org.springframework.orm.hibernate5;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.query.Query;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 简化 Hibernate 数据访问代码的辅助类。按照
 * {@code org.springframework.dao} 异常层次结构，
 * 自动将 HibernateExceptions 转换为 DataAccessExceptions。
 *
 * <p>核心方法是 {@code execute}，支持实现 {@link HibernateCallback} 接口的
 * Hibernate 访问代码。它提供 Hibernate Session 处理，
 * 使得 HibernateCallback 实现和调用代码都无需显式关心
 * Hibernate Session 的获取/关闭，也无需处理 Session 生命周期异常。
 * 对于典型的单步操作，提供了各种便捷方法（find、load、saveOrUpdate、delete）。
 *
 * <p>可以在服务实现中通过直接实例化并传入 SessionFactory 引用来使用，
 * 也可以在应用程序上下文中准备好，然后作为 bean 引用注入服务。
 * 注意：SessionFactory 应始终在应用程序上下文中配置为 bean，
 * 第一种情况下直接传给服务，第二种情况下传给准备好的模板。
 *
 * <p><b>注意：Hibernate 访问代码也可以针对原生 Hibernate
 * {@link Session} 编写。因此，对于新启动的项目，请考虑采用针对
 * {@link SessionFactory#getCurrentSession()} 编码的标准 Hibernate 风格。
 * 或者，使用 {@link #execute(HibernateCallback)} 配合针对回调提供的
 * {@code Session} 的 Java 8 lambda 代码块，这同样能产生优雅的代码，
 * 并与 Hibernate Session 生命周期解耦。此 HibernateTemplate 上的其余操作
 * 目前已被弃用，主要作为现有应用程序中旧版 Hibernate 3.x/4.x 数据访问代码的
 * 迁移辅助工具存在。</b>
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setSessionFactory
 * @see HibernateCallback
 * @see Session
 * @see LocalSessionFactoryBean
 * @see HibernateTransactionManager
 * @see org.springframework.orm.hibernate5.support.OpenSessionInViewFilter
 * @see org.springframework.orm.hibernate5.support.OpenSessionInViewInterceptor
 */
public class HibernateTemplate implements HibernateOperations, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private SessionFactory sessionFactory;

	@Nullable
	private String[] filterNames;

	private boolean exposeNativeSession = false;

	private boolean checkWriteOperations = true;

	private boolean cacheQueries = false;

	@Nullable
	private String queryCacheRegion;

	private int fetchSize = 0;

	private int maxResults = 0;


	/**
	 * 创建一个新的 HibernateTemplate 实例。
	 */
	public HibernateTemplate() {
	}

	/**
	 * 创建一个新的 HibernateTemplate 实例。
	 * @param sessionFactory the SessionFactory to create Sessions with
	 */
	public HibernateTemplate(SessionFactory sessionFactory) {
		setSessionFactory(sessionFactory);
		afterPropertiesSet();
	}


	/**
	 * 设置用于创建 Hibernate Session 的 Hibernate SessionFactory。
	 */
	public void setSessionFactory(@Nullable SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 返回用于创建 Hibernate Session 的 Hibernate SessionFactory。
	 */
	@Nullable
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	/**
	 * 获取实际使用的 SessionFactory。
	 * @return the SessionFactory (never {@code null})
	 * @throws IllegalStateException in case of no SessionFactory set
	 * @since 5.0
	 */
	protected final SessionFactory obtainSessionFactory() {
		SessionFactory sessionFactory = getSessionFactory();
		Assert.state(sessionFactory != null, "No SessionFactory set");
		return sessionFactory;
	}

	/**
	 * 设置要为此访问器所使用的所有 Session 激活的一个或多个 Hibernate 过滤器名称。
	 * <p>每个过滤器都会在每个操作开始时启用，
	 * 并在操作结束时相应地禁用。
	 * 这既适用于新打开的 Session，也适用于现有的
	 * Session（例如，在事务内）。
	 * @see #enableFilters(Session)
	 * @see Session#enableFilter(String)
	 */
	public void setFilterNames(@Nullable String... filterNames) {
		this.filterNames = filterNames;
	}

	/**
	 * 返回要激活的 Hibernate 过滤器名称（如果有）。
	 */
	@Nullable
	public String[] getFilterNames() {
		return this.filterNames;
	}

	/**
	 * 设置是否向 HibernateCallback 代码暴露原生 Hibernate Session。
	 * <p>默认值为 "false"：将返回 Session 代理，抑制
	 * {@code close} 调用，并自动应用查询缓存
	 * 设置和事务超时。
	 * @see HibernateCallback
	 * @see Session
	 * @see #setCacheQueries
	 * @see #setQueryCacheRegion
	 * @see #prepareQuery
	 * @see #prepareCriteria
	 */
	public void setExposeNativeSession(boolean exposeNativeSession) {
		this.exposeNativeSession = exposeNativeSession;
	}

	/**
	 * 返回是向 HibernateCallback 代码暴露原生 Hibernate Session，
	 * 还是暴露 Session 代理。
	 */
	public boolean isExposeNativeSession() {
		return this.exposeNativeSession;
	}

	/**
	 * 设置在进行写操作（save/update/delete）时是否检查 Hibernate Session
	 * 不处于只读模式。
	 * <p>默认值为 "true"，以便在只读事务中尝试写操作时快速失败。
	 * 关闭此选项可允许在刷新模式为 MANUAL 的 Session 上
	 * 执行 save/update/delete。
	 * @see #checkWriteOperationAllowed
	 * @see org.springframework.transaction.TransactionDefinition#isReadOnly
	 */
	public void setCheckWriteOperations(boolean checkWriteOperations) {
		this.checkWriteOperations = checkWriteOperations;
	}

	/**
	 * 返回在进行写操作（save/update/delete）时是否检查 Hibernate Session
	 * 不处于只读模式。
	 */
	public boolean isCheckWriteOperations() {
		return this.checkWriteOperations;
	}

	/**
	 * 设置是否缓存此模板执行的所有查询。
	 * <p>如果为 "true"，此模板创建的所有 Query 和 Criteria 对象
	 * 都将标记为可缓存（包括通过 find 方法执行的所有
	 * 查询）。
	 * <p>要指定此模板缓存的查询所使用的查询区域，
	 * 请设置 "queryCacheRegion" 属性。
	 * @see #setQueryCacheRegion
	 * @see Query#setCacheable
	 * @see Criteria#setCacheable
	 */
	public void setCacheQueries(boolean cacheQueries) {
		this.cacheQueries = cacheQueries;
	}

	/**
	 * 返回是否缓存此模板执行的所有查询。
	 */
	public boolean isCacheQueries() {
		return this.cacheQueries;
	}

	/**
	 * 设置此模板执行的查询的缓存区域名称。
	 * <p>如果指定，将应用于此模板创建的所有 Query 和 Criteria 对象
	 * （包括通过 find 方法执行的所有查询）。
	 * <p>除非通过 "cacheQueries" 属性将由此模板创建的查询
	 * 配置为缓存，否则缓存区域不会生效。
	 * @see #setCacheQueries
	 * @see Query#setCacheRegion
	 * @see Criteria#setCacheRegion
	 */
	public void setQueryCacheRegion(@Nullable String queryCacheRegion) {
		this.queryCacheRegion = queryCacheRegion;
	}

	/**
	 * 返回此模板执行的查询的缓存区域名称。
	 */
	@Nullable
	public String getQueryCacheRegion() {
		return this.queryCacheRegion;
	}

	/**
	 * 设置此 HibernateTemplate 的抓取大小。这对于处理
	 * 大型结果集很重要：将此值设置为高于默认值会以内存消耗为代价
	 * 提高处理速度；将其设置得更低可以
	 * 避免传输应用程序永远不会读取的行数据。
	 * <p>默认值为 0，表示使用 JDBC 驱动的默认值。
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	/**
	 * 返回为此 HibernateTemplate 指定的抓取大小。
	 */
	public int getFetchSize() {
		return this.fetchSize;
	}

	/**
	 * 设置此 HibernateTemplate 的最大行数。这对于
	 * 处理大型结果集的子集很重要，可以避免在数据库或 JDBC 驱动中
	 * 读取并持有整个结果集，特别是在我们
	 * 从一开始就不关心完整结果的情况下（例如，
	 * 执行可能返回大量匹配项的搜索时）。
	 * <p>默认值为 0，表示使用 JDBC 驱动的默认值。
	 */
	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	/**
	 * 返回为此 HibernateTemplate 指定的最大行数。
	 */
	public int getMaxResults() {
		return this.maxResults;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}


	@Override
	@Nullable
	public <T> T execute(HibernateCallback<T> action) throws DataAccessException {
		return doExecute(action, false);
	}

	/**
	 * 在原生 {@link Session} 中执行给定操作对象所指定的动作。
	 * <p>此 execute 变体会覆盖模板范围的
	 * {@link #isExposeNativeSession() "exposeNativeSession"} 设置。
	 * @param action callback object that specifies the Hibernate action
	 * @return a result object returned by the action, or {@code null}
	 * @throws DataAccessException in case of Hibernate errors
	 */
	@Nullable
	public <T> T executeWithNativeSession(HibernateCallback<T> action) {
		return doExecute(action, true);
	}

	/**
	 * 在 Session 中执行给定操作对象所指定的动作。
	 * @param action callback object that specifies the Hibernate action
	 * @param enforceNativeSession whether to enforce exposure of the native
	 * Hibernate Session to callback code
	 * @return a result object returned by the action, or {@code null}
	 * @throws DataAccessException in case of Hibernate errors
	 */
	@Nullable
	protected <T> T doExecute(HibernateCallback<T> action, boolean enforceNativeSession) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");

		Session session = null;
		boolean isNew = false;
		try {
			session = obtainSessionFactory().getCurrentSession();
		}
		catch (HibernateException ex) {
			logger.debug("Could not retrieve pre-bound Hibernate session", ex);
		}
		if (session == null) {
			session = obtainSessionFactory().openSession();
			session.setHibernateFlushMode(FlushMode.MANUAL);
			isNew = true;
		}

		try {
			enableFilters(session);
			Session sessionToExpose =
					(enforceNativeSession || isExposeNativeSession() ? session : createSessionProxy(session));
			return action.doInHibernate(sessionToExpose);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
		catch (PersistenceException ex) {
			if (ex.getCause() instanceof HibernateException) {
				throw SessionFactoryUtils.convertHibernateAccessException((HibernateException) ex.getCause());
			}
			throw ex;
		}
		catch (RuntimeException ex) {
			// Callback code threw application exception...
			throw ex;
		}
		finally {
			if (isNew) {
				SessionFactoryUtils.closeSession(session);
			}
			else {
				disableFilters(session);
			}
		}
	}

	/**
	 * 为给定的 Hibernate Session 创建一个抑制 close 的代理。
	 * 该代理还会准备返回的 Query 和 Criteria 对象。
	 * @param session the Hibernate Session to create a proxy for
	 * @return the Session proxy
	 * @see Session#close()
	 * @see #prepareQuery
	 * @see #prepareCriteria
	 */
	protected Session createSessionProxy(Session session) {
		return (Session) Proxy.newProxyInstance(
				session.getClass().getClassLoader(), new Class<?>[] {Session.class},
				new CloseSuppressingInvocationHandler(session));
	}

	/**
	 * 在给定 Session 上启用指定的过滤器。
	 * @param session the current Hibernate Session
	 * @see #setFilterNames
	 * @see Session#enableFilter(String)
	 */
	protected void enableFilters(Session session) {
		String[] filterNames = getFilterNames();
		if (filterNames != null) {
			for (String filterName : filterNames) {
				session.enableFilter(filterName);
			}
		}
	}

	/**
	 * 在给定 Session 上禁用指定的过滤器。
	 * @param session the current Hibernate Session
	 * @see #setFilterNames
	 * @see Session#disableFilter(String)
	 */
	protected void disableFilters(Session session) {
		String[] filterNames = getFilterNames();
		if (filterNames != null) {
			for (String filterName : filterNames) {
				session.disableFilter(filterName);
			}
		}
	}


	//-------------------------------------------------------------------------
	// Convenience methods for loading individual objects
	//-------------------------------------------------------------------------

	@Override
	@Nullable
	public <T> T get(Class<T> entityClass, Serializable id) throws DataAccessException {
		return get(entityClass, id, null);
	}

	@Override
	@Nullable
	public <T> T get(Class<T> entityClass, Serializable id, @Nullable LockMode lockMode) throws DataAccessException {
		return executeWithNativeSession(session -> {
			if (lockMode != null) {
				return session.get(entityClass, id, new LockOptions(lockMode));
			}
			else {
				return session.get(entityClass, id);
			}
		});
	}

	@Override
	@Nullable
	public Object get(String entityName, Serializable id) throws DataAccessException {
		return get(entityName, id, null);
	}

	@Override
	@Nullable
	public Object get(String entityName, Serializable id, @Nullable LockMode lockMode) throws DataAccessException {
		return executeWithNativeSession(session -> {
			if (lockMode != null) {
				return session.get(entityName, id, new LockOptions(lockMode));
			}
			else {
				return session.get(entityName, id);
			}
		});
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable id) throws DataAccessException {
		return load(entityClass, id, null);
	}

	@Override
	public <T> T load(Class<T> entityClass, Serializable id, @Nullable LockMode lockMode)
			throws DataAccessException {

		return nonNull(executeWithNativeSession(session -> {
			if (lockMode != null) {
				return session.load(entityClass, id, new LockOptions(lockMode));
			}
			else {
				return session.load(entityClass, id);
			}
		}));
	}

	@Override
	public Object load(String entityName, Serializable id) throws DataAccessException {
		return load(entityName, id, null);
	}

	@Override
	public Object load(String entityName, Serializable id, @Nullable LockMode lockMode) throws DataAccessException {
		return nonNull(executeWithNativeSession(session -> {
			if (lockMode != null) {
				return session.load(entityName, id, new LockOptions(lockMode));
			}
			else {
				return session.load(entityName, id);
			}
		}));
	}

	@Override
	@SuppressWarnings({"unchecked", "deprecation"})
	public <T> List<T> loadAll(Class<T> entityClass) throws DataAccessException {
		return nonNull(executeWithNativeSession((HibernateCallback<List<T>>) session -> {
			Criteria criteria = session.createCriteria(entityClass);
			criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
			prepareCriteria(criteria);
			return criteria.list();
		}));
	}

	@Override
	public void load(Object entity, Serializable id) throws DataAccessException {
		executeWithNativeSession(session -> {
			session.load(entity, id);
			return null;
		});
	}

	@Override
	public void refresh(Object entity) throws DataAccessException {
		refresh(entity, null);
	}

	@Override
	public void refresh(Object entity, @Nullable LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(session -> {
			if (lockMode != null) {
				session.refresh(entity, new LockOptions(lockMode));
			}
			else {
				session.refresh(entity);
			}
			return null;
		});
	}

	@Override
	public boolean contains(Object entity) throws DataAccessException {
		Boolean result = executeWithNativeSession(session -> session.contains(entity));
		Assert.state(result != null, "No contains result");
		return result;
	}

	@Override
	public void evict(Object entity) throws DataAccessException {
		executeWithNativeSession(session -> {
			session.evict(entity);
			return null;
		});
	}

	@Override
	public void initialize(Object proxy) throws DataAccessException {
		try {
			Hibernate.initialize(proxy);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}

	@Override
	public Filter enableFilter(String filterName) throws IllegalStateException {
		Session session = obtainSessionFactory().getCurrentSession();
		Filter filter = session.getEnabledFilter(filterName);
		if (filter == null) {
			filter = session.enableFilter(filterName);
		}
		return filter;
	}


	//-------------------------------------------------------------------------
	// Convenience methods for storing individual objects
	//-------------------------------------------------------------------------

	@Override
	public void lock(Object entity, LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(session -> {
			session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
			return null;
		});
	}

	@Override
	public void lock(String entityName, Object entity, LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(session -> {
			session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
			return null;
		});
	}

	@Override
	public Serializable save(Object entity) throws DataAccessException {
		return nonNull(executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			return session.save(entity);
		}));
	}

	@Override
	public Serializable save(String entityName, Object entity) throws DataAccessException {
		return nonNull(executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			return session.save(entityName, entity);
		}));
	}

	@Override
	public void update(Object entity) throws DataAccessException {
		update(entity, null);
	}

	@Override
	public void update(Object entity, @Nullable LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.update(entity);
			if (lockMode != null) {
				session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
			}
			return null;
		});
	}

	@Override
	public void update(String entityName, Object entity) throws DataAccessException {
		update(entityName, entity, null);
	}

	@Override
	public void update(String entityName, Object entity, @Nullable LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.update(entityName, entity);
			if (lockMode != null) {
				session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
			}
			return null;
		});
	}

	@Override
	public void saveOrUpdate(Object entity) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.saveOrUpdate(entity);
			return null;
		});
	}

	@Override
	public void saveOrUpdate(String entityName, Object entity) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.saveOrUpdate(entityName, entity);
			return null;
		});
	}

	@Override
	public void replicate(Object entity, ReplicationMode replicationMode) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.replicate(entity, replicationMode);
			return null;
		});
	}

	@Override
	public void replicate(String entityName, Object entity, ReplicationMode replicationMode)
			throws DataAccessException {

		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.replicate(entityName, entity, replicationMode);
			return null;
		});
	}

	@Override
	public void persist(Object entity) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.persist(entity);
			return null;
		});
	}

	@Override
	public void persist(String entityName, Object entity) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			session.persist(entityName, entity);
			return null;
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T merge(T entity) throws DataAccessException {
		return nonNull(executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			return (T) session.merge(entity);
		}));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T merge(String entityName, T entity) throws DataAccessException {
		return nonNull(executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			return (T) session.merge(entityName, entity);
		}));
	}

	@Override
	public void delete(Object entity) throws DataAccessException {
		delete(entity, null);
	}

	@Override
	public void delete(Object entity, @Nullable LockMode lockMode) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			if (lockMode != null) {
				session.buildLockRequest(new LockOptions(lockMode)).lock(entity);
			}
			session.delete(entity);
			return null;
		});
	}

	@Override
	public void delete(String entityName, Object entity) throws DataAccessException {
		delete(entityName, entity, null);
	}

	@Override
	public void delete(String entityName, Object entity, @Nullable LockMode lockMode)
			throws DataAccessException {

		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			if (lockMode != null) {
				session.buildLockRequest(new LockOptions(lockMode)).lock(entityName, entity);
			}
			session.delete(entityName, entity);
			return null;
		});
	}

	@Override
	public void deleteAll(Collection<?> entities) throws DataAccessException {
		executeWithNativeSession(session -> {
			checkWriteOperationAllowed(session);
			for (Object entity : entities) {
				session.delete(entity);
			}
			return null;
		});
	}

	@Override
	public void flush() throws DataAccessException {
		executeWithNativeSession(session -> {
			session.flush();
			return null;
		});
	}

	@Override
	public void clear() throws DataAccessException {
		executeWithNativeSession(session -> {
			session.clear();
			return null;
		});
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for detached criteria
	//-------------------------------------------------------------------------

	@Override
	public List<?> findByCriteria(DetachedCriteria criteria) throws DataAccessException {
		return findByCriteria(criteria, -1, -1);
	}

	@Override
	public List<?> findByCriteria(DetachedCriteria criteria, int firstResult, int maxResults)
			throws DataAccessException {

		Assert.notNull(criteria, "DetachedCriteria must not be null");
		return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
			Criteria executableCriteria = criteria.getExecutableCriteria(session);
			prepareCriteria(executableCriteria);
			if (firstResult >= 0) {
				executableCriteria.setFirstResult(firstResult);
			}
			if (maxResults > 0) {
				executableCriteria.setMaxResults(maxResults);
			}
			return executableCriteria.list();
		}));
	}

	@Override
	public <T> List<T> findByExample(T exampleEntity) throws DataAccessException {
		return findByExample(null, exampleEntity, -1, -1);
	}

	@Override
	public <T> List<T> findByExample(String entityName, T exampleEntity) throws DataAccessException {
		return findByExample(entityName, exampleEntity, -1, -1);
	}

	@Override
	public <T> List<T> findByExample(T exampleEntity, int firstResult, int maxResults) throws DataAccessException {
		return findByExample(null, exampleEntity, firstResult, maxResults);
	}

	@Override
	@SuppressWarnings({"unchecked", "deprecation"})
	public <T> List<T> findByExample(@Nullable String entityName, T exampleEntity, int firstResult, int maxResults)
			throws DataAccessException {

		Assert.notNull(exampleEntity, "Example entity must not be null");
		return nonNull(executeWithNativeSession((HibernateCallback<List<T>>) session -> {
			Criteria executableCriteria = (entityName != null ?
					session.createCriteria(entityName) : session.createCriteria(exampleEntity.getClass()));
			executableCriteria.add(Example.create(exampleEntity));
			prepareCriteria(executableCriteria);
			if (firstResult >= 0) {
				executableCriteria.setFirstResult(firstResult);
			}
			if (maxResults > 0) {
				executableCriteria.setMaxResults(maxResults);
			}
			return executableCriteria.list();
		}));
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for HQL strings
	//-------------------------------------------------------------------------

	@Deprecated
	@Override
	public List<?> find(String queryString, @Nullable Object... values) throws DataAccessException {
		return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
			Query<?> queryObject = session.createQuery(queryString);
			prepareQuery(queryObject);
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					queryObject.setParameter(i, values[i]);
				}
			}
			return queryObject.list();
		}));
	}

	@Deprecated
	@Override
	public List<?> findByNamedParam(String queryString, String paramName, Object value)
			throws DataAccessException {

		return findByNamedParam(queryString, new String[] {paramName}, new Object[] {value});
	}

	@Deprecated
	@Override
	public List<?> findByNamedParam(String queryString, String[] paramNames, Object[] values)
			throws DataAccessException {

		if (paramNames.length != values.length) {
			throw new IllegalArgumentException("Length of paramNames array must match length of values array");
		}
		return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
			Query<?> queryObject = session.createQuery(queryString);
			prepareQuery(queryObject);
			for (int i = 0; i < values.length; i++) {
				applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
			}
			return queryObject.list();
		}));
	}

	@Deprecated
	@Override
	public List<?> findByValueBean(String queryString, Object valueBean) throws DataAccessException {
		return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
			Query<?> queryObject = session.createQuery(queryString);
			prepareQuery(queryObject);
			queryObject.setProperties(valueBean);
			return queryObject.list();
		}));
	}


	//-------------------------------------------------------------------------
	// Convenience finder methods for named queries
	//-------------------------------------------------------------------------

	@Deprecated
	@Override
	public List<?> findByNamedQuery(String queryName, @Nullable Object... values) throws DataAccessException {
		return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
			Query<?> queryObject = session.getNamedQuery(queryName);
			prepareQuery(queryObject);
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					queryObject.setParameter(i, values[i]);
				}
			}
			return queryObject.list();
		}));
	}

	@Deprecated
	@Override
	public List<?> findByNamedQueryAndNamedParam(String queryName, String paramName, Object value)
			throws DataAccessException {

		return findByNamedQueryAndNamedParam(queryName, new String[] {paramName}, new Object[] {value});
	}

	@Deprecated
	@Override
	public List<?> findByNamedQueryAndNamedParam(
			String queryName, @Nullable String[] paramNames, @Nullable Object[] values)
			throws DataAccessException {

		if (values != null && (paramNames == null || paramNames.length != values.length)) {
			throw new IllegalArgumentException("Length of paramNames array must match length of values array");
		}
		return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
			Query<?> queryObject = session.getNamedQuery(queryName);
			prepareQuery(queryObject);
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					applyNamedParameterToQuery(queryObject, paramNames[i], values[i]);
				}
			}
			return queryObject.list();
		}));
	}

	@Deprecated
	@Override
	public List<?> findByNamedQueryAndValueBean(String queryName, Object valueBean) throws DataAccessException {
		return nonNull(executeWithNativeSession((HibernateCallback<List<?>>) session -> {
			Query<?> queryObject = session.getNamedQuery(queryName);
			prepareQuery(queryObject);
			queryObject.setProperties(valueBean);
			return queryObject.list();
		}));
	}


	//-------------------------------------------------------------------------
	// Convenience query methods for iteration and bulk updates/deletes
	//-------------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	@Deprecated
	@Override
	public Iterator<?> iterate(String queryString, @Nullable Object... values) throws DataAccessException {
		return nonNull(executeWithNativeSession((HibernateCallback<Iterator<?>>) session -> {
			Query<?> queryObject = session.createQuery(queryString);
			prepareQuery(queryObject);
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					queryObject.setParameter(i, values[i]);
				}
			}
			return queryObject.iterate();
		}));
	}

	@Deprecated
	@Override
	public void closeIterator(Iterator<?> it) throws DataAccessException {
		try {
			Hibernate.close(it);
		}
		catch (HibernateException ex) {
			throw SessionFactoryUtils.convertHibernateAccessException(ex);
		}
	}

	@Deprecated
	@Override
	public int bulkUpdate(String queryString, @Nullable Object... values) throws DataAccessException {
		Integer result = executeWithNativeSession(session -> {
			Query<?> queryObject = session.createQuery(queryString);
			prepareQuery(queryObject);
			if (values != null) {
				for (int i = 0; i < values.length; i++) {
					queryObject.setParameter(i, values[i]);
				}
			}
			return queryObject.executeUpdate();
		});
		Assert.state(result != null, "No update count");
		return result;
	}


	//-------------------------------------------------------------------------
	// Helper methods used by the operations above
	//-------------------------------------------------------------------------

	/**
	 * 检查是否允许在给定 Session 上执行写操作。
	 * <p>默认实现在 {@code FlushMode.MANUAL} 的情况下抛出
	 * InvalidDataAccessApiUsageException。可在子类中重写。
	 * @param session current Hibernate Session
	 * @throws InvalidDataAccessApiUsageException if write operations are not allowed
	 * @see #setCheckWriteOperations
	 * @see Session#getFlushMode()
	 * @see FlushMode#MANUAL
	 */
	protected void checkWriteOperationAllowed(Session session) throws InvalidDataAccessApiUsageException {
		if (isCheckWriteOperations() && session.getHibernateFlushMode().lessThan(FlushMode.COMMIT)) {
			throw new InvalidDataAccessApiUsageException(
					"Write operations are not allowed in read-only mode (FlushMode.MANUAL): "+
					"Turn your Session into FlushMode.COMMIT/AUTO or remove 'readOnly' marker from transaction definition.");
		}
	}

	/**
	 * 准备给定的 Criteria 对象，应用缓存设置和/或
	 * 事务超时。
	 * @param criteria the Criteria object to prepare
	 * @see #setCacheQueries
	 * @see #setQueryCacheRegion
	 */
	protected void prepareCriteria(Criteria criteria) {
		if (isCacheQueries()) {
			criteria.setCacheable(true);
			if (getQueryCacheRegion() != null) {
				criteria.setCacheRegion(getQueryCacheRegion());
			}
		}
		if (getFetchSize() > 0) {
			criteria.setFetchSize(getFetchSize());
		}
		if (getMaxResults() > 0) {
			criteria.setMaxResults(getMaxResults());
		}

		ResourceHolderSupport sessionHolder =
				(ResourceHolderSupport) TransactionSynchronizationManager.getResource(obtainSessionFactory());
		if (sessionHolder != null && sessionHolder.hasTimeout()) {
			criteria.setTimeout(sessionHolder.getTimeToLiveInSeconds());
		}
	}

	/**
	 * 准备给定的 Query 对象，应用缓存设置和/或
	 * 事务超时。
	 * @param queryObject the Query object to prepare
	 * @see #setCacheQueries
	 * @see #setQueryCacheRegion
	 */
	protected void prepareQuery(Query<?> queryObject) {
		if (isCacheQueries()) {
			queryObject.setCacheable(true);
			if (getQueryCacheRegion() != null) {
				queryObject.setCacheRegion(getQueryCacheRegion());
			}
		}
		if (getFetchSize() > 0) {
			queryObject.setFetchSize(getFetchSize());
		}
		if (getMaxResults() > 0) {
			queryObject.setMaxResults(getMaxResults());
		}

		ResourceHolderSupport sessionHolder =
				(ResourceHolderSupport) TransactionSynchronizationManager.getResource(obtainSessionFactory());
		if (sessionHolder != null && sessionHolder.hasTimeout()) {
			queryObject.setTimeout(sessionHolder.getTimeToLiveInSeconds());
		}
	}

	/**
	 * 将给定名称的参数应用到给定的 Query 对象。
	 * @param queryObject the Query object
	 * @param paramName the name of the parameter
	 * @param value the value of the parameter
	 * @throws HibernateException if thrown by the Query object
	 */
	protected void applyNamedParameterToQuery(Query<?> queryObject, String paramName, Object value)
			throws HibernateException {

		if (value instanceof Collection) {
			queryObject.setParameterList(paramName, (Collection<?>) value);
		}
		else if (value instanceof Object[]) {
			queryObject.setParameterList(paramName, (Object[]) value);
		}
		else {
			queryObject.setParameter(paramName, value);
		}
	}

	private static <T> T nonNull(@Nullable T result) {
		Assert.state(result != null, "No result");
		return result;
	}


	/**
	 * 抑制对 Hibernate Session 的 close 调用的调用处理器。
	 * 还会准备返回的 Query 和 Criteria 对象。
	 * @see Session#close
	 */
	private class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Session target;

		public CloseSuppressingInvocationHandler(Session target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Session interface coming in...

			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Session proxy.
					return System.identityHashCode(proxy);
				case "close":
					// Handle close method: suppress, not valid.
					return null;
			}

			// Invoke method on target Session.
			try {
				Object retVal = method.invoke(this.target, args);

				// If return value is a Query or Criteria, apply transaction timeout.
				// Applies to createQuery, getNamedQuery, createCriteria.
				if (retVal instanceof Criteria) {
					prepareCriteria(((Criteria) retVal));
				}
				else if (retVal instanceof Query) {
					prepareQuery(((Query<?>) retVal));
				}

				return retVal;
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
