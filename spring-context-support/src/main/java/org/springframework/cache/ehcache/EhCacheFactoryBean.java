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

package org.springframework.cache.ehcache;

import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;
import net.sf.ehcache.event.CacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean}，创建一个命名的 EhCache {@link net.sf.ehcache.Cache} 实例
 * （或实现 {@link net.sf.ehcache.Ehcache} 接口的装饰器），
 * 表示 EhCache {@link net.sf.ehcache.CacheManager} 中的一个缓存区域。
 *
 * <p>如果在缓存配置描述符中未配置指定的命名缓存，
 * 此 FactoryBean 将使用提供的名称和指定的缓存属性构建一个 Cache 实例，
 * 并将其添加到 CacheManager 中以供后续检索。如果在配置时未设置部分或全部属性，
 * 此 FactoryBean 将使用默认值。
 *
 * <p>注意：如果找到了命名的 Cache 实例，属性将被忽略，
 * 将从 CacheManager 中检索该 Cache 实例。
 *
 * <p>注意：从 Spring 5.0 开始，Spring 的 EhCache 支持需要 EhCache 2.10 或更高版本。
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 1.1.1
 * @see #setCacheManager
 * @see EhCacheManagerFactoryBean
 * @see net.sf.ehcache.Cache
 */
public class EhCacheFactoryBean extends CacheConfiguration implements FactoryBean<Ehcache>, BeanNameAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private CacheManager cacheManager;

	private boolean blocking = false;

	@Nullable
	private CacheEntryFactory cacheEntryFactory;

	@Nullable
	private BootstrapCacheLoader bootstrapCacheLoader;

	@Nullable
	private Set<CacheEventListener> cacheEventListeners;

	private boolean disabled = false;

	@Nullable
	private String beanName;

	@Nullable
	private Ehcache cache;


	public EhCacheFactoryBean() {
		setMaxEntriesLocalHeap(10000);
		setMaxEntriesLocalDisk(10000000);
		setTimeToLiveSeconds(120);
		setTimeToIdleSeconds(120);
	}


	/**
	 * 设置一个 CacheManager，从中获取命名的 Cache 实例。
	 * 默认情况下，将调用 {@code CacheManager.getInstance()}。
	 * <p>注意，特别是对于持久化缓存，建议正确处理 CacheManager 的关闭：
	 * 设置一个单独的 EhCacheManagerFactoryBean 并将引用传递到此 bean 属性。
	 * <p>从非默认配置位置加载 EhCache 配置时，也需要单独的 EhCacheManagerFactoryBean。
	 * @see EhCacheManagerFactoryBean
	 * @see net.sf.ehcache.CacheManager#getInstance
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * 设置要检索或创建缓存实例的名称。
	 * 默认为此 EhCacheFactoryBean 的 bean 名称。
	 */
	public void setCacheName(String cacheName) {
		setName(cacheName);
	}

	/**
	 * 设置存活时间（time to live）。
	 * @see #setTimeToLiveSeconds(long)
	 */
	public void setTimeToLive(int timeToLive) {
		setTimeToLiveSeconds(timeToLive);
	}

	/**
	 * 设置空闲时间（time to idle）。
	 * @see #setTimeToIdleSeconds(long)
	 */
	public void setTimeToIdle(int timeToIdle) {
		setTimeToIdleSeconds(timeToIdle);
	}

	/**
	 * 设置磁盘缓冲池大小（以 MB 为单位）。
	 * @see #setDiskSpoolBufferSizeMB(int)
	 */
	public void setDiskSpoolBufferSize(int diskSpoolBufferSize) {
		setDiskSpoolBufferSizeMB(diskSpoolBufferSize);
	}

	/**
	 * 设置是否使用阻塞缓存，让读请求阻塞直到所请求的元素被创建。
	 * <p>如果你打算构建一个自填充的阻塞缓存，
	 * 考虑指定 {@link #setCacheEntryFactory CacheEntryFactory}。
	 * @see net.sf.ehcache.constructs.blocking.BlockingCache
	 * @see #setCacheEntryFactory
	 */
	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	/**
	 * 设置用于自填充缓存的 EhCache {@link net.sf.ehcache.constructs.blocking.CacheEntryFactory}。
	 * 如果指定了此类工厂，缓存将被装饰为 EhCache 的
	 * {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}。
	 * <p>指定的工厂可以是
	 * {@link net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory} 类型，
	 * 这将导致使用
	 * {@link net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache}。
	 * <p>注意：任何此类自填充缓存都自动是阻塞缓存。
	 * @see net.sf.ehcache.constructs.blocking.SelfPopulatingCache
	 * @see net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache
	 * @see net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory
	 */
	public void setCacheEntryFactory(CacheEntryFactory cacheEntryFactory) {
		this.cacheEntryFactory = cacheEntryFactory;
	}

	/**
	 * 设置此缓存的 EhCache {@link net.sf.ehcache.bootstrap.BootstrapCacheLoader}（如果有）。
	 */
	public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) {
		this.bootstrapCacheLoader = bootstrapCacheLoader;
	}

	/**
	 * 指定要注册到此缓存中的 EhCache {@link net.sf.ehcache.event.CacheEventListener 缓存事件监听器}。
	 */
	public void setCacheEventListeners(Set<CacheEventListener> cacheEventListeners) {
		this.cacheEventListeners = cacheEventListeners;
	}

	/**
	 * 设置此缓存是否应被标记为禁用。
	 * @see net.sf.ehcache.Cache#setDisabled
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public void afterPropertiesSet() throws CacheException {
		// If no cache name given, use bean name as cache name.
		String cacheName = getName();
		if (cacheName == null) {
			cacheName = this.beanName;
			if (cacheName != null) {
				setName(cacheName);
			}
		}

		// If no CacheManager given, fetch the default.
		if (this.cacheManager == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using default EhCache CacheManager for cache region '" + cacheName + "'");
			}
			this.cacheManager = CacheManager.getInstance();
		}

		synchronized (this.cacheManager) {
			// Fetch cache region: If none with the given name exists, create one on the fly.
			Ehcache rawCache;
			boolean cacheExists = this.cacheManager.cacheExists(cacheName);

			if (cacheExists) {
				if (logger.isDebugEnabled()) {
					logger.debug("Using existing EhCache cache region '" + cacheName + "'");
				}
				rawCache = this.cacheManager.getEhcache(cacheName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Creating new EhCache cache region '" + cacheName + "'");
				}
				rawCache = createCache();
				rawCache.setBootstrapCacheLoader(this.bootstrapCacheLoader);
			}

			if (this.cacheEventListeners != null) {
				for (CacheEventListener listener : this.cacheEventListeners) {
					rawCache.getCacheEventNotificationService().registerListener(listener);
				}
			}

			// Needs to happen after listener registration but before setStatisticsEnabled
			if (!cacheExists) {
				this.cacheManager.addCache(rawCache);
			}

			if (this.disabled) {
				rawCache.setDisabled(true);
			}

			Ehcache decoratedCache = decorateCache(rawCache);
			if (decoratedCache != rawCache) {
				this.cacheManager.replaceCacheWithDecoratedCache(rawCache, decoratedCache);
			}
			this.cache = decoratedCache;
		}
	}

	/**
	 * 基于此 FactoryBean 的配置创建一个原始的 Cache 对象。
	 */
	protected Cache createCache() {
		return new Cache(this);
	}

	/**
	 * 如有必要，装饰给定的 Cache。
	 * @param cache the raw Cache object, based on the configuration of this FactoryBean
	 * @return the (potentially decorated) cache object to be registered with the CacheManager
	 */
	protected Ehcache decorateCache(Ehcache cache) {
		if (this.cacheEntryFactory != null) {
			if (this.cacheEntryFactory instanceof UpdatingCacheEntryFactory) {
				return new UpdatingSelfPopulatingCache(cache, (UpdatingCacheEntryFactory) this.cacheEntryFactory);
			}
			else {
				return new SelfPopulatingCache(cache, this.cacheEntryFactory);
			}
		}
		if (this.blocking) {
			return new BlockingCache(cache);
		}
		return cache;
	}


	@Override
	@Nullable
	public Ehcache getObject() {
		return this.cache;
	}

	/**
	 * 预测将从 {@link #getObject()} 返回的具体 {@code Ehcache} 实现，
	 * 基于 {@link #createCache()} 和 {@link #decorateCache(Ehcache)} 中的逻辑，
	 * 由 {@link #afterPropertiesSet()} 编排。
	 */
	@Override
	public Class<? extends Ehcache> getObjectType() {
		if (this.cache != null) {
			return this.cache.getClass();
		}
		if (this.cacheEntryFactory != null) {
			if (this.cacheEntryFactory instanceof UpdatingCacheEntryFactory) {
				return UpdatingSelfPopulatingCache.class;
			}
			else {
				return SelfPopulatingCache.class;
			}
		}
		if (this.blocking) {
			return BlockingCache.class;
		}
		return Cache.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
