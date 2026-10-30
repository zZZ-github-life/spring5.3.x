/*
 * Copyright 2002-2017 the original author or authors.
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

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.lang.Nullable;

/**
 * 封装 {@code ObjectName} 实例创建过程的策略接口。
 *
 * <p>由 {@code MBeanExporter} 在注册 Bean 时用来获取 {@code ObjectName}。
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.MBeanExporter
 * @see javax.management.ObjectName
 */
@FunctionalInterface
public interface ObjectNamingStrategy {

	/**
	 * 为提供的 Bean 获取一个 {@code ObjectName}。
	 * @param managedBean 将在返回的 {@code ObjectName} 下暴露的 Bean
	 * @param beanKey 传递给 {@code MBeanExporter} 的 Bean 映射中与该 Bean 关联的键
	 * @return 该 {@code ObjectName} 实例
	 * @throws MalformedObjectNameException 如果得到的 {@code ObjectName} 无效
	 */
	ObjectName getObjectName(Object managedBean, @Nullable String beanKey) throws MalformedObjectNameException;

}
