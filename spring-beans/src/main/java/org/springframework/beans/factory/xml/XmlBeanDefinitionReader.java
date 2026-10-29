/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.EmptyReaderEventListener;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.NullSourceExtractor;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.parsing.ReaderEventListener;
import org.springframework.beans.factory.parsing.SourceExtractor;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;

/**
 * 用于 XML Bean 定义的 Bean 定义读取器。将实际的 XML 文档读取委托给
 * {@link BeanDefinitionDocumentReader} 接口的实现。
 *
 * <p>通常应用于 {@link org.springframework.beans.factory.support.DefaultListableBeanFactory} 或
 * {@link org.springframework.context.support.GenericApplicationContext}。
 *
 * <p>此类加载 DOM 文档，并将 BeanDefinitionDocumentReader 应用于该文档。文档读取器会把每个 Bean 定
 * 义注册到给定的 Bean 工厂，并调用该工厂对
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 接口的实现。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Chris Beams
 * @since 26.11.2003
 * @see #setDocumentReaderClass
 * @see BeanDefinitionDocumentReader
 * @see DefaultBeanDefinitionDocumentReader
 * @see BeanDefinitionRegistry
 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory
 * @see org.springframework.context.support.GenericApplicationContext
 */
public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

	/**
	 * 表示应禁用校验。
	 */
	public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

	/**
	 * 表示应自动检测校验模式。
	 */
	public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

	/**
	 * 表示应使用 DTD 校验。
	 */
	public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

	/**
	 * 表示应使用 XSD 校验。
	 */
	public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;


	/** 此类的 Constants 实例。 */
	private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

	private int validationMode = VALIDATION_AUTO;

	private boolean namespaceAware = false;

	private Class<? extends BeanDefinitionDocumentReader> documentReaderClass =
			DefaultBeanDefinitionDocumentReader.class;

	private ProblemReporter problemReporter = new FailFastProblemReporter();

	private ReaderEventListener eventListener = new EmptyReaderEventListener();

	private SourceExtractor sourceExtractor = new NullSourceExtractor();

	@Nullable
	private NamespaceHandlerResolver namespaceHandlerResolver;

	private DocumentLoader documentLoader = new DefaultDocumentLoader();

	@Nullable
	private EntityResolver entityResolver;

	private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

	private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

	private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded =
			new NamedThreadLocal<Set<EncodedResource>>("XML bean definition resources currently being loaded"){
				@Override
				protected Set<EncodedResource> initialValue() {
					return new HashSet<>(4);
				}
			};


	/**
	 * 为给定的 Bean 工厂创建新的 XmlBeanDefinitionReader。
	 * @param registry the BeanFactory to load bean definitions into,
	 * in the form of a BeanDefinitionRegistry
	 */
	public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
		super(registry);
	}


	/**
	 * 设置是否使用 XML 校验。默认值为 {@code true}。<p>如果关闭校验，此方法会开启命名空间感知，以便在
	 * 这种情况下仍能正确处理 schema 命名空间。
	 * @see #setValidationMode
	 * @see #setNamespaceAware
	 */
	public void setValidating(boolean validating) {
		this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
		this.namespaceAware = !validating;
	}

	/**
	 * 按名称设置要使用的校验模式。默认为 {@link #VALIDATION_AUTO}。
	 * @see #setValidationMode
	 */
	public void setValidationModeName(String validationModeName) {
		setValidationMode(constants.asNumber(validationModeName).intValue());
	}

	/**
	 * 设置要使用的校验模式。默认为 {@link #VALIDATION_AUTO}。<p>请注意，此方法仅激活或停用校验本身。如
	 * 果要对 schema 文件关闭校验，可能需要显式激活 schema 命名空间支持：参见
	 * {@link #setNamespaceAware}。
	 */
	public void setValidationMode(int validationMode) {
		this.validationMode = validationMode;
	}

	/**
	 * 返回要使用的校验模式。
	 */
	public int getValidationMode() {
		return this.validationMode;
	}

	/**
	 * 设置 XML 解析器是否应感知 XML 命名空间。默认值为 "false"。<p>当 schema 校验处于活动状态时通常不
	 * 需要这样做。但在没有校验的情况下，必须将其切换为 "true"，以便正确处理 schema 命名空间。
	 */
	public void setNamespaceAware(boolean namespaceAware) {
		this.namespaceAware = namespaceAware;
	}

	/**
	 * 返回 XML 解析器是否应感知 XML 命名空间。
	 */
	public boolean isNamespaceAware() {
		return this.namespaceAware;
	}

	/**
	 * 指定要使用的 {@link org.springframework.beans.factory.parsing.ProblemReporter}。<p>默认实现是
	 * {@link org.springframework.beans.factory.parsing.FailFastProblemReporter}，它表现出快速失败行为。
	 * 外部工具可以提供替代实现，收集错误和警告以便在工具 UI 中显示。
	 */
	public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
		this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
	}

	/**
	 * 指定要使用的 {@link ReaderEventListener}。<p>默认实现是 EmptyReaderEventListener，它会丢弃所有事
	 * 件通知。外部工具可以提供替代实现，以监视注册到 BeanFactory 中的组件。
	 */
	public void setEventListener(@Nullable ReaderEventListener eventListener) {
		this.eventListener = (eventListener != null ? eventListener : new EmptyReaderEventListener());
	}

	/**
	 * 指定要使用的 {@link SourceExtractor}。<p>默认实现是 {@link NullSourceExtractor}，它直接返回
	 * {@code null} 作为源对象。这意味着，在正常运行时执行期间，不会向 Bean 配置元数据附加额外的源元数
	 * 据。
	 */
	public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
		this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new NullSourceExtractor());
	}

	/**
	 * 指定要使用的 {@link NamespaceHandlerResolver}。<p>如果未指定，将通过
	 * {@link #createDefaultNamespaceHandlerResolver()} 创建默认实例。
	 */
	public void setNamespaceHandlerResolver(@Nullable NamespaceHandlerResolver namespaceHandlerResolver) {
		this.namespaceHandlerResolver = namespaceHandlerResolver;
	}

	/**
	 * 指定要使用的 {@link DocumentLoader}。<p>默认实现是 {@link DefaultDocumentLoader}，它使用 JAXP 加
	 * 载 {@link Document} 实例。
	 */
	public void setDocumentLoader(@Nullable DocumentLoader documentLoader) {
		this.documentLoader = (documentLoader != null ? documentLoader : new DefaultDocumentLoader());
	}

	/**
	 * 设置用于解析的 SAX 实体解析器。<p>默认将使用 {@link ResourceEntityResolver}。可以覆盖以实现自定
	 * 义实体解析，例如相对于某个特定基础路径的解析。
	 */
	public void setEntityResolver(@Nullable EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * 返回要使用的 EntityResolver；如果未指定，则构建一个默认解析器。
	 */
	protected EntityResolver getEntityResolver() {
		if (this.entityResolver == null) {
			// Determine default EntityResolver to use.
			ResourceLoader resourceLoader = getResourceLoader();
			if (resourceLoader != null) {
				this.entityResolver = new ResourceEntityResolver(resourceLoader);
			}
			else {
				this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
			}
		}
		return this.entityResolver;
	}

	/**
	 * 设置 {@code org.xml.sax.ErrorHandler} 接口的实现，以自定义处理 XML 解析错误和警告。<p>如果未设置，
	 * 将使用默认的 SimpleSaxErrorHandler，它只使用视图类的 logger 实例记录警告，并重新抛出错误以终止
	 * XML 转换。
	 * @see SimpleSaxErrorHandler
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 指定要使用的 {@link BeanDefinitionDocumentReader} 实现，它负责实际读取 XML Bean 定义文档。<p>默
	 * 认为 {@link DefaultBeanDefinitionDocumentReader}。
	 * @param documentReaderClass the desired BeanDefinitionDocumentReader implementation class
	 */
	public void setDocumentReaderClass(Class<? extends BeanDefinitionDocumentReader> documentReaderClass) {
		this.documentReaderClass = documentReaderClass;
	}


	/**
	 * 从指定的 XML 文件加载 Bean 定义。
	 * @param resource the resource descriptor for the XML file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	@Override
	public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(new EncodedResource(resource));
	}

	/**
	 * 从指定的 XML 文件加载 Bean 定义。
	 * @param encodedResource the resource descriptor for the XML file,
	 * allowing to specify an encoding to use for parsing the file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
		Assert.notNull(encodedResource, "EncodedResource must not be null");
		if (logger.isTraceEnabled()) {
			logger.trace("Loading XML bean definitions from " + encodedResource);
		}

		Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();

		if (!currentResources.add(encodedResource)) {
			throw new BeanDefinitionStoreException(
					"Detected cyclic loading of " + encodedResource + " - check your import definitions!");
		}

		try (InputStream inputStream = encodedResource.getResource().getInputStream()) {
			InputSource inputSource = new InputSource(inputStream);
			if (encodedResource.getEncoding() != null) {
				inputSource.setEncoding(encodedResource.getEncoding());
			}
			return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"IOException parsing XML document from " + encodedResource.getResource(), ex);
		}
		finally {
			currentResources.remove(encodedResource);
			if (currentResources.isEmpty()) {
				this.resourcesCurrentlyBeingLoaded.remove();
			}
		}
	}

	/**
	 * 从指定的 XML 文件加载 Bean 定义。
	 * @param inputSource the SAX InputSource to read from
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
	}

	/**
	 * 从指定的 XML 文件加载 Bean 定义。
	 * @param inputSource the SAX InputSource to read from
	 * @param resourceDescription a description of the resource
	 * (can be {@code null} or empty)
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(InputSource inputSource, @Nullable String resourceDescription)
			throws BeanDefinitionStoreException {

		return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
	}


	/**
	 * 实际从指定的 XML 文件加载 Bean 定义。
	 * @param inputSource the SAX InputSource to read from
	 * @param resource the resource descriptor for the XML file
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #doLoadDocument
	 * @see #registerBeanDefinitions
	 */
	protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
			throws BeanDefinitionStoreException {

		try {
			Document doc = doLoadDocument(inputSource, resource);
			int count = registerBeanDefinitions(doc, resource);
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + count + " bean definitions from " + resource);
			}
			return count;
		}
		catch (BeanDefinitionStoreException ex) {
			throw ex;
		}
		catch (SAXParseException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
		}
		catch (SAXException ex) {
			throw new XmlBeanDefinitionStoreException(resource.getDescription(),
					"XML document from " + resource + " is invalid", ex);
		}
		catch (ParserConfigurationException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Parser configuration exception parsing XML from " + resource, ex);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"IOException parsing XML document from " + resource, ex);
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException(resource.getDescription(),
					"Unexpected exception parsing XML document from " + resource, ex);
		}
	}

	/**
	 * 使用配置的 DocumentLoader 实际加载指定的文档。
	 * @param inputSource the SAX InputSource to read from
	 * @param resource the resource descriptor for the XML file
	 * @return the DOM Document
	 * @throws Exception when thrown from the DocumentLoader
	 * @see #setDocumentLoader
	 * @see DocumentLoader#loadDocument
	 */
	protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
		return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler,
				getValidationModeForResource(resource), isNamespaceAware());
	}

	/**
	 * 确定指定 {@link Resource} 的校验模式。如果未配置显式的校验模式，则会从给定资源中
	 * {@link #detectValidationMode detected} 校验模式。<p>如果希望完全控制校验模式，即使设置了
	 * {@link #VALIDATION_AUTO} 以外的值，也可以覆盖此方法。
	 * @see #detectValidationMode
	 */
	protected int getValidationModeForResource(Resource resource) {
		int validationModeToUse = getValidationMode();
		if (validationModeToUse != VALIDATION_AUTO) {
			return validationModeToUse;
		}
		int detectedMode = detectValidationMode(resource);
		if (detectedMode != VALIDATION_AUTO) {
			return detectedMode;
		}
		// Hmm, we didn't get a clear indication... Let's assume XSD,
		// since apparently no DTD declaration has been found up until
		// detection stopped (before finding the document's root tag).
		return VALIDATION_XSD;
	}

	/**
	 * 检测对由所提供 {@link Resource} 标识的 XML 文件执行哪种校验。如果文件包含 {@code DOCTYPE} 定义，
	 * 则使用 DTD 校验，否则假定使用 XSD 校验。<p>如果希望自定义 {@link #VALIDATION_AUTO} 模式的解析方
	 * 式，可以覆盖此方法。
	 */
	protected int detectValidationMode(Resource resource) {
		if (resource.isOpen()) {
			throw new BeanDefinitionStoreException(
					"Passed-in Resource [" + resource + "] contains an open stream: " +
					"cannot determine validation mode automatically. Either pass in a Resource " +
					"that is able to create fresh streams, or explicitly specify the validationMode " +
					"on your XmlBeanDefinitionReader instance.");
		}

		InputStream inputStream;
		try {
			inputStream = resource.getInputStream();
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException(
					"Unable to determine validation mode for [" + resource + "]: cannot open InputStream. " +
					"Did you attempt to load directly from a SAX InputSource without specifying the " +
					"validationMode on your XmlBeanDefinitionReader instance?", ex);
		}

		try {
			return this.validationModeDetector.detectValidationMode(inputStream);
		}
		catch (IOException ex) {
			throw new BeanDefinitionStoreException("Unable to determine validation mode for [" +
					resource + "]: an error occurred whilst reading from the InputStream.", ex);
		}
	}

	/**
	 * 注册给定 DOM 文档中包含的 Bean 定义。由 {@code loadBeanDefinitions} 调用。<p>创建解析器类的新实
	 * 例，并在其上调用 {@code registerBeanDefinitions}。
	 * @param doc the DOM document
	 * @param resource the resource descriptor (for context information)
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of parsing errors
	 * @see #loadBeanDefinitions
	 * @see #setDocumentReaderClass
	 * @see BeanDefinitionDocumentReader#registerBeanDefinitions
	 */
	public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
		BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
		int countBefore = getRegistry().getBeanDefinitionCount();
		documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
		return getRegistry().getBeanDefinitionCount() - countBefore;
	}

	/**
	 * 创建用于实际从 XML 文档读取 Bean 定义的 {@link BeanDefinitionDocumentReader}。<p>默认实现会实例
	 * 化指定的 "documentReaderClass"。
	 * @see #setDocumentReaderClass
	 */
	protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
		return BeanUtils.instantiateClass(this.documentReaderClass);
	}

	/**
	 * 创建要传递给文档读取器的 {@link XmlReaderContext}。
	 */
	public XmlReaderContext createReaderContext(Resource resource) {
		return new XmlReaderContext(resource, this.problemReporter, this.eventListener,
				this.sourceExtractor, this, getNamespaceHandlerResolver());
	}

	/**
	 * 如果之前未设置，则延迟创建默认的 NamespaceHandlerResolver。
	 * @see #createDefaultNamespaceHandlerResolver()
	 */
	public NamespaceHandlerResolver getNamespaceHandlerResolver() {
		if (this.namespaceHandlerResolver == null) {
			this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
		}
		return this.namespaceHandlerResolver;
	}

	/**
	 * 创建在未指定时使用的 {@link NamespaceHandlerResolver} 默认实现。<p>默认实现返回
	 * {@link DefaultNamespaceHandlerResolver} 的实例。
	 * @see DefaultNamespaceHandlerResolver#DefaultNamespaceHandlerResolver(ClassLoader)
	 */
	protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
		ResourceLoader resourceLoader = getResourceLoader();
		ClassLoader cl = (resourceLoader != null ? resourceLoader.getClassLoader() : getBeanClassLoader());
		return new DefaultNamespaceHandlerResolver(cl);
	}

}
