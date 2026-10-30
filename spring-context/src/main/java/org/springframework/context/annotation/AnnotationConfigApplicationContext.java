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

package org.springframework.context.annotation;

import java.util.Arrays;
import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 独立的应用上下文，接受<i>组件类</i>作为输入 &mdash; 特别是 {@link Configuration @Configuration}
 * 注解的类，但也包括普通的 {@link org.springframework.stereotype.Component @Component} 类型以及使
 * 用 {@code javax.inject} 注解的符合 JSR-330 的类。
 *
 * <p>既支持使用 {@link #register(Class...)} 逐个注册类，也支持使用 {@link #scan(String...)} 进行类
 * 路径扫描。
 *
 * <p>在存在多个 {@code @Configuration} 类的情况下，后定义类中的 {@link Bean @Bean} 方法将覆盖先定
 * 义类中的方法。可以利用这一点，通过额外的 {@code @Configuration} 类来有意覆盖某些 Bean 定义。
 *
 * <p>用法示例请参见 {@link Configuration @Configuration} 的 javadoc。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see org.springframework.context.support.GenericXmlApplicationContext
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

	private final AnnotatedBeanDefinitionReader reader;

	private final ClassPathBeanDefinitionScanner scanner;


	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，需要通过 {@link #register} 调用进行填充，然后手
	 * 动 {@linkplain #refresh 刷新}。
	 */
	/*
	* 这里创建spring容器的两大成员
	* 	1.注解的bean定义读取器：
	* 		a.创建ConditionEvaluator，主要记录了容器的重要信息
	* 		b.给bean工厂注册各种后置处理器。
	* 	2.类路劲的bean定义扫描器：
	* 		a.注册了扫描过滤器。哪些注解的bean是需要被扫描的
	* 		b.内部创建了一个元数据缓存工厂，为了后面初始化bean存储类以及类的文件信息
	*
	* */
	public AnnotationConfigApplicationContext() {
		StartupStep createAnnotatedBeanDefReader = this.getApplicationStartup().start("spring.context.annotated-bean-reader.create");
		this.reader = new AnnotatedBeanDefinitionReader(this);
		createAnnotatedBeanDefReader.end();
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 使用给定的 DefaultListableBeanFactory 创建一个新的 AnnotationConfigApplicationContext。
	 * @param beanFactory 此上下文要使用的 DefaultListableBeanFactory 实例
	 */
	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
		this.reader = new AnnotatedBeanDefinitionReader(this);
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}

	/**
	 * 1.创建一个新的容器，初始化容器的相关资源，为后续使用。初始化两大成员： AnnotatedBeanDefinitionReader ，ClassPathBeanDefinitionScanner
	 * 2.注册bean
	 * 3.刷新容器
	 * @param componentClasses
	 */
	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
		this();
		register(componentClasses);
		refresh();
	}

	/**
	 * 创建一个新的 AnnotationConfigApplicationContext，在给定包中扫描组件，为这些组件注册 Bean 定义，
	 * 并自动刷新上下文。
	 * @param basePackages 要扫描组件类的包
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		this();
		scan(basePackages);
		refresh();
	}


	/**
	 * 将给定的自定义 {@code Environment} 传播到底层的 {@link AnnotatedBeanDefinitionReader} 和
	 * {@link ClassPathBeanDefinitionScanner}。
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		super.setEnvironment(environment);
		this.reader.setEnvironment(environment);
		this.scanner.setEnvironment(environment);
	}

	/**
	 * 提供自定义的 {@link BeanNameGenerator}，供 {@link AnnotatedBeanDefinitionReader} 和/或
	 * {@link ClassPathBeanDefinitionScanner} 使用（如果有）。
	 *
	 * <p>默认为 {@link AnnotationBeanNameGenerator}。
	 *
	 * <p>对此方法的任何调用都必须在调用 {@link #register(Class...)} 和/或 {@link #scan(String...)} 之
	 * 前进行。
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 * @see AnnotationBeanNameGenerator
	 * @see FullyQualifiedAnnotationBeanNameGenerator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
		getBeanFactory().registerSingleton(
				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
	}

	/**
	 * 设置用于已注册组件类的 {@link ScopeMetadataResolver}。
	 *
	 * <p>默认为 {@link AnnotationScopeMetadataResolver}。
	 *
	 * <p>对此方法的任何调用都必须在调用 {@link #register(Class...)} 和/或 {@link #scan(String...)} 之
	 * 前进行。
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}


	//---------------------------------------------------------------------
	// Implementation of AnnotationConfigRegistry
	//---------------------------------------------------------------------

	/**
	 * 注册一个或多个要处理的组件类。
	 *
	 * <p>请注意，为了使上下文完全处理新类，必须调用 {@link #refresh()}。
	 * @param componentClasses 一个或多个组件类，例如 {@link Configuration @Configuration} 类
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	@Override
	public void register(Class<?>... componentClasses) {
		Assert.notEmpty(componentClasses, "At least one component class must be specified");
		StartupStep registerComponentClass = this.getApplicationStartup().start("spring.context.component-classes.register")
				.tag("classes", () -> Arrays.toString(componentClasses));
		this.reader.register(componentClasses);
		registerComponentClass.end();
	}

	/**
	 * 在指定的基础包中执行扫描。
	 *
	 * <p>请注意，为了使上下文完全处理新类，必须调用 {@link #refresh()}。
	 * @param basePackages 要扫描组件类的包
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	@Override
	public void scan(String... basePackages) {
		Assert.notEmpty(basePackages, "At least one base package must be specified");
		StartupStep scanPackages = this.getApplicationStartup().start("spring.context.base-packages.scan")
				.tag("packages", () -> Arrays.toString(basePackages));
		this.scanner.scan(basePackages);
		scanPackages.end();
	}


	//---------------------------------------------------------------------
	// Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
	//---------------------------------------------------------------------

	@Override
	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {

		this.reader.registerBean(beanClass, beanName, supplier, customizers);
	}

}
