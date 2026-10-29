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

package org.springframework.messaging.handler.annotation;

/**
 * 常用注解值常量。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ValueConstants {

	/**
	 * 表示“无默认值”的常量——用于替代无法在注解属性中使用的 {@code null}。
	 * <p>这是由 16 个 Unicode 字符组成的人工安排，其唯一目的是永远不会匹配用户声明的值。
	 * @see Header#defaultValue()
	 */
	String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

}
