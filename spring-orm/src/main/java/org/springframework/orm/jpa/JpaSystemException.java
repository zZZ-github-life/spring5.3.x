/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.orm.jpa;

import org.springframework.dao.UncategorizedDataAccessException;

/**
 * {@link UncategorizedDataAccessException} 的 JPA 专用子类，
 * 用于表示不匹配任何具体 {@code org.springframework.dao} 异常的 JPA 系统错误。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see EntityManagerFactoryUtils#convertJpaAccessExceptionIfPossible
 */
@SuppressWarnings("serial")
public class JpaSystemException extends UncategorizedDataAccessException {

	public JpaSystemException(RuntimeException ex) {
		super(ex.getMessage(), ex);
	}

}
