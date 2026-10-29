/*
 * Copyright 2002-2016 the original author or authors.
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

import java.lang.reflect.Constructor;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * {@link InstantiationAwareBeanPostProcessor} 接口的扩展，增加了用于预测所处理 Bean 最终类型的回调。
 *
 * <p><b>注意：</b> 此接口是专用接口，主要供框架内部使用。一般而言，应用程序提供的后处理器应仅实现
 * 普通的 {@link BeanPostProcessor} 接口，或派生自
 * {@link InstantiationAwareBeanPostProcessorAdapter} 类。即使在补丁版本中也可能向此接口添加新方法。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see InstantiationAwareBeanPostProcessorAdapter
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

	/**
	 * 预测最终将从此处理器的 {@link #postProcessBeforeInstantiation} 回调返回的 Bean 类型。
	 * <p>默认实现返回 {@code null}。
	 * @param beanClass the raw class of the bean
	 * @param beanName the name of the bean
	 * @return the type of the bean, or {@code null} if not predictable
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	@Nullable
	default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
		return null;
	}

	/**
	 * 确定要用于给定 Bean 的候选构造器。
	 * <p>默认实现返回 {@code null}。
	 * @param beanClass the raw class of the bean (never {@code null})
	 * @param beanName the name of the bean
	 * @return the candidate constructors, or {@code null} if none specified
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	@Nullable
	default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
			throws BeansException {

		return null;
	}

	/**
	 * 获取对指定 Bean 的早期访问引用，通常用于解析循环引用。
	 * <p>此回调使后处理器有机会提前暴露一个包装器——即在目标 Bean 实例完全初始化之前。暴露的对象应等同
	 * 于 {@link #postProcessBeforeInitialization} / {@link #postProcessAfterInitialization} 原本会暴露
	 * 的对象。请注意，此方法返回的对象将用作 Bean 引用，除非后处理器从上述后处理回调中返回不同的包装器。
	 * 换言之：这些后处理回调最终可以暴露相同的引用，或者从后续回调中返回原始 Bean 实例（如果受影响
	 * Bean 的包装器已经为此方法的调用而构建，默认情况下它将作为最终 Bean 引用暴露）。
	 * <p>默认实现按原样返回给定的 {@code bean}。
	 * @param bean the raw bean instance
	 * @param beanName the name of the bean
	 * @return the object to expose as bean reference
	 * (typically with the passed-in bean instance as default)
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		return bean;
	}

}
