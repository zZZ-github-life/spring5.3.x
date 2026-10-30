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

package org.springframework.transaction;

/**
 * 当指定了无效的超时时抛出此异常，
 * 即指定的超时值超出范围或事务管理器实现不支持超时。
 *
 * @author Juergen Hoeller
 * @since 12.05.2003
 */
@SuppressWarnings("serial")
public class InvalidTimeoutException extends TransactionUsageException {

	private final int timeout;


	/**
	 * InvalidTimeoutException 的构造函数。
	 * @param msg 详细信息
	 * @param timeout 无效的超时值
	 */
	public InvalidTimeoutException(String msg, int timeout) {
		super(msg);
		this.timeout = timeout;
	}

	/**
	 * 返回无效的超时值。
	 */
	public int getTimeout() {
		return this.timeout;
	}

}
