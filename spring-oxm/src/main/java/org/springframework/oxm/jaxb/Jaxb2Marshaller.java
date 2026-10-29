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

package org.springframework.oxm.jaxb;

import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.oxm.GenericMarshaller;
import org.springframework.oxm.GenericUnmarshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.ValidationFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.oxm.mime.MimeMarshaller;
import org.springframework.oxm.mime.MimeUnmarshaller;
import org.springframework.oxm.support.SaxResourceUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * 适用于 JAXB 2.2 的 {@code GenericMarshaller} 接口实现。
 *
 * <p>典型用法是为此 bean 设置 "contextPath" 或 "classesToBeBound" 属性，
 * 并可能通过设置属性、schema、适配器和监听器来自定义 marshaller 和 unmarshaller，
 * 然后引用它。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see #setContextPath
 * @see #setClassesToBeBound
 * @see #setJaxbContextProperties
 * @see #setMarshallerProperties
 * @see #setUnmarshallerProperties
 * @see #setSchema
 * @see #setSchemas
 * @see #setMarshallerListener
 * @see #setUnmarshallerListener
 * @see #setAdapters
 */
public class Jaxb2Marshaller implements MimeMarshaller, MimeUnmarshaller, GenericMarshaller, GenericUnmarshaller,
		BeanClassLoaderAware, InitializingBean {

	private static final String CID = "cid:";

	private static final EntityResolver NO_OP_ENTITY_RESOLVER =
			(publicId, systemId) -> new InputSource(new StringReader(""));


	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private String contextPath;

	@Nullable
	private Class<?>[] classesToBeBound;

	@Nullable
	private String[] packagesToScan;

	@Nullable
	private Map<String, ?> jaxbContextProperties;

	@Nullable
	private Map<String, ?> marshallerProperties;

	@Nullable
	private Map<String, ?> unmarshallerProperties;

	@Nullable
	private Marshaller.Listener marshallerListener;

	@Nullable
	private Unmarshaller.Listener unmarshallerListener;

	@Nullable
	private ValidationEventHandler validationEventHandler;

	@Nullable
	private XmlAdapter<?, ?>[] adapters;

	@Nullable
	private Resource[] schemaResources;

	private String schemaLanguage = XMLConstants.W3C_XML_SCHEMA_NS_URI;

	@Nullable
	private LSResourceResolver schemaResourceResolver;

	private boolean lazyInit = false;

	private boolean mtomEnabled = false;

	private boolean supportJaxbElementClass = false;

	private boolean checkForXmlRootElement = true;

	@Nullable
	private Class<?> mappedClass;

	@Nullable
	private ClassLoader beanClassLoader;

	private final Object jaxbContextMonitor = new Object();

	@Nullable
	private volatile JAXBContext jaxbContext;

	@Nullable
	private Schema schema;

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;


	/**
	 * 设置多个 JAXB 上下文路径。给定的上下文路径数组会被转换为
	 * JAXB 所支持的冒号分隔字符串。
	 */
	public void setContextPaths(String... contextPaths) {
		Assert.notEmpty(contextPaths, "'contextPaths' must not be empty");
		this.contextPath = StringUtils.arrayToDelimitedString(contextPaths, ":");
	}

	/**
	 * 设置 JAXB 上下文路径。
	 * <p>必须设置此属性、{@link #setClassesToBeBound "classesToBeBound"}
	 * 或 {@link #setPackagesToScan "packagesToScan"} 之一。
	 */
	public void setContextPath(@Nullable String contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * 返回 JAXB 上下文路径。
	 */
	@Nullable
	public String getContextPath() {
		return this.contextPath;
	}

	/**
	 * 设置新建的 JAXBContext 所要识别的 Java 类列表。
	 * <p>必须设置此属性、{@link #setContextPath "contextPath"}
	 * 或 {@link #setPackagesToScan "packagesToScan"} 之一。
	 */
	public void setClassesToBeBound(@Nullable Class<?>... classesToBeBound) {
		this.classesToBeBound = classesToBeBound;
	}

	/**
	 * 返回新建的 JAXBContext 所要识别的 Java 类列表。
	 */
	@Nullable
	public Class<?>[] getClassesToBeBound() {
		return this.classesToBeBound;
	}

	/**
	 * 设置要在 classpath 中搜索带有 JAXB2 注解的类的包。
	 * 这里使用的是基于 Spring 的搜索，因此类似于 Spring 的组件扫描功能
	 * （{@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}）。
	 * <p>必须设置此属性、{@link #setContextPath "contextPath"} 或
	 * {@link #setClassesToBeBound "classesToBeBound"} 之一。
	 */
	public void setPackagesToScan(@Nullable String... packagesToScan) {
		this.packagesToScan = packagesToScan;
	}

	/**
	 * 返回用于搜索 JAXB2 注解的包。
	 */
	@Nullable
	public String[] getPackagesToScan() {
		return this.packagesToScan;
	}

	/**
	 * 设置 {@code JAXBContext} 属性。这些特定于实现的属性
	 * 将被设置到底层的 {@code JAXBContext} 上。
	 */
	public void setJaxbContextProperties(Map<String, ?> jaxbContextProperties) {
		this.jaxbContextProperties = jaxbContextProperties;
	}

	/**
	 * 设置 JAXB {@code Marshaller} 属性。
	 * <p>这些属性将被设置到底层的 JAXB {@code Marshaller} 上，
	 * 可用于缩进等功能。
	 * @param properties the properties
	 * @see javax.xml.bind.Marshaller#setProperty(String, Object)
	 * @see javax.xml.bind.Marshaller#JAXB_ENCODING
	 * @see javax.xml.bind.Marshaller#JAXB_FORMATTED_OUTPUT
	 * @see javax.xml.bind.Marshaller#JAXB_NO_NAMESPACE_SCHEMA_LOCATION
	 * @see javax.xml.bind.Marshaller#JAXB_SCHEMA_LOCATION
	 */
	public void setMarshallerProperties(Map<String, ?> properties) {
		this.marshallerProperties = properties;
	}

	/**
	 * 设置 JAXB {@code Unmarshaller} 属性。
	 * <p>这些属性将被设置到底层的 JAXB {@code Unmarshaller} 上。
	 * @param properties the properties
	 * @see javax.xml.bind.Unmarshaller#setProperty(String, Object)
	 */
	public void setUnmarshallerProperties(Map<String, ?> properties) {
		this.unmarshallerProperties = properties;
	}

	/**
	 * 指定要注册到 JAXB {@code Marshaller} 的 {@code Marshaller.Listener}。
	 */
	public void setMarshallerListener(Marshaller.Listener marshallerListener) {
		this.marshallerListener = marshallerListener;
	}

	/**
	 * 设置要注册到 JAXB {@code Unmarshaller} 的 {@code Unmarshaller.Listener}。
	 */
	public void setUnmarshallerListener(Unmarshaller.Listener unmarshallerListener) {
		this.unmarshallerListener = unmarshallerListener;
	}

	/**
	 * 设置 JAXB 校验事件处理器。如果在调用任何 marshal API 期间遇到
	 * 校验错误，JAXB 将调用此事件处理器。
	 */
	public void setValidationEventHandler(ValidationEventHandler validationEventHandler) {
		this.validationEventHandler = validationEventHandler;
	}

	/**
	 * 指定要注册到 JAXB {@code Marshaller} 和 {@code Unmarshaller} 的 {@code XmlAdapter}。
	 */
	public void setAdapters(XmlAdapter<?, ?>... adapters) {
		this.adapters = adapters;
	}

	/**
	 * 设置用于校验的 schema 资源。
	 */
	public void setSchema(Resource schemaResource) {
		this.schemaResources = new Resource[] {schemaResource};
	}

	/**
	 * 设置用于校验的 schema 资源。
	 */
	public void setSchemas(Resource... schemaResources) {
		this.schemaResources = schemaResources;
	}

	/**
	 * 设置 schema 语言。
	 * 默认为 W3C XML Schema：{@code http://www.w3.org/2001/XMLSchema"}。
	 * @see XMLConstants#W3C_XML_SCHEMA_NS_URI
	 * @see XMLConstants#RELAXNG_NS_URI
	 */
	public void setSchemaLanguage(String schemaLanguage) {
		this.schemaLanguage = schemaLanguage;
	}

	/**
	 * 设置资源解析器，用于加载 schema 资源。
	 * @see SchemaFactory#setResourceResolver(org.w3c.dom.ls.LSResourceResolver)
	 * @see #setSchema
	 * @see #setSchemas
	 */
	public void setSchemaResourceResolver(LSResourceResolver schemaResourceResolver) {
		this.schemaResourceResolver = schemaResourceResolver;
	}

	/**
	 * 设置是否为此 marshaller 延迟初始化 {@link JAXBContext}。
	 * 默认为 {@code false}，即在启动时初始化；可以改为 {@code true}。
	 * <p>只有在调用了 {@link #afterPropertiesSet()} 时才会执行提前初始化。
	 */
	public void setLazyInit(boolean lazyInit) {
		this.lazyInit = lazyInit;
	}

	/**
	 * 指定是否启用 MTOM 支持。
	 * 默认为 {@code false}：不启用使用 XOP/MTOM 的 marshalling。
	 */
	public void setMtomEnabled(boolean mtomEnabled) {
		this.mtomEnabled = mtomEnabled;
	}

	/**
	 * 指定 {@link #supports(Class)} 对 {@link JAXBElement} 类是否返回 {@code true}。
	 * <p>默认为 {@code false}，表示 {@code supports(Class)} 对 {@code JAXBElement}
	 * 类始终返回 {@code false}（不过 {@link #supports(Type)} 可能返回 {@code true}，
	 * 因为它可以获取 {@code JAXBElement} 的类型参数）。
	 * <p>此属性通常与 {@link org.springframework.web.servlet.view.xml.MarshallingView MarshallingView}
	 * 之类的类配合使用，因为 {@code ModelAndView} 在运行时并不提供类型参数信息。
	 * @see #supports(Class)
	 * @see #supports(Type)
	 */
	public void setSupportJaxbElementClass(boolean supportJaxbElementClass) {
		this.supportJaxbElementClass = supportJaxbElementClass;
	}

	/**
	 * 指定 {@link #supports(Class)} 是否应检查 {@link XmlRootElement @XmlRootElement} 注解。
	 * <p>默认为 {@code true}，表示 {@code supports(Class)} 将检查此注解。然而，某些 JAXB
	 * 实现（例如 EclipseLink MOXy）允许在外部定义文件中定义绑定，从而使类无需注解。
	 * 将此属性设置为 {@code false} 可支持这些 JAXB 实现。
	 * @see #supports(Class)
	 * @see #supports(Type)
	 */
	public void setCheckForXmlRootElement(boolean checkForXmlRootElement) {
		this.checkForXmlRootElement = checkForXmlRootElement;
	}

	/**
	 * 指定用于部分 unmarshalling 的 JAXB 映射类。
	 * @see javax.xml.bind.Unmarshaller#unmarshal(javax.xml.transform.Source, Class)
	 */
	public void setMappedClass(Class<?> mappedClass) {
		this.mappedClass = mappedClass;
	}

	/**
	 * 指示是否应支持 DTD 解析。
	 * <p>默认值为 {@code false}，表示禁用 DTD。
	 */
	public void setSupportDtd(boolean supportDtd) {
		this.supportDtd = supportDtd;
	}

	/**
	 * 返回是否支持 DTD 解析。
	 */
	public boolean isSupportDtd() {
		return this.supportDtd;
	}

	/**
	 * 指示在 unmarshalling 时是否处理外部 XML 实体。
	 * <p>默认值为 {@code false}，表示不解析外部实体。
	 * 请注意，只有当传递给 {@link #unmarshal(Source)} 的 {@code Source} 是
	 * {@link SAXSource} 或 {@link StreamSource} 时，才会启用/禁用外部实体的处理。
	 * 该设置对 {@link DOMSource} 或 {@link StAXSource} 实例没有影响。
	 * <p><strong>注意：</strong>将此选项设置为 {@code true} 也会自动将
	 * {@link #setSupportDtd} 设置为 {@code true}。
	 */
	public void setProcessExternalEntities(boolean processExternalEntities) {
		this.processExternalEntities = processExternalEntities;
		if (processExternalEntities) {
			this.supportDtd = true;
		}
	}

	/**
	 * 返回是否允许 XML 外部实体。
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		boolean hasContextPath = StringUtils.hasLength(this.contextPath);
		boolean hasClassesToBeBound = !ObjectUtils.isEmpty(this.classesToBeBound);
		boolean hasPackagesToScan = !ObjectUtils.isEmpty(this.packagesToScan);

		if (hasContextPath && (hasClassesToBeBound || hasPackagesToScan) ||
				(hasClassesToBeBound && hasPackagesToScan)) {
			throw new IllegalArgumentException("Specify either 'contextPath', 'classesToBeBound', " +
					"or 'packagesToScan'");
		}
		if (!hasContextPath && !hasClassesToBeBound && !hasPackagesToScan) {
			throw new IllegalArgumentException(
					"Setting either 'contextPath', 'classesToBeBound', " + "or 'packagesToScan' is required");
		}
		if (!this.lazyInit) {
			getJaxbContext();
		}
		if (!ObjectUtils.isEmpty(this.schemaResources)) {
			this.schema = loadSchema(this.schemaResources, this.schemaLanguage);
		}
	}

	/**
	 * 返回此 marshaller 所使用的 JAXBContext，如有必要则延迟构建。
	 */
	public JAXBContext getJaxbContext() {
		JAXBContext context = this.jaxbContext;
		if (context != null) {
			return context;
		}
		synchronized (this.jaxbContextMonitor) {
			context = this.jaxbContext;
			if (context == null) {
				try {
					if (StringUtils.hasLength(this.contextPath)) {
						context = createJaxbContextFromContextPath(this.contextPath);
					}
					else if (!ObjectUtils.isEmpty(this.classesToBeBound)) {
						context = createJaxbContextFromClasses(this.classesToBeBound);
					}
					else if (!ObjectUtils.isEmpty(this.packagesToScan)) {
						context = createJaxbContextFromPackages(this.packagesToScan);
					}
					else {
						context = JAXBContext.newInstance();
					}
					this.jaxbContext = context;
				}
				catch (JAXBException ex) {
					throw convertJaxbException(ex);
				}
			}
			return context;
		}
	}

	private JAXBContext createJaxbContextFromContextPath(String contextPath) throws JAXBException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating JAXBContext with context path [" + this.contextPath + "]");
		}
		if (this.jaxbContextProperties != null) {
			if (this.beanClassLoader != null) {
				return JAXBContext.newInstance(contextPath, this.beanClassLoader, this.jaxbContextProperties);
			}
			else {
				// analogous to the JAXBContext.newInstance(String) implementation
				return JAXBContext.newInstance(contextPath, Thread.currentThread().getContextClassLoader(),
						this.jaxbContextProperties);
			}
		}
		else {
			if (this.beanClassLoader != null) {
				return JAXBContext.newInstance(contextPath, this.beanClassLoader);
			}
			else {
				return JAXBContext.newInstance(contextPath);
			}
		}
	}

	private JAXBContext createJaxbContextFromClasses(Class<?>... classesToBeBound) throws JAXBException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating JAXBContext with classes to be bound [" +
					StringUtils.arrayToCommaDelimitedString(classesToBeBound) + "]");
		}
		if (this.jaxbContextProperties != null) {
			return JAXBContext.newInstance(classesToBeBound, this.jaxbContextProperties);
		}
		else {
			return JAXBContext.newInstance(classesToBeBound);
		}
	}

	private JAXBContext createJaxbContextFromPackages(String... packagesToScan) throws JAXBException {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating JAXBContext by scanning packages [" +
					StringUtils.arrayToCommaDelimitedString(packagesToScan) + "]");
		}
		ClassPathJaxb2TypeScanner scanner = new ClassPathJaxb2TypeScanner(this.beanClassLoader, packagesToScan);
		Class<?>[] jaxb2Classes = scanner.scanPackages();
		if (logger.isDebugEnabled()) {
			logger.debug("Found JAXB2 classes: [" + StringUtils.arrayToCommaDelimitedString(jaxb2Classes) + "]");
		}
		this.classesToBeBound = jaxb2Classes;
		if (this.jaxbContextProperties != null) {
			return JAXBContext.newInstance(jaxb2Classes, this.jaxbContextProperties);
		}
		else {
			return JAXBContext.newInstance(jaxb2Classes);
		}
	}

	@SuppressWarnings("deprecation")  // on JDK 9
	private Schema loadSchema(Resource[] resources, String schemaLanguage) throws IOException, SAXException {
		if (logger.isDebugEnabled()) {
			logger.debug("Setting validation schema to " +
					StringUtils.arrayToCommaDelimitedString(this.schemaResources));
		}
		Assert.notEmpty(resources, "No resources given");
		Assert.hasLength(schemaLanguage, "No schema language provided");
		Source[] schemaSources = new Source[resources.length];
		XMLReader xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
		xmlReader.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
		for (int i = 0; i < resources.length; i++) {
			Resource resource = resources[i];
			Assert.isTrue(resource != null && resource.exists(), () -> "Resource does not exist: " + resource);
			InputSource inputSource = SaxResourceUtils.createInputSource(resource);
			schemaSources[i] = new SAXSource(xmlReader, inputSource);
		}
		SchemaFactory schemaFactory = SchemaFactory.newInstance(schemaLanguage);
		if (this.schemaResourceResolver != null) {
			schemaFactory.setResourceResolver(this.schemaResourceResolver);
		}
		return schemaFactory.newSchema(schemaSources);
	}


	@Override
	public boolean supports(Class<?> clazz) {
		return (this.supportJaxbElementClass && JAXBElement.class.isAssignableFrom(clazz)) ||
				supportsInternal(clazz, this.checkForXmlRootElement);
	}

	@Override
	public boolean supports(Type genericType) {
		if (genericType instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) genericType;
			if (JAXBElement.class == parameterizedType.getRawType() &&
					parameterizedType.getActualTypeArguments().length == 1) {
				Type typeArgument = parameterizedType.getActualTypeArguments()[0];
				if (typeArgument instanceof Class) {
					Class<?> classArgument = (Class<?>) typeArgument;
					return ((classArgument.isArray() && Byte.TYPE == classArgument.getComponentType()) ||
							isPrimitiveWrapper(classArgument) || isStandardClass(classArgument) ||
							supportsInternal(classArgument, false));
				}
				else if (typeArgument instanceof GenericArrayType) {
					GenericArrayType arrayType = (GenericArrayType) typeArgument;
					return (Byte.TYPE == arrayType.getGenericComponentType());
				}
			}
		}
		else if (genericType instanceof Class) {
			Class<?> clazz = (Class<?>) genericType;
			return supportsInternal(clazz, this.checkForXmlRootElement);
		}
		return false;
	}

	private boolean supportsInternal(Class<?> clazz, boolean checkForXmlRootElement) {
		if (checkForXmlRootElement && AnnotationUtils.findAnnotation(clazz, XmlRootElement.class) == null) {
			return false;
		}
		if (StringUtils.hasLength(this.contextPath)) {
			String packageName = ClassUtils.getPackageName(clazz);
			String[] contextPaths = StringUtils.tokenizeToStringArray(this.contextPath, ":");
			for (String contextPath : contextPaths) {
				if (contextPath.equals(packageName)) {
					return true;
				}
			}
			return false;
		}
		else if (!ObjectUtils.isEmpty(this.classesToBeBound)) {
			return Arrays.asList(this.classesToBeBound).contains(clazz);
		}
		return false;
	}

	/**
	 * 检查给定类型是否为基本类型的包装类型。
	 * 参见 JAXB2 规范第 8.5.1 节。
	 */
	private boolean isPrimitiveWrapper(Class<?> clazz) {
		return (Boolean.class == clazz ||
				Byte.class == clazz ||
				Short.class == clazz ||
				Integer.class == clazz ||
				Long.class == clazz ||
				Float.class == clazz ||
				Double.class == clazz);
	}

	/**
	 * 检查给定类型是否为标准类。
	 * 参见 JAXB2 规范第 8.5.2 节。
	 */
	private boolean isStandardClass(Class<?> clazz) {
		return (String.class == clazz ||
				BigInteger.class.isAssignableFrom(clazz) ||
				BigDecimal.class.isAssignableFrom(clazz) ||
				Calendar.class.isAssignableFrom(clazz) ||
				Date.class.isAssignableFrom(clazz) ||
				QName.class.isAssignableFrom(clazz) ||
				URI.class == clazz ||
				XMLGregorianCalendar.class.isAssignableFrom(clazz) ||
				Duration.class.isAssignableFrom(clazz) ||
				Image.class == clazz ||
				DataHandler.class == clazz ||
				// Source and subclasses should be supported according to the JAXB2 spec, but aren't in the RI
				// Source.class.isAssignableFrom(clazz) ||
				UUID.class == clazz);

	}


	// Marshalling

	@Override
	public void marshal(Object graph, Result result) throws XmlMappingException {
		marshal(graph, result, null);
	}

	@Override
	public void marshal(Object graph, Result result, @Nullable MimeContainer mimeContainer) throws XmlMappingException {
		try {
			Marshaller marshaller = createMarshaller();
			if (this.mtomEnabled && mimeContainer != null) {
				marshaller.setAttachmentMarshaller(new Jaxb2AttachmentMarshaller(mimeContainer));
			}
			if (StaxUtils.isStaxResult(result)) {
				marshalStaxResult(marshaller, graph, result);
			}
			else {
				marshaller.marshal(graph, result);
			}
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * 返回一个新创建的 JAXB marshaller。
	 * <p>注意：JAXB marshaller 不一定是线程安全的。
	 * 此方法自 5.2 起为 public。
	 * @since 5.2
	 * @see #createUnmarshaller()
	 */
	public Marshaller createMarshaller() {
		try {
			Marshaller marshaller = getJaxbContext().createMarshaller();
			initJaxbMarshaller(marshaller);
			return marshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	private void marshalStaxResult(Marshaller jaxbMarshaller, Object graph, Result staxResult) throws JAXBException {
		XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
		if (streamWriter != null) {
			jaxbMarshaller.marshal(graph, streamWriter);
		}
		else {
			XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
			if (eventWriter != null) {
				jaxbMarshaller.marshal(graph, eventWriter);
			}
			else {
				throw new IllegalArgumentException("StAX Result contains neither XMLStreamWriter nor XMLEventConsumer");
			}
		}
	}

	/**
	 * 模板方法，可由具体的 JAXB marshaller 覆盖以实现自定义初始化行为。
	 * 在创建 JAXB {@code Marshaller} 之后、且设置好相应属性之后调用。
	 * <p>默认实现会设置
	 * {@link #setMarshallerProperties 已定义的属性}、
	 * {@link #setValidationEventHandler 校验事件处理器}、
	 * {@link #setSchemas schema}、{@link #setMarshallerListener 监听器}
	 * 以及 {@link #setAdapters 适配器}。
	 */
	protected void initJaxbMarshaller(Marshaller marshaller) throws JAXBException {
		if (this.marshallerProperties != null) {
			for (Map.Entry<String, ?> entry : this.marshallerProperties.entrySet()) {
				marshaller.setProperty(entry.getKey(), entry.getValue());
			}
		}
		if (this.marshallerListener != null) {
			marshaller.setListener(this.marshallerListener);
		}
		if (this.validationEventHandler != null) {
			marshaller.setEventHandler(this.validationEventHandler);
		}
		if (this.adapters != null) {
			for (XmlAdapter<?, ?> adapter : this.adapters) {
				marshaller.setAdapter(adapter);
			}
		}
		if (this.schema != null) {
			marshaller.setSchema(this.schema);
		}
	}


	// Unmarshalling

	@Override
	public Object unmarshal(Source source) throws XmlMappingException {
		return unmarshal(source, null);
	}

	@Override
	public Object unmarshal(Source source, @Nullable MimeContainer mimeContainer) throws XmlMappingException {
		source = processSource(source);

		try {
			Unmarshaller unmarshaller = createUnmarshaller();
			if (this.mtomEnabled && mimeContainer != null) {
				unmarshaller.setAttachmentUnmarshaller(new Jaxb2AttachmentUnmarshaller(mimeContainer));
			}
			if (StaxUtils.isStaxSource(source)) {
				return unmarshalStaxSource(unmarshaller, source);
			}
			else if (this.mappedClass != null) {
				return unmarshaller.unmarshal(source, this.mappedClass).getValue();
			}
			else {
				return unmarshaller.unmarshal(source);
			}
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling: " +
						"This can happen due to the presence of DTD declarations which are disabled.", ex);
			}
			throw ex;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	/**
	 * 返回一个新创建的 JAXB unmarshaller。
	 * <p>注意：JAXB unmarshaller 不一定是线程安全的。
	 * 此方法自 5.2 起为 public。
	 * @since 5.2
	 * @see #createMarshaller()
	 */
	public Unmarshaller createUnmarshaller() {
		try {
			Unmarshaller unmarshaller = getJaxbContext().createUnmarshaller();
			initJaxbUnmarshaller(unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException ex) {
			throw convertJaxbException(ex);
		}
	}

	protected Object unmarshalStaxSource(Unmarshaller jaxbUnmarshaller, Source staxSource) throws JAXBException {
		XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
		if (streamReader != null) {
			return (this.mappedClass != null ?
					jaxbUnmarshaller.unmarshal(streamReader, this.mappedClass).getValue() :
					jaxbUnmarshaller.unmarshal(streamReader));
		}
		else {
			XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
			if (eventReader != null) {
				return (this.mappedClass != null ?
						jaxbUnmarshaller.unmarshal(eventReader, this.mappedClass).getValue() :
						jaxbUnmarshaller.unmarshal(eventReader));
			}
			else {
				throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
			}
		}
	}

	@SuppressWarnings("deprecation")  // on JDK 9
	private Source processSource(Source source) {
		if (StaxUtils.isStaxSource(source) || source instanceof DOMSource) {
			return source;
		}

		XMLReader xmlReader = null;
		InputSource inputSource = null;

		if (source instanceof SAXSource) {
			SAXSource saxSource = (SAXSource) source;
			xmlReader = saxSource.getXMLReader();
			inputSource = saxSource.getInputSource();
		}
		else if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			if (streamSource.getInputStream() != null) {
				inputSource = new InputSource(streamSource.getInputStream());
			}
			else if (streamSource.getReader() != null) {
				inputSource = new InputSource(streamSource.getReader());
			}
			else {
				inputSource = new InputSource(streamSource.getSystemId());
			}
		}

		try {
			if (xmlReader == null) {
				xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
			}
			xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
			String name = "http://xml.org/sax/features/external-general-entities";
			xmlReader.setFeature(name, isProcessExternalEntities());
			if (!isProcessExternalEntities()) {
				xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
			}
			return new SAXSource(xmlReader, inputSource);
		}
		catch (SAXException ex) {
			logger.info("Processing of external entities could not be disabled", ex);
			return source;
		}
	}

	/**
	 * 模板方法，可由具体的 JAXB marshaller 覆盖以实现自定义初始化行为。
	 * 在创建 JAXB {@code Unmarshaller} 之后、且设置好相应属性之后调用。
	 * <p>默认实现会设置
	 * {@link #setUnmarshallerProperties 已定义的属性}、
	 * {@link #setValidationEventHandler 校验事件处理器}、
	 * {@link #setSchemas schema}、{@link #setUnmarshallerListener 监听器}
	 * 以及 {@link #setAdapters 适配器}。
	 */
	protected void initJaxbUnmarshaller(Unmarshaller unmarshaller) throws JAXBException {
		if (this.unmarshallerProperties != null) {
			for (Map.Entry<String, ?> entry : this.unmarshallerProperties.entrySet()) {
				unmarshaller.setProperty(entry.getKey(), entry.getValue());
			}
		}
		if (this.unmarshallerListener != null) {
			unmarshaller.setListener(this.unmarshallerListener);
		}
		if (this.validationEventHandler != null) {
			unmarshaller.setEventHandler(this.validationEventHandler);
		}
		if (this.adapters != null) {
			for (XmlAdapter<?, ?> adapter : this.adapters) {
				unmarshaller.setAdapter(adapter);
			}
		}
		if (this.schema != null) {
			unmarshaller.setSchema(this.schema);
		}
	}

	/**
	 * 将给定的 {@code JAXBException} 转换为 {@code org.springframework.oxm}
	 * 异常层次结构中相应的异常。
	 * @param ex {@code JAXBException} that occurred
	 * @return the corresponding {@code XmlMappingException}
	 */
	protected XmlMappingException convertJaxbException(JAXBException ex) {
		if (ex instanceof ValidationException) {
			return new ValidationFailureException("JAXB validation exception", ex);
		}
		else if (ex instanceof MarshalException) {
			return new MarshallingFailureException("JAXB marshalling exception", ex);
		}
		else if (ex instanceof UnmarshalException) {
			return new UnmarshallingFailureException("JAXB unmarshalling exception", ex);
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown JAXB exception", ex);
		}
	}


	private static class Jaxb2AttachmentMarshaller extends AttachmentMarshaller {

		private final MimeContainer mimeContainer;

		public Jaxb2AttachmentMarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public String addMtomAttachment(byte[] data, int offset, int length, String mimeType,
				String elementNamespace, String elementLocalName) {
			ByteArrayDataSource dataSource = new ByteArrayDataSource(mimeType, data, offset, length);
			return addMtomAttachment(new DataHandler(dataSource), elementNamespace, elementLocalName);
		}

		@Override
		public String addMtomAttachment(DataHandler dataHandler, String elementNamespace, String elementLocalName) {
			String host = getHost(elementNamespace, dataHandler);
			String contentId = UUID.randomUUID() + "@" + host;
			this.mimeContainer.addAttachment("<" + contentId + ">", dataHandler);
			try {
				contentId = URLEncoder.encode(contentId, "UTF-8");
			}
			catch (UnsupportedEncodingException ex) {
				// ignore
			}
			return CID + contentId;
		}

		private String getHost(String elementNamespace, DataHandler dataHandler) {
			try {
				URI uri = new URI(elementNamespace);
				return uri.getHost();
			}
			catch (URISyntaxException ex) {
				// ignore
			}
			return dataHandler.getName();
		}

		@Override
		public String addSwaRefAttachment(DataHandler dataHandler) {
			String contentId = UUID.randomUUID() + "@" + dataHandler.getName();
			this.mimeContainer.addAttachment(contentId, dataHandler);
			return contentId;
		}

		@Override
		public boolean isXOPPackage() {
			return this.mimeContainer.convertToXopPackage();
		}
	}


	private static class Jaxb2AttachmentUnmarshaller extends AttachmentUnmarshaller {

		private final MimeContainer mimeContainer;

		public Jaxb2AttachmentUnmarshaller(MimeContainer mimeContainer) {
			this.mimeContainer = mimeContainer;
		}

		@Override
		public byte[] getAttachmentAsByteArray(String cid) {
			try {
				DataHandler dataHandler = getAttachmentAsDataHandler(cid);
				return FileCopyUtils.copyToByteArray(dataHandler.getInputStream());
			}
			catch (IOException ex) {
				throw new UnmarshallingFailureException("Could not read attachment", ex);
			}
		}

		@Override
		public DataHandler getAttachmentAsDataHandler(String contentId) {
			if (contentId.startsWith(CID)) {
				contentId = contentId.substring(CID.length());
				try {
					contentId = URLDecoder.decode(contentId, "UTF-8");
				}
				catch (UnsupportedEncodingException ex) {
					// ignore
				}
				contentId = '<' + contentId + '>';
			}
			DataHandler dataHandler = this.mimeContainer.getAttachment(contentId);
			if (dataHandler == null) {
				throw new IllegalArgumentException("No attachment found for " + contentId);
			}
			return dataHandler;
		}

		@Override
		public boolean isXOPPackage() {
			return this.mimeContainer.isXopPackage();
		}
	}


	/**
	 * 包装字节数组的 DataSource。
	 */
	private static class ByteArrayDataSource implements DataSource {

		private final byte[] data;

		private final String contentType;

		private final int offset;

		private final int length;

		public ByteArrayDataSource(String contentType, byte[] data, int offset, int length) {
			this.contentType = contentType;
			this.data = data;
			this.offset = offset;
			this.length = length;
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(this.data, this.offset, this.length);
		}

		@Override
		public OutputStream getOutputStream() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getContentType() {
			return this.contentType;
		}

		@Override
		public String getName() {
			return "ByteArrayDataSource";
		}
	}

}
