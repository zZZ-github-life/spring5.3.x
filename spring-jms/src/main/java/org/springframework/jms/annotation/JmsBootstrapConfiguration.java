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

package org.springframework.jms.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.config.JmsListenerEndpointRegistry;

/**
 * {@code @Configuration} 类，注册一个能够处理 Spring {@link JmsListener @JmsListener}
 * 注解的 {@link JmsListenerAnnotationBeanPostProcessor} bean。同时注册一个默认的
 * {@link JmsListenerEndpointRegistry}。
 *
 * <p>使用 {@link EnableJms @EnableJms} 注解时会自动导入此配置类。
 * 完整的使用细节请参见 {@link EnableJms} 的 javadoc。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JmsListenerAnnotationBeanPostProcessor
 * @see JmsListenerEndpointRegistry
 * @see EnableJms
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class JmsBootstrapConfiguration {

	@Bean(name = JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JmsListenerAnnotationBeanPostProcessor jmsListenerAnnotationProcessor() {
		return new JmsListenerAnnotationBeanPostProcessor();
	}

	@Bean(name = JmsListenerConfigUtils.JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME)
	public JmsListenerEndpointRegistry defaultJmsListenerEndpointRegistry() {
		return new JmsListenerEndpointRegistry();
	}

}
