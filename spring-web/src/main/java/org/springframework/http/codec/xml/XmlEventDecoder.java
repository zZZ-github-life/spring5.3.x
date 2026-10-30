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

package org.springframework.http.codec.xml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.evt.EventAllocatorImpl;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.xml.StaxUtils;

/**
 * 将 {@link DataBuffer} 流解码为 {@link XMLEvent XMLEvent} 流。
 *
 * <p>给定以下 XML：
 *
 * <pre class="code">
 * &lt;root&gt;
 * &lt;child&gt;foo&lt;/child&gt;
 * &lt;child&gt;bar&lt;/child&gt;
 * &lt;/root&gt;
 * </pre>
 *
 * 此解码器将生成包含以下事件的 {@link Flux}：
 *
 * <ol>
 * <li>{@link javax.xml.stream.events.StartDocument}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code root}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.Characters} {@code foo}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.StartElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.Characters} {@code bar}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code child}</li>
 * <li>{@link javax.xml.stream.events.EndElement} {@code root}</li>
 * </ol>
 *
 * <p>请注意，此解码器默认不注册，而是由其他默认注册的
 * 解码器在内部使用。
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.0
 */
public class XmlEventDecoder extends AbstractDecoder<XMLEvent> {

	private static final XMLInputFactory inputFactory = StaxUtils.createDefensiveInputFactory();

	private static final boolean aaltoPresent = ClassUtils.isPresent(
			"com.fasterxml.aalto.AsyncXMLStreamReader", XmlEventDecoder.class.getClassLoader());

	boolean useAalto = aaltoPresent;

	private int maxInMemorySize = 256 * 1024;


	public XmlEventDecoder() {
		super(MimeTypeUtils.APPLICATION_XML, MimeTypeUtils.TEXT_XML, new MediaType("application", "*+xml"));
	}


	/**
	 * 设置此解码器可缓冲的最大字节数。这可以是整体解码时
	 * 整个输入的大小，或者在使用 Aalto XML 进行异步解析时，
	 * 一个顶层 XML 树的大小。超过限制时会抛出
	 * {@link DataBufferLimitException}。
	 * <p>默认设置为 256K。
	 * @param byteCount 要缓冲的最大字节数，-1 表示不限
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 返回{@link #setMaxInMemorySize 已配置}的字节数限制。
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})  // XMLEventReader is Iterator<Object> on JDK 9
	public Flux<XMLEvent> decode(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (this.useAalto) {
			AaltoDataBufferToXmlEvent mapper = new AaltoDataBufferToXmlEvent(this.maxInMemorySize);
			return Flux.from(input)
					.flatMapIterable(mapper)
					.doFinally(signalType -> mapper.endOfInput());
		}
		else {
			return DataBufferUtils.join(input, this.maxInMemorySize)
					.flatMapIterable(buffer -> {
						try {
							InputStream is = buffer.asInputStream();
							Iterator eventReader = inputFactory.createXMLEventReader(is);
							List<XMLEvent> result = new ArrayList<>();
							eventReader.forEachRemaining(event -> result.add((XMLEvent) event));
							return result;
						}
						catch (XMLStreamException ex) {
							throw new DecodingException(ex.getMessage(), ex);
						}
						finally {
							DataBufferUtils.release(buffer);
						}
					});
		}
	}


	/*
	 * Separate static class to isolate Aalto dependency.
	 */
	private static class AaltoDataBufferToXmlEvent implements Function<DataBuffer, List<? extends XMLEvent>> {

		private static final AsyncXMLInputFactory inputFactory =
				StaxUtils.createDefensiveInputFactory(InputFactoryImpl::new);

		private final AsyncXMLStreamReader<AsyncByteBufferFeeder> streamReader =
				inputFactory.createAsyncForByteBuffer();

		private final XMLEventAllocator eventAllocator = EventAllocatorImpl.getDefaultInstance();

		private final int maxInMemorySize;

		private int byteCount;

		private int elementDepth;


		public AaltoDataBufferToXmlEvent(int maxInMemorySize) {
			this.maxInMemorySize = maxInMemorySize;
		}


		@Override
		public List<? extends XMLEvent> apply(DataBuffer dataBuffer) {
			try {
				increaseByteCount(dataBuffer);
				this.streamReader.getInputFeeder().feedInput(dataBuffer.asByteBuffer());
				List<XMLEvent> events = new ArrayList<>();
				while (true) {
					if (this.streamReader.next() == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
						// no more events with what currently has been fed to the reader
						break;
					}
					else {
						XMLEvent event = this.eventAllocator.allocate(this.streamReader);
						events.add(event);
						if (event.isEndDocument()) {
							break;
						}
						checkDepthAndResetByteCount(event);
					}
				}
				if (this.maxInMemorySize > 0 && this.byteCount > this.maxInMemorySize) {
					raiseLimitException();
				}
				return events;
			}
			catch (XMLStreamException ex) {
				throw new DecodingException(ex.getMessage(), ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		private void increaseByteCount(DataBuffer dataBuffer) {
			if (this.maxInMemorySize > 0) {
				if (dataBuffer.readableByteCount() > Integer.MAX_VALUE - this.byteCount) {
					raiseLimitException();
				}
				else {
					this.byteCount += dataBuffer.readableByteCount();
				}
			}
		}

		private void checkDepthAndResetByteCount(XMLEvent event) {
			if (this.maxInMemorySize > 0) {
				if (event.isStartElement()) {
					this.byteCount = this.elementDepth == 1 ? 0 : this.byteCount;
					this.elementDepth++;
				}
				else if (event.isEndElement()) {
					this.elementDepth--;
					this.byteCount = this.elementDepth == 1 ? 0 : this.byteCount;
				}
			}
		}

		private void raiseLimitException() {
			throw new DataBufferLimitException(
					"Exceeded limit on max bytes per XML top-level node: " + this.maxInMemorySize);
		}

		public void endOfInput() {
			this.streamReader.getInputFeeder().endOfInput();
		}
	}



}
