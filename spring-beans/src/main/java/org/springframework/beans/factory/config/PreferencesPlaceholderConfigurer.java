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

package org.springframework.beans.factory.config;

import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * PropertyPlaceholderConfigurer 的子类，支持 JDK 1.4 的 Preferences API（{@code java.util.prefs}）。
 *
 *
 * <p>先尝试在用户首选项中、然后在系统首选项中、最后在此配置器的属性中将占位符作为键来解析。因此，
 * 如果未定义相应的首选项，其行为与 PropertyPlaceholderConfigurer 类似。
 *
 * <p>支持为系统和用户首选项树指定自定义路径。也支持在占位符中指定自定义路径
 * （"myPath/myPlaceholderKey"）。如果未指定，则使用各自的根节点。
 *
 * @author Juergen Hoeller
 * @since 16.02.2004
 * @see #setSystemTreePath
 * @see #setUserTreePath
 * @see java.util.prefs.Preferences
 * @deprecated as of 5.2, along with {@link PropertyPlaceholderConfigurer}
 */
@Deprecated
public class PreferencesPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements InitializingBean {

	@Nullable
	private String systemTreePath;

	@Nullable
	private String userTreePath;

	private Preferences systemPrefs = Preferences.systemRoot();

	private Preferences userPrefs = Preferences.userRoot();


	/**
	 * 设置系统首选项树中用于解析占位符的路径。默认为根节点。
	 */
	public void setSystemTreePath(String systemTreePath) {
		this.systemTreePath = systemTreePath;
	}

	/**
	 * 设置系统首选项树中用于解析占位符的路径。默认为根节点。
	 */
	public void setUserTreePath(String userTreePath) {
		this.userTreePath = userTreePath;
	}


	/**
	 * 此实现会急切地获取所需系统与用户树节点的 Preferences 实例。
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.systemTreePath != null) {
			this.systemPrefs = this.systemPrefs.node(this.systemTreePath);
		}
		if (this.userTreePath != null) {
			this.userPrefs = this.userPrefs.node(this.userTreePath);
		}
	}

	/**
	 * 此实现先尝试在用户首选项中、然后在系统首选项中、最后在传入的属性中将占位符作为键来解析。
	 */
	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {
		String path = null;
		String key = placeholder;
		int endOfPath = placeholder.lastIndexOf('/');
		if (endOfPath != -1) {
			path = placeholder.substring(0, endOfPath);
			key = placeholder.substring(endOfPath + 1);
		}
		String value = resolvePlaceholder(path, key, this.userPrefs);
		if (value == null) {
			value = resolvePlaceholder(path, key, this.systemPrefs);
			if (value == null) {
				value = props.getProperty(placeholder);
			}
		}
		return value;
	}

	/**
	 * 针对给定的 Preferences 解析给定的路径和键。
	 * @param path the preferences path (placeholder part before '/')
	 * @param key the preferences key (placeholder part after '/')
	 * @param preferences the Preferences to resolve against
	 * @return the value for the placeholder, or {@code null} if none found
	 */
	@Nullable
	protected String resolvePlaceholder(@Nullable String path, String key, Preferences preferences) {
		if (path != null) {
			// Do not create the node if it does not exist...
			try {
				if (preferences.nodeExists(path)) {
					return preferences.node(path).get(key, null);
				}
				else {
					return null;
				}
			}
			catch (BackingStoreException ex) {
				throw new BeanDefinitionStoreException("Cannot access specified node path [" + path + "]", ex);
			}
		}
		else {
			return preferences.get(key, null);
		}
	}

}
