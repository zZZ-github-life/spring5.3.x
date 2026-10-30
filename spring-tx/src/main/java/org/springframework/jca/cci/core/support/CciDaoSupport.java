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

package org.springframework.jca.cci.core.support;

import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;

import org.springframework.dao.support.DaoSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 基于 CCI 的数据访问对象的便捷超类。
 *
 * <p>需要设置 {@link javax.resource.cci.ConnectionFactory}，
 * 通过 {@link #getCciTemplate()} 方法向子类提供基于它的
 * {@link org.springframework.jca.cci.core.CciTemplate}。
 *
 * <p>此基类主要用于 CciTemplate 使用，但也可以在使用
 * Connection 直接操作或使用 {@code org.springframework.jca.cci.object} 类时使用。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setConnectionFactory
 * @see #getCciTemplate
 * @see org.springframework.jca.cci.core.CciTemplate
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public abstract class CciDaoSupport extends DaoSupport {

	@Nullable
	private org.springframework.jca.cci.core.CciTemplate cciTemplate;


	/**
	 * 设置此 DAO 使用的 ConnectionFactory。
	 */
	public final void setConnectionFactory(ConnectionFactory connectionFactory) {
		if (this.cciTemplate == null || connectionFactory != this.cciTemplate.getConnectionFactory()) {
			this.cciTemplate = createCciTemplate(connectionFactory);
		}
	}

	/**
	 * 为给定的 ConnectionFactory 创建 CciTemplate。
	 * 仅在使用 ConnectionFactory 引用填充 DAO 时调用！
	 * <p>可以在子类中重写，以提供具有不同配置的 CciTemplate 实例，
	 * 或自定义的 CciTemplate 子类。
	 * @param connectionFactory 要为其创建 CciTemplate 的 CCI ConnectionFactory
	 * @return 新的 CciTemplate 实例
	 * @see #setConnectionFactory(javax.resource.cci.ConnectionFactory)
	 */
	protected org.springframework.jca.cci.core.CciTemplate createCciTemplate(ConnectionFactory connectionFactory) {
		return new org.springframework.jca.cci.core.CciTemplate(connectionFactory);
	}

	/**
	 * 返回此 DAO 使用的 ConnectionFactory。
	 */
	@Nullable
	public final ConnectionFactory getConnectionFactory() {
		return (this.cciTemplate != null ? this.cciTemplate.getConnectionFactory() : null);
	}

	/**
	 * 显式设置此 DAO 的 CciTemplate，作为指定 ConnectionFactory 的替代方案。
	 */
	public final void setCciTemplate(org.springframework.jca.cci.core.CciTemplate cciTemplate) {
		this.cciTemplate = cciTemplate;
	}

	/**
	 * 返回此 DAO 的 CciTemplate，已使用 ConnectionFactory 预初始化或显式设置。
	 */
	@Nullable
	public final org.springframework.jca.cci.core.CciTemplate getCciTemplate() {
		return this.cciTemplate;
	}

	@Override
	protected final void checkDaoConfig() {
		if (this.cciTemplate == null) {
			throw new IllegalArgumentException("'connectionFactory' or 'cciTemplate' is required");
		}
	}


	/**
	 * 获取从主模板实例派生的 CciTemplate，
	 * 继承 ConnectionFactory 和其他设置，但覆盖用于获取 Connection 的 ConnectionSpec。
	 * @param connectionSpec 返回的模板实例应为其获取 Connection 的
	 * CCI ConnectionSpec
	 * @return 派生的模板实例
	 * @see org.springframework.jca.cci.core.CciTemplate#getDerivedTemplate(javax.resource.cci.ConnectionSpec)
	 */
	protected final org.springframework.jca.cci.core.CciTemplate getCciTemplate(ConnectionSpec connectionSpec) {
		org.springframework.jca.cci.core.CciTemplate cciTemplate = getCciTemplate();
		Assert.state(cciTemplate != null, "No CciTemplate set");
		return cciTemplate.getDerivedTemplate(connectionSpec);
	}

	/**
	 * 获取 CCI Connection，来自当前事务或新建的。
	 * @return 该 CCI Connection
	 * @throws org.springframework.jca.cci.CannotGetCciConnectionException 如果获取 Connection 的尝试失败
	 * @see org.springframework.jca.cci.connection.ConnectionFactoryUtils#getConnection(javax.resource.cci.ConnectionFactory)
	 */
	protected final Connection getConnection() throws org.springframework.jca.cci.CannotGetCciConnectionException {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return org.springframework.jca.cci.connection.ConnectionFactoryUtils.getConnection(connectionFactory);
	}

	/**
	 * 关闭通过此 bean 的 ConnectionFactory 创建的给定 CCI Connection，
	 * 如果它未绑定到线程。
	 * @param con 要关闭的 Connection
	 * @see org.springframework.jca.cci.connection.ConnectionFactoryUtils#releaseConnection
	 */
	protected final void releaseConnection(Connection con) {
		org.springframework.jca.cci.connection.ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
	}

}
