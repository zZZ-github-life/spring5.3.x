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

package org.springframework.oxm.support;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;

/**
 * 使用 {@link Marshaller} 的 {@link Source} 实现。可以使用 {@code Marshaller}
 * 以及待 marshal 的对象来构造。
 *
 * <p>尽管 {@code MarshallingSource} 继承自 {@code SAXSource}，但<strong>不支持</strong>调用
 * {@code SAXSource} 的方法。通常，此类唯一支持的操作是使用通过 {@link #getXMLReader()}
 * 获取的 {@code XMLReader} 来解析通过 {@link #getInputSource()} 获取的输入源。
 * 调用 {@link #setXMLReader(XMLReader)} 或 {@link #setInputSource(InputSource)}
 * 将导致抛出 {@code UnsupportedOperationException}。
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see javax.xml.transform.Transformer
 */
public class MarshallingSource extends SAXSource {

	private final Marshaller marshaller;

	private final Object content;


	/**
	 * 使用给定的 marshaller 和内容创建一个新的 {@code MarshallingSource}。
	 * @param marshaller 要使用的 marshaller
	 * @param content 要编组的对象
	 */
	public MarshallingSource(Marshaller marshaller, Object content) {
		super(new MarshallingXMLReader(marshaller, content), new InputSource());
		Assert.notNull(marshaller, "'marshaller' must not be null");
		Assert.notNull(content, "'content' must not be null");
		this.marshaller = marshaller;
		this.content = content;
	}


	/**
	 * 返回此 {@code MarshallingSource} 所使用的 {@code Marshaller}。
	 */
	public Marshaller getMarshaller() {
		return this.marshaller;
	}

	/**
	 * 返回待 marshal 的对象。
	 */
	public Object getContent() {
		return this.content;
	}

	/**
	 * 抛出 {@code UnsupportedOperationException}。
	 */
	@Override
	public void setInputSource(InputSource inputSource) {
		throw new UnsupportedOperationException("setInputSource is not supported");
	}

	/**
	 * 抛出 {@code UnsupportedOperationException}。
	 */
	@Override
	public void setXMLReader(XMLReader reader) {
		throw new UnsupportedOperationException("setXMLReader is not supported");
	}


	private static final class MarshallingXMLReader implements XMLReader {

		private final Marshaller marshaller;

		private final Object content;

		@Nullable
		private DTDHandler dtdHandler;

		@Nullable
		private ContentHandler contentHandler;

		@Nullable
		private EntityResolver entityResolver;

		@Nullable
		private ErrorHandler errorHandler;

		@Nullable
		private LexicalHandler lexicalHandler;

		private MarshallingXMLReader(Marshaller marshaller, Object content) {
			Assert.notNull(marshaller, "'marshaller' must not be null");
			Assert.notNull(content, "'content' must not be null");
			this.marshaller = marshaller;
			this.content = content;
		}

		@Override
		public void setContentHandler(@Nullable ContentHandler contentHandler) {
			this.contentHandler = contentHandler;
		}

		@Override
		@Nullable
		public ContentHandler getContentHandler() {
			return this.contentHandler;
		}

		@Override
		public void setDTDHandler(@Nullable DTDHandler dtdHandler) {
			this.dtdHandler = dtdHandler;
		}

		@Override
		@Nullable
		public DTDHandler getDTDHandler() {
			return this.dtdHandler;
		}

		@Override
		public void setEntityResolver(@Nullable EntityResolver entityResolver) {
			this.entityResolver = entityResolver;
		}

		@Override
		@Nullable
		public EntityResolver getEntityResolver() {
			return this.entityResolver;
		}

		@Override
		public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
			this.errorHandler = errorHandler;
		}

		@Override
		@Nullable
		public ErrorHandler getErrorHandler() {
			return this.errorHandler;
		}

		@Nullable
		protected LexicalHandler getLexicalHandler() {
			return this.lexicalHandler;
		}

		@Override
		public boolean getFeature(String name) throws SAXNotRecognizedException {
			throw new SAXNotRecognizedException(name);
		}

		@Override
		public void setFeature(String name, boolean value) throws SAXNotRecognizedException {
			throw new SAXNotRecognizedException(name);
		}

		@Override
		@Nullable
		public Object getProperty(String name) throws SAXNotRecognizedException {
			if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
				return this.lexicalHandler;
			}
			else {
				throw new SAXNotRecognizedException(name);
			}
		}

		@Override
		public void setProperty(String name, Object value) throws SAXNotRecognizedException {
			if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
				this.lexicalHandler = (LexicalHandler) value;
			}
			else {
				throw new SAXNotRecognizedException(name);
			}
		}

		@Override
		public void parse(InputSource input) throws SAXException {
			parse();
		}

		@Override
		public void parse(String systemId) throws SAXException {
			parse();
		}

		private void parse() throws SAXException {
			SAXResult result = new SAXResult(getContentHandler());
			result.setLexicalHandler(getLexicalHandler());
			try {
				this.marshaller.marshal(this.content, result);
			}
			catch (IOException ex) {
				SAXParseException saxException = new SAXParseException(ex.getMessage(), null, null, -1, -1, ex);
				ErrorHandler errorHandler = getErrorHandler();
				if (errorHandler != null) {
					errorHandler.fatalError(saxException);
				}
				else {
					throw saxException;
				}
			}
		}
	}

}
