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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于保存受管数组元素的标记集合类，其中可能包含运行时 Bean 引用（将被解析为 Bean 对象）。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ManagedArray extends ManagedList<Object> {

	/** 用于在运行时创建目标数组的已解析元素类型。 */
	@Nullable
	volatile Class<?> resolvedElementType;


	/**
	 * 创建一个新的受管数组占位符。
	 * @param elementTypeName 目标元素类型的类名
	 * @param size 数组的大小
	 */
	public ManagedArray(String elementTypeName, int size) {
		super(size);
		Assert.notNull(elementTypeName, "elementTypeName must not be null");
		setElementTypeName(elementTypeName);
	}

}
