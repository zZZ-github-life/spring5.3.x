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

package org.springframework.test.context;

import java.util.function.Supplier;

/**
 * 与 {@link DynamicPropertySource @DynamicPropertySource} 方法一起使用的注册表，
 * 使这些方法能够向 {@code Environment} 添加具有
 * 动态解析值的属性。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertySource
 */
public interface DynamicPropertyRegistry {

	/**
	 * 为此注册表中的给定属性名添加一个 {@link Supplier}。
	 * @param name 应为其添加供应商的属性名称
	 * @param valueSupplier 按需提供属性值的供应商
	 */
	void add(String name, Supplier<Object> valueSupplier);

}
