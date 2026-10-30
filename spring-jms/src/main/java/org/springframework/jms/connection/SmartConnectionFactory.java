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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

/**
 * {@code javax.jms.ConnectionFactory} 接口的扩展，
 * 用于指示如何释放从其中获取的 Connection。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 */
public interface SmartConnectionFactory extends ConnectionFactory {

	/**
	 * 是否应停止从此 ConnectionFactory 获取的 Connection？
	 * @param con 要检查的 Connection
	 * @return 是否需要调用 stop
	 * @see javax.jms.Connection#stop()
	 */
	boolean shouldStop(Connection con);

}
