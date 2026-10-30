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

package org.springframework.test.annotation;

import org.springframework.lang.Nullable;

/**
 * <p>
 * 用于为给定测试环境检索<em>配置值</em>的
 * 策略接口。
 * </p>
 * <p>
 * 具体实现必须提供 {@code public} 无参
 * 构造函数。
 * </p>
 * <p>
 * Spring 提供以下开箱即用的实现：
 * </p>
 * <ul>
 * <li>{@link SystemProfileValueSource}</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see ProfileValueSourceConfiguration
 * @see IfProfileValue
 * @see ProfileValueUtils
 */
public interface ProfileValueSource {

	/**
	 * 获取由指定键指示的<em>配置值</em>。
	 * @param key <em>profile 值</em>的名称
	 * @return <em>profile 值</em>的 String 值，
	 * 如果该键没有对应的 <em>profile 值</em>，则为 {@code null}
	 */
	@Nullable
	String get(String key);

}
