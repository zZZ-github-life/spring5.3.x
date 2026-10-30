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

package org.springframework.core.io.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PropertiesPersister;

/**
 * 需要从一个或多个资源加载属性的 JavaBean 风格组件的基类。也支持本地属性，并可通过配置覆盖。
 *
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public abstract class PropertiesLoaderSupport {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	protected Properties[] localProperties;

	protected boolean localOverride = false;

	@Nullable
	private Resource[] locations;

	private boolean ignoreResourceNotFound = false;

	@Nullable
	private String fileEncoding;

	private PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;


	/**
	 * 设置本地属性，例如通过 XML Bean 定义中的 "props" 标签。这些可视为默认值，会被从文件加载的属性覆
	 * 盖。
	 */
	public void setProperties(Properties properties) {
		this.localProperties = new Properties[] {properties};
	}

	/**
	 * 设置本地属性，例如通过 XML Bean 定义中的 "props" 标签，允许将多个属性集合合并为一个。
	 */
	public void setPropertiesArray(Properties... propertiesArray) {
		this.localProperties = propertiesArray;
	}

	/**
	 * 设置要加载的 properties 文件的位置。
	 * <p>可指向经典的 properties 文件，或指向遵循 JDK 1.5 的 properties XML 格式的 XML 文件。
	 */
	public void setLocation(Resource location) {
		this.locations = new Resource[] {location};
	}

	/**
	 * 设置要加载的 properties 文件的位置。
	 * <p>可指向经典的 properties 文件，或指向遵循 JDK 1.5 的 properties XML 格式的 XML 文件。
	 * <p>注意：如果键有重叠，后面的文件中定义的属性将覆盖前面文件中定义的属性。因此，请确保最具体的文
	 * 件位于给定位置列表的最后。
	 */
	public void setLocations(Resource... locations) {
		this.locations = locations;
	}

	/**
	 * 设置本地属性是否覆盖来自文件的属性。
	 * <p>默认为 "false"：来自文件的属性覆盖本地默认值。可切换为 "true"，以让本地属性覆盖来自文件的默认
	 * 值。
	 */
	public void setLocalOverride(boolean localOverride) {
		this.localOverride = localOverride;
	}

	/**
	 * 设置是否应忽略找不到属性资源的情况。
	 * <p>如果 properties 文件完全是可选的，则 "true" 是合适的。默认为 "false"。
	 */
	public void setIgnoreResourceNotFound(boolean ignoreResourceNotFound) {
		this.ignoreResourceNotFound = ignoreResourceNotFound;
	}

	/**
	 * 设置用于解析 properties 文件的编码。
	 * <p>默认没有，使用 {@code java.util.Properties} 的默认编码。
	 * <p>仅适用于经典的 properties 文件，不适用于 XML 文件。
	 * @see org.springframework.util.PropertiesPersister#load
	 */
	public void setFileEncoding(String encoding) {
		this.fileEncoding = encoding;
	}

	/**
	 * 设置用于解析 properties 文件的 PropertiesPersister。默认是 ResourcePropertiesPersister。
	 * @see ResourcePropertiesPersister#INSTANCE
	 */
	public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
		this.propertiesPersister =
				(propertiesPersister != null ? propertiesPersister : ResourcePropertiesPersister.INSTANCE);
	}


	/**
	 * 返回一个合并的 Properties 实例，其中包含已加载的属性和在此 FactoryBean 上设置的属性。
	 */
	protected Properties mergeProperties() throws IOException {
		Properties result = new Properties();

		if (this.localOverride) {
			// Load properties from file upfront, to let local properties override.
			loadProperties(result);
		}

		if (this.localProperties != null) {
			for (Properties localProp : this.localProperties) {
				CollectionUtils.mergePropertiesIntoMap(localProp, result);
			}
		}

		if (!this.localOverride) {
			// Load properties from file afterwards, to let those properties override.
			loadProperties(result);
		}

		return result;
	}

	/**
	 * 将属性加载到给定的实例中。
	 * @param props 要加载到的 Properties 实例
	 * @throws IOException 在出现 I/O 错误时
	 * @see #setLocations
	 */
	protected void loadProperties(Properties props) throws IOException {
		if (this.locations != null) {
			for (Resource location : this.locations) {
				if (logger.isTraceEnabled()) {
					logger.trace("Loading properties file from " + location);
				}
				try {
					PropertiesLoaderUtils.fillProperties(
							props, new EncodedResource(location, this.fileEncoding), this.propertiesPersister);
				}
				catch (FileNotFoundException | UnknownHostException | SocketException ex) {
					if (this.ignoreResourceNotFound) {
						if (logger.isDebugEnabled()) {
							logger.debug("Properties resource not found: " + ex.getMessage());
						}
					}
					else {
						throw ex;
					}
				}
			}
		}
	}

}
