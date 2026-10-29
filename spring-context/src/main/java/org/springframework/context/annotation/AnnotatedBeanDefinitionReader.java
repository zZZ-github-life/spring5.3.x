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

package org.springframework.context.annotation;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于以编程方式注册 Bean 类的便捷适配器。
 *
 * <p>这是 {@link ClassPathBeanDefinitionScanner} 的替代方案，应用相同的注解解析，但仅适用于显式注
 * 册的类。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.0
 * @see AnnotationConfigApplicationContext#register
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private ConditionEvaluator conditionEvaluator;


	/**
	 * 为给定注册表创建一个新的 {@code AnnotatedBeanDefinitionReader}。
	 *
	 * <p>如果注册表是 {@link EnvironmentCapable}，例如是一个 {@code ApplicationContext}，则会继承其
	 * {@link Environment}，否则将创建并使用一个新的 {@link StandardEnvironment}。
	 * @param registry the {@code BeanFactory} to load bean definitions into,
	 * in the form of a {@code BeanDefinitionRegistry}
	 * @see #AnnotatedBeanDefinitionReader(BeanDefinitionRegistry, Environment)
	 * @see #setEnvironment(Environment)
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		this(registry, getOrCreateEnvironment(registry));
	}

	/**
	 * 创建AnnotatedBeanDefinitionReader 。
		 * 1.赋值BeanDefinitionRegistry
		 * 2.创建ConditionEvaluator，记录相关信息
		 * 3.注册bean的后置处理器
	 * @param registry  实现类 AnnotationConfigApplicationContext
	 * @param environment  实现类 StandardEnvironment
	 */
	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.registry = registry;

		/*创建ConditionEvaluator，同时创建静态内部类对象ConditionContextImpl给context赋值
			这个类的作用用于根据条件判断某个bean是否应该被注入到spring容器当中
		*
		* 主要作用记录了spring容器的重要信息为了后面使用：
		* 	记录bean定义的注册器
		* 	记录bean工厂是谁
		* 	记录spring的环境
		* 	记录当前的资源加载器
		* 	记录类加载器
		* */
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
		/*
		* 给bean工厂注册各种后置处理器。
		*	1.注册了依赖排序器
		* 	2.注册Autowire候选处理器
		* 	3.注册配置类后置处理器（spring的bean扫描，解析就是这个处理器完成的）
		* 	4.注册@Autowire注解的后置处理器，处理@Autowire注解
		* 	5.注册JSR规范的注解处理器 (@Resource,@PostConstruct,@PreDestroy等注解都是通过此处理器完成)
		*  	6.注册jpa后置处理器
		* 	7.注册监听方法的事件监听处理器（查找bean工厂中带有@EventListeners注解的bean的方法放入事件监听器的集合）
		* 	8.注册事件监听器工厂
		* */
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}


	/**
	 * 获取此读取器所操作的 BeanDefinitionRegistry。
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * 设置用于评估是否应注册 {@link Conditional @Conditional} 注解的组件类的 {@code Environment}。
	 *
	 * <p>默认为 {@link StandardEnvironment}。
	 * @see #registerBean(Class, String, Class...)
	 */
	public void setEnvironment(Environment environment) {
		this.conditionEvaluator = new ConditionEvaluator(this.registry, environment, null);
	}

	/**
	 * 设置用于检测到的 Bean 类的 {@code BeanNameGenerator}。
	 *
	 * <p>默认为 {@link AnnotationBeanNameGenerator}。
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator =
				(beanNameGenerator != null ? beanNameGenerator : AnnotationBeanNameGenerator.INSTANCE);
	}

	/**
	 * 设置用于已注册组件类的 {@code ScopeMetadataResolver}。
	 *
	 * <p>默认为 {@link AnnotationScopeMetadataResolver}。
	 */
	public void setScopeMetadataResolver(@Nullable ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver =
				(scopeMetadataResolver != null ? scopeMetadataResolver : new AnnotationScopeMetadataResolver());
	}


	/**
	 * 注册一个或多个要处理的组件类。
	 *
	 * <p>对 {@code register} 的调用是幂等的；多次添加相同的组件类不会产生额外效果。
	 * @param componentClasses one or more component classes,
	 * e.g. {@link Configuration @Configuration} classes
	 */
	public void register(Class<?>... componentClasses) {
		for (Class<?> componentClass : componentClasses) {
			registerBean(componentClass);
		}
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据。
	 * @param beanClass the class of the bean
	 */
	public void registerBean(Class<?> beanClass) {
		doRegisterBean(beanClass, null, null, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据。
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @since 5.2
	 */
	public void registerBean(Class<?> beanClass, @Nullable String name) {
		doRegisterBean(beanClass, name, null, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据。
	 * @param beanClass the class of the bean
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, Class<? extends Annotation>... qualifiers) {
		doRegisterBean(beanClass, null, qualifiers, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据。
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param qualifiers specific qualifier annotations to consider,
	 * in addition to qualifiers at the bean class level
	 */
	@SuppressWarnings("unchecked")
	public void registerBean(Class<?> beanClass, @Nullable String name,
			Class<? extends Annotation>... qualifiers) {

		doRegisterBean(beanClass, name, qualifiers, null, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据，使用给定的 supplier 获取新实例
	 * （可以声明为 lambda 表达式或方法引用）。
	 * @param beanClass the class of the bean
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, null, null, supplier, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据，使用给定的 supplier 获取新实例
	 * （可以声明为 lambda 表达式或方法引用）。
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @since 5.0
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier) {
		doRegisterBean(beanClass, name, null, supplier, null);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据。
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * (or {@code null} for generating a default bean name)
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.2
	 */
	public <T> void registerBean(Class<T> beanClass, @Nullable String name, @Nullable Supplier<T> supplier,
			BeanDefinitionCustomizer... customizers) {

		doRegisterBean(beanClass, name, null, supplier, customizers);
	}

	/**
	 * 从给定的 Bean 类注册一个 Bean，并从类声明的注解中派生其元数据。
	 * @param beanClass the class of the bean
	 * @param name an explicit name for the bean
	 * @param qualifiers specific qualifier annotations to consider, if any,
	 * in addition to qualifiers at the bean class level
	 * @param supplier a callback for creating an instance of the bean
	 * (may be {@code null})
	 * @param customizers one or more callbacks for customizing the factory's
	 * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
	 * @since 5.0
	 */
	private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
			@Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
			@Nullable BeanDefinitionCustomizer[] customizers) {

		/*创建AnnotatedGenericBeanDefinition实例。目的是将系统中扫描的实例转化为spring的bean（即BeanDefinition） 。

			内部记录当前的class
			并且创建了一个AnnotationMetadata类型对象（实例为StandardAnnotationMetadata）

			实例StandardAnnotationMetadata中记录了目标类的class，创建了一个MergedAnnotations类型对象（实例为TypeMappedAnnotations）

			创建MergedAnnotations类型对象的条件：
				查找的 AnnotatedElement；
				搜索策略 SearchStrategy；
				可重复注解容器 RepeatableContainers；
				注解过滤器 AnnotationFilter；
			*/
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
		/*根据 @Conditional 注解判断是否跳过注册*/
		if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
			return;
		}

		abd.setInstanceSupplier(supplier);

		/*解析bean作用域(单例或者原型)，如果有@Scope注解，则解析@Scope，没有则默认为单例的bean*/
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);

		/*设置获取到的bean的作用域*/
		abd.setScope(scopeMetadata.getScopeName());

		/*获取bean的名称，如果没有自动的创建一个bean的名称*/
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));

		// 处理这五个注解：@LazyInit、@Primary、@dependsOn、@role、@description 用户没有赋值则使用默认赋值
		AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);

		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				if (Primary.class == qualifier) {
					abd.setPrimary(true);
				}
				else if (Lazy.class == qualifier) {
					abd.setLazyInit(true);
				}
				else {
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}

		/*bean的回调处理*/
		if (customizers != null) {
			for (BeanDefinitionCustomizer customizer : customizers) {
				customizer.customize(abd);
			}
		}

		/*创建一个BeanDefinitionHolder实例，并且持有abd，beanName*/
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);

		/*是否使用代理模式创建definitionHolder：
		* 	否：返回原来的definitionHolder
		* 	是：创建一个新的definitionHolder
		* */
		definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);

		/*
		* 将bean定义注册到bean工厂，如果有别名就注册别名
		* */
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
	}


	/**
	 * 尽可能从给定注册表获取 Environment，否则返回一个新的 StandardEnvironment。
	 */
	private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		if (registry instanceof EnvironmentCapable) { //如果是EnvironmentCapable类型，那么可直接获取环境。因为该接口表示能够有访问spring环境的能力
			return ((EnvironmentCapable) registry).getEnvironment();
		}
		return new StandardEnvironment();
	}

}
