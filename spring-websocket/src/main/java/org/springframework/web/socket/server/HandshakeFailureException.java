/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.server;

import org.springframework.core.NestedRuntimeException;

/**
 * 当握手处理由于内部不可恢复的错误而未能完成时抛出。这意味着服务器错误
 * （HTTP 状态码 500），而不是握手协商失败。
 *
 * <p>相比之下，当握手协商失败时，响应状态码将为 200，
 * 并且响应头和响应体将被更新以反映失败原因。
 * 在这些情况下，{@link HandshakeHandler} 实现会提供受保护的方法
 * 来定制对响应的更新。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("serial")
public class HandshakeFailureException extends NestedRuntimeException {

	public HandshakeFailureException(String message) {
		super(message);
	}

	public HandshakeFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
