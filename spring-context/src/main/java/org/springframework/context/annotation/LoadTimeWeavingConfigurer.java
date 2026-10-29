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

package org.springframework.context.annotation;

import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 希望自定义要使用的 {@link LoadTimeWeaver} 实例、且标注了
 * {@link EnableLoadTimeWeaving @EnableLoadTimeWeaving} 的
 * {@link org.springframework.context.annotation.Configuration @Configuration} 类应实现此接口。
 *
 * <p>有关用法示例以及未使用此接口时如何选择默认 {@code LoadTimeWeaver} 的信息，请参见
 * {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync}。
 *
 * @author Chris Beams
 * @since 3.1
 * @see LoadTimeWeavingConfiguration
 * @see EnableLoadTimeWeaving
 */
public interface LoadTimeWeavingConfigurer {

	/**
	 * 创建、配置并返回要使用的 {@code LoadTimeWeaver} 实例。注意，无需用 {@code @Bean} 标注此方法，因
	 * 为返回的对象将由 {@link LoadTimeWeavingConfiguration#loadTimeWeaver()} 自动注册为 Bean。
	 */
	LoadTimeWeaver getLoadTimeWeaver();

}
