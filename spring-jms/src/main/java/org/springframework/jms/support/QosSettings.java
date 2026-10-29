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

import javax.jms.Message;

import org.springframework.lang.Nullable;

/**
 * 收集发送消息时可使用的服务质量（Quality-of-Service）设置。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
public class QosSettings {

	private int deliveryMode;

	private int priority;

	private long timeToLive;


	/**
	 * 使用默认设置创建一个新实例。
	 * @see Message#DEFAULT_DELIVERY_MODE
	 * @see Message#DEFAULT_PRIORITY
	 * @see Message#DEFAULT_TIME_TO_LIVE
	 */
	public QosSettings() {
		this(Message.DEFAULT_DELIVERY_MODE, Message.DEFAULT_PRIORITY, Message.DEFAULT_TIME_TO_LIVE);
	}

	/**
	 * 使用指定的设置创建一个新实例。
	 */
	public QosSettings(int deliveryMode, int priority, long timeToLive) {
		this.deliveryMode = deliveryMode;
		this.priority = priority;
		this.timeToLive = timeToLive;
	}


	/**
	 * 设置发送消息时要使用的投递模式。
	 * 默认是 JMS Message 的默认值："PERSISTENT"。
	 * @param deliveryMode the delivery mode to use
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 * @see javax.jms.Message#DEFAULT_DELIVERY_MODE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * 返回发送消息时要使用的投递模式。
	 */
	public int getDeliveryMode() {
		return this.deliveryMode;
	}

	/**
	 * 设置发送时消息的优先级。
	 * @see javax.jms.Message#DEFAULT_PRIORITY
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * 返回发送时消息的优先级。
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * 设置发送时消息的生存时间。
	 * @param timeToLive the message's lifetime (in milliseconds)
	 * @see javax.jms.Message#DEFAULT_TIME_TO_LIVE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * 返回发送时消息的生存时间。
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof QosSettings)) {
			return false;
		}

		QosSettings otherSettings = (QosSettings) other;
		return (this.deliveryMode == otherSettings.deliveryMode &&
				this.priority == otherSettings.priority &&
				this.timeToLive == otherSettings.timeToLive);
	}

	@Override
	public int hashCode() {
		return (this.deliveryMode * 31 + this.priority);
	}

	@Override
	public String toString() {
		return "QosSettings{" + "deliveryMode=" + this.deliveryMode +
				", priority=" + this.priority + ", timeToLive=" + this.timeToLive + '}';
	}
}
