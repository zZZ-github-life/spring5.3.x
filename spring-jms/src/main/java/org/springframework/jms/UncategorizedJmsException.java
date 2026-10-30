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

package org.springframework.jms;

/**
 * 在找不到其他匹配的子类时抛出的 JmsException。
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public class UncategorizedJmsException extends JmsException {

	/**
	 * 接受消息的构造函数。
	 * @param msg 详细消息
	 */
	public UncategorizedJmsException(String msg) {
		super(msg);
	}

	/**
	 * 接受消息和根本原因的构造函数。
	 * @param msg 详细消息
	 * @param cause 异常的原因。该参数通常应为 {@link javax.jms.JMSException}
	 * 的适当子类，但也可以是 JNDI NamingException 等类似异常。
	 */
	public UncategorizedJmsException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * 仅接受根本原因的构造函数。
	 * @param cause 异常的原因。该参数通常应为 {@link javax.jms.JMSException}
	 * 的适当子类，但也可以是 JNDI NamingException 等类似异常。
	 */
	public UncategorizedJmsException(Throwable cause) {
		super("Uncategorized exception occurred during JMS processing", cause);
	}

}
