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

package org.springframework.dao;

/**
 * 查询超时时抛出的异常。这可能因所使用的数据库 API 不同而有不同的原因，
 * 但最可能是在数据库中断或停止查询处理（查询尚未完成）时抛出。
 *
 * <p>此异常可以由捕获原生数据库异常的用户代码抛出，
 * 也可以通过异常翻译机制抛出。
 *
 * @author Thomas Risberg
 * @since 3.1
 */
@SuppressWarnings("serial")
public class QueryTimeoutException extends TransientDataAccessException {

	/**
	 * QueryTimeoutException 的构造方法。
	 * @param msg 详细信息
	 */
	public QueryTimeoutException(String msg) {
		super(msg);
	}

	/**
	 * QueryTimeoutException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自所用数据访问 API 的根本原因
	 */
	public QueryTimeoutException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
