/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.xml;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.reactivestreams.Publisher;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * 从包含 XML 元素的字节流解码为 {@code Object}（POJO）流。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @see Jaxb2XmlEncoder
 */
public class Jaxb2XmlDecoder extends AbstractDecoder<Object> {

	/**
	 * JAXB 注解的默认值。
	 * @see XmlRootElement#name()
	 * @see XmlRootElement#namespace()
	 * @see XmlType#name()
	 * @see XmlType#namespace()
	 */
	private static final String JAXB_DEFAULT_ANNOTATION_VALUE = "##default";

	private static final XMLInputFactory inputFactory = StaxUtils.createDefensiveInputFactory();


	private final XmlEventDecoder xmlEventDecoder = new XmlEventDecoder();

	private final JaxbContextContainer jaxbContexts = new JaxbContextContainer();

	private Function<Unmarshaller, Unmarshaller> unmarshallerProcessor = Function.identity();

	private int maxInMemorySize = 256 * 1024;


	public Jaxb2XmlDecoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}

	/**
	 * 使用指定的 MIME 类型创建 {@code Jaxb2XmlDecoder}。
	 * @param supportedMimeTypes 支持的 MIME 类型
	 * @since 5.1.9
	 */
	public Jaxb2XmlDecoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	/**
	 * 配置用于自定义 Unmarshaller 实例的处理器函数。
	 * @param processor 要使用的函数
	 * @since 5.1.3
	 */
	public void setUnmarshallerProcessor(Function<Unmarshaller, Unmarshaller> processor) {
		this.unmarshallerProcessor = this.unmarshallerProcessor.andThen(processor);
	}

	/**
	 * 返回用于自定义 Unmarshaller 实例的已配置处理器。
	 * @since 5.1.3
	 */
	public Function<Unmarshaller, Unmarshaller> getUnmarshallerProcessor() {
		return this.unmarshallerProcessor;
	}

	/**
	 * 设置此解码器可缓冲的最大字节数。
	 * 这可以是整体解码时整个输入的大小，或者在使用 Aalto XML
	 * 进行异步解析时，一个顶层 XML 树的大小。
	 * 超过限制时会抛出 {@link DataBufferLimitException}。
	 * <p>默认设置为 256K。
	 * @param byteCount 要缓冲的最大字节数，-1 表示不限
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
		this.xmlEventDecoder.setMaxInMemorySize(byteCount);
	}

	/**
	 * 返回{@link #setMaxInMemorySize 已配置}的字节数限制。
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		Class<?> outputClass = elementType.toClass();
		return (outputClass.isAnnotationPresent(XmlRootElement.class) ||
				outputClass.isAnnotationPresent(XmlType.class)) && super.canDecode(elementType, mimeType);
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Flux<XMLEvent> xmlEventFlux = this.xmlEventDecoder.decode(
				inputStream, ResolvableType.forClass(XMLEvent.class), mimeType, hints);

		Class<?> outputClass = elementType.toClass();
		QName typeName = toQName(outputClass);
		Flux<List<XMLEvent>> splitEvents = split(xmlEventFlux, typeName);

		return splitEvents.map(events -> {
			Object value = unmarshal(events, outputClass);
			LogFormatUtils.traceDebug(logger, traceOn -> {
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
			});
			return value;
		});
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})  // XMLEventReader is Iterator<Object> on JDK 9
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(input, this.maxInMemorySize)
				.map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints));
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})  // XMLEventReader is Iterator<Object> on JDK 9
	public Object decode(DataBuffer dataBuffer, ResolvableType targetType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		try {
			Iterator eventReader = inputFactory.createXMLEventReader(dataBuffer.asInputStream(), encoding(mimeType));
			List<XMLEvent> events = new ArrayList<>();
			eventReader.forEachRemaining(event -> events.add((XMLEvent) event));
			return unmarshal(events, targetType.toClass());
		}
		catch (XMLStreamException ex) {
			throw new DecodingException(ex.getMessage(), ex);
		}
		catch (Throwable ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof XMLStreamException) {
				throw new DecodingException(cause.getMessage(), cause);
			}
			else {
				throw Exceptions.propagate(ex);
			}
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
	}

	@Nullable
	private static String encoding(@Nullable MimeType mimeType) {
		if (mimeType == null) {
			return null;
		}
		Charset charset = mimeType.getCharset();
		if (charset == null) {
			return null;
		}
		else {
			return charset.name();
		}
	}

	private Object unmarshal(List<XMLEvent> events, Class<?> outputClass) {
		try {
			Unmarshaller unmarshaller = initUnmarshaller(outputClass);
			XMLEventReader eventReader = StaxUtils.createXMLEventReader(events);
			if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
				return unmarshaller.unmarshal(eventReader);
			}
			else {
				JAXBElement<?> jaxbElement = unmarshaller.unmarshal(eventReader, outputClass);
				return jaxbElement.getValue();
			}
		}
		catch (UnmarshalException ex) {
			throw new DecodingException("Could not unmarshal XML to " + outputClass, ex);
		}
		catch (JAXBException ex) {
			throw new CodecException("Invalid JAXB configuration", ex);
		}
	}

	private Unmarshaller initUnmarshaller(Class<?> outputClass) throws CodecException, JAXBException {
		Unmarshaller unmarshaller = this.jaxbContexts.createUnmarshaller(outputClass);
		return this.unmarshallerProcessor.apply(unmarshaller);
	}

	/**
	 * 根据 JAXB 规范中的映射规则，返回给定类的限定名称。
	 */
	QName toQName(Class<?> outputClass) {
		String localPart;
		String namespaceUri;

		if (outputClass.isAnnotationPresent(XmlRootElement.class)) {
			XmlRootElement annotation = outputClass.getAnnotation(XmlRootElement.class);
			localPart = annotation.name();
			namespaceUri = annotation.namespace();
		}
		else if (outputClass.isAnnotationPresent(XmlType.class)) {
			XmlType annotation = outputClass.getAnnotation(XmlType.class);
			localPart = annotation.name();
			namespaceUri = annotation.namespace();
		}
		else {
			throw new IllegalArgumentException("Output class [" + outputClass.getName() +
					"] is neither annotated with @XmlRootElement nor @XmlType");
		}

		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(localPart)) {
			localPart = ClassUtils.getShortNameAsProperty(outputClass);
		}
		if (JAXB_DEFAULT_ANNOTATION_VALUE.equals(namespaceUri)) {
			Package outputClassPackage = outputClass.getPackage();
			if (outputClassPackage != null && outputClassPackage.isAnnotationPresent(XmlSchema.class)) {
				XmlSchema annotation = outputClassPackage.getAnnotation(XmlSchema.class);
				namespaceUri = annotation.namespace();
			}
			else {
				namespaceUri = XMLConstants.NULL_NS_URI;
			}
		}
		return new QName(namespaceUri, localPart);
	}

	/**
	 * 将 {@link XMLEvent XMLEvent} 的 Flux 拆分为 XMLEvent 列表的 Flux，
	 * 每个列表对应以给定限定名称开头的树的一个分支。
	 * 也就是说，给定{@linkplain XmlEventDecoder 此处}所示的 XMLEvent，
	 * 以及 {@code desiredName} 为 "{@code child}"，此方法返回包含两个列表的 Flux，
	 * 每个列表包含以 "{@code child}" 开头的树中某个特定分支的事件。
	 * <ol>
	 * <li>第一个列表，处理树的第一个分支：
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * <li>第二个列表，处理树的第二个分支：
	 * <ol>
	 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
	 * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
	 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
	 * </ol>
	 * </li>
	 * </ol>
	 */
	Flux<List<XMLEvent>> split(Flux<XMLEvent> xmlEventFlux, QName desiredName) {
		return xmlEventFlux.handle(new SplitHandler(desiredName));
	}


	private static class SplitHandler implements BiConsumer<XMLEvent, SynchronousSink<List<XMLEvent>>> {

		private final QName desiredName;

		@Nullable
		private List<XMLEvent> events;

		private int elementDepth = 0;

		private int barrier = Integer.MAX_VALUE;

		public SplitHandler(QName desiredName) {
			this.desiredName = desiredName;
		}

		@Override
		public void accept(XMLEvent event, SynchronousSink<List<XMLEvent>> sink) {
			if (event.isStartElement()) {
				if (this.barrier == Integer.MAX_VALUE) {
					QName startElementName = event.asStartElement().getName();
					if (this.desiredName.equals(startElementName)) {
						this.events = new ArrayList<>();
						this.barrier = this.elementDepth;
					}
				}
				this.elementDepth++;
			}
			if (this.elementDepth > this.barrier) {
				Assert.state(this.events != null, "No XMLEvent List");
				this.events.add(event);
			}
			if (event.isEndElement()) {
				this.elementDepth--;
				if (this.elementDepth == this.barrier) {
					this.barrier = Integer.MAX_VALUE;
					Assert.state(this.events != null, "No XMLEvent List");
					sink.next(this.events);
				}
			}
		}
	}

}
