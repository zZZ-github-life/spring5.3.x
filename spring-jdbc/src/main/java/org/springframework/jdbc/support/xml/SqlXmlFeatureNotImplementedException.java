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

package org.springframework.jdbc.support.xml;

import org.springframework.dao.InvalidDataAccessApiUsageException;

/**
 * 当底层实现不支持 API 所请求的功能时抛出的异常。
 *
 * @author Thomas Risberg
 * @since 2.5.5
 */
@SuppressWarnings("serial")
public class SqlXmlFeatureNotImplementedException extends InvalidDataAccessApiUsageException {

	/**
	 * 构造一个新的 {@code SqlXmlFeatureNotImplementedException}。
	 * @param msg the detail message
	 */
	public SqlXmlFeatureNotImplementedException(String msg) {
		super(msg);
	}

	/**
	 * 构造一个新的 {@code SqlXmlFeatureNotImplementedException}。
	 * @param msg the detail message
	 * @param cause the root cause from the data access API in use
	 */
	public SqlXmlFeatureNotImplementedException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
