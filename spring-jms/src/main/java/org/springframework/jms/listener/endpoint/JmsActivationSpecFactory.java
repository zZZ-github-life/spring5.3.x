/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jms.listener.endpoint;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

/**
 * 策略接口，用于基于已配置的 {@link JmsActivationSpecConfig} 对象
 * 创建 JCA 1.5 ActivationSpec 对象。
 *
 * <p>JCA 1.5 ActivationSpec 对象通常是 JavaBean，但遗憾的是
 * 它们特定于提供者。此策略接口允许接入任何基于 JCA 的 JMS 提供者，
 * 基于通用 JMS 配置设置创建相应的 ActivationSpec 对象。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see JmsActivationSpecConfig
 * @see JmsMessageEndpointManager#setActivationSpecFactory
 * @see javax.resource.spi.ResourceAdapter#endpointActivation
 */
public interface JmsActivationSpecFactory {

	/**
	 * 基于给定的 {@link JmsActivationSpecConfig} 对象创建
	 * JCA 1.5 ActivationSpec 对象。
	 * @param adapter the ResourceAdapter to create an ActivationSpec object for
	 * @param config the configured object holding common JMS settings
	 * @return the provider-specific JCA ActivationSpec object,
	 * representing the same settings
	 */
	ActivationSpec createActivationSpec(ResourceAdapter adapter, JmsActivationSpecConfig config);

}
