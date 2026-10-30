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

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * 基于资源包约定的 {@code MessageSource} 实现的抽象基类，例如 {@link ResourceBundleMessageSource}
 * 和 {@link ReloadableResourceBundleMessageSource}。提供通用的配置方法及相应的语义定义。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see ResourceBundleMessageSource
 * @see ReloadableResourceBundleMessageSource
 */
public abstract class AbstractResourceBasedMessageSource extends AbstractMessageSource {

	private final Set<String> basenameSet = new LinkedHashSet<>(4);

	@Nullable
	private String defaultEncoding;

	private boolean fallbackToSystemLocale = true;

	@Nullable
	private Locale defaultLocale;

	private long cacheMillis = -1;


	/**
	 * 设置单个基名，遵循不指定文件扩展名或语言代码的基本 ResourceBundle 约定。资源位置格式取决于具体的
	 * {@code MessageSource} 实现。
	 *
	 * <p>支持常规 properties 文件和 XML properties 文件：例如 "messages" 会找到 "messages.properties"、
	 * "messages_en.properties" 等组合，以及 "messages.xml"、"messages_en.xml" 等。
	 * @param basename 单个基名
	 * @see #setBasenames
	 * @see org.springframework.core.io.ResourceEditor
	 * @see java.util.ResourceBundle
	 */
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * 设置基名数组，每个基名都遵循不指定文件扩展名或语言代码的基本 ResourceBundle 约定。资源位置格式取
	 * 决于具体的 {@code MessageSource} 实现。
	 *
	 * <p>支持常规 properties 文件和 XML properties 文件：例如 "messages" 会找到 "messages.properties"、
	 * "messages_en.properties" 等组合，以及 "messages.xml"、"messages_en.xml" 等。
	 *
	 * <p>解析消息代码时，将按顺序检查关联的资源包。请注意，由于采用顺序查找，<i>前一个</i>资源包中的消
	 * 息定义将覆盖后一个资源包中的定义。
	 *
	 * <p>注意：与 {@link #addBasenames} 不同，此方法会使用给定名称替换现有条目，因此也可用于重置配置。
	 * @param basenames 基名数组
	 * @see #setBasename
	 * @see java.util.ResourceBundle
	 */
	public void setBasenames(String... basenames) {
		this.basenameSet.clear();
		addBasenames(basenames);
	}

	/**
	 * 将指定的基名添加到现有基名配置中。
	 *
	 * <p>注意：如果给定基名已存在，其条目的位置将保持在原始集合中的位置。新条目将添加到列表末尾，在现
	 * 有基名之后进行搜索。
	 * @since 4.3
	 * @see #setBasenames
	 * @see java.util.ResourceBundle
	 */
	public void addBasenames(String... basenames) {
		if (!ObjectUtils.isEmpty(basenames)) {
			for (String basename : basenames) {
				Assert.hasText(basename, "Basename must not be empty");
				this.basenameSet.add(basename.trim());
			}
		}
	}

	/**
	 * 返回此 {@code MessageSource} 的基名集合，其中条目按注册顺序排列。
	 *
	 * <p>调用代码可以内省此集合，也可以添加或删除条目。
	 * @since 4.3
	 * @see #addBasenames
	 */
	public Set<String> getBasenameSet() {
		return this.basenameSet;
	}

	/**
	 * 设置用于解析 properties 文件的默认字符集。当文件未指定特定字符集时使用。
	 *
	 * <p>有效的默认值是 {@code java.util.Properties} 的默认编码：ISO-8859-1。{@code null} 值表示使用平
	 * 台默认编码。
	 *
	 * <p>仅适用于经典 properties 文件，不适用于 XML 文件。
	 * @param defaultEncoding 默认字符集
	 */
	public void setDefaultEncoding(@Nullable String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 返回用于解析 properties 文件的默认字符集（如果有）。
	 * @since 4.3
	 */
	@Nullable
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * 设置如果在未找到特定 Locale 的文件时是否回退到系统 Locale。默认值为 "true"；如果关闭此选项，唯一
	 * 的回退将是默认文件（例如基名 "messages" 对应的 "messages.properties"）。
	 *
	 * <p>回退到系统 Locale 是 {@code java.util.ResourceBundle} 的默认行为。然而，在应用服务器环境中，
	 * 系统 Locale 可能与应用程序完全无关，因此这通常并不可取：在这种情况下，请将此标志设置为 "false"。
	 * @see #setDefaultLocale
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	/**
	 * 返回在未找到特定 Locale 的文件时是否回退到系统 Locale。
	 * @since 4.3
	 * @deprecated as of 5.2.2, in favor of {@link #getDefaultLocale()}
	 */
	@Deprecated
	protected boolean isFallbackToSystemLocale() {
		return this.fallbackToSystemLocale;
	}

	/**
	 * 指定要回退到的默认 Locale，作为回退到系统 Locale 的替代方案。
	 *
	 * <p>默认回退到系统 Locale。你可以在此处用本地指定的默认 Locale 覆盖此项，或者通过禁用
	 * {@link #setFallbackToSystemLocale "fallbackToSystemLocale"} 来强制完全不使用回退区域设置。
	 * @since 5.2.2
	 * @see #setFallbackToSystemLocale
	 * @see #getDefaultLocale()
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 确定要回退到的默认 Locale：本地指定的默认 Locale 或系统 Locale，或者 {@code null} 表示完全不使用
	 * 回退区域设置。
	 * @since 5.2.2
	 * @see #setDefaultLocale
	 * @see #setFallbackToSystemLocale
	 * @see Locale#getDefault()
	 */
	@Nullable
	protected Locale getDefaultLocale() {
		if (this.defaultLocale != null) {
			return this.defaultLocale;
		}
		if (this.fallbackToSystemLocale) {
			return Locale.getDefault();
		}
		return null;
	}

	/**
	 * 设置缓存已加载 properties 文件的秒数。
	 * <ul>
	 * <li>默认值为 "-1"，表示永久缓存（与 {@code java.util.ResourceBundle} 的默认行为一致）。请注意，此常量遵循 Spring 的约定，而不是 {@link java.util.ResourceBundle.Control#getTimeToLive}。
	 * <li>正数会将已加载的 properties 文件缓存指定的秒数。这本质上就是两次刷新检查之间的间隔。请注意，刷新尝试会先检查文件的最后修改时间戳，然后才实际重新加载；因此如果文件未发生变化，此间隔可以设置得相当低，因为刷新尝试不会真正重新加载。
	 * <li>值为 "0" 时，每次访问消息都会检查文件的最后修改时间戳。<b>请勿在生产环境中使用！</b>
	 * </ul>
	 * <p><b>请注意，根据所使用的 ClassLoader，过期可能无法可靠生效，因为 ClassLoader 可能会保留资源包
	 * 文件的缓存版本。</b>在这种情况下，请优先使用 {@link ReloadableResourceBundleMessageSource} 而不
	 * 是 {@link ResourceBundleMessageSource}，并结合非类路径位置使用。
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheMillis = cacheSeconds * 1000L;
	}

	/**
	 * 设置缓存已加载 properties 文件的毫秒数。请注意，通常设置秒数即可：{@link #setCacheSeconds}。
	 * <ul>
	 * <li>默认值为 "-1"，表示永久缓存（与 {@code java.util.ResourceBundle} 的默认行为一致）。请注意，此常量遵循 Spring 的约定，而不是 {@link java.util.ResourceBundle.Control#getTimeToLive}。
	 * <li>正数会将已加载的 properties 文件缓存指定的毫秒数。这本质上就是两次刷新检查之间的间隔。请注意，刷新尝试会先检查文件的最后修改时间戳，然后才实际重新加载；因此如果文件未发生变化，此间隔可以设置得相当低，因为刷新尝试不会真正重新加载。
	 * <li>值为 "0" 时，每次访问消息都会检查文件的最后修改时间戳。<b>请勿在生产环境中使用！</b>
	 * </ul>
	 * @since 4.3
	 * @see #setCacheSeconds
	 */
	public void setCacheMillis(long cacheMillis) {
		this.cacheMillis = cacheMillis;
	}

	/**
	 * 返回缓存已加载 properties 文件的毫秒数。
	 * @since 4.3
	 */
	protected long getCacheMillis() {
		return this.cacheMillis;
	}

}
