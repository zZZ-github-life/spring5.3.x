/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.context.ApplicationContext} 实现的便捷基类，从包含 Bean 定义的 XML 文
 * 档中获取配置，这些定义由 {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}
 * 理解。
 *
 * <p>子类只需实现 {@link #getConfigResources} 和/或 {@link #getConfigLocations} 方法。此外，它们可
 * 以重写 {@link #getResourceByPath} 钩子，以环境特定的方式解释相对路径，和/或重写
 * {@link #getResourcePatternResolver} 以扩展模式解析。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConfigResources
 * @see #getConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

	private boolean validating = true;


	/**
	 * 创建一个没有父上下文的新 AbstractXmlApplicationContext。
	 */
	public AbstractXmlApplicationContext() {
	}

	/**
	 * 使用给定的父上下文创建一个新的 AbstractXmlApplicationContext。
	 * @param parent the parent context
	 */
	public AbstractXmlApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 设置是否使用 XML 验证。默认值为 {@code true}。
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}


	/**
	 * 通过 XmlBeanDefinitionReader 加载 Bean 定义。
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
	 * @see #initBeanDefinitionReader
	 * @see #loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// Create a new XmlBeanDefinitionReader for the given BeanFactory.
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// Configure the bean definition reader with this context's
		// resource loading environment.
		beanDefinitionReader.setEnvironment(this.getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文 Bean 定义的 Bean 定义读取器。默认实现为空。
	 *
	 * <p>可以在子类中重写，例如关闭 XML 验证或使用不同的 XmlBeanDefinitionParser 实现。
	 * @param reader the bean definition reader used by this context
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
		reader.setValidating(this.validating);
	}

	/**
	 * 使用给定的 XmlBeanDefinitionReader 加载 Bean 定义。
	 *
	 * <p>Bean 工厂的生命周期由 {@link #refreshBeanFactory} 方法处理；因此，此方法只应负责加载和/或注册
	 * Bean 定义。
	 * @param reader the XmlBeanDefinitionReader to use
	 * @throws BeansException in case of bean registration errors
	 * @throws IOException if the required XML document isn't found
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		Resource[] configResources = getConfigResources();
		if (configResources != null) {
			reader.loadBeanDefinitions(configResources);
		}
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			reader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * 返回 Resource 对象数组，这些对象指向构建此上下文应使用的 XML Bean 定义文件。
	 *
	 * <p>默认实现返回 {@code null}。子类可以重写此方法，提供预先构建的 Resource 对象，而不是位置
	 * String。
	 * @return an array of Resource objects, or {@code null} if none
	 * @see #getConfigLocations()
	 */
	@Nullable
	protected Resource[] getConfigResources() {
		return null;
	}

}
