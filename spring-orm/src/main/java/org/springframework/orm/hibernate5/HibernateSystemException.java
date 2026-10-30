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

package org.springframework.orm.hibernate5;

import org.hibernate.HibernateException;

import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.lang.Nullable;

/**
 * UncategorizedDataAccessException 的 Hibernate 专用子类，
 * 用于不匹配任何具体 {@code org.springframework.dao} 异常的
 * Hibernate 系统错误。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see SessionFactoryUtils#convertHibernateAccessException
 */
@SuppressWarnings("serial")
public class HibernateSystemException extends UncategorizedDataAccessException {

	/**
	 * 创建一个新的 HibernateSystemException，
	 * 包装任意 HibernateException。
	 * @param cause 抛出的 HibernateException
	 */
	public HibernateSystemException(@Nullable HibernateException cause) {
		super(cause != null ? cause.getMessage() : null, cause);
	}

}
