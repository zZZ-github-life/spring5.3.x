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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * 用于包装带根因的受检 {@code Exceptions} 的便捷类。
 *
 * <p>此类为 {@code abstract}，以强制程序员扩展该类。{@code getMessage} 将包含嵌套异常信息；
 * {@code printStackTrace} 及类似方法将委托给所包装的异常（如果有）。
 *
 * <p>此类与 {@link NestedRuntimeException} 类之间的相似性不可避免，因为 Java 强制这两个类具有不同
 * 的超类（唉，具体继承的不灵活性！）。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getMessage
 * @see #printStackTrace
 * @see NestedRuntimeException
 */
public abstract class NestedCheckedException extends Exception {

	/** 使用 Spring 1.2 中的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = 7100714597678207546L;

	static {
		// Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
		// issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	/**
	 * 使用指定的详细消息构造一个 {@code NestedCheckedException}。
	 * @param msg the detail message
	 */
	public NestedCheckedException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和嵌套异常构造一个 {@code NestedCheckedException}。
	 * @param msg the detail message
	 * @param cause the nested exception
	 */
	public NestedCheckedException(@Nullable String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}


	/**
	 * 返回详细消息，其中包括来自嵌套异常的消息（如果有）。
	 */
	@Override
	@Nullable
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}


	/**
	 * 获取此异常的最内层原因（如果有）。
	 * @return the innermost exception, or {@code null} if none
	 */
	@Nullable
	public Throwable getRootCause() {
		return NestedExceptionUtils.getRootCause(this);
	}

	/**
	 * 获取此异常最具体的原因，即最内层原因（根因）或此异常本身。
	 *
	 * <p>与 {@link #getRootCause()} 的不同之处在于：如果没有根因，则回退到当前异常。
	 * @return the most specific cause (never {@code null})
	 * @since 2.0.3
	 */
	public Throwable getMostSpecificCause() {
		Throwable rootCause = getRootCause();
		return (rootCause != null ? rootCause : this);
	}

	/**
	 * 检查此异常是否包含给定类型的异常：它本身属于给定的类，或包含给定类型的嵌套原因。
	 * @param exType the exception type to look for
	 * @return whether there is a nested exception of the specified type
	 */
	public boolean contains(@Nullable Class<?> exType) {
		if (exType == null) {
			return false;
		}
		if (exType.isInstance(this)) {
			return true;
		}
		Throwable cause = getCause();
		if (cause == this) {
			return false;
		}
		if (cause instanceof NestedCheckedException) {
			return ((NestedCheckedException) cause).contains(exType);
		}
		else {
			while (cause != null) {
				if (exType.isInstance(cause)) {
					return true;
				}
				if (cause.getCause() == cause) {
					break;
				}
				cause = cause.getCause();
			}
			return false;
		}
	}

}
