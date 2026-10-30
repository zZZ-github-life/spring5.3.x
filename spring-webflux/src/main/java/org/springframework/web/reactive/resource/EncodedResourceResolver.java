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

package org.springframework.web.reactive.resource;

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

import reactor.core.publisher.Mono;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * 委托给链的解析器；如果找到资源，则尝试根据 "Accept-Encoding" 请求头查找可接受的编码（例如 gzip、brotli）变体。
 *
 * <p>可以按首选项顺序配置受支持的{@link #setContentCodings(List) contentCodings} 列表，并且每种编码都必须与{@link #setExtensions(Map) 扩展名}关联。
 *
 * <p>请注意，此解析器必须排在具有基于内容版本策略的 {@link VersionResourceResolver} 之前，以确保版本计算不受编码影响。
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
	 * 按首选项顺序配置受支持的内容编码。将使用在给定请求的 {@literal "Accept-Encoding"} 头中出现的第一个编码，并且该编码存在具有关联扩展名的文件。
	 * <p><strong>注意：</strong>每种编码都必须通过 {@link #registerExtension} 或 {@link #setExtensions} 与文件扩展名关联。此外，对此处编码列表的自定义应与 {@link CachingResourceResolver} 中相同列表的自定义相匹配，以确保资源的编码变体缓存在单独的键下。
	 * <p>默认情况下，此属性设置为 {@literal ["br", "gzip"]}。
	 * @param codings 一个或多个受支持的内容编码
	 */
	public void setContentCodings(List<String> codings) {
		Assert.notEmpty(codings, "At least one content coding expected");
		this.contentCodings.clear();
		this.contentCodings.addAll(codings);
	}

	/**
	 * 返回受支持内容编码的只读列表。
	 */
	public List<String> getContentCodings() {
		return Collections.unmodifiableList(this.contentCodings);
	}

	/**
	 * 配置内容编码到文件扩展名的映射。如果扩展名值前没有点 "."，则会添加一个点。
	 * <p>默认配置为 {@literal ["br" -> ".br"]} 和 {@literal ["gzip" -> ".gz"]}。
	 * @param extensions 要使用的扩展名。
	 * @see #registerExtension(String, String)
	 */
	public void setExtensions(Map<String, String> extensions) {
		extensions.forEach(this::registerExtension);
	}

	/**
	 * 返回编码到扩展名映射的只读映射。
	 */
	public Map<String, String> getExtensions() {
		return Collections.unmodifiableMap(this.extensions);
	}

	/**
	 * 对 Java 配置友好的 {@link #setExtensions(Map)} 替代方法。
	 * @param coding 内容编码
	 * @param extension 关联的文件扩展名
	 */
	public void registerExtension(String coding, String extension) {
		this.extensions.put(coding, (extension.startsWith(".") ? extension : "." + extension));
	}


	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations).map(resource -> {

			if (exchange == null) {
				return resource;
			}

			String acceptEncoding = getAcceptEncoding(exchange);
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
						logger.trace(exchange.getLogPrefix() +
								"No " + coding + " resource for [" + resource.getFilename() + "]", ex);
					}
				}
			}

			return resource;
		});
	}

	@Nullable
	private String getAcceptEncoding(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		String header = request.getHeaders().getFirst(HttpHeaders.ACCEPT_ENCODING);
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
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations);
	}


	/**
	 * 已编码的 {@link HttpResource}。
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
