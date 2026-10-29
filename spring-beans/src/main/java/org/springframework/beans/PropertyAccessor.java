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

package org.springframework.beans;

import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * 可访问命名属性（例如对象的 Bean 属性或对象中的字段）的类的通用接口。
 *
 * <p>作为 {@link BeanWrapper} 的基础接口。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see BeanWrapper
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see PropertyAccessorFactory#forDirectFieldAccess
 */
public interface PropertyAccessor {

	/**
	 * 嵌套属性的路径分隔符。遵循常规 Java 约定：getFoo().getBar() 对应“foo.bar”。
	 */
	String NESTED_PROPERTY_SEPARATOR = ".";

	/**
	 * 嵌套属性的路径分隔符。遵循常规 Java 约定：getFoo().getBar() 对应“foo.bar”。
	 */
	char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

	/**
	 * 标记索引或映射属性（如“person.addresses[0]”）中属性键的起始位置。
	 */
	String PROPERTY_KEY_PREFIX = "[";

	/**
	 * 标记索引或映射属性（如“person.addresses[0]”）中属性键的起始位置。
	 */
	char PROPERTY_KEY_PREFIX_CHAR = '[';

	/**
	 * 标记索引或映射属性（如“person.addresses[0]”）中属性键的结束位置。
	 */
	String PROPERTY_KEY_SUFFIX = "]";

	/**
	 * 标记索引或映射属性（如“person.addresses[0]”）中属性键的结束位置。
	 */
	char PROPERTY_KEY_SUFFIX_CHAR = ']';


	/**
	 * 确定指定的属性是否可读。
	 * <p>如果属性不存在，则返回 {@code false}。
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return whether the property is readable
	 */
	boolean isReadableProperty(String propertyName);

	/**
	 * 确定指定的属性是否可写。
	 * <p>如果属性不存在，则返回 {@code false}。
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return whether the property is writable
	 */
	boolean isWritableProperty(String propertyName);

	/**
	 * 确定指定属性的属性类型，既可以检查属性描述符，也可以在索引或映射元素的情况下检查值。
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return the property type for the particular property,
	 * or {@code null} if not determinable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed
	 */
	@Nullable
	Class<?> getPropertyType(String propertyName) throws BeansException;

	/**
	 * 返回指定属性的类型描述符：优先来自读取方法，回退到写入方法。
	 * @param propertyName the property to check
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return the property type for the particular property,
	 * or {@code null} if not determinable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed
	 */
	@Nullable
	TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException;

	/**
	 * 获取指定属性的当前值。
	 * @param propertyName the name of the property to get the value of
	 * (may be a nested path and/or an indexed/mapped property)
	 * @return the value of the property
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't readable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed
	 */
	@Nullable
	Object getPropertyValue(String propertyName) throws BeansException;

	/**
	 * 将指定值设置为当前属性值。
	 * @param propertyName the name of the property to set the value of
	 * (may be a nested path and/or an indexed/mapped property)
	 * @param value the new value
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed or a type mismatch occurred
	 */
	void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

	/**
	 * 将指定值设置为当前属性值。
	 * @param pv an object containing the new property value
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyAccessException if the property was valid but the
	 * accessor method failed or a type mismatch occurred
	 */
	void setPropertyValue(PropertyValue pv) throws BeansException;

	/**
	 * 从 Map 执行批量更新。
	 * <p>从 PropertyValues 进行批量更新更强大：此方法仅为方便而提供。其行为与
	 * {@link #setPropertyValues(PropertyValues)} 方法完全相同。
	 * @param map a Map to take properties from. Contains property value objects,
	 * keyed by property name
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 */
	void setPropertyValues(Map<?, ?> map) throws BeansException;

	/**
	 * 执行批量更新的首选方式。
	 * <p>请注意，执行批量更新与执行单次更新不同：如果遇到 <b>可恢复</b>的错误（例如类型不匹配，但 <b>
	 * 不</b>包括无效的字段名之类），此类的实现会继续更新属性，并抛出包含所有单个错误的
	 * {@link PropertyBatchUpdateException}。之后可以检查此异常以查看所有绑定错误。已成功更新的属性保持
	 * 更改。
	 * <p>不允许未知字段或无效字段。
	 * @param pvs a PropertyValues to set on the target object
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs) throws BeansException;

	/**
	 * 对行为具有更多控制地执行批量更新。
	 * <p>请注意，执行批量更新与执行单次更新不同：如果遇到 <b>可恢复</b>的错误（例如类型不匹配，但 <b>
	 * 不</b>包括无效的字段名之类），此类的实现会继续更新属性，并抛出包含所有单个错误的
	 * {@link PropertyBatchUpdateException}。之后可以检查此异常以查看所有绑定错误。已成功更新的属性保持
	 * 更改。
	 * @param pvs a PropertyValues to set on the target object
	 * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown)
			throws BeansException;

	/**
	 * 对行为具有完全控制地执行批量更新。
	 * <p>请注意，执行批量更新与执行单次更新不同：如果遇到 <b>可恢复</b>的错误（例如类型不匹配，但 <b>
	 * 不</b>包括无效的字段名之类），此类的实现会继续更新属性，并抛出包含所有单个错误的
	 * {@link PropertyBatchUpdateException}。之后可以检查此异常以查看所有绑定错误。已成功更新的属性保持
	 * 更改。
	 * @param pvs a PropertyValues to set on the target object
	 * @param ignoreUnknown should we ignore unknown properties (not found in the bean)
	 * @param ignoreInvalid should we ignore invalid properties (found but not accessible)
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occurred for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException;

}
