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
	 * @param requiredType 属性的类型
	 * @param propertyEditor 要注册的编辑器
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
	 * @param requiredType 属性的类型。
	 * 如果给出了属性，则可以为 {@code null}，
	 * 但无论如何都应指定，
	 * 尤其是在 Collection 的情况下——
	 * 以明确该编辑器是应用于整个 Collection 本身，还是应用于其每个元素。
	 * 因此一般规则是：
	 * <b>在 Collection/数组的情况下，不要在此指定 {@code null}！</b>
	 * @param propertyPath 属性的路径（名称或嵌套路径），
	 * 如果要为给定类型的所有属性注册编辑器则为 {@code null}
	 * @param propertyEditor 要注册的编辑器
	 */
	void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor);

	/**
	 * 为给定的类型和属性查找自定义属性编辑器。
	 * @param requiredType 属性的类型
	 * （如果给出了属性则可以为 {@code null}，
	 * 但无论如何都应指定，以便进行一致性检查）
	 * @param propertyPath 属性的路径（名称或嵌套路径），
	 * 如果要查找给定类型所有属性的编辑器则为 {@code null}
	 * @return 已注册的编辑器，如果没有则为 {@code null}
	 */
	@Nullable
	PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);

}
