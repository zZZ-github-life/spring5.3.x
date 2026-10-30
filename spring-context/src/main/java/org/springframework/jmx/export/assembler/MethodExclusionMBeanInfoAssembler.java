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

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler} 的子类，允许将方法名显式排除在 MBean 操作和属性之外。
 *
 * <p>任何未被显式排除在管理接口之外的方法都将暴露给 JMX。JavaBean getter 和 setter 将自动作为 JMX
 * 属性暴露。
 *
 * <p>你可以通过 {@code ignoredMethods} 属性提供方法名数组。如果有多个 Bean，并且希望每个 Bean 使用
 * 不同的方法名集，则可以使用 {@code ignoredMethodMappings} 属性将 Bean 键（即用于将 Bean 传递给
 * {@code MBeanExporter} 的名称）映射到方法名列表。
 *
 * <p>如果同时为 {@code ignoredMethodMappings} 和 {@code ignoredMethods} 指定了值，Spring 将首先尝
 * 试在映射中查找方法名。如果未找到该 Bean 的方法名，则使用 {@code ignoredMethods} 定义的方法名。
 *
 * @author Rob Harrop
 * @author Seth Ladd
 * @since 1.2.5
 * @see #setIgnoredMethods
 * @see #setIgnoredMethodMappings
 * @see InterfaceBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see MethodNameBasedMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class MethodExclusionMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	@Nullable
	private Set<String> ignoredMethods;

	@Nullable
	private Map<String, Set<String>> ignoredMethodMappings;


	/**
	 * 设置创建管理信息时要<b>忽略</b>的方法名数组。
	 * <p>如果在 {@code ignoredMethodsMappings} 属性中找不到与该 Bean 对应的条目，则这些方法名将用于该
	 * Bean。
	 * @see #setIgnoredMethodMappings(java.util.Properties)
	 */
	public void setIgnoredMethods(String... ignoredMethodNames) {
		this.ignoredMethods = new HashSet<>(Arrays.asList(ignoredMethodNames));
	}

	/**
	 * 设置 Bean 键到逗号分隔的方法名列表的映射。
	 * <p>创建管理接口时，这些方法名会被<b>忽略</b>。
	 * <p>属性键必须与 Bean 键匹配，属性值必须与方法名列表匹配。在为 Bean 搜索要忽略的方法名时，Spring
	 * 将首先检查这些映射。
	 */
	public void setIgnoredMethodMappings(Properties mappings) {
		this.ignoredMethodMappings = new HashMap<>();
		for (Enumeration<?> en = mappings.keys(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] methodNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			this.ignoredMethodMappings.put(beanKey, new HashSet<>(Arrays.asList(methodNames)));
		}
	}


	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	/**
	 * 确定给定方法是否应被包含，即未被配置为忽略。
	 * @param method 操作方法
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中
	 * 关联的键
	 */
	protected boolean isNotIgnored(Method method, String beanKey) {
		if (this.ignoredMethodMappings != null) {
			Set<String> methodNames = this.ignoredMethodMappings.get(beanKey);
			if (methodNames != null) {
				return !methodNames.contains(method.getName());
			}
		}
		if (this.ignoredMethods != null) {
			return !this.ignoredMethods.contains(method.getName());
		}
		return true;
	}

}
