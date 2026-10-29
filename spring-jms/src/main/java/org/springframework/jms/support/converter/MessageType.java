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

package org.springframework.jms.support.converter;

/**
 * 用于指示要转换的目标消息类型的常量：{@link javax.jms.TextMessage}、
 * {@link javax.jms.BytesMessage}、{@link javax.jms.MapMessage}
 * 或 {@link javax.jms.ObjectMessage}。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see MarshallingMessageConverter#setTargetType
 */
public enum MessageType {

	/**
	 * {@link javax.jms.TextMessage} 类型的消息。
	 */
	TEXT,

	/**
	 * {@link javax.jms.BytesMessage} 类型的消息。
	 */
	BYTES,

	/**
	 * {@link javax.jms.MapMessage} 类型的消息。
	 */
	MAP,

	/**
	 * {@link javax.jms.ObjectMessage} 类型的消息。
	 */
	OBJECT

}
