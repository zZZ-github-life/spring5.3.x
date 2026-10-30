/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * {@link org.springframework.web.context.WebApplicationContext} 实现，
 * 其配置取自 XML 文档，由
 * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader} 解析。
 * 它本质上相当于 Web 环境中的
 * {@link org.springframework.context.support.GenericXmlApplicationContext}。
 *
 * <p>默认情况下，根上下文的配置取自 "/WEB-INF/applicationContext.xml"，命名空间为 "test-servlet"
 * 的上下文（例如 servlet-name 为 "test" 的 DispatcherServlet 实例）则取自
 * "/WEB-INF/test-servlet.xml"。
 *
 * <p>可以通过 {@link org.springframework.web.context.ContextLoader} 的 "contextConfigLocation"
 * context-param 以及 {@link org.springframework.web.servlet.FrameworkServlet} 的 servlet
 * init-param 覆盖默认的配置位置。配置位置既可以表示具体文件（如 "/WEB-INF/context.xml"），也可以
 * 表示 Ant 风格的模式（如 "/WEB-INF/*-context.xml"，模式细节请参见
 * {@link org.springframework.util.PathMatcher} 的 javadoc）。
 *
 * <p>注意：在有多个配置位置的情况下，后加载的 bean 定义将覆盖先前加载文件中定义的内容。可以利用
 * 这一点，通过一个额外的 XML 文件有意覆盖某些 bean 定义。
 *
 * <p><b>对于读取其他 bean 定义格式的 WebApplicationContext，
 * 应创建 {@link AbstractRefreshableWebApplicationContext} 的类似子类。</b>
 * 这种上下文实现可以指定为 ContextLoader 的 "contextClass" context-param，
 * 或 FrameworkServlet 的 "contextClass" init-param。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setNamespace
 * @see #setConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 * @see org.springframework.web.context.ContextLoader#initWebApplicationContext
 * @see org.springframework.web.servlet.FrameworkServlet#initWebApplicationContext
 */
public class XmlWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	/** 根上下文的默认配置位置。 */
	public static final String DEFAULT_CONFIG_LOCATION = "/WEB-INF/applicationContext.xml";

	/** 为命名空间构建配置位置时使用的默认前缀。 */
	public static final String DEFAULT_CONFIG_LOCATION_PREFIX = "/WEB-INF/";

	/** 为命名空间构建配置位置时使用的默认后缀。 */
	public static final String DEFAULT_CONFIG_LOCATION_SUFFIX = ".xml";


	/**
	 * 通过 XmlBeanDefinitionReader 加载 bean 定义。
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
		beanDefinitionReader.setEnvironment(getEnvironment());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		// Allow a subclass to provide custom initialization of the reader,
		// then proceed with actually loading the bean definitions.
		initBeanDefinitionReader(beanDefinitionReader);
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * 初始化用于加载此上下文 bean 定义的 bean 定义读取器。
	 * 默认实现为空。
	 * <p>可以在子类中重写，例如用于关闭 XML 校验
	 * 或使用不同的 XmlBeanDefinitionParser 实现。
	 * @param beanDefinitionReader 此上下文使用的 Bean 定义读取器
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setValidationMode
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的 XmlBeanDefinitionReader 加载 bean 定义。
	 * <p>bean 工厂的生命周期由 refreshBeanFactory 方法负责管理；
	 * 因此此方法只负责加载和/或注册 bean 定义。
	 * <p>委托给 ResourcePatternResolver 将位置模式解析为
	 * Resource 实例。
	 * @throws IOException 如果找不到所需的 XML 文档
	 * @see #refreshBeanFactory
	 * @see #getConfigLocations
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws IOException {
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				reader.loadBeanDefinitions(configLocation);
			}
		}
	}

	/**
	 * 根上下文的默认位置为 "/WEB-INF/applicationContext.xml"；
	 * 对于命名空间为 "test-servlet" 的上下文（例如 servlet-name 为 "test" 的
	 * DispatcherServlet 实例），则为 "/WEB-INF/test-servlet.xml"。
	 */
	@Override
	protected String[] getDefaultConfigLocations() {
		if (getNamespace() != null) {
			return new String[] {DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace() + DEFAULT_CONFIG_LOCATION_SUFFIX};
		}
		else {
			return new String[] {DEFAULT_CONFIG_LOCATION};
		}
	}

}
