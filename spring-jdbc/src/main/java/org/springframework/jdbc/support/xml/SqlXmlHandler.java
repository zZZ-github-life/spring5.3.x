/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.support.xml;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.w3c.dom.Document;

import org.springframework.lang.Nullable;

/**
 * 用于处理特定数据库中 XML 字段的抽象。其主要目的是隔离对数据库中
 * 存储的 XML 的数据库特定处理。
 *
 * <p>JDBC 4.0 引入了新的数据类型 {@code java.sql.SQLXML}，
 * 但目前大多数数据库及其驱动仍依赖于数据库特定的数据类型和特性。
 *
 * <p>提供对 XML 字段的访问器方法，并充当
 * {@link SqlXmlValue} 实例的工厂。
 *
 * @author Thomas Risberg
 * @since 2.5.5
 * @see Jdbc4SqlXmlHandler
 * @see java.sql.SQLXML
 * @see java.sql.ResultSet#getSQLXML
 * @see java.sql.PreparedStatement#setSQLXML
 */
public interface SqlXmlHandler {

	//-------------------------------------------------------------------------
	// Convenience methods for accessing XML content
	//-------------------------------------------------------------------------

	/**
	 * 从给定的 ResultSet 中检索指定列的 String 值。
	 * <p>可能只是简单地调用 {@code ResultSet.getString}，也可能根据
	 * 数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as String, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getString
	 * @see java.sql.ResultSet#getSQLXML
	 */
	@Nullable
	String getXmlAsString(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的 ResultSet 中检索指定列的 String 值。
	 * <p>可能只是简单地调用 {@code ResultSet.getString}，也可能根据
	 * 数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as String, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getString
	 * @see java.sql.ResultSet#getSQLXML
	 */
	@Nullable
	String getXmlAsString(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的 ResultSet 中检索指定列的二进制流。
	 * <p>可能只是简单地调用 {@code ResultSet.getAsciiStream}，也可能根据
	 * 数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as a binary stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getBinaryStream
	 */
	@Nullable
	InputStream getXmlAsBinaryStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的 ResultSet 中检索指定列的二进制流。
	 * <p>可能只是简单地调用 {@code ResultSet.getAsciiStream}，也可能根据
	 * 数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as binary stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getBinaryStream
	 */
	@Nullable
	InputStream getXmlAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的 ResultSet 中检索指定列的字符流。
	 * <p>可能只是简单地调用 {@code ResultSet.getCharacterStream}，也可能根据
	 * 数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getCharacterStream
	 */
	@Nullable
	Reader getXmlAsCharacterStream(ResultSet rs, String columnName) throws SQLException;

	/**
	 * 从给定的 ResultSet 中检索指定列的字符流。
	 * <p>可能只是简单地调用 {@code ResultSet.getCharacterStream}，也可能根据
	 * 数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getCharacterStream
	 */
	@Nullable
	Reader getXmlAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException;

	/**
	 * 从给定的 ResultSet 中检索指定列的 Source，
	 * 该 Source 使用指定的 source 类实现。
	 * <p>可能根据数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnName the column name to use
	 * @param sourceClass the implementation class to be used
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getSource
	 */
	@Nullable
	Source getXmlAsSource(ResultSet rs, String columnName, @Nullable Class<? extends Source> sourceClass) throws SQLException;

	/**
	 * 从给定的 ResultSet 中检索指定列的 Source，
	 * 该 Source 使用指定的 source 类实现。
	 * <p>可能根据数据库和驱动使用 {@code SQLXML} 或数据库特定的类。
	 * @param rs the ResultSet to retrieve the content from
	 * @param columnIndex the column index to use
	 * @param sourceClass the implementation class to be used
	 * @return the content as character stream, or {@code null} in case of SQL NULL
	 * @throws SQLException if thrown by JDBC methods
	 * @see java.sql.ResultSet#getSQLXML
	 * @see java.sql.SQLXML#getSource
	 */
	@Nullable
	Source getXmlAsSource(ResultSet rs, int columnIndex, @Nullable Class<? extends Source> sourceClass) throws SQLException;


	//-------------------------------------------------------------------------
	// Convenience methods for building XML content
	//-------------------------------------------------------------------------

	/**
	 * 为给定的 XML 数据创建 {@code SqlXmlValue} 实例，
	 * 具体取决于底层 JDBC 驱动所支持的方式。
	 * @param value the XML String value providing XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setString(String)
	 */
	SqlXmlValue newSqlXmlValue(String value);

	/**
	 * 为给定的 XML 数据创建 {@code SqlXmlValue} 实例，
	 * 具体取决于底层 JDBC 驱动所支持的方式。
	 * @param provider the {@code XmlBinaryStreamProvider} providing XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setBinaryStream()
	 */
	SqlXmlValue newSqlXmlValue(XmlBinaryStreamProvider provider);

	/**
	 * 为给定的 XML 数据创建 {@code SqlXmlValue} 实例，
	 * 具体取决于底层 JDBC 驱动所支持的方式。
	 * @param provider the {@code XmlCharacterStreamProvider} providing XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setCharacterStream()
	 */
	SqlXmlValue newSqlXmlValue(XmlCharacterStreamProvider provider);

	/**
	 * 为给定的 XML 数据创建 {@code SqlXmlValue} 实例，
	 * 具体取决于底层 JDBC 驱动所支持的方式。
	 * @param resultClass the Result implementation class to be used
	 * @param provider the {@code XmlResultProvider} that will provide the XML data
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 * @see java.sql.SQLXML#setResult(Class)
	 */
	SqlXmlValue newSqlXmlValue(Class<? extends Result> resultClass, XmlResultProvider provider);

	/**
	 * 为给定的 XML 数据创建 {@code SqlXmlValue} 实例，
	 * 具体取决于底层 JDBC 驱动所支持的方式。
	 * @param doc the XML Document to be used
	 * @return the implementation specific instance
	 * @see SqlXmlValue
	 */
	SqlXmlValue newSqlXmlValue(Document doc);

}
