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

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link RowMapper}实现，将一行数据转换为指定映射目标类的新实例。
 * 映射目标类必须是顶级类或{@code static}内部类，可以暴露具有与列名对应的
 * 命名参数的<em>数据类</em>构造函数，或者暴露具有与列名对应的属性名的
 * 经典bean属性setter方法（甚至可以是两者的组合）。
 *
 * <p>"数据类"术语适用于Java <em>record</em>、Kotlin <em>data class</em>，
 * 以及任何具有命名参数构造函数的类，这些参数旨在映射到对应的列名。
 *
 * <p>当将数据类构造函数与setter方法结合使用时，通过构造函数参数成功映射的任何属性
 * 不会再通过对应的setter方法进行额外映射。这意味着构造函数参数优先于属性setter方法。
 *
 * <p>请注意，此类继承自{@link BeanPropertyRowMapper}，因此可以作为任何映射目标类的通用选择，
 * 灵活地适配映射类中的构造函数风格或setter方法。
 *
 * <p>请注意，此类旨在提供便利性而非高性能。如需最佳性能，
 * 请考虑使用自定义的{@code RowMapper}实现。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.3
 * @param <T> 结果类型
 */
public class DataClassRowMapper<T> extends BeanPropertyRowMapper<T> {

	@Nullable
	private Constructor<T> mappedConstructor;

	@Nullable
	private String[] constructorParameterNames;

	@Nullable
	private TypeDescriptor[] constructorParameterTypes;


	/**
	 * 创建一个新的{@code DataClassRowMapper}，用于bean风格的配置。
	 * @see #setMappedClass
	 * @see #setConversionService
	 */
	public DataClassRowMapper() {
	}

	/**
	 * 创建一个新的{@code DataClassRowMapper}。
	 * @param mappedClass 每行应映射到的类
	 */
	public DataClassRowMapper(Class<T> mappedClass) {
		super(mappedClass);
	}


	@Override
	protected void initialize(Class<T> mappedClass) {
		super.initialize(mappedClass);

		this.mappedConstructor = BeanUtils.getResolvableConstructor(mappedClass);
		int paramCount = this.mappedConstructor.getParameterCount();
		if (paramCount > 0) {
			this.constructorParameterNames = BeanUtils.getParameterNames(this.mappedConstructor);
			for (String name : this.constructorParameterNames) {
				suppressProperty(name);
			}
			this.constructorParameterTypes = new TypeDescriptor[paramCount];
			for (int i = 0; i < paramCount; i++) {
				this.constructorParameterTypes[i] = new TypeDescriptor(new MethodParameter(this.mappedConstructor, i));
			}
		}
	}

	@Override
	protected T constructMappedInstance(ResultSet rs, TypeConverter tc) throws SQLException  {
		Assert.state(this.mappedConstructor != null, "Mapped constructor was not initialized");

		Object[] args;
		if (this.constructorParameterNames != null && this.constructorParameterTypes != null) {
			args = new Object[this.constructorParameterNames.length];
			for (int i = 0; i < args.length; i++) {
				String name = this.constructorParameterNames[i];
				int index;
				try {
					// Try direct name match first
					index = rs.findColumn(lowerCaseName(name));
				}
				catch (SQLException ex) {
					// Try underscored name match instead
					index = rs.findColumn(underscoreName(name));
				}
				TypeDescriptor td = this.constructorParameterTypes[i];
				Object value = getColumnValue(rs, index, td.getType());
				args[i] = tc.convertIfNecessary(value, td.getType(), td);
			}
		}
		else {
			args = new Object[0];
		}

		return BeanUtils.instantiateClass(this.mappedConstructor, args);
	}


	/**
	 * 静态工厂方法，创建一个新的{@code DataClassRowMapper}。
	 * @param mappedClass 每行应映射到的类
	 * @see #newInstance(Class, ConversionService)
	 */
	public static <T> DataClassRowMapper<T> newInstance(Class<T> mappedClass) {
		return new DataClassRowMapper<>(mappedClass);
	}

	/**
	 * 静态工厂方法，创建一个新的{@code DataClassRowMapper}。
	 * @param mappedClass 每行应映射到的类
	 * @param conversionService 用于将 JDBC 值绑定到 Bean 属性的 {@link ConversionService}；
	 * 如果没有则为 {@code null}
	 * @see #newInstance(Class)
	 * @see #setConversionService
	 */
	public static <T> DataClassRowMapper<T> newInstance(
			Class<T> mappedClass, @Nullable ConversionService conversionService) {

		DataClassRowMapper<T> rowMapper = newInstance(mappedClass);
		rowMapper.setConversionService(conversionService);
		return rowMapper;
	}

}
