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

package org.springframework.r2dbc.core;

import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 可以在语句中设置的数据库值。
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.3
 */
public final class Parameter {

	@Nullable
	private final Object value;

	private final Class<?> type;


	private Parameter(@Nullable Object value, Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		this.value = value;
		this.type = type;
	}


	/**
	 * 根据 {@code value} 创建一个新的 {@link Parameter}。
	 * @param value 参数值
	 * @return {@code value} 对应的 {@link Parameter} 值
	 */
	public static Parameter from(Object value) {
		Assert.notNull(value, "Value must not be null");
		return new Parameter(value, ClassUtils.getUserClass(value));
	}

	/**
	 * 根据 {@code value} 和 {@code type} 创建一个新的 {@link Parameter}。
	 * @param value 参数值（可以为 {@code null}）
	 * @param type 参数类型
	 * @return {@code value} 对应的 {@link Parameter} 值
	 */
	public static Parameter fromOrEmpty(@Nullable Object value, Class<?> type) {
		return (value == null ? empty(type) : new Parameter(value, ClassUtils.getUserClass(value)));
	}

	/**
	 * 为 {@code type} 创建一个新的空 {@link Parameter}。
	 * @return {@code type} 对应的空 {@link Parameter} 值
	 */
	public static Parameter empty(Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		return new Parameter(null, type);
	}


	/**
	 * 返回列值（可以为 {@code null}）。
	 * @see #hasValue()
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 返回列值类型。即使 {@code value} 为 {@code null}，该类型也必须存在。
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * 返回此 {@link Parameter} 是否有值。
	 * @return 如果 {@link #getValue()} 为 {@code null} 则返回 {@code false}
	 */
	public boolean hasValue() {
		return (this.value != null);
	}

	/**
	 * 返回此 {@link Parameter} 是否有空值。
	 * @return 如果 {@link #getValue()} 为 {@code null} 则返回 {@code true}
	 */
	public boolean isEmpty() {
		return (this.value == null);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Parameter)) {
			return false;
		}
		Parameter other = (Parameter) obj;
		return (ObjectUtils.nullSafeEquals(this.value, other.value) &&
				ObjectUtils.nullSafeEquals(this.type, other.type));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.value, this.type);
	}

	@Override
	public String toString() {
		return "Parameter[value=" + this.value + ",type=" + this.type.getName() + "]";
	}

}
