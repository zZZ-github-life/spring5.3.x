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

package org.springframework.jmx.support;

import java.util.Hashtable;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * 用于创建 {@link javax.management.ObjectName} 实例的辅助类。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see javax.management.ObjectName#getInstance(String)
 */
public final class ObjectNameManager {

	private ObjectNameManager() {
	}


	/**
	 * 检索与所提供名称对应的 {@code ObjectName} 实例。
	 * @param objectName the {@code ObjectName} in {@code ObjectName} or
	 * {@code String} format
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String)
	 * @see ObjectName#getInstance(String)
	 */
	public static ObjectName getInstance(Object objectName) throws MalformedObjectNameException {
		if (objectName instanceof ObjectName) {
			return (ObjectName) objectName;
		}
		if (!(objectName instanceof String)) {
			throw new MalformedObjectNameException("Invalid ObjectName value type [" +
					objectName.getClass().getName() + "]: only ObjectName and String supported.");
		}
		return getInstance((String) objectName);
	}

	/**
	 * 检索与所提供名称对应的 {@code ObjectName} 实例。
	 * @param objectName the {@code ObjectName} in {@code String} format
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String)
	 * @see ObjectName#getInstance(String)
	 */
	public static ObjectName getInstance(String objectName) throws MalformedObjectNameException {
		return ObjectName.getInstance(objectName);
	}

	/**
	 * 为指定的域以及一个具有所提供键和值的属性检索 {@code ObjectName} 实例。
	 * @param domainName the domain name for the {@code ObjectName}
	 * @param key the key for the single property in the {@code ObjectName}
	 * @param value the value for the single property in the {@code ObjectName}
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String, String, String)
	 * @see ObjectName#getInstance(String, String, String)
	 */
	public static ObjectName getInstance(String domainName, String key, String value)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, key, value);
	}

	/**
	 * 检索具有指定域名和所提供键/名称属性的 {@code ObjectName} 实例。
	 * @param domainName the domain name for the {@code ObjectName}
	 * @param properties the properties for the {@code ObjectName}
	 * @return the {@code ObjectName} instance
	 * @throws MalformedObjectNameException in case of an invalid object name specification
	 * @see ObjectName#ObjectName(String, java.util.Hashtable)
	 * @see ObjectName#getInstance(String, java.util.Hashtable)
	 */
	public static ObjectName getInstance(String domainName, Hashtable<String, String> properties)
			throws MalformedObjectNameException {

		return ObjectName.getInstance(domainName, properties);
	}

}
