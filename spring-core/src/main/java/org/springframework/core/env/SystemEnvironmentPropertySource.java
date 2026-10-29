/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link MapPropertySource} 的特化实现，设计用于
 * {@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}。
 * 它弥补了 Bash 及其他 shell 中的限制，这些 shell 不允许变量包含句点字符和/或连字符；
 * 同时还允许属性名使用大写变体，以便更符合 shell 的使用习惯。
 *
 * <p>例如，调用 {@code getProperty("foo.bar")} 将尝试查找原始属性或任何“等价”属性的值，
 * 并返回第一个找到的：
 * <ul>
 * <li>{@code foo.bar} - 原始名称</li>
 * <li>{@code foo_bar} - 用下划线替换句点（如果有）</li>
 * <li>{@code FOO.BAR} - 原始名称的大写形式</li>
 * <li>{@code FOO_BAR} - 使用下划线并大写</li>
 * </ul>
 * 上述属性的任何连字符变体也都可以，甚至可以混用点/连字符变体。
 *
 * <p>对 {@link #containsProperty(String)} 的调用也是如此，
 * 如果上述任一属性存在则返回 {@code true}，否则返回 {@code false}。
 *
 * <p>当把活动或默认 profile 指定为环境变量时，这一特性尤其有用。
 * 以下写法在 Bash 下是不允许的：
 *
 * <pre class="code">spring.profiles.active=p1 java -classpath ... MyApp</pre>
 *
 * 不过，以下语法是允许的，而且也更符合惯例：
 *
 * <pre class="code">SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp</pre>
 *
 * <p>为此类（或包）启用 debug 或 trace 级别的日志记录，
 * 以获取解释这些“属性名解析”何时发生的消息。
 *
 * <p>此属性源默认包含在 {@link StandardEnvironment} 及其所有子类中。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see StandardEnvironment
 * @see AbstractEnvironment#getSystemEnvironment()
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {

	/**
	 * 使用给定的名称创建一个新的 {@code SystemEnvironmentPropertySource}，
	 * 并委托给给定的 {@code MapPropertySource}。
	 */
	public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}


	/**
	 * 如果此属性源中存在具有给定名称或任何下划线/大写变体的属性，则返回 {@code true}。
	 */
	@Override
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * 如果此属性源中存在具有给定名称或任何下划线/大写变体的属性，此实现返回 {@code true}。
	 */
	@Override
	@Nullable
	public Object getProperty(String name) {
		String actualName = resolvePropertyName(name);
		if (logger.isDebugEnabled() && !name.equals(actualName)) {
			logger.debug("PropertySource '" + getName() + "' does not contain property '" + name +
					"', but found equivalent '" + actualName + "'");
		}
		return super.getProperty(actualName);
	}

	/**
	 * 检查此属性源是否包含具有给定名称的属性，或具有其任何下划线/大写变体。
	 * 如果找到则返回解析后的名称，否则返回原始名称。永远不会返回 {@code null}。
	 */
	protected final String resolvePropertyName(String name) {
		Assert.notNull(name, "Property name must not be null");
		String resolvedName = checkPropertyName(name);
		if (resolvedName != null) {
			return resolvedName;
		}
		String uppercasedName = name.toUpperCase();
		if (!name.equals(uppercasedName)) {
			resolvedName = checkPropertyName(uppercasedName);
			if (resolvedName != null) {
				return resolvedName;
			}
		}
		return name;
	}

	@Nullable
	private String checkPropertyName(String name) {
		// Check name as-is
		if (containsKey(name)) {
			return name;
		}
		// Check name with just dots replaced
		String noDotName = name.replace('.', '_');
		if (!name.equals(noDotName) && containsKey(noDotName)) {
			return noDotName;
		}
		// Check name with just hyphens replaced
		String noHyphenName = name.replace('-', '_');
		if (!name.equals(noHyphenName) && containsKey(noHyphenName)) {
			return noHyphenName;
		}
		// Check name with dots and hyphens replaced
		String noDotNoHyphenName = noDotName.replace('-', '_');
		if (!noDotName.equals(noDotNoHyphenName) && containsKey(noDotNoHyphenName)) {
			return noDotNoHyphenName;
		}
		// Give up
		return null;
	}

	private boolean containsKey(String name) {
		return (isSecurityManagerPresent() ? this.source.keySet().contains(name) : this.source.containsKey(name));
	}

	protected boolean isSecurityManagerPresent() {
		return (System.getSecurityManager() != null);
	}

}
