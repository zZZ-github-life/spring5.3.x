/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.context.MessageSource} 的实现，使用指定的基名访问资源包。此类依赖底层
 * JDK 的 {@link java.util.ResourceBundle} 实现，并结合由 {@link java.text.MessageFormat} 提供的
 * JDK 标准消息解析功能。
 *
 * <p>此 MessageSource 既缓存访问过的 ResourceBundle 实例，也缓存为每条消息生成的 MessageFormat。它
 * 还实现了如 AbstractMessageSource 基类所支持的那样，在不使用 MessageFormat 的情况下渲染无参消息。
 * 此 MessageSource 提供的缓存明显快于 {@code java.util.ResourceBundle} 类的内置缓存。
 *
 * <p>基名遵循 {@link java.util.ResourceBundle} 的约定：基本上就是一个完全限定的类路径位置。如果它
 * 不包含包限定符（如 {@code org.mypackage}），则会从类路径根目录解析。请注意，JDK 的标准
 * ResourceBundle 将点号视为包分隔符：这意味着 "test.theme" 实际上等价于 "test/theme"。
 *
 * <p>在类路径上，资源包将使用本地配置的 {@link #setDefaultEncoding encoding} 读取：默认为
 * ISO-8859-1；可考虑将其改为 UTF-8，或改为 {@code null} 以使用平台默认编码。在 JDK 9+ 模块路径上，
 * 如果本地提供的 {@code ResourceBundle.Control} 句柄不受支持，此 MessageSource 总是回退到使用平台
 * 默认编码的 {@link ResourceBundle#getBundle} 检索方式：在 JDK 9+ 上为 UTF-8 并回退到 ISO-8859-1
 * （可通过 "java.util.PropertyResourceBundle.encoding" 系统属性配置）。请注意，这种情况下也不会调用
 * {@link #loadBundle(Reader)}/{@link #loadBundle(InputStream)}，实际上会忽略子类中的覆盖。请考虑改
 * 为实现 JDK 9 的 {@code java.util.spi.ResourceBundleProvider}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @see #setBasenames
 * @see ReloadableResourceBundleMessageSource
 * @see java.util.ResourceBundle
 * @see java.text.MessageFormat
 */
public class ResourceBundleMessageSource extends AbstractResourceBasedMessageSource implements BeanClassLoaderAware {

	@Nullable
	private ClassLoader bundleClassLoader;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * 用于保存已加载 ResourceBundle 的缓存。此 Map 以资源包基名为键，其值是一个以 Locale 为键的 Map，
	 * 后者又保存 ResourceBundle 实例。这样可以实现非常高效的哈希查找，明显快于 ResourceBundle 类自身的
	 * 缓存。
	 */
	private final Map<String, Map<Locale, ResourceBundle>> cachedResourceBundles =
			new ConcurrentHashMap<>();

	/**
	 * 用于保存已生成 MessageFormat 的缓存。此 Map 以 ResourceBundle 为键，其值是一个以消息代码为键的
	 * Map，后者又保存一个以 Locale 为键、存放 MessageFormat 值的 Map。这样无需拼接键即可实现非常高效的
	 * 哈希查找。
	 * @see #getMessageFormat
	 */
	private final Map<ResourceBundle, Map<String, Map<Locale, MessageFormat>>> cachedBundleMessageFormats =
			new ConcurrentHashMap<>();

	@Nullable
	private volatile MessageSourceControl control = new MessageSourceControl();


	public ResourceBundleMessageSource() {
		setDefaultEncoding("ISO-8859-1");
	}


	/**
	 * 设置用于加载资源包的 ClassLoader。
	 * <p>默认是所在 BeanFactory 的
	 * {@link org.springframework.beans.factory.BeanClassLoaderAware bean ClassLoader}；如果不在
	 * BeanFactory 中运行，则是由 {@link org.springframework.util.ClassUtils#getDefaultClassLoader()}
	 * 确定的默认 ClassLoader。
	 */
	public void setBundleClassLoader(ClassLoader classLoader) {
		this.bundleClassLoader = classLoader;
	}

	/**
	 * 返回用于加载资源包的 ClassLoader。
	 * <p>默认是所在 BeanFactory 的 Bean ClassLoader。
	 * @see #setBundleClassLoader
	 */
	@Nullable
	protected ClassLoader getBundleClassLoader() {
		return (this.bundleClassLoader != null ? this.bundleClassLoader : this.beanClassLoader);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * 在已注册的资源包中将给定消息代码作为键进行解析，并原样返回包中找到的值（不进行 MessageFormat 解
	 * 析）。
	 */
	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				String result = getStringOrNull(bundle, code);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * 在已注册的资源包中将给定消息代码作为键进行解析，并为每个消息代码使用一个缓存的 MessageFormat 实
	 * 例。
	 */
	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		Set<String> basenames = getBasenameSet();
		for (String basename : basenames) {
			ResourceBundle bundle = getResourceBundle(basename, locale);
			if (bundle != null) {
				MessageFormat messageFormat = getMessageFormat(bundle, code, locale);
				if (messageFormat != null) {
					return messageFormat;
				}
			}
		}
		return null;
	}


	/**
	 * 返回给定基名和 Locale 的 ResourceBundle，并从缓存中获取已生成的 ResourceBundle。
	 * @param basename ResourceBundle 的基名
	 * @param locale 要为其查找 ResourceBundle 的 Locale
	 * @return 得到的 ResourceBundle；如果对给定的基名和 Locale 未找到，则为 {@code null}
	 */
	@Nullable
	protected ResourceBundle getResourceBundle(String basename, Locale locale) {
		if (getCacheMillis() >= 0) {
			// Fresh ResourceBundle.getBundle call in order to let ResourceBundle
			// do its native caching, at the expense of more extensive lookup steps.
			return doGetBundle(basename, locale);
		}
		else {
			// Cache forever: prefer locale cache over repeated getBundle calls.
			Map<Locale, ResourceBundle> localeMap = this.cachedResourceBundles.get(basename);
			if (localeMap != null) {
				ResourceBundle bundle = localeMap.get(locale);
				if (bundle != null) {
					return bundle;
				}
			}
			try {
				ResourceBundle bundle = doGetBundle(basename, locale);
				if (localeMap == null) {
					localeMap = this.cachedResourceBundles.computeIfAbsent(basename, bn -> new ConcurrentHashMap<>());
				}
				localeMap.put(locale, bundle);
				return bundle;
			}
			catch (MissingResourceException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("ResourceBundle [" + basename + "] not found for MessageSource: " + ex.getMessage());
				}
				// Assume bundle not found
				// -> do NOT throw the exception to allow for checking parent message source.
				return null;
			}
		}
	}

	/**
	 * 获取给定基名和 Locale 的资源包。
	 * @param basename 要查找的基名
	 * @param locale 要查找的 Locale
	 * @return 相应的 ResourceBundle
	 * @throws MissingResourceException 如果找不到匹配的资源包
	 * @see java.util.ResourceBundle#getBundle(String, Locale, ClassLoader)
	 * @see #getBundleClassLoader()
	 */
	protected ResourceBundle doGetBundle(String basename, Locale locale) throws MissingResourceException {
		ClassLoader classLoader = getBundleClassLoader();
		Assert.state(classLoader != null, "No bundle ClassLoader set");

		MessageSourceControl control = this.control;
		if (control != null) {
			try {
				return ResourceBundle.getBundle(basename, locale, classLoader, control);
			}
			catch (UnsupportedOperationException ex) {
				// Probably in a Jigsaw environment on JDK 9+
				this.control = null;
				String encoding = getDefaultEncoding();
				if (encoding != null && logger.isInfoEnabled()) {
					logger.info("ResourceBundleMessageSource is configured to read resources with encoding '" +
							encoding + "' but ResourceBundle.Control not supported in current system environment: " +
							ex.getMessage() + " - falling back to plain ResourceBundle.getBundle retrieval with the " +
							"platform default encoding. Consider setting the 'defaultEncoding' property to 'null' " +
							"for participating in the platform default and therefore avoiding this log message.");
				}
			}
		}

		// Fallback: plain getBundle lookup without Control handle
		return ResourceBundle.getBundle(basename, locale, classLoader);
	}

	/**
	 * 从给定 reader 加载基于属性的资源包。
	 * <p>在指定了 {@link #setDefaultEncoding "defaultEncoding"} 时会调用此方法，包括
	 * {@link ResourceBundleMessageSource} 默认的 ISO-8859-1 编码。请注意，此方法只能在使用
	 * {@code ResourceBundle.Control} 时被调用：在 JDK 9+ 模块路径上运行且不支持此类 control 句柄时，自
	 * 定义子类中的任何覆盖实际上都会被忽略。
	 * <p>默认实现返回一个 {@link PropertyResourceBundle}。
	 * @param reader 目标资源的读取器
	 * @return 完全加载的资源包
	 * @throws IOException 如果发生 I/O 失败
	 * @since 4.2
	 * @see #loadBundle(InputStream)
	 * @see PropertyResourceBundle#PropertyResourceBundle(Reader)
	 */
	protected ResourceBundle loadBundle(Reader reader) throws IOException {
		return new PropertyResourceBundle(reader);
	}

	/**
	 * 从给定输入流加载基于属性的资源包，在 JDK 9+ 上采用默认的属性编码。
	 * <p>只有在 {@link #setDefaultEncoding "defaultEncoding"} 设为 {@code null} 时才会调用此方法，此时
	 * 显式采用平台默认编码（在 JDK 9+ 上为 UTF-8 并回退到 ISO-8859-1，但可通过
	 * "java.util.PropertyResourceBundle.encoding" 系统属性配置）。请注意，此方法只能在使用
	 * {@code ResourceBundle.Control} 时被调用：在 JDK 9+ 模块路径上运行且不支持此类 control 句柄时，自
	 * 定义子类中的任何覆盖实际上都会被忽略。
	 * <p>默认实现返回一个 {@link PropertyResourceBundle}。
	 * @param inputStream 目标资源的输入流
	 * @return 完全加载的资源包
	 * @throws IOException 如果发生 I/O 失败
	 * @since 5.1
	 * @see #loadBundle(Reader)
	 * @see PropertyResourceBundle#PropertyResourceBundle(InputStream)
	 */
	protected ResourceBundle loadBundle(InputStream inputStream) throws IOException {
		return new PropertyResourceBundle(inputStream);
	}

	/**
	 * 返回给定资源包和代码的 MessageFormat，并从缓存中获取已生成的 MessageFormat。
	 * @param bundle 要操作的 ResourceBundle
	 * @param code 要获取的消息代码
	 * @param locale 用于构建 MessageFormat 的 Locale
	 * @return 得到的 MessageFormat；如果给定代码未定义消息，则为 {@code null}
	 * @throws MissingResourceException 如果由 ResourceBundle 抛出
	 */
	@Nullable
	protected MessageFormat getMessageFormat(ResourceBundle bundle, String code, Locale locale)
			throws MissingResourceException {

		Map<String, Map<Locale, MessageFormat>> codeMap = this.cachedBundleMessageFormats.get(bundle);
		Map<Locale, MessageFormat> localeMap = null;
		if (codeMap != null) {
			localeMap = codeMap.get(code);
			if (localeMap != null) {
				MessageFormat result = localeMap.get(locale);
				if (result != null) {
					return result;
				}
			}
		}

		String msg = getStringOrNull(bundle, code);
		if (msg != null) {
			if (codeMap == null) {
				codeMap = this.cachedBundleMessageFormats.computeIfAbsent(bundle, b -> new ConcurrentHashMap<>());
			}
			if (localeMap == null) {
				localeMap = codeMap.computeIfAbsent(code, c -> new ConcurrentHashMap<>());
			}
			MessageFormat result = createMessageFormat(msg, locale);
			localeMap.put(locale, result);
			return result;
		}

		return null;
	}

	/**
	 * 高效地获取指定键的 String 值，如果未找到则返回 {@code null}。
	 * <p>从 4.2 起，默认实现会先检查 {@code containsKey}，然后再尝试调用 {@code getString}（否则在键不
	 * 存在时需要捕获 {@code MissingResourceException}）。
	 * <p>可在子类中覆盖。
	 * @param bundle 要在其中执行查找的 ResourceBundle
	 * @param key 要查找的键
	 * @return 关联的值；如果没有则为 {@code null}
	 * @since 4.2
	 * @see ResourceBundle#getString(String)
	 * @see ResourceBundle#containsKey(String)
	 */
	@Nullable
	protected String getStringOrNull(ResourceBundle bundle, String key) {
		if (bundle.containsKey(key)) {
			try {
				return bundle.getString(key);
			}
			catch (MissingResourceException ex) {
				// Assume key not found for some other reason
				// -> do NOT throw the exception to allow for checking parent message source.
			}
		}
		return null;
	}

	/**
	 * 显示此 MessageSource 的配置。
	 */
	@Override
	public String toString() {
		return getClass().getName() + ": basenames=" + getBasenameSet();
	}


	/**
	 * {@code ResourceBundle.Control} 的自定义实现，增加了对自定义文件编码的支持，禁用了回退到系统
	 * Locale 的行为，并可按需启用 ResourceBundle 的原生缓存。
	 */
	private class MessageSourceControl extends ResourceBundle.Control {

		@Override
		@Nullable
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
				throws IllegalAccessException, InstantiationException, IOException {

			// Special handling of default encoding
			if (format.equals("java.properties")) {
				String bundleName = toBundleName(baseName, locale);
				final String resourceName = toResourceName(bundleName, "properties");
				final ClassLoader classLoader = loader;
				final boolean reloadFlag = reload;
				InputStream inputStream;
				try {
					inputStream = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> {
						InputStream is = null;
						if (reloadFlag) {
							URL url = classLoader.getResource(resourceName);
							if (url != null) {
								URLConnection connection = url.openConnection();
								if (connection != null) {
									connection.setUseCaches(false);
									is = connection.getInputStream();
								}
							}
						}
						else {
							is = classLoader.getResourceAsStream(resourceName);
						}
						return is;
					});
				}
				catch (PrivilegedActionException ex) {
					throw (IOException) ex.getException();
				}
				if (inputStream != null) {
					String encoding = getDefaultEncoding();
					if (encoding != null) {
						try (InputStreamReader bundleReader = new InputStreamReader(inputStream, encoding)) {
							return loadBundle(bundleReader);
						}
					}
					else {
						try (InputStream bundleStream = inputStream) {
							return loadBundle(bundleStream);
						}
					}
				}
				else {
					return null;
				}
			}
			else {
				// Delegate handling of "java.class" format to standard Control
				return super.newBundle(baseName, locale, format, loader, reload);
			}
		}

		@Override
		@Nullable
		public Locale getFallbackLocale(String baseName, Locale locale) {
			Locale defaultLocale = getDefaultLocale();
			return (defaultLocale != null && !defaultLocale.equals(locale) ? defaultLocale : null);
		}

		@Override
		public long getTimeToLive(String baseName, Locale locale) {
			long cacheMillis = getCacheMillis();
			return (cacheMillis >= 0 ? cacheMillis : super.getTimeToLive(baseName, locale));
		}

		@Override
		public boolean needsReload(
				String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {

			if (super.needsReload(baseName, locale, format, loader, bundle, loadTime)) {
				cachedBundleMessageFormats.remove(bundle);
				return true;
			}
			else {
				return false;
			}
		}
	}

}
