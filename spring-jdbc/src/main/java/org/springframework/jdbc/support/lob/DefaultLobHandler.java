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

package org.springframework.jdbc.support.lob;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * {@link LobHandler} 接口的默认实现。
 * 调用 {@code java.sql.ResultSet} 和 {@code java.sql.PreparedStatement} 提供的直接访问方法。
 *
 * <p>默认情况下，传入的流将被传递给 JDBC 驱动 {@link PreparedStatement}
 * 上相应的 {@code setBinary/Ascii/CharacterStream} 方法。
 * 如果指定的内容长度为负数，此处理器将使用不带长度参数的 JDBC 4.0 版 set-stream 方法；
 * 否则，它将把指定的长度传递给驱动。
 *
 * <p>只要 JDBC 驱动在规范关于简单 BLOB 和 CLOB 处理的建议方面符合 JDBC 规范，
 * 此 LobHandler 就应能正常工作。
 * 这完全不适用于 Oracle 9i 的驱动；从 Oracle 10g 起，它可以工作，
 * 但仍可能带有 LOB 大小限制。即使针对较旧的数据库服务器，
 * 也应考虑使用较新的 Oracle 驱动。
 * 完整建议请参见 {@link LobHandler} 的 javadoc。
 *
 * <p>某些 JDBC 驱动要求通过 JDBC {@code setBlob} / {@code setClob} API
 * 显式设置 BLOB/CLOB 目标列的值：例如 PostgreSQL 的驱动。
 * 针对此类驱动操作时，请将 {@link #setWrapAsLob "wrapAsLob"} 属性切换为 "true"。
 *
 * <p>在 JDBC 4.0 上，此 LobHandler 还支持通过直接接收流参数的
 * {@code setBlob} / {@code setClob} 变体来流式传输 BLOB/CLOB 内容。
 * 针对完全符合 JDBC 4.0 的驱动操作时，
 * 可考虑将 {@link #setStreamAsLob "streamAsLob"} 属性切换为 "true"。
 *
 * <p>最后，此 LobHandler 还支持创建临时 BLOB/CLOB 对象。
 * 当 "streamAsLob" 遇到 LOB 大小限制时，
 * 可考虑将 {@link #setCreateTemporaryLob "createTemporaryLob"} 属性切换为 "true"。
 *
 * <p>建议摘要请参见 {@link LobHandler} 接口的 javadoc。
 *
 * @author Juergen Hoeller
 * @since 04.12.2003
 * @see java.sql.ResultSet#getBytes
 * @see java.sql.ResultSet#getBinaryStream
 * @see java.sql.ResultSet#getString
 * @see java.sql.ResultSet#getAsciiStream
 * @see java.sql.ResultSet#getCharacterStream
 * @see java.sql.PreparedStatement#setBytes
 * @see java.sql.PreparedStatement#setBinaryStream
 * @see java.sql.PreparedStatement#setString
 * @see java.sql.PreparedStatement#setAsciiStream
 * @see java.sql.PreparedStatement#setCharacterStream
 */
public class DefaultLobHandler extends AbstractLobHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean wrapAsLob = false;

	private boolean streamAsLob = false;

	private boolean createTemporaryLob = false;


	/**
	 * 指定是否将字节数组 / String 包装在 JDBC Blob / Clob 对象中提交给 JDBC 驱动，
	 * 即使用以 Blob / Clob 为参数的 JDBC {@code setBlob} / {@code setClob} 方法。
	 * <p>默认值为 "false"，使用常见的 JDBC 2.0 {@code setBinaryStream}
	 * / {@code setCharacterStream} 方法来设置内容。
	 * 对于已知需要此类包装的 JDBC 驱动（例如 PostgreSQL 驱动访问 OID 列时需要，
	 * 而 BYTEA 列则需按标准方式访问），可将此值切换为 "true"。
	 * <p>此设置同时影响字节数组 / String 参数以及流参数，
	 * 除非 {@link #setStreamAsLob "streamAsLob"} 覆盖此处理方式，
	 * 改用 JDBC 4.0 新增的显式流支持（如果可用）。
	 * @see java.sql.PreparedStatement#setBlob(int, java.sql.Blob)
	 * @see java.sql.PreparedStatement#setClob(int, java.sql.Clob)
	 */
	public void setWrapAsLob(boolean wrapAsLob) {
		this.wrapAsLob = wrapAsLob;
	}

	/**
	 * 指定是否将二进制流 / 字符流作为显式 LOB 内容提交给 JDBC 驱动，
	 * 即使用以流为参数的 JDBC 4.0 {@code setBlob} / {@code setClob} 方法。
	 * <p>默认值为 "false"，使用常见的 JDBC 2.0 {@code setBinaryStream}
	 * / {@code setCharacterStream} 方法来设置内容。
	 * 如果你的 JDBC 驱动确实支持这些 JDBC 4.0 操作（例如 Derby 的驱动），
	 * 可将此值切换为 "true" 以启用显式 JDBC 4.0 流式传输。
	 * <p>此设置同时影响流参数以及字节数组 / String 参数，需要 JDBC 4.0 支持。
	 * 若要在 JDBC 3.0 上支持 LOB 内容，请查看 {@link #setWrapAsLob "wrapAsLob"} 设置。
	 * @see java.sql.PreparedStatement#setBlob(int, java.io.InputStream, long)
	 * @see java.sql.PreparedStatement#setClob(int, java.io.Reader, long)
	 */
	public void setStreamAsLob(boolean streamAsLob) {
		this.streamAsLob = streamAsLob;
	}

	/**
	 * 指定是否将字节数组 / String 复制到通过 JDBC 4.0 {@code createBlob} /
	 * {@code createClob} 方法创建的临时 JDBC Blob / Clob 对象中。
	 * <p>默认值为 "false"，使用常见的 JDBC 2.0 {@code setBinaryStream}
	 * / {@code setCharacterStream} 方法来设置内容。
	 * 可将此值切换为 "true"，使用 JDBC 4.0 显式创建 Blob / Clob。
	 * <p>此设置同时影响流参数以及字节数组 / String 参数，需要 JDBC 4.0 支持。
	 * 若要在 JDBC 3.0 上支持 LOB 内容，请查看 {@link #setWrapAsLob "wrapAsLob"} 设置。
	 * @see java.sql.Connection#createBlob()
	 * @see java.sql.Connection#createClob()
	 */
	public void setCreateTemporaryLob(boolean createTemporaryLob) {
		this.createTemporaryLob = createTemporaryLob;
	}


	@Override
	@Nullable
	public byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning BLOB as bytes");
		if (this.wrapAsLob) {
			Blob blob = rs.getBlob(columnIndex);
			return blob.getBytes(1, (int) blob.length());
		}
		else {
			return rs.getBytes(columnIndex);
		}
	}

	@Override
	@Nullable
	public InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning BLOB as binary stream");
		if (this.wrapAsLob) {
			Blob blob = rs.getBlob(columnIndex);
			return blob.getBinaryStream();
		}
		else {
			return rs.getBinaryStream(columnIndex);
		}
	}

	@Override
	@Nullable
	public String getClobAsString(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as string");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getSubString(1, (int) clob.length());
		}
		else {
			return rs.getString(columnIndex);
		}
	}

	@Override
	public InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as ASCII stream");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getAsciiStream();
		}
		else {
			return rs.getAsciiStream(columnIndex);
		}
	}

	@Override
	public Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as character stream");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getCharacterStream();
		}
		else {
			return rs.getCharacterStream(columnIndex);
		}
	}

	@Override
	public LobCreator getLobCreator() {
		return (this.createTemporaryLob ? new TemporaryLobCreator() : new DefaultLobCreator());
	}


	/**
	 * 作为内部类提供的默认 LobCreator 实现。
	 * 可在 DefaultLobHandler 的扩展类中继承。
	 */
	protected class DefaultLobCreator implements LobCreator {

		@Override
		public void setBlobAsBytes(PreparedStatement ps, int paramIndex, @Nullable byte[] content)
				throws SQLException {

			if (streamAsLob) {
				if (content != null) {
					ps.setBlob(paramIndex, new ByteArrayInputStream(content), content.length);
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (wrapAsLob) {
				if (content != null) {
					ps.setBlob(paramIndex, new PassThroughBlob(content));
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else {
				ps.setBytes(paramIndex, content);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(content != null ? "Set bytes for BLOB with length " + content.length :
						"Set BLOB to null");
			}
		}

		@Override
		public void setBlobAsBinaryStream(
				PreparedStatement ps, int paramIndex, @Nullable InputStream binaryStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (binaryStream != null) {
					if (contentLength >= 0) {
						ps.setBlob(paramIndex, binaryStream, contentLength);
					}
					else {
						ps.setBlob(paramIndex, binaryStream);
					}
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (wrapAsLob) {
				if (binaryStream != null) {
					ps.setBlob(paramIndex, new PassThroughBlob(binaryStream, contentLength));
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setBinaryStream(paramIndex, binaryStream, contentLength);
			}
			else {
				ps.setBinaryStream(paramIndex, binaryStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(binaryStream != null ? "Set binary stream for BLOB with length " + contentLength :
						"Set BLOB to null");
			}
		}

		@Override
		public void setClobAsString(PreparedStatement ps, int paramIndex, @Nullable String content)
				throws SQLException {

			if (streamAsLob) {
				if (content != null) {
					ps.setClob(paramIndex, new StringReader(content), content.length());
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (content != null) {
					ps.setClob(paramIndex, new PassThroughClob(content));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else {
				ps.setString(paramIndex, content);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(content != null ? "Set string for CLOB with length " + content.length() :
						"Set CLOB to null");
			}
		}

		@Override
		public void setClobAsAsciiStream(
				PreparedStatement ps, int paramIndex, @Nullable InputStream asciiStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (asciiStream != null) {
					Reader reader = new InputStreamReader(asciiStream, StandardCharsets.US_ASCII);
					if (contentLength >= 0) {
						ps.setClob(paramIndex, reader, contentLength);
					}
					else {
						ps.setClob(paramIndex, reader);
					}
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (asciiStream != null) {
					ps.setClob(paramIndex, new PassThroughClob(asciiStream, contentLength));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setAsciiStream(paramIndex, asciiStream, contentLength);
			}
			else {
				ps.setAsciiStream(paramIndex, asciiStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(asciiStream != null ? "Set ASCII stream for CLOB with length " + contentLength :
						"Set CLOB to null");
			}
		}

		@Override
		public void setClobAsCharacterStream(
				PreparedStatement ps, int paramIndex, @Nullable Reader characterStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (characterStream != null) {
					if (contentLength >= 0) {
						ps.setClob(paramIndex, characterStream, contentLength);
					}
					else {
						ps.setClob(paramIndex, characterStream);
					}
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (characterStream != null) {
					ps.setClob(paramIndex, new PassThroughClob(characterStream, contentLength));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setCharacterStream(paramIndex, characterStream, contentLength);
			}
			else {
				ps.setCharacterStream(paramIndex, characterStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(characterStream != null ? "Set character stream for CLOB with length " + contentLength :
						"Set CLOB to null");
			}
		}

		@Override
		public void close() {
			// nothing to do when not creating temporary LOBs
		}
	}

}
