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

package org.springframework.mock.env;

import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * 用于测试的简单 {@link PropertySource} 实现。接受
 * 用户提供的 {@link Properties} 对象；如果在构造时省略，
 * 该实现将自行初始化一个。
 *
 * 为方便起见，暴露了 {@link #setProperty} 和 {@link #withProperty} 方法，
 * 例如：
 * <pre class="code">
 * {@code
 *   PropertySource<?> source = new MockPropertySource().withProperty("foo", "bar");
 * }
 * </pre>
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.mock.env.MockEnvironment
 */
public class MockPropertySource extends PropertiesPropertySource {

	/**
	 * {@value} 是未另行显式指定名称的 {@link MockPropertySource}
	 * 实例的默认名称。
	 * @see #MockPropertySource()
	 * @see #MockPropertySource(String)
	 */
	public static final String MOCK_PROPERTIES_PROPERTY_SOURCE_NAME = "mockProperties";

	/**
	 * 创建一个名为 {@value #MOCK_PROPERTIES_PROPERTY_SOURCE_NAME} 的新 {@code MockPropertySource}，
	 * 它将维护自己内部的 {@link Properties} 实例。
	 */
	public MockPropertySource() {
		this(new Properties());
	}

	/**
	 * 使用给定名称创建一个新的 {@code MockPropertySource}，
	 * 它将维护自己内部的 {@link Properties} 实例。
	 * @param name 属性源的 {@linkplain #getName() name}
	 */
	public MockPropertySource(String name) {
		this(name, new Properties());
	}

	/**
	 * 创建一个名为 {@value #MOCK_PROPERTIES_PROPERTY_SOURCE_NAME} 的新 {@code MockPropertySource}，
	 * 并由给定的 {@link Properties} 对象提供支持。
	 * @param properties 要使用的属性
	 */
	public MockPropertySource(Properties properties) {
		this(MOCK_PROPERTIES_PROPERTY_SOURCE_NAME, properties);
	}

	/**
	 * 使用给定名称创建一个新的 {@code MockPropertySource}，并由给定的
	 * {@link Properties} 对象提供支持。
	 * @param name 属性源的 {@linkplain #getName() name}
	 * @param properties 要使用的属性
	 */
	public MockPropertySource(String name, Properties properties) {
		super(name, properties);
	}

	/**
	 * 在底层 {@link Properties} 对象上设置给定属性。
	 */
	public void setProperty(String name, Object value) {
		this.source.put(name, value);
	}

	/**
	 * {@link #setProperty} 的便捷同义词，返回当前实例。
	 * 适用于方法链和流式风格的使用。
	 * @return 该 {@link MockPropertySource} 实例
	 */
	public MockPropertySource withProperty(String name, Object value) {
		this.setProperty(name, value);
		return this;
	}

}
