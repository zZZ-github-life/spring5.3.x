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

/**
 * 一种与 {@code @Configuration} 一起使用时提供更细粒度控制的 {@link Condition}。允许某些条件在匹配
 * 时根据配置阶段进行调整。例如，检查某个 Bean 是否已注册的条件可能选择只在
 * {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} {@link ConfigurationPhase} 期间进行评估。
 *
 * @author Phillip Webb
 * @since 4.0
 * @see Configuration
 */
public interface ConfigurationCondition extends Condition {

	/**
	 * 返回应评估该条件的 {@link ConfigurationPhase}。
	 */
	ConfigurationPhase getConfigurationPhase();


	/**
	 * 可评估该条件的各个配置阶段。
	 */
	enum ConfigurationPhase {

		/**
		 * 应在解析 {@code @Configuration} 类时评估 {@link Condition}。
		 * <p>如果条件在此处不匹配，则不会添加该 {@code @Configuration} 类。
		 */
		PARSE_CONFIGURATION,

		/**
		 * 应在添加常规（非 {@code @Configuration}）Bean 时评估 {@link Condition}。该条件不会阻止添加
		 * {@code @Configuration} 类。
		 * <p>在评估该条件时，所有 {@code @Configuration} 类都已解析完毕。
		 */
		REGISTER_BEAN
	}

}
