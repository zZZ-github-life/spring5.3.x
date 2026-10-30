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
 * 当 API 使用不当时抛出的异常，例如
 * 未能对需要在执行前进行"编译"的查询对象进行编译。
 *
 * <p>这表示我们的 Java 数据访问框架中的问题，
 * 而不是底层数据访问基础设施的问题。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class InvalidDataAccessApiUsageException extends NonTransientDataAccessException {

	/**
	 * InvalidDataAccessApiUsageException 的构造方法。
	 * @param msg 详细信息
	 */
	public InvalidDataAccessApiUsageException(String msg) {
		super(msg);
	}

	/**
	 * InvalidDataAccessApiUsageException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自所用数据访问 API 的根本原因
	 */
	public InvalidDataAccessApiUsageException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
