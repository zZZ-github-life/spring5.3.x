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

package org.springframework.jmx.access;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 创建指向本地或远程运行的受管资源的代理。"proxyInterface" 属性定义生成的代理应当实现的接口。该接
 * 口应定义与要代理资源的管理接口中的操作和属性对应的方法和属性。
 *
 * <p>受管资源无需实现代理接口，尽管这样做可能更方便。并不要求管理接口中的每个操作和属性都由代理接
 * 口中对应的属性或方法匹配。
 *
 * <p>尝试调用或访问代理接口上与管理接口不对应的任何方法或属性，将导致
 * {@code InvalidInvocationException}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see MBeanClientInterceptor
 * @see InvalidInvocationException
 */
public class MBeanProxyFactoryBean extends MBeanClientInterceptor
		implements FactoryBean<Object>, BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Class<?> proxyInterface;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private Object mbeanProxy;


	/**
	 * 设置生成的代理将实现的接口。<p>这通常是匹配目标 MBean 的管理接口，为 MBean 属性暴露 Bean 属性的
	 * setter 和 getter，并为 MBean 操作暴露常规 Java 方法。
	 * @see #setObjectName
	 */
	public void setProxyInterface(Class<?> proxyInterface) {
		this.proxyInterface = proxyInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 检查是否已指定 {@code proxyInterface}，然后为目标 MBean 生成代理。
	 */
	@Override
	public void afterPropertiesSet() throws MBeanServerNotFoundException, MBeanInfoRetrievalException {
		super.afterPropertiesSet();

		if (this.proxyInterface == null) {
			this.proxyInterface = getManagementInterface();
			if (this.proxyInterface == null) {
				throw new IllegalArgumentException("Property 'proxyInterface' or 'managementInterface' is required");
			}
		}
		else {
			if (getManagementInterface() == null) {
				setManagementInterface(this.proxyInterface);
			}
		}
		this.mbeanProxy = new ProxyFactory(this.proxyInterface, this).getProxy(this.beanClassLoader);
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.mbeanProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.proxyInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
