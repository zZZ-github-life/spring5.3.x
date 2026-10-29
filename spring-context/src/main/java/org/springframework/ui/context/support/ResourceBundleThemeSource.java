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

package org.springframework.ui.context.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

/**
 * {@link ThemeSource} 实现，为每个主题查找单独的 {@link java.util.ResourceBundle}。主题名称被解释
 * 为 ResourceBundle 的基名，并支持为所有主题设置公共的基名前缀。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see #setBasenamePrefix
 * @see java.util.ResourceBundle
 * @see org.springframework.context.support.ResourceBundleMessageSource
 */
public class ResourceBundleThemeSource implements HierarchicalThemeSource, BeanClassLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ThemeSource parentThemeSource;

	private String basenamePrefix = "";

	@Nullable
	private String defaultEncoding;

	@Nullable
	private Boolean fallbackToSystemLocale;

	@Nullable
	private ClassLoader beanClassLoader;

	/** 从主题名称到 Theme 实例的 Map。 */
	private final Map<String, Theme> themeCache = new ConcurrentHashMap<>();


	@Override
	public void setParentThemeSource(@Nullable ThemeSource parent) {
		this.parentThemeSource = parent;

		// Update existing Theme objects.
		// Usually there shouldn't be any at the time of this call.
		synchronized (this.themeCache) {
			for (Theme theme : this.themeCache.values()) {
				initParent(theme);
			}
		}
	}

	@Override
	@Nullable
	public ThemeSource getParentThemeSource() {
		return this.parentThemeSource;
	}

	/**
	 * 设置应用于 ResourceBundle 基名（即主题名称）的前缀。例如：basenamePrefix="test."、
	 * themeName="theme" &rarr; basename="test.theme"。<p>请注意，ResourceBundle 名称实际上是类路径位置：
	 * 因此，JDK 标准的 ResourceBundle 会将点号视为包分隔符。这意味着 "test.theme" 实际上等同于
	 * "test/theme"，就像以编程方式使用 {@code java.util.ResourceBundle} 时一样。
	 * @see java.util.ResourceBundle#getBundle(String)
	 */
	public void setBasenamePrefix(@Nullable String basenamePrefix) {
		this.basenamePrefix = (basenamePrefix != null ? basenamePrefix : "");
	}

	/**
	 * 设置解析资源包文件时使用的默认字符集。<p>{@link ResourceBundleMessageSource} 的默认值是
	 * {@code java.util.ResourceBundle} 的默认编码：ISO-8859-1。
	 * @since 4.2
	 * @see ResourceBundleMessageSource#setDefaultEncoding
	 */
	public void setDefaultEncoding(@Nullable String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 设置当未找到针对特定 Locale 的文件时，是否回退到系统 Locale。<p>
	 * {@link ResourceBundleMessageSource} 的默认值为 "true"。
	 * @since 4.2
	 * @see ResourceBundleMessageSource#setFallbackToSystemLocale
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 此实现返回一个 SimpleTheme 实例，其中持有基于 ResourceBundle 的 MessageSource，其基名与给定主题
	 * 名称相对应（由配置的 "basenamePrefix" 添加前缀）。<p>SimpleTheme 实例按主题名称缓存。如果主题需
	 * 要反映底层文件的变化，请使用可重新加载的 MessageSource。
	 * @see #setBasenamePrefix
	 * @see #createMessageSource
	 */
	@Override
	@Nullable
	public Theme getTheme(String themeName) {
		Theme theme = this.themeCache.get(themeName);
		if (theme == null) {
			synchronized (this.themeCache) {
				theme = this.themeCache.get(themeName);
				if (theme == null) {
					String basename = this.basenamePrefix + themeName;
					MessageSource messageSource = createMessageSource(basename);
					theme = new SimpleTheme(themeName, messageSource);
					initParent(theme);
					this.themeCache.put(themeName, theme);
					if (logger.isDebugEnabled()) {
						logger.debug("Theme created: name '" + themeName + "', basename [" + basename + "]");
					}
				}
			}
		}
		return theme;
	}

	/**
	 * 为给定基名创建 MessageSource，用作相应主题的 MessageSource。<p>默认实现为给定基名创建
	 * ResourceBundleMessageSource。子类例如可以创建经过专门配置的
	 * ReloadableResourceBundleMessageSource。
	 * @param basename the basename to create a MessageSource for
	 * @return the MessageSource
	 * @see org.springframework.context.support.ResourceBundleMessageSource
	 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
	 */
	protected MessageSource createMessageSource(String basename) {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(basename);
		if (this.defaultEncoding != null) {
			messageSource.setDefaultEncoding(this.defaultEncoding);
		}
		if (this.fallbackToSystemLocale != null) {
			messageSource.setFallbackToSystemLocale(this.fallbackToSystemLocale);
		}
		if (this.beanClassLoader != null) {
			messageSource.setBeanClassLoader(this.beanClassLoader);
		}
		return messageSource;
	}

	/**
	 * 使用此 ThemeSource 相应父级的 MessageSource 初始化给定主题的 MessageSource。
	 * @param theme the Theme to (re-)initialize
	 */
	protected void initParent(Theme theme) {
		if (theme.getMessageSource() instanceof HierarchicalMessageSource) {
			HierarchicalMessageSource messageSource = (HierarchicalMessageSource) theme.getMessageSource();
			if (getParentThemeSource() != null && messageSource.getParentMessageSource() == null) {
				Theme parentTheme = getParentThemeSource().getTheme(theme.getName());
				if (parentTheme != null) {
					messageSource.setParentMessageSource(parentTheme.getMessageSource());
				}
			}
		}
	}

}
