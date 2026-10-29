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

package org.springframework.http.converter.xml;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.converter.HttpMessageConversionException;

/**
 * 使用 JAXB2 的
 * {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * 抽象基类。延迟创建 {@link JAXBContext} 对象。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 * @param <T> the converted object type
 */
public abstract class AbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

	private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);


	/**
	 * 为给定的类创建新的 {@link Marshaller}。
	 * @param clazz the class to create the marshaller for
	 * @return the {@code Marshaller}
	 * @throws HttpMessageConversionException in case of JAXB errors
	 */
	protected final Marshaller createMarshaller(Class<?> clazz) {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			Marshaller marshaller = jaxbContext.createMarshaller();
			customizeMarshaller(marshaller);
			return marshaller;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException(
					"Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 在此消息转换器使用 {@link Marshaller} 将对象写入输出之前，
	 * 自定义该 Marshaller。
	 * @param marshaller the marshaller to customize
	 * @since 4.0.3
	 * @see #createMarshaller(Class)
	 */
	protected void customizeMarshaller(Marshaller marshaller) {
	}

	/**
	 * 为给定的类创建新的 {@link Unmarshaller}。
	 * @param clazz the class to create the unmarshaller for
	 * @return the {@code Unmarshaller}
	 * @throws HttpMessageConversionException in case of JAXB errors
	 */
	protected final Unmarshaller createUnmarshaller(Class<?> clazz) {
		try {
			JAXBContext jaxbContext = getJaxbContext(clazz);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			customizeUnmarshaller(unmarshaller);
			return unmarshaller;
		}
		catch (JAXBException ex) {
			throw new HttpMessageConversionException(
					"Could not create Unmarshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 在此消息转换器使用 {@link Unmarshaller} 从输入读取对象之前，
	 * 自定义该 Unmarshaller。
	 * @param unmarshaller the unmarshaller to customize
	 * @since 4.0.3
	 * @see #createUnmarshaller(Class)
	 */
	protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
	}

	/**
	 * 返回给定类的 {@link JAXBContext}。
	 * @param clazz the class to return the context for
	 * @return the {@code JAXBContext}
	 * @throws HttpMessageConversionException in case of JAXB errors
	 */
	protected final JAXBContext getJaxbContext(Class<?> clazz) {
		return this.jaxbContexts.computeIfAbsent(clazz, key -> {
			try {
				return JAXBContext.newInstance(clazz);
			}
			catch (JAXBException ex) {
				throw new HttpMessageConversionException(
						"Could not create JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
			}
		});
	}

}
