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

package org.springframework.jdbc.support.lob;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.lang.Nullable;

/**
 * 抽象了可能因数据库而异的大型二进制字段和大型文本字段创建过程的接口。
 * API 中不使用 {@code java.sql.Blob} 和 {@code java.sql.Clob} 实例，
 * 因为某些 JDBC 驱动并不直接支持这些类型。
 *
 * <p>LOB 创建部分是各 {@link LobHandler} 实现通常存在差异的地方。
 * 可能的策略包括使用 {@code PreparedStatement.setBinaryStream/setCharacterStream}，
 * 以及使用带流参数（需要 JDBC 4.0）或 {@code java.sql.Blob/Clob} 包装对象的
 * {@code PreparedStatement.setBlob/setClob}。
 *
 * <p>LobCreator 表示一个用于创建 BLOB 的会话：它<i>并非</i>线程安全的，
 * 需要为每次语句执行或每个事务实例化一个新对象。
 * 每个 LobCreator 在使用完毕后都需要关闭。
 *
 * <p>为方便地配合 PreparedStatement 和 LobCreator 使用，
 * 可考虑将 {@link org.springframework.jdbc.core.JdbcTemplate} 与
 *{@link org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback}
 * 实现结合使用。详情请参见后者的 javadoc。
 *
 * @author Juergen Hoeller
 * @since 04.12.2003
 * @see #close()
 * @see LobHandler#getLobCreator()
 * @see DefaultLobHandler.DefaultLobCreator
 * @see java.sql.PreparedStatement#setBlob
 * @see java.sql.PreparedStatement#setClob
 * @see java.sql.PreparedStatement#setBytes
 * @see java.sql.PreparedStatement#setBinaryStream
 * @see java.sql.PreparedStatement#setString
 * @see java.sql.PreparedStatement#setAsciiStream
 * @see java.sql.PreparedStatement#setCharacterStream
 */
public interface LobCreator extends Closeable {

	/**
	 * 使用给定参数索引将给定内容以字节形式设置到给定语句上。
	 * 可能只是调用 {@code PreparedStatement.setBytes}，
	 * 也可能为其创建 Blob 实例，具体取决于数据库和驱动。
	 * @param ps 要为其设置内容的 PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param content 以字节数组表示的内容，对于 SQL NULL 为 {@code null}
	 * @throws SQLException 如果由 JDBC 方法抛出
	 * @see java.sql.PreparedStatement#setBytes
	 */
	void setBlobAsBytes(PreparedStatement ps, int paramIndex, @Nullable byte[] content)
			throws SQLException;

	/**
	 * 使用给定参数索引将给定内容以二进制流形式设置到给定语句上。
	 * 可能只是调用 {@code PreparedStatement.setBinaryStream}，
	 * 也可能为其创建 Blob 实例，具体取决于数据库和驱动。
	 * @param ps 要为其设置内容的 PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param contentStream 以二进制流表示的内容，对于 SQL NULL 为 {@code null}
	 * @throws SQLException 如果由 JDBC 方法抛出
	 * @see java.sql.PreparedStatement#setBinaryStream
	 */
	void setBlobAsBinaryStream(
			PreparedStatement ps, int paramIndex, @Nullable InputStream contentStream, int contentLength)
			throws SQLException;

	/**
	 * 使用给定参数索引将给定内容以 String 形式设置到给定语句上。
	 * 可能只是调用 {@code PreparedStatement.setString}，
	 * 也可能为其创建 Clob 实例，具体取决于数据库和驱动。
	 * @param ps 要为其设置内容的 PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param content 以 String 表示的内容，对于 SQL NULL 为 {@code null}
	 * @throws SQLException 如果由 JDBC 方法抛出
	 * @see java.sql.PreparedStatement#setBytes
	 */
	void setClobAsString(PreparedStatement ps, int paramIndex, @Nullable String content)
			throws SQLException;

	/**
	 * 使用给定参数索引将给定内容以 ASCII 流形式设置到给定语句上。
	 * 可能只是调用 {@code PreparedStatement.setAsciiStream}，
	 * 也可能为其创建 Clob 实例，具体取决于数据库和驱动。
	 * @param ps 要为其设置内容的 PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param asciiStream 以 ASCII 流表示的内容，对于 SQL NULL 为 {@code null}
	 * @throws SQLException 如果由 JDBC 方法抛出
	 * @see java.sql.PreparedStatement#setAsciiStream
	 */
	void setClobAsAsciiStream(
			PreparedStatement ps, int paramIndex, @Nullable InputStream asciiStream, int contentLength)
			throws SQLException;

	/**
	 * 使用给定参数索引将给定内容以字符流形式设置到给定语句上。
	 * 可能只是调用 {@code PreparedStatement.setCharacterStream}，
	 * 也可能为其创建 Clob 实例，具体取决于数据库和驱动。
	 * @param ps 要为其设置内容的 PreparedStatement
	 * @param paramIndex 要使用的参数索引
	 * @param characterStream 以字符流表示的内容，对于 SQL NULL 为 {@code null}
	 * @throws SQLException 如果由 JDBC 方法抛出
	 * @see java.sql.PreparedStatement#setCharacterStream
	 */
	void setClobAsCharacterStream(
			PreparedStatement ps, int paramIndex, @Nullable Reader characterStream, int contentLength)
			throws SQLException;

	/**
	 * 关闭此 LobCreator 会话，并释放其临时创建的 BLOB 和 CLOB。
	 * 如果使用 PreparedStatement 的标准方法，则无需做任何事情；
	 * 但如果使用专有手段，则可能需要释放数据库资源。
	 * <p><b>注意</b>：必须在相关 PreparedStatement 执行完毕
	 * 或受影响的 O/R 映射会话已刷新之后调用。
	 * 否则，临时 BLOB 对应的数据库资源可能仍处于已分配状态。
	 */
	@Override
	void close();

}
