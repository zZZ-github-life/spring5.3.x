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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * Spring Web 请求拦截器，在整个请求处理过程中将 Hibernate {@code Session}
 * 绑定到当前线程。
 *
 * <p>此类是 "Open Session in View" 模式的具体实现，该模式允许在 Web 视图中
 * 对关联对象进行延迟加载，即便原始事务已经完成。
 *
 * <p>此拦截器使 Hibernate Session 可通过当前线程获取，
 * 事务管理器会自动检测到它。它既适用于通过
 * {@link org.springframework.orm.hibernate5.HibernateTransactionManager}
 * 管理服务层事务的场景，也适用于非事务性执行（如果配置得当）。
 *
 * <p>与 {@link OpenSessionInViewFilter} 不同，此拦截器在 Spring 应用上下文中
 * 进行配置，因此可以利用 bean 装配的优势。
 *
 * <p><b>警告：</b>将此拦截器应用于现有逻辑可能会因为对整个请求的处理使用
 * 单个 Hibernate {@code Session} 而引发以前未出现过的问题。特别是，必须在
 * 请求处理的最开始处将持久化对象重新关联到 Hibernate {@code Session}，
 * 以避免与已加载的同一对象实例发生冲突。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see OpenSessionInViewFilter
 * @see OpenSessionInterceptor
 * @see org.springframework.orm.hibernate5.HibernateTransactionManager
 * @see TransactionSynchronizationManager
 * @see SessionFactory#getCurrentSession()
 */
public class OpenSessionInViewInterceptor implements AsyncWebRequestInterceptor {

	/**
	 * 附加到 {@code SessionFactory} 的 {@code toString()} 表示形式之后的后缀，
	 * 用于 "参与现有会话处理" 的请求属性。
	 * @see #getParticipateAttributeName
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";

	protected final Log logger = LogFactory.getLog(getClass());

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

	private SessionFactory obtainSessionFactory() {
		SessionFactory sf = getSessionFactory();
		Assert.state(sf != null, "No SessionFactory set");
		return sf;
	}


	/**
	 * 打开一个新的 Hibernate {@code Session}，并通过
	 * {@link TransactionSynchronizationManager} 将其绑定到线程。
	 */
	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		String key = getParticipateAttributeName();
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		if (asyncManager.hasConcurrentResult() && applySessionBindingInterceptor(asyncManager, key)) {
			return;
		}

		if (TransactionSynchronizationManager.hasResource(obtainSessionFactory())) {
			// Do not modify the Session: just mark the request accordingly.
			Integer count = (Integer) request.getAttribute(key, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		}
		else {
			logger.debug("Opening Hibernate Session in OpenSessionInViewInterceptor");
			Session session = openSession();
			SessionHolder sessionHolder = new SessionHolder(session);
			TransactionSynchronizationManager.bindResource(obtainSessionFactory(), sessionHolder);

			AsyncRequestInterceptor asyncRequestInterceptor =
					new AsyncRequestInterceptor(obtainSessionFactory(), sessionHolder);
			asyncManager.registerCallableInterceptor(key, asyncRequestInterceptor);
			asyncManager.registerDeferredResultInterceptor(key, asyncRequestInterceptor);
		}
	}

	@Override
	public void postHandle(WebRequest request, @Nullable ModelMap model) {
	}

	/**
	 * 从线程解绑 Hibernate {@code Session} 并将其关闭。
	 * @see TransactionSynchronizationManager
	 */
	@Override
	public void afterCompletion(WebRequest request, @Nullable Exception ex) throws DataAccessException {
		if (!decrementParticipateCount(request)) {
			SessionHolder sessionHolder =
					(SessionHolder) TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
			logger.debug("Closing Hibernate Session in OpenSessionInViewInterceptor");
			SessionFactoryUtils.closeSession(sessionHolder.getSession());
		}
	}

	private boolean decrementParticipateCount(WebRequest request) {
		String participateAttributeName = getParticipateAttributeName();
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		if (count == null) {
			return false;
		}
		// Do not modify the Session: just clear the marker.
		if (count > 1) {
			request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
		}
		else {
			request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		}
		return true;
	}

	@Override
	public void afterConcurrentHandlingStarted(WebRequest request) {
		if (!decrementParticipateCount(request)) {
			TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
		}
	}

	/**
	 * 为此拦截器使用的 SessionFactory 打开一个 Session。
	 * <p>默认实现委托给 {@link SessionFactory#openSession} 方法，
	 * 并将 {@link Session} 的 flush 模式设置为 "MANUAL"。
	 * @return 要使用的 Session
	 * @throws DataAccessResourceFailureException 如果无法创建 Session
	 * @see FlushMode#MANUAL
	 */
	protected Session openSession() throws DataAccessResourceFailureException {
		try {
			Session session = obtainSessionFactory().openSession();
			session.setHibernateFlushMode(FlushMode.MANUAL);
			return session;
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	/**
	 * 返回标识某个请求已被拦截的请求属性名称。
	 * <p>默认实现取 {@code SessionFactory} 实例的 {@code toString()} 表示形式，
	 * 并追加 {@link #PARTICIPATE_SUFFIX}。
	 */
	protected String getParticipateAttributeName() {
		return obtainSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		CallableProcessingInterceptor cpi = asyncManager.getCallableInterceptor(key);
		if (cpi == null) {
			return false;
		}
		((AsyncRequestInterceptor) cpi).bindSession();
		return true;
	}

}
