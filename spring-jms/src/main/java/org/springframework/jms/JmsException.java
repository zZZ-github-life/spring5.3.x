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

package org.springframework.jms;

import javax.jms.JMSException;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * 框架在遇到与 JMS 相关的问题时抛出的异常的基类。
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 */
@SuppressWarnings("serial")
public abstract class JmsException extends NestedRuntimeException {

	/**
	 * 接受消息的构造函数。
	 * @param msg the detail message
	 */
	public JmsException(String msg) {
		super(msg);
	}

	/**
	 * 接受消息和根本原因的构造函数。
	 * @param msg the detail message
	 * @param cause the cause of the exception. This argument is generally
	 * expected to be a proper subclass of {@link javax.jms.JMSException},
	 * but can also be a JNDI NamingException or the like.
	 */
	public JmsException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

	/**
	 * 仅接受根本原因的构造函数，用于对应 {@code javax.jms} 异常的子类。
	 * @param cause the cause of the exception. This argument is generally
	 * expected to be a proper subclass of {@link javax.jms.JMSException}.
	 */
	public JmsException(@Nullable Throwable cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}


	/**
	 * 便捷方法：如果根本原因是 JMSException 的实例，
	 * 则获取厂商特定的错误代码。
	 * @return a string specifying the vendor-specific error code if the
	 * root cause is an instance of JMSException, or {@code null}
	 */
	@Nullable
	public String getErrorCode() {
		Throwable cause = getCause();
		if (cause instanceof JMSException) {
			return ((JMSException) cause).getErrorCode();
		}
		return null;
	}

	/**
	 * 返回详细信息，如果存在关联异常，则包含其消息。
	 * @see javax.jms.JMSException#getLinkedException()
	 */
	@Override
	@Nullable
	public String getMessage() {
		String message = super.getMessage();
		Throwable cause = getCause();
		if (cause instanceof JMSException) {
			Exception linkedEx = ((JMSException) cause).getLinkedException();
			if (linkedEx != null) {
				String linkedMessage = linkedEx.getMessage();
				String causeMessage = cause.getMessage();
				if (linkedMessage != null && (causeMessage == null || !causeMessage.contains(linkedMessage))) {
					message = message + "; nested exception is " + linkedEx;
				}
			}
		}
		return message;
	}

}
