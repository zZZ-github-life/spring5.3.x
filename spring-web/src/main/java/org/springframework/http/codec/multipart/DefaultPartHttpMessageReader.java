/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于将 {@code "multipart/form-data"} 请求解析为 {@link Part} 流的
 * 默认 {@code HttpMessageReader}。
 *
 * <p>在默认的非流式模式下，此消息读取器会将小于
 * {@link #setMaxInMemorySize(int) maxInMemorySize} 的部件的
 * {@linkplain Part#content() 内容}存储在内存中，而将更大的部件
 * 存储在 {@link #setFileStorageDirectory(Path) fileStorageDirectory}
 * 中的临时文件里。
 * <p>在{@linkplain #setStreaming(boolean) 流式}模式下，部件内容直接从
 * 解析后的输入缓冲区流中流式传输，既不存储在内存中，也不存储在文件中。
 *
 * <p>可以将此读取器提供给 {@link MultipartHttpMessageReader}，
 * 以便将所有部件聚合到一个 Map 中。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
public class DefaultPartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part> {

	private int maxInMemorySize = 256 * 1024;

	private int maxHeadersSize = 10 * 1024;

	private long maxDiskUsagePerPart = -1;

	private int maxParts = -1;

	private boolean streaming;

	private Scheduler blockingOperationScheduler = Schedulers.boundedElastic();

	private FileStorage fileStorage = FileStorage.tempDirectory(this::getBlockingOperationScheduler);

	private Charset headersCharset = StandardCharsets.UTF_8;


	/**
	 * 配置每个部件的头部分允许使用的最大内存量。当达到该限制时
	 * @param byteCount the maximum amount of memory for headers
	 */
	public void setMaxHeadersSize(int byteCount) {
		this.maxHeadersSize = byteCount;
	}

	/**
	 * 获取{@link #setMaxInMemorySize 已配置}的最大内存大小。
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	/**
	 * 配置每个部件允许使用的最大内存量。
	 * 超过限制时：
	 * <ul>
	 * <li>文件部件将写入临时文件。
	 * <li>非文件部件将被拒绝，并抛出 {@link DataBufferLimitException}。
	 * </ul>
	 * <p>默认设置为 256K。
	 * <p>请注意，启用{@linkplain #setStreaming(boolean) 流式}模式时，
	 * 此属性将被忽略。
	 * @param maxInMemorySize the in-memory limit in bytes; if set to -1 the entire
	 * contents will be stored in memory
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.maxInMemorySize = maxInMemorySize;
	}

	/**
	 * 配置文件部件允许使用的最大磁盘空间量。
	 * <p>默认设置为 -1，表示没有上限。
	 * <p>请注意，启用{@linkplain #setStreaming(boolean) 流式}模式时，
	 * 或将 {@link #setMaxInMemorySize(int) maxInMemorySize} 设置为 -1 时，
	 * 此属性将被忽略。
	 */
	public void setMaxDiskUsagePerPart(long maxDiskUsagePerPart) {
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
	}

	/**
	 * 指定给定 multipart 请求中允许的最大部件数。
	 * <p>默认设置为 -1，表示没有上限。
	 */
	public void setMaxParts(int maxParts) {
		this.maxParts = maxParts;
	}

	/**
	 * 设置用于存储大于
	 * {@link #setMaxInMemorySize(int) maxInMemorySize} 的部件的目录。
	 * 默认情况下，会在系统临时目录下创建名为
	 * {@code spring-webflux-multipart} 的目录。
	 * <p>请注意，启用{@linkplain #setStreaming(boolean) 流式}模式时，
	 * 或将 {@link #setMaxInMemorySize(int) maxInMemorySize} 设置为 -1 时，
	 * 此属性将被忽略。
	 * @throws IOException if an I/O error occurs, or the parent directory
	 * does not exist
	 */
	public void setFileStorageDirectory(Path fileStorageDirectory) throws IOException {
		Assert.notNull(fileStorageDirectory, "FileStorageDirectory must not be null");
		this.fileStorage = FileStorage.fromPath(fileStorageDirectory);
	}

	/**
	 * 设置用于创建文件和目录以及写入文件的 Reactor {@link Scheduler}。
	 * 默认使用 {@link Schedulers#boundedElastic()}，但此属性允许
	 * 将其改为外部管理的调度器。
	 * <p>请注意，启用{@linkplain #setStreaming(boolean) 流式}模式时，
	 * 或将 {@link #setMaxInMemorySize(int) maxInMemorySize} 设置为 -1 时，
	 * 此属性将被忽略。
	 * @see Schedulers#newBoundedElastic
	 */
	public void setBlockingOperationScheduler(Scheduler blockingOperationScheduler) {
		Assert.notNull(blockingOperationScheduler, "FileCreationScheduler must not be null");
		this.blockingOperationScheduler = blockingOperationScheduler;
	}

	private Scheduler getBlockingOperationScheduler() {
		return this.blockingOperationScheduler;
	}

	/**
	 * 当设置为 {@code true} 时，{@linkplain Part#content() 部件内容}
	 * 直接从解析后的输入缓冲区流中流式传输，既不存储在内存中，
	 * 也不存储在文件中。
	 * 当为 {@code false} 时，部件由内存和/或文件存储支持。默认为 {@code false}。
	 * <p><strong>注意</strong>：启用流式处理后，此消息读取器生成的
	 * {@code Flux<Part>} 必须按原始顺序消费，即 HTTP 消息的顺序。
	 * 此外，在移动到下一个部件之前，必须完全消费或取消
	 * {@linkplain Part#content() 正文内容}。
	 * <p>另请注意，启用此属性会实际忽略
	 * {@link #setMaxInMemorySize(int) maxInMemorySize}、
	 * {@link #setMaxDiskUsagePerPart(long) maxDiskUsagePerPart}、
	 * {@link #setFileStorageDirectory(Path) fileStorageDirectory} 和
	 * {@link #setBlockingOperationScheduler(Scheduler) fileCreationScheduler}。
	 */
	public void setStreaming(boolean streaming) {
		this.streaming = streaming;
	}

	/**
	 * 设置用于解码头的字符集。
	 * 按照 RFC 7578 默认为 UTF-8。
	 * @param headersCharset the charset to use for decoding headers
	 * @since 5.3.6
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-5.1">RFC-7578 Section 5.1</a>
	 */
	public void setHeadersCharset(Charset headersCharset) {
		Assert.notNull(headersCharset, "HeadersCharset must not be null");
		this.headersCharset = headersCharset;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return Part.class.equals(elementType.toClass()) &&
				(mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
	}

	@Override
	public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {
		return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
	}

	@Override
	public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Flux.defer(() -> {
			byte[] boundary = boundary(message);
			if (boundary == null) {
				return Flux.error(new DecodingException("No multipart boundary found in Content-Type: \"" +
						message.getHeaders().getContentType() + "\""));
			}
			Flux<MultipartParser.Token> tokens = MultipartParser.parse(message.getBody(), boundary,
					this.maxHeadersSize, this.headersCharset);

			return PartGenerator.createParts(tokens, this.maxParts, this.maxInMemorySize, this.maxDiskUsagePerPart,
					this.streaming, this.fileStorage.directory(), this.blockingOperationScheduler);
		});
	}

	@Nullable
	private byte[] boundary(HttpMessage message) {
		MediaType contentType = message.getHeaders().getContentType();
		if (contentType != null) {
			String boundary = contentType.getParameter("boundary");
			if (boundary != null) {
				int len = boundary.length();
				if (len > 2 && boundary.charAt(0) == '"' && boundary.charAt(len - 1) == '"') {
					boundary = boundary.substring(1, len - 1);
				}
				return boundary.getBytes(this.headersCharset);
			}
		}
		return null;
	}

}
