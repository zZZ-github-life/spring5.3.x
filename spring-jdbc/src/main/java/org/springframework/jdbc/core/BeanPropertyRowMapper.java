/*
 * Copyright 2002-2023 the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link RowMapper}实现，将一行数据转换为指定映射目标类的新实例。
 * 映射目标类必须是顶级类或{@code static}内部类，并且必须具有默认构造函数或无参构造函数。
 *
 * <p>列值通过列名（从结果集元数据中获取）与目标类中对应属性的public setter方法进行匹配来映射。
 * 名称可以直接匹配，也可以将通过下划线分隔的名称转换为"驼峰"命名后进行匹配。
 *
 * <p>为目标类中许多常见类型的属性提供了映射支持 &mdash;
 * 例如：String、boolean、Boolean、byte、Byte、short、Short、int、Integer、
 * long、Long、float、Float、double、Double、BigDecimal、{@code java.util.Date}等。
 *
 * <p>为了便于在名称不匹配的列和属性之间进行映射，可以尝试在SQL语句中使用列别名，例如
 * {@code "select fname as first_name from customer"}，其中{@code first_name}
 * 可以映射到目标类中的{@code setFirstName(String)}方法。
 *
 * <p>对于从数据库读取的{@code NULL}值，将尝试使用{@code null}调用对应的setter方法，
 * 但对于Java基本类型，这将默认导致{@link TypeMismatchException}。
 * 要忽略目标类中所有基本类型属性的{@code NULL}数据库值，
 * 请将{@code primitivesDefaultedForNullValue}标志设置为{@code true}。
 * 详情请参见{@link #setPrimitivesDefaultedForNullValue(boolean)}。
 *
 * <p如果需要映射到具有<em>数据类</em>构造函数的目标类 &mdash;
 * 例如Java {@code record}或Kotlin {@code data}类 &mdash;
 * 请改用{@link DataClassRowMapper}。
 *
 * <p请注意，此类旨在提供便利性而非高性能。如需最佳性能，
 * 请考虑使用自定义的{@code RowMapper}实现。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @param <T> 结果类型
 * @see DataClassRowMapper
 */
public class BeanPropertyRowMapper<T> implements RowMapper<T> {

	/** 子类可用的Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 我们要映射到的类。 */
	@Nullable
	private Class<T> mappedClass;

	/** 是否进行严格验证。 */
	private boolean checkFullyPopulated = false;

	/**
	 * 目标类中的基本类型属性是否应忽略{@code NULL}数据库值。
	 * @see #setPrimitivesDefaultedForNullValue(boolean)
	 */
	private boolean primitivesDefaultedForNullValue = false;

	/** 用于将JDBC值绑定到bean属性的ConversionService。 */
	@Nullable
	private ConversionService conversionService = DefaultConversionService.getSharedInstance();

	/** 我们提供映射的属性Map。 */
	@Nullable
	private Map<String, PropertyDescriptor> mappedProperties;

	/** 我们提供映射的bean属性名称集合。 */
	@Nullable
	private Set<String> mappedPropertyNames;


	/**
	 * 创建一个新的{@code BeanPropertyRowMapper}，用于bean风格的配置。
	 * @see #setMappedClass
	 * @see #setCheckFullyPopulated
	 */
	public BeanPropertyRowMapper() {
	}

	/**
	 * 创建一个新的{@code BeanPropertyRowMapper}，接受目标bean中未填充的属性。
	 * @param mappedClass 每行应映射到的类
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass) {
		initialize(mappedClass);
	}

	/**
	 * 创建一个新的{@code BeanPropertyRowMapper}。
	 * @param mappedClass 每行应映射到的类
	 * @param checkFullyPopulated 是否严格校验所有 Bean 属性都已从对应的数据库列映射
	 */
	public BeanPropertyRowMapper(Class<T> mappedClass, boolean checkFullyPopulated) {
		initialize(mappedClass);
		this.checkFullyPopulated = checkFullyPopulated;
	}


	/**
	 * 设置每行应映射到的类。
	 */
	public void setMappedClass(Class<T> mappedClass) {
		if (this.mappedClass == null) {
			initialize(mappedClass);
		}
		else {
			if (this.mappedClass != mappedClass) {
				throw new InvalidDataAccessApiUsageException("The mapped class can not be reassigned to map to " +
						mappedClass + " since it is already providing mapping for " + this.mappedClass);
			}
		}
	}

	/**
	 * 获取我们要映射到的类。
	 */
	@Nullable
	public final Class<T> getMappedClass() {
		return this.mappedClass;
	}

	/**
	 * 设置是否严格验证所有bean属性都已从对应的数据库列映射。
	 * <p>默认为{@code false}，接受目标bean中未填充的属性。
	 */
	public void setCheckFullyPopulated(boolean checkFullyPopulated) {
		this.checkFullyPopulated = checkFullyPopulated;
	}

