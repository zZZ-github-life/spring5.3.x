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

package org.springframework.core.env;

import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.SpringProperties;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link Environment} 实现的抽象基类。支持保留默认 profile 名称的概念，并允许通过
 * {@link #ACTIVE_PROFILES_PROPERTY_NAME} 和{@link #DEFAULT_PROFILES_PROPERTY_NAME} 属性指定活动
 * profile 和默认 profile。
 *
 * <p>具体子类的主要区别在于默认添加哪些 {@link PropertySource} 对象。{@code AbstractEnvironment}
 * 不添加任何属性源。子类应通过受保护的{@link #customizePropertySources(MutablePropertySources)} 钩
 * 子提供属性源，而客户端应使用 {@link ConfigurableEnvironment#getPropertySources()} 进行自定义，并
 * 基于 {@link MutablePropertySources} API 操作。使用示例参见 {@link ConfigurableEnvironment} 的
 * javadoc。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1
 * @see ConfigurableEnvironment
 * @see StandardEnvironment
 */
public abstract class 	AbstractEnvironment implements ConfigurableEnvironment {

	/**
	 * 指示 Spring 忽略系统环境变量的系统属性，即永不尝试通过 {@link System#getenv()} 获取此类变量。
	 * <p>默认值为“false”，如果无法以其他方式解析 Spring 环境属性（例如配置 String 中的占位符），则回退
	 * 到系统环境变量检查。如果你遇到来自 Spring 的 {@code getenv} 调用所产生的日志警告，例如在具有严格
	 * SecurityManager 设置和 AccessControlExceptions 警告的 WebSphere 上，可以考虑将此标志切换为“true”。
	 *
	 * @see #suppressGetenvAccess()
	 */
	public static final String IGNORE_GETENV_PROPERTY_NAME = "spring.getenv.ignore";

	/**
	 * 用于指定活动 profile 的属性名称：{@value}。值可以用逗号分隔。
	 * <p>请注意，某些 shell 环境（如 Bash）不允许在变量名中使用句点字符。假设正在使用 Spring 的
	 * {@link SystemEnvironmentPropertySource}，则该属性可以通过环境变量 {@code SPRING_PROFILES_ACTIVE}
	 * 指定。
	 * @see ConfigurableEnvironment#setActiveProfiles
	 */
	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active";

	/**
	 * 用于指定默认活动 profile 的属性名称：{@value}。值可以用逗号分隔。
	 * <p>请注意，某些 shell 环境（如 Bash）不允许在变量名中使用句点字符。假设正在使用 Spring 的
	 * {@link SystemEnvironmentPropertySource}，则该属性可以通过环境变量
	 * {@code SPRING_PROFILES_DEFAULT} 指定。
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 */
	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profiles.default";

	/**
	 * 保留的默认 profile 名称的名称：{@value}。如果既未显式设置默认 profile 名称，也未显式设置活动
	 * profile 名称，则该 profile 将默认自动激活。
	 * @see #getReservedDefaultProfiles
	 * @see ConfigurableEnvironment#setDefaultProfiles
	 * @see ConfigurableEnvironment#setActiveProfiles
	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
	 */
	protected static final String RESERVED_DEFAULT_PROFILE_NAME = "default";


	protected final Log logger = LogFactory.getLog(getClass());

	private final Set<String> activeProfiles = new LinkedHashSet<>();

	private final Set<String> defaultProfiles = new LinkedHashSet<>(getReservedDefaultProfiles());

	private final MutablePropertySources propertySources;

	private final ConfigurablePropertyResolver propertyResolver;


	/**
	 * 创建一个新的 {@code Environment} 实例，并在构造期间回调
	 * {@link #customizePropertySources(MutablePropertySources)}，以便子类可以按需提供或操作
	 * {@link PropertySource} 实例。
	 * @see #customizePropertySources(MutablePropertySources)
	 */
	public AbstractEnvironment() {
		this(new MutablePropertySources());
	}

	/**
	 * 使用特定的 {@link MutablePropertySources} 实例创建一个新的 {@code Environment} 实例，并在构造期
	 * 间回调 {@link #customizePropertySources(MutablePropertySources)}，以便子类可以按需提供或操作
	 * {@link PropertySource} 实例。
	 * @param propertySources property sources to use
	 * @since 5.3.4
	 * @see #customizePropertySources(MutablePropertySources)
	 */
	protected AbstractEnvironment(MutablePropertySources propertySources) {
		this.propertySources = propertySources;
		this.propertyResolver = createPropertyResolver(propertySources);
		customizePropertySources(propertySources);
	}


	/**
	 * 用于创建 Environment 所使用的 {@link ConfigurablePropertyResolver} 实例的工厂方法。
	 * @since 5.3.4
	 * @see #getPropertyResolver()
	 */
	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
		return new PropertySourcesPropertyResolver(propertySources);
	}

	/**
	 * 返回 {@link Environment} 正在使用的 {@link ConfigurablePropertyResolver}。
	 * @since 5.3.4
	 * @see #createPropertyResolver(MutablePropertySources)
	 */
	protected final ConfigurablePropertyResolver getPropertyResolver() {
		return this.propertyResolver;
	}

	/**
	 * 自定义在调用 {@link #getProperty(String)} 及相关方法期间由此 {@code Environment} 搜索的
	 * {@link PropertySource} 对象集合。
	 *
	 * <p>建议重写此方法的子类使用 {@link MutablePropertySources#addLast(PropertySource)} 添加属性源，
	 * 这样进一步派生的子类调用 {@code super.customizePropertySources()} 时可获得可预测的结果。例如：
	 *
	 * <pre class="code">
	 * public class Level1Environment extends AbstractEnvironment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // no-op from base class
	 *         propertySources.addLast(new PropertySourceA(...));
	 *         propertySources.addLast(new PropertySourceB(...));
	 *     }
	 * }
	 *
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>在这种安排下，属性将按 A、B、C、D 的顺序在这些源中解析。也就是说，属性源“A”的优先级高于属性源
	 * “D”。如果 {@code Level2Environment} 子类希望属性源 C 和 D 的优先级高于 A 和 B，只需在添加自己的
	 * 属性源之后（而不是之前）调用 {@code super.customizePropertySources}：
	 *
	 * <pre class="code">
	 * public class Level2Environment extends Level1Environment {
	 *     &#064;Override
	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
	 *         propertySources.addLast(new PropertySourceC(...));
	 *         propertySources.addLast(new PropertySourceD(...));
	 *         super.customizePropertySources(propertySources); // add all from superclass
	 *     }
	 * }
	 * </pre>
	 *
	 * <p>现在搜索顺序正如所愿，为 C、D、A、B。
	 *
	 * <p>除这些建议之外，子类还可以使用 {@link MutablePropertySources} 暴露的任意 {@code add*}、
	 * {@code remove} 或 {@code replace} 方法，以创建所需的精确属性源安排。
	 *
	 * <p>基类实现不注册任何属性源。
	 *
	 * <p>请注意，任何 {@link ConfigurableEnvironment} 的客户端都可以通过{@link #getPropertySources()}
	 * 访问器进一步自定义属性源，通常是在一个
	 * {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer}
	 * 中。例如：
	 *
	 * <pre class="code">
	 * ConfigurableEnvironment env = new StandardEnvironment();
	 * env.getPropertySources().addLast(new PropertySourceX(...));
	 * </pre>
	 *
	 * <h2>关于实例变量访问的警告</h2>
	 * <p>子类中声明的、具有默认初始值的实例变量<em>不应</em>在此方法内访问。由于 Java 对象创建生命周期
	 * 的限制，当 {@link #AbstractEnvironment()} 构造函数调用此回调时，任何初始值都尚未赋值，这可能导致
	 * {@code NullPointerException} 或其他问题。如果需要访问实例变量的默认值，请将此方法保留为空操作，
	 * 并直接在子类构造函数中执行属性源操作和实例变量访问。请注意，为实例变量<em>赋值</em>没有问题；只
	 * 有尝试读取默认值才是必须避免的。
	 * @see MutablePropertySources
	 * @see PropertySourcesPropertyResolver
	 * @see org.springframework.context.ApplicationContextInitializer
	 */
	protected void customizePropertySources(MutablePropertySources propertySources) {
	}

	/**
	 * 返回保留的默认 profile 名称集合。此实现返回 {@value #RESERVED_DEFAULT_PROFILE_NAME}。子类可以重
	 * 写以自定义保留名称集合。
	 * @see #RESERVED_DEFAULT_PROFILE_NAME
	 * @see #doGetDefaultProfiles()
	 */
	protected Set<String> getReservedDefaultProfiles() {
		return Collections.singleton(RESERVED_DEFAULT_PROFILE_NAME);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableEnvironment interface
	//---------------------------------------------------------------------

	@Override
	public String[] getActiveProfiles() {
		return StringUtils.toStringArray(doGetActiveProfiles());
	}

	/**
	 * 返回通过 {@link #setActiveProfiles} 显式设置的活动 profile 集合；如果当前活动 profile 集合为空，
	 * 则检查 {@link #doGetActiveProfilesProperty()} 是否存在，并将其值赋给活动 profile 集合。
	 * @see #getActiveProfiles()
	 * @see #doGetActiveProfilesProperty()
	 */
	protected Set<String> doGetActiveProfiles() {
		synchronized (this.activeProfiles) {
			if (this.activeProfiles.isEmpty()) {
				String profiles = doGetActiveProfilesProperty();
				if (StringUtils.hasText(profiles)) {
					setActiveProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.activeProfiles;
		}
	}

	/**
	 * 返回活动 profile 的属性值。
	 * @since 5.3.4
	 * @see #ACTIVE_PROFILES_PROPERTY_NAME
	 */
	@Nullable
	protected String doGetActiveProfilesProperty() {
		return getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
	}

	@Override
	public void setActiveProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profiles " + Arrays.toString(profiles));
		}
		synchronized (this.activeProfiles) {
			this.activeProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.activeProfiles.add(profile);
			}
		}
	}

	@Override
	public void addActiveProfile(String profile) {
		if (logger.isDebugEnabled()) {
			logger.debug("Activating profile '" + profile + "'");
		}
		validateProfile(profile);
		doGetActiveProfiles();
		synchronized (this.activeProfiles) {
			this.activeProfiles.add(profile);
		}
	}


	@Override
	public String[] getDefaultProfiles() {
		return StringUtils.toStringArray(doGetDefaultProfiles());
	}

	/**
	 * 返回通过 {@link #setDefaultProfiles(String...)} 显式设置的默认 profile 集合；如果当前默认
	 * profile 集合仅由 {@linkplain #getReservedDefaultProfiles() 保留的默认 profile} 组成，则检查
	 * {@link #doGetActiveProfilesProperty()} 是否存在，并将其值（如果有）赋给默认 profile 集合。
	 * @see #AbstractEnvironment()
	 * @see #getDefaultProfiles()
	 * @see #getReservedDefaultProfiles()
	 * @see #doGetDefaultProfilesProperty()
	 */
	protected Set<String> doGetDefaultProfiles() {
		synchronized (this.defaultProfiles) {
			if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
				String profiles = doGetDefaultProfilesProperty();
				if (StringUtils.hasText(profiles)) {
					setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(
							StringUtils.trimAllWhitespace(profiles)));
				}
			}
			return this.defaultProfiles;
		}
	}

	/**
	 * 返回默认 profile 的属性值。
	 * @since 5.3.4
	 * @see #DEFAULT_PROFILES_PROPERTY_NAME
	 */
	@Nullable
	protected String doGetDefaultProfilesProperty() {
		return getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
	}

	/**
	 * 指定在没有通过 {@link #setActiveProfiles} 显式激活其他 profile 时默认激活的 profile 集合。
	 * <p>调用此方法会移除并覆盖在环境构造期间可能添加的任何保留默认 profile。
	 * @see #AbstractEnvironment()
	 * @see #getReservedDefaultProfiles()
	 */
	@Override
	public void setDefaultProfiles(String... profiles) {
		Assert.notNull(profiles, "Profile array must not be null");
		synchronized (this.defaultProfiles) {
			this.defaultProfiles.clear();
			for (String profile : profiles) {
				validateProfile(profile);
				this.defaultProfiles.add(profile);
			}
		}
	}

	@Override
	@Deprecated
	public boolean acceptsProfiles(String... profiles) {
		Assert.notEmpty(profiles, "Must specify at least one profile");
		for (String profile : profiles) {
			if (StringUtils.hasLength(profile) && profile.charAt(0) == '!') {
				if (!isProfileActive(profile.substring(1))) {
					return true;
				}
			}
			else if (isProfileActive(profile)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean acceptsProfiles(Profiles profiles) {
		Assert.notNull(profiles, "Profiles must not be null");
		return profiles.matches(this::isProfileActive);
	}

	/**
	 * 返回给定 profile 是否处于活动状态；如果活动 profile 为空，则返回该 profile 是否应默认处于活动状
	 * 态。
	 * @throws IllegalArgumentException per {@link #validateProfile(String)}
	 */
	protected boolean isProfileActive(String profile) {
		validateProfile(profile);
		Set<String> currentActiveProfiles = doGetActiveProfiles();
		return (currentActiveProfiles.contains(profile) ||
				(currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile)));
	}

	/**
	 * 校验给定的 profile，在添加到活动或默认 profile 集合之前由内部调用。
	 * <p>子类可以重写以对 profile 语法施加更多限制。
	 * @throws IllegalArgumentException if the profile is null, empty, whitespace-only or
	 * begins with the profile NOT operator (!).
	 * @see #acceptsProfiles
	 * @see #addActiveProfile
	 * @see #setDefaultProfiles
	 */
	protected void validateProfile(String profile) {
		if (!StringUtils.hasText(profile)) {
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must contain text");
		}
		if (profile.charAt(0) == '!') {
			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not begin with ! operator");
		}
	}

	@Override
	public MutablePropertySources getPropertySources() {
		return this.propertySources;
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemProperties() {
		try {
			return (Map) System.getProperties();
		}
		catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getProperty(attributeName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system property '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Map<String, Object> getSystemEnvironment() {
		if (suppressGetenvAccess()) {
			return Collections.emptyMap();
		}
		try {
			return (Map) System.getenv();
		}
		catch (AccessControlException ex) {
			return (Map) new ReadOnlySystemAttributesMap() {
				@Override
				@Nullable
				protected String getSystemAttribute(String attributeName) {
					try {
						return System.getenv(attributeName);
					}
					catch (AccessControlException ex) {
						if (logger.isInfoEnabled()) {
							logger.info("Caught AccessControlException when accessing system environment variable '" +
									attributeName + "'; its value will be returned [null]. Reason: " + ex.getMessage());
						}
						return null;
					}
				}
			};
		}
	}

	/**
	 * 确定是否为 {@link #getSystemEnvironment()} 的目的而禁止访问{@link System#getenv()}/
	 * {@link System#getenv(String)}。
	 * <p>如果此方法返回 {@code true}，将使用空的占位 Map 代替常规的系统环境 Map，甚至绝不会尝试调用
	 * {@code getenv}，从而避免安全管理器警告（如果有）。
	 * <p>默认实现检查“spring.getenv.ignore”系统属性，如果其值在任意大小写下等于“true”，则返回
	 * {@code true}。
	 * @see #IGNORE_GETENV_PROPERTY_NAME
	 * @see SpringProperties#getFlag
	 */
	protected boolean suppressGetenvAccess() {
		return SpringProperties.getFlag(IGNORE_GETENV_PROPERTY_NAME);
	}

	@Override
	public void merge(ConfigurableEnvironment parent) {
		for (PropertySource<?> ps : parent.getPropertySources()) {
			if (!this.propertySources.contains(ps.getName())) {
				this.propertySources.addLast(ps);
			}
		}
		String[] parentActiveProfiles = parent.getActiveProfiles();
		if (!ObjectUtils.isEmpty(parentActiveProfiles)) {
			synchronized (this.activeProfiles) {
				Collections.addAll(this.activeProfiles, parentActiveProfiles);
			}
		}
		String[] parentDefaultProfiles = parent.getDefaultProfiles();
		if (!ObjectUtils.isEmpty(parentDefaultProfiles)) {
			synchronized (this.defaultProfiles) {
				this.defaultProfiles.remove(RESERVED_DEFAULT_PROFILE_NAME);
				Collections.addAll(this.defaultProfiles, parentDefaultProfiles);
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurablePropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public ConfigurableConversionService getConversionService() {
		return this.propertyResolver.getConversionService();
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		this.propertyResolver.setConversionService(conversionService);
	}

	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
	}

	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
	}

	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.propertyResolver.setValueSeparator(valueSeparator);
	}

	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		this.propertyResolver.setRequiredProperties(requiredProperties);
	}

	@Override
	public void validateRequiredProperties() throws MissingRequiredPropertiesException {
		this.propertyResolver.validateRequiredProperties();
	}


	//---------------------------------------------------------------------
	// Implementation of PropertyResolver interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsProperty(String key) {
		return this.propertyResolver.containsProperty(key);
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		return this.propertyResolver.getProperty(key);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		return this.propertyResolver.getProperty(key, defaultValue);
	}

	@Override
	@Nullable
	public <T> T getProperty(String key, Class<T> targetType) {
		return this.propertyResolver.getProperty(key, targetType);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		return this.propertyResolver.getProperty(key, targetType, defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key);
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
		return this.propertyResolver.getRequiredProperty(key, targetType);
	}

	@Override
	public String resolvePlaceholders(String text) {
		return this.propertyResolver.resolvePlaceholders(text);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		return this.propertyResolver.resolveRequiredPlaceholders(text);
	}


	@Override
	public String toString() {
		return getClass().getSimpleName() + " {activeProfiles=" + this.activeProfiles +
				", defaultProfiles=" + this.defaultProfiles + ", propertySources=" + this.propertySources + "}";
	}

}
