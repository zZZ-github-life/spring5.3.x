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

package org.springframework.http.converter.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StreamUtils;

/**
 * 用于在 XML 之间进行转换的
 * {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * 抽象基类。
 *
 * <p>默认情况下，此转换器的子类支持 {@code text/xml}、{@code application/xml} 和
 * {@code application/*-xml}。可以通过设置
 * {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} 属性来覆盖。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> 转换后的对象类型
 */
public abstract class AbstractXmlHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();


	/**
	 * 受保护的构造函数，将 {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes}
	 * 设置为 {@code text/xml}、{@code application/xml} 和 {@code application/*-xml}。
	 */
	protected AbstractXmlHttpMessageConverter() {
		super(MediaType.APPLICATION_XML, MediaType.TEXT_XML, new MediaType("application", "*+xml"));
	}


	@Override
	public final T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		try {
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());
			return readFromSource(clazz, inputMessage.getHeaders(), new StreamSource(inputStream));
		}
		catch (IOException | HttpMessageConversionException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex.getMessage(),
					ex, inputMessage);
		}
	}

	@Override
	protected final void writeInternal(T t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		try {
			writeToResult(t, outputMessage.getHeaders(), new StreamResult(outputMessage.getBody()));
		}
		catch (IOException | HttpMessageConversionException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new HttpMessageNotWritableException("Could not marshal [" + t + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 将给定的 {@code Source} 转换为 {@code Result}。
	 * @param source 要从中进行转换的源
	 * @param result 要转换到的结果
	 * @throws TransformerException 发生转换错误时抛出
	 */
	protected void transform(Source source, Result result) throws TransformerException {
		this.transformerFactory.newTransformer().transform(source, result);
	}


	/**
	 * 从 {@link #read(Class, HttpInputMessage)} 调用的抽象模板方法。
	 * @param clazz 要返回的对象类型
	 * @param headers HTTP 输入头
	 * @param source HTTP 输入主体
	 * @return 转换后的对象
	 * @throws Exception 发生 I/O 或转换错误时抛出
	 */
	protected abstract T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception;

	/**
	 * 从 {@link #writeInternal(Object, HttpOutputMessage)} 调用的抽象模板方法。
	 * @param t 要写入输出消息的对象
	 * @param headers HTTP 输出头
	 * @param result HTTP 输出主体
	 * @throws Exception 发生 I/O 或转换错误时抛出
	 */
	protected abstract void writeToResult(T t, HttpHeaders headers, Result result) throws Exception;

}
