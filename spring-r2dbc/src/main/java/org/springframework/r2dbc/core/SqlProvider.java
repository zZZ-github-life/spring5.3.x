/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.r2dbc.core;

import org.springframework.lang.Nullable;

/**
 * 由能够提供 SQL 字符串的对象实现的接口。
 *
 * <p>通常由希望暴露其用于创建语句的 SQL 的对象实现，
 * 以便在发生异常时提供更好的上下文信息。
 *
 * @author Mark Paluch
 * @since 5.3
 */
public interface SqlProvider {

	/**
	 * 返回此对象的 SQL 字符串，即通常用于创建语句的 SQL。
	 * @return the SQL string, or {@code null} if not available
	 */
	@Nullable
	String getSql();

}
