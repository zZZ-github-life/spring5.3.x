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

package org.springframework.jms.support;

/**
 * 预定义的名称和前缀，用于在通用消息头中设置和/或读取 JMS 属性。
 *
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @since 4.1
 */
public interface JmsHeaders {

	/**
	 * 用于 JMS API 相关头部的名称前缀，以便与用户定义的头部和其他内部头部
	 * （例如 correlationId）区分开。
	 * @see SimpleJmsHeaderMapper
	 */
	String PREFIX = "jms_";

	/**
	 * 消息的关联 ID。这可能是此消息所回复消息的 {@link #MESSAGE_ID}。
	 * 也可以是应用程序特定的标识符。
	 * @see javax.jms.Message#getJMSCorrelationID()
	 */
	String CORRELATION_ID = PREFIX + "correlationId";

	/**
	 * 消息的目的地（主题或队列）名称。
	 * <p>只读值。
	 * @see javax.jms.Message#getJMSDestination()
	 * @see javax.jms.Destination
	 * @see javax.jms.Queue
	 * @see javax.jms.Topic
	 */
	String DESTINATION = PREFIX + "destination";

	/**
	 * 分发模式。
	 * <p>只读值。
	 * @see javax.jms.Message#getJMSDeliveryMode()
	 * @see javax.jms.DeliveryMode
	 */
	String DELIVERY_MODE = PREFIX + "deliveryMode";

	/**
	 * 消息的过期日期和时间。
	 * <p>只读值。
	 * @see javax.jms.Message#getJMSExpiration()
	 */
	String EXPIRATION = PREFIX + "expiration";

	/**
	 * 消息的唯一标识符。
	 * <p>只读值。
	 * @see javax.jms.Message#getJMSMessageID()
	 */
	String MESSAGE_ID = PREFIX + "messageId";

	/**
	 * 消息的优先级。
	 * <p>只读值。
	 * @see javax.jms.Message#getJMSPriority()
	 */
	String PRIORITY = PREFIX + "priority";

	/**
	 * 消息回复应发送到的目的地（主题或队列）名称。
	 * @see javax.jms.Message#getJMSReplyTo()
	 */
	String REPLY_TO = PREFIX + "replyTo";

	/**
	 * 指定消息是否被重新发送。当消息消费者未能确认消息接收时会发生这种情况。
	 * <p>只读值。
	 * @see javax.jms.Message#getJMSRedelivered()
	 */
	String REDELIVERED = PREFIX + "redelivered";

	/**
	 * 消息类型标签。此类型是一个从功能上描述该消息的字符串值。
	 * @see javax.jms.Message#getJMSType()
	 */
	String TYPE = PREFIX + "type";

	/**
	 * 消息发送操作的日期和时间。
	 * <p>只读值。
	 * @see javax.jms.Message#getJMSTimestamp()
	 */
	String TIMESTAMP = PREFIX + "timestamp";

}