	/**
	 * 返回是否严格验证所有bean属性都已从对应的数据库列映射。
	 */
	public boolean isCheckFullyPopulated() {
		return this.checkFullyPopulated;
	}

	/**
	 * 设置映射到目标类中对应基本类型属性时，是否忽略{@code NULL}数据库列值。
	 * <p>默认为{@code false}，当null映射到Java基本类型时抛出异常。
	 * <p>如果将此标志设置为{@code true}，并且使用映射bean中被<em>忽略</em>的
	 * 基本类型属性值来更新数据库，则数据库中的值将从{@code NULL}更改为该基本类型属性的当前值。
	 * 该值可能是属性的初始值（可能是Java对应基本类型的默认值），
	 * 也可能是默认构造函数（或初始化块）中为该属性设置的某些值，
	 * 或者是设置映射bean中其他属性时产生的副作用值。
	 */
	public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
		this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
	}

	/**
	 * 获取{@code primitivesDefaultedForNullValue}标志的值。
	 * @see #setPrimitivesDefaultedForNullValue(boolean)
	 */
	public boolean isPrimitivesDefaultedForNullValue() {
		return this.primitivesDefaultedForNullValue;
	}

	/**
	 * 设置用于将JDBC值绑定到bean属性的{@link ConversionService}，
	 * 或设置为{@code null}表示不使用。
	 * <p>从Spring 4.3开始，默认为{@link DefaultConversionService}。
	 * 它提供了对{@code java.time}转换和其他特殊类型的支持。
	 * @since 4.3
	 * @see #initBeanWrapper(BeanWrapper)
	 */
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回用于将JDBC值绑定到bean属性的{@link ConversionService}，
	 * 如果没有则返回{@code null}。
	 * @since 4.3
	 */
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	/**
	 * 初始化给定类的映射元数据。
	 * @param mappedClass 映射的类
	 */
	protected void initialize(Class<T> mappedClass) {
		this.mappedClass = mappedClass;
		this.mappedProperties = new HashMap<>();
		this.mappedPropertyNames = new HashSet<>();

		for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(mappedClass)) {
			if (pd.getWriteMethod() != null) {
				String lowerCaseName = lowerCaseName(pd.getName());
				this.mappedProperties.put(lowerCaseName, pd);
				String underscoreName = underscoreName(pd.getName());
				if (!lowerCaseName.equals(underscoreName)) {
					this.mappedProperties.put(underscoreName, pd);
				}
				this.mappedPropertyNames.add(pd.getName());
			}
		}
	}

	/**
	 * 从映射的属性中移除指定的属性。
	 * @param propertyName 属性名称（如属性描述符所使用的）
	 * @since 5.3.9
	 */
	protected void suppressProperty(String propertyName) {
		if (this.mappedProperties != null) {
			this.mappedProperties.remove(lowerCaseName(propertyName));
			this.mappedProperties.remove(underscoreName(propertyName));
		}
	}

	/**
	 * 将给定名称转换为小写。
	 * 默认情况下，转换在US语言环境中进行。
	 * @param name 原始名称
	 * @return 转换后的名称
	 * @since 4.2
	 */
	protected String lowerCaseName(String name) {
		return name.toLowerCase(Locale.US);
	}

	/**
	 * 将驼峰命名转换为小写的下划线命名。
	 * 任何大写字母都将转换为小写，并在前面加上下划线。
	 * @param name 原始名称
	 * @return 转换后的名称
	 * @since 4.2
	 * @see #lowerCaseName
	 */
	protected String underscoreName(String name) {
		if (!StringUtils.hasLength(name)) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		result.append(Character.toLowerCase(name.charAt(0)));
		for (int i = 1; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				result.append('_').append(Character.toLowerCase(c));
			}
			else {
				result.append(c);
			}
		}
		return result.toString();
	}


	/**
	 * 提取当前行中所有列的值。
	 * <p>利用public setter方法和结果集元数据。
	 * @see java.sql.ResultSetMetaData
	 */
	@Override
	public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
		BeanWrapperImpl bw = new BeanWrapperImpl();
		initBeanWrapper(bw);

		T mappedObject = constructMappedInstance(rs, bw);
		bw.setBeanInstance(mappedObject);

		ResultSetMetaData rsmd = rs.getMetaData();
		int columnCount = rsmd.getColumnCount();
		Set<String> populatedProperties = (isCheckFullyPopulated() ? new HashSet<>() : null);

		for (int index = 1; index <= columnCount; index++) {
			String column = JdbcUtils.lookupColumnName(rsmd, index);
			String property = lowerCaseName(StringUtils.delete(column, " "));
			PropertyDescriptor pd = (this.mappedProperties != null ? this.mappedProperties.get(property) : null);
			if (pd != null) {
				try {
					Object value = getColumnValue(rs, index, pd);
					if (rowNumber == 0 && logger.isDebugEnabled()) {
						logger.debug("Mapping column '" + column + "' to property '" + pd.getName() +
								"' of type '" + ClassUtils.getQualifiedName(pd.getPropertyType()) + "'");
					}
					try {
						bw.setPropertyValue(pd.getName(), value);
					}
					catch (TypeMismatchException ex) {
						if (value == null && this.primitivesDefaultedForNullValue) {
							if (logger.isDebugEnabled()) {
								String propertyType = ClassUtils.getQualifiedName(pd.getPropertyType());
								logger.debug(String.format(
										"Ignoring intercepted TypeMismatchException for row %d and column '%s' " +
										"with null value when setting property '%s' of type '%s' on object: %s",
										rowNumber, column, pd.getName(), propertyType, mappedObject), ex);
							}
						}
						else {
							throw ex;
						}
					}
					if (populatedProperties != null) {
						populatedProperties.add(pd.getName());
					}
				}
				catch (NotWritablePropertyException ex) {
					throw new DataRetrievalFailureException(
							"Unable to map column '" + column + "' to property '" + pd.getName() + "'", ex);
				}
			}
		}

		if (populatedProperties != null && !populatedProperties.equals(this.mappedPropertyNames)) {
			throw new InvalidDataAccessApiUsageException("Given ResultSet does not contain all properties " +
					"necessary to populate object of " + this.mappedClass + ": " + this.mappedPropertyNames);
		}

		return mappedObject;
	}

	/**
	 * 为当前行构造映射类的实例。
	 * @param rs 要映射的 ResultSet（已为当前行预先初始化）
	 * @param tc 使用此 RowMapper 的转换服务的 TypeConverter
	 * @return 映射类的相应实例
	 * @throws SQLException 如果遇到 SQLException
	 * @since 5.3
	 */
	protected T constructMappedInstance(ResultSet rs, TypeConverter tc) throws SQLException  {
		Assert.state(this.mappedClass != null, "Mapped class was not specified");
		return BeanUtils.instantiateClass(this.mappedClass);
	}

	/**
	 * 初始化给定的BeanWrapper，用于行映射。
	 * 每行调用时都需要调用此方法。
	 * <p>默认实现会应用配置的{@link ConversionService}（如果有的话）。
	 * 子类可以重写此方法。
	 * @param bw 要初始化的 BeanWrapper
	 * @see #getConversionService()
	 * @see BeanWrapper#setConversionService
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		ConversionService cs = getConversionService();
		if (cs != null) {
			bw.setConversionService(cs);
		}
	}

	/**
	 * 检索指定列的JDBC对象值。
	 * <p>默认实现委托给{@link #getColumnValue(ResultSet, int, Class)}。
	 * @param rs 保存数据的 ResultSet
	 * @param index 列索引
	 * @param pd 每个结果对象预期匹配的 Bean 属性
	 * @return Object 值
	 * @throws SQLException 如果提取失败
	 * @see #getColumnValue(ResultSet, int, Class)
	 */
	@Nullable
	protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
	}

	/**
	 * 检索指定列的JDBC对象值。
	 * <p>默认实现调用{@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}。
	 * 子类可以重写此方法以预先检查特定的值类型，
	 * 或对从{@code getResultSetValue}返回的值进行后处理。
	 * @param rs 保存数据的 ResultSet
	 * @param index 列索引
	 * @param paramType 目标参数类型
	 * @return Object 值
	 * @throws SQLException 如果提取失败
	 * @since 5.3
	 * @see org.springframework.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)
	 */
	@Nullable
	protected Object getColumnValue(ResultSet rs, int index, Class<?> paramType) throws SQLException {
		return JdbcUtils.getResultSetValue(rs, index, paramType);
	}


	/**
	 * 静态工厂方法，创建一个新的{@code BeanPropertyRowMapper}。
	 * @param mappedClass 每行应映射到的类
	 * @see #newInstance(Class, ConversionService)
	 */
	public static <T> BeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
		return new BeanPropertyRowMapper<>(mappedClass);
	}

	/**
	 * 静态工厂方法，创建一个新的{@code BeanPropertyRowMapper}。
	 * @param mappedClass 每行应映射到的类
	 * @param conversionService 用于将 JDBC 值绑定到 Bean 属性的 {@link ConversionService}；
	 * 如果没有则为 {@code null}
	 * @since 5.2.3
	 * @see #newInstance(Class)
	 * @see #setConversionService
	 */
	public static <T> BeanPropertyRowMapper<T> newInstance(
			Class<T> mappedClass, @Nullable ConversionService conversionService) {

		BeanPropertyRowMapper<T> rowMapper = newInstance(mappedClass);
		rowMapper.setConversionService(conversionService);
		return rowMapper;
	}

}
