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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 类型化 String 值的持有者。可以添加到 Bean 定义中，以便为 String 值显式指定目标类型，例如用于集合
 * 元素。
 *
 * <p>此持有者仅存储 String 值和目标类型。实际转换将由 Bean 工厂执行。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see BeanDefinition#getPropertyValues
 * @see org.springframework.beans.MutablePropertyValues#addPropertyValue
 */
public class TypedStringValue implements BeanMetadataElement {

	@Nullable
	private String value;

	@Nullable
	private volatile Object targetType;

	@Nullable
	private Object source;

	@Nullable
	private String specifiedTypeName;

	private volatile boolean dynamic;


	/**
	 * 为给定 String 值创建一个新的 {@link TypedStringValue}。
	 * @param value String 值
	 */
	public TypedStringValue(@Nullable String value) {
		setValue(value);
	}

	/**
	 * 为给定 String 值和目标类型创建一个新的 {@link TypedStringValue}。
	 * @param value String 值
	 * @param targetType 要转换为的类型
	 */
	public TypedStringValue(@Nullable String value, Class<?> targetType) {
		setValue(value);
		setTargetType(targetType);
	}

	/**
	 * 为给定 String 值和目标类型创建一个新的 {@link TypedStringValue}。
	 * @param value String 值
	 * @param targetTypeName 要转换为的类型
	 */
	public TypedStringValue(@Nullable String value, String targetTypeName) {
		setValue(value);
		setTargetTypeName(targetTypeName);
	}


	/**
	 * 设置 String 值。
	 * <p>仅在操作已注册的值时需要，例如在 BeanFactoryPostProcessors 中。
	 */
	public void setValue(@Nullable String value) {
		this.value = value;
	}

	/**
	 * 返回 String 值。
	 */
	@Nullable
	public String getValue() {
		return this.value;
	}

	/**
	 * 设置要转换到的类型。
	 * <p>仅在操作已注册的值时需要，例如在 BeanFactoryPostProcessors 中。
	 */
	public void setTargetType(Class<?> targetType) {
		Assert.notNull(targetType, "'targetType' must not be null");
		this.targetType = targetType;
	}

	/**
	 * 返回要转换到的类型。
	 */
	public Class<?> getTargetType() {
		Object targetTypeValue = this.targetType;
		if (!(targetTypeValue instanceof Class)) {
			throw new IllegalStateException("Typed String value does not carry a resolved target type");
		}
		return (Class<?>) targetTypeValue;
	}

	/**
	 * 指定要转换到的类型。
	 */
	public void setTargetTypeName(@Nullable String targetTypeName) {
		this.targetType = targetTypeName;
	}

	/**
	 * 返回要转换到的类型。
	 */
	@Nullable
	public String getTargetTypeName() {
		Object targetTypeValue = this.targetType;
		if (targetTypeValue instanceof Class) {
			return ((Class<?>) targetTypeValue).getName();
		}
		else {
			return (String) targetTypeValue;
		}
	}

	/**
	 * 返回此类型化 String 值是否带有目标类型。
	 */
	public boolean hasTargetType() {
		return (this.targetType instanceof Class);
	}

	/**
	 * 确定要转换到的类型，必要时从指定的类名解析。当调用时目标类型已解析，还会根据类名重新加载指定的
	 * Class。
	 * @param classLoader 用于解析（可能的）类名的 ClassLoader
	 * @return 要转换到的已解析类型
	 * @throws ClassNotFoundException 如果无法解析该类型
	 */
	@Nullable
	public Class<?> resolveTargetType(@Nullable ClassLoader classLoader) throws ClassNotFoundException {
		String typeName = getTargetTypeName();
		if (typeName == null) {
			return null;
		}
		Class<?> resolvedClass = ClassUtils.forName(typeName, classLoader);
		this.targetType = resolvedClass;
		return resolvedClass;
	}


	/**
	 * 为此元数据元素设置配置来源 {@code Object}。
	 * <p>对象的确切类型取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}

	/**
	 * 设置此特定值实际指定的类型名称（如果有）。
	 */
	public void setSpecifiedTypeName(@Nullable String specifiedTypeName) {
		this.specifiedTypeName = specifiedTypeName;
	}

	/**
	 * 返回此特定值实际指定的类型名称（如果有）。
	 */
	@Nullable
	public String getSpecifiedTypeName() {
		return this.specifiedTypeName;
	}

	/**
	 * 将此值标记为动态，即包含表达式，因此不受缓存影响。
	 */
	public void setDynamic() {
		this.dynamic = true;
	}

	/**
	 * 返回此值是否已被标记为动态。
	 */
	public boolean isDynamic() {
		return this.dynamic;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TypedStringValue)) {
			return false;
		}
		TypedStringValue otherValue = (TypedStringValue) other;
		return (ObjectUtils.nullSafeEquals(this.value, otherValue.value) &&
				ObjectUtils.nullSafeEquals(this.targetType, otherValue.targetType));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.value) * 29 + ObjectUtils.nullSafeHashCode(this.targetType);
	}

	@Override
	public String toString() {
		return "TypedStringValue: value [" + this.value + "], target type [" + this.targetType + "]";
	}

}
