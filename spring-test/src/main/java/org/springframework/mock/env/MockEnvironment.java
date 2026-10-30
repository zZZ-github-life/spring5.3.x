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

package org.springframework.mock.env;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 用于测试目的的简单 {@link ConfigurableEnvironment} 实现，
 * 暴露 {@link #setProperty(String, String)} 和 {@link #withProperty(String, String)} 方法。
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.2
 * @see org.springframework.mock.env.MockPropertySource
 */
public class MockEnvironment extends AbstractEnvironment {

	private MockPropertySource propertySource = new MockPropertySource();

	/**
	 * 使用单个 {@link MockPropertySource} 创建一个新的 {@code MockEnvironment}。
	 */
	public MockEnvironment() {
		getPropertySources().addLast(this.propertySource);
	}

	/**
	 * 为此环境的底层 {@link MockPropertySource} 设置属性。
	 */
	public void setProperty(String key, String value) {
		this.propertySource.setProperty(key, value);
	}

	/**
	 * {@link #setProperty} 的便捷同义词，返回当前实例。
	 * 适用于方法链和流式风格的使用。
	 * @return 该 {@link MockEnvironment} 实例
	 * @see MockPropertySource#withProperty
	 */
	public MockEnvironment withProperty(String key, String value) {
		this.setProperty(key, value);
		return this;
	}

}
