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

package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * {@link PropertiesPersister} 接口的默认实现。遵循 {@code java.util.Properties} 的原生解析方式。
 *
 * <p>允许从任意 Reader 读取并写入任意 Writer，例如为 properties 文件指定字符集。遗憾的是，标准
 * {@code java.util.Properties} 直到 JDK 5 才具备此能力：在此之前只能使用 ISO-8859-1 字符集加载文件。
 *
 *
 * <p>从流加载和存储到流分别委托给 {@code Properties.load} 和 {@code Properties.store}，以便与 JDK
 * Properties 类实现的 Unicode 转换完全兼容。自 JDK 6 起，{@code Properties.load/store} 也用于
 * reader/writer，实际上使此类成为一个纯粹的后向兼容适配器。
 *
 * <p>与 Reader/Writer 配合使用的持久化代码遵循 JDK 的解析策略，但不实现 Unicode 转换，因为
 * Reader/Writer 应已应用正确的字符解码/编码。如果你希望在 properties 文件中转义 Unicode 字符，请
 * <i>不要</i>为 Reader/Writer 指定编码（例如 ReloadableResourceBundleMessageSource 的
 * "defaultEncoding" 和 "fileEncodings" 属性）。
 *
 * @author Juergen Hoeller
 * @since 10.03.2004
 * @see java.util.Properties
 * @see java.util.Properties#load
 * @see java.util.Properties#store
 * @see org.springframework.core.io.support.ResourcePropertiesPersister
 */
public class DefaultPropertiesPersister implements PropertiesPersister {

	@Override
	public void load(Properties props, InputStream is) throws IOException {
		props.load(is);
	}

	@Override
	public void load(Properties props, Reader reader) throws IOException {
		props.load(reader);
	}

	@Override
	public void store(Properties props, OutputStream os, String header) throws IOException {
		props.store(os, header);
	}

	@Override
	public void store(Properties props, Writer writer, String header) throws IOException {
		props.store(writer, header);
	}

	@Override
	public void loadFromXml(Properties props, InputStream is) throws IOException {
		props.loadFromXML(is);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
		props.storeToXML(os, header);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
		props.storeToXML(os, header, encoding);
	}

}
