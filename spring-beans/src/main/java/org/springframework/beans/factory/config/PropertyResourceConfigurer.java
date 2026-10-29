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

package org.springframework.beans.factory.config;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.util.ObjectUtils;

/**
 * 允许从属性资源（即 properties 文件）配置各个 Bean 属性值。适用于面向系统管理员、用于覆盖应用上下
 * 文中配置的 Bean 属性的自定义配置文件。
 *
 * <p>发行包中提供了两个具体实现：
 * <ul>
 * <li>{@link PropertyOverrideConfigurer} 用于 "beanName.property=value" 风格的覆盖（将属性文件中的值<i>推入</i> Bean 定义）
 * <li>{@link PropertyPlaceholderConfigurer} 用于替换 "${...}" 占位符（将属性文件中的值<i>拉取</i>到 Bean 定义中）
 * </ul>
 *
 * <p>属性值读入后可以通过覆盖 {@link #convertPropertyValue} 方法进行转换。例如，可以在处理之前检测
 * 加密值并相应解密。
 *
 * @author Juergen Hoeller
 * @since 02.10.2003
 * @see PropertyOverrideConfigurer
 * @see PropertyPlaceholderConfigurer
 */
public abstract class PropertyResourceConfigurer extends PropertiesLoaderSupport
		implements BeanFactoryPostProcessor, PriorityOrdered {

	private int order = Ordered.LOWEST_PRECEDENCE;  // default: same as non-Ordered


	/**
	 * 设置此对象的顺序值，用于排序。
	 * @see PriorityOrdered
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	/**
	 * 针对给定的 Bean 工厂{@linkplain #mergeProperties Merge}、{@linkplain #convertProperties convert}
	 * 和 {@linkplain #processProperties process} 属性。
	 * @throws BeanInitializationException if any properties cannot be loaded
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		try {
			Properties mergedProps = mergeProperties();

			// Convert the merged properties, if necessary.
			convertProperties(mergedProps);

			// Let the subclass process the properties.
			processProperties(beanFactory, mergedProps);
		}
		catch (IOException ex) {
			throw new BeanInitializationException("Could not load properties", ex);
		}
	}

	/**
	 * 转换给定的已合并属性，必要时转换属性值。随后将处理结果。
	 * <p>默认实现会为每个属性值调用 {@link #convertPropertyValue}，用转换后的值替换原值。
	 * @param props the Properties to convert
	 * @see #processProperties
	 */
	protected void convertProperties(Properties props) {
		Enumeration<?> propertyNames = props.propertyNames();
		while (propertyNames.hasMoreElements()) {
			String propertyName = (String) propertyNames.nextElement();
			String propertyValue = props.getProperty(propertyName);
			String convertedValue = convertProperty(propertyName, propertyValue);
			if (!ObjectUtils.nullSafeEquals(propertyValue, convertedValue)) {
				props.setProperty(propertyName, convertedValue);
			}
		}
	}

	/**
	 * 将属性源中的给定属性转换为应当应用的值。
	 * <p>默认实现会调用 {@link #convertPropertyValue(String)}。
	 * @param propertyName the name of the property that the value is defined for
	 * @param propertyValue the original value from the properties source
	 * @return the converted value, to be used for processing
	 * @see #convertPropertyValue(String)
	 */
	protected String convertProperty(String propertyName, String propertyValue) {
		return convertPropertyValue(propertyValue);
	}

	/**
	 * 将属性源中的给定属性值转换为应当应用的值。
	 * <p>默认实现仅返回原值。可以在子类中覆盖，例如用于检测加密值并相应解密。
	 * @param originalValue the original value from the properties source
	 * (properties file or local "properties")
	 * @return the converted value, to be used for processing
	 * @see #setProperties
	 * @see #setLocations
	 * @see #setLocation
	 * @see #convertProperty(String, String)
	 */
	protected String convertPropertyValue(String originalValue) {
		return originalValue;
	}


	/**
	 * 将给定的 Properties 应用到给定的 BeanFactory。
	 * @param beanFactory the BeanFactory used by the application context
	 * @param props the Properties to apply
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	protected abstract void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException;

}
