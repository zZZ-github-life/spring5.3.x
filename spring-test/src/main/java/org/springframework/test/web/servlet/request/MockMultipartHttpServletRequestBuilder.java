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

package org.springframework.test.web.servlet.request;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.Part;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link MockMultipartHttpServletRequest} 的默认构建器。
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.2
 */
public class MockMultipartHttpServletRequestBuilder extends MockHttpServletRequestBuilder {

	private final List<MockMultipartFile> files = new ArrayList<>();

	private final MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();


	/**
	 * 包级私有构造函数。请使用 {@link MockMvcRequestBuilders} 中的静态工厂方法。
	 * <p>有关初始化 {@code MockMultipartHttpServletRequest} 的其他方式，
	 * 请参见 {@link #with(RequestPostProcessor)} 和 {@link RequestPostProcessor} 扩展点。
	 * @param urlTemplate a URL template; the resulting URL will be encoded
	 * @param uriVariables zero or more URI variables
	 */
	MockMultipartHttpServletRequestBuilder(String urlTemplate, Object... uriVariables) {
		this(HttpMethod.POST, urlTemplate, uriVariables);
	}

	/**
	 * {@link #MockMultipartHttpServletRequestBuilder(String, Object...)} 的变体，
	 * 还接受 {@link HttpMethod}。
	 * @since 5.3.22
	 */
	MockMultipartHttpServletRequestBuilder(HttpMethod httpMethod, String urlTemplate, Object... uriVariables) {
		super(httpMethod, urlTemplate, uriVariables);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}

	/**
	 * {@link #MockMultipartHttpServletRequestBuilder(String, Object...)} 的变体，
	 * 使用 {@link URI}。
	 * @since 4.0.3
	 */
	MockMultipartHttpServletRequestBuilder(URI uri) {
		this(HttpMethod.POST, uri);
	}

	/**
	 * {@link #MockMultipartHttpServletRequestBuilder(String, Object...)} 的变体，
	 * 使用 {@link URI} 和 {@link HttpMethod}。
	 * @since 5.3.21
	 */
	MockMultipartHttpServletRequestBuilder(HttpMethod httpMethod, URI uri) {
		super(httpMethod, uri);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}


	/**
	 * 使用给定内容创建新的 MockMultipartFile。
	 * @param name the name of the file
	 * @param content the content of the file
	 */
	public MockMultipartHttpServletRequestBuilder file(String name, byte[] content) {
		this.files.add(new MockMultipartFile(name, content));
		return this;
	}

	/**
	 * 添加给定的 MockMultipartFile。
	 * @param file the multipart file
	 */
	public MockMultipartHttpServletRequestBuilder file(MockMultipartFile file) {
		this.files.add(file);
		return this;
	}

	/**
	 * 向请求中添加 {@link Part} 组件。
	 * @param parts one or more parts to add
	 * @since 5.0
	 */
	public MockMultipartHttpServletRequestBuilder part(Part... parts) {
		Assert.notEmpty(parts, "'parts' must not be empty");
		for (Part part : parts) {
			this.parts.add(part.getName(), part);
		}
		return this;
	}

	@Override
	public Object merge(@Nullable Object parent) {
		if (parent == null) {
			return this;
		}
		if (parent instanceof MockHttpServletRequestBuilder) {
			super.merge(parent);
			if (parent instanceof MockMultipartHttpServletRequestBuilder) {
				MockMultipartHttpServletRequestBuilder parentBuilder = (MockMultipartHttpServletRequestBuilder) parent;
				this.files.addAll(parentBuilder.files);
				parentBuilder.parts.keySet().forEach(name ->
						this.parts.putIfAbsent(name, parentBuilder.parts.get(name)));
			}

		}
		else {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}
		return this;
	}

	/**
	 * 基于提供的 {@code ServletContext} 和添加到此构建器的
	 * {@code MockMultipartFiles} 创建新的 {@link MockMultipartHttpServletRequest}。
	 */
	@Override
	protected final MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest(servletContext);
		Charset defaultCharset = (request.getCharacterEncoding() != null ?
				Charset.forName(request.getCharacterEncoding()) : StandardCharsets.UTF_8);

		this.files.forEach(request::addFile);
		this.parts.values().stream().flatMap(Collection::stream).forEach(part -> {
			request.addPart(part);
			try {
				String name = part.getName();
				String filename = part.getSubmittedFileName();
				InputStream is = part.getInputStream();
				if (filename != null) {
					request.addFile(new MockMultipartFile(name, filename, part.getContentType(), is));
				}
				else {
					InputStreamReader reader = new InputStreamReader(is, getCharsetOrDefault(part, defaultCharset));
					String value = FileCopyUtils.copyToString(reader);
					request.addParameter(part.getName(), value);
				}
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to read content for part " + part.getName(), ex);
			}
		});

		return request;
	}

	private Charset getCharsetOrDefault(Part part, Charset defaultCharset) {
		if (part.getContentType() != null) {
			MediaType mediaType = MediaType.parseMediaType(part.getContentType());
			if (mediaType.getCharset() != null) {
				return mediaType.getCharset();
			}
		}
		return defaultCharset;
	}
}
