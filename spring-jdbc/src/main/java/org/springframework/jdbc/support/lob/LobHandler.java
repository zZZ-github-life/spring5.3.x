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

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.lang.Nullable;

/**
 * 对特定数据库中大型二进制字段和大型文本字段的处理抽象，
 * 无论这些字段是以简单类型还是大型对象（LOB）形式表示。
 *
 * <p>提供 BLOB 和 CLOB 的访问方法，并充当 LobCreator 实例的工厂，
 * LobCreator 实例用作创建 BLOB 或 CLOB 的会话。
 * LobCreator 通常为每次语句执行或每个事务实例化；
 * 它们不是线程安全的，因为它们可能会跟踪已分配的数据库资源，
 * 以便在执行后释放这些资源。
 *
 * <p>大多数数据库/驱动都应能配合 {@link DefaultLobHandler} 工作，
 * 它默认委托给 JDBC 的直接访问方法，完全避免使用
 * {@code java.sql.Blob} 和 {@code java.sql.Clob} API。
 * 通过设置 {@link DefaultLobHandler#setWrapAsLob "wrapAsLob"} 属性，
 * {@link DefaultLobHandler} 还可以配置为使用
 * {@code PreparedStatement.setBlob/setClob} 访问 LOB（例如用于 PostgreSQL）。
 *
 * <p>当然，你需要为每种数据库声明不同的字段类型。
 * 在 Oracle 中，任何二进制内容都需要放入 BLOB，所有超过 4000 字节的字符内容
 * 都需要放入 CLOB。在 MySQL 中没有 CLOB 类型的概念，
 * 而是一种行为类似 VARCHAR 的 LONGTEXT 类型。
 * 为了实现完全的移植性，对于某些数据库中通常因字段大小而可能需要 LOB 的字段，
 * 请使用 LobHandler（可以 Oracle 的数值作为参考）。
 *
 * <p><b>推荐选项总结（针对实际的 LOB 字段）：</b>
 * <ul>
 * <li><b>JDBC 4.0 驱动（包括 Oracle 11g 驱动）：</b> 使用 {@link DefaultLobHandler}，
 * 如果你的数据库驱动在填充 LOB 字段时需要相应提示，可配合 {@code streamAsLob=true}。
 * 如果你使用的（Oracle）数据库配置遇到 LOB 大小限制，
 * 可以回退到 {@code createTemporaryLob=true}。
 * <li><b>Oracle 10g 驱动：</b> 使用标准配置的 {@link DefaultLobHandler}。
 * 在 Oracle 10.1 上，设置 "SetBigStringTryClob" 连接属性；从 Oracle 10.2 起，
 * DefaultLobHandler 使用标准配置开箱即用。
 * <li><b>PostgreSQL：</b> 将 {@link DefaultLobHandler} 配置为 {@code wrapAsLob=true}，
 * 并使用该 LobHandler 访问数据库表中的 OID 列（而不是 BYTEA 列）。
 * <li>对于所有其他数据库驱动（以及某些数据库中可能变成 LOB 的非 LOB 字段）：
 * 直接使用普通的 {@link DefaultLobHandler} 即可。
 * </ul>
 *
 * @author Juergen Hoeller
 * @since 23.12.2003
 * @see DefaultLobHandler
 * @see java.sql.ResultSet#getBlob
 * @see java.sql.ResultSet#getClob
 * @see java.sql.ResultSet#getBytes
 * @see java.sql.ResultSet#getBinaryStream
 * @see java.sql.ResultSet#getString
 * @see java.sql.ResultSet#getAsciiStream
 * @see java.sql.ResultSet#getCharacterStream
 */
public interface LobHandler {

	/**
	 * 从给定 ResultSet 中检索给定列，以字节形式返回。
	 * 可能只是调用 {@code ResultSet.getBytes}，也可能使用
	 * {@code ResultSet.getBlob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as byte array, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getBytes
	 */
	@Nullable
	byte[] getBlobAsBytes(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以字节形式返回。
	 * 可能只是调用 {@code ResultSet.getBytes}，也可能使用
	 * {@code ResultSet.getBlob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as byte array, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getBytes
	 */
	@Nullable
	byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以二进制流形式返回。
	 * 可能只是调用 {@code ResultSet.getBinaryStream}，也可能使用
	 * {@code ResultSet.getBlob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as binary stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getBinaryStream
	 */
	@Nullable
	InputStream getBlobAsBinaryStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以二进制流形式返回。
	 * 可能只是调用 {@code ResultSet.getBinaryStream}，也可能使用
	 * {@code ResultSet.getBlob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as binary stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getBinaryStream
	 */
	@Nullable
	InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以 String 形式返回。
	 * 可能只是调用 {@code ResultSet.getString}，也可能使用
	 * {@code ResultSet.getClob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as String, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getString
	 */
	@Nullable
	String getClobAsString(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以 String 形式返回。
	 * 可能只是调用 {@code ResultSet.getString}，也可能使用
	 * {@code ResultSet.getClob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as String, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getString
	 */
	@Nullable
	String getClobAsString(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以 ASCII 流形式返回。
	 * 可能只是调用 {@code ResultSet.getAsciiStream}，也可能使用
	 * {@code ResultSet.getClob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as ASCII stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getAsciiStream
	 */
	@Nullable
	InputStream getClobAsAsciiStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以 ASCII 流形式返回。
	 * 可能只是调用 {@code ResultSet.getAsciiStream}，也可能使用
	 * {@code ResultSet.getClob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as ASCII stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getAsciiStream
	 */
	@Nullable
	InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以字符流形式返回。
	 * 可能只是调用 {@code ResultSet.getCharacterStream}，也可能使用
	 * {@code ResultSet.getClob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as character stream
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getCharacterStream
	 */
	Reader getClobAsCharacterStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定 ResultSet 中检索给定列，以字符流形式返回。
	 * 可能只是调用 {@code ResultSet.getCharacterStream}，也可能使用
	 * {@code ResultSet.getClob}，具体取决于数据库和驱动。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as character stream
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getCharacterStream
	 */
	Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 创建一个新的 {@link LobCreator} 实例，即一个用于创建 BLOB 和 CLOB 的会话。
	 * 在创建的 LOB 不再需要之后需要关闭它，
	 * 通常是在语句执行或事务完成之后。
	 * @return the new LobCreator instance
	 * @see LobCreator#close()
	 */
	LobCreator getLobCreator();

}
