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

package org.springframework.jca.cci;

import java.sql.SQLException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 当以无效方式访问 ResultSet 时抛出的异常。
 * 此类异常始终具有 {@code java.sql.SQLException} 根原因。
 *
 * <p>通常发生在指定了无效的 ResultSet 列索引或列名时。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.resource.cci.ResultSet
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class InvalidResultSetAccessException extends InvalidDataAccessResourceUsageException {

	/**
	 * InvalidResultSetAccessException 的构造器。
	 * @param msg message
	 * @param ex the root cause
	 */
	public InvalidResultSetAccessException(String msg, SQLException ex) {
		super(ex.getMessage(), ex);
	}

}
