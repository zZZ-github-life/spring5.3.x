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

package org.springframework.jca.cci;

import javax.resource.ResourceException;

import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * 当由于连接器不支持所需的 CCI Record 类型而导致 CCI Record 创建失败时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @deprecated as of 5.3, in favor of specific data access APIs
 * (or native CCI usage if there is no alternative)
 */
@Deprecated
@SuppressWarnings("serial")
public class RecordTypeNotSupportedException extends InvalidDataAccessResourceUsageException {

	/**
	 * RecordTypeNotSupportedException 的构造器。
	 * @param msg 消息
	 * @param ex 根本的 ResourceException 原因
	 */
	public RecordTypeNotSupportedException(String msg, ResourceException ex) {
		super(msg, ex);
	}

}
