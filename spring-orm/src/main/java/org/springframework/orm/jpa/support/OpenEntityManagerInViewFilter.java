/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.orm.jpa.support;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet 过滤器，在整个请求处理过程中将 JPA EntityManager 绑定到当前线程。
 * 适用于"Open EntityManager in View"模式，即允许在 Web 视图中进行延迟加载，
 * 即使原始事务已经完成。
 *
 * <p>此过滤器使 JPA EntityManager 可通过当前线程获取，
 * 事务管理器会自动检测到它。它既适用于通过
 * {@link org.springframework.orm.jpa.JpaTransactionManager}
 * 或 {@link org.springframework.transaction.jta.JtaTransactionManager}
 * 管理的服务层事务，也适用于非事务性的只读执行。
 *
 * <p>从 Spring 的根 Web 应用上下文中查找 EntityManagerFactory。
 * 支持在 {@code web.xml} 中配置 "entityManagerFactoryBeanName" 过滤器初始化参数；
 * 默认 bean 名称为 "entityManagerFactory"。另一种方式是使用
 * "persistenceUnitName" 初始化参数，按逻辑单元名称
 * （如 {@code persistence.xml} 中所指定）进行获取。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see OpenEntityManagerInViewInterceptor
 * @see org.springframework.orm.jpa.JpaTransactionManager
 * @see org.springframework.orm.jpa.SharedEntityManagerCreator
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class OpenEntityManagerInViewFilter extends OncePerRequestFilter {

	/**
	 * 默认的 EntityManagerFactory bean 名称："entityManagerFactory"。
	 * 仅在未指定 "persistenceUnitName" 参数时适用。
	 * @see #setEntityManagerFactoryBeanName
	 * @see #setPersistenceUnitName
	 */
	public static final String DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";


	@Nullable
	private String entityManagerFactoryBeanName;

	@Nullable
	private String persistenceUnitName;

	@Nullable
	private volatile EntityManagerFactory entityManagerFactory;


	/**
	 * 设置要从 Spring 根应用上下文中获取的 EntityManagerFactory 的 bean 名称。
	 * <p>默认值为 "entityManagerFactory"。请注意，此默认值仅在
	 * 未指定 "persistenceUnitName" 参数时适用。
	 * @see #setPersistenceUnitName
	 * @see #DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME
	 */
	public void setEntityManagerFactoryBeanName(@Nullable String entityManagerFactoryBeanName) {
		this.entityManagerFactoryBeanName = entityManagerFactoryBeanName;
	}

	/**
	 * 返回要从 Spring 根应用上下文中获取的 EntityManagerFactory 的 bean 名称。
	 */
	@Nullable
	protected String getEntityManagerFactoryBeanName() {
		return this.entityManagerFactoryBeanName;
	}

	/**
	 * 设置要访问其 EntityManagerFactory 的持久化单元名称。
	 * <p>这是按 bean 名称指定 EntityManagerFactory 的替代方案，
	 * 改为通过持久化单元名称来解析它。如果既未指定 bean 名称
	 * 也未指定持久化单元名称，我们将检查默认 bean 名称
	 * "entityManagerFactory" 对应的 bean 是否存在；如果不存在，
	 * 则通过查找类型为 EntityManagerFactory 的唯一 bean 来获取
	 * 默认的 EntityManagerFactory。
	 * @see #setEntityManagerFactoryBeanName
	 * @see #DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME
	 */
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * 返回要访问其 EntityManagerFactory 的持久化单元名称（如果有）。
	 */
	@Nullable
	protected String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}


	/**
	 * 返回 "false"，以便过滤器可以将打开的 {@code EntityManager} 重新绑定到每个
	 * 异步派发的线程，并将关闭操作推迟到最后一个异步派发。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回 "false"，以便过滤器可以为每个错误派发提供 {@code EntityManager}。
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		EntityManagerFactory emf = lookupEntityManagerFactory(request);
		boolean participate = false;

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		String key = getAlreadyFilteredAttributeName();

		if (TransactionSynchronizationManager.hasResource(emf)) {
			// Do not modify the EntityManager: just set the participate flag.
			participate = true;
		}
		else {
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applyEntityManagerBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening JPA EntityManager in OpenEntityManagerInViewFilter");
				try {
					EntityManager em = createEntityManager(emf);
					EntityManagerHolder emHolder = new EntityManagerHolder(em);
					TransactionSynchronizationManager.bindResource(emf, emHolder);

					AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(emf, emHolder);
					asyncManager.registerCallableInterceptor(key, interceptor);
					asyncManager.registerDeferredResultInterceptor(key, interceptor);
				}
				catch (PersistenceException ex) {
					throw new DataAccessResourceFailureException("Could not create JPA EntityManager", ex);
				}
			}
		}

		try {
			filterChain.doFilter(request, response);
		}

		finally {
			if (!participate) {
				EntityManagerHolder emHolder = (EntityManagerHolder)
						TransactionSynchronizationManager.unbindResource(emf);
				if (!isAsyncStarted(request)) {
					logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewFilter");
					EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
				}
			}
		}
	}

	/**
	 * 查找此过滤器应使用的 EntityManagerFactory，
	 * 并以当前 HTTP 请求作为参数。
	 * <p>默认实现委托给无参的 {@code lookupEntityManagerFactory}，
	 * 并在获取 EntityManagerFactory 引用后对其进行缓存。
	 * @return 要使用的 EntityManagerFactory
	 * @see #lookupEntityManagerFactory()
	 */
	protected EntityManagerFactory lookupEntityManagerFactory(HttpServletRequest request) {
		EntityManagerFactory emf = this.entityManagerFactory;
		if (emf == null) {
			emf = lookupEntityManagerFactory();
			this.entityManagerFactory = emf;
		}
		return emf;
	}

	/**
	 * 查找此过滤器应使用的 EntityManagerFactory。
	 * <p>默认实现在 Spring 的根应用上下文中查找具有指定名称的 bean。
	 * @return 要使用的 EntityManagerFactory
	 * @see #getEntityManagerFactoryBeanName
	 */
	protected EntityManagerFactory lookupEntityManagerFactory() {
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		String emfBeanName = getEntityManagerFactoryBeanName();
		String puName = getPersistenceUnitName();
		if (StringUtils.hasLength(emfBeanName)) {
			return wac.getBean(emfBeanName, EntityManagerFactory.class);
		}
		else if (!StringUtils.hasLength(puName) && wac.containsBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME)) {
			return wac.getBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME, EntityManagerFactory.class);
		}
		else {
			// Includes fallback search for single EntityManagerFactory bean by type.
			return EntityManagerFactoryUtils.findEntityManagerFactory(wac, puName);
		}
	}

	/**
	 * 创建一个要绑定到请求的 JPA EntityManager。
	 * <p>可在子类中重写。
	 * @param emf 要使用的 EntityManagerFactory
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 */
	protected EntityManager createEntityManager(EntityManagerFactory emf) {
		return emf.createEntityManager();
	}

	private boolean applyEntityManagerBindingInterceptor(WebAsyncManager asyncManager, String key) {
		CallableProcessingInterceptor cpi = asyncManager.getCallableInterceptor(key);
		if (cpi == null) {
			return false;
		}
		((AsyncRequestInterceptor) cpi).bindEntityManager();
		return true;
	}

}
