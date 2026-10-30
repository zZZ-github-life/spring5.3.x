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

package org.springframework.beans.factory.config;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.core.Constants;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;

/**
 * {@link PlaceholderConfigurerSupport} 的子类，针对 {@link #setLocation local}
 * {@link #setProperties properties} 和/或系统属性以及环境变量解析 ${...} 占位符。
 *
 * <p>在以下情况下，{@link PropertyPlaceholderConfigurer} 仍然适用：
 * <ul>
 * <li>无法使用 {@code spring-context} 模块时（即使用 Spring 的 {@code BeanFactory} API 而非 {@code ApplicationContext}）。
 * <li>现有配置使用了 {@link #setSystemPropertiesMode(int) "systemPropertiesMode"} 和/或 {@link #setSystemPropertiesModeName(String) "systemPropertiesModeName"} 属性。鼓励用户不再使用这些设置，而是通过容器的 {@code Environment} 配置属性源搜索顺序；不过，继续使用 {@code PropertyPlaceholderConfigurer} 可以保持功能完全不变。
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 02.10.2003
 * @see #setSystemPropertiesModeName
 * @see PlaceholderConfigurerSupport
 * @see PropertyOverrideConfigurer
 * @deprecated as of 5.2; use {@code org.springframework.context.support.PropertySourcesPlaceholderConfigurer}
 * instead which is more flexible through taking advantage of the {@link org.springframework.core.env.Environment}
 * and {@link org.springframework.core.env.PropertySource} mechanisms.
 */
@Deprecated
public class PropertyPlaceholderConfigurer extends PlaceholderConfigurerSupport {

	/** 从不检查系统属性。 */
	public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

	/**
	 * 如果在指定的属性中无法解析，则检查系统属性。这是默认值。
	 */
	public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

	/**
	 * 先检查系统属性，然后再尝试指定的属性。这样系统属性就能覆盖任何其他属性源。
	 */
	public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;


	private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

	private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

	private boolean searchSystemEnvironment =
			!SpringProperties.getFlag(AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME);


	/**
	 * 通过对应常量的名称设置系统属性模式，例如 "SYSTEM_PROPERTIES_MODE_OVERRIDE"。
	 * @param constantName 常量的名称
	 * @see #setSystemPropertiesMode
	 */
	public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
		this.systemPropertiesMode = constants.asNumber(constantName).intValue();
	}

	/**
	 * 设置如何检查系统属性：作为回退、作为覆盖，或从不检查。例如，会把 ${user.dir} 解析为 "user.dir"
	 * 系统属性。
	 * <p>默认值为 "fallback"：如果无法使用指定的属性解析某个占位符，则会尝试系统属性。"override" 会先
	 * 检查系统属性，然后再尝试指定的属性。"never" 则完全不检查系统属性。
	 * @see #SYSTEM_PROPERTIES_MODE_NEVER
	 * @see #SYSTEM_PROPERTIES_MODE_FALLBACK
	 * @see #SYSTEM_PROPERTIES_MODE_OVERRIDE
	 * @see #setSystemPropertiesModeName
	 */
	public void setSystemPropertiesMode(int systemPropertiesMode) {
		this.systemPropertiesMode = systemPropertiesMode;
	}

	/**
	 * 设置是否在未找到匹配的系统属性时搜索匹配的系统环境变量。仅当 "systemPropertyMode" 处于活动状态
	 * （即 "fallback" 或 "override"）时，且在检查 JVM 系统属性之后立即应用。
	 * <p>默认值为 "true"。关闭此设置可完全不针对系统环境变量解析占位符。请注意，通常建议将外部值作为
	 * JVM 系统属性传入：即使在启动脚本中，也能轻松做到这一点，对现有环境变量也同样适用。
	 * @see #setSystemPropertiesMode
	 * @see System#getProperty(String)
	 * @see System#getenv(String)
	 */
	public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
		this.searchSystemEnvironment = searchSystemEnvironment;
	}

	/**
	 * 使用给定属性解析给定占位符，并根据给定模式执行系统属性检查。
	 * <p>默认实现在系统属性检查之前/之后委托给 {@code resolvePlaceholder(placeholder, props)}。
	 * <p>子类可以覆盖此方法以使用自定义解析策略，包括自定义系统属性检查的时机。
	 * @param placeholder 要解析的占位符
	 * @param props 此配置器合并后的属性
	 * @param systemPropertiesMode 系统属性模式，
	 * 参见本类中的常量
	 * @return 解析后的值；如果没有则为 null
	 * @see #setSystemPropertiesMode
	 * @see System#getProperty
	 * @see #resolvePlaceholder(String, java.util.Properties)
	 */
	@Nullable
	protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
		String propVal = null;
		if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
			propVal = resolveSystemProperty(placeholder);
		}
		if (propVal == null) {
			propVal = resolvePlaceholder(placeholder, props);
		}
		if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
			propVal = resolveSystemProperty(placeholder);
		}
		return propVal;
	}

	/**
	 * 使用给定属性解析给定占位符。默认实现仅检查对应的属性键。
	 * <p>子类可以覆盖此方法以自定义占位符到键的映射或自定义解析策略，例如仅使用给定属性作为回退。
	 * <p>请注意，根据系统属性模式，仍会在调用此方法之前或之后相应地检查系统属性。
	 * @param placeholder 要解析的占位符
	 * @param props 此配置器合并后的属性
	 * @return 解析后的值；如果没有则为 {@code null}
	 * @see #setSystemPropertiesMode
	 */
	@Nullable
	protected String resolvePlaceholder(String placeholder, Properties props) {
		return props.getProperty(placeholder);
	}

	/**
	 * 将给定键解析为 JVM 系统属性；如果未找到匹配的系统属性，还可以选择解析为系统环境变量。
	 * @param key 要作为系统属性键解析的占位符
	 * @return 系统属性值；如果未找到则为 {@code null}
	 * @see #setSearchSystemEnvironment
	 * @see System#getProperty(String)
	 * @see System#getenv(String)
	 */
	@Nullable
	protected String resolveSystemProperty(String key) {
		try {
			String value = System.getProperty(key);
			if (value == null && this.searchSystemEnvironment) {
				value = System.getenv(key);
			}
			return value;
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access system property '" + key + "': " + ex);
			}
			return null;
		}
	}


	/**
	 * 遍历给定 Bean 工厂中的每个 Bean 定义，并尝试用给定属性中的值替换 ${...} 属性占位符。
	 */
	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {

		StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);
		doProcessProperties(beanFactoryToProcess, valueResolver);
	}


	private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

		private final PropertyPlaceholderHelper helper;

		private final PlaceholderResolver resolver;

		public PlaceholderResolvingStringValueResolver(Properties props) {
			this.helper = new PropertyPlaceholderHelper(
					placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
			this.resolver = new PropertyPlaceholderConfigurerResolver(props);
		}

		@Override
		@Nullable
		public String resolveStringValue(String strVal) throws BeansException {
			String resolved = this.helper.replacePlaceholders(strVal, this.resolver);
			if (trimValues) {
				resolved = resolved.trim();
			}
			return (resolved.equals(nullValue) ? null : resolved);
		}
	}


	private final class PropertyPlaceholderConfigurerResolver implements PlaceholderResolver {

		private final Properties props;

		private PropertyPlaceholderConfigurerResolver(Properties props) {
			this.props = props;
		}

		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			return PropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName,
					this.props, systemPropertiesMode);
		}
	}

}
