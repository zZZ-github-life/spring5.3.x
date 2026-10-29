/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.jms.Session;
import javax.resource.spi.ResourceAdapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;

/**
 * {@link JmsActivationSpecFactory} 接口的默认实现。
 * 支持 JCA 1.5 规范定义的标准 JMS 属性，并通过自动检测
 * 已知的厂商特定提供者属性，支持 Spring 扩展的
 * "maxConcurrency" 和 "prefetchSize" 设置。
 *
 * <p>ActivationSpec 工厂实际上依赖于具体的 JMS 提供者，例如 ActiveMQ。
 * 此默认实现只是根据提供者的类名猜测 ActivationSpec 类名
 * （"ActiveMQResourceAdapter" &rarr; 同一包中的 "ActiveMQActivationSpec"，
 * 或与 ResourceAdapter 类同一包中的 "ActivationSpecImpl"），
 * 并按照 JCA 1.5 规范（附录 B）的建议填充 ActivationSpec 属性。
 * 如果这些默认命名规则不适用，请显式指定 'activationSpecClass' 属性。
 *
 * <p>注意：就扩展设置而言，支持 ActiveMQ、JORAM 和 WebSphere
 * （通过检测它们的 Bean 属性命名约定）。
 * 默认的 ActivationSpec 类检测规则也可能适用于其他 JMS 提供者。
 *
 * <p>感谢 Agim Emruli 和 Laurie Chan 指出 WebSphere MQ 设置
 * 并贡献了相应的测试！
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setActivationSpecClass
 */
public class DefaultJmsActivationSpecFactory extends StandardJmsActivationSpecFactory {

	private static final String RESOURCE_ADAPTER_SUFFIX = "ResourceAdapter";

	private static final String RESOURCE_ADAPTER_IMPL_SUFFIX = "ResourceAdapterImpl";

	private static final String ACTIVATION_SPEC_SUFFIX = "ActivationSpec";

	private static final String ACTIVATION_SPEC_IMPL_SUFFIX = "ActivationSpecImpl";


	/** 子类可用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * 此实现从提供者的类名猜测 ActivationSpec 类名：
	 * 例如同一包中的 "ActiveMQResourceAdapter" &rarr;
	 * "ActiveMQActivationSpec"，或与 ResourceAdapter 类同一包中
	 * 名为 "ActivationSpecImpl" 的类。
	 */
	@Override
	protected Class<?> determineActivationSpecClass(ResourceAdapter adapter) {
		String adapterClassName = adapter.getClass().getName();

		if (adapterClassName.endsWith(RESOURCE_ADAPTER_SUFFIX)) {
			// e.g. ActiveMQ
			String providerName =
					adapterClassName.substring(0, adapterClassName.length() - RESOURCE_ADAPTER_SUFFIX.length());
			String specClassName = providerName + ACTIVATION_SPEC_SUFFIX;
			try {
				return adapter.getClass().getClassLoader().loadClass(specClassName);
			}
			catch (ClassNotFoundException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No default <Provider>ActivationSpec class found: " + specClassName);
				}
			}
		}

		else if (adapterClassName.endsWith(RESOURCE_ADAPTER_IMPL_SUFFIX)){
			//e.g. WebSphere
			String providerName =
					adapterClassName.substring(0, adapterClassName.length() - RESOURCE_ADAPTER_IMPL_SUFFIX.length());
			String specClassName = providerName + ACTIVATION_SPEC_IMPL_SUFFIX;
			try {
				return adapter.getClass().getClassLoader().loadClass(specClassName);
			}
			catch (ClassNotFoundException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("No default <Provider>ActivationSpecImpl class found: " + specClassName);
				}
			}
		}

		// e.g. JORAM
		String providerPackage = adapterClassName.substring(0, adapterClassName.lastIndexOf('.') + 1);
		String specClassName = providerPackage + ACTIVATION_SPEC_IMPL_SUFFIX;
		try {
			return adapter.getClass().getClassLoader().loadClass(specClassName);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No default ActivationSpecImpl class found in provider package: " + specClassName);
			}
		}

		// ActivationSpecImpl class in "inbound" subpackage (WebSphere MQ 6.0.2.1)
		specClassName = providerPackage + "inbound." + ACTIVATION_SPEC_IMPL_SUFFIX;
		try {
			return adapter.getClass().getClassLoader().loadClass(specClassName);
		}
		catch (ClassNotFoundException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No default ActivationSpecImpl class found in inbound subpackage: " + specClassName);
			}
		}

		throw new IllegalStateException("No ActivationSpec class defined - " +
				"specify the 'activationSpecClass' property or override the 'determineActivationSpecClass' method");
	}

	/**
	 * 此实现通过检测对应的 ActivationSpec 属性，支持 Spring 扩展的
	 * "maxConcurrency" 和 "prefetchSize" 设置：
	 * 分别为 "maxSessions"/"maxNumberOfWorks" 和
	 * "maxMessagesPerSessions"/"maxMessages"
	 * （遵循 ActiveMQ 和 JORAM 的命名约定）。
	 */
	@Override
	protected void populateActivationSpecProperties(BeanWrapper bw, JmsActivationSpecConfig config) {
		super.populateActivationSpecProperties(bw, config);
		if (config.getMaxConcurrency() > 0) {
			if (bw.isWritableProperty("maxSessions")) {
				// ActiveMQ
				bw.setPropertyValue("maxSessions", Integer.toString(config.getMaxConcurrency()));
			}
			else if (bw.isWritableProperty("maxNumberOfWorks")) {
				// JORAM
				bw.setPropertyValue("maxNumberOfWorks", Integer.toString(config.getMaxConcurrency()));
			}
			else if (bw.isWritableProperty("maxConcurrency")){
				// WebSphere
				bw.setPropertyValue("maxConcurrency", Integer.toString(config.getMaxConcurrency()));
			}
		}
		if (config.getPrefetchSize() > 0) {
			if (bw.isWritableProperty("maxMessagesPerSessions")) {
				// ActiveMQ
				bw.setPropertyValue("maxMessagesPerSessions", Integer.toString(config.getPrefetchSize()));
			}
			else if (bw.isWritableProperty("maxMessages")) {
				// JORAM
				bw.setPropertyValue("maxMessages", Integer.toString(config.getPrefetchSize()));
			}
			else if (bw.isWritableProperty("maxBatchSize")){
				// WebSphere
				bw.setPropertyValue("maxBatchSize", Integer.toString(config.getPrefetchSize()));
			}
		}
	}

	/**
	 * 如果存在名为 "useRAManagedTransaction" 的 ActivationSpec 属性，
	 * 此实现会将 {@code SESSION_TRANSACTED} 映射到该属性
	 * （遵循 ActiveMQ 的命名约定）。
	 */
	@Override
	protected void applyAcknowledgeMode(BeanWrapper bw, int ackMode) {
		if (ackMode == Session.SESSION_TRANSACTED && bw.isWritableProperty("useRAManagedTransaction")) {
			// ActiveMQ
			bw.setPropertyValue("useRAManagedTransaction", "true");
		}
		else {
			super.applyAcknowledgeMode(bw, ackMode);
		}
	}

}
