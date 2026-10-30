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

package org.springframework.orm.hibernate5.support;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.support.DaoSupport;
import org.springframework.lang.Nullable;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.util.Assert;

/**
 * 基于 Hibernate 的数据访问对象的便捷基类。
 *
 * <p>要求设置一个 {@link SessionFactory}，并基于它为子类提供一个
 * {@link org.springframework.orm.hibernate5.HibernateTemplate}，
 * 可通过 {@link #getHibernateTemplate()} 方法获取。
 * 也可以直接使用一个 HibernateTemplate 进行初始化，
 * 以便复用其设置（例如 SessionFactory、异常转换器、flush 模式等）。
 *
 * <p>如果传入 SessionFactory，此类将创建自己的 HibernateTemplate 实例。
 * 该 HibernateTemplate 上的 "allowCreate" 标志默认为 "true"。
 * 可以通过重写 {@link #createHibernateTemplate} 来使用自定义的 HibernateTemplate 实例。
 *
 * <p><b>注意：Hibernate 访问代码也可以采用纯 Hibernate 风格编写。
 * 因此，对于新启动的项目，建议改为采用基于
 * {@link SessionFactory#getCurrentSession()} 的标准 Hibernate 风格
 * 编写数据访问对象。此 HibernateTemplate 主要是作为基于 Hibernate 3
 * 的数据访问代码的迁移辅助工具而存在，以便受益于 Hibernate 5.x 中的错误修复。</b>
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setSessionFactory
 * @see #getHibernateTemplate
 * @see org.springframework.orm.hibernate5.HibernateTemplate
 */
public abstract class HibernateDaoSupport extends DaoSupport {

	@Nullable
	private HibernateTemplate hibernateTemplate;


	/**
	 * 设置此 DAO 要使用的 Hibernate SessionFactory。
	 * 将自动为给定的 SessionFactory 创建一个 HibernateTemplate。
	 * @see #createHibernateTemplate
	 * @see #setHibernateTemplate
	 */
	public final void setSessionFactory(SessionFactory sessionFactory) {
		if (this.hibernateTemplate == null || sessionFactory != this.hibernateTemplate.getSessionFactory()) {
			this.hibernateTemplate = createHibernateTemplate(sessionFactory);
		}
	}

	/**
	 * 为给定的 SessionFactory 创建一个 HibernateTemplate。
	 * 仅在向 DAO 填充 SessionFactory 引用时调用！
	 * <p>可以在子类中重写，以提供具有不同配置的 HibernateTemplate 实例，
	 * 或提供自定义的 HibernateTemplate 子类。
	 * @param sessionFactory 要为其创建 HibernateTemplate 的 Hibernate SessionFactory
	 * @return 新的 HibernateTemplate 实例
	 * @see #setSessionFactory
	 */
	protected HibernateTemplate createHibernateTemplate(SessionFactory sessionFactory) {
		return new HibernateTemplate(sessionFactory);
	}

	/**
	 * 返回此 DAO 使用的 Hibernate SessionFactory。
	 */
	@Nullable
	public final SessionFactory getSessionFactory() {
		return (this.hibernateTemplate != null ? this.hibernateTemplate.getSessionFactory() : null);
	}

	/**
	 * 为此 DAO 显式设置 HibernateTemplate，
	 * 作为指定 SessionFactory 的替代方式。
	 * @see #setSessionFactory
	 */
	public final void setHibernateTemplate(@Nullable HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * 返回此 DAO 的 HibernateTemplate，
	 * 该实例已使用 SessionFactory 预先初始化或已被显式设置。
	 * <p><b>注意：返回的 HibernateTemplate 是一个共享实例。</b>
	 * 可以查看其配置，但不能修改配置
	 * （除非在 {@link #initDao} 实现中）。
	 * 考虑通过 {@code new HibernateTemplate(getSessionFactory())} 创建自定义的
	 * HibernateTemplate 实例，在这种情况下，
	 * 你可以自定义所生成实例上的设置。
	 */
	@Nullable
	public final HibernateTemplate getHibernateTemplate() {
		return this.hibernateTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.hibernateTemplate == null) {
			throw new IllegalArgumentException("'sessionFactory' or 'hibernateTemplate' is required");
		}
	}


	/**
	 * 便捷地获取当前的 Hibernate Session。
	 * @return 该 Hibernate Session
	 * @throws DataAccessResourceFailureException 如果无法创建 Session
	 * @see SessionFactory#getCurrentSession()
	 */
	protected final Session currentSession() throws DataAccessResourceFailureException {
		SessionFactory sessionFactory = getSessionFactory();
		Assert.state(sessionFactory != null, "No SessionFactory set");
		return sessionFactory.getCurrentSession();
	}

}
