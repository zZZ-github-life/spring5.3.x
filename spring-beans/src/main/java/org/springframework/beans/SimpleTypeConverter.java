/*
 * Copyright 2002-2013 the original author or authors.
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

/**
 * {@link TypeConverter} 接口的简单实现，不针对特定的目标对象进行操作。对于任意类型转换需求，它是使
 * 用完整的 BeanWrapperImpl 实例的替代方案，同时底层使用完全相同的转换算法（包括委托给
 * {@link java.beans.PropertyEditor} 和 {@link org.springframework.core.convert.ConversionService}）。
 *
 *
 * <p><b>注意：</b>由于依赖于 {@link java.beans.PropertyEditor PropertyEditors}，
 * SimpleTypeConverter <em>不是</em>线程安全的。请为每个线程使用单独的实例。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see BeanWrapperImpl
 */
public class SimpleTypeConverter extends TypeConverterSupport {

	public SimpleTypeConverter() {
		this.typeConverterDelegate = new TypeConverterDelegate(this);
		registerDefaultEditors();
	}

}
