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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;

import org.springframework.lang.Nullable;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * Spring 默认的 {@link DocumentLoader} 实现。
 *
 * <p>使用标准 JAXP 配置的 XML 解析器简单地加载 {@link Document documents}。如果要更改用于加载文档
 * 的 {@link DocumentBuilder}，一种策略是在启动 JVM 时定义相应的 Java 系统属性。例如，要使用 Oracle
 * {@link DocumentBuilder}，可以按如下方式启动应用：
 *
 * <pre class="code">java -Djavax.xml.parsers.DocumentBuilderFactory=oracle.xml.jaxp.JXDocumentBuilderFactory MyMainClass</pre>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class DefaultDocumentLoader implements DocumentLoader {

	/**
	 * 用于配置验证所用 schema 语言的 JAXP 属性。
	 */
	private static final String SCHEMA_LANGUAGE_ATTRIBUTE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	/**
	 * 表示 XSD schema 语言的 JAXP 属性值。
	 */
	private static final String XSD_SCHEMA_LANGUAGE = "http://www.w3.org/2001/XMLSchema";


	private static final Log logger = LogFactory.getLog(DefaultDocumentLoader.class);


	/**
	 * 使用标准 JAXP 配置的 XML 解析器，加载位于所提供 {@link InputSource} 处的 {@link Document}。
	 */
	@Override
	public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
			ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exception {

		DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
		if (logger.isTraceEnabled()) {
			logger.trace("Using JAXP provider [" + factory.getClass().getName() + "]");
		}
		DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHandler);
		return builder.parse(inputSource);
	}

	/**
	 * 创建 {@link DocumentBuilderFactory} 实例。
	 * @param validationMode 验证类型：{@link XmlValidationModeDetector#VALIDATION_DTD DTD}
	 * 或 {@link XmlValidationModeDetector#VALIDATION_XSD XSD}
	 * @param namespaceAware 返回的工厂是否要提供对 XML 命名空间的支持
	 * @return JAXP 的 DocumentBuilderFactory
	 * @throws ParserConfigurationException 如果无法构建合适的 DocumentBuilderFactory
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
			throws ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);

		if (validationMode != XmlValidationModeDetector.VALIDATION_NONE) {
			factory.setValidating(true);
			if (validationMode == XmlValidationModeDetector.VALIDATION_XSD) {
				// Enforce namespace aware for XSD...
				factory.setNamespaceAware(true);
				try {
					factory.setAttribute(SCHEMA_LANGUAGE_ATTRIBUTE, XSD_SCHEMA_LANGUAGE);
				}
				catch (IllegalArgumentException ex) {
					ParserConfigurationException pcex = new ParserConfigurationException(
							"Unable to validate using XSD: Your JAXP provider [" + factory +
							"] does not support XML Schema. Are you running on Java 1.4 with Apache Crimson? " +
							"Upgrade to Apache Xerces (or Java 1.5) for full XSD support.");
					pcex.initCause(ex);
					throw pcex;
				}
			}
		}

		return factory;
	}

	/**
	 * 创建一个 JAXP DocumentBuilder，此 Bean 定义读取器将用它解析 XML 文档。可以在子类中覆盖，以添加对
	 * builder 的进一步初始化。
	 * @param factory 用于创建 DocumentBuilder 的 JAXP DocumentBuilderFactory
	 * @param entityResolver 要使用的 SAX EntityResolver
	 * @param errorHandler 要使用的 SAX ErrorHandler
	 * @return JAXP 的 DocumentBuilder
	 * @throws ParserConfigurationException 如果由 JAXP 方法抛出
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory,
			@Nullable EntityResolver entityResolver, @Nullable ErrorHandler errorHandler)
			throws ParserConfigurationException {

		DocumentBuilder docBuilder = factory.newDocumentBuilder();
		if (entityResolver != null) {
			docBuilder.setEntityResolver(entityResolver);
		}
		if (errorHandler != null) {
			docBuilder.setErrorHandler(errorHandler);
		}
		return docBuilder;
	}

}
