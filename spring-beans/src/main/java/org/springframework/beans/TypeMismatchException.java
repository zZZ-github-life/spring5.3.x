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

package org.springframework.beans;

import java.beans.PropertyChangeEvent;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 尝试设置 Bean 属性时发生类型不匹配所抛出的异常。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class TypeMismatchException extends PropertyAccessException {

	/**
	 * 类型不匹配错误将注册到的错误码。
	 */
	public static final String ERROR_CODE = "typeMismatch";


	@Nullable
	private String propertyName;

	@Nullable
	private final transient Object value;

	@Nullable
	private final Class<?> requiredType;


	/**
	 * 创建新的 {@code TypeMismatchException}。
	 * @param propertyChangeEvent 导致该问题的 PropertyChangeEvent
	 * @param requiredType 所需的目标类型
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType) {
		this(propertyChangeEvent, requiredType, null);
	}

	/**
	 * 创建新的 {@code TypeMismatchException}。
	 * @param propertyChangeEvent 导致该问题的 PropertyChangeEvent
	 * @param requiredType 所需的目标类型（若未知则为 {@code null}）
	 * @param cause 根本原因（可能为 {@code null}）
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, @Nullable Class<?> requiredType,
			@Nullable Throwable cause) {

		super(propertyChangeEvent,
				"Failed to convert property value of type '" +
				ClassUtils.getDescriptiveType(propertyChangeEvent.getNewValue()) + "'" +
				(requiredType != null ?
				" to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
				(propertyChangeEvent.getPropertyName() != null ?
				" for property '" + propertyChangeEvent.getPropertyName() + "'" : ""),
				cause);
		this.propertyName = propertyChangeEvent.getPropertyName();
		this.value = propertyChangeEvent.getNewValue();
		this.requiredType = requiredType;
	}

	/**
	 * 创建不带 {@code PropertyChangeEvent} 的新的 {@code TypeMismatchException}。
	 * @param value 无法转换的问题值（可能为 {@code null}）
	 * @param requiredType 所需的目标类型（若未知则为 {@code null}）
	 * @see #initPropertyName
	 */
	public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType) {
		this(value, requiredType, null);
	}

	/**
	 * 创建不带 {@code PropertyChangeEvent} 的新的 {@code TypeMismatchException}。
	 * @param value 无法转换的问题值（可能为 {@code null}）
	 * @param requiredType 所需的目标类型（若未知则为 {@code null}）
	 * @param cause 根本原因（可能为 {@code null}）
	 * @see #initPropertyName
	 */
	public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
		super("Failed to convert value of type '" + ClassUtils.getDescriptiveType(value) + "'" +
				(requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : ""),
				cause);
		this.value = value;
		this.requiredType = requiredType;
	}


	/**
	 * 初始化此异常的属性名，以便通过 {@link #getPropertyName()} 暴露，作为通过
	 * {@link PropertyChangeEvent} 初始化的替代方式。
	 * @param propertyName 要暴露的属性名称
	 * @since 5.0.4
	 * @see #TypeMismatchException(Object, Class)
	 * @see #TypeMismatchException(Object, Class, Throwable)
	 */
	public void initPropertyName(String propertyName) {
		Assert.state(this.propertyName == null, "Property name already initialized");
		this.propertyName = propertyName;
	}

	/**
	 * 返回受影响的属性名（如果可用）。
	 */
	@Override
	@Nullable
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * 返回引起问题的值（可能为 {@code null}）。
	 */
	@Override
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 返回所需的目标类型（如果有）。
	 */
	@Nullable
	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
