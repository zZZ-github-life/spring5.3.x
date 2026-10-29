/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.config;

/**
 * 用于跨子包内部共享的配置常量。
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class JmsListenerConfigUtils {

	/**
	 * 内部管理的 JMS 监听器注解处理器的 bean 名称。
	 */
	public static final String JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME =
			"org.springframework.jms.config.internalJmsListenerAnnotationProcessor";

	/**
	 * 内部管理的 JMS 监听器端点注册表的 bean 名称。
	 */
	public static final String JMS_LISTENER_ENDPOINT_REGISTRY_BEAN_NAME =
			"org.springframework.jms.config.internalJmsListenerEndpointRegistry";

}
