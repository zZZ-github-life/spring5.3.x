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

package org.springframework.beans.factory.xml;

import java.io.StringReader;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderContext;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 对 {@link org.springframework.beans.factory.parsing.ReaderContext} 的扩展，专门用于配合
 * {@link XmlBeanDefinitionReader}。提供对 {@link XmlBeanDefinitionReader} 中配置的
 * {@link NamespaceHandlerResolver} 的访问。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XmlReaderContext extends ReaderContext {

	private final XmlBeanDefinitionReader reader;

	private final NamespaceHandlerResolver namespaceHandlerResolver;


	/**
	 * 构造一个新的 {@code XmlReaderContext}。
	 * @param resource the XML bean definition resource
	 * @param problemReporter the problem reporter in use
	 * @param eventListener the event listener in use
	 * @param sourceExtractor the source extractor in use
	 * @param reader the XML bean definition reader in use
	 * @param namespaceHandlerResolver the XML namespace resolver
	 */
	public XmlReaderContext(
			Resource resource, ProblemReporter problemReporter,
			ReaderEventListener eventListener, SourceExtractor sourceExtractor,
			XmlBeanDefinitionReader reader, NamespaceHandlerResolver namespaceHandlerResolver) {

		super(resource, problemReporter, eventListener, sourceExtractor);
		this.reader = reader;
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}


	/**
	 * 返回正在使用的 XML Bean 定义读取器。
	 */
	public final XmlBeanDefinitionReader getReader() {
		return this.reader;
	}

	/**
	 * 返回要使用的 Bean 定义注册表。
	 * @see XmlBeanDefinitionReader#XmlBeanDefinitionReader(BeanDefinitionRegistry)
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.reader.getRegistry();
	}

	/**
	 * 返回要使用的资源加载器（如果有）。<p>在常规场景中它将非 null，同时也允许访问资源类加载器。
	 * @see XmlBeanDefinitionReader#setResourceLoader
	 * @see ResourceLoader#getClassLoader()
	 */
	@Nullable
	public final ResourceLoader getResourceLoader() {
		return this.reader.getResourceLoader();
	}

	/**
	 * 返回要使用的 Bean 类加载器（如果有）。<p>请注意，在常规场景中它将为 null，表示应延迟解析 Bean 类。
	 * @see XmlBeanDefinitionReader#setBeanClassLoader
	 */
	@Nullable
	public final ClassLoader getBeanClassLoader() {
		return this.reader.getBeanClassLoader();
	}

	/**
	 * 返回要使用的环境。
	 * @see XmlBeanDefinitionReader#setEnvironment
	 */
	public final Environment getEnvironment() {
		return this.reader.getEnvironment();
	}

	/**
	 * 返回命名空间解析器。
	 * @see XmlBeanDefinitionReader#setNamespaceHandlerResolver
	 */
	public final NamespaceHandlerResolver getNamespaceHandlerResolver() {
		return this.namespaceHandlerResolver;
	}


	// Convenience methods to delegate to

	/**
	 * 为给定的 Bean 定义调用 Bean 名称生成器。
	 * @see XmlBeanDefinitionReader#getBeanNameGenerator()
	 * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName
	 */
	public String generateBeanName(BeanDefinition beanDefinition) {
		return this.reader.getBeanNameGenerator().generateBeanName(beanDefinition, getRegistry());
	}

	/**
	 * 为给定的 Bean 定义调用 Bean 名称生成器，并以生成的名称注册该 Bean 定义。
	 * @see XmlBeanDefinitionReader#getBeanNameGenerator()
	 * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName
	 * @see BeanDefinitionRegistry#registerBeanDefinition
	 */
	public String registerWithGeneratedName(BeanDefinition beanDefinition) {
		String generatedName = generateBeanName(beanDefinition);
		getRegistry().registerBeanDefinition(generatedName, beanDefinition);
		return generatedName;
	}

	/**
	 * 从给定的 String 读取 XML 文档。
	 * @see #getReader()
	 */
	public Document readDocumentFromString(String documentContent) {
		InputSource is = new InputSource(new StringReader(documentContent));
		try {
			return this.reader.doLoadDocument(is, getResource());
		}
		catch (Exception ex) {
			throw new BeanDefinitionStoreException("Failed to read XML document", ex);
		}
	}

}
