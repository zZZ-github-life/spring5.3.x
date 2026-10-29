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

package org.springframework.test.annotation;

import org.springframework.util.Assert;

/**
 * 使用系统属性作为底层来源的 {@link ProfileValueSource} 实现。
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 */
public final class SystemProfileValueSource implements ProfileValueSource {

	private static final SystemProfileValueSource INSTANCE = new SystemProfileValueSource();


	/**
	 * 获取此 ProfileValueSource 的规范实例。
	 */
	public static final SystemProfileValueSource getInstance() {
		return INSTANCE;
	}


	/**
	 * 私有构造函数，强制实施单例模式。
	 */
	private SystemProfileValueSource() {
	}

	/**
	 * 从系统属性中获取由指定键指示的
	 * <em>配置值</em>。
	 * @see System#getProperty(String)
	 */
	@Override
	public String get(String key) {
		Assert.hasText(key, "'key' must not be empty");
		return System.getProperty(key);
	}

}
