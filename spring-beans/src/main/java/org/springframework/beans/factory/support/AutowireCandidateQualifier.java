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

package org.springframework.beans.factory.support;

import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.util.Assert;

/**
 * 用于解析自动装配候选者的限定符。包含一个或多个此类限定符的 Bean 定义可以对字段或参数上的注解进行
 * 细粒度匹配，以便自动装配。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.annotation.Qualifier
 */
@SuppressWarnings("serial")
public class AutowireCandidateQualifier extends BeanMetadataAttributeAccessor {

	/**
	 * 用于存储值的键的名称。
	 */
	public static final String VALUE_KEY = "value";

	private final String typeName;


	/**
	 * 构造一个限定符，用于与给定类型的注解进行匹配。
	 * @param type 注解类型
	 */
	public AutowireCandidateQualifier(Class<?> type) {
		this(type.getName());
	}

	/**
	 * 构造一个限定符，用于与给定类型名称的注解进行匹配。<p>类型名称可以匹配注解的全限定类名或短类名
	 * （不含包名）。
	 * @param typeName 注解类型的名称
	 */
	public AutowireCandidateQualifier(String typeName) {
		Assert.notNull(typeName, "Type name must not be null");
		this.typeName = typeName;
	}

	/**
	 * 构造一个限定符，用于与给定类型的注解进行匹配，且该注解的 {@code value} 属性也与指定的值匹配。
	 * @param type 注解类型
	 * @param value 要匹配的注解值
	 */
	public AutowireCandidateQualifier(Class<?> type, Object value) {
		this(type.getName(), value);
	}

	/**
	 * 构造一个限定符，用于与给定类型名称的注解进行匹配，且该注解的 {@code value} 属性也与指定的值匹配。
	 * <p>类型名称可以匹配注解的全限定类名或短类名（不含包名）。
	 * @param typeName 注解类型的名称
	 * @param value 要匹配的注解值
	 */
	public AutowireCandidateQualifier(String typeName, Object value) {
		Assert.notNull(typeName, "Type name must not be null");
		this.typeName = typeName;
		setAttribute(VALUE_KEY, value);
	}


	/**
	 * 检索类型名称。该值将与提供给构造函数的类型名称相同；如果向构造函数提供了 Class 实例，则为全限定
	 * 类名。
	 */
	public String getTypeName() {
		return this.typeName;
	}

}
