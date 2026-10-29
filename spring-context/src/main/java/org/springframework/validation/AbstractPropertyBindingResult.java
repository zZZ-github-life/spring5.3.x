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

package org.springframework.validation;

import java.beans.PropertyEditor;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConvertingPropertyEditorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于配合 Spring 的 {@link org.springframework.beans.PropertyAccessor} 机制工作的
 * {@link BindingResult} 实现的抽象基类。通过委托给相应的 PropertyAccessor 方法预先实现字段访问。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #getPropertyAccessor()
 * @see org.springframework.beans.PropertyAccessor
 * @see org.springframework.beans.ConfigurablePropertyAccessor
 */
@SuppressWarnings("serial")
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {

	@Nullable
	private transient ConversionService conversionService;


	/**
	 * 创建新的 AbstractPropertyBindingResult 实例。
	 * @param objectName the name of the target object
	 * @see DefaultMessageCodesResolver
	 */
	protected AbstractPropertyBindingResult(String objectName) {
		super(objectName);
	}


	public void initConversion(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
		if (getTarget() != null) {
			getPropertyAccessor().setConversionService(conversionService);
		}
	}

	/**
	 * 返回底层的 PropertyAccessor。
	 * @see #getPropertyAccessor()
	 */
	@Override
	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return (getTarget() != null ? getPropertyAccessor() : null);
	}

	/**
	 * 返回规范属性名。
	 * @see org.springframework.beans.PropertyAccessorUtils#canonicalPropertyName
	 */
	@Override
	protected String canonicalFieldName(String field) {
		return PropertyAccessorUtils.canonicalPropertyName(field);
	}

	/**
	 * 根据属性类型确定字段类型。
	 * @see #getPropertyAccessor()
	 */
	@Override
	@Nullable
	public Class<?> getFieldType(@Nullable String field) {
		return (getTarget() != null ? getPropertyAccessor().getPropertyType(fixedField(field)) :
				super.getFieldType(field));
	}

	/**
	 * 从 PropertyAccessor 获取字段值。
	 * @see #getPropertyAccessor()
	 */
	@Override
	@Nullable
	protected Object getActualFieldValue(String field) {
		return getPropertyAccessor().getPropertyValue(field);
	}

	/**
	 * 根据已注册的 PropertyEditor 格式化字段值。
	 * @see #getCustomEditor
	 */
	@Override
	protected Object formatFieldValue(String field, @Nullable Object value) {
		String fixedField = fixedField(field);
		// Try custom editor...
		PropertyEditor customEditor = getCustomEditor(fixedField);
		if (customEditor != null) {
			customEditor.setValue(value);
			String textValue = customEditor.getAsText();
			// If the PropertyEditor returned null, there is no appropriate
			// text representation for this value: only use it if non-null.
			if (textValue != null) {
				return textValue;
			}
		}
		if (this.conversionService != null) {
			// Try custom converter...
			TypeDescriptor fieldDesc = getPropertyAccessor().getPropertyTypeDescriptor(fixedField);
			TypeDescriptor strDesc = TypeDescriptor.valueOf(String.class);
			if (fieldDesc != null && this.conversionService.canConvert(fieldDesc, strDesc)) {
				return this.conversionService.convert(value, fieldDesc, strDesc);
			}
		}
		return value;
	}

	/**
	 * 检索给定字段的自定义 PropertyEditor（如果有）。
	 * @param fixedField the fully qualified field name
	 * @return the custom PropertyEditor, or {@code null}
	 */
	@Nullable
	protected PropertyEditor getCustomEditor(String fixedField) {
		Class<?> targetType = getPropertyAccessor().getPropertyType(fixedField);
		PropertyEditor editor = getPropertyAccessor().findCustomEditor(targetType, fixedField);
		if (editor == null) {
			editor = BeanUtils.findEditorByConvention(targetType);
		}
		return editor;
	}

	/**
	 * 此实现为 Formatter 暴露一个 PropertyEditor 适配器（如果适用）。
	 */
	@Override
	@Nullable
	public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
		Class<?> valueTypeForLookup = valueType;
		if (valueTypeForLookup == null) {
			valueTypeForLookup = getFieldType(field);
		}
		PropertyEditor editor = super.findEditor(field, valueTypeForLookup);
		if (editor == null && this.conversionService != null) {
			TypeDescriptor td = null;
			if (field != null && getTarget() != null) {
				TypeDescriptor ptd = getPropertyAccessor().getPropertyTypeDescriptor(fixedField(field));
				if (ptd != null && (valueType == null || valueType.isAssignableFrom(ptd.getType()))) {
					td = ptd;
				}
			}
			if (td == null) {
				td = TypeDescriptor.valueOf(valueTypeForLookup);
			}
			if (this.conversionService.canConvert(TypeDescriptor.valueOf(String.class), td)) {
				editor = new ConvertingPropertyEditorAdapter(this.conversionService, td);
			}
		}
		return editor;
	}


	/**
	 * 根据具体的访问策略，提供要使用的 PropertyAccessor。<p>请注意，BindingResult 使用的
	 * PropertyAccessor 默认应始终将其 "extractOldValueForEditor" 标志设置为 "true"，因为对于作为数据绑
	 * 定目标的模型对象，这通常可以在没有副作用的情况下实现。
	 * @see ConfigurablePropertyAccessor#setExtractOldValueForEditor
	 */
	public abstract ConfigurablePropertyAccessor getPropertyAccessor();

}
