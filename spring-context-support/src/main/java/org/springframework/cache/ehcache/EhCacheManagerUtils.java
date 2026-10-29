/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

import org.springframework.core.io.Resource;

/**
 * EhCache 2.5+ {@link CacheManager} 设置的便捷构建方法，
 * 提供从 Spring 资源进行简单编程引导的能力。
 * 主要用于 Spring 配置类中的 {@code @Bean} 方法。
 *
 * <p>这些方法是自定义 {@link CacheManager} 设置代码的简单替代方案。
 * 对于任何高级用途，考虑使用 {@link #parseConfiguration}，
 * 自定义配置对象，然后调用 {@link CacheManager#CacheManager(Configuration)} 构造函数。
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class EhCacheManagerUtils {

	/**
	 * 从默认配置构建一个 EhCache {@link CacheManager}。
	 * <p>CacheManager 将从类路径根目录下的 "ehcache.xml" 进行配置
	 * （即默认 EhCache 初始化——如 EhCache 文档中所定义——将生效）。
	 * 如果找不到配置文件，将使用故障安全的回退配置。
	 * @return the new EhCache CacheManager
	 * @throws CacheException in case of configuration parsing failure
	 */
	public static CacheManager buildCacheManager() throws CacheException {
		return new CacheManager(ConfigurationFactory.parseConfiguration());
	}

	/**
	 * 从默认配置构建一个 EhCache {@link CacheManager}。
	 * <p>CacheManager 将从类路径根目录下的 "ehcache.xml" 进行配置
	 * （即默认 EhCache 初始化——如 EhCache 文档中所定义——将生效）。
	 * 如果找不到配置文件，将使用故障安全的回退配置。
	 * @param name the desired name of the cache manager
	 * @return the new EhCache CacheManager
	 * @throws CacheException in case of configuration parsing failure
	 */
	public static CacheManager buildCacheManager(String name) throws CacheException {
		Configuration configuration = ConfigurationFactory.parseConfiguration();
		configuration.setName(name);
		return new CacheManager(configuration);
	}

	/**
	 * 从给定的配置资源构建一个 EhCache {@link CacheManager}。
	 * @param configLocation the location of the configuration file (as a Spring resource)
	 * @return the new EhCache CacheManager
	 * @throws CacheException in case of configuration parsing failure
	 */
	public static CacheManager buildCacheManager(Resource configLocation) throws CacheException {
		return new CacheManager(parseConfiguration(configLocation));
	}

	/**
	 * 从给定的配置资源构建一个 EhCache {@link CacheManager}。
	 * @param name the desired name of the cache manager
	 * @param configLocation the location of the configuration file (as a Spring resource)
	 * @return the new EhCache CacheManager
	 * @throws CacheException in case of configuration parsing failure
	 */
	public static CacheManager buildCacheManager(String name, Resource configLocation) throws CacheException {
		Configuration configuration = parseConfiguration(configLocation);
		configuration.setName(name);
		return new CacheManager(configuration);
	}

	/**
	 * 从给定资源解析 EhCache 配置，用于自定义 {@link CacheManager} 创建。
	 * @param configLocation the location of the configuration file (as a Spring resource)
	 * @return the EhCache Configuration handle
	 * @throws CacheException in case of configuration parsing failure
	 * @see CacheManager#CacheManager(Configuration)
	 * @see CacheManager#create(Configuration)
	 */
	public static Configuration parseConfiguration(Resource configLocation) throws CacheException {
		InputStream is = null;
		try {
			is = configLocation.getInputStream();
			return ConfigurationFactory.parseConfiguration(is);
		}
		catch (IOException ex) {
			throw new CacheException("Failed to parse EhCache configuration resource", ex);
		}
		finally {
			if (is != null) {
				try {
					is.close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

}
