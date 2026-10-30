/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.util.HtmlUtils;

/**
 * 用于暴露字段或对象绑定状态的简单适配器。
 * JSP bind 标签和 FreeMarker 宏都会将其设置为变量。
 *
 * <p>显然，对象状态表示（即对象级别而非字段级别的错误）没有表达式和值，只有错误代码和消息。为简单起见，并且为了能够使用相同的标签和宏，两种场景使用同一个状态类。
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Darren Davison
 * @see RequestContext#getBindStatus
 * @see org.springframework.web.servlet.tags.BindTag
 * @see org.springframework.web.servlet.view.AbstractTemplateView#setExposeSpringMacroHelpers
 */
public class BindStatus {

	private final RequestContext requestContext;

	private final String path;

	private final boolean htmlEscape;

	@Nullable
	private final String expression;

	@Nullable
	private final Errors errors;

	private final String[] errorCodes;

	@Nullable
	private String[] errorMessages;

	@Nullable
	private List<? extends ObjectError> objectErrors;

	@Nullable
	private Object value;

	@Nullable
	private Class<?> valueType;

	@Nullable
	private Object actualValue;

	@Nullable
	private PropertyEditor editor;

	@Nullable
	private BindingResult bindingResult;


	/**
	 * 创建一个新的 BindStatus 实例，表示字段或对象状态。
	 * @param requestContext 当前的 RequestContext
	 * @param path 要解析其值和错误的 Bean 及属性路径（例如 “customer.address.street”）
	 * @param htmlEscape 是否对错误消息和字符串值进行 HTML 转义
	 * @throws IllegalStateException 如果未找到对应的 Errors 对象
	 */
	public BindStatus(RequestContext requestContext, String path, boolean htmlEscape) throws IllegalStateException {
		this.requestContext = requestContext;
		this.path = path;
		this.htmlEscape = htmlEscape;

		// determine name of the object and property
		String beanName;
		int dotPos = path.indexOf('.');
		if (dotPos == -1) {
			// property not set, only the object itself
			beanName = path;
			this.expression = null;
		}
		else {
			beanName = path.substring(0, dotPos);
			this.expression = path.substring(dotPos + 1);
		}

		this.errors = requestContext.getErrors(beanName, false);

		if (this.errors != null) {
			// Usual case: A BindingResult is available as request attribute.
			// Can determine error codes and messages for the given expression.
			// Can use a custom PropertyEditor, as registered by a form controller.
			if (this.expression != null) {
				if ("*".equals(this.expression)) {
					this.objectErrors = this.errors.getAllErrors();
				}
				else if (this.expression.endsWith("*")) {
					this.objectErrors = this.errors.getFieldErrors(this.expression);
				}
				else {
					this.objectErrors = this.errors.getFieldErrors(this.expression);
					this.value = this.errors.getFieldValue(this.expression);
					this.valueType = this.errors.getFieldType(this.expression);
					if (this.errors instanceof BindingResult) {
						this.bindingResult = (BindingResult) this.errors;
						this.actualValue = this.bindingResult.getRawFieldValue(this.expression);
						this.editor = this.bindingResult.findEditor(this.expression, null);
					}
					else {
						this.actualValue = this.value;
					}
				}
			}
			else {
				this.objectErrors = this.errors.getGlobalErrors();
			}
			this.errorCodes = initErrorCodes(this.objectErrors);
		}

		else {
			// No BindingResult available as request attribute:
			// Probably forwarded directly to a form view.
			// Let's do the best we can: extract a plain target if appropriate.
			Object target = requestContext.getModelObject(beanName);
			if (target == null) {
				throw new IllegalStateException("Neither BindingResult nor plain target object for bean name '" +
						beanName + "' available as request attribute");
			}
			if (this.expression != null && !"*".equals(this.expression) && !this.expression.endsWith("*")) {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(target);
				this.value = bw.getPropertyValue(this.expression);
				this.valueType = bw.getPropertyType(this.expression);
				this.actualValue = this.value;
			}
			this.errorCodes = new String[0];
			this.errorMessages = new String[0];
		}

		if (htmlEscape && this.value instanceof String) {
			this.value = HtmlUtils.htmlEscape((String) this.value);
		}
	}

	/**
	 * 从 ObjectError 列表中提取错误代码。
	 */
	private static String[] initErrorCodes(List<? extends ObjectError> objectErrors) {
		String[] errorCodes = new String[objectErrors.size()];
		for (int i = 0; i < objectErrors.size(); i++) {
			ObjectError error = objectErrors.get(i);
			errorCodes[i] = error.getCode();
		}
		return errorCodes;
	}


