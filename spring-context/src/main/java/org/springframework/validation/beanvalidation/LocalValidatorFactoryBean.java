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

package org.springframework.validation.beanvalidation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.Validator;
import javax.validation.ValidatorContext;
import javax.validation.ValidatorFactory;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 这是在 Spring 应用上下文中进行 {@code javax.validation}（JSR-303）设置的核心类：它引导
 * {@code javax.validation.ValidationFactory}，并通过 Spring 的
 * {@link org.springframework.validation.Validator} 接口、JSR-303 的
 * {@link javax.validation.Validator} 接口以及 {@link javax.validation.ValidatorFactory} 接口本身将
 * 其公开。
 *
 * <p>通过 Spring 或 JSR-303 Validator 接口与此 Bean 的实例交互时，你与之交互的是底层
 * ValidatorFactory 的默认 Validator。这非常方便，因为假定你几乎总是使用默认 Validator，就不必再对
 * 工厂进行一次调用。它也可以直接注入到类型为 {@link org.springframework.validation.Validator} 的任
 * 何目标依赖中！
 *
 * <p><b>从 Spring 5.0 开始，此类要求 Bean Validation 1.1+，并对 Hibernate Validator 5.x 提供特殊支
 * 持</b>（请参见 {@link #setValidationMessageSource}）。此类在运行时也可与 Bean Validation 2.0 和
 * Hibernate Validator 6.0 兼容，但有一点特别说明：如果要调用 BV 2.0 的 {@code getClockProvider()}
 * 方法，请通过 {@code #unwrap(ValidatorFactory.class)} 获取原生 {@code ValidatorFactory}，并在返回
 * 的原生引用上调用 {@code getClockProvider()} 方法。
 *
 * <p>如果存在 {@code javax.validation} API 但没有配置显式的 Validator，Spring 的 MVC 配置命名空间
 * 也会使用此类。
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see javax.validation.ValidatorFactory
 * @see javax.validation.Validator
 * @see javax.validation.Validation#buildDefaultValidatorFactory()
 * @see javax.validation.ValidatorFactory#getValidator()
 */
public class LocalValidatorFactoryBean extends SpringValidatorAdapter
		implements ValidatorFactory, ApplicationContextAware, InitializingBean, DisposableBean {

	@SuppressWarnings("rawtypes")
	@Nullable
	private Class providerClass;

	@Nullable
	private ValidationProviderResolver validationProviderResolver;

	@Nullable
	private MessageInterpolator messageInterpolator;

	@Nullable
	private TraversableResolver traversableResolver;

	@Nullable
	private ConstraintValidatorFactory constraintValidatorFactory;

	@Nullable
	private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	@Nullable
	private Resource[] mappingLocations;

	private final Map<String, String> validationPropertyMap = new HashMap<>();

	@Nullable
	private Consumer<Configuration<?>> configurationInitializer;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ValidatorFactory validatorFactory;


	/**
	 * 指定所需的提供程序类（如果有）。
	 *
	 * <p>如果未指定，将使用 JSR-303 的默认查找机制。
	 * @see javax.validation.Validation#byProvider(Class)
	 * @see javax.validation.Validation#byDefaultProvider()
	 */
	@SuppressWarnings("rawtypes")
	public void setProviderClass(Class providerClass) {
		this.providerClass = providerClass;
	}

	/**
	 * 指定一个 JSR-303 {@link ValidationProviderResolver} 用于引导所选提供程序，作为由
	 * {@code META-INF} 驱动解析的替代方案。
	 * @since 4.3
	 */
	public void setValidationProviderResolver(ValidationProviderResolver validationProviderResolver) {
		this.validationProviderResolver = validationProviderResolver;
	}

	/**
	 * 指定要用于此 ValidatorFactory 及其公开的默认 Validator 的自定义 MessageInterpolator。
	 */
	public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
		this.messageInterpolator = messageInterpolator;
	}

	/**
	 * 指定用于解析校验消息的自定义 Spring MessageSource，而不是依赖类路径中 JSR-303 默认的
	 * "ValidationMessages.properties" 资源包。这可以引用 Spring 上下文中共享的 "messageSource" Bean，
	 * 或者引用仅为校验目的而设置的某个特殊 MessageSource。
	 *
	 * <p><b>注意：</b>此功能需要在类路径上使用 Hibernate Validator 4.3 或更高版本。你仍然可以使用其他
	 * 校验提供程序，但 Hibernate Validator 的 {@link ResourceBundleMessageInterpolator} 类必须在配置期
	 * 间可访问。
	 *
	 * <p>请指定此属性或 {@link #setMessageInterpolator "messageInterpolator"}，而不要同时指定两者。如
	 * 果要构建自定义 MessageInterpolator，可以考虑派生自 Hibernate Validator 的
	 * {@link ResourceBundleMessageInterpolator}，并在构造插值器时传入基于 Spring 的
	 * {@code ResourceBundleLocator}。
	 *
	 * <p>为了让 Hibernate 的默认校验消息仍能被解析，你的 {@link MessageSource} 必须配置为可选解析（通
	 * 常是默认行为）。特别是，此处指定的 {@code MessageSource} 实例不应应用
	 * {@link org.springframework.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage "useCodeAsDefaultMessage"}
	 * 行为。请相应地仔细检查你的设置。
	 * @see ResourceBundleMessageInterpolator
	 */
	public void setValidationMessageSource(MessageSource messageSource) {
		this.messageInterpolator = HibernateValidatorDelegate.buildMessageInterpolator(messageSource);
	}

	/**
	 * 指定要用于此 ValidatorFactory 及其公开的默认 Validator 的自定义 TraversableResolver。
	 */
	public void setTraversableResolver(TraversableResolver traversableResolver) {
		this.traversableResolver = traversableResolver;
	}

	/**
	 * 指定要用于此 ValidatorFactory 的自定义 ConstraintValidatorFactory。
	 *
	 * <p>默认为 {@link SpringConstraintValidatorFactory}，它委托包含的 ApplicationContext 来创建自动注
	 * 入的 ConstraintValidator 实例。
	 */
	public void setConstraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
		this.constraintValidatorFactory = constraintValidatorFactory;
	}

	/**
	 * 设置用于解析方法和构造函数参数名的 ParameterNameDiscoverer（如果消息插值需要）。
	 *
	 * <p>默认为 {@link org.springframework.core.DefaultParameterNameDiscoverer}。
	 */
	public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}

	/**
	 * 指定要从中加载 XML 约束映射文件的资源位置（如果有）。
	 */
	public void setMappingLocations(Resource... mappingLocations) {
		this.mappingLocations = mappingLocations;
	}

	/**
	 * 指定要传递给校验提供程序的 Bean Validation 属性。
	 *
	 * <p>可以用 String "value"（通过 PropertiesEditor 解析）或 XML Bean 定义中的 "props" 元素来填充。
	 * @see javax.validation.Configuration#addProperty(String, String)
	 */
	public void setValidationProperties(Properties jpaProperties) {
		CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.validationPropertyMap);
	}

	/**
	 * 以 Map 形式指定要传递给校验提供程序的 Bean Validation 属性。
	 *
	 * <p>可以用 XML Bean 定义中的 "map" 或 "props" 元素来填充。
	 * @see javax.validation.Configuration#addProperty(String, String)
	 */
	public void setValidationPropertyMap(@Nullable Map<String, String> validationProperties) {
		if (validationProperties != null) {
			this.validationPropertyMap.putAll(validationProperties);
		}
	}

	/**
	 * 允许以 Map 方式访问要传递给校验提供程序的 Bean Validation 属性，并可选择添加或覆盖特定条目。
	 *
	 * <p>适用于直接指定条目，例如通过 "validationPropertyMap[myKey]"。
	 */
	public Map<String, String> getValidationPropertyMap() {
		return this.validationPropertyMap;
	}

	/**
	 * 指定用于自定义 Bean Validation {@code Configuration} 实例的回调，作为在自定义
	 * {@code LocalValidatorFactoryBean} 子类中覆盖 {@link #postProcessConfiguration(Configuration)} 方
	 * 法的替代方案。
	 *
	 * <p>这使得针对应用程序目的的自定义变得方便。基础设施扩展仍可继续覆盖
	 * {@link #postProcessConfiguration} 模板方法。
	 * @since 5.3.19
	 */
	public void setConfigurationInitializer(Consumer<Configuration<?>> configurationInitializer) {
		this.configurationInitializer = configurationInitializer;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	public void afterPropertiesSet() {
		Configuration<?> configuration;
		if (this.providerClass != null) {
			ProviderSpecificBootstrap bootstrap = Validation.byProvider(this.providerClass);
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}
		else {
			GenericBootstrap bootstrap = Validation.byDefaultProvider();
			if (this.validationProviderResolver != null) {
				bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
			}
			configuration = bootstrap.configure();
		}

		// Try Hibernate Validator 5.2's externalClassLoader(ClassLoader) method
		if (this.applicationContext != null) {
			try {
				Method eclMethod = configuration.getClass().getMethod("externalClassLoader", ClassLoader.class);
				ReflectionUtils.invokeMethod(eclMethod, configuration, this.applicationContext.getClassLoader());
			}
			catch (NoSuchMethodException ex) {
				// Ignore - no Hibernate Validator 5.2+ or similar provider
			}
		}

		MessageInterpolator targetInterpolator = this.messageInterpolator;
		if (targetInterpolator == null) {
			targetInterpolator = configuration.getDefaultMessageInterpolator();
		}
		configuration.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));

		if (this.traversableResolver != null) {
			configuration.traversableResolver(this.traversableResolver);
		}

		ConstraintValidatorFactory targetConstraintValidatorFactory = this.constraintValidatorFactory;
		if (targetConstraintValidatorFactory == null && this.applicationContext != null) {
			targetConstraintValidatorFactory =
					new SpringConstraintValidatorFactory(this.applicationContext.getAutowireCapableBeanFactory());
		}
		if (targetConstraintValidatorFactory != null) {
			configuration.constraintValidatorFactory(targetConstraintValidatorFactory);
		}

		if (this.parameterNameDiscoverer != null) {
			configureParameterNameProvider(this.parameterNameDiscoverer, configuration);
		}

		List<InputStream> mappingStreams = null;
		if (this.mappingLocations != null) {
			mappingStreams = new ArrayList<>(this.mappingLocations.length);
			for (Resource location : this.mappingLocations) {
				try {
					InputStream stream = location.getInputStream();
					mappingStreams.add(stream);
					configuration.addMapping(stream);
				}
				catch (IOException ex) {
					closeMappingStreams(mappingStreams);
					throw new IllegalStateException("Cannot read mapping resource: " + location);
				}
			}
		}

		this.validationPropertyMap.forEach(configuration::addProperty);

		// Allow for custom post-processing before we actually build the ValidatorFactory.
		if (this.configurationInitializer != null) {
			this.configurationInitializer.accept(configuration);
		}
		postProcessConfiguration(configuration);

		try {
			this.validatorFactory = configuration.buildValidatorFactory();
			setTargetValidator(this.validatorFactory.getValidator());
		}
		finally {
			closeMappingStreams(mappingStreams);
		}
	}

	private void configureParameterNameProvider(ParameterNameDiscoverer discoverer, Configuration<?> configuration) {
		final ParameterNameProvider defaultProvider = configuration.getDefaultParameterNameProvider();
		configuration.parameterNameProvider(new ParameterNameProvider() {
			@Override
			public List<String> getParameterNames(Constructor<?> constructor) {
				String[] paramNames = discoverer.getParameterNames(constructor);
				return (paramNames != null ? Arrays.asList(paramNames) :
						defaultProvider.getParameterNames(constructor));
			}
			@Override
			public List<String> getParameterNames(Method method) {
				String[] paramNames = discoverer.getParameterNames(method);
				return (paramNames != null ? Arrays.asList(paramNames) :
						defaultProvider.getParameterNames(method));
			}
		});
	}

	private void closeMappingStreams(@Nullable List<InputStream> mappingStreams){
		if (!CollectionUtils.isEmpty(mappingStreams)) {
			for (InputStream stream : mappingStreams) {
				try {
					stream.close();
				}
				catch (IOException ignored) {
				}
			}
		}
	}

	/**
	 * 对给定的 Bean Validation 配置进行后处理，添加或覆盖其中的任何设置。
	 *
	 * <p>在构建 {@link ValidatorFactory} 之前立即调用。
	 * @param configuration Configuration 对象，已预先填充
	 * 由 LocalValidatorFactoryBean 的属性驱动的设置。
	 */
	protected void postProcessConfiguration(Configuration<?> configuration) {
	}


	@Override
	public Validator getValidator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getValidator();
	}

	@Override
	public ValidatorContext usingContext() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.usingContext();
	}

	@Override
	public MessageInterpolator getMessageInterpolator() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getMessageInterpolator();
	}

	@Override
	public TraversableResolver getTraversableResolver() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getTraversableResolver();
	}

	@Override
	public ConstraintValidatorFactory getConstraintValidatorFactory() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getConstraintValidatorFactory();
	}

	@Override
	public ParameterNameProvider getParameterNameProvider() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getParameterNameProvider();
	}

	// Bean Validation 2.0: currently not implemented here since it would imply
	// a hard dependency on the new javax.validation.ClockProvider interface.
	// To be resolved once Spring Framework requires Bean Validation 2.0+.
	// Obtain the native ValidatorFactory through unwrap(ValidatorFactory.class)
	// instead which will fully support a getClockProvider() call as well.
	/*
	@Override
	public javax.validation.ClockProvider getClockProvider() {
		Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
		return this.validatorFactory.getClockProvider();
	}
	*/

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(@Nullable Class<T> type) {
		if (type == null || !ValidatorFactory.class.isAssignableFrom(type)) {
			try {
				return super.unwrap(type);
			}
			catch (ValidationException ex) {
				// Ignore - we'll try ValidatorFactory unwrapping next
			}
		}
		if (this.validatorFactory != null) {
			try {
				return this.validatorFactory.unwrap(type);
			}
			catch (ValidationException ex) {
				// Ignore if just being asked for ValidatorFactory
				if (ValidatorFactory.class == type) {
					return (T) this.validatorFactory;
				}
				throw ex;
			}
		}
		throw new ValidationException("Cannot unwrap to " + type);
	}

	@Override
	public void close() {
		if (this.validatorFactory != null) {
			this.validatorFactory.close();
		}
	}

	@Override
	public void destroy() {
		close();
	}


	/**
	 * 为避免硬编码 Hibernate Validator 依赖而使用的内部类。
	 */
	private static class HibernateValidatorDelegate {

		public static MessageInterpolator buildMessageInterpolator(MessageSource messageSource) {
			return new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource));
		}
	}

}
