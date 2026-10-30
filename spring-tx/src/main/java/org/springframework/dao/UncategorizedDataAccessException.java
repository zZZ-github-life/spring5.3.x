/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * 当我们无法比"底层资源出了问题"更具体地分类时的常规超类，例如，
 * 来自 JDBC 的 SQLException 无法更精确地定位。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public abstract class UncategorizedDataAccessException extends NonTransientDataAccessException {

	/**
	 * UncategorizedDataAccessException 的构造方法。
	 * @param msg 详细信息
	 * @param cause 底层数据访问 API 抛出的异常
	 */
	public UncategorizedDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
