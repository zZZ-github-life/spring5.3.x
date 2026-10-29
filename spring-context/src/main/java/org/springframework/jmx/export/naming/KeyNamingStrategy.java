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

package org.springframework.jmx.export.naming;

import java.io.IOException;
import java.util.Properties;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@code ObjectNamingStrategy} 实现，它根据传给 {@code MBeanExporter} 的 "beans" 映射中使用的键来
 * 构建 {@code ObjectName} 实例。
 *
 * <p>还可以检查以 {@code Properties} 形式或以属性文件的 {@code mappingLocations} 形式给出的对象名
 * 称映射。用于查找的键就是 {@code MBeanExporter} 的 "beans" 映射中使用的键。如果给定键找不到映射，
 * 则使用该键本身来构建 {@code ObjectName}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setMappings
 * @see #setMappingLocation
 * @see #setMappingLocations
 * @see org.springframework.jmx.export.MBeanExporter#setBeans
 */
public class KeyNamingStrategy implements ObjectNamingStrategy, InitializingBean {

	/**
	 * 此类的 {@code Log} 实例。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 存储 Bean 键到 {@code ObjectName} 的映射。
	 */
	@Nullable
	private Properties mappings;

	/**
	 * 存储包含属性的 {@code Resource}，这些属性应加载到用于解析 {@code ObjectName} 的最终合并
	 * {@code Properties} 集合中。
	 */
	@Nullable
	private Resource[] mappingLocations;

	/**
	 * 存储将 {@code mappings} 中的 {@code Properties} 与 {@code mappingLocations} 所定义资源中存储的属
	 * 性合并后的结果。
	 */
	@Nullable
	private Properties mergedMappings;


	/**
	 * 设置包含对象名称映射的本地属性，例如通过 XML Bean 定义中的 "props" 标签设置。这些属性可视为默认
	 * 值，会被从文件加载的属性覆盖。
	 */
	public void setMappings(Properties mappings) {
		this.mappings = mappings;
	}

	/**
	 * 设置要加载的属性文件位置，该文件包含对象名称映射。
	 */
	public void setMappingLocation(Resource location) {
		this.mappingLocations = new Resource[] {location};
	}

	/**
	 * 设置要加载的属性文件的位置，这些文件包含对象名称映射。
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}


	/**
	 * 将 {@code mappings} 和 {@code mappingLocations} 中配置的 {@code Properties} 合并到用于解析
	 * {@code ObjectName} 的最终 {@code Properties} 实例中。
	 */
	@Override
	public void afterPropertiesSet() throws IOException {
		this.mergedMappings = new Properties();
		CollectionUtils.mergePropertiesIntoMap(this.mappings, this.mergedMappings);

		if (this.mappingLocations != null) {
			for (Resource location : this.mappingLocations) {
				if (logger.isDebugEnabled()) {
					logger.debug("Loading JMX object name mappings file from " + location);
				}
				PropertiesLoaderUtils.fillProperties(this.mergedMappings, location);
			}
		}
	}


	/**
	 * 尝试通过给定键获取 {@code ObjectName}，首先尝试在映射中查找映射值。
	 */
	@Override
	public ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException {
		Assert.notNull(beanKey, "KeyNamingStrategy requires bean key");
		String objectName = null;
		if (this.mergedMappings != null) {
			objectName = this.mergedMappings.getProperty(beanKey);
		}
		if (objectName == null) {
			objectName = beanKey;
		}
		return ObjectNameManager.getInstance(objectName);
	}

}
