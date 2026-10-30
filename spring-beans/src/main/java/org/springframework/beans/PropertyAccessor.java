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
	 * @param propertyName 要检查的属性
	 * （可以是嵌套路径和/或索引/映射属性）
	 * @return 该属性是否可读
	 */
	boolean isReadableProperty(String propertyName);

	/**
	 * 确定指定的属性是否可写。
	 * <p>如果属性不存在，则返回 {@code false}。
	 * @param propertyName 要检查的属性
	 * （可以是嵌套路径和/或索引/映射属性）
	 * @return 该属性是否可写
	 */
	boolean isWritableProperty(String propertyName);

	/**
	 * 确定指定属性的属性类型，既可以检查属性描述符，也可以在索引或映射元素的情况下检查值。
	 * @param propertyName 要检查的属性
	 * （可以是嵌套路径和/或索引/映射属性）
	 * @return 特定属性的属性类型，如果无法确定则为 {@code null}
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 */
	@Nullable
	Class<?> getPropertyType(String propertyName) throws BeansException;

	/**
	 * 返回指定属性的类型描述符：优先来自读取方法，回退到写入方法。
	 * @param propertyName 要检查的属性
	 * （可以是嵌套路径和/或索引/映射属性）
	 * @return 特定属性的属性类型，如果无法确定则为 {@code null}
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 */
	@Nullable
	TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException;

	/**
	 * 获取指定属性的当前值。
	 * @param propertyName 要获取其值的属性名称
	 * （可以是嵌套路径和/或索引/映射属性）
	 * @return 该属性的值
	 * @throws InvalidPropertyException 如果不存在这样的属性，
	 * 或者该属性不可读
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 */
	@Nullable
	Object getPropertyValue(String propertyName) throws BeansException;

	/**
	 * 将指定值设置为当前属性值。
	 * @param propertyName 要设置其值的属性名称
	 * （可以是嵌套路径和/或索引/映射属性）
	 * @param value 新值
	 * @throws InvalidPropertyException 如果不存在这样的属性，
	 * 或者该属性不可写
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 * 或发生了类型不匹配
	 */
	void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

	/**
	 * 将指定值设置为当前属性值。
	 * @param pv 包含新属性值的对象
	 * @throws InvalidPropertyException 如果不存在这样的属性，
	 * 或者该属性不可写
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 * 或发生了类型不匹配
	 */
	void setPropertyValue(PropertyValue pv) throws BeansException;

	/**
	 * 从 Map 执行批量更新。
	 * <p>从 PropertyValues 进行批量更新更强大：此方法仅为方便而提供。其行为与
	 * {@link #setPropertyValues(PropertyValues)} 方法完全相同。
	 * @param map 从中获取属性的 Map。
	 * 包含以属性名为键的属性值对象
	 * @throws InvalidPropertyException 如果不存在这样的属性，
	 * 或者该属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间特定属性发生了一个或多个 PropertyAccessException。
	 * 该异常会捆绑所有单独的 PropertyAccessException。
	 * 所有其他属性都将已成功更新。
	 */
	void setPropertyValues(Map<?, ?> map) throws BeansException;

	/**
	 * 执行批量更新的首选方式。
	 * <p>请注意，执行批量更新与执行单次更新不同：如果遇到 <b>可恢复</b>的错误（例如类型不匹配，但 <b>
	 * 不</b>包括无效的字段名之类），此类的实现会继续更新属性，并抛出包含所有单个错误的
	 * {@link PropertyBatchUpdateException}。之后可以检查此异常以查看所有绑定错误。已成功更新的属性保持
	 * 更改。
	 * <p>不允许未知字段或无效字段。
	 * @param pvs 要在目标对象上设置的 PropertyValues
	 * @throws InvalidPropertyException 如果不存在这样的属性，
	 * 或者该属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间特定属性发生了一个或多个 PropertyAccessException。
	 * 该异常会捆绑所有单独的 PropertyAccessException。
	 * 所有其他属性都将已成功更新。
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs) throws BeansException;

	/**
	 * 对行为具有更多控制地执行批量更新。
	 * <p>请注意，执行批量更新与执行单次更新不同：如果遇到 <b>可恢复</b>的错误（例如类型不匹配，但 <b>
	 * 不</b>包括无效的字段名之类），此类的实现会继续更新属性，并抛出包含所有单个错误的
	 * {@link PropertyBatchUpdateException}。之后可以检查此异常以查看所有绑定错误。已成功更新的属性保持
	 * 更改。
	 * @param pvs 要在目标对象上设置的 PropertyValues
	 * @param ignoreUnknown 是否忽略未知属性（在 Bean 中未找到的属性）
	 * @throws InvalidPropertyException 如果不存在这样的属性，
	 * 或者该属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间特定属性发生了一个或多个 PropertyAccessException。
	 * 该异常会捆绑所有单独的 PropertyAccessException。
	 * 所有其他属性都将已成功更新。
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
	 * @param pvs 要在目标对象上设置的 PropertyValues
	 * @param ignoreUnknown 是否忽略未知属性（在 Bean 中未找到的属性）
	 * @param ignoreInvalid 是否忽略无效属性（已找到但无法访问的属性）
	 * @throws InvalidPropertyException 如果不存在这样的属性，
	 * 或者该属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间特定属性发生了一个或多个 PropertyAccessException。
	 * 该异常会捆绑所有单独的 PropertyAccessException。
	 * 所有其他属性都将已成功更新。
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException;

}
