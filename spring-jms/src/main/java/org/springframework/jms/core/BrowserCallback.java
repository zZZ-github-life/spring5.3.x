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

package org.springframework.jms.core;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.springframework.lang.Nullable;

/**
 * 用于浏览 JMS 队列中消息的回调。
 *
 * <p>与 {@link JmsTemplate} 中接受 {@link BrowserCallback} 参数的回调方法一起使用，
 * 通常实现为匿名内部类或 lambda 表达式。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @param <T> the result type
 * @see JmsTemplate#browse(BrowserCallback)
 * @see JmsTemplate#browseSelected(String, BrowserCallback)
 */
@FunctionalInterface
public interface BrowserCallback<T> {

	/**
	 * 对给定的 {@link javax.jms.Session} 和 {@link javax.jms.QueueBrowser} 执行操作。
	 * @param session the JMS {@code Session} object to use
	 * @param browser the JMS {@code QueueBrowser} object to use
	 * @return a result object from working with the {@code Session}, if any
	 * (or {@code null} if none)
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	@Nullable
	T doInJms(Session session, QueueBrowser browser) throws JMSException;

}
