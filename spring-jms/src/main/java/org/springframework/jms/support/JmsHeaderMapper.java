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

package org.springframework.jms.support;

import javax.jms.Message;

import org.springframework.messaging.support.HeaderMapper;

/**
 * 策略接口，用于将 {@link org.springframework.messaging.Message} 消息头映射到出站
 * JMS {@link javax.jms.Message}（例如配置 JMS 属性），或从入站 JMS Message 中
 * 提取消息头值。
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 4.1
 */
public interface JmsHeaderMapper extends HeaderMapper<Message> {

	/**
	 * 符合 JMS 规范的 {@code content_type} 属性。
	 */
	String CONTENT_TYPE_PROPERTY = "content_type";

}

