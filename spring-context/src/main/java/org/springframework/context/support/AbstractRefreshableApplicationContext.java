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

package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.context.ApplicationContext} 实现的基类，这些实现应支持多次调用
 * {@link #refresh()}，每次都会创建一个新的内部 Bean 工厂实例。通常（但不一定），此类上下文由一组用
 * 于加载 Bean 定义的配置位置驱动。
 *
 * <p>子类唯一需要实现的方法是 {@link #loadBeanDefinitions}，它在每次刷新时被调用。具体实现应将
 * Bean 定义加载到给定的
 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} 中，通常委托给一个
 * 或多个特定的 Bean 定义读取器。
 *
 * <p><b>注意，WebApplicationContext 也有类似的基类。</b>
 * {@link org.springframework.web.context.support.AbstractRefreshableWebApplicationContext} 提供相
 * 同的子类化策略，但还额外为 Web 环境预实现了所有上下文功能。此外，还有预定义的方式为 Web 上下文接
 * 收配置位置。
 *
 * <p>读取特定 Bean 定义格式的具体独立子类包括 {@link ClassPathXmlApplicationContext} 和
 * {@link FileSystemXmlApplicationContext}，二者都派生自通用基类
 * {@link AbstractXmlApplicationContext}；
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext} 支持将
 * {@code @Configuration} 注解类作为 Bean 定义的来源。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.3
 * @see #loadBeanDefinitions
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
 * @see AbstractXmlApplicationContext
 * @see ClassPathXmlApplicationContext
 * @see FileSystemXmlApplicationContext
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 */
public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

	@Nullable
	private Boolean allowBeanDefinitionOverriding;

	@Nullable
	private Boolean allowCircularReferences;

	/** 此上下文的 bean 工厂。 */
	@Nullable
	private volatile DefaultListableBeanFactory beanFactory;


	/**
	 * 创建一个没有父上下文的新 AbstractRefreshableApplicationContext。
	 */
	public AbstractRefreshableApplicationContext() {
	}

	/**
	 * 使用给定的父上下文创建一个新的 AbstractRefreshableApplicationContext。
	 * @param parent 父上下文
	 */
	public AbstractRefreshableApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 设置是否允许通过注册同名但不同的定义来覆盖 Bean 定义，并自动替换前者。如果不允许，则会抛出异常。
	 * 默认值为 "true"。
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * 设置是否允许 Bean 之间的循环引用，并自动尝试解析它们。
	 *
	 * <p>默认值为 "true"。将其关闭后，遇到循环引用时将抛出异常，从而完全禁止循环引用。
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 */
	public void setAllowCircularReferences(boolean allowCircularReferences) {
		this.allowCircularReferences = allowCircularReferences;
	}


	/**
	 * 此实现会实际刷新此上下文底层的 Bean 工厂，关闭先前的 Bean 工厂（如果有），并为上下文生命周期的下
	 * 一阶段初始化一个新的 Bean 工厂。
	 */
	@Override
	protected final void refreshBeanFactory() throws BeansException {
		if (hasBeanFactory()) {
			destroyBeans();
			closeBeanFactory();
		}
		try {
			DefaultListableBeanFactory beanFactory = createBeanFactory();
			beanFactory.setSerializationId(getId());
			customizeBeanFactory(beanFactory);
			loadBeanDefinitions(beanFactory);
			this.beanFactory = beanFactory;
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
		}
	}

	@Override
	protected void cancelRefresh(BeansException ex) {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
		}
		super.cancelRefresh(ex);
	}

	@Override
	protected final void closeBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory != null) {
			beanFactory.setSerializationId(null);
			this.beanFactory = null;
		}
	}

	/**
	 * 确定此上下文当前是否持有 Bean 工厂，即是否已至少刷新过一次且尚未关闭。
	 */
	protected final boolean hasBeanFactory() {
		return (this.beanFactory != null);
	}

	@Override
	public final ConfigurableListableBeanFactory getBeanFactory() {
		DefaultListableBeanFactory beanFactory = this.beanFactory;
		if (beanFactory == null) {
			throw new IllegalStateException("BeanFactory not initialized or already closed - " +
					"call 'refresh' before accessing beans via the ApplicationContext");
		}
		return beanFactory;
	}

	/**
	 * 重写为空操作：对于 AbstractRefreshableApplicationContext，{@link #getBeanFactory()} 本身已对活动
	 * 上下文起到强断言作用。
	 */
	@Override
	protected void assertBeanFactoryActive() {
	}

	/**
	 * 为此上下文创建一个内部 Bean 工厂。每次 {@link #refresh()} 尝试时调用。
	 *
	 * <p>默认实现会创建一个
	 * {@link org.springframework.beans.factory.support.DefaultListableBeanFactory}，并将此上下文父级的
	 * {@linkplain #getInternalParentBeanFactory() internal bean factory}作为父 Bean 工厂。可以在子类中
	 * 重写，例如自定义 DefaultListableBeanFactory 的设置。
	 * @return 此上下文的 Bean 工厂
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * 自定义此上下文使用的内部 Bean 工厂。每次 {@link #refresh()} 尝试时调用。
	 *
	 * <p>如果指定了，默认实现会应用此上下文的
	 * {@linkplain #setAllowBeanDefinitionOverriding "allowBeanDefinitionOverriding"} 和
	 * {@linkplain #setAllowCircularReferences "allowCircularReferences"} 设置。可以在子类中重写，以自
	 * 定义 {@link DefaultListableBeanFactory} 的任意设置。
	 * @param beanFactory 为此上下文新创建的 Bean 工厂
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see DefaultListableBeanFactory#setAllowCircularReferences
	 * @see DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 * @see DefaultListableBeanFactory#setAllowEagerClassLoading
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
		if (this.allowBeanDefinitionOverriding != null) {
			beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
		if (this.allowCircularReferences != null) {
			beanFactory.setAllowCircularReferences(this.allowCircularReferences);
		}
	}

	/**
	 * 将 Bean 定义加载到给定的 Bean 工厂中，通常通过委托给一个或多个 Bean 定义读取器来完成。
	 * @param beanFactory 要向其加载 Bean 定义的 Bean 工厂
	 * @throws BeansException 如果 Bean 定义解析失败
	 * @throws IOException 如果 Bean 定义文件加载失败
	 * @see org.springframework.beans.factory.support.PropertiesBeanDefinitionReader
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 */
	protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws BeansException, IOException;

}
