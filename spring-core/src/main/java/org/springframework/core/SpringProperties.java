/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.springframework.lang.Nullable;

/**
 * 本地 Spring 属性的静态持有者，即在 Spring 库级别定义的属性。
 *
 * <p>从 Spring 库类路径的根目录读取 {@code spring.properties} 文件，还允许通过
 * {@link #setProperty} 以编程方式设置属性。检查属性时，先检查本地条目，然后通过
 * {@link System#getProperty} 回退到 JVM 级别的系统属性进行检查。
 *
 * <p>这是设置 Spring 相关系统属性（例如 "spring.getenv.ignore" 和 "spring.beaninfo.ignore"）的另一
 * 种方式，特别适用于目标平台上 JVM 系统属性被锁定的场景（例如 WebSphere）。有关在本地将此类标志设
 * 置为 "true" 的便捷方式，请参见 {@link #setFlag}。
 *
 * @author Juergen Hoeller
 * @since 3.2.7
 * @see org.springframework.beans.CachedIntrospectionResults#IGNORE_BEANINFO_PROPERTY_NAME
 * @see org.springframework.context.index.CandidateComponentsIndexLoader#IGNORE_INDEX
 * @see org.springframework.core.env.AbstractEnvironment#IGNORE_GETENV_PROPERTY_NAME
 * @see org.springframework.expression.spel.SpelParserConfiguration#SPRING_EXPRESSION_COMPILER_MODE_PROPERTY_NAME
 * @see org.springframework.jdbc.core.StatementCreatorUtils#IGNORE_GETPARAMETERTYPE_PROPERTY_NAME
 * @see org.springframework.jndi.JndiLocatorDelegate#IGNORE_JNDI_PROPERTY_NAME
 * @see org.springframework.objenesis.SpringObjenesis#IGNORE_OBJENESIS_PROPERTY_NAME
 * @see org.springframework.test.context.NestedTestConfiguration#ENCLOSING_CONFIGURATION_PROPERTY_NAME
 * @see org.springframework.test.context.TestConstructor#TEST_CONSTRUCTOR_AUTOWIRE_MODE_PROPERTY_NAME
 * @see org.springframework.test.context.cache.ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
 */
public final class SpringProperties {

	private static final String PROPERTIES_RESOURCE_LOCATION = "spring.properties";

	private static final Properties localProperties = new Properties();


	static {
		try {
			ClassLoader cl = SpringProperties.class.getClassLoader();
			URL url = (cl != null ? cl.getResource(PROPERTIES_RESOURCE_LOCATION) :
					ClassLoader.getSystemResource(PROPERTIES_RESOURCE_LOCATION));
			if (url != null) {
				try (InputStream is = url.openStream()) {
					localProperties.load(is);
				}
			}
		}
		catch (IOException ex) {
			System.err.println("Could not load 'spring.properties' file from local classpath: " + ex);
		}
	}


	private SpringProperties() {
	}


	/**
	 * 以编程方式设置本地属性，覆盖 {@code spring.properties} 文件中的条目（如果有）。
	 * @param key the property key
	 * @param value the associated property value, or {@code null} to reset it
	 */
	public static void setProperty(String key, @Nullable String value) {
		if (value != null) {
			localProperties.setProperty(key, value);
		}
		else {
			localProperties.remove(key);
		}
	}

	/**
	 * 获取给定键的属性值，先检查本地 Spring 属性，然后回退到 JVM 级别的系统属性。
	 * @param key the property key
	 * @return the associated property value, or {@code null} if none found
	 */
	@Nullable
	public static String getProperty(String key) {
		String value = localProperties.getProperty(key);
		if (value == null) {
			try {
				value = System.getProperty(key);
			}
			catch (Throwable ex) {
				System.err.println("Could not retrieve system property '" + key + "': " + ex);
			}
		}
		return value;
	}

	/**
	 * 以编程方式将本地标志设置为 "true"，覆盖 {@code spring.properties} 文件中的条目（如果有）。
	 * @param key the property key
	 */
	public static void setFlag(String key) {
		localProperties.put(key, Boolean.TRUE.toString());
	}

	/**
	 * 获取给定属性键的标志。
	 * @param key the property key
	 * @return {@code true} if the property is set to "true",
	 * {@code} false otherwise
	 */
	public static boolean getFlag(String key) {
		return Boolean.parseBoolean(getProperty(key));
	}

}
