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

package org.springframework.context.support;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

/**
 * {@link PlaceholderConfigurerSupport} 的专用版本，针对当前的 Spring {@link Environment} 及其
 * {@link PropertySources} 集合，解析 Bean 定义属性值和 {@code @Value} 注解中的 ${...} 占位符。
 *
 * <p>该类被设计为 {@code PropertyPlaceholderConfigurer} 的通用替代品。针对 spring-context-3.1 或更
 * 高版本的 XSD 时，默认使用它来支持 {@code property-placeholder} 元素；而对于 spring-context 3.0
 * 及以下版本（&lt;= 3.0），为确保向后兼容，默认使用 {@code PropertyPlaceholderConfigurer}。完整细
 * 节请参见 spring-context XSD 文档。
 *
 * <p>任何本地属性（例如通过 {@link #setProperties}、{@link #setLocations} 等添加的属性）都会作为一
 * 个 {@code PropertySource} 添加。本地属性的搜索优先级取决于
 * {@link #setLocalOverride localOverride} 属性的值，该属性默认为 {@code false}，意味着本地属性最后
 * 被搜索，即在所有环境属性源之后。
 *
 * <p>有关操作环境属性源的详细信息，请参见
 * {@link org.springframework.core.env.ConfigurableEnvironment} 及相关 Javadoc。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 * @see org.springframework.core.env.ConfigurableEnvironment
 * @see org.springframework.beans.factory.config.PlaceholderConfigurerSupport
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
public class PropertySourcesPlaceholderConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

	/**
	 * {@value} 是提供给此配置器的 {@linkplain #mergeProperties() merged properties} 集合所对应
	 * {@link PropertySource} 的名称。
	 */
	public static final String LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME = "localProperties";

	/**
	 * {@value} 是包装提供给此配置器的 {@linkplain #setEnvironment environment} 的
	 * {@link PropertySource} 的名称。
	 */
	public static final String ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME = "environmentProperties";


	@Nullable
	private MutablePropertySources propertySources;

	@Nullable
	private PropertySources appliedPropertySources;

	@Nullable
	private Environment environment;


	/**
	 * 自定义此配置器要使用的 {@link PropertySources} 集合。
	 * <p>设置此属性表示应忽略环境属性源和本地属性。
	 * @see #postProcessBeanFactory
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = new MutablePropertySources(propertySources);
	}

	/**
	 * 替换 ${...} 占位符时，将搜索给定 {@link Environment} 中的 {@code PropertySources}。
	 * @see #setPropertySources
	 * @see #postProcessBeanFactory
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}


	/**
	 * 处理过程会替换 Bean 定义中的 ${...} 占位符，具体做法是逐个针对此配置器的 {@link PropertySources}
	 * 集合进行解析，该集合包括：
	 * <ul>
	 * <li>如果存在 {@code Environment}（即 {@linkplain #setEnvironment 已设置}），则为所有 {@linkplain org.springframework.core.env.ConfigurableEnvironment#getPropertySources 环境属性源}
	 * <li>如果通过 {@linkplain #setLocation}、{@linkplain #setLocations}、{@linkplain #setProperties} 或 {@linkplain #setPropertiesArray} 指定了任何属性，则为 {@linkplain #mergeProperties 合并的本地属性}
	 * <li>通过调用 {@link #setPropertySources} 设置的任何属性源
	 * </ul>
	 * <p>如果调用了 {@link #setPropertySources}，<strong>环境属性和本地属性将被忽略</strong>。此方法旨
	 * 在让用户对属性源进行细粒度控制，一旦设置，配置器就不再对添加额外的源作任何假设。
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertySources == null) {
			this.propertySources = new MutablePropertySources();
			if (this.environment != null) {
				PropertyResolver propertyResolver = this.environment;
				// If the ignoreUnresolvablePlaceholders flag is set to true, we have to create a
				// local PropertyResolver to enforce that setting, since the Environment is most
				// likely not configured with ignoreUnresolvablePlaceholders set to true.
				// See https://github.com/spring-projects/spring-framework/issues/27947
				if (this.ignoreUnresolvablePlaceholders && (this.environment instanceof ConfigurableEnvironment)) {
					ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) this.environment;
					PropertySourcesPropertyResolver resolver =
							new PropertySourcesPropertyResolver(configurableEnvironment.getPropertySources());
					resolver.setIgnoreUnresolvableNestedPlaceholders(true);
					propertyResolver = resolver;
				}
				PropertyResolver propertyResolverToUse = propertyResolver;
				this.propertySources.addLast(
					new PropertySource<Environment>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
						@Override
						@Nullable
						public String getProperty(String key) {
							return propertyResolverToUse.getProperty(key);
						}
					}
				);
			}
			try {
				PropertySource<?> localPropertySource =
						new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, mergeProperties());
				if (this.localOverride) {
					this.propertySources.addFirst(localPropertySource);
				}
				else {
					this.propertySources.addLast(localPropertySource);
				}
			}
			catch (IOException ex) {
				throw new BeanInitializationException("Could not load properties", ex);
			}
		}

		processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
		this.appliedPropertySources = this.propertySources;
	}

	/**
	 * 遍历给定 Bean 工厂中的每个 Bean 定义，并尝试用给定属性中的值替换 ${...} 属性占位符。
	 */
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			final ConfigurablePropertyResolver propertyResolver) throws BeansException {

		propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
		propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
		propertyResolver.setValueSeparator(this.valueSeparator);

		StringValueResolver valueResolver = strVal -> {
			String resolved = (this.ignoreUnresolvablePlaceholders ?
					propertyResolver.resolvePlaceholders(strVal) :
					propertyResolver.resolveRequiredPlaceholders(strVal));
			if (this.trimValues) {
				resolved = resolved.trim();
			}
			return (resolved.equals(this.nullValue) ? null : resolved);
		};

		doProcessProperties(beanFactoryToProcess, valueResolver);
	}

	/**
	 * 为兼容 {@link org.springframework.beans.factory.config.PlaceholderConfigurerSupport} 而实现。
	 * @throws UnsupportedOperationException 在此实现中
	 * @deprecated in favor of
	 * {@link #processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver)}
	 */
	@Override
	@Deprecated
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
		throw new UnsupportedOperationException(
				"Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
	}

	/**
	 * 返回在{@link #postProcessBeanFactory(ConfigurableListableBeanFactory) 后处理}期间实际应用的属性
	 * 源。
	 * @return 已应用的属性源
	 * @throws IllegalStateException 如果属性源尚未应用
	 * @since 4.0
	 */
	public PropertySources getAppliedPropertySources() throws IllegalStateException {
		Assert.state(this.appliedPropertySources != null, "PropertySources have not yet been applied");
		return this.appliedPropertySources;
	}

}
