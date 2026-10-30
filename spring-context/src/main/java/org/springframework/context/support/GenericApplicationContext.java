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
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 通用的 ApplicationContext 实现，持有单个内部
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} 实例，并且不假定特
 * 定的 Bean 定义格式。它实现
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 接口，以便可以向其应用
 * 任何 Bean 定义读取器。
 *
 * <p>典型用法是通过 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 接口
 * 注册各种 Bean 定义，然后调用 {@link #refresh()} 以应用上下文语义初始化这些 Bean（处理
 * {@link org.springframework.context.ApplicationContextAware}、自动检测
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors}
 * 等）。
 *
 * <p>与其他每次 refresh 都创建新的内部 BeanFactory 实例的 ApplicationContext 实现不同，此上下文的
 * 内部 BeanFactory 从一开始就可用，从而可以在其上注册 Bean 定义。{@link #refresh()} 只能调用一次。
 *
 * <p>用法示例：
 *
 * <pre class="code">
 * GenericApplicationContext ctx = new GenericApplicationContext();
 * XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(ctx);
 * xmlReader.loadBeanDefinitions(new ClassPathResource("applicationContext.xml"));
 * PropertiesBeanDefinitionReader propReader = new PropertiesBeanDefinitionReader(ctx);
 * propReader.loadBeanDefinitions(new ClassPathResource("otherBeans.properties"));
 * ctx.refresh();
 *
 * MyBean myBean = (MyBean) ctx.getBean("myBean");
 * ...</pre>
 *
 * 对于典型的 XML Bean 定义场景，只需使用 {@link ClassPathXmlApplicationContext} 或
 * {@link FileSystemXmlApplicationContext}，它们更易于设置，但灵活性较低，因为只能将标准资源位置用
 * 于 XML Bean 定义，而不能混合任意的 Bean 定义格式。Web 环境中的等效项是
 * {@link org.springframework.web.context.support.XmlWebApplicationContext}。
 *
 * <p>对于需要以可刷新方式读取特殊 Bean 定义格式的自定义应用上下文实现，可考虑从
 * {@link AbstractRefreshableApplicationContext} 基类派生。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 1.1.2
 * @see #registerBeanDefinition
 * @see #refresh()
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
 */
public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

	private final DefaultListableBeanFactory beanFactory;

	@Nullable
	private ResourceLoader resourceLoader;

	private boolean customClassLoader = false;

	private final AtomicBoolean refreshed = new AtomicBoolean();


	/**
	 * 创建一个新的 GenericApplicationContext。
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	/*
	* spring容器初始化时会自动调用该父类的构造，初始化bean工厂
	* */
	public GenericApplicationContext() {
		this.beanFactory = new DefaultListableBeanFactory();
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 创建一个新的 GenericApplicationContext。
	 * @param beanFactory 要用于此上下文的 DefaultListableBeanFactory 实例
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * 使用给定的父上下文创建一个新的 GenericApplicationContext。
	 * @param parent 父应用上下文
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 创建一个新的 GenericApplicationContext。
	 * @param beanFactory 要用于此上下文的 DefaultListableBeanFactory 实例
	 * @param parent 父应用上下文
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericApplicationContext(DefaultListableBeanFactory beanFactory, ApplicationContext parent) {
		this(beanFactory);
		setParent(parent);
	}


	/**
	 * 设置此应用上下文的父级，同时相应地设置内部 BeanFactory 的父级。
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#setParentBeanFactory
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		super.setParent(parent);
		this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		super.setApplicationStartup(applicationStartup);
		this.beanFactory.setApplicationStartup(applicationStartup);
	}

	/**
	 * 设置是否允许通过注册同名但不同的定义来覆盖 Bean 定义，并自动替换前一个定义。如果不允许，将抛出异
	 * 常。默认值为 "true"。
	 * @since 3.0
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.beanFactory.setAllowBeanDefinitionOverriding(allowBeanDefinitionOverriding);
	}

	/**
	 * 设置是否允许 Bean 之间的循环引用，并自动尝试解析它们。<p>默认值为 "true"。关闭此选项后，遇到循环
	 * 引用时将抛出异常，从而完全禁止循环引用。
	 * @since 3.0
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.beanFactory.setAllowCircularReferences(allowCircularReferences);
	}

	/**
	 * 设置要用于此上下文的 ResourceLoader。如果已设置，上下文会将所有 {@code getResource} 调用委托给给
	 * 定的 ResourceLoader。如果未设置，则应用默认的资源加载方式。<p>指定自定义 ResourceLoader 的主要目
	 * 的是以特定方式解析资源路径（不带 URL 前缀）。默认行为是将此类路径解析为类路径位置。要将资源路径
	 * 解析为文件系统位置，请在此处指定 FileSystemResourceLoader。<p>也可以传入完整的
	 * ResourcePatternResolver，上下文会自动检测它，并将其用于 {@code getResources} 调用。否则，将应用
	 * 默认的资源模式匹配。
	 * @see #getResource
	 * @see org.springframework.core.io.DefaultResourceLoader
	 * @see org.springframework.core.io.FileSystemResourceLoader
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see #getResources
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	//---------------------------------------------------------------------
	// ResourceLoader / ResourcePatternResolver override if necessary
	//---------------------------------------------------------------------

	/**
	 * 如果已设置，此实现会委托给此上下文的 {@code ResourceLoader}，否则回退到超类的默认行为。<p>从
	 * Spring Framework 5.3.22 起，当设置了自定义 {@code ResourceLoader} 时，此方法还会遵循已注册的
	 * {@linkplain #getProtocolResolvers() protocol resolvers}。
	 * @see #setResourceLoader(ResourceLoader)
	 * @see #addProtocolResolver(ProtocolResolver)
	 */
	@Override
	public Resource getResource(String location) {
		if (this.resourceLoader != null) {
			for (ProtocolResolver protocolResolver : getProtocolResolvers()) {
				Resource resource = protocolResolver.resolve(location, this);
				if (resource != null) {
					return resource;
				}
			}
			return this.resourceLoader.getResource(location);
		}
		return super.getResource(location);
	}

	/**
	 * 如果此上下文的 ResourceLoader 实现了 ResourcePatternResolver 接口，则此实现委托给它，否则回退到
	 * 超类的默认行为。
	 * @see #setResourceLoader
	 */
	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		if (this.resourceLoader instanceof ResourcePatternResolver) {
			return ((ResourcePatternResolver) this.resourceLoader).getResources(locationPattern);
		}
		return super.getResources(locationPattern);
	}

	@Override
	public void setClassLoader(@Nullable ClassLoader classLoader) {
		super.setClassLoader(classLoader);
		this.customClassLoader = true;
	}

	@Override
	@Nullable
	public ClassLoader getClassLoader() {
		if (this.resourceLoader != null && !this.customClassLoader) {
			return this.resourceLoader.getClassLoader();
		}
		return super.getClassLoader();
	}


	//---------------------------------------------------------------------
	// Implementations of AbstractApplicationContext's template methods
	//---------------------------------------------------------------------

	/**
	 * 不执行任何操作：我们持有单个内部 BeanFactory，并依赖调用方通过我们的公共方法（或 BeanFactory 的
	 * 公共方法）注册 Bean。
	 * @see #registerBeanDefinition
	 */
	@Override
	protected final void refreshBeanFactory() throws IllegalStateException {
		if (!this.refreshed.compareAndSet(false, true)) {
			throw new IllegalStateException(
					"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once");
		}
		this.beanFactory.setSerializationId(getId());
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		this.beanFactory.setSerializationId(null);
		super.cancelRefresh(ex);
	}

	/**
	 * 无需太多处理：我们持有单个内部 BeanFactory，它永远不会被释放。
	 */
	@Override
	protected final void closeBeanFactory() {
		this.beanFactory.setSerializationId(null);
	}

	/**
	 * 返回此上下文持有的单个内部 BeanFactory（作为 ConfigurableListableBeanFactory）。
	 */
	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 返回此上下文的底层 Bean 工厂，可用于注册 Bean 定义。<p><b>注意：</b>需要调用 {@link #refresh()}，
	 * 以应用上下文语义初始化该 Bean 工厂及其包含的 Bean（自动检测 BeanFactoryPostProcessors 等）。
	 * @return 内部 Bean 工厂（作为 DefaultListableBeanFactory）
	 */
	public final DefaultListableBeanFactory getDefaultListableBeanFactory() {
		return this.beanFactory;
	}

	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		assertBeanFactoryActive();
		return this.beanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry
	//---------------------------------------------------------------------

	@Override
	public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {

		this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
	}

	@Override
	public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		this.beanFactory.removeBeanDefinition(beanName);
	}

	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
		return this.beanFactory.getBeanDefinition(beanName);
	}

	@Override
	public boolean isBeanNameInUse(String beanName) {
		return this.beanFactory.isBeanNameInUse(beanName);
	}

	@Override
	public void registerAlias(String beanName, String alias) {
		this.beanFactory.registerAlias(beanName, alias);
	}

	@Override
	public void removeAlias(String alias) {
		this.beanFactory.removeAlias(alias);
	}

	@Override
	public boolean isAlias(String beanName) {
		return this.beanFactory.isAlias(beanName);
	}


	//---------------------------------------------------------------------
	// Convenient methods for registering individual beans
	//---------------------------------------------------------------------

	/**
	 * 从给定的 Bean 类注册一个 Bean，可选地提供显式构造函数参数，供自动装配过程考虑。
	 * @param beanClass Bean 的类
	 * @param constructorArgs 提供给 Spring 构造器解析算法的自定义参数值，可解析所有参数或仅特定参数，
	 * 其余参数通过常规自动装配解析（可以为 {@code null} 或为空）
	 * @since 5.2 (since 5.0 on the AnnotationConfigApplicationContext subclass)
	 */
	public <T> void registerBean(Class<T> beanClass, Object... constructorArgs) {
		registerBean(null, beanClass, constructorArgs);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，可选地提供显式构造函数参数，供自动装配过程考虑。
	 * @param beanName Bean 的名称（可以为 {@code null}）
	 * @param beanClass Bean 的类
	 * @param constructorArgs 提供给 Spring 构造器解析算法的自定义参数值，可解析所有参数或仅特定参数，
	 * 其余参数通过常规自动装配解析（可以为 {@code null} 或为空）
	 * @since 5.2 (since 5.0 on the AnnotationConfigApplicationContext subclass)
	 */
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass, Object... constructorArgs) {
		registerBean(beanName, beanClass, (Supplier<T>) null,
				bd -> {
					for (Object arg : constructorArgs) {
						bd.getConstructorArgumentValues().addGenericArgumentValue(arg);
					}
				});
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，可选地自定义其 Bean 定义元数据（通常声明为 lambda 表达式）。
	 * @param beanClass Bean 的类（解析要自动装配的公共构造器，可能只是默认构造器）
	 * @param customizers 用于自定义工厂 {@link BeanDefinition} 的一个或多个回调，
	 * 例如设置 lazy-init 或 primary 标志
	 * @since 5.0
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 */
	public final <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
		registerBean(null, beanClass, null, customizers);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，可选地自定义其 Bean 定义元数据（通常声明为 lambda 表达式）。
	 * @param beanName Bean 的名称（可以为 {@code null}）
	 * @param beanClass Bean 的类（解析要自动装配的公共构造器，可能只是默认构造器）
	 * @param customizers 用于自定义工厂 {@link BeanDefinition} 的一个或多个回调，
	 * 例如设置 lazy-init 或 primary 标志
	 * @since 5.0
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 */
	public final <T> void registerBean(
			@Nullable String beanName, Class<T> beanClass, BeanDefinitionCustomizer... customizers) {

		registerBean(beanName, beanClass, null, customizers);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，使用给定的 supplier 获取新实例（通常声明为 lambda 表达式或方法引
	 * 用），并可选择自定义其 Bean 定义元数据（同样通常声明为 lambda 表达式）。
	 * @param beanClass Bean 的类
	 * @param supplier 用于创建 Bean 实例的回调
	 * @param customizers 用于自定义工厂 {@link BeanDefinition} 的一个或多个回调，
	 * 例如设置 lazy-init 或 primary 标志
	 * @since 5.0
	 * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
	 */
	public final <T> void registerBean(
			Class<T> beanClass, Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		registerBean(null, beanClass, supplier, customizers);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，使用给定的 supplier 获取新实例（通常声明为 lambda 表达式或方法引
	 * 用），并可选择自定义其 Bean 定义元数据（同样通常声明为 lambda 表达式）。<p>此方法可以覆盖，以调
	 * 整所有 {@code registerBean} 方法的注册机制（因为它们都委托给此方法）。
	 * @param beanName Bean 的名称（可以为 {@code null}）
	 * @param beanClass Bean 的类
	 * @param supplier 用于创建 Bean 实例的回调（如果为 {@code null}，则改为解析要自动装配的公共构造器）
	 * @param customizers 用于自定义工厂 {@link BeanDefinition} 的一个或多个回调，
	 * 例如设置 lazy-init 或 primary 标志
	 * @since 5.0
	 */
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		ClassDerivedBeanDefinition beanDefinition = new ClassDerivedBeanDefinition(beanClass);
		if (supplier != null) {
			beanDefinition.setInstanceSupplier(supplier);
		}
		for (BeanDefinitionCustomizer customizer : customizers) {
			customizer.customize(beanDefinition);
		}

		String nameToUse = (beanName != null ? beanName : beanClass.getName());
		registerBeanDefinition(nameToUse, beanDefinition);
	}


	/**
	 * {@link RootBeanDefinition} 的标记子类，用于基于 {@code #registerBean} 的注册，并对公共构造函数进
	 * 行灵活的自动装配。
	 */
	@SuppressWarnings("serial")
	private static class ClassDerivedBeanDefinition extends RootBeanDefinition {

		public ClassDerivedBeanDefinition(Class<?> beanClass) {
			super(beanClass);
		}

		public ClassDerivedBeanDefinition(ClassDerivedBeanDefinition original) {
			super(original);
		}

		@Override
		@Nullable
		public Constructor<?>[] getPreferredConstructors() {
			Class<?> clazz = getBeanClass();
			Constructor<?> primaryCtor = BeanUtils.findPrimaryConstructor(clazz);
			if (primaryCtor != null) {
				return new Constructor<?>[] {primaryCtor};
			}
			Constructor<?>[] publicCtors = clazz.getConstructors();
			if (publicCtors.length > 0) {
				return publicCtors;
			}
			return null;
		}

		@Override
		public RootBeanDefinition cloneBeanDefinition() {
			return new ClassDerivedBeanDefinition(this);
		}
	}

}
