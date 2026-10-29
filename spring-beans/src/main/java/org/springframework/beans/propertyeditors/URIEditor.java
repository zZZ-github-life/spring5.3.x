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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * 用于 {@code java.net.URI} 的编辑器，可直接填充 URI 属性，而无需使用 String 属性作为桥接。
 *
 * <p>支持 Spring 风格的 URI 表示法：任何完全限定的标准 URI（"file:"、"http:" 等）以及 Spring 特殊
 * 的 "classpath:" 伪 URL，后者将被解析为相应的 URI。
 *
 * <p>默认情况下，此编辑器会将 String 编码为 URI。例如，空格将被编码为 {@code %20}。可以通过调用
 * {@link #URIEditor(boolean)} 构造函数更改此行为。
 *
 * <p>注意：URI 比 URL 更宽松，因为它不要求指定有效的协议。允许使用有效 URI 语法中的任何 scheme，即
 * 使未注册匹配的协议处理器也是如此。
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see java.net.URI
 * @see URLEditor
 */
public class URIEditor extends PropertyEditorSupport {

	@Nullable
	private final ClassLoader classLoader;

	private final boolean encode;



	/**
	 * 创建一个新的编码 URIEditor，将 "classpath:" 位置转换为标准 URI（不尝试将其解析为物理资源）。
	 */
	public URIEditor() {
		this(true);
	}

	/**
	 * 创建一个新的 URIEditor，将 "classpath:" 位置转换为标准 URI（不尝试将其解析为物理资源）。
	 * @param encode indicates whether Strings will be encoded or not
	 * @since 3.0
	 */
	public URIEditor(boolean encode) {
		this.classLoader = null;
		this.encode = encode;
	}

	/**
	 * 创建一个新的 URIEditor，使用给定的 ClassLoader 将 "classpath:" 位置解析为物理资源 URL。
	 * @param classLoader the ClassLoader to use for resolving "classpath:" locations
	 * (may be {@code null} to indicate the default ClassLoader)
	 */
	public URIEditor(@Nullable ClassLoader classLoader) {
		this(classLoader, true);
	}

	/**
	 * 创建一个新的 URIEditor，使用给定的 ClassLoader 将 "classpath:" 位置解析为物理资源 URL。
	 * @param classLoader the ClassLoader to use for resolving "classpath:" locations
	 * (may be {@code null} to indicate the default ClassLoader)
	 * @param encode indicates whether Strings will be encoded or not
	 * @since 3.0
	 */
	public URIEditor(@Nullable ClassLoader classLoader, boolean encode) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.encode = encode;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			String uri = text.trim();
			if (this.classLoader != null && uri.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
				ClassPathResource resource = new ClassPathResource(
						uri.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length()), this.classLoader);
				try {
					setValue(resource.getURI());
				}
				catch (IOException ex) {
					throw new IllegalArgumentException("Could not retrieve URI for " + resource + ": " + ex.getMessage());
				}
			}
			else {
				try {
					setValue(createURI(uri));
				}
				catch (URISyntaxException ex) {
					throw new IllegalArgumentException("Invalid URI syntax: " + ex.getMessage());
				}
			}
		}
		else {
			setValue(null);
		}
	}

	/**
	 * 为给定的用户指定 String 值创建一个 URI 实例。
	 * <p>默认实现将该值编码为符合 RFC-2396 的 URI。
	 * @param value the value to convert into a URI instance
	 * @return the URI instance
	 * @throws java.net.URISyntaxException if URI conversion failed
	 */
	protected URI createURI(String value) throws URISyntaxException {
		int colonIndex = value.indexOf(':');
		if (this.encode && colonIndex != -1) {
			int fragmentIndex = value.indexOf('#', colonIndex + 1);
			String scheme = value.substring(0, colonIndex);
			String ssp = value.substring(colonIndex + 1, (fragmentIndex > 0 ? fragmentIndex : value.length()));
			String fragment = (fragmentIndex > 0 ? value.substring(fragmentIndex + 1) : null);
			return new URI(scheme, ssp, fragment);
		}
		else {
			// not encoding or the value contains no scheme - fallback to default
			return new URI(value);
		}
	}


	@Override
	public String getAsText() {
		URI value = (URI) getValue();
		return (value != null ? value.toString() : "");
	}

}
