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

package org.springframework.orm.hibernate5.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 简单的 AOP Alliance {@link MethodInterceptor} 实现，用于在每次方法调用时
 * 绑定一个新的 Hibernate {@link Session}（如果之前没有绑定任何 Session）。
 *
 * <p>这是一个简单的 Hibernate Session 作用域拦截器，与
 * {@link OpenSessionInViewInterceptor} 类似，只是用于 AOP 配置而非 MVC 配置。
 * 它以 "MANUAL" flush 模式打开一个新的 {@link Session}，
 * 因为除非参与事务，否则该 Session 仅用于读取。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see OpenSessionInViewInterceptor
 * @see OpenSessionInViewFilter
 * @see org.springframework.orm.hibernate5.HibernateTransactionManager
 * @see TransactionSynchronizationManager
 * @see SessionFactory#getCurrentSession()
 */
public class OpenSessionInterceptor implements MethodInterceptor, InitializingBean {

	@Nullable
	private SessionFactory sessionFactory;


	/**
	 * 设置应用于创建 Hibernate Session 的 Hibernate SessionFactory。
	 */
	public void setSessionFactory(@Nullable SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 返回应用于创建 Hibernate Session 的 Hibernate SessionFactory。
	 */
	@Nullable
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("Property 'sessionFactory' is required");
		}
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		SessionFactory sf = getSessionFactory();
		Assert.state(sf != null, "No SessionFactory set");

		if (!TransactionSynchronizationManager.hasResource(sf)) {
			// New Session to be bound for the current method's scope...
			Session session = openSession(sf);
			try {
				TransactionSynchronizationManager.bindResource(sf, new SessionHolder(session));
				return invocation.proceed();
			}
			finally {
				SessionFactoryUtils.closeSession(session);
				TransactionSynchronizationManager.unbindResource(sf);
			}
		}
		else {
			// Pre-bound Session found -> simply proceed.
			return invocation.proceed();
		}
	}

	/**
	 * 为给定的 SessionFactory 打开一个 Session。
	 * <p>默认实现委托给 {@link SessionFactory#openSession} 方法，
	 * 并将 {@link Session} 的 flush 模式设置为 "MANUAL"。
	 * @param sessionFactory the SessionFactory to use
	 * @return the Session to use
	 * @throws DataAccessResourceFailureException if the Session could not be created
	 * @since 5.0
	 * @see FlushMode#MANUAL
	 */
	protected Session openSession(SessionFactory sessionFactory) throws DataAccessResourceFailureException {
		Session session = openSession();
		if (session == null) {
			try {
				session = sessionFactory.openSession();
				session.setHibernateFlushMode(FlushMode.MANUAL);
			}
			catch (HibernateException ex) {
				throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
			}
		}
		return session;
	}

	/**
	 * 为给定的 SessionFactory 打开一个 Session。
	 * @deprecated as of 5.0, in favor of {@link #openSession(SessionFactory)}
	 */
	@Deprecated
	@Nullable
	protected Session openSession() throws DataAccessResourceFailureException {
		return null;
	}

}
