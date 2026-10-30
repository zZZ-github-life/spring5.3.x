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

package org.springframework.context.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertiesPersister;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * Spring 特有的 {@link org.springframework.context.MessageSource} 实现，使用指定的基名访问资源包，
 * 并参与 Spring {@link org.springframework.context.ApplicationContext} 的资源加载。
 *
 * <p>与基于 JDK 的 {@link ResourceBundleMessageSource} 不同，此类使用 {@link java.util.Properties}
 * 实例作为其消息的自定义数据结构，并通过 {@link org.springframework.util.PropertiesPersister} 策略
 * 从 Spring {@link Resource} 句柄加载它们。该策略不仅能根据时间戳变化重新加载文件，还能以指定的字
 * 符编码加载属性文件。它也会识别 XML 属性文件。
 *
 * <p>请注意，通过 {@link #setBasenames "basenames"} 属性设置的基名，其处理方式与
 * {@link ResourceBundleMessageSource} 的 "basenames" 属性略有不同。它遵循 ResourceBundle 的基本规
 * 则，不指定文件扩展名或语言代码，但可以指向任意 Spring 资源位置（而不仅限于类路径资源）。使用
 * "classpath:" 前缀时，资源仍可从类路径加载，但此时除 "-1"（永久缓存）之外的 "cacheSeconds" 值可能
 * 无法可靠工作。
 *
 * <p>对于典型的 Web 应用，消息文件可以放在 {@code WEB-INF} 中：例如基名 "WEB-INF/messages" 会找到
 * "WEB-INF/messages.properties"、"WEB-INF/messages_en.properties" 等组合，以及
 * "WEB-INF/messages.xml"、"WEB-INF/messages_en.xml" 等。请注意，由于按顺序查找，<i>前一个</i>资源
 * 包中的消息定义会覆盖后一个资源包中的定义。
 * <p>此 MessageSource 可以很容易地在 {@link org.springframework.context.ApplicationContext} 之外使
 * 用：它默认使用 {@link org.springframework.core.io.DefaultResourceLoader}，如果在上下文中运行，则
 * 会被 ApplicationContext 的资源加载器覆盖。它没有其他特定依赖。
 *
 * <p>感谢 Thomas Achleitner 提供了此消息源的初始实现！
 *
 * @author Juergen Hoeller
 * @see #setCacheSeconds
 * @see #setBasenames
 * @see #setDefaultEncoding
 * @see #setFileEncodings
 * @see #setPropertiesPersister
 * @see #setResourceLoader
 * @see ResourcePropertiesPersister
 * @see org.springframework.core.io.DefaultResourceLoader
 * @see ResourceBundleMessageSource
 * @see java.util.ResourceBundle
 */
public class ReloadableResourceBundleMessageSource extends AbstractResourceBasedMessageSource
		implements ResourceLoaderAware {

	private static final String PROPERTIES_SUFFIX = ".properties";

	private static final String XML_SUFFIX = ".xml";


	@Nullable
	private Properties fileEncodings;

	private boolean concurrentRefresh = true;

	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	// Cache to hold filename lists per Locale
	private final ConcurrentMap<String, Map<Locale, List<String>>> cachedFilenames = new ConcurrentHashMap<>();

	// Cache to hold already loaded properties per filename
	private final ConcurrentMap<String, PropertiesHolder> cachedProperties = new ConcurrentHashMap<>();

	// Cache to hold already loaded properties per filename
	private final ConcurrentMap<Locale, PropertiesHolder> cachedMergedProperties = new ConcurrentHashMap<>();


	/**
	 * 设置用于解析属性文件的按文件字符集。
	 * <p>仅适用于传统的属性文件，不适用于 XML 文件。
	 * @param fileEncodings 以文件名为键、字符集名称为值的 Properties。文件名必须符合基名语法，
	 * 可带可选的区域设置特定组成部分：例如 "WEB-INF/messages" 或 "WEB-INF/messages_en"。
	 * @see #setBasenames
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	public void setFileEncodings(Properties fileEncodings) {
		this.fileEncodings = fileEncodings;
	}

	/**
	 * 指定是否允许并发刷新行为，即一个线程在针对某个已缓存的属性文件进行刷新尝试时被锁定，而其他线程在
	 * 此期间继续返回旧的属性，直到刷新尝试完成。
	 * <p>默认值为 "true"：此行为自 Spring Framework 4.1 起新增，可最大限度减少线程之间的争用。如果你更
	 * 喜欢旧行为（即在刷新时完全阻塞），请将此标志切换为 "false"。
	 * @since 4.1
	 * @see #setCacheSeconds
	 */
	public void setConcurrentRefresh(boolean concurrentRefresh) {
		this.concurrentRefresh = concurrentRefresh;
	}

	/**
	 * 设置用于解析属性文件的 PropertiesPersister。
	 * <p>默认为 ResourcePropertiesPersister。
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}

	/**
	 * 设置用于加载资源包属性文件的 ResourceLoader。
	 * <p>默认为 DefaultResourceLoader。如果在上下文中运行，它会被 ApplicationContext 覆盖，因为它实现
	 * 了 ResourceLoaderAware 接口。在 ApplicationContext 之外运行时可以手动覆盖。
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.context.ResourceLoaderAware
	 */
	@Override
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
	}


	/**
	 * 在检索到的资源包文件中将给定消息代码作为键进行解析，并原样返回包中找到的值（不进行 MessageFormat
	 * 解析）。
	 */
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		if (getCacheMillis() < 0) {
			PropertiesHolder propHolder = getMergedProperties(locale);
			String result = propHolder.getProperty(code);
			if (result != null) {
				return result;
			}
		}
		else {
			for (String basename : getBasenameSet()) {
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					PropertiesHolder propHolder = getProperties(filename);
					String result = propHolder.getProperty(code);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 在检索到的资源包文件中将给定消息代码作为键进行解析，并为每个消息代码使用一个缓存的 MessageFormat
	 * 实例。
	 */
	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		if (getCacheMillis() < 0) {
			PropertiesHolder propHolder = getMergedProperties(locale);
			MessageFormat result = propHolder.getMessageFormat(code, locale);
			if (result != null) {
				return result;
			}
		}
		else {
			for (String basename : getBasenameSet()) {
				List<String> filenames = calculateAllFilenames(basename, locale);
				for (String filename : filenames) {
					PropertiesHolder propHolder = getProperties(filename);
					MessageFormat result = propHolder.getMessageFormat(code, locale);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}


	/**
	 * 获取一个 PropertiesHolder，其中包含合并所有指定资源包后对某个 Locale 实际可见的属性。该 holder
	 * 要么从缓存中获取，要么重新加载。
	 * <p>仅在永久缓存资源包内容时使用，即 cacheSeconds &lt; 0 的情况。因此，合并后的属性总是被永久缓存。
	 */
	protected PropertiesHolder getMergedProperties(Locale locale) {
		PropertiesHolder mergedHolder = this.cachedMergedProperties.get(locale);
		if (mergedHolder != null) {
			return mergedHolder;
		}

		Properties mergedProps = newProperties();
		long latestTimestamp = -1;
		String[] basenames = StringUtils.toStringArray(getBasenameSet());
		for (int i = basenames.length - 1; i >= 0; i--) {
			List<String> filenames = calculateAllFilenames(basenames[i], locale);
			for (int j = filenames.size() - 1; j >= 0; j--) {
				String filename = filenames.get(j);
				PropertiesHolder propHolder = getProperties(filename);
				if (propHolder.getProperties() != null) {
					mergedProps.putAll(propHolder.getProperties());
					if (propHolder.getFileTimestamp() > latestTimestamp) {
						latestTimestamp = propHolder.getFileTimestamp();
					}
				}
			}
		}

		mergedHolder = new PropertiesHolder(mergedProps, latestTimestamp);
		PropertiesHolder existing = this.cachedMergedProperties.putIfAbsent(locale, mergedHolder);
		if (existing != null) {
			mergedHolder = existing;
		}
		return mergedHolder;
	}

	/**
	 * 计算给定资源包基名和 Locale 的所有文件名。将计算给定 Locale、系统 Locale（如果适用）以及默认文件
	 * 的文件名。
	 * @param basename 资源包的基名
	 * @param locale 该 Locale
	 * @return 要检查的文件名 List
	 * @see #setFallbackToSystemLocale
	 * @see #calculateFilenamesForLocale
	 */
	protected List<String> calculateAllFilenames(String basename, Locale locale) {
		Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
		if (localeMap != null) {
			List<String> filenames = localeMap.get(locale);
			if (filenames != null) {
				return filenames;
			}
		}

		// Filenames for given Locale
		List<String> filenames = new ArrayList<>(7);
		filenames.addAll(calculateFilenamesForLocale(basename, locale));

		// Filenames for default Locale, if any
		Locale defaultLocale = getDefaultLocale();
		if (defaultLocale != null && !defaultLocale.equals(locale)) {
			List<String> fallbackFilenames = calculateFilenamesForLocale(basename, defaultLocale);
			for (String fallbackFilename : fallbackFilenames) {
				if (!filenames.contains(fallbackFilename)) {
					// Entry for fallback locale that isn't already in filenames list.
					filenames.add(fallbackFilename);
				}
			}
		}

		// Filename for default bundle file
		filenames.add(basename);

		if (localeMap == null) {
			localeMap = new ConcurrentHashMap<>();
			Map<Locale, List<String>> existing = this.cachedFilenames.putIfAbsent(basename, localeMap);
			if (existing != null) {
				localeMap = existing;
			}
		}
		localeMap.put(locale, filenames);
		return filenames;
	}

	/**
	 * 计算给定资源包基名和 Locale 的文件名，依次附加语言代码、国家/地区代码和变体代码。
	 * <p>例如，基名 "messages"、Locale "de_AT_oo" &rarr; "messages_de_AT_OO"、"messages_de_AT"、
	 * "messages_de"。
	 * <p>遵循 {@link java.util.Locale#toString()} 定义的规则。
	 * @param basename 资源包的基名
	 * @param locale 该 Locale
	 * @return 要检查的文件名 List
	 */
	protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
		List<String> result = new ArrayList<>(3);
		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();
		StringBuilder temp = new StringBuilder(basename);

		temp.append('_');
		if (language.length() > 0) {
			temp.append(language);
			result.add(0, temp.toString());
		}

		temp.append('_');
		if (country.length() > 0) {
			temp.append(country);
			result.add(0, temp.toString());
		}

		if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
			temp.append('_').append(variant);
			result.add(0, temp.toString());
		}

		return result;
	}


	/**
	 * 获取给定文件名的 PropertiesHolder，或来自缓存，或重新加载。
	 * @param filename 资源包文件名（基名 + Locale）
	 * @return 该资源包当前的 PropertiesHolder
	 */
	protected PropertiesHolder getProperties(String filename) {
		PropertiesHolder propHolder = this.cachedProperties.get(filename);
		long originalTimestamp = -2;

		if (propHolder != null) {
			originalTimestamp = propHolder.getRefreshTimestamp();
			if (originalTimestamp == -1 || originalTimestamp > System.currentTimeMillis() - getCacheMillis()) {
				// Up to date
				return propHolder;
			}
		}
		else {
			propHolder = new PropertiesHolder();
			PropertiesHolder existingHolder = this.cachedProperties.putIfAbsent(filename, propHolder);
			if (existingHolder != null) {
				propHolder = existingHolder;
			}
		}

		// At this point, we need to refresh...
		if (this.concurrentRefresh && propHolder.getRefreshTimestamp() >= 0) {
			// A populated but stale holder -> could keep using it.
			if (!propHolder.refreshLock.tryLock()) {
				// Getting refreshed by another thread already ->
				// let's return the existing properties for the time being.
				return propHolder;
			}
		}
		else {
			propHolder.refreshLock.lock();
		}
		try {
			PropertiesHolder existingHolder = this.cachedProperties.get(filename);
			if (existingHolder != null && existingHolder.getRefreshTimestamp() > originalTimestamp) {
				return existingHolder;
			}
			return refreshProperties(filename, propHolder);
		}
		finally {
			propHolder.refreshLock.unlock();
		}
	}

	/**
	 * 刷新给定资源包文件名的 PropertiesHolder。如果之前未缓存，该 holder 可以为 {@code null}，也可以是
	 * 已超时的缓存条目（可能会根据当前的最后修改时间戳重新校验）。
	 * @param filename 资源包文件名（基名 + Locale）
	 * @param propHolder 该资源包当前的 PropertiesHolder
	 */
	protected PropertiesHolder refreshProperties(String filename, @Nullable PropertiesHolder propHolder) {
		long refreshTimestamp = (getCacheMillis() < 0 ? -1 : System.currentTimeMillis());

		Resource resource = this.resourceLoader.getResource(filename + PROPERTIES_SUFFIX);
		if (!resource.exists()) {
			resource = this.resourceLoader.getResource(filename + XML_SUFFIX);
		}

		if (resource.exists()) {
			long fileTimestamp = -1;
			if (getCacheMillis() >= 0) {
				// Last-modified timestamp of file will just be read if caching with timeout.
				try {
					fileTimestamp = resource.lastModified();
					if (propHolder != null && propHolder.getFileTimestamp() == fileTimestamp) {
						if (logger.isDebugEnabled()) {
							logger.debug("Re-caching properties for filename [" + filename + "] - file hasn't been modified");
						}
						propHolder.setRefreshTimestamp(refreshTimestamp);
						return propHolder;
					}
				}
				catch (IOException ex) {
					// Probably a class path resource: cache it forever.
					if (logger.isDebugEnabled()) {
						logger.debug(resource + " could not be resolved in the file system - assuming that it hasn't changed", ex);
					}
					fileTimestamp = -1;
				}
			}
			try {
				Properties props = loadProperties(resource, filename);
				propHolder = new PropertiesHolder(props, fileTimestamp);
			}
			catch (IOException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Could not parse properties file [" + resource.getFilename() + "]", ex);
				}
				// Empty holder representing "not valid".
				propHolder = new PropertiesHolder();
			}
		}

		else {
			// Resource does not exist.
			if (logger.isDebugEnabled()) {
				logger.debug("No properties file found for [" + filename + "] - neither plain properties nor XML");
			}
			// Empty holder representing "not found".
			propHolder = new PropertiesHolder();
		}

		propHolder.setRefreshTimestamp(refreshTimestamp);
		this.cachedProperties.put(filename, propHolder);
		return propHolder;
	}

	/**
	 * 从给定资源加载属性。
	 * @param resource 要从中加载的资源
	 * @param filename 原始资源包文件名（基名 + Locale）
	 * @return 填充后的 Properties 实例
	 * @throws IOException 如果属性加载失败
	 */
	protected Properties loadProperties(Resource resource, String filename) throws IOException {
		Properties props = newProperties();
		try (InputStream is = resource.getInputStream()) {
			String resourceFilename = resource.getFilename();
			if (resourceFilename != null && resourceFilename.endsWith(XML_SUFFIX)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Loading properties [" + resource.getFilename() + "]");
				}
				this.propertiesPersister.loadFromXml(props, is);
			}
			else {
				String encoding = null;
				if (this.fileEncodings != null) {
					encoding = this.fileEncodings.getProperty(filename);
				}
				if (encoding == null) {
					encoding = getDefaultEncoding();
				}
				if (encoding != null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "] with encoding '" + encoding + "'");
					}
					this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
				}
				else {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading properties [" + resource.getFilename() + "]");
					}
					this.propertiesPersister.load(props, is);
				}
			}
			return props;
		}
	}

	/**
	 * 用于创建普通的新 {@link Properties} 实例的模板方法。默认实现只是调用
	 * {@link Properties#Properties()}。
	 * <p>允许在子类中返回自定义的 {@link Properties} 扩展。覆盖方法只需实例化一个自定义的
	 * {@link Properties} 子类，此时不应执行进一步的初始化或填充。
	 * @return 一个普通的 Properties 实例
	 * @since 4.2
	 */
	protected Properties newProperties() {
		return new Properties();
	}


	/**
	 * 清除资源包缓存。之后的解析调用将重新加载属性文件。
	 */
	public void clearCache() {
		logger.debug("Clearing entire resource bundle cache");
		this.cachedProperties.clear();
		this.cachedMergedProperties.clear();
	}

	/**
	 * 清除此 MessageSource 及其所有祖先的资源包缓存。
	 * @see #clearCache
	 */
	public void clearCacheIncludingAncestors() {
		clearCache();
		if (getParentMessageSource() instanceof ReloadableResourceBundleMessageSource) {
			((ReloadableResourceBundleMessageSource) getParentMessageSource()).clearCacheIncludingAncestors();
		}
	}


	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * 用于缓存的 PropertiesHolder。存储源文件的最后修改时间戳以便高效检测变化，以及上次刷新尝试的时间
	 * 戳（每次重新校验缓存条目时更新）。
	 */
	protected class PropertiesHolder {

		@Nullable
		private final Properties properties;

		private final long fileTimestamp;

		private volatile long refreshTimestamp = -2;

		private final ReentrantLock refreshLock = new ReentrantLock();

		/** 用于保存每个消息码已生成的 MessageFormat 的缓存。 */
		private final ConcurrentMap<String, Map<Locale, MessageFormat>> cachedMessageFormats =
				new ConcurrentHashMap<>();

		public PropertiesHolder() {
			this.properties = null;
			this.fileTimestamp = -1;
		}

		public PropertiesHolder(Properties properties, long fileTimestamp) {
			this.properties = properties;
			this.fileTimestamp = fileTimestamp;
		}

		@Nullable
		public Properties getProperties() {
			return this.properties;
		}

		public long getFileTimestamp() {
			return this.fileTimestamp;
		}

		public void setRefreshTimestamp(long refreshTimestamp) {
			this.refreshTimestamp = refreshTimestamp;
		}

		public long getRefreshTimestamp() {
			return this.refreshTimestamp;
		}

		@Nullable
		public String getProperty(String code) {
			if (this.properties == null) {
				return null;
			}
			return this.properties.getProperty(code);
		}

		@Nullable
		public MessageFormat getMessageFormat(String code, Locale locale) {
			if (this.properties == null) {
				return null;
			}
			Map<Locale, MessageFormat> localeMap = this.cachedMessageFormats.get(code);
			if (localeMap != null) {
				MessageFormat result = localeMap.get(locale);
				if (result != null) {
					return result;
				}
			}
			String msg = this.properties.getProperty(code);
			if (msg != null) {
				if (localeMap == null) {
					localeMap = new ConcurrentHashMap<>();
					Map<Locale, MessageFormat> existing = this.cachedMessageFormats.putIfAbsent(code, localeMap);
					if (existing != null) {
						localeMap = existing;
					}
				}
				MessageFormat result = createMessageFormat(msg, locale);
				localeMap.put(locale, result);
				return result;
			}
			return null;
		}
	}

}
