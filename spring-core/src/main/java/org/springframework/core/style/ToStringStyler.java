/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.style;

import org.springframework.lang.Nullable;

/**
 * 用于美观输出 {@code toString()} 方法的策略接口。封装打印算法；工作流应由诸如构建器之类的其他对象
 * 提供。
 *
 * @author Keith Donald
 * @since 1.2.2
 */
public interface ToStringStyler {

	/**
	 * 在样式化其字段之前，为已调用 {@code toString()} 的对象设置样式。
	 * @param buffer the buffer to print to
	 * @param obj the object to style
	 */
	void styleStart(StringBuilder buffer, Object obj);

	/**
	 * 在其字段被样式化之后，为已调用 {@code toString()} 的对象设置样式。
	 * @param buffer the buffer to print to
	 * @param obj the object to style
	 */
	void styleEnd(StringBuilder buffer, Object obj);

	/**
	 * 将字段值样式化为字符串。
	 * @param buffer the buffer to print to
	 * @param fieldName the name of the field
	 * @param value the field value
	 */
	void styleField(StringBuilder buffer, String fieldName, @Nullable Object value);

	/**
	 * 为给定的值设置样式。
	 * @param buffer the buffer to print to
	 * @param value the field value
	 */
	void styleValue(StringBuilder buffer, Object value);

	/**
	 * 为字段分隔符设置样式。
	 * @param buffer the buffer to print to
	 */
	void styleFieldSeparator(StringBuilder buffer);

}
