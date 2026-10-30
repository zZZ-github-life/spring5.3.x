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

package org.springframework.util.xml;

import java.util.List;
import java.util.function.Supplier;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;

import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;

import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * 用于处理 StAX API 的便捷方法。部分方法因 JAXP 1.3 兼容性
 * 而具有历史原因；自 Spring 4.0 起，依赖 JDK 1.6 及更高版本中包含的 JAXP 1.4。
 *
 * <p>特别是用于将 StAX（{@code javax.xml.stream}）与
 * TrAX API（{@code javax.xml.transform}）结合使用，以及在 StAX 读取器/写入器与 SAX
 * 读取器/处理器之间相互转换的方法。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class StaxUtils {

	private static final XMLResolver NO_OP_XML_RESOLVER =
			(publicID, systemID, base, ns) -> StreamUtils.emptyInput();


	/**
	 * 使用 Spring 的防御性设置创建 {@link XMLInputFactory}，
	 * 即不支持解析 DTD 和外部实体。
	 * @return 一个可供使用的新防御性初始化的输入工厂实例
	 * @since 5.0
	 */
	public static XMLInputFactory createDefensiveInputFactory() {
		return createDefensiveInputFactory(XMLInputFactory::newInstance);
	}

	/**
	 * {@link #createDefensiveInputFactory()} 的变体，使用自定义实例。
	 * @param instanceSupplier 输入工厂实例的 supplier
	 * @return 一个可供使用的新防御性初始化的输入工厂实例
	 * @since 5.0.12
	 */
	public static <T extends XMLInputFactory> T createDefensiveInputFactory(Supplier<T> instanceSupplier) {
		T inputFactory = instanceSupplier.get();
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		inputFactory.setXMLResolver(NO_OP_XML_RESOLVER);
		return inputFactory;
	}

	/**
	 * 为给定的 {@link XMLStreamReader} 创建一个 JAXP 1.4 {@link StAXSource}。
	 * @param streamReader StAX 流读取器
	 * @return 包装 {@code streamReader} 的源
	 */
	public static Source createStaxSource(XMLStreamReader streamReader) {
		return new StAXSource(streamReader);
	}

	/**
	 * 为给定的 {@link XMLEventReader} 创建一个 JAXP 1.4 {@link StAXSource}。
	 * @param eventReader StAX 事件读取器
	 * @return 包装 {@code eventReader} 的源
	 */
	public static Source createStaxSource(XMLEventReader eventReader) throws XMLStreamException {
		return new StAXSource(eventReader);
	}

	/**
	 * 为给定的 {@link XMLStreamReader} 创建一个自定义的、非 JAXP 1.4 的 StAX {@link Source}。
	 * @param streamReader StAX 流读取器
	 * @return 包装 {@code streamReader} 的源
	 */
	public static Source createCustomStaxSource(XMLStreamReader streamReader) {
		return new StaxSource(streamReader);
	}

	/**
	 * 为给定的 {@link XMLEventReader} 创建一个自定义的、非 JAXP 1.4 的 StAX {@link Source}。
	 * @param eventReader StAX 事件读取器
	 * @return 包装 {@code eventReader} 的源
	 */
	public static Source createCustomStaxSource(XMLEventReader eventReader) {
		return new StaxSource(eventReader);
	}

	/**
	 * 指示给定的 {@link Source} 是 JAXP 1.4 StAX Source 还是
	 * 自定义 StAX Source。
	 * @return 如果 {@code source} 是 JAXP 1.4 {@link StAXSource} 或
	 * 自定义 StAX Source 则返回 {@code true}；否则返回 {@code false}
	 */
	public static boolean isStaxSource(Source source) {
		return (source instanceof StAXSource || source instanceof StaxSource);
	}

	/**
	 * 返回给定 StAX Source 的 {@link XMLStreamReader}。
	 * @param source 一个 JAXP 1.4 {@link StAXSource}
	 * @return 该 {@link XMLStreamReader}
	 * @throws IllegalArgumentException 如果 {@code source} 不是 JAXP 1.4 {@link StAXSource}
	 * 或自定义 StAX Source
	 */
	@Nullable
	public static XMLStreamReader getXMLStreamReader(Source source) {
		if (source instanceof StAXSource) {
			return ((StAXSource) source).getXMLStreamReader();
		}
		else if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLStreamReader();
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * 返回给定 StAX Source 的 {@link XMLEventReader}。
	 * @param source 一个 JAXP 1.4 {@link StAXSource}
	 * @return 该 {@link XMLEventReader}
	 * @throws IllegalArgumentException 如果 {@code source} 不是 JAXP 1.4 {@link StAXSource}
	 * 或自定义 StAX Source
	 */
	@Nullable
	public static XMLEventReader getXMLEventReader(Source source) {
		if (source instanceof StAXSource) {
			return ((StAXSource) source).getXMLEventReader();
		}
		else if (source instanceof StaxSource) {
			return ((StaxSource) source).getXMLEventReader();
		}
		else {
			throw new IllegalArgumentException("Source '" + source + "' is neither StaxSource nor StAXSource");
		}
	}

	/**
	 * 为给定的 {@link XMLStreamWriter} 创建一个 JAXP 1.4 {@link StAXResult}。
	 * @param streamWriter StAX 流写入器
	 * @return 包装 {@code streamWriter} 的结果
	 */
	public static Result createStaxResult(XMLStreamWriter streamWriter) {
		return new StAXResult(streamWriter);
	}

	/**
	 * 为给定的 {@link XMLEventWriter} 创建一个 JAXP 1.4 {@link StAXResult}。
	 * @param eventWriter StAX 事件写入器
	 * @return 包装 {@code streamReader} 的结果
	 */
	public static Result createStaxResult(XMLEventWriter eventWriter) {
		return new StAXResult(eventWriter);
	}

	/**
	 * 为给定的 {@link XMLStreamWriter} 创建一个自定义的、非 JAXP 1.4 的 StAX {@link Result}。
	 * @param streamWriter StAX 流写入器
	 * @return 包装 {@code streamWriter} 的源
	 */
	public static Result createCustomStaxResult(XMLStreamWriter streamWriter) {
		return new StaxResult(streamWriter);
	}

	/**
	 * 为给定的 {@link XMLEventWriter} 创建一个自定义的、非 JAXP 1.4 的 StAX {@link Result}。
	 * @param eventWriter StAX 事件写入器
	 * @return 包装 {@code eventWriter} 的源
	 */
	public static Result createCustomStaxResult(XMLEventWriter eventWriter) {
		return new StaxResult(eventWriter);
	}

	/**
	 * 指示给定的 {@link Result} 是 JAXP 1.4 StAX Result 还是
	 * 自定义 StAX Result。
	 * @return 如果 {@code result} 是 JAXP 1.4 {@link StAXResult} 或自定义 StAX Result，
	 * 则返回 {@code true}，否则返回 {@code false}。
	 */
	public static boolean isStaxResult(Result result) {
		return (result instanceof StAXResult || result instanceof StaxResult);
	}

	/**
	 * 返回给定 StAX Result 的 {@link XMLStreamWriter}。
	 * @param result JAXP 1.4 的 {@link StAXResult}
	 * @return 该 {@link XMLStreamReader}
	 * @throws IllegalArgumentException 如果 {@code source} 不是 JAXP 1.4 {@link StAXResult} 或自定义 StAX Result
	 */
	@Nullable
	public static XMLStreamWriter getXMLStreamWriter(Result result) {
		if (result instanceof StAXResult) {
			return ((StAXResult) result).getXMLStreamWriter();
		}
		else if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLStreamWriter();
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * 返回给定 StAX Result 的 {@link XMLEventWriter}。
	 * @param result JAXP 1.4 的 {@link StAXResult}
	 * @return 该 {@link XMLStreamReader}
	 * @throws IllegalArgumentException 如果 {@code source} 不是 JAXP 1.4 {@link StAXResult} 或自定义 StAX Result
	 */
	@Nullable
	public static XMLEventWriter getXMLEventWriter(Result result) {
		if (result instanceof StAXResult) {
			return ((StAXResult) result).getXMLEventWriter();
		}
		else if (result instanceof StaxResult) {
			return ((StaxResult) result).getXMLEventWriter();
		}
		else {
			throw new IllegalArgumentException("Result '" + result + "' is neither StaxResult nor StAXResult");
		}
	}

	/**
	 * 根据给定的 {@link XMLEvent} 列表创建 {@link XMLEventReader}。
	 * @param events {@link XMLEvent XMLEvents} 的列表。
	 * @return 从给定事件中读取的 {@code XMLEventReader}
	 * @since 5.0
	 */
	public static XMLEventReader createXMLEventReader(List<XMLEvent> events) {
		return new ListBasedXMLEventReader(events);
	}

	/**
	 * 创建一个写入给定 StAX {@link XMLStreamWriter} 的 SAX {@link ContentHandler}。
	 * @param streamWriter StAX 流写入器
	 * @return 写入 {@code streamWriter} 的内容处理器
	 */
	public static ContentHandler createContentHandler(XMLStreamWriter streamWriter) {
		return new StaxStreamHandler(streamWriter);
	}

	/**
	 * 创建一个将事件写入给定 StAX {@link XMLEventWriter} 的 SAX {@link ContentHandler}。
	 * @param eventWriter StAX 事件写入器
	 * @return 写入 {@code eventWriter} 的内容处理器
	 */
	public static ContentHandler createContentHandler(XMLEventWriter eventWriter) {
		return new StaxEventHandler(eventWriter);
	}

	/**
	 * 创建一个从给定 StAX {@link XMLStreamReader} 读取的 SAX {@link XMLReader}。
	 * @param streamReader StAX 流读取器
	 * @return 从 {@code streamWriter} 读取的 XMLReader
	 */
	public static XMLReader createXMLReader(XMLStreamReader streamReader) {
		return new StaxStreamXMLReader(streamReader);
	}

	/**
	 * 创建一个从给定 StAX {@link XMLEventReader} 读取的 SAX {@link XMLReader}。
	 * @param eventReader StAX 事件读取器
	 * @return 从 {@code eventWriter} 读取的 XMLReader
	 */
	public static XMLReader createXMLReader(XMLEventReader eventReader) {
		return new StaxEventXMLReader(eventReader);
	}

	/**
	 * 返回一个从 {@link XMLEventReader} 读取的 {@link XMLStreamReader}。
	 * 这很有用，因为 StAX {@code XMLInputFactory} 允许从流读取器创建
	 * 事件读取器，但反之则不行。
	 * @return 从事件读取器读取的流读取器
	 */
	public static XMLStreamReader createEventStreamReader(XMLEventReader eventReader) throws XMLStreamException {
		return new XMLEventStreamReader(eventReader);
	}

	/**
	 * 返回一个写入到 {@link XMLEventWriter} 的 {@link XMLStreamWriter}。
	 * @return 写入事件写入器的流写入器
	 * @since 3.2
	 */
	public static XMLStreamWriter createEventStreamWriter(XMLEventWriter eventWriter) {
		return new XMLEventStreamWriter(eventWriter, XMLEventFactory.newFactory());
	}

	/**
	 * 返回一个写入到 {@link XMLEventWriter} 的 {@link XMLStreamWriter}。
	 * @return 写入事件写入器的流写入器
	 * @since 3.0.5
	 */
	public static XMLStreamWriter createEventStreamWriter(XMLEventWriter eventWriter, XMLEventFactory eventFactory) {
		return new XMLEventStreamWriter(eventWriter, eventFactory);
	}

}
