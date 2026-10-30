/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.oxm.xstream;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.MarshallingStrategy;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.ConverterMatcher;
import com.thoughtworks.xstream.converters.ConverterRegistry;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.SingleValueConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.core.DefaultConverterLookup;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.DomReader;
import com.thoughtworks.xstream.io.xml.DomWriter;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.SaxWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxReader;
import com.thoughtworks.xstream.io.xml.StaxWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import com.thoughtworks.xstream.security.TypePermission;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.support.AbstractMarshaller;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.xml.StaxUtils;

/**
 * 适用于 XStream 的 {@code Marshaller} 接口实现。
 *
 * <p>默认情况下，XStream 不需要任何额外配置，并且可以 (un)marshal classpath 上的任何类。
 * 因此，<b>不建议使用 {@code XStreamMarshaller} 来 unmarshal 来自外部来源（例如 Web）
 * 的 XML</b>，因为这可能导致<b>安全漏洞</b>。如果确实要使用 {@code XStreamMarshaller}
 * 来 unmarshal 外部 XML，请设置 {@link #setSupportedClasses(Class[]) supportedClasses}
 * 和 {@link #setConverters(ConverterMatcher[]) converters} 属性（可以使用
 * {@link CatchAllConverter}），或者覆盖 {@link #customizeXStream(XStream)} 方法，
 * 以确保它只接受你希望支持的类。
 *
 * <p>由于 XStream 的 API 限制，必须设置写入 OutputStream 时使用的编码。
 * 默认值为 {@code UTF-8}。
 *
 * <p><b>注意：</b>XStream 是 XML 序列化库，而不是数据绑定库。
 * 因此，它的命名空间支持有限。所以它不太适合在 Web 服务中使用。
 *
 * <p>自 Spring 5.2.17 起，此 marshaller 要求 XStream 1.4.7 或更高版本。
 * 请注意，{@link XStream} 的构造方式在 4.0 中已重构，现在流驱动程序和类加载器
 * 都直接传入 XStream 本身。
 *
 * @author Peter Meijer
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public class XStreamMarshaller extends AbstractMarshaller implements BeanClassLoaderAware, InitializingBean {

	/**
	 * 用于流访问的默认编码：UTF-8。
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";


	@Nullable
	private ReflectionProvider reflectionProvider;

	@Nullable
	private HierarchicalStreamDriver streamDriver;

	@Nullable
	private HierarchicalStreamDriver defaultDriver;

	@Nullable
	private Mapper mapper;

	@Nullable
	private Class<? extends MapperWrapper>[] mapperWrappers;

	private ConverterLookup converterLookup = new DefaultConverterLookup();

	private ConverterRegistry converterRegistry = (ConverterRegistry) this.converterLookup;

	@Nullable
	private ConverterMatcher[] converters;

	@Nullable
	private TypePermission[] typePermissions;

	@Nullable
	private MarshallingStrategy marshallingStrategy;

	@Nullable
	private Integer mode;

	@Nullable
	private Map<String, ?> aliases;

	@Nullable
	private Map<String, ?> aliasesByType;

	@Nullable
	private Map<String, String> fieldAliases;

	@Nullable
	private Class<?>[] useAttributeForTypes;

	@Nullable
	private Map<?, ?> useAttributeFor;

	@Nullable
	private Map<Class<?>, String> implicitCollections;

	@Nullable
	private Map<Class<?>, String> omittedFields;

	@Nullable
	private Class<?>[] annotatedClasses;

	private boolean autodetectAnnotations;

	private String encoding = DEFAULT_ENCODING;

	private NameCoder nameCoder = new XmlFriendlyNameCoder();

	@Nullable
	private Class<?>[] supportedClasses;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private final SingletonSupplier<XStream> xstream = SingletonSupplier.of(this::buildXStream);


	/**
	 * 设置要使用的自定义 XStream {@link ReflectionProvider}。
	 * @since 4.0
	 */
	public void setReflectionProvider(ReflectionProvider reflectionProvider) {
		this.reflectionProvider = reflectionProvider;
	}

	/**
	 * 设置用于读写器的 XStream {@link HierarchicalStreamDriver}。
	 * <p>自 Spring 4.0 起，此流驱动程序也会传给 {@link XStream} 构造函数，
	 * 因此流相关的原生 API 方法本身也会使用它。
	 */
	public void setStreamDriver(HierarchicalStreamDriver streamDriver) {
		this.streamDriver = streamDriver;
		this.defaultDriver = streamDriver;
	}

	private HierarchicalStreamDriver getDefaultDriver() {
		if (this.defaultDriver == null) {
			this.defaultDriver = new XppDriver();
		}
		return this.defaultDriver;
	}

	/**
	 * 设置要使用的自定义 XStream {@link Mapper}。
	 * @since 4.0
	 */
	public void setMapper(Mapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 设置一个或多个自定义 XStream {@link MapperWrapper} 类。
	 * 每个类都需要有一个仅接收一个 {@link Mapper} 或 {@link MapperWrapper}
	 * 类型参数的构造函数。
	 * @since 4.0
	 */
	@SuppressWarnings("unchecked")
	public void setMapperWrappers(Class<? extends MapperWrapper>... mapperWrappers) {
		this.mapperWrappers = mapperWrappers;
	}

	/**
	 * 设置要使用的自定义 XStream {@link ConverterLookup}。
	 * 如果给定的引用同时实现了 {@link ConverterRegistry}，也会将其用作 {@link ConverterRegistry}。
	 * @since 4.0
	 * @see DefaultConverterLookup
	 */
	public void setConverterLookup(ConverterLookup converterLookup) {
		this.converterLookup = converterLookup;
		if (converterLookup instanceof ConverterRegistry) {
			this.converterRegistry = (ConverterRegistry) converterLookup;
		}
	}

	/**
	 * 设置要使用的自定义 XStream {@link ConverterRegistry}。
	 * @since 4.0
	 * @see #setConverterLookup
	 * @see DefaultConverterLookup
	 */
	public void setConverterRegistry(ConverterRegistry converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

	/**
	 * 设置要注册到 {@code XStream} 实例的 {@code Converters} 或 {@code SingleValueConverters}。
	 * @see Converter
	 * @see SingleValueConverter
	 */
	public void setConverters(ConverterMatcher... converters) {
		this.converters = converters;
	}

	/**
	 * 设置 XStream 类型权限，例如
	 * {@link com.thoughtworks.xstream.security.AnyTypePermission}、
	 * {@link com.thoughtworks.xstream.security.ExplicitTypePermission} 等，
	 * 作为覆盖 {@link #customizeXStream} 方法的替代方案。
	 * <p>注意：自 XStream 1.4.18 起，默认类型权限被限制为知名的核心 JDK 类型。
	 * 对于任何自定义类型，都需要注册显式的类型权限。
	 * @since 5.2.17
	 */
	public void setTypePermissions(TypePermission... typePermissions) {
		this.typePermissions = typePermissions;
	}

	/**
	 * 设置要使用的自定义 XStream {@link MarshallingStrategy}。
	 * @since 4.0
	 */
	public void setMarshallingStrategy(MarshallingStrategy marshallingStrategy) {
		this.marshallingStrategy = marshallingStrategy;
	}

	/**
	 * 设置要使用的 XStream 模式。
	 * @see XStream#ID_REFERENCES
	 * @see XStream#NO_REFERENCES
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * 设置别名/类型映射，由映射到类的字符串别名组成。
	 * <p>键为别名；值为 {@code Class} 实例或 String 类名。
	 * @see XStream#alias(String, Class)
	 */
	public void setAliases(Map<String, ?> aliases) {
		this.aliases = aliases;
	}

	/**
	 * 设置<em>按类型别名</em>映射，由映射到类的字符串别名组成。
	 * <p>任何可赋值给此类型的类都将被赋予相同的别名。
	 * 键为别名；值为 {@code Class} 实例或 String 类名。
	 * @see XStream#aliasType(String, Class)
	 */
	public void setAliasesByType(Map<String, ?> aliasesByType) {
		this.aliasesByType = aliasesByType;
	}

	/**
	 * 设置字段别名/类型映射，由字段名组成。
	 * @see XStream#aliasField(String, Class, String)
	 */
	public void setFieldAliases(Map<String, String> fieldAliases) {
		this.fieldAliases = fieldAliases;
	}

	/**
	 * 设置要使用 XML 属性的类型。
	 * @see XStream#useAttributeFor(Class)
	 */
	public void setUseAttributeForTypes(Class<?>... useAttributeForTypes) {
		this.useAttributeForTypes = useAttributeForTypes;
	}

	/**
	 * 设置要使用 XML 属性的类型。给定的映射可以包含
	 * {@code <String, Class>} 键值对，此时会调用
	 * {@link XStream#useAttributeFor(String, Class)}。
	 * 或者，映射也可以包含 {@code <Class, String>}
	 * 或 {@code <Class, List<String>>} 键值对，此时会调用
	 * {@link XStream#useAttributeFor(Class, String)}。
	 */
	public void setUseAttributeFor(Map<?, ?> useAttributeFor) {
		this.useAttributeFor = useAttributeFor;
	}

	/**
	 * 指定隐式集合字段，形式为将 {@code Class} 实例映射到逗号分隔的集合字段名的 Map。
	 * @see XStream#addImplicitCollection(Class, String)
	 */
	public void setImplicitCollections(Map<Class<?>, String> implicitCollections) {
		this.implicitCollections = implicitCollections;
	}

	/**
	 * 指定要忽略的字段，形式为将 {@code Class} 实例映射到逗号分隔的字段名的 Map。
	 * @see XStream#omitField(Class, String)
	 */
	public void setOmittedFields(Map<Class<?>, String> omittedFields) {
		this.omittedFields = omittedFields;
	}

	/**
	 * 设置带注解的类，这些类的别名将从类级注解元数据中读取。
	 * @see XStream#processAnnotations(Class[])
	 */
	public void setAnnotatedClasses(Class<?>... annotatedClasses) {
		this.annotatedClasses = annotatedClasses;
	}

	/**
	 * 激活 XStream 的自动检测模式。
	 * <p><b>注意</b>：自动检测意味着 XStream 实例会在处理 XML 流的同时进行配置，
	 * 因此会引入潜在的并发问题。
	 * @see XStream#autodetectAnnotations(boolean)
	 */
	public void setAutodetectAnnotations(boolean autodetectAnnotations) {
		this.autodetectAnnotations = autodetectAnnotations;
	}

	/**
	 * 设置用于流访问的编码。
	 * @see #DEFAULT_ENCODING
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Override
	protected String getDefaultEncoding() {
		return this.encoding;
	}

	/**
	 * 设置要使用的自定义 XStream {@link NameCoder}。
	 * 默认为 {@link XmlFriendlyNameCoder}。
	 * @since 4.0.4
	 */
	public void setNameCoder(NameCoder nameCoder) {
		this.nameCoder = nameCoder;
	}

	/**
	 * 设置此 marshaller 所支持的类。
	 * <p>如果此属性为空（默认值），则支持所有类。
	 * @see #supports(Class)
	 */
	public void setSupportedClasses(Class<?>... supportedClasses) {
		this.supportedClasses = supportedClasses;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public void afterPropertiesSet() {
		// no-op due to use of SingletonSupplier for the XStream field
	}

	/**
	 * 构建此 marshaller 使用的原生 XStream 委托，依次委托给
	 * {@link #constructXStream}、{@link #configureXStream}
	 * 和 {@link #customizeXStream}。
	 */
	protected XStream buildXStream() {
		XStream xstream = constructXStream();
		configureXStream(xstream);
		customizeXStream(xstream);
		return xstream;
	}

	/**
	 * 构造一个 XStream 实例，可以使用标准构造函数之一，或创建自定义子类。
	 * @return {@code XStream} 实例
	 */
	protected XStream constructXStream() {
		return new XStream(this.reflectionProvider, getDefaultDriver(), new ClassLoaderReference(this.beanClassLoader),
				this.mapper, this.converterLookup, this.converterRegistry) {
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				MapperWrapper mapperToWrap = next;
				if (mapperWrappers != null) {
					for (Class<? extends MapperWrapper> mapperWrapper : mapperWrappers) {
						Constructor<? extends MapperWrapper> ctor;
						try {
							ctor = mapperWrapper.getConstructor(Mapper.class);
						}
						catch (NoSuchMethodException ex) {
							try {
								ctor = mapperWrapper.getConstructor(MapperWrapper.class);
							}
							catch (NoSuchMethodException ex2) {
								throw new IllegalStateException("No appropriate MapperWrapper constructor found: " + mapperWrapper);
							}
						}
						try {
							mapperToWrap = ctor.newInstance(mapperToWrap);
						}
						catch (Throwable ex) {
							throw new IllegalStateException("Failed to construct MapperWrapper: " + mapperWrapper);
						}
					}
				}
				return mapperToWrap;
			}
		};
	}

	/**
	 * 使用此 marshaller 的 bean 属性配置 XStream 实例。
	 * @param xstream {@code XStream} 实例
	 */
	protected void configureXStream(XStream xstream) {
		if (this.converters != null) {
			for (int i = 0; i < this.converters.length; i++) {
				if (this.converters[i] instanceof Converter) {
					xstream.registerConverter((Converter) this.converters[i], i);
				}
				else if (this.converters[i] instanceof SingleValueConverter) {
					xstream.registerConverter((SingleValueConverter) this.converters[i], i);
				}
				else {
					throw new IllegalArgumentException("Invalid ConverterMatcher [" + this.converters[i] + "]");
				}
			}
		}

		if (this.typePermissions != null) {
			for (TypePermission permission : this.typePermissions) {
				xstream.addPermission(permission);
			}
		}

		if (this.marshallingStrategy != null) {
			xstream.setMarshallingStrategy(this.marshallingStrategy);
		}
		if (this.mode != null) {
			xstream.setMode(this.mode);
		}

		try {
			if (this.aliases != null) {
				Map<String, Class<?>> classMap = toClassMap(this.aliases);
				classMap.forEach(xstream::alias);
			}
			if (this.aliasesByType != null) {
				Map<String, Class<?>> classMap = toClassMap(this.aliasesByType);
				classMap.forEach(xstream::aliasType);
			}
			if (this.fieldAliases != null) {
				for (Map.Entry<String, String> entry : this.fieldAliases.entrySet()) {
					String alias = entry.getValue();
					String field = entry.getKey();
					int idx = field.lastIndexOf('.');
					if (idx != -1) {
						String className = field.substring(0, idx);
						Class<?> clazz = ClassUtils.forName(className, this.beanClassLoader);
						String fieldName = field.substring(idx + 1);
						xstream.aliasField(alias, clazz, fieldName);
					}
					else {
						throw new IllegalArgumentException("Field name [" + field + "] does not contain '.'");
					}
				}
			}
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load specified alias class", ex);
		}

		if (this.useAttributeForTypes != null) {
			for (Class<?> type : this.useAttributeForTypes) {
				xstream.useAttributeFor(type);
			}
		}
		if (this.useAttributeFor != null) {
			for (Map.Entry<?, ?> entry : this.useAttributeFor.entrySet()) {
				if (entry.getKey() instanceof String) {
					if (entry.getValue() instanceof Class) {
						xstream.useAttributeFor((String) entry.getKey(), (Class<?>) entry.getValue());
					}
					else {
						throw new IllegalArgumentException(
								"'useAttributesFor' takes Map<String, Class> when using a map key of type String");
					}
				}
				else if (entry.getKey() instanceof Class) {
					Class<?> key = (Class<?>) entry.getKey();
					if (entry.getValue() instanceof String) {
						xstream.useAttributeFor(key, (String) entry.getValue());
					}
					else if (entry.getValue() instanceof List) {
						@SuppressWarnings("unchecked")
						List<Object> listValue = (List<Object>) entry.getValue();
						for (Object element : listValue) {
							if (element instanceof String) {
								xstream.useAttributeFor(key, (String) element);
							}
						}
					}
					else {
						throw new IllegalArgumentException("'useAttributesFor' property takes either Map<Class, String> " +
								"or Map<Class, List<String>> when using a map key of type Class");
					}
				}
				else {
					throw new IllegalArgumentException(
							"'useAttributesFor' property takes either a map key of type String or Class");
				}
			}
		}

		if (this.implicitCollections != null) {
			this.implicitCollections.forEach((key, fields) -> {
				String[] collectionFields = StringUtils.commaDelimitedListToStringArray(fields);
				for (String collectionField : collectionFields) {
					xstream.addImplicitCollection(key, collectionField);
				}
			});
		}
		if (this.omittedFields != null) {
			this.omittedFields.forEach((key, value) -> {
				String[] fields = StringUtils.commaDelimitedListToStringArray(value);
				for (String field : fields) {
					xstream.omitField(key, field);
				}
			});
		}

		if (this.annotatedClasses != null) {
			xstream.processAnnotations(this.annotatedClasses);
		}
		if (this.autodetectAnnotations) {
			xstream.autodetectAnnotations(true);
		}
	}

	private Map<String, Class<?>> toClassMap(Map<String, ?> map) throws ClassNotFoundException {
		Map<String, Class<?>> result = CollectionUtils.newLinkedHashMap(map.size());
		for (Map.Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			Class<?> type;
			if (value instanceof Class) {
				type = (Class<?>) value;
			}
			else if (value instanceof String) {
				String className = (String) value;
				type = ClassUtils.forName(className, this.beanClassLoader);
			}
			else {
				throw new IllegalArgumentException("Unknown value [" + value + "] - expected String or Class");
			}
			result.put(key, type);
		}
		return result;
	}

	/**
	 * 用于自定义给定 {@link XStream} 的模板。
	 * <p>默认实现为空。
	 * @param xstream {@code XStream} 实例
	 */
	protected void customizeXStream(XStream xstream) {
	}

	/**
	 * 返回此 marshaller 所使用的原生 XStream 委托。
	 * <p><b>注意：此方法自 Spring 4.0 起已被标记为 final。</b>
	 * 它可用于访问完全配置好的 XStream 以进行 marshalling，
	 * 但不再用于配置目的。
	 * <p>自 Spring Framework 5.1.16 起，此方法返回的 {@link XStream} 实例的创建
	 * 是线程安全的。
	 */
	public final XStream getXStream() {
		return this.xstream.obtain();
	}


	@Override
	public boolean supports(Class<?> clazz) {
		if (ObjectUtils.isEmpty(this.supportedClasses)) {
			return true;
		}
		else {
			for (Class<?> supportedClass : this.supportedClasses) {
				if (supportedClass.isAssignableFrom(clazz)) {
					return true;
				}
			}
			return false;
		}
	}


	// Marshalling

	@Override
	protected void marshalDomNode(Object graph, Node node) throws XmlMappingException {
		HierarchicalStreamWriter streamWriter;
		if (node instanceof Document) {
			streamWriter = new DomWriter((Document) node, this.nameCoder);
		}
		else if (node instanceof Element) {
			streamWriter = new DomWriter((Element) node, node.getOwnerDocument(), this.nameCoder);
		}
		else {
			throw new IllegalArgumentException("DOMResult contains neither Document nor Element");
		}
		doMarshal(graph, streamWriter, null);
	}

	@Override
	protected void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter) throws XmlMappingException {
		ContentHandler contentHandler = StaxUtils.createContentHandler(eventWriter);
		LexicalHandler lexicalHandler = null;
		if (contentHandler instanceof LexicalHandler) {
			lexicalHandler = (LexicalHandler) contentHandler;
		}
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	@Override
	protected void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter) throws XmlMappingException {
		try {
			StaxWriter writer;
			if (this.streamDriver instanceof StaxDriver) {
				writer = ((StaxDriver) this.streamDriver).createStaxWriter(streamWriter);
			}
			else {
				writer = new StaxWriter(new QNameMap(), streamWriter, this.nameCoder);
			}
			doMarshal(graph, writer, null);
		}
		catch (XMLStreamException ex) {
			throw convertXStreamException(ex, true);
		}
	}

	@Override
	protected void marshalSaxHandlers(Object graph, ContentHandler contentHandler, @Nullable LexicalHandler lexicalHandler)
			throws XmlMappingException {

		SaxWriter saxWriter = new SaxWriter(this.nameCoder);
		saxWriter.setContentHandler(contentHandler);
		doMarshal(graph, saxWriter, null);
	}

	@Override
	public void marshalOutputStream(Object graph, OutputStream outputStream) throws XmlMappingException, IOException {
		marshalOutputStream(graph, outputStream, null);
	}

	public void marshalOutputStream(Object graph, OutputStream outputStream, @Nullable DataHolder dataHolder)
			throws XmlMappingException, IOException {

		if (this.streamDriver != null) {
			doMarshal(graph, this.streamDriver.createWriter(outputStream), dataHolder);
		}
		else {
			marshalWriter(graph, new OutputStreamWriter(outputStream, this.encoding), dataHolder);
		}
	}

	@Override
	public void marshalWriter(Object graph, Writer writer) throws XmlMappingException, IOException {
		marshalWriter(graph, writer, null);
	}

	public void marshalWriter(Object graph, Writer writer, @Nullable DataHolder dataHolder)
			throws XmlMappingException, IOException {

		if (this.streamDriver != null) {
			doMarshal(graph, this.streamDriver.createWriter(writer), dataHolder);
		}
		else {
			doMarshal(graph, new CompactWriter(writer), dataHolder);
		}
	}

	/**
	 * 将给定对象图 marshalling 到给定的 XStream HierarchicalStreamWriter。
	 * 使用 {@link #convertXStreamException} 转换异常。
	 */
	private void doMarshal(Object graph, HierarchicalStreamWriter streamWriter, @Nullable DataHolder dataHolder) {
		try {
			getXStream().marshal(graph, streamWriter, dataHolder);
		}
		catch (Exception ex) {
			throw convertXStreamException(ex, true);
		}
		finally {
			try {
				streamWriter.flush();
			}
			catch (Exception ex) {
				logger.debug("Could not flush HierarchicalStreamWriter", ex);
			}
		}
	}


	// Unmarshalling

	@Override
	protected Object unmarshalStreamSource(StreamSource streamSource) throws XmlMappingException, IOException {
		if (streamSource.getInputStream() != null) {
			return unmarshalInputStream(streamSource.getInputStream());
		}
		else if (streamSource.getReader() != null) {
			return unmarshalReader(streamSource.getReader());
		}
		else {
			throw new IllegalArgumentException("StreamSource contains neither InputStream nor Reader");
		}
	}

	@Override
	protected Object unmarshalDomNode(Node node) throws XmlMappingException {
		HierarchicalStreamReader streamReader;
		if (node instanceof Document) {
			streamReader = new DomReader((Document) node, this.nameCoder);
		}
		else if (node instanceof Element) {
			streamReader = new DomReader((Element) node, this.nameCoder);
		}
		else {
			throw new IllegalArgumentException("DOMSource contains neither Document nor Element");
		}
		return doUnmarshal(streamReader, null);
	}

	@Override
	protected Object unmarshalXmlEventReader(XMLEventReader eventReader) throws XmlMappingException {
		try {
			XMLStreamReader streamReader = StaxUtils.createEventStreamReader(eventReader);
			return unmarshalXmlStreamReader(streamReader);
		}
		catch (XMLStreamException ex) {
			throw convertXStreamException(ex, false);
		}
	}

	@Override
	protected Object unmarshalXmlStreamReader(XMLStreamReader streamReader) throws XmlMappingException {
		return doUnmarshal(new StaxReader(new QNameMap(), streamReader, this.nameCoder), null);
	}

	@Override
	protected Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException {

		throw new UnsupportedOperationException(
				"XStreamMarshaller does not support unmarshalling using SAX XMLReaders");
	}

	@Override
	public Object unmarshalInputStream(InputStream inputStream) throws XmlMappingException, IOException {
		return unmarshalInputStream(inputStream, null);
	}

	public Object unmarshalInputStream(InputStream inputStream, @Nullable DataHolder dataHolder) throws XmlMappingException, IOException {
		if (this.streamDriver != null) {
			return doUnmarshal(this.streamDriver.createReader(inputStream), dataHolder);
		}
		else {
			return unmarshalReader(new InputStreamReader(inputStream, this.encoding), dataHolder);
		}
	}

	@Override
	public Object unmarshalReader(Reader reader) throws XmlMappingException, IOException {
		return unmarshalReader(reader, null);
	}

	public Object unmarshalReader(Reader reader, @Nullable DataHolder dataHolder) throws XmlMappingException, IOException {
		return doUnmarshal(getDefaultDriver().createReader(reader), dataHolder);
	}

	/**
	 * 将给定对象图 unmarshalling 到给定的 XStream HierarchicalStreamWriter。
	 * 使用 {@link #convertXStreamException} 转换异常。
	 */
	private Object doUnmarshal(HierarchicalStreamReader streamReader, @Nullable DataHolder dataHolder) {
		try {
			return getXStream().unmarshal(streamReader, null, dataHolder);
		}
		catch (Exception ex) {
			throw convertXStreamException(ex, false);
		}
	}


	/**
	 * 将给定的 XStream 异常转换为 {@code org.springframework.oxm} 异常层次结构中
	 * 相应的异常。
	 * <p>由于 XStream 自身的异常层次结构并不区分 marshalling 与 unmarshalling，
	 * 因此使用一个布尔标志来指示该异常是发生在 marshalling 还是 unmarshalling 期间。
	 * @param ex 发生的 XStream 异常
	 * @param marshalling 指示异常是在编组期间发生（{@code true}），
	 * 还是在解组期间发生（{@code false}）
	 * @return 对应的 {@code XmlMappingException}
	 */
	protected XmlMappingException convertXStreamException(Exception ex, boolean marshalling) {
		if (ex instanceof StreamException || ex instanceof CannotResolveClassException ||
				ex instanceof ForbiddenClassException || ex instanceof ConversionException) {
			if (marshalling) {
				return new MarshallingFailureException("XStream marshalling exception",  ex);
			}
			else {
				return new UnmarshallingFailureException("XStream unmarshalling exception", ex);
			}
		}
		else {
			// fallback
			return new UncategorizedMappingException("Unknown XStream exception", ex);
		}
	}

}