	/**
	 * 返回值以及错误将要解析的 bean 和属性路径（例如 "customer.address.street"）。
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * 返回一个绑定表达式，可在 HTML 表单中用作相应字段的 input 名称；如果不是特定于字段的，则返回 {@code null}。
	 * <p>返回适合重新提交的绑定路径，例如 "address.street"。请注意，如果绑定到 "customer" bean，则 bind 标签所需的完整绑定路径是 "customer.address.street"。
	 */
	@Nullable
	public String getExpression() {
		return this.expression;
	}

	/**
	 * 返回字段的当前值，即属性值或被拒绝的更新值；如果不是特定于字段的，则返回 {@code null}。
	 * <p>如果原始值本来就是 String，则此值将是经过 HTML 转义的 String。
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 获取字段的 '{@code Class}' 类型。优先使用此方法而不是 '{@code getValue().getClass()}'，因为 '{@code getValue()}' 可能返回 '{@code null}'。
	 */
	@Nullable
	public Class<?> getValueType() {
		return this.valueType;
	}

	/**
	 * 返回字段的实际值，即原始属性值；如果不可用，则返回 {@code null}。
	 */
	@Nullable
	public Object getActualValue() {
		return this.actualValue;
	}

	/**
	 * 返回字段的合适显示值，即值非 null 时的字符串化值，值为 null 时则为空字符串。
	 * <p>如果原始值非 null，则此值将是经过 HTML 转义的 String：原始值的 {@code toString} 结果会进行 HTML 转义。
	 */
	public String getDisplayValue() {
		if (this.value instanceof String) {
			return (String) this.value;
		}
		if (this.value != null) {
			return (this.htmlEscape ? HtmlUtils.htmlEscape(this.value.toString()) : this.value.toString());
		}
		return "";
	}

	/**
	 * 返回此状态是否表示字段错误或对象错误。
	 */
	public boolean isError() {
		return (this.errorCodes.length > 0);
	}

	/**
	 * 返回字段或对象的错误代码（如果有）。如果没有，则返回空数组而不是 null。
	 */
	public String[] getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * 返回字段或对象的第一个错误代码（如果有）。
	 */
	public String getErrorCode() {
		return (this.errorCodes.length > 0 ? this.errorCodes[0] : "");
	}

	/**
	 * 返回字段或对象的已解析错误消息（如果有）。如果没有，则返回空数组而不是 null。
	 */
	public String[] getErrorMessages() {
		return initErrorMessages();
	}

	/**
	 * 返回字段或对象的第一条错误消息（如果有）。
	 */
	public String getErrorMessage() {
		String[] errorMessages = initErrorMessages();
		return (errorMessages.length > 0 ? errorMessages[0] : "");
	}

	/**
	 * 返回一个错误消息字符串，将所有消息以给定分隔符连接起来。
	 * @param delimiter 分隔字符串，例如 “, ” 或 “<br>”
	 * @return 错误消息字符串
	 */
	public String getErrorMessagesAsString(String delimiter) {
		return StringUtils.arrayToDelimitedString(initErrorMessages(), delimiter);
	}

	/**
	 * 从 ObjectError 列表中提取错误消息。
	 */
	private String[] initErrorMessages() throws NoSuchMessageException {
		if (this.errorMessages == null) {
			if (this.objectErrors != null) {
				this.errorMessages = new String[this.objectErrors.size()];
				for (int i = 0; i < this.objectErrors.size(); i++) {
					ObjectError error = this.objectErrors.get(i);
					this.errorMessages[i] = this.requestContext.getMessage(error, this.htmlEscape);
				}
			}
			else {
				this.errorMessages = new String[0];
			}
		}
		return this.errorMessages;
	}

	/**
	 * 返回此绑定状态当前关联的 Errors 实例（通常是 BindingResult）。
	 * @return 当前的 Errors 实例；如果没有，则为 {@code null}
	 * @see org.springframework.validation.BindingResult
	 */
	@Nullable
	public Errors getErrors() {
		return this.errors;
	}

	/**
	 * 返回此绑定状态当前所绑定属性的 PropertyEditor。
	 * @return 当前的 PropertyEditor；如果没有，则为 {@code null}
	 */
	@Nullable
	public PropertyEditor getEditor() {
		return this.editor;
	}

	/**
	 * 为给定值类查找与此绑定状态当前所绑定属性相关联的 PropertyEditor。
	 * @param valueClass 需要为其提供编辑器的值类型
	 * @return 关联的 PropertyEditor；如果没有，则为 {@code null}
	 */
	@Nullable
	public PropertyEditor findEditor(Class<?> valueClass) {
		return (this.bindingResult != null ? this.bindingResult.findEditor(this.expression, valueClass) : null);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("BindStatus: ");
		sb.append("expression=[").append(this.expression).append("]; ");
		sb.append("value=[").append(this.value).append(']');
		if (!ObjectUtils.isEmpty(this.errorCodes)) {
			sb.append("; errorCodes=").append(Arrays.asList(this.errorCodes));
		}
		return sb.toString();
	}

}
