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

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler} 的子类，允许使用任意接口来定义 Bean 的管理接口。这
 * 些接口中定义的所有方法或属性都将作为 MBean 操作和属性暴露。
 *
 * <p>默认情况下，此类根据 Bean 类所实现的接口对每个操作或属性的包含进行投票。但是，你可以通过
 * {@code managedInterfaces} 属性提供一个接口数组，改为使用这些接口。如果有多个 Bean，并且希望每个
 * Bean 使用不同的接口集，则可以使用 {@code interfaceMappings} 属性将 Bean 键（即用于将 Bean 传递给
 * {@code MBeanExporter} 的名称）映射到接口名称列表。
 *
 * <p>如果同时为 {@code interfaceMappings} 和 {@code managedInterfaces} 指定了值，Spring 将首先尝试
 * 在映射中查找接口。如果未找到该 Bean 的接口，则使用 {@code managedInterfaces} 定义的接口。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setManagedInterfaces
 * @see #setInterfaceMappings
 * @see MethodNameBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class InterfaceBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler
		implements BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Class<?>[] managedInterfaces;

	/** bean 键到类数组的映射。 */
	@Nullable
	private Properties interfaceMappings;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** bean 键到类数组的映射。 */
	@Nullable
	private Map<String, Class<?>[]> resolvedInterfaceMappings;


	/**
	 * 设置用于创建管理信息的接口数组。如果在 {@code interfaceMappings} 属性中找不到与该 Bean 对应的条
	 * 目，则这些接口将用于该 Bean。
	 * @param managedInterfaces an array of classes indicating the interfaces to use.
	 * Each entry <strong>MUST</strong> be an interface.
	 * @see #setInterfaceMappings
	 */
	public void setManagedInterfaces(@Nullable Class<?>... managedInterfaces) {
		if (managedInterfaces != null) {
			for (Class<?> ifc : managedInterfaces) {
				if (!ifc.isInterface()) {
					throw new IllegalArgumentException(
							"Management interface [" + ifc.getName() + "] is not an interface");
				}
			}
		}
		this.managedInterfaces = managedInterfaces;
	}

	/**
	 * 设置 Bean 键到逗号分隔的接口名称列表的映射。
	 * <p>属性键应与 Bean 键匹配，属性值应与接口名称列表匹配。在为 Bean 搜索接口时，Spring 将首先检查这
	 * 些映射。
	 * @param mappings the mappings of bean keys to interface names
	 */
	public void setInterfaceMappings(@Nullable Properties mappings) {
		this.interfaceMappings = mappings;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.interfaceMappings != null) {
			this.resolvedInterfaceMappings = resolveInterfaceMappings(this.interfaceMappings);
		}
	}

	/**
	 * 解析给定的接口映射，将类名转换为 Class 对象。
	 * @param mappings the specified interface mappings
	 * @return the resolved interface mappings (with Class objects as values)
	 */
	private Map<String, Class<?>[]> resolveInterfaceMappings(Properties mappings) {
		Map<String, Class<?>[]> resolvedMappings = CollectionUtils.newHashMap(mappings.size());
		for (Enumeration<?> en = mappings.propertyNames(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] classNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			Class<?>[] classes = resolveClassNames(classNames, beanKey);
			resolvedMappings.put(beanKey, classes);
		}
		return resolvedMappings;
	}

	/**
	 * 将给定的类名解析为 Class 对象。
	 * @param classNames the class names to resolve
	 * @param beanKey the bean key that the class names are associated with
	 * @return the resolved Class
	 */
	private Class<?>[] resolveClassNames(String[] classNames, String beanKey) {
		Class<?>[] classes = new Class<?>[classNames.length];
		for (int x = 0; x < classes.length; x++) {
			Class<?> cls = ClassUtils.resolveClassName(classNames[x].trim(), this.beanClassLoader);
			if (!cls.isInterface()) {
				throw new IllegalArgumentException(
						"Class [" + classNames[x] + "] mapped to bean key [" + beanKey + "] is no interface");
			}
			classes[x] = cls;
		}
		return classes;
	}


	/**
	 * 检查 {@code Method} 是否在某个已配置的接口中声明，以及它是否为 public。
	 * @param method the accessor {@code Method}.
	 * @param beanKey the key associated with the MBean in the
	 * {@code beans} {@code Map}.
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces, otherwise {@code false}.
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查 {@code Method} 是否在某个已配置的接口中声明，以及它是否为 public。
	 * @param method the mutator {@code Method}.
	 * @param beanKey the key associated with the MBean in the
	 * {@code beans} {@code Map}.
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces, otherwise {@code false}.
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查 {@code Method} 是否在某个已配置的接口中声明，以及它是否为 public。
	 * @param method the operation {@code Method}.
	 * @param beanKey the key associated with the MBean in the
	 * {@code beans} {@code Map}.
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces, otherwise {@code false}.
	 */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查 {@code Method} 是否同时为 public 且已在某个已配置的接口中声明。
	 * @param method the {@code Method} to check.
	 * @param beanKey the key associated with the MBean in the beans map
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces and is public, otherwise {@code false}.
	 */
	private boolean isPublicInInterface(Method method, String beanKey) {
		return Modifier.isPublic(method.getModifiers()) && isDeclaredInInterface(method, beanKey);
	}

	/**
	 * 检查给定方法是否在给定 Bean 的某个受管理接口中声明。
	 */
	private boolean isDeclaredInInterface(Method method, String beanKey) {
		Class<?>[] ifaces = null;

		if (this.resolvedInterfaceMappings != null) {
			ifaces = this.resolvedInterfaceMappings.get(beanKey);
		}

		if (ifaces == null) {
			ifaces = this.managedInterfaces;
			if (ifaces == null) {
				ifaces = ClassUtils.getAllInterfacesForClass(method.getDeclaringClass());
			}
		}

		for (Class<?> ifc : ifaces) {
			for (Method ifcMethod : ifc.getMethods()) {
				if (ifcMethod.getName().equals(method.getName()) &&
						ifcMethod.getParameterCount() == method.getParameterCount() &&
						Arrays.equals(ifcMethod.getParameterTypes(), method.getParameterTypes())) {
					return true;
				}
			}
		}

		return false;
	}

}
