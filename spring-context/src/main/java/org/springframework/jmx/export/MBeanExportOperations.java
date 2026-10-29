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

package org.springframework.jmx.export;

import javax.management.ObjectName;

/**
 * 定义一组 MBean 导出操作的接口，这些操作旨在供应用开发者在应用运行时访问。
 *
 * <p>此接口应用于使用 Spring 的管理接口生成能力，以及（可选地）其 {@link ObjectName} 生成能力，将
 * 应用资源导出到 JMX。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see MBeanExporter
 */
public interface MBeanExportOperations {

	/**
	 * 向 JMX 注册所提供的资源。如果该资源尚不是有效的 MBean，Spring 将为其生成管理接口。生成的确切接口
	 * 取决于实现及其配置。此调用还会为受管资源生成一个 {@link ObjectName} 并将其返回给调用方。
	 * @param managedResource the resource to expose via JMX
	 * @return the {@link ObjectName} under which the resource was exposed
	 * @throws MBeanExportException if Spring is unable to generate an {@link ObjectName}
	 * or register the MBean
	 */
	ObjectName registerManagedResource(Object managedResource) throws MBeanExportException;

	/**
	 * 向 JMX 注册所提供的资源。如果该资源尚不是有效的 MBean，Spring 将为其生成管理接口。生成的确切接口
	 * 取决于实现及其配置。
	 * @param managedResource the resource to expose via JMX
	 * @param objectName the {@link ObjectName} under which to expose the resource
	 * @throws MBeanExportException if Spring is unable to register the MBean
	 */
	void registerManagedResource(Object managedResource, ObjectName objectName) throws MBeanExportException;

	/**
	 * 从底层 MBeanServer 注册表中移除指定的 MBean。
	 * @param objectName the {@link ObjectName} of the resource to remove
	 */
	void unregisterManagedResource(ObjectName objectName);

}
