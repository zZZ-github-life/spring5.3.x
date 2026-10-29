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

package org.springframework.context.annotation;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 根据将 {@code mode} 和 {@code proxyTargetClass} 属性设置为正确值的 {@code @Enable*} 注解，视情况
 * 向当前的 {@link BeanDefinitionRegistry} 注册一个自动代理创建器。
 *
 * @author Chris Beams
 * @since 3.1
 * @see org.springframework.cache.annotation.EnableCaching
 * @see org.springframework.transaction.annotation.EnableTransactionManagement
 */
public class AutoProxyRegistrar implements ImportBeanDefinitionRegistrar {

	private final Log logger = LogFactory.getLog(getClass());

	/**
	 * 向给定的注册表注册、升级并配置标准的自动代理创建器（APC）。其工作原理是查找导入的
	 * {@code @Configuration} 类上声明的、同时具有 {@code mode} 和 {@code proxyTargetClass} 属性的最近
	 * 注解。如果 {@code mode} 设置为 {@code PROXY}，则注册 APC；如果 {@code proxyTargetClass} 设置为
	 * {@code true}，则强制 APC 使用基于子类（CGLIB）的代理。<p>若干 {@code @Enable*} 注解都同时暴露
	 * {@code mode} 和 {@code proxyTargetClass} 属性。需要注意的是，这些能力大多最终会共享
	 * {@linkplain AopConfigUtils#AUTO_PROXY_CREATOR_BEAN_NAME 单个 APC}。因此，本实现并不“关心”它找到
	 * 的究竟是哪个注解——只要该注解暴露了正确的 {@code mode} 和 {@code proxyTargetClass} 属性，APC 就能
	 * 以相同的方式被注册和配置。
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		boolean candidateFound = false;
		Set<String> annTypes = importingClassMetadata.getAnnotationTypes();
		for (String annType : annTypes) {
			AnnotationAttributes candidate = AnnotationConfigUtils.attributesFor(importingClassMetadata, annType);
			if (candidate == null) {
				continue;
			}
			Object mode = candidate.get("mode");
			Object proxyTargetClass = candidate.get("proxyTargetClass");
			if (mode != null && proxyTargetClass != null && AdviceMode.class == mode.getClass() &&
					Boolean.class == proxyTargetClass.getClass()) {
				candidateFound = true;
				if (mode == AdviceMode.PROXY) {
					AopConfigUtils.registerAutoProxyCreatorIfNecessary(registry);
					if ((Boolean) proxyTargetClass) {
						AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);
						return;
					}
				}
			}
		}
		if (!candidateFound && logger.isInfoEnabled()) {
			String name = getClass().getSimpleName();
			logger.info(String.format("%s was imported but no annotations were found " +
					"having both 'mode' and 'proxyTargetClass' attributes of type " +
					"AdviceMode and boolean respectively. This means that auto proxy " +
					"creator registration and configuration may not have occurred as " +
					"intended, and components may not be proxied as expected. Check to " +
					"ensure that %s has been @Import'ed on the same class where these " +
					"annotations are declared; otherwise remove the import of %s " +
					"altogether.", name, name, name));
		}
	}

}
