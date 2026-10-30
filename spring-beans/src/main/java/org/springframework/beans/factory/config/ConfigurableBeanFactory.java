/*
 * Copyright 2002-2020 the original author or authors.
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

import java.beans.PropertyEditor;
import java.security.AccessControlContext;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

/**
 * 供大多数 Bean 工厂实现的配置接口。除 {@link org.springframework.beans.factory.BeanFactory} 接口
 * 中的 Bean 工厂客户端方法外，还提供配置 Bean 工厂的功能。
 *
 * <p>此 Bean 工厂接口并不打算在普通的应用代码中使用：典型需求请坚持使用
 * {@link org.springframework.beans.factory.BeanFactory} 或
 * {@link org.springframework.beans.factory.ListableBeanFactory}。这个扩展接口只是为了支持框架内部
 * 的即插即用，以及对 Bean 工厂配置方法的特殊访问。
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.beans.factory.ListableBeanFactory
 * @see ConfigurableListableBeanFactory
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

	/**
	 * 标准 singleton 作用域的作用域标识符：{@value}。
	 * <p>可以通过 {@code registerScope} 添加自定义作用域。
	 * @see #registerScope
	 */
	String SCOPE_SINGLETON = "singleton";

	/**
	 * 标准 prototype 作用域的作用域标识符：{@value}。
	 * <p>可以通过 {@code registerScope} 添加自定义作用域。
	 * @see #registerScope
	 */
	String SCOPE_PROTOTYPE = "prototype";


	/**
	 * 设置此 Bean 工厂的父工厂。
	 * <p>注意，父工厂不可更改：如果工厂实例化时父工厂尚不可用，则只应在构造函数之外设置它。
	 * @param parentBeanFactory 父 BeanFactory
	 * @throws IllegalStateException 如果此工厂已关联了父 BeanFactory 则抛出
	 * @see #getParentBeanFactory()
	 */
	void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

	/**
	 * 设置用于加载 Bean 类的类加载器。默认是线程上下文类加载器。
	 * <p>注意，此类加载器仅适用于尚未携带已解析 Bean 类的 Bean 定义。从 Spring 2.0 起默认就是这种情况：
	 * Bean 定义只携带 Bean 类名，等到工厂处理该 Bean 定义时再解析。
	 * @param beanClassLoader 要使用的类加载器，
	 * 或 {@code null} 表示建议使用默认类加载器
	 */
	void setBeanClassLoader(@Nullable ClassLoader beanClassLoader);

	/**
	 * 返回此工厂用于加载 Bean 类的类加载器（仅当连系统 ClassLoader 也无法访问时才为 {@code null}）。
	 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * 指定一个临时 ClassLoader 用于类型匹配。默认没有，即直接使用标准的 Bean ClassLoader。
	 * <p>通常仅在涉及<i>加载时织入</i>时才指定临时 ClassLoader，以确保实际 Bean 类尽可能延迟加载。
	 * BeanFactory 完成其引导阶段后，该临时加载器即被移除。
	 * @since 2.5
	 */
	void setTempClassLoader(@Nullable ClassLoader tempClassLoader);

	/**
	 * 返回用于类型匹配的临时 ClassLoader（如果有）。
	 * @since 2.5
	 */
	@Nullable
	ClassLoader getTempClassLoader();

	/**
	 * 设置是否缓存 Bean 元数据，例如给定的 Bean 定义（以合并形式）和已解析的 Bean 类。默认开启。
	 * <p>关闭此标志可启用 Bean 定义对象特别是 Bean 类的热刷新。如果此标志关闭，每次创建 Bean 实例都会
	 * 向 Bean 类加载器重新查询新解析的类。
	 */
	void setCacheBeanMetadata(boolean cacheBeanMetadata);

	/**
	 * 返回是否缓存 Bean 元数据，例如给定的 Bean 定义（以合并形式）和已解析的 Bean 类。
	 */
	boolean isCacheBeanMetadata();

	/**
	 * 指定 Bean 定义值中表达式的解析策略。
	 * <p>默认情况下，BeanFactory 中不会激活任何表达式支持。ApplicationContext 通常会在此设置标准表达式
	 * 策略，支持 Unified EL 兼容风格的 "#{...}" 表达式。
	 * @since 3.0
	 */
	void setBeanExpressionResolver(@Nullable BeanExpressionResolver resolver);

	/**
	 * 返回 Bean 定义值中表达式的解析策略。
	 * @since 3.0
	 */
	@Nullable
	BeanExpressionResolver getBeanExpressionResolver();

	/**
	 * 指定一个 Spring 3.0 的 ConversionService 用于转换属性值，作为 JavaBeans PropertyEditors 的替代方
	 * 案。
	 * @since 3.0
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * 返回关联的 ConversionService（如果有）。
	 * @since 3.0
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * 添加一个 PropertyEditorRegistrar，应用于所有 Bean 创建过程。
	 * <p>此类注册器会创建新的 PropertyEditor 实例，并在每次 Bean 创建尝试时将它们注册到给定的注册表上。
	 * 这避免了对自定义编辑器进行同步的需要；因此，通常优先使用此方法而不是
	 * {@link #registerCustomEditor}。
	 * @param registrar 要注册的 PropertyEditorRegistrar
	 */
	void addPropertyEditorRegistrar(PropertyEditorRegistrar registrar);

	/**
	 * 为给定类型的所有属性注册给定的自定义属性编辑器。将在工厂配置期间调用。
	 * <p>注意，此方法会注册一个共享的自定义编辑器实例；对该实例的访问将进行同步以保证线程安全。通常更
	 * 推荐使用 {@link #addPropertyEditorRegistrar} 而不是此方法，以避免对自定义编辑器进行同步的需要。
	 * @param requiredType 属性的类型
	 * @param propertyEditorClass 要注册的 {@link PropertyEditor} 类
	 */
	void registerCustomEditor(Class<?> requiredType, Class<? extends PropertyEditor> propertyEditorClass);

	/**
	 * 用已注册到此 BeanFactory 的自定义编辑器初始化给定的 PropertyEditorRegistry。
	 * @param registry 要初始化的 PropertyEditorRegistry
	 */
	void copyRegisteredEditorsTo(PropertyEditorRegistry registry);

	/**
	 * 设置此 BeanFactory 应用于转换 Bean 属性值、构造函数参数值等的自定义类型转换器。
	 * <p>这会覆盖默认的 PropertyEditor 机制，从而使任何自定义编辑器或自定义编辑器注册器都变得无关紧要。
	 *
	 * @since 2.5
	 * @see #addPropertyEditorRegistrar
	 * @see #registerCustomEditor
	 */
	void setTypeConverter(TypeConverter typeConverter);

	/**
	 * 获取此 BeanFactory 所使用的类型转换器。由于 TypeConverter 通常<i>不是</i>线程安全的，每次调用都
	 * 可能返回一个新实例。
	 * <p>如果默认的 PropertyEditor 机制处于激活状态，返回的 TypeConverter 将能识别所有已注册的自定义编
	 * 辑器。
	 * @since 2.5
	 */
	TypeConverter getTypeConverter();

	/**
	 * 为嵌入值（例如注解属性）添加 String 解析器。
	 * @param valueResolver 要应用于内嵌值的 String 解析器
	 * @since 3.0
	 */
	void addEmbeddedValueResolver(StringValueResolver valueResolver);

	/**
	 * 确定是否已向此 Bean 工厂注册嵌入值解析器，以便通过 {@link #resolveEmbeddedValue(String)} 应用。
	 * @since 4.3
	 */
	boolean hasEmbeddedValueResolver();

	/**
	 * 解析给定的嵌入值，例如注解属性。
	 * @param value 要解析的值
	 * @return 解析后的值（可能为原始值本身）
	 * @since 3.0
	 */
	@Nullable
	String resolveEmbeddedValue(String value);

	/**
	 * 添加一个新的 BeanPostProcessor，它将应用于此工厂创建的 Bean。将在工厂配置期间调用。
	 * <p>注意：此处提交的后处理器将按注册顺序应用；通过实现 {@link org.springframework.core.Ordered}
	 * 接口所表达的任何排序语义都将被忽略。注意，自动检测到的后处理器（例如作为 ApplicationContext 中的
	 * Bean）将始终在以编程方式注册的后处理器之后应用。
	 * @param beanPostProcessor 要注册的后处理器
	 */
	void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

	/**
	 * 返回当前已注册的 BeanPostProcessor 数量（如果有）。
	 */
	int getBeanPostProcessorCount();

	/**
	 * 注册给定的作用域，由给定的 Scope 实现提供支持。
	 * @param scopeName 作用域标识符
	 * @param scope 底层 Scope 实现
	 */
	void registerScope(String scopeName, Scope scope);

	/**
	 * 返回当前所有已注册作用域的名称。
	 * <p>这只会返回显式注册的作用域的名称。诸如 "singleton" 和 "prototype" 之类的内置作用域不会暴露。
	 * @return 作用域名称数组；如果没有则为空数组
	 * @see #registerScope
	 */
	String[] getRegisteredScopeNames();

	/**
	 * 返回给定作用域名称对应的 Scope 实现（如果有）。
	 * <p>这只会返回显式注册的作用域。诸如 "singleton" 和 "prototype" 之类的内置作用域不会暴露。
	 * @param scopeName 作用域的名称
	 * @return 已注册的 Scope 实现；如果没有则为 {@code null}
	 * @see #registerScope
	 */
	@Nullable
	Scope getRegisteredScope(String scopeName);

	/**
	 * 为此 Bean 工厂设置 {@code ApplicationStartup}。
	 * <p>这允许应用上下文在应用启动期间记录指标。
	 * @param applicationStartup 新的应用启动
	 * @since 5.3
	 */
	void setApplicationStartup(ApplicationStartup applicationStartup);

	/**
	 * 返回此 Bean 工厂的 {@code ApplicationStartup}。
	 * @since 5.3
	 */
	ApplicationStartup getApplicationStartup();

	/**
	 * 提供与此工厂相关的安全访问控制上下文。
	 * @return 适用的 AccessControlContext（永不为 {@code null}）
	 * @since 3.0
	 */
	AccessControlContext getAccessControlContext();

	/**
	 * 复制给定其他工厂的所有相关配置。
	 * <p>应包含所有标准配置设置以及 BeanPostProcessors、Scopes 和工厂特定的内部设置。不应包含实际 Bean
	 * 定义的任何元数据，例如 BeanDefinition 对象和 Bean 名称别名。
	 * @param otherFactory 要从中复制的另一个 BeanFactory
	 */
	void copyConfigurationFrom(ConfigurableBeanFactory otherFactory);

	/**
	 * 给定一个 Bean 名称，创建一个别名。我们通常使用此方法来支持在 XML id（用于 Bean 名称）中不合法的
	 * 名称。
	 * <p>通常在工厂配置期间调用，但也可用于在运行时注册别名。因此，工厂实现应同步别名的访问。
	 * @param beanName 目标 Bean 的规范名称
	 * @param alias 要为该 Bean 注册的别名
	 * @throws BeanDefinitionStoreException 如果别名已被使用则抛出
	 */
	void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException;

	/**
	 * 解析此工厂中注册的所有别名目标名称和别名，对它们应用给定的 StringValueResolver。
	 * <p>例如，值解析器可以解析目标 Bean 名称乃至别名中的占位符。
	 * @param valueResolver 要应用的 StringValueResolver
	 * @since 2.5
	 */
	void resolveAliases(StringValueResolver valueResolver);

	/**
	 * 返回给定 Bean 名称的合并 BeanDefinition，如有必要，将子 Bean 定义与其父定义合并。也会考虑祖先工
	 * 厂中的 Bean 定义。
	 * @param beanName 要获取合并定义的目标 Bean 的名称
	 * @return 给定 Bean 的（可能已合并的）BeanDefinition
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 Bean 定义则抛出
	 * @since 2.5
	 */
	BeanDefinition getMergedBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

	/**
	 * 确定给定名称的 Bean 是否为 FactoryBean。
	 * @param name 要检查的 Bean 的名称
	 * @return 该 Bean 是否为 FactoryBean
	 * （{@code false} 表示该 Bean 存在但不是 FactoryBean）
	 * @throws NoSuchBeanDefinitionException 如果没有给定名称的 Bean 则抛出
	 * @since 2.5
	 */
	boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException;

	/**
	 * 显式控制指定 Bean 当前的创建中状态。仅供容器内部使用。
	 * @param beanName 该 Bean 的名称
	 * @param inCreation 该 Bean 当前是否正在创建
	 * @since 3.1
	 */
	void setCurrentlyInCreation(String beanName, boolean inCreation);

	/**
	 * 确定指定 Bean 当前是否正在创建中。
	 * @param beanName 该 Bean 的名称
	 * @return 该 Bean 当前是否正在创建
	 * @since 2.5
	 */
	boolean isCurrentlyInCreation(String beanName);

	/**
	 * 为给定 Bean 注册一个依赖 Bean，使其在给定 Bean 被销毁之前销毁。
	 * @param beanName 该 Bean 的名称
	 * @param dependentBeanName 依赖 Bean 的名称
	 * @since 2.5
	 */
	void registerDependentBean(String beanName, String dependentBeanName);

	/**
	 * 返回依赖指定 Bean 的所有 Bean 的名称（如果有）。
	 * @param beanName 该 Bean 的名称
	 * @return 依赖 Bean 名称数组；如果没有则为空数组
	 * @since 2.5
	 */
	String[] getDependentBeans(String beanName);

	/**
	 * 返回指定 Bean 所依赖的所有 Bean 的名称（如果有）。
	 * @param beanName 该 Bean 的名称
	 * @return 该 Bean 所依赖的 Bean 名称数组，
	 * 如果没有则为空数组
	 * @since 2.5
	 */
	String[] getDependenciesForBean(String beanName);

	/**
	 * 根据其 Bean 定义销毁给定的 Bean 实例（通常是从此工厂获取的 prototype 实例）。
	 * <p>销毁期间出现的任何异常都应被捕获并记录，而不是传播给此方法的调用方。
	 * @param beanName Bean 定义的名称
	 * @param beanInstance 要销毁的 Bean 实例
	 */
	void destroyBean(String beanName, Object beanInstance);

	/**
	 * 销毁当前目标作用域中的指定作用域 Bean（如果有）。
	 * <p>销毁期间出现的任何异常都应被捕获并记录，而不是传播给此方法的调用方。
	 * @param beanName 作用域 Bean 的名称
	 */
	void destroyScopedBean(String beanName);

	/**
	 * 销毁此工厂中的所有 singleton Bean，包括已注册为可销毁的内部 Bean。在工厂关闭时调用。
	 * <p>销毁期间出现的任何异常都应被捕获并记录，而不是传播给此方法的调用方。
	 */
	void destroySingletons();

}
