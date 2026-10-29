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

import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.aop.support.AopUtils;
import org.springframework.jmx.support.JmxUtils;

/**
 * {@code MBeanInfoAssembler} 接口的抽象实现，它封装了 {@code ModelMBeanInfo} 实例的创建，但将元数
 * 据的创建委托给子类。
 *
 * <p>此类提供两种从被管理 Bean 实例中提取 Class 的方式：{@link #getTargetClass} 提取任意类型 AOP
 * 代理背后的目标类，{@link #getClassToExpose} 返回将要在其中搜索注解并暴露给 JMX 运行时的类或接口。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 */
public abstract class AbstractMBeanInfoAssembler implements MBeanInfoAssembler {

	/**
	 * 创建所有 JMX 实现都会提供的 {@code ModelMBeanInfoSupport} 类实例，并通过调用子类来填充元数据。
	 * @param managedBean the bean that will be exposed (might be an AOP proxy)
	 * @param beanKey the key associated with the managed bean
	 * @return the populated ModelMBeanInfo instance
	 * @throws JMException in case of errors
	 * @see #getDescription(Object, String)
	 * @see #getAttributeInfo(Object, String)
	 * @see #getConstructorInfo(Object, String)
	 * @see #getOperationInfo(Object, String)
	 * @see #getNotificationInfo(Object, String)
	 * @see #populateMBeanDescriptor(javax.management.Descriptor, Object, String)
	 */
	@Override
	public ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException {
		checkManagedBean(managedBean);
		ModelMBeanInfo info = new ModelMBeanInfoSupport(
				getClassName(managedBean, beanKey), getDescription(managedBean, beanKey),
				getAttributeInfo(managedBean, beanKey), getConstructorInfo(managedBean, beanKey),
				getOperationInfo(managedBean, beanKey), getNotificationInfo(managedBean, beanKey));
		Descriptor desc = info.getMBeanDescriptor();
		populateMBeanDescriptor(desc, managedBean, beanKey);
		info.setMBeanDescriptor(desc);
		return info;
	}

	/**
	 * 检查给定的 Bean 实例，如果它不适合用此装配器暴露，则抛出 IllegalArgumentException。
	 * <p>默认实现为空，接受所有 Bean 实例。
	 * @param managedBean the bean that will be exposed (might be an AOP proxy)
	 * @throws IllegalArgumentException the bean is not valid for exposure
	 */
	protected void checkManagedBean(Object managedBean) throws IllegalArgumentException {
	}

	/**
	 * 返回给定 Bean 实例的实际 Bean 类。这是暴露给描述式 JMX 属性的类。
	 * <p>默认实现对于 AOP 代理返回目标类，否则返回普通 Bean 类。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @return the bean class to expose
	 * @see org.springframework.aop.support.AopUtils#getTargetClass(Object)
	 */
	protected Class<?> getTargetClass(Object managedBean) {
		return AopUtils.getTargetClass(managedBean);
	}

	/**
	 * 返回要为给定 Bean 暴露的类或接口。这是将要搜索属性和操作（例如检查注解）的类。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @return the bean class to expose
	 * @see JmxUtils#getClassToExpose(Object)
	 */
	protected Class<?> getClassToExpose(Object managedBean) {
		return JmxUtils.getClassToExpose(managedBean);
	}

	/**
	 * 返回要为给定 Bean 类暴露的类或接口。这是将要搜索属性和操作的类。
	 * @param beanClass the bean class (might be an AOP proxy class)
	 * @return the bean class to expose
	 * @see JmxUtils#getClassToExpose(Class)
	 */
	protected Class<?> getClassToExpose(Class<?> beanClass) {
		return JmxUtils.getClassToExpose(beanClass);
	}

	/**
	 * 获取 MBean 资源的类名。
	 * <p>默认实现基于类名返回 MBean 的简单描述。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the MBean description
	 * @throws JMException in case of errors
	 */
	protected String getClassName(Object managedBean, String beanKey) throws JMException {
		return getTargetClass(managedBean).getName();
	}

	/**
	 * 获取 MBean 资源的描述。
	 * <p>默认实现基于类名返回 MBean 的简单描述。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @throws JMException in case of errors
	 */
	protected String getDescription(Object managedBean, String beanKey) throws JMException {
		String targetClassName = getTargetClass(managedBean).getName();
		if (AopUtils.isAopProxy(managedBean)) {
			return "Proxy for " + targetClassName;
		}
		return targetClassName;
	}

	/**
	 * 在 {@code ModelMBeanInfo} 实例构造完成之后、传递给 {@code MBeanExporter} 之前调用。
	 * <p>子类可以实现此方法，向 MBean 元数据添加额外的描述符。默认实现为空。
	 * @param descriptor the {@code Descriptor} for the MBean resource.
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @throws JMException in case of errors
	 */
	protected void populateMBeanDescriptor(Descriptor descriptor, Object managedBean, String beanKey)
			throws JMException {
	}

	/**
	 * 获取 MBean 资源的构造器元数据。子类应实现此方法，为所有应在被管理资源的管理接口中暴露的构造器返
	 * 回适当的元数据。
	 * <p>默认实现返回空的 {@code ModelMBeanConstructorInfo} 数组。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the constructor metadata
	 * @throws JMException in case of errors
	 */
	protected ModelMBeanConstructorInfo[] getConstructorInfo(Object managedBean, String beanKey)
			throws JMException {
		return new ModelMBeanConstructorInfo[0];
	}

	/**
	 * 获取 MBean 资源的通知元数据。子类应实现此方法，为所有应在被管理资源的管理接口中暴露的通知返回适
	 * 当的元数据。
	 * <p>默认实现返回空的 {@code ModelMBeanNotificationInfo} 数组。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the notification metadata
	 * @throws JMException in case of errors
	 */
	protected ModelMBeanNotificationInfo[] getNotificationInfo(Object managedBean, String beanKey)
			throws JMException {
		return new ModelMBeanNotificationInfo[0];
	}


	/**
	 * 获取 MBean 资源的属性元数据。子类应实现此方法，为所有应在被管理资源的管理接口中暴露的属性返回适
	 * 当的元数据。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the attribute metadata
	 * @throws JMException in case of errors
	 */
	protected abstract ModelMBeanAttributeInfo[] getAttributeInfo(Object managedBean, String beanKey)
			throws JMException;

	/**
	 * 获取 MBean 资源的操作元数据。子类应实现此方法，为所有应在被管理资源的管理接口中暴露的操作返回适
	 * 当的元数据。
	 * @param managedBean the bean instance (might be an AOP proxy)
	 * @param beanKey the key associated with the MBean in the beans map
	 * of the {@code MBeanExporter}
	 * @return the operation metadata
	 * @throws JMException in case of errors
	 */
	protected abstract ModelMBeanOperationInfo[] getOperationInfo(Object managedBean, String beanKey)
			throws JMException;

}
