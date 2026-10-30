/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.xml;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * XML 专用的 BeanDefinitionStoreException 子类，它包装 {@link org.xml.sax.SAXException}，通常是包
 * 含错误位置信息的 {@link org.xml.sax.SAXParseException}。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #getLineNumber()
 * @see org.xml.sax.SAXParseException
 */
@SuppressWarnings("serial")
public class XmlBeanDefinitionStoreException extends BeanDefinitionStoreException {

	/**
	 * 创建一个新的 XmlBeanDefinitionStoreException。
	 * @param resourceDescription Bean 定义来源资源的描述
	 * @param msg 详细消息（原样用作异常消息）
	 * @param cause SAXException（通常是 SAXParseException）根本原因
	 * @see org.xml.sax.SAXParseException
	 */
	public XmlBeanDefinitionStoreException(String resourceDescription, String msg, SAXException cause) {
		super(resourceDescription, msg, cause);
	}

	/**
	 * 返回失败的 XML 资源中的行号。
	 * @return 如果可用则返回行号（在出现 SAXParseException 的情况下）；否则返回 -1
	 * @see org.xml.sax.SAXParseException#getLineNumber()
	 */
	public int getLineNumber() {
		Throwable cause = getCause();
		if (cause instanceof SAXParseException) {
			return ((SAXParseException) cause).getLineNumber();
		}
		return -1;
	}

}
