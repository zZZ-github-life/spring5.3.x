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

package org.springframework.test.context.support;

import org.springframework.lang.Nullable;

/**
 * 提供命名属性的策略 &mdash; 例如，以通用方式查找键值对。
 *
 * <p>主要供框架内部使用。
 *
 * @author Sam Brannen
 * @since 5.3
 */
@FunctionalInterface
public interface PropertyProvider {

	/**
	 * 获取指定名称属性的值。
	 * @param name 要检索的属性的名称
	 * @return 属性的值；如果未找到，则为 {@code null}
	 */
	@Nullable
	String get(String name);

}
