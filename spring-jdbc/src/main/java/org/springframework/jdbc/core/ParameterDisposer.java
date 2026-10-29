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

package org.springframework.jdbc.core;

/**
 * 由能够关闭参数分配的资源（如{@code SqlLobValue}对象）的对象实现的接口。
 *
 * <p>通常由支持{@link DisposableSqlTypeValue}对象（如{@code SqlLobValue}）
 * 作为参数的{@code PreparedStatementCreators}和{@code PreparedStatementSetters}实现。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see PreparedStatementCreator
 * @see PreparedStatementSetter
 * @see DisposableSqlTypeValue
 * @see org.springframework.jdbc.core.support.SqlLobValue
 */
public interface ParameterDisposer {

	/**
	 * 关闭实现对象持有的参数所分配的资源，
	 * 例如在DisposableSqlTypeValue（如SqlLobValue）的情况下。
	 * @see DisposableSqlTypeValue#cleanup()
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 */
	void cleanupParameters();

}
