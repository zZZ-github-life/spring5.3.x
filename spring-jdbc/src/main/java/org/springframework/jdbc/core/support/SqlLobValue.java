/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.jdbc.core.DisposableSqlTypeValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.lang.Nullable;

/**
 * 表示 SQL BLOB/CLOB 值参数的对象。BLOB 可以是 InputStream 或字节数组。
 * CLOB 可以是 Reader、InputStream 或 String 形式。每个 CLOB/BLOB 值
 * 都与其长度一起存储。类型取决于所使用的构造器。此类的实例是有状态的
 * 且不可变的：使用后即丢弃。
 *
 * <p>此类持有对 {@link LobCreator} 的引用，该 {@code LobCreator} 必须在更新
 * 完成后关闭。这是通过调用 {@link #cleanup()} 方法完成的。
 * 对 {@code LobCreator} 的所有处理都由使用它的框架类完成——
 * 此类的最终用户无需设置或关闭 {@code LobCreator}。
 *
 * <p>使用示例：
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.update(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new Object[] {
 *       name,
 *       new SqlLobValue(contentStream, contentLength, lobHandler),
 *       new SqlLobValue(description, lobHandler)
 *     },
 *     new int[] {Types.VARCHAR, Types.BLOB, Types.CLOB});
 * </pre>
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jdbc.support.lob.LobHandler
 * @see org.springframework.jdbc.support.lob.LobCreator
 * @see org.springframework.jdbc.core.JdbcTemplate#update(String, Object[], int[])
 * @see org.springframework.jdbc.object.SqlUpdate#update(Object[])
 * @see org.springframework.jdbc.object.StoredProcedure#execute(java.util.Map)
 */
public class SqlLobValue implements DisposableSqlTypeValue {

	@Nullable
	private final Object content;

	private final int length;

	/**
	 * 对 LobCreator 的引用——以便在更新完成后将其关闭。
	 */
	private final LobCreator lobCreator;


	/**
	 * 使用给定的字节数组创建一个新的 BLOB 值，
	 * 使用 DefaultLobHandler。
	 * @param bytes 包含 BLOB 值的字节数组
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(@Nullable byte[] bytes) {
		this(bytes, new DefaultLobHandler());
	}

	/**
	 * 使用给定的字节数组创建一个新的 BLOB 值。
	 * @param bytes 包含 BLOB 值的字节数组
	 * @param lobHandler 要使用的 LobHandler
	 */
	public SqlLobValue(@Nullable byte[] bytes, LobHandler lobHandler) {
		this.content = bytes;
		this.length = (bytes != null ? bytes.length : 0);
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * 使用给定的内容字符串创建一个新的 CLOB 值，
	 * 使用 DefaultLobHandler。
	 * @param content 包含 CLOB 值的 String
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(@Nullable String content) {
		this(content, new DefaultLobHandler());
	}

	/**
	 * 使用给定的内容字符串创建一个新的 CLOB 值。
	 * @param content 包含 CLOB 值的 String
	 * @param lobHandler 要使用的 LobHandler
	 */
	public SqlLobValue(@Nullable String content, LobHandler lobHandler) {
		this.content = content;
		this.length = (content != null ? content.length() : 0);
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * 使用给定的流创建一个新的 BLOB/CLOB 值，
	 * 使用 DefaultLobHandler。
	 * @param stream 包含 LOB 值的流
	 * @param length LOB 值的长度
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(InputStream stream, int length) {
		this(stream, length, new DefaultLobHandler());
	}

	/**
	 * 使用给定的流创建一个新的 BLOB/CLOB 值。
	 * @param stream 包含 LOB 值的流
	 * @param length LOB 值的长度
	 * @param lobHandler 要使用的 LobHandler
	 */
	public SqlLobValue(InputStream stream, int length, LobHandler lobHandler) {
		this.content = stream;
		this.length = length;
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * 使用给定的字符流创建一个新的 CLOB 值，
	 * 使用 DefaultLobHandler。
	 * @param reader 包含 CLOB 值的字符流
	 * @param length CLOB 值的长度
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(Reader reader, int length) {
		this(reader, length, new DefaultLobHandler());
	}

	/**
	 * 使用给定的字符流创建一个新的 CLOB 值。
	 * @param reader 包含 CLOB 值的字符流
	 * @param length CLOB 值的长度
	 * @param lobHandler 要使用的 LobHandler
	 */
	public SqlLobValue(Reader reader, int length, LobHandler lobHandler) {
		this.content = reader;
		this.length = length;
		this.lobCreator = lobHandler.getLobCreator();
	}


	/**
	 * 通过 LobCreator 设置指定的内容。
	 */
	@Override
	public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException {

		if (sqlType == Types.BLOB) {
			if (this.content instanceof byte[] || this.content == null) {
				this.lobCreator.setBlobAsBytes(ps, paramIndex, (byte[]) this.content);
			}
			else if (this.content instanceof String) {
				this.lobCreator.setBlobAsBytes(ps, paramIndex, ((String) this.content).getBytes());
			}
			else if (this.content instanceof InputStream) {
				this.lobCreator.setBlobAsBinaryStream(ps, paramIndex, (InputStream) this.content, this.length);
			}
			else {
				throw new IllegalArgumentException(
						"Content type [" + this.content.getClass().getName() + "] not supported for BLOB columns");
			}
		}
		else if (sqlType == Types.CLOB) {
			if (this.content instanceof String || this.content == null) {
				this.lobCreator.setClobAsString(ps, paramIndex, (String) this.content);
			}
			else if (this.content instanceof InputStream) {
				this.lobCreator.setClobAsAsciiStream(ps, paramIndex, (InputStream) this.content, this.length);
			}
			else if (this.content instanceof Reader) {
				this.lobCreator.setClobAsCharacterStream(ps, paramIndex, (Reader) this.content, this.length);
			}
			else {
				throw new IllegalArgumentException(
						"Content type [" + this.content.getClass().getName() + "] not supported for CLOB columns");
			}
		}
		else {
			throw new IllegalArgumentException("SqlLobValue only supports SQL types BLOB and CLOB");
		}
	}

	/**
	 * 关闭 LobCreator（如果有）。
	 */
	@Override
	public void cleanup() {
		this.lobCreator.close();
	}

}
