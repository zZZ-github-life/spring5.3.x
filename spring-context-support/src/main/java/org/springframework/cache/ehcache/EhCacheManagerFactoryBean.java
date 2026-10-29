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

package org.springframework.cache.ehcache;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean}，暴露一个 EhCache {@link net.sf.ehcache.CacheManager} 实例
 * （独立的或共享的），从指定的配置位置进行配置。
 *
 * <p>如果未指定配置位置，将从类路径根目录的 "ehcache.xml" 配置 CacheManager
 * （即默认 EhCache 初始化——如 EhCache 文档中所定义——将生效）。
 *
 * <p>在使用 EhCacheFactoryBean 时，也建议设置单独的 EhCacheManagerFactoryBean，
 * 因为它提供（默认情况下）独立的 CacheManager 实例，并负责 CacheManager 的正确关闭。
 * 从非默认配置位置加载 EhCache 配置时，也需要 EhCacheManagerFactoryBean。
 *
 * <p>注意：从 Spring 5.0 开始，Spring 的 EhCache 支持需要 EhCache 2.10 或更高版本。
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 1.1.1
 * @see #setConfigLocation
 * @see #setShared
 * @see EhCacheFactoryBean
 * @see net.sf.ehcache.CacheManager
 */
public class EhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Resource configLocation;

	@Nullable
	private String cacheManagerName;

	private boolean acceptExisting = false;

	private boolean shared = false;

	@Nullable
	private CacheManager cacheManager;

	private boolean locallyManaged = true;


	/**
	 * 设置 EhCache 配置文件的位置。典型值为 "/WEB-INF/ehcache.xml"。
	 * <p>默认为类路径根目录下的 "ehcache.xml"，如果未找到，
	 * 则使用 EhCache jar 中的 "ehcache-failsafe.xml"（默认 EhCache 初始化）。
	 * @see net.sf.ehcache.CacheManager#create(java.io.InputStream)
	 * @see net.sf.ehcache.CacheManager#CacheManager(java.io.InputStream)
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置 EhCache CacheManager 的名称（如果需要特定名称）。
	 * @see net.sf.ehcache.config.Configuration#setName(String)
	 */
	public void setCacheManagerName(String cacheManagerName) {
		this.cacheManagerName = cacheManagerName;
	}

	/**
	 * 设置在此 EhCacheManagerFactoryBean 设置中是否接受同名
	 * 的已有 EhCache CacheManager。默认为 "false"。
	 * <p>通常与 {@link #setCacheManagerName "cacheManagerName"} 结合使用，
	 * 但如果未指定，也可以简单地使用默认的 CacheManager 名称。
	 * 在同一 ClassLoader 空间中，对相同 CacheManager 名称（或相同默认值）的
	 * 所有引用将共享指定的 CacheManager。
	 * @see #setCacheManagerName
	 * #see #setShared
	 * @see net.sf.ehcache.CacheManager#getCacheManager(String)
	 * @see net.sf.ehcache.CacheManager#CacheManager()
	 */
	public void setAcceptExisting(boolean acceptExisting) {
		this.acceptExisting = acceptExisting;
	}

	/**
	 * 设置 EhCache CacheManager 应该是共享的（在 ClassLoader 级别作为单例）
	 * 还是独立的（通常在应用程序内部是局部的）。默认为 "false"，创建独立的本地实例。
	 * <p><b>注意：</b>此特性允许将此 EhCacheManagerFactoryBean 的 CacheManager
	 * 与在同一 ClassLoader 空间中调用 <code>CacheManager.create()</code> 的任何代码共享，
	 * 无需就特定的 CacheManager 名称达成一致。但是，它只支持涉及单个
	 * EhCacheManagerFactoryBean，该工厂将控制底层 CacheManager 的生命周期（特别是其关闭）。
	 * <p>如果同时设置了此标志和 {@link #setAcceptExisting "acceptExisting"}，
	 * 此标志将覆盖后者，因为它表示"更强"的共享模式。
	 * @see #setCacheManagerName
	 * @see #setAcceptExisting
	 * @see net.sf.ehcache.CacheManager#create()
	 * @see net.sf.ehcache.CacheManager#CacheManager()
	 */
	public void setShared(boolean shared) {
		this.shared = shared;
	}


	@Override
	public void afterPropertiesSet() throws CacheException {
		if (logger.isDebugEnabled()) {
			logger.debug("Initializing EhCache CacheManager" +
					(this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
		}

		Configuration configuration = (this.configLocation != null ?
				EhCacheManagerUtils.parseConfiguration(this.configLocation) : ConfigurationFactory.parseConfiguration());
		if (this.cacheManagerName != null) {
			configuration.setName(this.cacheManagerName);
		}

		if (this.shared) {
			// Old-school EhCache singleton sharing...
			// No way to find out whether we actually created a new CacheManager
			// or just received an existing singleton reference.
			this.cacheManager = CacheManager.create(configuration);
		}
		else if (this.acceptExisting) {
			// EhCache 2.5+: Reusing an existing CacheManager of the same name.
			// Basically the same code as in CacheManager.getInstance(String),
			// just storing whether we're dealing with an existing instance.
			synchronized (CacheManager.class) {
				this.cacheManager = CacheManager.getCacheManager(this.cacheManagerName);
				if (this.cacheManager == null) {
					this.cacheManager = new CacheManager(configuration);
				}
				else {
					this.locallyManaged = false;
				}
			}
		}
		else {
			// Throwing an exception if a CacheManager of the same name exists already...
			this.cacheManager = new CacheManager(configuration);
		}
	}


	@Override
	@Nullable
	public CacheManager getObject() {
		return this.cacheManager;
	}

	@Override
	public Class<? extends CacheManager> getObjectType() {
		return (this.cacheManager != null ? this.cacheManager.getClass() : CacheManager.class);
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	@Override
	public void destroy() {
		if (this.cacheManager != null && this.locallyManaged) {
			if (logger.isDebugEnabled()) {
				logger.debug("Shutting down EhCache CacheManager" +
						(this.cacheManagerName != null ? " '" + this.cacheManagerName + "'" : ""));
			}
			this.cacheManager.shutdown();
		}
	}

}
