/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.lang.Nullable;

/**
 * 用于解析 JMS 目的地的策略接口。
 *
 * <p>{@link org.springframework.jms.core.JmsTemplate} 使用它将简单的
 * {@link String String} 形式的目的地名称解析为实际的 {@link Destination}
 * 实现实例。
 *
 * <p>{@link org.springframework.jms.core.JmsTemplate} 实例使用的默认
 * {@link DestinationResolver} 实现是 {@link DynamicDestinationResolver} 类。
 * 对于更高级的场景，可以考虑使用 {@link JndiDestinationResolver}。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jms.core.JmsTemplate#setDestinationResolver
 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
 * @see org.springframework.jms.support.destination.JndiDestinationResolver
 */
@FunctionalInterface
public interface DestinationResolver {

	/**
	 * 解析给定的目的地名称，可以作为已定位的资源，也可以作为动态目的地。
	 * @param session 当前的 JMS Session
	 * （如果解析器实现无需它即可工作，则可能为 {@code null}）
	 * @param destinationName 目标的名称
	 * @param pubSubDomain 如果域为发布-订阅，则为 {@code true}；如果为点对点（P2P），则为 {@code false}
	 * @return JMS 目标（主题或队列）
	 * @throws javax.jms.JMSException 如果 JMS Session 无法解析目标
	 * @throws DestinationResolutionException 发生一般性目标解析失败时
	 */
	Destination resolveDestinationName(@Nullable Session session, String destinationName, boolean pubSubDomain)
			throws JMSException;

}
