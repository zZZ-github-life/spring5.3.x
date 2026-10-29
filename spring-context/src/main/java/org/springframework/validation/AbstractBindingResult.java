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

package org.springframework.validation;

import java.beans.PropertyEditor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link BindingResult} 接口及其父接口 {@link Errors} 的抽象实现。封装
 * {@link ObjectError ObjectErrors} 和 {@link FieldError FieldErrors} 的通用管理。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.0
 * @see Errors
 */
@SuppressWarnings("serial")
public abstract class AbstractBindingResult extends AbstractErrors implements BindingResult, Serializable {

	private final String objectName;

	private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

	private final List<ObjectError> errors = new ArrayList<>();

	private final Map<String, Class<?>> fieldTypes = new HashMap<>();

	private final Map<String, Object> fieldValues = new HashMap<>();

	private final Set<String> suppressedFields = new HashSet<>();


	/**
	 * 创建新的 AbstractBindingResult 实例。
	 * @param objectName the name of the target object
	 * @see DefaultMessageCodesResolver
	 */
	protected AbstractBindingResult(String objectName) {
		this.objectName = objectName;
	}


	/**
	 * 设置用于将错误解析为消息代码的策略。默认是 DefaultMessageCodesResolver。
	 * @see DefaultMessageCodesResolver
	 */
	public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		Assert.notNull(messageCodesResolver, "MessageCodesResolver must not be null");
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * 返回用于将错误解析为消息代码的策略。
	 */
	public MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}


	//---------------------------------------------------------------------
	// Implementation of the Errors interface
	//---------------------------------------------------------------------

	@Override
	public String getObjectName() {
		return this.objectName;
	}

	@Override
	public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
		addError(new ObjectError(getObjectName(), resolveMessageCodes(errorCode), errorArgs, defaultMessage));
	}

	@Override
	public void rejectValue(@Nullable String field, String errorCode, @Nullable Object[] errorArgs,
			@Nullable String defaultMessage) {

		if (!StringUtils.hasLength(getNestedPath()) && !StringUtils.hasLength(field)) {
			// We're at the top of the nested object hierarchy,
			// so the present level is not a field but rather the top object.
			// The best we can do is register a global error here...
			reject(errorCode, errorArgs, defaultMessage);
			return;
		}

		String fixedField = fixedField(field);
		Object newVal = getActualFieldValue(fixedField);
		FieldError fe = new FieldError(getObjectName(), fixedField, newVal, false,
				resolveMessageCodes(errorCode, field), errorArgs, defaultMessage);
		addError(fe);
	}

	@Override
	public void addAllErrors(Errors errors) {
		if (!errors.getObjectName().equals(getObjectName())) {
			throw new IllegalArgumentException("Errors object needs to have same object name");
		}
		this.errors.addAll(errors.getAllErrors());
	}

	@Override
	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	@Override
	public int getErrorCount() {
		return this.errors.size();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return Collections.unmodifiableList(this.errors);
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		List<ObjectError> result = new ArrayList<>();
		for (ObjectError objectError : this.errors) {
			if (!(objectError instanceof FieldError)) {
				result.add(objectError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	@Nullable
	public ObjectError getGlobalError() {
		for (ObjectError objectError : this.errors) {
			if (!(objectError instanceof FieldError)) {
				return objectError;
			}
		}
		return null;
	}

	@Override
	public List<FieldError> getFieldErrors() {
		List<FieldError> result = new ArrayList<>();
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError) {
				result.add((FieldError) objectError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	@Nullable
	public FieldError getFieldError() {
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError) {
				return (FieldError) objectError;
			}
		}
		return null;
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		List<FieldError> result = new ArrayList<>();
		String fixedField = fixedField(field);
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError && isMatchingFieldError(fixedField, (FieldError) objectError)) {
				result.add((FieldError) objectError);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	@Nullable
	public FieldError getFieldError(String field) {
		String fixedField = fixedField(field);
		for (ObjectError objectError : this.errors) {
			if (objectError instanceof FieldError) {
				FieldError fieldError = (FieldError) objectError;
				if (isMatchingFieldError(fixedField, fieldError)) {
					return fieldError;
				}
			}
		}
		return null;
	}

	@Override
	@Nullable
	public Object getFieldValue(String field) {
		FieldError fieldError = getFieldError(field);
		// Use rejected value in case of error, current field value otherwise.
		if (fieldError != null) {
			Object value = fieldError.getRejectedValue();
			// Do not apply formatting on binding failures like type mismatches.
			return (fieldError.isBindingFailure() || getTarget() == null ? value : formatFieldValue(field, value));
		}
		else if (getTarget() != null) {
			Object value = getActualFieldValue(fixedField(field));
			return formatFieldValue(field, value);
		}
		else {
			return this.fieldValues.get(field);
		}
	}

	/**
	 * 此默认实现根据实际字段值（如果有）确定类型。子类应重写此方法，以便从描述符中确定类型，即使值为
	 * {@code null} 也是如此。
	 * @see #getActualFieldValue
	 */
	@Override
	@Nullable
	public Class<?> getFieldType(@Nullable String field) {
		if (getTarget() != null) {
			Object value = getActualFieldValue(fixedField(field));
			if (value != null) {
				return value.getClass();
			}
		}
		return this.fieldTypes.get(field);
	}


	//---------------------------------------------------------------------
	// Implementation of BindingResult interface
	//---------------------------------------------------------------------

	/**
	 * 为所获得的状态返回一个模型 Map，将 Errors 实例暴露为 '{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX}
	 * + objectName'，并暴露对象本身。<p>请注意，每次调用此方法都会构造该 Map。向 Map 中添加内容后再重
	 * 新调用此方法将不起作用。<p>此方法返回的模型 Map 中的属性通常会被包含在使用 Spring 的 bind 标签的
	 * 表单视图的 ModelAndView 中，该视图需要访问 Errors 实例。
	 * @see #getObjectName
	 * @see #MODEL_KEY_PREFIX
	 */
	@Override
	public Map<String, Object> getModel() {
		Map<String, Object> model = new LinkedHashMap<>(2);
		// Mapping from name to target object.
		model.put(getObjectName(), getTarget());
		// Errors instance, even if no errors.
		model.put(MODEL_KEY_PREFIX + getObjectName(), this);
		return model;
	}

	@Override
	@Nullable
	public Object getRawFieldValue(String field) {
		return (getTarget() != null ? getActualFieldValue(fixedField(field)) : null);
	}

	/**
	 * 此实现委托给 {@link #getPropertyEditorRegistry() PropertyEditorRegistry} 的编辑器查找功能（如果
	 * 可用）。
	 */
	@Override
	@Nullable
	public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
		PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();
		if (editorRegistry != null) {
			Class<?> valueTypeToUse = valueType;
			if (valueTypeToUse == null) {
				valueTypeToUse = getFieldType(field);
			}
			return editorRegistry.findCustomEditor(valueTypeToUse, fixedField(field));
		}
		else {
			return null;
		}
	}

	/**
	 * 此实现返回 {@code null}。
	 */
	@Override
	@Nullable
	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return null;
	}

	@Override
	public String[] resolveMessageCodes(String errorCode) {
		return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
	}

	@Override
	public String[] resolveMessageCodes(String errorCode, @Nullable String field) {
		return getMessageCodesResolver().resolveMessageCodes(
				errorCode, getObjectName(), fixedField(field), getFieldType(field));
	}

	@Override
	public void addError(ObjectError error) {
		this.errors.add(error);
	}

	@Override
	public void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
		this.fieldTypes.put(field, type);
		this.fieldValues.put(field, value);
	}

	/**
	 * 将指定的不允许字段标记为已抑制。<p>数据绑定器会针对每个被检测到指向不允许字段的字段值调用此方法。
	 * @see DataBinder#setAllowedFields
	 */
	@Override
	public void recordSuppressedField(String field) {
		this.suppressedFields.add(field);
	}

	/**
	 * 返回在绑定过程中被抑制的字段列表。<p>可用于确定是否有任何字段值指向了不允许的字段。
	 * @see DataBinder#setAllowedFields
	 */
	@Override
	public String[] getSuppressedFields() {
		return StringUtils.toStringArray(this.suppressedFields);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BindingResult)) {
			return false;
		}
		BindingResult otherResult = (BindingResult) other;
		return (getObjectName().equals(otherResult.getObjectName()) &&
				ObjectUtils.nullSafeEquals(getTarget(), otherResult.getTarget()) &&
				getAllErrors().equals(otherResult.getAllErrors()));
	}

	@Override
	public int hashCode() {
		return getObjectName().hashCode();
	}


	//---------------------------------------------------------------------
	// Template methods to be implemented/overridden by subclasses
	//---------------------------------------------------------------------

	/**
	 * 返回被包装的目标对象。
	 */
	@Override
	@Nullable
	public abstract Object getTarget();

	/**
	 * 提取给定字段的实际字段值。
	 * @param field the field to check
	 * @return the current value of the field
	 */
	@Nullable
	protected abstract Object getActualFieldValue(String field);

	/**
	 * 为指定字段格式化给定值。<p>默认实现直接原样返回字段值。
	 * @param field the field to check
	 * @param value the value of the field (either a rejected value
	 * other than from a binding error, or an actual field value)
	 * @return the formatted value
	 */
	@Nullable
	protected Object formatFieldValue(String field, @Nullable Object value) {
		return value;
	}

}
