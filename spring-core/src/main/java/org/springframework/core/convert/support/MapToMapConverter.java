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

package org.springframework.core.convert.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

/**
 * 将 Map 转换为另一个 Map。<p>首先，创建一个所请求 targetType 的新 Map，其大小等于源 Map 的大小。然后从源 Map 中复制每个元素到目标 Map。必要时，会执行从源 Map 的参数化 K,V 类型到目标 Map 参数化类型 K,V 的转换。
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class MapToMapConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public MapToMapConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Map.class, Map.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return canConvertKey(sourceType, targetType) && canConvertValue(sourceType, targetType);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<Object, Object> sourceMap = (Map<Object, Object>) source;

		// Shortcut if possible...
		boolean copyRequired = !targetType.getType().isInstance(source);
		if (!copyRequired && sourceMap.isEmpty()) {
			return sourceMap;
		}
		TypeDescriptor keyDesc = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor valueDesc = targetType.getMapValueTypeDescriptor();

		List<MapEntry> targetEntries = new ArrayList<>(sourceMap.size());
		for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
			Object sourceKey = entry.getKey();
			Object sourceValue = entry.getValue();
			Object targetKey = convertKey(sourceKey, sourceType, keyDesc);
			Object targetValue = convertValue(sourceValue, sourceType, valueDesc);
			targetEntries.add(new MapEntry(targetKey, targetValue));
			if (sourceKey != targetKey || sourceValue != targetValue) {
				copyRequired = true;
			}
		}
		if (!copyRequired) {
			return sourceMap;
		}

		Map<Object, Object> targetMap = CollectionFactory.createMap(targetType.getType(),
				(keyDesc != null ? keyDesc.getType() : null), sourceMap.size());

		for (MapEntry entry : targetEntries) {
			entry.addToMap(targetMap);
		}
		return targetMap;
	}


	// internal helpers

	private boolean canConvertKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getMapKeyTypeDescriptor(),
				targetType.getMapKeyTypeDescriptor(), this.conversionService);
	}

	private boolean canConvertValue(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getMapValueTypeDescriptor(),
				targetType.getMapValueTypeDescriptor(), this.conversionService);
	}

	@Nullable
	private Object convertKey(Object sourceKey, TypeDescriptor sourceType, @Nullable TypeDescriptor targetType) {
		if (targetType == null) {
			return sourceKey;
		}
		return this.conversionService.convert(sourceKey, sourceType.getMapKeyTypeDescriptor(sourceKey), targetType);
	}

	@Nullable
	private Object convertValue(Object sourceValue, TypeDescriptor sourceType, @Nullable TypeDescriptor targetType) {
		if (targetType == null) {
			return sourceValue;
		}
		return this.conversionService.convert(sourceValue, sourceType.getMapValueTypeDescriptor(sourceValue), targetType);
	}


	private static class MapEntry {

		@Nullable
		private final Object key;

		@Nullable
		private final Object value;

		public MapEntry(@Nullable Object key, @Nullable Object value) {
			this.key = key;
			this.value = value;
		}

		public void addToMap(Map<Object, Object> map) {
			map.put(this.key, this.value);
		}
	}

}
