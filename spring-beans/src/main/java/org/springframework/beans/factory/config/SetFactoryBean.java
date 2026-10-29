/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.TypeConverter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * 用于共享 Set 实例的简单工厂。允许通过 XML Bean 定义中的 "set" 元素集中设置 Set。
 *
 * @author Juergen Hoeller
 * @since 09.12.2003
 * @see ListFactoryBean
 * @see MapFactoryBean
 */
public class SetFactoryBean extends AbstractFactoryBean<Set<Object>> {

	@Nullable
	private Set<?> sourceSet;

	@SuppressWarnings("rawtypes")
	@Nullable
	private Class<? extends Set> targetSetClass;


	/**
	 * 设置源 Set，通常通过 XML 的 "set" 元素填充。
	 */
	public void setSourceSet(Set<?> sourceSet) {
		this.sourceSet = sourceSet;
	}

	/**
	 * 设置用于目标 Set 的类。在 Spring 应用上下文中定义时，可以用完全限定类名填充。
	 * <p>默认是链表式 HashSet，保持注册顺序。
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("rawtypes")
	public void setTargetSetClass(@Nullable Class<? extends Set> targetSetClass) {
		if (targetSetClass == null) {
			throw new IllegalArgumentException("'targetSetClass' must not be null");
		}
		if (!Set.class.isAssignableFrom(targetSetClass)) {
			throw new IllegalArgumentException("'targetSetClass' must implement [java.util.Set]");
		}
		this.targetSetClass = targetSetClass;
	}


	@Override
	@SuppressWarnings("rawtypes")
	public Class<Set> getObjectType() {
		return Set.class;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Set<Object> createInstance() {
		if (this.sourceSet == null) {
			throw new IllegalArgumentException("'sourceSet' is required");
		}
		Set<Object> result = null;
		if (this.targetSetClass != null) {
			result = BeanUtils.instantiateClass(this.targetSetClass);
		}
		else {
			result = new LinkedHashSet<>(this.sourceSet.size());
		}
		Class<?> valueType = null;
		if (this.targetSetClass != null) {
			valueType = ResolvableType.forClass(this.targetSetClass).asCollection().resolveGeneric();
		}
		if (valueType != null) {
			TypeConverter converter = getBeanTypeConverter();
			for (Object elem : this.sourceSet) {
				result.add(converter.convertIfNecessary(elem, valueType));
			}
		}
		else {
			result.addAll(this.sourceSet);
		}
		return result;
	}

}
