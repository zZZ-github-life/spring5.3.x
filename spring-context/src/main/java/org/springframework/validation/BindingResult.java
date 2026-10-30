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
import java.util.Map;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.lang.Nullable;

/**
 * 表示绑定结果的通用接口。扩展了 {@link Errors interface} 以提供错误注册能力，允许应用
 * {@link Validator}，并增加了特定于绑定的分析和模型构建功能。
 *
 * <p>作为 {@link DataBinder} 的结果持有者，可通过 {@link DataBinder#getBindingResult()} 方法获得。
 * BindingResult 实现也可以直接使用，例如在其上调用 {@link Validator}（例如作为单元测试的一部分）。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see DataBinder
 * @see Errors
 * @see Validator
 * @see BeanPropertyBindingResult
 * @see DirectFieldBindingResult
 * @see MapBindingResult
 */
public interface BindingResult extends Errors {

	/**
	 * 模型中 BindingResult 实例名称的前缀，其后跟随对象名称。
	 */
	String MODEL_KEY_PREFIX = BindingResult.class.getName() + ".";


	/**
	 * 返回被包装的目标对象，它可能是一个 Bean、一个带有公共字段的对象或一个 Map——具体取决于具体的绑定
	 * 策略。
	 */
	@Nullable
	Object getTarget();

	/**
	 * 为所获得的状态返回一个模型 Map，将 BindingResult 实例暴露为 '
	 * {@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName'，并将对象本身暴露为 'objectName'。<p>请
	 * 注意，每次调用此方法都会构造该 Map。向 Map 中添加内容后再重新调用此方法将不起作用。<p>此方法返回
	 * 的模型 Map 中的属性通常会被包含在 {@link org.springframework.web.servlet.ModelAndView} 中，用于
	 * 在 JSP 中使用 Spring 的 {@code bind} 标签的表单视图，该视图需要访问 BindingResult 实例。Spring
	 * 预置的表单控制器在渲染表单视图时会为您完成这些工作。当您自行构建 ModelAndView 实例时，需要包含此
	 * 方法返回的模型 Map 中的属性。
	 * @see #getObjectName()
	 * @see #MODEL_KEY_PREFIX
	 * @see org.springframework.web.servlet.ModelAndView
	 * @see org.springframework.web.servlet.tags.BindTag
	 */
	Map<String, Object> getModel();

	/**
	 * 提取给定字段的原始字段值。通常用于比较目的。
	 * @param field 要检查的字段
	 * @return 字段当前值的原始形式，如果未知则为 {@code null}
	 */
	@Nullable
	Object getRawFieldValue(String field);

	/**
	 * 查找给定类型和属性的自定义属性编辑器。
	 * @param field 属性的路径（名称或嵌套路径）；如果查找适用于给定类型的
	 * 所有属性的编辑器，则为 {@code null}
	 * @param valueType 属性的类型（如果给定了属性则可为 {@code null}，
	 * 但为了进行一致性检查，无论如何都应指定）
	 * @return 注册的编辑器，如果没有则为 {@code null}
	 */
	@Nullable
	PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType);

	/**
	 * 返回底层的 PropertyEditorRegistry。
	 * @return PropertyEditorRegistry；如果此 BindingResult 没有可用的，
	 * 则为 {@code null}
	 */
	@Nullable
	PropertyEditorRegistry getPropertyEditorRegistry();

	/**
	 * 将给定错误代码解析为消息代码。<p>使用适当的参数调用已配置的 {@link MessageCodesResolver}。
	 * @param errorCode 要解析为消息码的错误码
	 * @return 解析得到的消息码
	 */
	String[] resolveMessageCodes(String errorCode);

	/**
	 * 将给定字段的错误代码解析为消息代码。<p>使用适当的参数调用已配置的 {@link MessageCodesResolver}。
	 * @param errorCode 要解析为消息码的错误码
	 * @param field 要为其解析消息码的字段
	 * @return 解析得到的消息码
	 */
	String[] resolveMessageCodes(String errorCode, String field);

	/**
	 * 向错误列表添加自定义的 {@link ObjectError} 或 {@link FieldError}。<p>旨在由诸如
	 * {@link BindingErrorProcessor} 之类的协作策略使用。
	 * @see ObjectError
	 * @see FieldError
	 * @see BindingErrorProcessor
	 */
	void addError(ObjectError error);

	/**
	 * 记录指定字段的给定值。<p>用于无法构造目标对象的情况，使原始字段值可通过 {@link #getFieldValue}
	 * 获得。如果注册了错误，则对于每个受影响的字段将暴露被拒绝的值。
	 * @param field 要为其记录值的字段
	 * @param type 字段的类型
	 * @param value 原始值
	 * @since 5.0.4
	 */
	default void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
	}

	/**
	 * 将指定的不允许字段标记为已抑制。<p>数据绑定器会针对每个被检测到指向不允许字段的字段值调用此方法。
	 * @see DataBinder#setAllowedFields
	 */
	default void recordSuppressedField(String field) {
	}

	/**
	 * 返回在绑定过程中被抑制的字段列表。<p>可用于确定是否有任何字段值指向了不允许的字段。
	 * @see DataBinder#setAllowedFields
	 */
	default String[] getSuppressedFields() {
		return new String[0];
	}

}
