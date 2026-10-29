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

package org.springframework.jdbc.core.namedparam;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link SqlParameterSource} 实现，从给定 JavaBean 对象的 Bean 属性中获取参数值。
 * Bean 属性的名称必须与参数名称匹配。
 *
 * <p>底层使用 Spring 的 BeanWrapper 进行 Bean 属性访问。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see NamedParameterJdbcTemplate
 * @see org.springframework.beans.BeanWrapper
 */
public class BeanPropertySqlParameterSource extends AbstractSqlParameterSource {

	private final BeanWrapper beanWrapper;

	@Nullable
	private String[] propertyNames;


	/**
	 * 为给定的 Bean 创建新的 BeanPropertySqlParameterSource。
	 * @param object 要包装的 Bean 实例
	 */
	public BeanPropertySqlParameterSource(Object object) {
		this.beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
	}


	@Override
	public boolean hasValue(String paramName) {
		return this.beanWrapper.isReadableProperty(paramName);
	}

	@Override
	@Nullable
	public Object getValue(String paramName) throws IllegalArgumentException {
		try {
			return this.beanWrapper.getPropertyValue(paramName);
		}
		catch (NotReadablePropertyException ex) {
			throw new IllegalArgumentException(ex.getMessage());
		}
	}

	/**
	 * 从相应的属性类型推导默认的 SQL 类型。
	 * @see org.springframework.jdbc.core.StatementCreatorUtils#javaTypeToSqlParameterType
	 */
	@Override
	public int getSqlType(String paramName) {
		int sqlType = super.getSqlType(paramName);
		if (sqlType != TYPE_UNKNOWN) {
			return sqlType;
		}
		Class<?> propType = this.beanWrapper.getPropertyType(paramName);
		return StatementCreatorUtils.javaTypeToSqlParameterType(propType);
	}

	@Override
	@NonNull
	public String[] getParameterNames() {
		return getReadablePropertyNames();
	}

	/**
	 * 提供对所包装 Bean 的属性名称的访问。
	 * 使用 {@link PropertyAccessor} 接口提供的支持。
	 * @return 包含所有已知属性名称的数组
	 */
	public String[] getReadablePropertyNames() {
		if (this.propertyNames == null) {
			List<String> names = new ArrayList<>();
			PropertyDescriptor[] props = this.beanWrapper.getPropertyDescriptors();
			for (PropertyDescriptor pd : props) {
				if (this.beanWrapper.isReadableProperty(pd.getName())) {
					names.add(pd.getName());
				}
			}
			this.propertyNames = StringUtils.toStringArray(names);
		}
		return this.propertyNames;
	}

}
