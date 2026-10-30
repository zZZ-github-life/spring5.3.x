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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

/**
 * {@link PropertyResolver} 实现，针对底层的一组 {@link PropertySources} 解析属性值。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 * @see PropertySources
 * @see AbstractEnvironment
 */
public class PropertySourcesPropertyResolver extends AbstractPropertyResolver {

	@Nullable
	private final PropertySources propertySources;


	/**
	 * 针对给定的属性源创建一个新的解析器。
	 * @param propertySources 要使用的 {@link PropertySource} 对象集合
	 */
	public PropertySourcesPropertyResolver(@Nullable PropertySources propertySources) {
		this.propertySources = propertySources;
	}


	@Override
	public boolean containsProperty(String key) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (propertySource.containsProperty(key)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		return getProperty(key, String.class, true);
	}

	@Override
	@Nullable
	public <T> T getProperty(String key, Class<T> targetValueType) {
		return getProperty(key, targetValueType, true);
	}

	@Override
	@Nullable
	protected String getPropertyAsRawString(String key) {
		return getProperty(key, String.class, false);
	}

	@Nullable
	protected <T> T getProperty(String key, Class<T> targetValueType, boolean resolveNestedPlaceholders) {
		if (this.propertySources != null) {
			for (PropertySource<?> propertySource : this.propertySources) {
				if (logger.isTraceEnabled()) {
					logger.trace("Searching for key '" + key + "' in PropertySource '" +
							propertySource.getName() + "'");
				}
				Object value = propertySource.getProperty(key);
				if (value != null) {
					if (resolveNestedPlaceholders && value instanceof String) {
						value = resolveNestedPlaceholders((String) value);
					}
					logKeyFound(key, propertySource, value);
					return convertValueIfNecessary(value, targetValueType);
				}
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Could not find key '" + key + "' in any property source");
		}
		return null;
	}

	/**
	 * 记录在给定 {@link PropertySource} 中找到给定键并由此得到给定值。
	 * <p>默认实现会写入包含键和源的 debug 日志消息。
	 * 从 4.3.3 起，它不再记录值，以避免意外记录敏感设置。
	 * 子类可以重写此方法以更改日志级别和/或日志消息，包括在需要时记录属性值。
	 * @param key 找到的键
	 * @param propertySource 在其中找到该键的 {@code PropertySource}
	 * @param value 对应的值
	 * @since 4.3.1
	 */
	protected void logKeyFound(String key, PropertySource<?> propertySource, Object value) {
		if (logger.isDebugEnabled()) {
			logger.debug("Found key '" + key + "' in PropertySource '" + propertySource.getName() +
					"' with value of type " + value.getClass().getSimpleName());
		}
	}

}
