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
import javax.jms.Session;

import org.springframework.lang.Nullable;

/**
 * 用于在提供的 {@link Session} 上执行任意数量操作的回调。
 *
 * <p>与 {@link JmsTemplate#execute(SessionCallback)} 方法一起使用，
 * 通常实现为匿名内部类或 lambda 表达式。
 *
 * @author Mark Pollack
 * @since 1.1
 * @param <T> 结果类型
 * @see JmsTemplate#execute(SessionCallback)
 */
@FunctionalInterface
public interface SessionCallback<T> {

	/**
	 * 对所提供的 JMS {@link Session} 执行任意数量的操作，并可能返回结果。
	 * @param session 该 JMS {@code Session}
	 * @return 使用 {@code Session} 得到的结果对象（如果存在），
	 * 否则为 {@code null}
	 * @throws javax.jms.JMSException 如果由 JMS API 方法抛出
	 */
	@Nullable
	T doInJms(Session session) throws JMSException;

}
