/*
 * Copyright 2002-2012 the original author or authors.
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

import java.beans.PropertyEditor;

import org.springframework.lang.Nullable;

/**
 * 封装用于注册 JavaBeans {@link PropertyEditor PropertyEditors} 的方法。这是
 * {@link PropertyEditorRegistrar} 所操作的核心接口。
 *
 * <p>由 {@link BeanWrapper} 扩展；由 {@link BeanWrapperImpl} 和
 * {@link org.springframework.validation.DataBinder} 实现。
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see java.beans.PropertyEditor
 * @see PropertyEditorRegistrar
 * @see BeanWrapper
 * @see org.springframework.validation.DataBinder
 */
public interface PropertyEditorRegistry {

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器。
	 * @param requiredType the type of the property
	 * @param propertyEditor the editor to register
	 */
	void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

	/**
	 * 为给定的类型和属性注册给定的自定义属性编辑器，或为给定类型的所有属性注册该编辑器。
	 * <p>如果属性路径表示数组或 Collection 属性，则编辑器将应用于数组/Collection 本身
	 * （{@link PropertyEditor} 必须创建数组或 Collection 值）或应用于每个元素（{@code PropertyEditor} 必
	 * 须创建元素类型），具体取决于指定的必需类型。
	 * <p>注意：每个属性路径仅支持一个已注册的自定义编辑器。对于 Collection/数组，不要为同一属性既注册
	 * 集合/数组的编辑器又注册每个元素的编辑器。
	 * <p>例如，如果你想为“items[n].quantity”（针对所有 n 值）注册编辑器，则应将“items.quantity”作为此
	 * 方法 'propertyPath' 参数的值。
	 * @param requiredType the type of the property. This may be {@code null}
	 * if a property is given but should be specified in any case, in particular in
	 * case of a Collection - making clear whether the editor is supposed to apply
	 * to the entire Collection itself or to each of its entries. So as a general rule:
	 * <b>Do not specify {@code null} here in case of a Collection/array!</b>
	 * @param propertyPath the path of the property (name or nested path), or
	 * {@code null} if registering an editor for all properties of the given type
	 * @param propertyEditor editor to register
	 */
	void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor);

	/**
	 * 为给定的类型和属性查找自定义属性编辑器。
	 * @param requiredType the type of the property (can be {@code null} if a property
	 * is given but should be specified in any case for consistency checking)
	 * @param propertyPath the path of the property (name or nested path), or
	 * {@code null} if looking for an editor for all properties of the given type
	 * @return the registered editor, or {@code null} if none
	 */
	@Nullable
	PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);

}
