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

package org.springframework.core.io.support;

import java.util.Locale;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于加载本地化资源的辅助类，资源通过名称、扩展名和当前区域设置指定。
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 */
public class LocalizedResourceHelper {

	/** 用于分隔文件名各部分的默认分隔符：下划线。 */
	public static final String DEFAULT_SEPARATOR = "_";


	private final ResourceLoader resourceLoader;

	private String separator = DEFAULT_SEPARATOR;


	/**
	 * 使用 DefaultResourceLoader 创建一个新的 LocalizedResourceHelper。
	 * @see org.springframework.core.io.DefaultResourceLoader
	 */
	public LocalizedResourceHelper() {
		this.resourceLoader = new DefaultResourceLoader();
	}

	/**
	 * 使用给定的 ResourceLoader 创建一个新的 LocalizedResourceHelper。
	 * @param resourceLoader the ResourceLoader to use
	 */
	public LocalizedResourceHelper(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 设置用于文件名各部分之间的分隔符。默认为下划线（"_"）。
	 */
	public void setSeparator(@Nullable String separator) {
		this.separator = (separator != null ? separator : DEFAULT_SEPARATOR);
	}


	/**
	 * 为给定的名称、扩展名和区域设置查找最具体的本地化资源：
	 * <p>将按以下顺序搜索文件位置，类似于 {@code java.util.ResourceBundle} 的搜索顺序：
	 * <ul>
	 * <li>[name]_[language]_[country]_[variant][extension]
	 * <li>[name]_[language]_[country][extension]
	 * <li>[name]_[language][extension]
	 * <li>[name][extension]
	 * </ul>
	 * <p>如果找不到任何特定的文件，将返回默认位置的资源描述符。
	 * @param name the name of the file, without localization part nor extension
	 * @param extension the file extension (e.g. ".xls")
	 * @param locale the current locale (may be {@code null})
	 * @return the most specific localized resource found
	 * @see java.util.ResourceBundle
	 */
	public Resource findLocalizedResource(String name, String extension, @Nullable Locale locale) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(extension, "Extension must not be null");

		Resource resource = null;

		if (locale != null) {
			String lang = locale.getLanguage();
			String country = locale.getCountry();
			String variant = locale.getVariant();

			// Check for file with language, country and variant localization.
			if (variant.length() > 0) {
				String location =
						name + this.separator + lang + this.separator + country + this.separator + variant + extension;
				resource = this.resourceLoader.getResource(location);
			}

			// Check for file with language and country localization.
			if ((resource == null || !resource.exists()) && country.length() > 0) {
				String location = name + this.separator + lang + this.separator + country + extension;
				resource = this.resourceLoader.getResource(location);
			}

			// Check for document with language localization.
			if ((resource == null || !resource.exists()) && lang.length() > 0) {
				String location = name + this.separator + lang + extension;
				resource = this.resourceLoader.getResource(location);
			}
		}

		// Check for document without localization.
		if (resource == null || !resource.exists()) {
			String location = name + extension;
			resource = this.resourceLoader.getResource(location);
		}

		return resource;
	}

}
