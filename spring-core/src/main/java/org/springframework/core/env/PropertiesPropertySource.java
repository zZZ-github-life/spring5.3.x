/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.env;

import java.util.Map;
import java.util.Properties;

/**
 * {@link PropertySource} 实现，从 {@link java.util.Properties} 对象中提取属性。
 *
 * <p>请注意，由于 {@code Properties} 对象在技术上是一个
 * {@code <Object, Object>} {@link java.util.Hashtable Hashtable}，
 * 它可以包含非 {@code String} 的键或值。不过，此实现仅限于访问基于 {@code String} 的键和值，
 * 与 {@link Properties#getProperty} 和 {@link Properties#setProperty} 的方式相同。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class PropertiesPropertySource extends MapPropertySource {

	@SuppressWarnings({"rawtypes", "unchecked"})
	public PropertiesPropertySource(String name, Properties source) {
		super(name, (Map) source);
	}

	protected PropertiesPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}


	@Override
	public String[] getPropertyNames() {
		synchronized (this.source) {
			return super.getPropertyNames();
		}
	}

}
