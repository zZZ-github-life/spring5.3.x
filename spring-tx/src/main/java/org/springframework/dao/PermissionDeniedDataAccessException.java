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
 * 当底层资源拒绝访问特定元素（例如特定的数据库表）的权限时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class PermissionDeniedDataAccessException extends NonTransientDataAccessException {

	/**
	 * PermissionDeniedDataAccessException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 来自底层数据访问 API（例如 JDBC）的根本原因
	 */
	public PermissionDeniedDataAccessException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
