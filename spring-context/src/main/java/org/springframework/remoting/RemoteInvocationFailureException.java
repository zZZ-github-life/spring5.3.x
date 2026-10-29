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

package org.springframework.remoting;

/**
 * RemoteAccessException 的子类，在服务器端执行目标方法失败时抛出，例如在目标对象上找不到某个方法时。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see RemoteProxyFailureException
 */
@SuppressWarnings("serial")
public class RemoteInvocationFailureException extends RemoteAccessException {

	/**
	 * RemoteInvocationFailureException 的构造器。
	 * @param msg the detail message
	 * @param cause the root cause from the remoting API in use
	 */
	public RemoteInvocationFailureException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
