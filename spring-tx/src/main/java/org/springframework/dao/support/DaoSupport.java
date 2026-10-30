/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.dao.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

/**
 * DAO 的通用基类，定义 DAO 初始化的模板方法。
 *
 * <p>由 Spring 特定的 DAO 支持类扩展，例如：
 * JdbcDaoSupport、JdoDaoSupport 等。
 *
 * @author Juergen Hoeller
 * @since 1.2.2
 * @see org.springframework.jdbc.core.support.JdbcDaoSupport
 */
public abstract class DaoSupport implements InitializingBean {

	/** 子类可用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public final void afterPropertiesSet() throws IllegalArgumentException, BeanInitializationException {
		// Let abstract subclasses check their configuration.
		checkDaoConfig();

		// Let concrete implementations initialize themselves.
		try {
			initDao();
		}
		catch (Exception ex) {
			throw new BeanInitializationException("Initialization of DAO failed", ex);
		}
	}

	/**
	 * 抽象子类必须覆盖此方法以检查其配置。
	 * <p>如果不允许具体子类覆盖此模板方法，则实现者应标记为 {@code final}。
	 * @throws IllegalArgumentException 在配置非法的情况下
	 */
	protected abstract void checkDaoConfig() throws IllegalArgumentException;

	/**
	 * 具体子类可以覆盖此方法以自定义初始化行为。
	 * 在此实例的 Bean 属性填充完成后调用。
	 * @throws Exception 如果 DAO 初始化失败
	 * （将作为 BeanInitializationException 重新抛出）
	 * @see org.springframework.beans.factory.BeanInitializationException
	 */
	protected void initDao() throws Exception {
	}

}
