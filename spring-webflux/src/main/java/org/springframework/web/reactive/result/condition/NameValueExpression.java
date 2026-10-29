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

package org.springframework.web.reactive.result.condition;

import org.springframework.lang.Nullable;

/**
 * {@code "name!=value"} 形式表达式的契约，用于在 {@code @RequestMapping} 中指定请求参数和请求头条件。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the value type
 */
public interface NameValueExpression<T> {

	String getName();

	@Nullable
	T getValue();

	boolean isNegated();

}
