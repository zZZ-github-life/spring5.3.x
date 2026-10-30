/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jms.connection;

import javax.jms.Session;

/**
 * {@link javax.jms.Session} 的子接口，由 Session 代理实现。
 * 允许访问底层的目标 Session。
 *
 * @author Juergen Hoeller
 * @since 2.0.4
 * @see TransactionAwareConnectionFactoryProxy
 * @see CachingConnectionFactory
 * @see ConnectionFactoryUtils#getTargetSession(javax.jms.Session)
 */
public interface SessionProxy extends Session {

	/**
	 * 返回此代理的目标 Session。
	 * <p>这通常是原生提供者的 Session，或会话池中的包装器。
	 * @return 底层 Session（永不为 {@code null}）
	 */
	Session getTargetSession();

}
