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

package org.springframework.jca.cci.connection;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * CCI {@link ConnectionFactory} 实现，将所有调用委托给给定的目标
 * {@link ConnectionFactory}。
 *
 * <p>此类旨在被子类化，子类只需重写那些不应简单地委托给目标
 * {@link ConnectionFactory} 的方法（如 {@link #getConnection()}）。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getConnection
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class DelegatingConnectionFactory implements ConnectionFactory, InitializingBean {

	@Nullable
	private ConnectionFactory targetConnectionFactory;


	/**
	 * 设置此 ConnectionFactory 应委托的目标 ConnectionFactory。
	 */
	public void setTargetConnectionFactory(@Nullable ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * 返回此 ConnectionFactory 应委托的目标 ConnectionFactory。
	 */
	@Nullable
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * 获取实际使用的目标 {@code ConnectionFactory}（永不返回 {@code null}）。
	 * @since 5.0
	 */
	protected ConnectionFactory obtainTargetConnectionFactory() {
		ConnectionFactory connectionFactory = getTargetConnectionFactory();
		Assert.state(connectionFactory != null, "No 'targetConnectionFactory' set");
		return connectionFactory;
	}


	@Override
	public void afterPropertiesSet() {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'targetConnectionFactory' is required");
		}
	}


	@Override
	public Connection getConnection() throws ResourceException {
		return obtainTargetConnectionFactory().getConnection();
	}

	@Override
	public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
		return obtainTargetConnectionFactory().getConnection(connectionSpec);
	}

	@Override
	public RecordFactory getRecordFactory() throws ResourceException {
		return obtainTargetConnectionFactory().getRecordFactory();
	}

	@Override
	public ResourceAdapterMetaData getMetaData() throws ResourceException {
		return obtainTargetConnectionFactory().getMetaData();
	}

	@Override
	public Reference getReference() throws NamingException {
		return obtainTargetConnectionFactory().getReference();
	}

	@Override
	public void setReference(Reference reference) {
		obtainTargetConnectionFactory().setReference(reference);
	}

}
