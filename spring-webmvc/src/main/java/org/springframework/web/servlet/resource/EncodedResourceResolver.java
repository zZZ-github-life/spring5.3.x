/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 解析器，它委托给链，如果找到资源，则
 * 尝试根据 "Accept-Encoding" 请求头查找可接受的
 * 编码（例如 gzip、brotli）变体。
 *
 * <p>可以按优先级顺序配置受支持的 {@link #setContentCodings(List) contentCodings}
 * 列表，并且每种编码都必须与
 * {@link #setExtensions(Map) extensions} 关联。
 *
 * <p>请注意，此解析器必须排在使用基于内容的版本策略的
 * {@link VersionResourceResolver} 之前，以确保
 * 版本计算不受编码影响。
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class EncodedResourceResolver extends AbstractResourceResolver {

	/**
	 * 默认内容编码。
	 */
	public static final List<String> DEFAULT_CODINGS = Arrays.asList("br", "gzip");


	private final List<String> contentCodings = new ArrayList<>(DEFAULT_CODINGS);

	private final Map<String, String> extensions = new LinkedHashMap<>();


	public EncodedResourceResolver() {
		this.extensions.put("gzip", ".gz");
		this.extensions.put("br", ".br");
	}


	/**
	 * 按优先级顺序配置受支持的内容编码。对于给定请求，
	 * 使用 {@literal "Accept-Encoding"} 头中存在的第一种编码，
	 * 且该编码具有带关联扩展名的文件。
	 * <p><strong>注意：</strong>每种编码都必须通过 {@link #registerExtension}
	 * 或 {@link #setExtensions} 与文件扩展名关联。此外，此处编码列表的自定义
	 * 应与 {@link CachingResourceResolver} 中同一列表的自定义保持一致，
	 * 以确保资源的编码变体被分别缓存到不同的键下。
	 * <p>默认情况下此属性设置为 {@literal ["br", "gzip"]}。
	 * @param codings one or more supported content codings
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * 返回包含受支持内容编码的只读列表。
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}

	/**
	 * 配置从内容编码到文件扩展名的映射。如果扩展名值前没有点 "."，
	 * 将为其添加该点。
	 * <p>默认配置为 {@literal ["br" -> ".br"]} 和
	 * {@literal ["gzip" -> ".gz"]}。
	 * @param extensions the extensions to use.
	 * @see #registerExtension(String, String)
	 */
	public void setExtensions(Map<String, String> extensions) {
		extensions.forEach(this::registerExtension);
	}

	/**
	 * 返回包含编码到扩展名映射的只读 map。
	 */
	public Map<String, String> getExtensions() {
		return Collections.unmodifiableMap(this.extensions);
	}

	/**
	 * {@link #setExtensions(Map)} 的 Java 配置友好替代方案。
	 * @param coding the content coding
	 * @param extension the associated file extension
	 */
	public void registerExtension(String coding, String extension) {
		this.extensions.put(coding, (extension.startsWith(".") ? extension : "." + extension));
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resource = chain.resolveResource(request, requestPath, locations);
		if (resource == null || request == null) {
			return resource;
		}

		String acceptEncoding = getAcceptEncoding(request);
		if (acceptEncoding == null) {
			return resource;
		}

		for (String coding : this.contentCodings) {
			if (acceptEncoding.contains(coding)) {
				try {
					String extension = getExtension(coding);
					Resource encoded = new EncodedResource(resource, coding, extension);
					if (encoded.exists()) {
						return encoded;
					}
				}
				catch (IOException ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("No " + coding + " resource for [" + resource.getFilename() + "]", ex);
					}
				}
			}
		}

		return resource;
	}

	@Nullable
	private String getAcceptEncoding(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
		return (header != null ? header.toLowerCase() : null);
	}

	private String getExtension(String coding) {
		String extension = this.extensions.get(coding);
		if (extension == null) {
			throw new IllegalStateException("No file extension associated with content coding " + coding);
		}
		return extension;
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations);
	}


	/**
	 * 经过编码的 {@link HttpResource}。
	 */
	static final class EncodedResource extends AbstractResource implements HttpResource {

		private final Resource original;

		private final String coding;

		private final Resource encoded;

		EncodedResource(Resource original, String coding, String extension) throws IOException {
			this.original = original;
			this.coding = coding;
			this.encoded = original.createRelative(original.getFilename() + extension);
		}

		@Override
		public boolean exists() {
			return this.encoded.exists();
		}

		@Override
		public boolean isReadable() {
			return this.encoded.isReadable();
		}

		@Override
		public boolean isOpen() {
			return this.encoded.isOpen();
		}

		@Override
		public boolean isFile() {
			return this.encoded.isFile();
		}

		@Override
		public URL getURL() throws IOException {
			return this.encoded.getURL();
		}

		@Override
		public URI getURI() throws IOException {
			return this.encoded.getURI();
		}

		@Override
		public File getFile() throws IOException {
			return this.encoded.getFile();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.encoded.getInputStream();
		}

		@Override
		public ReadableByteChannel readableChannel() throws IOException {
			return this.encoded.readableChannel();
		}

		@Override
		public long contentLength() throws IOException {
			return this.encoded.contentLength();
		}

		@Override
		public long lastModified() throws IOException {
			return this.encoded.lastModified();
		}

		@Override
		public Resource createRelative(String relativePath) throws IOException {
			return this.encoded.createRelative(relativePath);
		}

		@Override
		@Nullable
		public String getFilename() {
			return this.original.getFilename();
		}

		@Override
		public String getDescription() {
			return this.encoded.getDescription();
		}

		@Override
		public HttpHeaders getResponseHeaders() {
			HttpHeaders headers;
			if (this.original instanceof HttpResource) {
				headers = ((HttpResource) this.original).getResponseHeaders();
			}
			else {
				headers = new HttpHeaders();
			}
			headers.add(HttpHeaders.CONTENT_ENCODING, this.coding);
			headers.add(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
			return headers;
		}
	}

}
