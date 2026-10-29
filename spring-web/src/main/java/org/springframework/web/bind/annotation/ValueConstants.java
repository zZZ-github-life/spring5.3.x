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

package org.springframework.web.bind.annotation;

/**
 * 绑定注解之间共享的常用值常量。
 *
 * @author Juergen Hoeller
 * @since 3.0.1
 */
public interface ValueConstants {

	/**
	 * 表示无默认值的常量 —— 作为 {@code null} 的替代，因为注解属性中无法使用
	 * {@code null}。
	 * <p>这是由 16 个 Unicode 字符组成的人为构造，其唯一目的就是永远不会
	 * 与用户声明的值匹配。
	 * @see RequestParam#defaultValue()
	 * @see RequestHeader#defaultValue()
	 * @see CookieValue#defaultValue()
	 */
	String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

}
