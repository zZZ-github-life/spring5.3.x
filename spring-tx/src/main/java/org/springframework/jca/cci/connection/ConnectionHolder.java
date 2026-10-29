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

package org.springframework.jca.cci.connection;

import javax.resource.cci.Connection;

import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * 包装 CCI {@link Connection} 的资源持有者。
 * {@link CciLocalTransactionManager} 将此类的实例绑定到线程，
 * 对应给定的 {@link javax.resource.cci.ConnectionFactory}。
 *
 * <p>注意：这是一个 SPI 类，不打算被应用程序使用。
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see CciLocalTransactionManager
 * @see ConnectionFactoryUtils
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
public class ConnectionHolder extends ResourceHolderSupport {

	private final Connection connection;


	public ConnectionHolder(Connection connection) {
		this.connection = connection;
	}


	public Connection getConnection() {
		return this.connection;
	}

}
