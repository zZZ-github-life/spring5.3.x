/*
 * Copyright 2002-2014 the original author or authors.
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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

/**
 * 将一个数组转换为 Collection。
 *
 * <p>首先，创建所需目标类型的新 Collection。然后将每个数组元素添加到目标集合中。如有必要，将执行从
 * 源组件类型到集合参数化类型的元素转换。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class ArrayToCollectionConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public ArrayToCollectionConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Object[].class, Collection.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(
				sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor(), this.conversionService);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}

		int length = Array.getLength(source);
		TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
		Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), length);

		if (elementDesc == null) {
			for (int i = 0; i < length; i++) {
				Object sourceElement = Array.get(source, i);
				target.add(sourceElement);
			}
		}
		else {
			for (int i = 0; i < length; i++) {
				Object sourceElement = Array.get(source, i);
				Object targetElement = this.conversionService.convert(sourceElement,
						sourceType.elementTypeDescriptor(sourceElement), elementDesc);
				target.add(targetElement);
			}
		}
		return target;
	}

}
