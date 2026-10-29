/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.lang.Nullable;

/**
 * {@link BeanPostProcessor} 的子接口，增加了实例化前回调，以及实例化之后但在设置显式属性或进行自动
 * 装配之前执行的回调。
 *
 * <p>通常用于抑制特定目标 Bean 的默认实例化，例如使用特殊的 TargetSources（池化目标、延迟初始化目
 * 标等）创建代理，或实现额外的注入策略，例如字段注入。
 *
 * <p><b>NOTE:</b> 此接口是一个特殊用途的接口，主要用于框架内部。建议尽可能实现普通的
 * {@link BeanPostProcessor} 接口。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 1.2
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator#setCustomTargetSourceCreators
 * @see org.springframework.aop.framework.autoproxy.target.LazyInitTargetSourceCreator
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

	/**
	 * 在<i>目标 Bean 被实例化之前</i>应用此 BeanPostProcessor。返回的 Bean 对象可以是用于替代目标 Bean
	 * 的代理，从而有效地抑制目标 Bean 的默认实例化。
	 * <p>如果此方法返回非 null 对象，则 Bean 创建过程将被短路。唯一进一步执行的处理是来自已配置的
	 * {@link BeanPostProcessor BeanPostProcessors} 的 {@link #postProcessAfterInitialization} 回调。
	 * <p>此回调将应用于带有 Bean 类的 Bean 定义，也会应用于工厂方法定义，在后一种情况下这里会传入返回
	 * 的 Bean 类型。
	 * <p>后处理器可以实现扩展的 {@link SmartInstantiationAwareBeanPostProcessor} 接口，以预测它们将在
	 * 此处返回的 Bean 对象的类型。
	 * <p>默认实现返回 {@code null}。
	 * @param beanClass the class of the bean to be instantiated
	 * @param beanName the name of the bean
	 * @return the bean object to expose instead of a default instance of the target bean,
	 * or {@code null} to proceed with default instantiation
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessAfterInstantiation
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getBeanClass()
	 * @see org.springframework.beans.factory.support.AbstractBeanDefinition#getFactoryMethodName()
	 */
	@Nullable
	default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 在 Bean 通过构造方法或工厂方法实例化之后，但在 Spring 进行属性填充（来自显式属性或自动装配）之前
	 * 执行操作。
	 * <p>这是在给定 Bean 实例上执行自定义字段注入的理想回调，此时正好在 Spring 的自动装配生效之前。
	 * <p>默认实现返回 {@code true}。
	 * @param bean the bean instance created, with properties not having been set yet
	 * @param beanName the name of the bean
	 * @return {@code true} if properties should be set on the bean; {@code false}
	 * if property population should be skipped. Normal implementations should return {@code true}.
	 * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
	 * instances being invoked on this bean instance.
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessBeforeInstantiation
	 */
	default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
		return true;
	}

	/**
	 * 在工厂将给定的属性值应用到给定 Bean 之前对其进行后处理，无需任何属性描述符。
	 * <p>如果实现提供了自定义的 {@link #postProcessPropertyValues} 实现，则应返回 {@code null}（默认值），
	 * 否则应返回 {@code pvs}。在此接口的未来版本中（移除了 {@link #postProcessPropertyValues}），
	 * 默认实现将直接原样返回给定的 {@code pvs}。
	 * @param pvs the property values that the factory is about to apply (never {@code null})
	 * @param bean the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean (can be the passed-in
	 * PropertyValues instance), or {@code null} which proceeds with the existing properties
	 * but specifically continues with a call to {@link #postProcessPropertyValues}
	 * (requiring initialized {@code PropertyDescriptor}s for the current bean class)
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @since 5.1
	 * @see #postProcessPropertyValues
	 */
	@Nullable
	default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * 在工厂将给定的属性值应用到给定 Bean 之前对其进行后处理。可以检查所有依赖是否已满足，例如基于
	 * Bean 属性 setter 上的 "Required" 注解。
	 * <p>还允许替换要应用的属性值，通常是通过基于原始 PropertyValues 创建新的 MutablePropertyValues 实
	 * 例，并添加或删除特定的值。
	 * <p>默认实现原样返回给定的 {@code pvs}。
	 * @param pvs the property values that the factory is about to apply (never {@code null})
	 * @param pds the relevant property descriptors for the target bean (with ignored
	 * dependency types - which the factory handles specifically - already filtered out)
	 * @param bean the bean instance created, but whose properties have not yet been set
	 * @param beanName the name of the bean
	 * @return the actual property values to apply to the given bean (can be the passed-in
	 * PropertyValues instance), or {@code null} to skip property population
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see #postProcessProperties
	 * @see org.springframework.beans.MutablePropertyValues
	 * @deprecated as of 5.1, in favor of {@link #postProcessProperties(PropertyValues, Object, String)}
	 */
	@Deprecated
	@Nullable
	default PropertyValues postProcessPropertyValues(
			PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		return pvs;
	}

}
