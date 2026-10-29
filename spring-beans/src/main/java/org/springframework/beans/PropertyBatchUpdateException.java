/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 组合异常，由单个 PropertyAccessException 实例组成。此类的对象在绑定过程开始时创建，并根据需要向
 * 其中添加错误。
 *
 * <p>绑定过程在遇到应用级 PropertyAccessExceptions 时继续，应用可以应用的更改，并将被拒绝的更改存
 * 储在此类的对象中。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 18 April 2001
 */
@SuppressWarnings("serial")
public class PropertyBatchUpdateException extends BeansException {

	/** PropertyAccessException 对象列表。 */
	private final PropertyAccessException[] propertyAccessExceptions;


	/**
	 * 创建一个新的 PropertyBatchUpdateException。
	 * @param propertyAccessExceptions the List of PropertyAccessExceptions
	 */
	public PropertyBatchUpdateException(PropertyAccessException[] propertyAccessExceptions) {
		super(null, null);
		Assert.notEmpty(propertyAccessExceptions, "At least 1 PropertyAccessException required");
		this.propertyAccessExceptions = propertyAccessExceptions;
	}


	/**
	 * 如果返回 0，则表示绑定过程中未遇到错误。
	 */
	public final int getExceptionCount() {
		return this.propertyAccessExceptions.length;
	}

	/**
	 * 返回此对象中存储的 propertyAccessExceptions 数组。
	 * <p>如果没有错误，将返回空数组（而不是 {@code null}）。
	 */
	public final PropertyAccessException[] getPropertyAccessExceptions() {
		return this.propertyAccessExceptions;
	}

	/**
	 * 返回此字段的异常，如果不存在则返回 {@code null}。
	 */
	@Nullable
	public PropertyAccessException getPropertyAccessException(String propertyName) {
		for (PropertyAccessException pae : this.propertyAccessExceptions) {
			if (ObjectUtils.nullSafeEquals(propertyName, pae.getPropertyName())) {
				return pae;
			}
		}
		return null;
	}


	@Override
	public String getMessage() {
		StringJoiner stringJoiner = new StringJoiner("; ", "Failed properties: ", "");
		for (PropertyAccessException exception : this.propertyAccessExceptions) {
			stringJoiner.add(exception.getMessage());
		}
		return stringJoiner.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName()).append("; nested PropertyAccessExceptions (");
		sb.append(getExceptionCount()).append(") are:");
		for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
			sb.append('\n').append("PropertyAccessException ").append(i + 1).append(": ");
			sb.append(this.propertyAccessExceptions[i]);
		}
		return sb.toString();
	}

	@Override
	public void printStackTrace(PrintStream ps) {
		synchronized (ps) {
			ps.println(getClass().getName() + "; nested PropertyAccessException details (" +
					getExceptionCount() + ") are:");
			for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
				ps.println("PropertyAccessException " + (i + 1) + ":");
				this.propertyAccessExceptions[i].printStackTrace(ps);
			}
		}
	}

	@Override
	public void printStackTrace(PrintWriter pw) {
		synchronized (pw) {
			pw.println(getClass().getName() + "; nested PropertyAccessException details (" +
					getExceptionCount() + ") are:");
			for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
				pw.println("PropertyAccessException " + (i + 1) + ":");
				this.propertyAccessExceptions[i].printStackTrace(pw);
			}
		}
	}

	@Override
	public boolean contains(@Nullable Class<?> exType) {
		if (exType == null) {
			return false;
		}
		if (exType.isInstance(this)) {
			return true;
		}
		for (PropertyAccessException pae : this.propertyAccessExceptions) {
			if (pae.contains(exType)) {
				return true;
			}
		}
		return false;
	}

}
