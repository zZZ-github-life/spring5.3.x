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

package org.springframework.jms.annotation;

import org.springframework.jms.config.JmsListenerEndpointRegistrar;

/**
 * 可选接口，由希望自定义 JMS 监听器端点配置方式的 Spring 管理 bean 实现。
 * 通常用于定义要使用的默认
 * {@link org.springframework.jms.config.JmsListenerContainerFactory
 * JmsListenerContainerFactory}，或用于以<em>编程</em>方式注册 JMS 端点，
 * 而不是采用使用 {@link JmsListener @JmsListener} 注解的<em>声明</em>方式。
 *
 * <p>详细用法示例请参见 {@link EnableJms @EnableJms}。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see EnableJms
 * @see JmsListenerEndpointRegistrar
 */
@FunctionalInterface
public interface JmsListenerConfigurer {

	/**
	 * 回调方法，允许将
	 * {@link org.springframework.jms.config.JmsListenerEndpointRegistry
	 * JmsListenerEndpointRegistry} 和特定的
	 * {@link org.springframework.jms.config.JmsListenerEndpoint
	 * JmsListenerEndpoint} 实例注册到给定的
	 * {@link JmsListenerEndpointRegistrar}。默认的
	 * {@link org.springframework.jms.config.JmsListenerContainerFactory JmsListenerContainerFactory}
	 * 也可以在此自定义。
	 * @param registrar 要配置的注册器
	 */
	void configureJmsListeners(JmsListenerEndpointRegistrar registrar);

}
