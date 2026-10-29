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

package org.springframework.oxm.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.UnmarshallingFailureException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.util.xml.StaxUtils;

/**
 * {@code Marshaller} 和 {@code Unmarshaller} 接口的抽象实现。
 * 此实现会检查给定的 {@code Source} 或 {@code Result}，并将后续处理
 * 委托给可覆盖的模板方法。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractMarshaller implements Marshaller, Unmarshaller {

	private static final EntityResolver NO_OP_ENTITY_RESOLVER =
			(publicId, systemId) -> new InputSource(new StringReader(""));

	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean supportDtd = false;

	private boolean processExternalEntities = false;

	@Nullable
	private DocumentBuilderFactory documentBuilderFactory;

	private final Object documentBuilderFactoryMonitor = new Object();


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
	 * @see #createXmlReader()
	 */
	public boolean isProcessExternalEntities() {
		return this.processExternalEntities;
	}


	/**
	 * 使用此 marshaller 的 {@link DocumentBuilderFactory} 构建一个新的 {@link Document}，
	 * 作为 DOM 节点的占位符。
	 * @see #createDocumentBuilderFactory()
	 * @see #createDocumentBuilder(DocumentBuilderFactory)
	 */
	protected Document buildDocument() {
		try {
			DocumentBuilder documentBuilder;
			synchronized (this.documentBuilderFactoryMonitor) {
				if (this.documentBuilderFactory == null) {
					this.documentBuilderFactory = createDocumentBuilderFactory();
				}
				documentBuilder = createDocumentBuilder(this.documentBuilderFactory);
			}
			return documentBuilder.newDocument();
		}
		catch (ParserConfigurationException ex) {
			throw new UnmarshallingFailureException("Could not create document placeholder: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 创建一个 {@code DocumentBuilder}，此 marshaller 在传入空的 {@code DOMSource} 时
	 * 将用它来创建 DOM 文档。
	 * <p>生成的 {@code DocumentBuilderFactory} 会被缓存，因此此方法只会被调用一次。
	 * @return the DocumentBuilderFactory
	 * @throws ParserConfigurationException if thrown by JAXP methods
	 */
	protected DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
		factory.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
		return factory;
	}

	/**
	 * 创建一个 {@code DocumentBuilder}，此 marshaller 在传入空的 {@code DOMSource} 时
	 * 将用它来创建 DOM 文档。
	 * <p>可在子类中覆盖此方法，以对 builder 进行进一步的初始化。
	 * @param factory the {@code DocumentBuilderFactory} that the DocumentBuilder should be created with
	 * @return the {@code DocumentBuilder}
	 * @throws ParserConfigurationException if thrown by JAXP methods
	 */
	protected DocumentBuilder createDocumentBuilder(DocumentBuilderFactory factory)
			throws ParserConfigurationException {

		DocumentBuilder documentBuilder = factory.newDocumentBuilder();
		if (!isProcessExternalEntities()) {
			documentBuilder.setEntityResolver(NO_OP_ENTITY_RESOLVER);
		}
		return documentBuilder;
	}

	/**
	 * 创建一个 {@code XMLReader}，此 marshaller 在传入空的 {@code SAXSource} 时将使用它。
	 * @return the XMLReader
	 * @throws SAXException if thrown by JAXP methods
	 */
	@SuppressWarnings("deprecation")  // on JDK 9
	protected XMLReader createXmlReader() throws SAXException {
		XMLReader xmlReader = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
		xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", !isSupportDtd());
		xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", isProcessExternalEntities());
		if (!isProcessExternalEntities()) {
			xmlReader.setEntityResolver(NO_OP_ENTITY_RESOLVER);
		}
		return xmlReader;
	}

	/**
	 * 确定从字节流进行 marshalling 或 unmarshalling 时使用的默认编码，若无则返回 {@code null}。
	 * <p>默认实现返回 {@code null}。
	 */
	@Nullable
	protected String getDefaultEncoding() {
		return null;
	}


	// Marshalling

	/**
	 * 将给定根对象的对象图 marshal 到所提供的 {@code javax.xml.transform.Result} 中。
	 * <p>此实现会检查给定的 result，并调用 {@code marshalDomResult}、
	 * {@code marshalSaxResult} 或 {@code marshalStreamResult}。
	 * @param graph the root of the object graph to marshal
	 * @param result the result to marshal to
	 * @throws IOException if an I/O exception occurs
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if {@code result} if neither a {@code DOMResult},
	 * a {@code SAXResult}, nor a {@code StreamResult}
	 * @see #marshalDomResult(Object, javax.xml.transform.dom.DOMResult)
	 * @see #marshalSaxResult(Object, javax.xml.transform.sax.SAXResult)
	 * @see #marshalStreamResult(Object, javax.xml.transform.stream.StreamResult)
	 */
	@Override
	public final void marshal(Object graph, Result result) throws IOException, XmlMappingException {
		if (result instanceof DOMResult) {
			marshalDomResult(graph, (DOMResult) result);
		}
		else if (StaxUtils.isStaxResult(result)) {
			marshalStaxResult(graph, result);
		}
		else if (result instanceof SAXResult) {
			marshalSaxResult(graph, (SAXResult) result);
		}
		else if (result instanceof StreamResult) {
			marshalStreamResult(graph, (StreamResult) result);
		}
		else {
			throw new IllegalArgumentException("Unknown Result type: " + result.getClass());
		}
	}

	/**
	 * 用于处理 {@code DOMResult} 的模板方法。
	 * <p>此实现委托给 {@code marshalDomNode}。
	 * @param graph the root of the object graph to marshal
	 * @param domResult the {@code DOMResult}
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if the {@code domResult} is empty
	 * @see #marshalDomNode(Object, org.w3c.dom.Node)
	 */
	protected void marshalDomResult(Object graph, DOMResult domResult) throws XmlMappingException {
		if (domResult.getNode() == null) {
			domResult.setNode(buildDocument());
		}
		marshalDomNode(graph, domResult.getNode());
	}

	/**
	 * 用于处理 {@code StaxResult} 的模板方法。
	 * <p>根据 {@code StaxResult} 中包含的内容，此实现委托给
	 * {@code marshalXMLSteamWriter} 或 {@code marshalXMLEventConsumer}。
	 * @param graph the root of the object graph to marshal
	 * @param staxResult a JAXP 1.4 {@link StAXSource}
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if the {@code domResult} is empty
	 * @see #marshalDomNode(Object, org.w3c.dom.Node)
	 */
	protected void marshalStaxResult(Object graph, Result staxResult) throws XmlMappingException {
		XMLStreamWriter streamWriter = StaxUtils.getXMLStreamWriter(staxResult);
		if (streamWriter != null) {
			marshalXmlStreamWriter(graph, streamWriter);
		}
		else {
			XMLEventWriter eventWriter = StaxUtils.getXMLEventWriter(staxResult);
			if (eventWriter != null) {
				marshalXmlEventWriter(graph, eventWriter);
			}
			else {
				throw new IllegalArgumentException("StaxResult contains neither XMLStreamWriter nor XMLEventConsumer");
			}
		}
	}

	/**
	 * 用于处理 {@code SAXResult} 的模板方法。
	 * <p>此实现委托给 {@code marshalSaxHandlers}。
	 * @param graph the root of the object graph to marshal
	 * @param saxResult the {@code SAXResult}
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @see #marshalSaxHandlers(Object, org.xml.sax.ContentHandler, org.xml.sax.ext.LexicalHandler)
	 */
	protected void marshalSaxResult(Object graph, SAXResult saxResult) throws XmlMappingException {
		ContentHandler contentHandler = saxResult.getHandler();
		Assert.notNull(contentHandler, "ContentHandler not set on SAXResult");
		LexicalHandler lexicalHandler = saxResult.getLexicalHandler();
		marshalSaxHandlers(graph, contentHandler, lexicalHandler);
	}

	/**
	 * 用于处理 {@code StreamResult} 的模板方法。
	 * <p>根据 {@code StreamResult} 中包含的内容，此实现委托给
	 * {@code marshalOutputStream} 或 {@code marshalWriter}
	 * @param graph the root of the object graph to marshal
	 * @param streamResult the {@code StreamResult}
	 * @throws IOException if an I/O Exception occurs
	 * @throws XmlMappingException if the given object cannot be marshalled to the result
	 * @throws IllegalArgumentException if {@code streamResult} does neither
	 * contain an {@code OutputStream} nor a {@code Writer}
	 */
	protected void marshalStreamResult(Object graph, StreamResult streamResult)
			throws XmlMappingException, IOException {

		if (streamResult.getOutputStream() != null) {
			marshalOutputStream(graph, streamResult.getOutputStream());
		}
		else if (streamResult.getWriter() != null) {
			marshalWriter(graph, streamResult.getWriter());
		}
		else {
			throw new IllegalArgumentException("StreamResult contains neither OutputStream nor Writer");
		}
	}


	// Unmarshalling

	/**
	 * 将所提供的 {@code javax.xml.transform.Source} unmarshal 为对象图。
	 * <p>此实现会检查给定的 source，并调用 {@code unmarshalDomSource}、
	 * {@code unmarshalSaxSource} 或 {@code unmarshalStreamSource}。
	 * @param source the source to marshal from
	 * @return the object graph
	 * @throws IOException if an I/O Exception occurs
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IllegalArgumentException if {@code source} is neither a {@code DOMSource},
	 * a {@code SAXSource}, nor a {@code StreamSource}
	 * @see #unmarshalDomSource(javax.xml.transform.dom.DOMSource)
	 * @see #unmarshalSaxSource(javax.xml.transform.sax.SAXSource)
	 * @see #unmarshalStreamSource(javax.xml.transform.stream.StreamSource)
	 */
	@Override
	public final Object unmarshal(Source source) throws IOException, XmlMappingException {
		if (source instanceof DOMSource) {
			return unmarshalDomSource((DOMSource) source);
		}
		else if (StaxUtils.isStaxSource(source)) {
			return unmarshalStaxSource(source);
		}
		else if (source instanceof SAXSource) {
			return unmarshalSaxSource((SAXSource) source);
		}
		else if (source instanceof StreamSource) {
			return unmarshalStreamSource((StreamSource) source);
		}
		else {
			throw new IllegalArgumentException("Unknown Source type: " + source.getClass());
		}
	}

	/**
	 * 用于处理 {@code DOMSource} 的模板方法。
	 * <p>此实现委托给 {@code unmarshalDomNode}。
	 * 如果给定的 source 为空，则会创建一个空的 source {@code Document} 作为占位符。
	 * @param domSource the {@code DOMSource}
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IllegalArgumentException if the {@code domSource} is empty
	 * @see #unmarshalDomNode(org.w3c.dom.Node)
	 */
	protected Object unmarshalDomSource(DOMSource domSource) throws XmlMappingException {
		if (domSource.getNode() == null) {
			domSource.setNode(buildDocument());
		}
		try {
			return unmarshalDomNode(domSource.getNode());
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling. " +
						"This can happen on JDK 1.6 due to the presence of DTD " +
						"declarations, which are disabled.", ex);
			}
			throw ex;
		}
	}

	/**
	 * 用于处理 {@code StaxSource} 的模板方法。
	 * <p>此实现委托给 {@code unmarshalXmlStreamReader} 或
	 * {@code unmarshalXmlEventReader}。
	 * @param staxSource the {@code StaxSource}
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 */
	protected Object unmarshalStaxSource(Source staxSource) throws XmlMappingException {
		XMLStreamReader streamReader = StaxUtils.getXMLStreamReader(staxSource);
		if (streamReader != null) {
			return unmarshalXmlStreamReader(streamReader);
		}
		else {
			XMLEventReader eventReader = StaxUtils.getXMLEventReader(staxSource);
			if (eventReader != null) {
				return unmarshalXmlEventReader(eventReader);
			}
			else {
				throw new IllegalArgumentException("StaxSource contains neither XMLStreamReader nor XMLEventReader");
			}
		}
	}

	/**
	 * 用于处理 {@code SAXSource} 的模板方法。
	 * <p>此实现委托给 {@code unmarshalSaxReader}。
	 * @param saxSource the {@code SAXSource}
	 * @return the object graph
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 * @throws IOException if an I/O Exception occurs
	 * @see #unmarshalSaxReader(org.xml.sax.XMLReader, org.xml.sax.InputSource)
	 */
	protected Object unmarshalSaxSource(SAXSource saxSource) throws XmlMappingException, IOException {
		if (saxSource.getXMLReader() == null) {
			try {
				saxSource.setXMLReader(createXmlReader());
			}
			catch (SAXException ex) {
				throw new UnmarshallingFailureException("Could not create XMLReader for SAXSource", ex);
			}
		}
		if (saxSource.getInputSource() == null) {
			saxSource.setInputSource(new InputSource());
		}
		try {
			return unmarshalSaxReader(saxSource.getXMLReader(), saxSource.getInputSource());
		}
		catch (NullPointerException ex) {
			if (!isSupportDtd()) {
				throw new UnmarshallingFailureException("NPE while unmarshalling. " +
						"This can happen on JDK 1.6 due to the presence of DTD " +
						"declarations, which are disabled.");
			}
			throw ex;
		}
	}

	/**
	 * 用于处理 {@code StreamSource} 的模板方法。
	 * <p>此实现委托给 {@code unmarshalInputStream} 或 {@code unmarshalReader}。
	 * @param streamSource the {@code StreamSource}
	 * @return the object graph
	 * @throws IOException if an I/O exception occurs
	 * @throws XmlMappingException if the given source cannot be mapped to an object
	 */
	protected Object unmarshalStreamSource(StreamSource streamSource) throws XmlMappingException, IOException {
		if (streamSource.getInputStream() != null) {
			if (isProcessExternalEntities() && isSupportDtd()) {
				return unmarshalInputStream(streamSource.getInputStream());
			}
			else {
				InputSource inputSource = new InputSource(streamSource.getInputStream());
				inputSource.setEncoding(getDefaultEncoding());
				return unmarshalSaxSource(new SAXSource(inputSource));
			}
		}
		else if (streamSource.getReader() != null) {
			if (isProcessExternalEntities() && isSupportDtd()) {
				return unmarshalReader(streamSource.getReader());
			}
			else {
				return unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getReader())));
			}
		}
		else {
			return unmarshalSaxSource(new SAXSource(new InputSource(streamSource.getSystemId())));
		}
	}


	// Abstract template methods

	/**
	 * 用于将给定对象图 marshalling 到 DOM {@code Node} 的抽象模板方法。
	 * <p>在实践中，{@code node} 是 {@code Document} 节点、{@code DocumentFragment} 节点
	 * 或 {@code Element} 节点。换言之，是一个可以接受子节点的节点。
	 * @param graph the root of the object graph to marshal
	 * @param node the DOM node that will contain the result tree
	 * @throws XmlMappingException if the given object cannot be marshalled to the DOM node
	 * @see org.w3c.dom.Document
	 * @see org.w3c.dom.DocumentFragment
	 * @see org.w3c.dom.Element
	 */
	protected abstract void marshalDomNode(Object graph, Node node)
			throws XmlMappingException;

	/**
	 * 用于将给定对象 marshalling 到 StAX {@code XMLEventWriter} 的抽象模板方法。
	 * @param graph the root of the object graph to marshal
	 * @param eventWriter the {@code XMLEventWriter} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the DOM node
	 */
	protected abstract void marshalXmlEventWriter(Object graph, XMLEventWriter eventWriter)
			throws XmlMappingException;

	/**
	 * 用于将给定对象 marshalling 到 StAX {@code XMLStreamWriter} 的抽象模板方法。
	 * @param graph the root of the object graph to marshal
	 * @param streamWriter the {@code XMLStreamWriter} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the DOM node
	 */
	protected abstract void marshalXmlStreamWriter(Object graph, XMLStreamWriter streamWriter)
			throws XmlMappingException;

	/**
	 * 用于将给定对象图 marshalling 到 SAX {@code ContentHandler} 的抽象模板方法。
	 * @param graph the root of the object graph to marshal
	 * @param contentHandler the SAX {@code ContentHandler}
	 * @param lexicalHandler the SAX2 {@code LexicalHandler}. Can be {@code null}.
	 * @throws XmlMappingException if the given object cannot be marshalled to the handlers
	 */
	protected abstract void marshalSaxHandlers(
			Object graph, ContentHandler contentHandler, @Nullable LexicalHandler lexicalHandler)
			throws XmlMappingException;

	/**
	 * 用于将给定对象图 marshalling 到 {@code OutputStream} 的抽象模板方法。
	 * @param graph the root of the object graph to marshal
	 * @param outputStream the {@code OutputStream} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the writer
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract void marshalOutputStream(Object graph, OutputStream outputStream)
			throws XmlMappingException, IOException;

	/**
	 * 用于将给定对象图 marshalling 到 {@code Writer} 的抽象模板方法。
	 * @param graph the root of the object graph to marshal
	 * @param writer the {@code Writer} to write to
	 * @throws XmlMappingException if the given object cannot be marshalled to the writer
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract void marshalWriter(Object graph, Writer writer)
			throws XmlMappingException, IOException;

	/**
	 * 用于从给定 DOM {@code Node} 进行 unmarshalling 的抽象模板方法。
	 * @param node the DOM node that contains the objects to be unmarshalled
	 * @return the object graph
	 * @throws XmlMappingException if the given DOM node cannot be mapped to an object
	 */
	protected abstract Object unmarshalDomNode(Node node) throws XmlMappingException;

	/**
	 * 用于从给定 Stax {@code XMLEventReader} 进行 unmarshalling 的抽象模板方法。
	 * @param eventReader the {@code XMLEventReader} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given event reader cannot be converted to an object
	 */
	protected abstract Object unmarshalXmlEventReader(XMLEventReader eventReader)
			throws XmlMappingException;

	/**
	 * 用于从给定 Stax {@code XMLStreamReader} 进行 unmarshalling 的抽象模板方法。
	 * @param streamReader the {@code XMLStreamReader} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given stream reader cannot be converted to an object
	 */
	protected abstract Object unmarshalXmlStreamReader(XMLStreamReader streamReader)
			throws XmlMappingException;

	/**
	 * 用于使用给定 SAX {@code XMLReader} 和 {@code InputSource} 进行 unmarshalling
	 * 的抽象模板方法。
	 * @param xmlReader the SAX {@code XMLReader} to parse with
	 * @param inputSource the input source to parse from
	 * @return the object graph
	 * @throws XmlMappingException if the given reader and input source cannot be converted to an object
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract Object unmarshalSaxReader(XMLReader xmlReader, InputSource inputSource)
			throws XmlMappingException, IOException;

	/**
	 * 用于从给定 {@code InputStream} 进行 unmarshalling 的抽象模板方法。
	 * @param inputStream the {@code InputStreamStream} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given stream cannot be converted to an object
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract Object unmarshalInputStream(InputStream inputStream)
			throws XmlMappingException, IOException;

	/**
	 * 用于从给定 {@code Reader} 进行 unmarshalling 的抽象模板方法。
	 * @param reader the {@code Reader} to read from
	 * @return the object graph
	 * @throws XmlMappingException if the given reader cannot be converted to an object
	 * @throws IOException if an I/O exception occurs
	 */
	protected abstract Object unmarshalReader(Reader reader)
			throws XmlMappingException, IOException;

}
