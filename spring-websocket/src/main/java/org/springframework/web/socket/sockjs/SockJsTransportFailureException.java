/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.sockjs;

import org.springframework.lang.Nullable;

/**
 * 表示 SockJS 实现中（而非用户代码中）发生的严重故障（例如写入响应时发生 IOException）。
 * 引发此异常时，SockJS 会话通常会被关闭。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class SockJsTransportFailureException extends SockJsException {

	/**
	 * SockJsTransportFailureException 的构造函数。
	 * @param message the exception message
	 * @param cause the root cause
	 * @since 4.1.7
	 */
	public SockJsTransportFailureException(String message, @Nullable Throwable cause) {
		super(message, cause);
	}

	/**
	 * SockJsTransportFailureException 的构造函数。
	 * @param message the exception message
	 * @param sessionId the SockJS session id
	 * @param cause the root cause
	 */
	public SockJsTransportFailureException(String message, String sessionId, @Nullable Throwable cause) {
		super(message, sessionId, cause);
	}

}
