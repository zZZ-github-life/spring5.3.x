/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.context.support;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.util.Assert;

/**
 * 用于处理 {@link ApplicationContextInitializer ApplicationContextInitializers} 的工具方法。
 *
 * <p>尽管 {@code ApplicationContextInitializerUtils} 首次引入于 Spring Framework 4.1，但此类中方法的初始实现基于 {@code ContextLoaderUtils} 中的现有代码库。
 *
 * @author Sam Brannen
 * @since 4.1
 * @see ContextConfiguration#initializers
 */
abstract class ApplicationContextInitializerUtils {

	private static final Log logger = LogFactory.getLog(ApplicationContextInitializerUtils.class);


	/**
	 * 为所提供的 {@code ContextConfigurationAttributes} 列表解析合并后的 {@code ApplicationContextInitializer} 类集合。
	 * <p>请注意，将考虑 {@link ContextConfiguration @ContextConfiguration} 的 {@link ContextConfiguration#inheritInitializers inheritInitializers} 标志。具体而言，如果对于所提供的配置属性所表示的类层次结构和外围类层次结构中的给定层级，{@code inheritInitializers} 标志设置为 {@code true}，则在该层级定义的上下文初始化器类将与类层次结构或外围类层次结构中更高层级定义的初始化器类合并。
	 * @param configAttributesList 要处理的配置属性列表；不能为 {@code null} 或<em>空</em>；
	 * 必须按<em>自下而上</em>顺序排列
	 * （即，如同向上遍历类层次结构或外围类层次结构）
	 * @return 合并后的上下文初始化器类集合，若适用则包括来自超类的
	 * 初始化器类（从不为 {@code null}）
	 * @since 3.2
	 */
	static Set<Class<? extends ApplicationContextInitializer<?>>> resolveInitializerClasses(
			List<ContextConfigurationAttributes> configAttributesList) {

		Assert.notEmpty(configAttributesList, "ContextConfigurationAttributes List must not be empty");
		Set<Class<? extends ApplicationContextInitializer<?>>> initializerClasses = new LinkedHashSet<>();

		for (ContextConfigurationAttributes configAttributes : configAttributesList) {
			if (logger.isTraceEnabled()) {
				logger.trace("Processing context initializers for configuration attributes " + configAttributes);
			}
			Collections.addAll(initializerClasses, configAttributes.getInitializers());
			if (!configAttributes.isInheritInitializers()) {
				break;
			}
		}

		return initializerClasses;
	}

}
