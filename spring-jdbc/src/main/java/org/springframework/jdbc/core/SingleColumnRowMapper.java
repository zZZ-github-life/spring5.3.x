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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.jdbc.IncorrectResultSetColumnCountException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;

/**
 * {@link RowMapper}实现，将单列转换为每行单个结果值。
 * 期望操作仅包含单列的{@code java.sql.ResultSet}。
 *
 * <p>可以指定每行结果值的类型。将从{@code ResultSet}中提取单列的值
 * 并转换为指定的目标类型。
 *
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @since 1.2
 * @param <T> 结果类型
 * @see JdbcTemplate#queryForList(String, Class)
 * @see JdbcTemplate#queryForObject(String, Class)
 */
public class SingleColumnRowMapper<T> implements RowMapper<T> {

	@Nullable
	private Class<?> requiredType;

	@Nullable
	private ConversionService conversionService = DefaultConversionService.getSharedInstance();

	/**
	 * 创建新的{@code SingleColumnRowMapper}，用于bean风格的配置。
	 * @see #setRequiredType
	 */
	public SingleColumnRowMapper() {
	}

	/**
	 * 创建新的{@code SingleColumnRowMapper}。
	 * @param requiredType 每个结果对象应匹配的类型
	 */
	public SingleColumnRowMapper(Class<T> requiredType) {
		setRequiredType(requiredType);
	}


	/**
	 * 设置每个结果对象期望匹配的类型。
	 * <p>如果未指定，列值将以JDBC驱动返回的形式暴露。
	 */
	public void setRequiredType(Class<T> requiredType) {
		this.requiredType = ClassUtils.resolvePrimitiveIfNecessary(requiredType);
	}

	/**
	 * 设置用于转换获取值的{@link ConversionService}。
	 * <p>默认为{@link DefaultConversionService}。
	 * @since 5.0.4
	 * @see DefaultConversionService#getSharedInstance
	 */
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 提取当前行中单列的值。
	 * <p>验证只选择了一列，然后委托给{@code getColumnValue()}，
	 * 如有必要还会委托给{@code convertValueToRequiredType()}。
	 * @see java.sql.ResultSetMetaData#getColumnCount()
	 * @see #getColumnValue(java.sql.ResultSet, int, Class)
	 * @see #convertValueToRequiredType(Object, Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public T mapRow(ResultSet rs, int rowNum) throws SQLException {
		// Validate column count.
		ResultSetMetaData rsmd = rs.getMetaData();
		int nrOfColumns = rsmd.getColumnCount();
		if (nrOfColumns != 1) {
			throw new IncorrectResultSetColumnCountException(1, nrOfColumns);
		}

		// Extract column value from JDBC ResultSet.
		Object result = getColumnValue(rs, 1, this.requiredType);
		if (result != null && this.requiredType != null && !this.requiredType.isInstance(result)) {
			// Extracted value does not match already: try to convert it.
			try {
				return (T) convertValueToRequiredType(result, this.requiredType);
			}
			catch (IllegalArgumentException ex) {
				throw new TypeMismatchDataAccessException(
						"Type mismatch affecting row number " + rowNum + " and column type '" +
						rsmd.getColumnTypeName(1) + "': " + ex.getMessage());
			}
		}
		return (T) result;
	}

	/**
	 * 检索指定列的JDBC对象值。
	 * <p>默认实现调用{@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}。
	 * 如果未指定必需类型，此方法委托给{@code getColumnValue(rs, index)}，
	 * 它基本上调用{@code ResultSet.getObject(index)}，但会对适当的值类型应用一些额外的默认转换。
	 * @param rs 是持有数据的 ResultSet
	 * @param index 是列索引
	 * @param requiredType 每个结果对象应匹配的类型
	 * （如果未指定则为 {@code null}）
	 * @return Object 值
	 * @throws SQLException 在提取失败的情况下
	 * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)
	 * @see #getColumnValue(java.sql.ResultSet, int)
	 */
	@Nullable
	protected Object getColumnValue(ResultSet rs, int index, @Nullable Class<?> requiredType) throws SQLException {
		if (requiredType != null) {
			return JdbcUtils.getResultSetValue(rs, index, requiredType);
		}
		else {
			// No required type specified -> perform default extraction.
			return getColumnValue(rs, index);
		}
	}

	/**
	 * 检索指定列的JDBC对象值，使用最合适的值类型。
	 * 在未指定必需类型时调用。
	 * <p>默认实现委托给{@code JdbcUtils.getResultSetValue()}，
	 * 它使用{@code ResultSet.getObject(index)}方法。此外，
	 * 它包含一个"技巧"来绕过Oracle为其TIMESTAMP数据类型返回非标准对象的问题。
	 * 详情请参见{@code JdbcUtils#getResultSetValue()}的javadoc。
	 * @param rs 是持有数据的 ResultSet
	 * @param index 是列索引
	 * @return Object 值
	 * @throws SQLException 在提取失败的情况下
	 * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int)
	 */
	@Nullable
	protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index);
	}

	/**
	 * 将给定的列值转换为指定的必需类型。
	 * 仅在提取的列值不匹配时调用。
	 * <p>如果必需类型是String，值将通过{@code toString()}简单地转换为字符串。
	 * 如果是Number类型，值将通过数字转换或字符串解析（取决于值类型）转换为Number。
	 * 否则，值将使用{@link ConversionService}转换为必需类型。
	 * @param value 从 {@code getColumnValue()} 提取的列值
	 * （永不为 {@code null}）
	 * @param requiredType 每个结果对象应匹配的类型（永不为 {@code null}）
	 * @return 转换后的值
	 * @see #getColumnValue(java.sql.ResultSet, int, Class)
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected Object convertValueToRequiredType(Object value, Class<?> requiredType) {
		if (String.class == requiredType) {
			return value.toString();
		}
		else if (Number.class.isAssignableFrom(requiredType)) {
			if (value instanceof Number) {
				// Convert original Number to target Number class.
				return NumberUtils.convertNumberToTargetClass(((Number) value), (Class<Number>) requiredType);
			}
			else {
				// Convert stringified value to target Number class.
				return NumberUtils.parseNumber(value.toString(),(Class<Number>) requiredType);
			}
		}
		else if (this.conversionService != null && this.conversionService.canConvert(value.getClass(), requiredType)) {
			return this.conversionService.convert(value, requiredType);
		}
		else {
			throw new IllegalArgumentException(
					"Value [" + value + "] is of type [" + value.getClass().getName() +
					"] and cannot be converted to required type [" + requiredType.getName() + "]");
		}
	}


	/**
	 * 静态工厂方法，创建一个新的{@code SingleColumnRowMapper}。
	 * @param requiredType 每个结果对象应匹配的类型
	 * @since 4.1
	 * @see #newInstance(Class, ConversionService)
	 */
	public static <T> SingleColumnRowMapper<T> newInstance(Class<T> requiredType) {
		return new SingleColumnRowMapper<>(requiredType);
	}

	/**
	 * 静态工厂方法，创建一个新的{@code SingleColumnRowMapper}。
	 * @param requiredType 每个结果对象应匹配的类型
	 * @param conversionService 用于转换所获取值的 {@link ConversionService}，
	 * 如果没有则为 {@code null}
	 * @since 5.0.4
	 * @see #newInstance(Class)
	 * @see #setConversionService
	 */
	public static <T> SingleColumnRowMapper<T> newInstance(
			Class<T> requiredType, @Nullable ConversionService conversionService) {

		SingleColumnRowMapper<T> rowMapper = newInstance(requiredType);
		rowMapper.setConversionService(conversionService);
		return rowMapper;
	}

}
