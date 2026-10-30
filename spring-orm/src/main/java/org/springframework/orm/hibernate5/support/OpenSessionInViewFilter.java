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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet 过滤器，在整个请求处理过程中将 Hibernate Session 绑定到当前线程。
 * 旨在支持 "Open Session in View" 模式，即允许在原始事务已经完成的情况下，
 * 在 Web 视图中进行延迟加载。
 *
 * <p>此过滤器使 Hibernate Session 可通过当前线程获取，
 * 事务管理器会自动检测到它。它既适用于通过
 * {@link org.springframework.orm.hibernate5.HibernateTransactionManager}
 * 管理服务层事务的场景，也适用于非事务性执行（如果配置得当）。
 *
 * <p><b>注意</b>：此过滤器默认<i>不会</i>刷新 Hibernate Session，
 * 其 flush 模式被设置为 {@code FlushMode.MANUAL}。它假定与负责刷新的
 * 服务层事务配合使用：在读写事务期间，活动的事务管理器会临时将 flush 模式
 * 改为 {@code FlushMode.AUTO}，并在每个事务结束时将 flush 模式重置为
 * {@code FlushMode.MANUAL}。
 *
 * <p><b>警告：</b>将此过滤器应用于现有逻辑可能会因为对整个请求的处理
 * 使用单个 Hibernate Session 而引发以前未出现过的问题。特别是，必须在请求处理
 * 的最开始处将持久化对象重新关联到 Hibernate Session，
 * 以避免与已加载的同一对象实例发生冲突。
 *
 * <p>在 Spring 的根 Web 应用上下文中查找 SessionFactory。
 * 支持在 {@code web.xml} 中配置 "sessionFactoryBeanName" 过滤器初始化参数；
 * 默认 bean 名称为 "sessionFactory"。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #lookupSessionFactory
 * @see OpenSessionInViewInterceptor
 * @see OpenSessionInterceptor
 * @see org.springframework.orm.hibernate5.HibernateTransactionManager
 * @see TransactionSynchronizationManager
 * @see SessionFactory#getCurrentSession()
 */
public class OpenSessionInViewFilter extends OncePerRequestFilter {

	/**
	 * 用于 SessionFactory 的默认 bean 名称。
	 */
	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";

	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;


	/**
	 * 设置要从 Spring 根应用上下文中获取的 SessionFactory 的 bean 名称。
	 * 默认为 "sessionFactory"。
	 * @see #DEFAULT_SESSION_FACTORY_BEAN_NAME
	 */
	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	/**
	 * 返回要从 Spring 根应用上下文中获取的 SessionFactory 的 bean 名称。
	 */
	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}


	/**
	 * 返回 "false"，以便该过滤器可以将打开的 Hibernate {@code Session}
	 * 重新绑定到每个异步派发的线程，并将关闭操作推迟到
	 * 最后一次异步派发时进行。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回 "false"，以便该过滤器可以为每次错误派发提供一个
	 * Hibernate {@code Session}。
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		SessionFactory sessionFactory = lookupSessionFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			// Do not modify the Session: just set the participate flag.
			participate = true;
		}
		else {
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applySessionBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening Hibernate Session in OpenSessionInViewFilter");
				Session session = openSession(sessionFactory);
				SessionHolder sessionHolder = new SessionHolder(session);
				TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);

				AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(sessionFactory, sessionHolder);
				asyncManager.registerCallableInterceptor(key, interceptor);
				asyncManager.registerDeferredResultInterceptor(key, interceptor);
			}
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				SessionHolder sessionHolder =
						(SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
				if (!isAsyncStarted(request)) {
					logger.debug("Closing Hibernate Session in OpenSessionInViewFilter");
					SessionFactoryUtils.closeSession(sessionHolder.getSession());
				}
			}
		}
	}

	/**
	 * 查找此过滤器应使用的 SessionFactory，
	 * 并将当前 HTTP 请求作为参数。
	 * <p>默认实现委托给不带参数的 {@link #lookupSessionFactory()} 变体。
	 * @param request 当前请求
	 * @return 要使用的 SessionFactory
	 */
	protected SessionFactory lookupSessionFactory(HttpServletRequest request) {
		return lookupSessionFactory();
	}

	/**
	 * 查找此过滤器应使用的 SessionFactory。
	 * <p>默认实现在 Spring 的根应用上下文中
	 * 查找具有指定名称的 bean。
	 * @return 要使用的 SessionFactory
	 * @see #getSessionFactoryBeanName
	 */
	protected SessionFactory lookupSessionFactory() {
		if (logger.isDebugEnabled()) {
			logger.debug("Using SessionFactory '" + getSessionFactoryBeanName() + "' for OpenSessionInViewFilter");
		}
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		return wac.getBean(getSessionFactoryBeanName(), SessionFactory.class);
	}

	/**
	 * 为此过滤器使用的 SessionFactory 打开一个 Session。
	 * <p>默认实现委托给 {@link SessionFactory#openSession} 方法，
	 * 并将 {@link Session} 的 flush 模式设置为 "MANUAL"。
	 * @param sessionFactory 此过滤器使用的 SessionFactory
	 * @return 要使用的 Session
	 * @throws DataAccessResourceFailureException 如果无法创建 Session
	 * @see FlushMode#MANUAL
	 */
	protected Session openSession(SessionFactory sessionFactory) throws DataAccessResourceFailureException {
		try {
			Session session = sessionFactory.openSession();
			session.setHibernateFlushMode(FlushMode.MANUAL);
			return session;
		}
		catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
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
