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
 * 当预期结果至少应有一行（或元素），但实际返回了零行（或元素）时
 * 抛出的数据访问异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see IncorrectResultSizeDataAccessException
 */
@SuppressWarnings("serial")
public class EmptyResultDataAccessException extends IncorrectResultSizeDataAccessException {

	/**
	 * EmptyResultDataAccessException 的构造方法。
	 * @param expectedSize the expected result size
	 */
	public EmptyResultDataAccessException(int expectedSize) {
		super(expectedSize, 0);
	}

	/**
	 * EmptyResultDataAccessException 的构造方法。
	 * @param msg the detail message
	 * @param expectedSize the expected result size
	 */
	public EmptyResultDataAccessException(String msg, int expectedSize) {
		super(msg, expectedSize, 0);
	}

	/**
	 * EmptyResultDataAccessException 的构造方法。
	 * @param msg the detail message
	 * @param expectedSize the expected result size
	 * @param ex the wrapped exception
	 */
	public EmptyResultDataAccessException(String msg, int expectedSize, Throwable ex) {
		super(msg, expectedSize, 0, ex);
	}

}
