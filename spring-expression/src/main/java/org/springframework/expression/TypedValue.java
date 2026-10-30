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

package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 封装一个对象以及描述它的 {@link TypeDescriptor}。类型描述符可以包含无法通过对象上的简单
 * {@code getClass()} 调用访问的泛型声明。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TypedValue {

	/**
	 * {@code null} 对应的 {@link TypedValue}。
	 */
	public static final TypedValue NULL = new TypedValue(null);


	@Nullable
	private final Object value;

	@Nullable
	private TypeDescriptor typeDescriptor;


	/**
	 * 为简单对象创建 {@link TypedValue}。{@link TypeDescriptor} 从该对象推断而来，因此不会保留泛型声明。
	 * @param value 对象值
	 */
	public TypedValue(@Nullable Object value) {
		this.value = value;
		this.typeDescriptor = null;  // initialized when/if requested
	}

	/**
	 * 为特定值创建带有特定 {@link TypeDescriptor} 的 {@link TypedValue}，该描述符可以包含额外的泛型声
	 * 明。
	 * @param value 对象值
	 * @param typeDescriptor 描述该值类型的类型描述符
	 */
	public TypedValue(@Nullable Object value, @Nullable TypeDescriptor typeDescriptor) {
		this.value = value;
		this.typeDescriptor = typeDescriptor;
	}


	@Nullable
	public Object getValue() {
		return this.value;
	}

	@Nullable
	public TypeDescriptor getTypeDescriptor() {
		if (this.typeDescriptor == null && this.value != null) {
			this.typeDescriptor = TypeDescriptor.forObject(this.value);
		}
		return this.typeDescriptor;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypedValue)) {
			return false;
		}
		TypedValue otherTv = (TypedValue) other;
		// Avoid TypeDescriptor initialization if not necessary
		return (ObjectUtils.nullSafeEquals(this.value, otherTv.value) &&
				((this.typeDescriptor == null && otherTv.typeDescriptor == null) ||
						ObjectUtils.nullSafeEquals(getTypeDescriptor(), otherTv.getTypeDescriptor())));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value);
	}

	@Override
	public String toString() {
		return "TypedValue: '" + this.value + "' of [" + getTypeDescriptor() + "]";
	}

}
