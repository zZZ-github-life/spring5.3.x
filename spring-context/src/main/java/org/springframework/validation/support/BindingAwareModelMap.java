/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.validation.support;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;

/**
 * {@link org.springframework.ui.ExtendedModelMap} 的子类，如果对应的目标属性通过常规 {@link Map}
 * 操作被替换，则自动移除 {@link org.springframework.validation.BindingResult} 对象。
 *
 * <p>这是由 Spring MVC 暴露给处理方法使用的类，通常通过声明 {@link org.springframework.ui.Model}
 * 接口来使用。无需在用户代码中构建它；普通的 {@link org.springframework.ui.ModelMap}，甚至只是带
 * String 键的常规 {@link Map}，就足以返回用户模型。
 *
 * @author Juergen Hoeller
 * @since 2.5.6
 * @see org.springframework.validation.BindingResult
 */
@SuppressWarnings("serial")
public class BindingAwareModelMap extends ExtendedModelMap {

	@Override
	public Object put(String key, @Nullable Object value) {
		removeBindingResultIfNecessary(key, value);
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		map.forEach(this::removeBindingResultIfNecessary);
		super.putAll(map);
	}

	private void removeBindingResultIfNecessary(Object key, @Nullable Object value) {
		if (key instanceof String) {
			String attributeName = (String) key;
			if (!attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + attributeName;
				BindingResult bindingResult = (BindingResult) get(bindingResultKey);
				if (bindingResult != null && bindingResult.getTarget() != value) {
					remove(bindingResultKey);
				}
			}
		}
	}

}
