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

package org.springframework.util.xml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import org.springframework.lang.Nullable;

/**
 * StAX 读取器的 {@code Source} 标记接口实现。可以用
 * {@code XMLEventReader} 或 {@code XMLStreamReader} 构造。
 *
 * <p>此类是必要的，因为 JAXP 1.3 中没有针对 StAX 读取器的 {@code Source} 实现。JAXP 1.4（JDK 1.6）
 * 中提供了 {@code StAXSource}，但出于向后兼容的原因保留了此类。
 *
 * <p>尽管 {@code StaxSource} 继承自 {@code SAXSource}，但调用 {@code SAXSource} 的方法<strong>不受
 * 支持</strong>。通常，此类上唯一受支持的操作是使用通过 {@link #getXMLReader()} 获取的
 * {@code XMLReader} 解析通过 {@link #getInputSource()} 获取的输入源。调用
 * {@link #setXMLReader(XMLReader)} 或 {@link #setInputSource(InputSource)} 将导致
 * {@code UnsupportedOperationException #setInputSource(InputSource)} 将导致
 * {@code UnsupportedOperationExceptions}。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see XMLEventReader
 * @see XMLStreamReader
 * @see javax.xml.transform.Transformer
 */
class StaxSource extends SAXSource {

	@Nullable
	private XMLEventReader eventReader;

	@Nullable
	private XMLStreamReader streamReader;


	/**
	 * 使用指定的 {@code XMLEventReader} 构造一个新的 {@code StaxSource} 实例。
	 * 提供的事件读取器必须处于 {@code XMLStreamConstants.START_DOCUMENT} 或
	 * {@code XMLStreamConstants.START_ELEMENT} 状态。
	 * @param eventReader the {@code XMLEventReader} to read from
	 * @throws IllegalStateException if the reader is not at the start of a document or element
	 */
	StaxSource(XMLEventReader eventReader) {
		super(new StaxEventXMLReader(eventReader), new InputSource());
		this.eventReader = eventReader;
	}

	/**
	 * 使用指定的 {@code XMLStreamReader} 构造一个新的 {@code StaxSource} 实例。
	 * 提供的流读取器必须处于 {@code XMLStreamConstants.START_DOCUMENT} 或
	 * {@code XMLStreamConstants.START_ELEMENT} 状态。
	 * @param streamReader the {@code XMLStreamReader} to read from
	 * @throws IllegalStateException if the reader is not at the start of a document or element
	 */
	StaxSource(XMLStreamReader streamReader) {
		super(new StaxStreamXMLReader(streamReader), new InputSource());
		this.streamReader = streamReader;
	}


	/**
	 * 返回此 {@code StaxSource} 使用的 {@code XMLEventReader}。
	 * <p>如果此 {@code StaxSource} 是使用 {@code XMLStreamReader} 创建的，
	 * 则结果为 {@code null}。
	 * @return the StAX event reader used by this source
	 * @see StaxSource#StaxSource(javax.xml.stream.XMLEventReader)
	 */
	@Nullable
	XMLEventReader getXMLEventReader() {
		return this.eventReader;
	}

	/**
	 * 返回此 {@code StaxSource} 使用的 {@code XMLStreamReader}。
	 * <p>如果此 {@code StaxSource} 是使用 {@code XMLEventReader} 创建的，
	 * 则结果为 {@code null}。
	 * @return the StAX event reader used by this source
	 * @see StaxSource#StaxSource(javax.xml.stream.XMLEventReader)
	 */
	@Nullable
	XMLStreamReader getXMLStreamReader() {
		return this.streamReader;
	}


	/**
	 * 抛出一个 {@code UnsupportedOperationException}。
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setInputSource(InputSource inputSource) {
		throw new UnsupportedOperationException("setInputSource is not supported");
	}

	/**
	 * 抛出一个 {@code UnsupportedOperationException}。
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setXMLReader(XMLReader reader) {
		throw new UnsupportedOperationException("setXMLReader is not supported");
	}

}
