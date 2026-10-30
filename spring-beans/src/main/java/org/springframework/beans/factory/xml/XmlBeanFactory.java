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

package org.springframework.beans.factory.xml;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.Resource;

/**
 * 对 {@link DefaultListableBeanFactory} 的便利扩展，可从 XML 文档读取 Bean 定义。底层委托给
 * {@link XmlBeanDefinitionReader}；实际上等同于将 XmlBeanDefinitionReader 与
 * DefaultListableBeanFactory 一起使用。
 *
 * <p>所需 XML 文档的结构、元素名和属性名都硬编码在此类中。（当然，如有必要可以运行转换以生成这种格
 * 式）。"beans" 不必是 XML 文档的根元素：此类会解析 XML 文件中的所有 Bean 定义元素。
 *
 * <p>此类将每个 Bean 定义注册到 {@link DefaultListableBeanFactory} 超类，并依赖后者对
 * {@link BeanFactory} 接口的实现。它支持单例、原型以及对这两种 Bean 的引用。有关选项和配置风格的详
 * 细信息，请参见 {@code "spring-beans-3.x.xsd"}（或历史上的 {@code "spring-beans-2.0.dtd"}）。
 *
 * <p><b>如有高级需求，可考虑将 {@link DefaultListableBeanFactory} 与
 * {@link XmlBeanDefinitionReader} 一起使用。</b>后者允许从多个 XML 资源读取，并且其实际的 XML 解析
 * 行为高度可配置。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 15 April 2001
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see XmlBeanDefinitionReader
 * @deprecated as of Spring 3.1 in favor of {@link DefaultListableBeanFactory} and
 * {@link XmlBeanDefinitionReader}
 */
@Deprecated
@SuppressWarnings({"serial", "all"})
public class XmlBeanFactory extends DefaultListableBeanFactory {

	private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);


	/**
	 * 使用给定资源创建新的 XmlBeanFactory，该资源必须可使用 DOM 解析。
	 * @param resource 要从中加载 Bean 定义的 XML 资源
	 * @throws BeansException 在加载或解析出错时
	 */
	public XmlBeanFactory(Resource resource) throws BeansException {
		this(resource, null);
	}

	/**
	 * 使用给定输入流创建新的 XmlBeanFactory，该输入流必须可使用 DOM 解析。
	 * @param resource 要从中加载 Bean 定义的 XML 资源
	 * @param parentBeanFactory 父 Bean 工厂
	 * @throws BeansException 在加载或解析出错时
	 */
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException {
		super(parentBeanFactory);
		this.reader.loadBeanDefinitions(resource);
	}

}
