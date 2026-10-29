/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;

/**
 * 管理 {@link LobCreator} 的抽象 {@link PreparedStatementCallback} 实现。
 * 通常用作内部类，以便访问外围方法的参数。
 *
 * <p>对于 BLOB/CLOB 参数，使用给定的 LobCreator 委托给 {@code setValues}
 * 模板方法在 PreparedStatement 上设置值。
 *
 * <p>使用 {@link org.springframework.jdbc.core.JdbcTemplate} 的示例：
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.execute(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new AbstractLobCreatingPreparedStatementCallback(lobHandler) {
 *       protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException {
 *         ps.setString(1, name);
 *         lobCreator.setBlobAsBinaryStream(ps, 2, contentStream, contentLength);
 *         lobCreator.setClobAsString(ps, 3, description);
 *       }
 *     }
 * );</pre>
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.jdbc.support.lob.LobCreator
 */
public abstract class AbstractLobCreatingPreparedStatementCallback implements PreparedStatementCallback<Integer> {

	private final LobHandler lobHandler;


	/**
	 * 为给定的 LobHandler 创建一个新的
	 * AbstractLobCreatingPreparedStatementCallback。
	 * @param lobHandler the LobHandler to create LobCreators with
	 */
	public AbstractLobCreatingPreparedStatementCallback(LobHandler lobHandler) {
		Assert.notNull(lobHandler, "LobHandler must not be null");
		this.lobHandler = lobHandler;
	}


	@Override
	public final Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
		try (LobCreator lobCreator = this.lobHandler.getLobCreator()) {
			setValues(ps, lobCreator);
			return ps.executeUpdate();
		}
	}

	/**
	 * 使用给定的 LobCreator 在给定的 PreparedStatement 上
	 * 为 BLOB/CLOB 参数设置值。
	 * @param ps the PreparedStatement to use
	 * @param lobCreator the LobCreator to use
	 * @throws SQLException if thrown by JDBC methods
	 * @throws DataAccessException in case of custom exceptions
	 */
	protected abstract void setValues(PreparedStatement ps, LobCreator lobCreator)
			throws SQLException, DataAccessException;

}
