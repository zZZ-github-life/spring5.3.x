/*
 * Copyright 2002-2012 the original author or authors.
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
 * 一个监听器，允许在通过 {@link MBeanExporter} 注册和注销 MBean 时通知应用代码。
 *
 * @author Rob Harrop
 * @since 1.2.2
 * @see org.springframework.jmx.export.MBeanExporter#setListeners
 */
public interface MBeanExporterListener {

	/**
	 * 在 MBean 已<i>成功</i>注册到 {@link javax.management.MBeanServer} 之后由 {@link MBeanExporter}
	 * 调用。
	 * @param objectName the {@code ObjectName} of the registered MBean
	 */
	void mbeanRegistered(ObjectName objectName);

	/**
	 * 在 MBean 已<i>成功</i>从 {@link javax.management.MBeanServer} 注销之后由 {@link MBeanExporter}
	 * 调用。
	 * @param objectName the {@code ObjectName} of the unregistered MBean
	 */
	void mbeanUnregistered(ObjectName objectName);

}
