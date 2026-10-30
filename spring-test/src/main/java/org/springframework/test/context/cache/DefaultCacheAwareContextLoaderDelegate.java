/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.Assert;

/**
 * {@link CacheAwareContextLoaderDelegate} 接口的默认实现。
 * <p>要使用静态 {@link DefaultContextCache}，请调用 {@link #DefaultCacheAwareContextLoaderDelegate()} 构造函数；否则，请调用 {@link #DefaultCacheAwareContextLoaderDelegate(ContextCache)} 并提供自定义 {@link ContextCache} 实现。
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultCacheAwareContextLoaderDelegate implements CacheAwareContextLoaderDelegate {

	private static final Log logger = LogFactory.getLog(DefaultCacheAwareContextLoaderDelegate.class);

	/**
	 * Spring 应用程序上下文的默认静态缓存。
	 */
	static final ContextCache defaultContextCache = new DefaultContextCache();

	private final ContextCache contextCache;


	/**
	 * 使用静态 {@link DefaultContextCache} 构造一个新的 {@code DefaultCacheAwareContextLoaderDelegate}。
	 * <p>此默认缓存是静态的，以便每个上下文都能被缓存，并可在同一 JVM 进程中为所有声明相同唯一上下文配置的后续测试复用。
	 * @see #DefaultCacheAwareContextLoaderDelegate(ContextCache)
	 */
	public DefaultCacheAwareContextLoaderDelegate() {
		this(defaultContextCache);
	}

	/**
	 * 使用提供的 {@link ContextCache} 构造一个新的 {@code DefaultCacheAwareContextLoaderDelegate}。
	 * @see #DefaultCacheAwareContextLoaderDelegate()
	 */
	public DefaultCacheAwareContextLoaderDelegate(ContextCache contextCache) {
		Assert.notNull(contextCache, "ContextCache must not be null");
		this.contextCache = contextCache;
	}


	@Override
	public boolean isContextLoaded(MergedContextConfiguration mergedContextConfiguration) {
		synchronized (this.contextCache) {
			return this.contextCache.contains(mergedContextConfiguration);
		}
	}

	@Override
	public ApplicationContext loadContext(MergedContextConfiguration mergedContextConfiguration) {
		synchronized (this.contextCache) {
			ApplicationContext context = this.contextCache.get(mergedContextConfiguration);
			if (context == null) {
				try {
					context = loadContextInternal(mergedContextConfiguration);
					if (logger.isDebugEnabled()) {
						logger.debug(String.format("Storing ApplicationContext [%s] in cache under key [%s]",
								System.identityHashCode(context), mergedContextConfiguration));
					}
					this.contextCache.put(mergedContextConfiguration, context);
				}
				catch (Exception ex) {
					throw new IllegalStateException("Failed to load ApplicationContext", ex);
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Retrieved ApplicationContext [%s] from cache with key [%s]",
							System.identityHashCode(context), mergedContextConfiguration));
				}
			}

			this.contextCache.logStatistics();

			return context;
		}
	}

	@Override
	public void closeContext(MergedContextConfiguration mergedContextConfiguration, @Nullable HierarchyMode hierarchyMode) {
		synchronized (this.contextCache) {
			this.contextCache.remove(mergedContextConfiguration, hierarchyMode);
		}
	}

	/**
	 * 获取此上下文加载器委托使用的 {@link ContextCache}。
	 */
	protected ContextCache getContextCache() {
		return this.contextCache;
	}

	/**
	 * 为提供的合并上下文配置加载 {@code ApplicationContext}。
	 * <p>同时支持 {@link SmartContextLoader} 和 {@link ContextLoader} SPI。
	 * @throws Exception 如果加载应用上下文时发生错误
	 */
	protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration)
			throws Exception {

		ContextLoader contextLoader = mergedContextConfiguration.getContextLoader();
		Assert.notNull(contextLoader, "Cannot load an ApplicationContext with a NULL 'contextLoader'. " +
				"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");

		if (contextLoader instanceof SmartContextLoader) {
			return ((SmartContextLoader) contextLoader).loadContext(mergedContextConfiguration);
		}
		else {
			String[] locations = mergedContextConfiguration.getLocations();
			Assert.notNull(locations, "Cannot load an ApplicationContext with a NULL 'locations' array. " +
					"Consider annotating your test class with @ContextConfiguration or @ContextHierarchy.");
			return contextLoader.loadContext(locations);
		}
	}

}
