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

import org.springframework.beans.PropertyAccessException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 默认的 {@link BindingErrorProcessor} 实现。
 *
 * <p>使用 "required" 错误代码和字段名来解析缺失字段错误的消息代码。
 *
 * <p>为给定的每个 {@code PropertyAccessException} 创建 {@code FieldError}，并使用
 * {@code PropertyAccessException} 的错误代码（"typeMismatch"、"methodInvocation"）来解析消息代码。
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @since 1.2
 * @see #MISSING_FIELD_ERROR_CODE
 * @see DataBinder#setBindingErrorProcessor
 * @see BeanPropertyBindingResult#addError
 * @see BeanPropertyBindingResult#resolveMessageCodes
 * @see org.springframework.beans.PropertyAccessException#getErrorCode
 * @see org.springframework.beans.TypeMismatchException#ERROR_CODE
 * @see org.springframework.beans.MethodInvocationException#ERROR_CODE
 */
public class DefaultBindingErrorProcessor implements BindingErrorProcessor {

	/**
	 * 缺失字段错误（即在属性值列表中未找到的必填字段）注册时使用的错误代码："required"。
	 */
	public static final String MISSING_FIELD_ERROR_CODE = "required";


	@Override
	public void processMissingFieldError(String missingField, BindingResult bindingResult) {
		// Create field error with code "required".
		String fixedField = bindingResult.getNestedPath() + missingField;
		String[] codes = bindingResult.resolveMessageCodes(MISSING_FIELD_ERROR_CODE, missingField);
		Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), fixedField);
		FieldError error = new FieldError(bindingResult.getObjectName(), fixedField, "", true,
				codes, arguments, "Field '" + fixedField + "' is required");
		bindingResult.addError(error);
	}

	@Override
	public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
		// Create field error with the exceptions's code, e.g. "typeMismatch".
		String field = ex.getPropertyName();
		Assert.state(field != null, "No field in exception");
		String[] codes = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
		Object[] arguments = getArgumentsForBindError(bindingResult.getObjectName(), field);
		Object rejectedValue = ex.getValue();
		if (ObjectUtils.isArray(rejectedValue)) {
			rejectedValue = StringUtils.arrayToCommaDelimitedString(ObjectUtils.toObjectArray(rejectedValue));
		}
		FieldError error = new FieldError(bindingResult.getObjectName(), field, rejectedValue, true,
				codes, arguments, ex.getLocalizedMessage());
		error.wrap(ex);
		bindingResult.addError(error);
	}

	/**
	 * 返回给定字段上绑定错误的 FieldError 参数。对每个缺失的必填字段和每个类型不匹配调用。<p>默认实现
	 * 返回一个表示字段名的参数（类型为 DefaultMessageSourceResolvable，以 "objectName.field" 和
	 * "field" 作为代码）。
	 * @param objectName the name of the target object
	 * @param field the field that caused the binding error
	 * @return the Object array that represents the FieldError arguments
	 * @see org.springframework.validation.FieldError#getArguments
	 * @see org.springframework.context.support.DefaultMessageSourceResolvable
	 */
	protected Object[] getArgumentsForBindError(String objectName, String field) {
		String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		return new Object[] {new DefaultMessageSourceResolvable(codes, field)};
	}

}
