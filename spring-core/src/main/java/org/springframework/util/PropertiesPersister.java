/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * 用于持久化 {@code java.util.Properties} 的策略接口，允许使用可插拔的解析策略。
 *
 * <p>默认实现是 DefaultPropertiesPersister，提供对 {@code java.util.Properties} 的原生解析，同时允
 * 许从任意 Reader 读取并写入任意 Writer（从而可以为 properties 文件指定编码）。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see DefaultPropertiesPersister
 * @see org.springframework.core.io.support.ResourcePropertiesPersister
 * @see java.util.Properties
 */
public interface PropertiesPersister {

	/**
	 * 从给定的 InputStream 将属性加载到给定的 Properties 对象中。
	 * @param props 要加载到的 Properties 对象
	 * @param is 要从中加载的 InputStream
	 * @throws IOException 在发生 I/O 错误时
	 * @see java.util.Properties#load
	 */
	void load(Properties props, InputStream is) throws IOException;

	/**
	 * 从给定的 Reader 将属性加载到给定的 Properties 对象中。
	 * @param props 要加载到的 Properties 对象
	 * @param reader 要从中加载的 Reader
	 * @throws IOException 在发生 I/O 错误时
	 */
	void load(Properties props, Reader reader) throws IOException;

	/**
	 * 将给定 Properties 对象的内容写入给定的 OutputStream。
	 * @param props 要存储的 Properties 对象
	 * @param os 要写入的 OutputStream
	 * @param header 属性列表的描述
	 * @throws IOException 在发生 I/O 错误时
	 * @see java.util.Properties#store
	 */
	void store(Properties props, OutputStream os, String header) throws IOException;

	/**
	 * 将给定 Properties 对象的内容写入给定的 Writer。
	 * @param props 要存储的 Properties 对象
	 * @param writer 要写入的 Writer
	 * @param header 属性列表的描述
	 * @throws IOException 在发生 I/O 错误时
	 */
	void store(Properties props, Writer writer, String header) throws IOException;

	/**
	 * 从给定的 XML InputStream 将属性加载到给定的 Properties 对象中。
	 * @param props 要加载到的 Properties 对象
	 * @param is 要从中加载的 InputStream
	 * @throws IOException 在发生 I/O 错误时
	 * @see java.util.Properties#loadFromXML(java.io.InputStream)
	 */
	void loadFromXml(Properties props, InputStream is) throws IOException;

	/**
	 * 将给定 Properties 对象的内容写入给定的 XML OutputStream。
	 * @param props 要存储的 Properties 对象
	 * @param os 要写入的 OutputStream
	 * @param header 属性列表的描述
	 * @throws IOException 在发生 I/O 错误时
	 * @see java.util.Properties#storeToXML(java.io.OutputStream, String)
	 */
	void storeToXml(Properties props, OutputStream os, String header) throws IOException;

	/**
	 * 将给定 Properties 对象的内容写入给定的 XML OutputStream。
	 * @param props 要存储的 Properties 对象
	 * @param os 要写入的 OutputStream
	 * @param encoding 要使用的编码
	 * @param header 属性列表的描述
	 * @throws IOException 在发生 I/O 错误时
	 * @see java.util.Properties#storeToXML(java.io.OutputStream, String, String)
	 */
	void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException;

}
