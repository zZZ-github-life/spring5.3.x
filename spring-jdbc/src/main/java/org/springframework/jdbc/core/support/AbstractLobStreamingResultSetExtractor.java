/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.LobRetrievalFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.lang.Nullable;

/**
 * 假定对 LOB 数据进行流式处理的抽象 ResultSetExtractor 实现。
 * 通常用作内部类，以便访问外围方法的参数。
 *
 * <p>委托给 {@code streamData} 模板方法将 LOB 内容流式传输到某个
 * OutputStream（通常使用 LobHandler）。将流式处理过程中抛出的
 * IOException 转换为 LobRetrievalFailureException。
 *
 * <p>使用 JdbcTemplate 的示例：
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * final LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.query(
 *		 "SELECT content FROM imagedb WHERE image_name=?", new Object[] {name},
 *		 new AbstractLobStreamingResultSetExtractor() {
 *			 public void streamData(ResultSet rs) throws SQLException, IOException {
 *				 FileCopyUtils.copy(lobHandler.getBlobAsBinaryStream(rs, 1), contentStream);
 *             }
 *         }
 * );</pre>
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @param <T> the result type
 * @see org.springframework.jdbc.support.lob.LobHandler
 * @see org.springframework.jdbc.LobRetrievalFailureException
 */
public abstract class AbstractLobStreamingResultSetExtractor<T> implements ResultSetExtractor<T> {

	/**
	 * 根据 ResultSet 的状态委托给 handleNoRowFound、handleMultipleRowsFound
	 * 和 streamData。将 streamData 抛出的 IOException 转换为
	 * LobRetrievalFailureException。
	 * @see #handleNoRowFound
	 * @see #handleMultipleRowsFound
	 * @see #streamData
	 * @see org.springframework.jdbc.LobRetrievalFailureException
	 */
	@Override
	@Nullable
	public final T extractData(ResultSet rs) throws SQLException, DataAccessException {
		if (!rs.next()) {
			handleNoRowFound();
		}
		else {
			try {
				streamData(rs);
				if (rs.next()) {
					handleMultipleRowsFound();
				}
			}
			catch (IOException ex) {
				throw new LobRetrievalFailureException("Could not stream LOB content", ex);
			}
		}
		return null;
	}

	/**
	 * 处理 ResultSet 不包含任何行的情况。
	 * @throws DataAccessException a corresponding exception,
	 * by default an EmptyResultDataAccessException
	 * @see org.springframework.dao.EmptyResultDataAccessException
	 */
	protected void handleNoRowFound() throws DataAccessException {
		throw new EmptyResultDataAccessException(
				"LobStreamingResultSetExtractor did not find row in database", 1);
	}

	/**
	 * 处理 ResultSet 包含多行的情况。
	 * @throws DataAccessException a corresponding exception,
	 * by default an IncorrectResultSizeDataAccessException
	 * @see org.springframework.dao.IncorrectResultSizeDataAccessException
	 */
	protected void handleMultipleRowsFound() throws DataAccessException {
		throw new IncorrectResultSizeDataAccessException(
				"LobStreamingResultSetExtractor found multiple rows in database", 1);
	}

	/**
	 * 将给定 ResultSet 中的 LOB 内容流式传输到某个 OutputStream。
	 * <p>通常用作内部类，以便访问外围方法的参数
	 * 以及外围类的 LobHandler 实例变量。
	 * @param rs the ResultSet to take the LOB content from
	 * @throws SQLException if thrown by JDBC methods
	 * @throws IOException if thrown by stream access methods
	 * @throws DataAccessException in case of custom exceptions
	 * @see org.springframework.jdbc.support.lob.LobHandler#getBlobAsBinaryStream
	 * @see org.springframework.util.FileCopyUtils
	 */
	protected abstract void streamData(ResultSet rs) throws SQLException, IOException, DataAccessException;

}
