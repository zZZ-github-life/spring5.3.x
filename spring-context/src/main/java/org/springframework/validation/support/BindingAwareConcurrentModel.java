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
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BindingResult;

/**
 * {@link ConcurrentModel} 的子类，当对应的目标属性通过常规 {@link Map} 操作被替换时，自动移除
 * {@link BindingResult} 对象。
 *
 * <p>这是由 Spring WebFlux 暴露给处理方法使用的类，通常通过将 {@link org.springframework.ui.Model}
 * 接口声明为参数类型来使用。通常无需在用户代码中创建它。如有必要，处理方法可以返回常规的
 * {@code java.util.Map}（很可能是 {@code java.util.ConcurrentMap}），以提供预先确定的模型。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see BindingResult
 * @see BindingAwareModelMap
 */
@SuppressWarnings("serial")
public class BindingAwareConcurrentModel extends ConcurrentModel {

	@Override
	@Nullable
	public Object put(String key, @Nullable Object value) {
		removeBindingResultIfNecessary(key, value);
		return super.put(key, value);
	}

	private void removeBindingResultIfNecessary(String key, @Nullable Object value) {
		if (!key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			String resultKey = BindingResult.MODEL_KEY_PREFIX + key;
			BindingResult result = (BindingResult) get(resultKey);
			if (result != null && result.getTarget() != value) {
				remove(resultKey);
			}
		}
	}

}
