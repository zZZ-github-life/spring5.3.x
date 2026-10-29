/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanInitializationException;

/**
 * 属性资源配置器，用于覆盖应用上下文定义中的 Bean 属性值。它将属性文件中的值<i>推送</i>到 Bean 定
 * 义中。
 *
 * <p>配置行应具有以下形式：
 *
 * <pre class="code">beanName.property=value</pre>
 *
 * 示例属性文件：
 *
 * <pre class="code">dataSource.driverClassName=com.mysql.jdbc.Driver
 * dataSource.url=jdbc:mysql:mydb</pre>
 *
 * 与 PropertyPlaceholderConfigurer 不同，原始定义可以为这些 Bean 属性提供默认值，或者完全不提供值。
 * 如果覆盖用的属性文件没有针对某个 Bean 属性的条目，则使用默认的上下文定义。
 *
 * <p>请注意，上下文定义<i>并不</i>知道自己被覆盖；因此在查看 XML 定义文件时并不会立即察觉。此外，
 * 请注意指定的覆盖值始终是<i>字面</i>值；它们不会被转换为 Bean 引用。当 XML Bean 定义中的原始值指
 * 定了 Bean 引用时也是如此。
 *
 * <p>如果有多个 PropertyOverrideConfigurer 为同一 Bean 属性定义了不同的值，则<i>最后</i>一个会生效
 * （这是覆盖机制决定的）。
 *
 * <p>在读取属性值之后，可以通过重写 {@code convertPropertyValue} 方法对其进行转换。例如，可以在处
 * 理之前检测加密值并相应地进行解密。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 12.03.2003
 * @see #convertPropertyValue
 * @see PropertyPlaceholderConfigurer
 */
public class PropertyOverrideConfigurer extends PropertyResourceConfigurer {

	/**
	 * 默认的 Bean 名称分隔符。
	 */
	public static final String DEFAULT_BEAN_NAME_SEPARATOR = ".";


	private String beanNameSeparator = DEFAULT_BEAN_NAME_SEPARATOR;

	private boolean ignoreInvalidKeys = false;

	/**
	 * 包含具有覆盖值的 Bean 名称。
	 */
	private final Set<String> beanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(16));


	/**
	 * 设置 Bean 名称与属性路径之间预期的分隔符。默认为点号（"."）。
	 */
	public void setBeanNameSeparator(String beanNameSeparator) {
		this.beanNameSeparator = beanNameSeparator;
	}

	/**
	 * 设置是否忽略无效的键。默认为 "false"。
	 * <p>如果忽略无效的键，那么不符合 'beanName.property' 格式（或引用了无效的 Bean 名称或属性）的键只
	 * 会以 debug 级别记录日志。这样就能在属性文件中保留任意其他键。
	 */
	public void setIgnoreInvalidKeys(boolean ignoreInvalidKeys) {
		this.ignoreInvalidKeys = ignoreInvalidKeys;
	}


	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException {

		for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements();) {
			String key = (String) names.nextElement();
			try {
				processKey(beanFactory, key, props.getProperty(key));
			}
			catch (BeansException ex) {
				String msg = "Could not process key '" + key + "' in PropertyOverrideConfigurer";
				if (!this.ignoreInvalidKeys) {
					throw new BeanInitializationException(msg, ex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug(msg, ex);
				}
			}
		}
	}

	/**
	 * 将给定的键作为 'beanName.property' 条目处理。
	 */
	protected void processKey(ConfigurableListableBeanFactory factory, String key, String value)
			throws BeansException {

		int separatorIndex = key.indexOf(this.beanNameSeparator);
		if (separatorIndex == -1) {
			throw new BeanInitializationException("Invalid key '" + key +
					"': expected 'beanName" + this.beanNameSeparator + "property'");
		}
		String beanName = key.substring(0, separatorIndex);
		String beanProperty = key.substring(separatorIndex + 1);
		this.beanNames.add(beanName);
		applyPropertyValue(factory, beanName, beanProperty, value);
		if (logger.isDebugEnabled()) {
			logger.debug("Property '" + key + "' set to value [" + value + "]");
		}
	}

	/**
	 * 将给定的属性值应用到对应的 Bean。
	 */
	protected void applyPropertyValue(
			ConfigurableListableBeanFactory factory, String beanName, String property, String value) {

		BeanDefinition bd = factory.getBeanDefinition(beanName);
		BeanDefinition bdToUse = bd;
		while (bd != null) {
			bdToUse = bd;
			bd = bd.getOriginatingBeanDefinition();
		}
		PropertyValue pv = new PropertyValue(property, value);
		pv.setOptional(this.ignoreInvalidKeys);
		bdToUse.getPropertyValues().addPropertyValue(pv);
	}


	/**
	 * 此 Bean 是否有覆盖值？仅在至少执行过一次处理之后才有效。
	 * @param beanName name of the bean to query status for
	 * @return whether there were property overrides for the named bean
	 */
	public boolean hasPropertyOverridesFor(String beanName) {
		return this.beanNames.contains(beanName);
	}

}
