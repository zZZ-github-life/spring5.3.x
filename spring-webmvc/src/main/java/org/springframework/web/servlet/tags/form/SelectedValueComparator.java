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

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.support.BindStatus;

/**
 * 用于检测候选值是否匹配 {@link BindStatus#getValue 数据绑定值} 的工具类。它会积极地尝试通过多种途径证明二者相等，以处理实例不相等、逻辑（基于 String 表示）相等以及基于 {@link PropertyEditor} 的比较等问题。<p>完全支持比较数组、{@link Collection Collections} 和 {@link Map Maps}。<p><h1><a name="equality-contract">相等性约定</a></h1>对于单值对象，首先使用标准的 {@link Object#equals Java 相等性} 进行检测。因此，用户代码应尽量实现 {@link Object#equals} 以加快比较过程。如果 {@link Object#equals} 返回 {@code false}，则尝试进行 {@link #exhaustiveCompare 穷举比较}，目标是<strong>证明</strong>相等，而不是证明不相等。<p>接下来，尝试比较候选值和绑定值的 {@code String} 表示。由于展示给用户时两个值都会表示为 {@code String}，因此在某些情况下这可能返回 {@code true}。<p>接下来，如果候选值是 {@code String}，则尝试将绑定值与对候选值应用相应 {@link PropertyEditor} 后的结果进行比较。该比较可能执行两次：先直接针对 {@code String} 实例，如果第一次比较结果为 {@code false}，再针对 {@code String} 表示进行比较。
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
abstract class SelectedValueComparator {

	/**
	 * 如果给定的候选值与绑定到给定 {@link BindStatus} 的值相等，则返回 {@code true}。此处的相等性不同于标准 Java 相等性，详细信息请参见<a href="#equality-contract">此处</a>。
	 */
	public static boolean isSelected(BindStatus bindStatus, @Nullable Object candidateValue) {
		// Check obvious equality matches with the candidate first,
		// both with the rendered value and with the original value.
		Object boundValue = bindStatus.getValue();
		if (ObjectUtils.nullSafeEquals(boundValue, candidateValue)) {
			return true;
		}
		Object actualValue = bindStatus.getActualValue();
		if (actualValue != null && actualValue != boundValue &&
				ObjectUtils.nullSafeEquals(actualValue, candidateValue)) {
			return true;
		}
		if (actualValue != null) {
			boundValue = actualValue;
		}
		else if (boundValue == null) {
			return false;
		}

		// Non-null value but no obvious equality with the candidate value:
		// go into more exhaustive comparisons.
		boolean selected = false;
		if (candidateValue != null) {
			if (boundValue.getClass().isArray()) {
				selected = collectionCompare(CollectionUtils.arrayToList(boundValue), candidateValue, bindStatus);
			}
			else if (boundValue instanceof Collection) {
				selected = collectionCompare((Collection<?>) boundValue, candidateValue, bindStatus);
			}
			else if (boundValue instanceof Map) {
				selected = mapCompare((Map<?, ?>) boundValue, candidateValue, bindStatus);
			}
		}
		if (!selected) {
			selected = exhaustiveCompare(boundValue, candidateValue, bindStatus.getEditor(), null);
		}
		return selected;
	}

	private static boolean collectionCompare(
			Collection<?> boundCollection, Object candidateValue, BindStatus bindStatus) {
		try {
			if (boundCollection.contains(candidateValue)) {
				return true;
			}
		}
		catch (ClassCastException ex) {
			// Probably from a TreeSet - ignore.
		}
		return exhaustiveCollectionCompare(boundCollection, candidateValue, bindStatus);
	}

	private static boolean mapCompare(Map<?, ?> boundMap, Object candidateValue, BindStatus bindStatus) {
		try {
			if (boundMap.containsKey(candidateValue)) {
				return true;
			}
		}
		catch (ClassCastException ex) {
			// Probably from a TreeMap - ignore.
		}
		return exhaustiveCollectionCompare(boundMap.keySet(), candidateValue, bindStatus);
	}

	private static boolean exhaustiveCollectionCompare(
			Collection<?> collection, Object candidateValue, BindStatus bindStatus) {

		Map<PropertyEditor, Object> convertedValueCache = new HashMap<>();
		PropertyEditor editor = null;
		boolean candidateIsString = (candidateValue instanceof String);
		if (!candidateIsString) {
			editor = bindStatus.findEditor(candidateValue.getClass());
		}
		for (Object element : collection) {
			if (editor == null && element != null && candidateIsString) {
				editor = bindStatus.findEditor(element.getClass());
			}
			if (exhaustiveCompare(element, candidateValue, editor, convertedValueCache)) {
				return true;
			}
		}
		return false;
	}

	private static boolean exhaustiveCompare(@Nullable Object boundValue, @Nullable Object candidate,
			@Nullable PropertyEditor editor, @Nullable Map<PropertyEditor, Object> convertedValueCache) {

		String candidateDisplayString = ValueFormatter.getDisplayString(candidate, editor, false);
		if (boundValue != null && boundValue.getClass().isEnum()) {
			Enum<?> boundEnum = (Enum<?>) boundValue;
			String enumCodeAsString = ObjectUtils.getDisplayString(boundEnum.name());
			if (enumCodeAsString.equals(candidateDisplayString)) {
				return true;
			}
			String enumLabelAsString = ObjectUtils.getDisplayString(boundEnum.toString());
			if (enumLabelAsString.equals(candidateDisplayString)) {
				return true;
			}
		}
		else if (ObjectUtils.getDisplayString(boundValue).equals(candidateDisplayString)) {
			return true;
		}

		if (editor != null && candidate instanceof String) {
			// Try PE-based comparison (PE should *not* be allowed to escape creating thread)
			String candidateAsString = (String) candidate;
			Object candidateAsValue;
			if (convertedValueCache != null && convertedValueCache.containsKey(editor)) {
				candidateAsValue = convertedValueCache.get(editor);
			}
			else {
				editor.setAsText(candidateAsString);
				candidateAsValue = editor.getValue();
				if (convertedValueCache != null) {
					convertedValueCache.put(editor, candidateAsValue);
				}
			}
			if (ObjectUtils.nullSafeEquals(boundValue, candidateAsValue)) {
				return true;
			}
		}
		return false;
	}

}
