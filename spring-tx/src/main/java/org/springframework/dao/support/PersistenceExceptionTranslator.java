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

package org.springframework.dao.support;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * 由 Spring 与抛出运行时异常的数据访问技术（如 JPA 和 Hibernate）集成时实现的接口。
 *
 * <p>这允许一致地使用组合的异常翻译功能，
 * 而无需强制单个翻译器理解每一种可能的异常类型。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 */
@FunctionalInterface
public interface PersistenceExceptionTranslator {

	/**
	 * 如果可能，将持久化框架抛出的给定运行时异常翻译为
	 * Spring 通用的 {@link org.springframework.dao.DataAccessException}
	 * 层次结构中对应的异常。
	 * <p>不要翻译此翻译器不理解的异常：例如，如果异常来自另一个持久化框架，
	 * 或者源于用户代码或与持久化无关。
	 * <p>其中特别重要的是正确翻译为 DataIntegrityViolationException，
	 * 例如在约束违反时。实现可以使用 Spring JDBC 的高级异常翻译功能，
	 * 在 SQLException 作为根原因时提供更多信息。
	 * @param ex 要转换的 RuntimeException
	 * @return 对应的 DataAccessException；如果该异常无法转换，
	 * 则为 {@code null}，因为这种情况下它可能来自用户代码，
	 * 而不是实际的持久化问题
	 * @see org.springframework.dao.DataIntegrityViolationException
	 * @see org.springframework.jdbc.support.SQLExceptionTranslator
	 */
	@Nullable
	DataAccessException translateExceptionIfPossible(RuntimeException ex);

}
