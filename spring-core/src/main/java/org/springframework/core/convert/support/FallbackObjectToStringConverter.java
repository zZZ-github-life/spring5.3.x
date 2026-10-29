/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.convert.support;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

/**
 * 直接调用 {@link Object#toString()} 将任何支持的对象转换为 {@link String}。<p>支持 {@link CharSequence}、{@link StringWriter}，以及任何具有 String 构造函数或以下静态工厂方法之一的类：{@code valueOf(String)}、{@code of(String)}、{@code from(String)}。<p>当没有注册其他显式的 to-String 转换器时，{@link DefaultConversionService} 会将其用作回退。
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see ObjectToObjectConverter
 */
final class FallbackObjectToStringConverter implements ConditionalGenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object.class, String.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Class<?> sourceClass = sourceType.getObjectType();
		if (String.class == sourceClass) {
			// no conversion required
			return false;
		}
		return (CharSequence.class.isAssignableFrom(sourceClass) ||
				StringWriter.class.isAssignableFrom(sourceClass) ||
				ObjectToObjectConverter.hasConversionMethodOrConstructor(sourceClass, String.class));
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return (source != null ? source.toString() : null);
	}

}
