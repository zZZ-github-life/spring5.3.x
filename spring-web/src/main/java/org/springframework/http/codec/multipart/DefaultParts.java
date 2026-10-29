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

package org.springframework.http.codec.multipart;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * {@link Part} 及其子类型的默认实现。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class DefaultParts {

	/**
	 * 使用给定参数创建新的 {@link FormFieldPart}。
	 * @param headers the part headers
	 * @param value the form field value
	 * @return the created part
	 */
	public static FormFieldPart formFieldPart(HttpHeaders headers, String value) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(value, "Value must not be null");

		return new DefaultFormFieldPart(headers, value);
	}

	/**
	 * 基于数据缓冲区的 Flux 创建新的 {@link Part} 或 {@link FilePart}。
	 * 如果给定头的 {@code Content-Disposition} 包含文件名，则返回
	 * {@link FilePart}，否则返回"普通"的 {@link Part}。
	 * @param headers the part headers
	 * @param dataBuffers the content of the part
	 * @return {@link Part} or {@link FilePart}, depending on {@link HttpHeaders#getContentDisposition()}
	 */
	public static Part part(HttpHeaders headers, Flux<DataBuffer> dataBuffers) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(dataBuffers, "DataBuffers must not be null");

		return partInternal(headers, new FluxContent(dataBuffers));
	}

	/**
	 * 基于给定文件创建新的 {@link Part} 或 {@link FilePart}。
	 * 如果给定头的 {@code Content-Disposition} 包含文件名，则返回
	 * {@link FilePart}，否则返回"普通"的 {@link Part}
	 * @param headers the part headers
	 * @param file  the file
	 * @param scheduler the scheduler used for reading the file
	 * @return {@link Part} or {@link FilePart}, depending on {@link HttpHeaders#getContentDisposition()}
	 */
	public static Part part(HttpHeaders headers, Path file, Scheduler scheduler) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(file, "File must not be null");
		Assert.notNull(scheduler, "Scheduler must not be null");

		return partInternal(headers, new FileContent(file, scheduler));
	}


	private static Part partInternal(HttpHeaders headers, Content content) {
		String filename = headers.getContentDisposition().getFilename();
		if (filename != null) {
			return new DefaultFilePart(headers, content);
		}
		else {
			return new DefaultPart(headers, content);
		}
	}


	/**
	 * {@link Part} 实现的抽象基类。
	 */
	private static abstract class AbstractPart implements Part {

		private final HttpHeaders headers;

		protected AbstractPart(HttpHeaders headers) {
			Assert.notNull(headers, "HttpHeaders is required");
			this.headers = headers;
		}

		@Override
		public String name() {
			String name = headers().getContentDisposition().getName();
			Assert.state(name != null, "No name available");
			return name;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}
	}


	/**
	 * {@link FormFieldPart} 的默认实现。
	 */
	private static class DefaultFormFieldPart extends AbstractPart implements FormFieldPart {

		private final String value;

		public DefaultFormFieldPart(HttpHeaders headers, String value) {
			super(headers);
			this.value = value;
		}

		@Override
		public Flux<DataBuffer> content() {
			return Flux.defer(() -> {
				byte[] bytes = this.value.getBytes(MultipartUtils.charset(headers()));
				return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
			});
		}

		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultFormFieldPart{" + name() + "}";
			}
			else {
				return "DefaultFormFieldPart";
			}
		}
	}


	/**
	 * {@link Part} 的默认实现。
	 */
	private static class DefaultPart extends AbstractPart {

		protected final Content content;

		public DefaultPart(HttpHeaders headers, Content content) {
			super(headers);
			this.content = content;
		}

		@Override
		public Flux<DataBuffer> content() {
			return this.content.content();
		}

		@Override
		public Mono<Void> delete() {
			return this.content.delete();
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultPart{" + name + "}";
			}
			else {
				return "DefaultPart";
			}
		}
	}


	/**
	 * {@link FilePart} 的默认实现。
	 */
	private static final class DefaultFilePart extends DefaultPart implements FilePart {

		public DefaultFilePart(HttpHeaders headers, Content content) {
			super(headers, content);
		}

		@Override
		public String filename() {
			String filename = headers().getContentDisposition().getFilename();
			Assert.state(filename != null, "No filename found");
			return filename;
		}

		@Override
		public Mono<Void> transferTo(Path dest) {
			return this.content.transferTo(dest);
		}

		@Override
		public String toString() {
			ContentDisposition contentDisposition = headers().getContentDisposition();
			String name = contentDisposition.getName();
			String filename = contentDisposition.getFilename();
			if (name != null) {
				return "DefaultFilePart{" + name + " (" + filename + ")}";
			}
			else {
				return "DefaultFilePart{(" + filename + ")}";
			}
		}
	}


	/**
	 * 部件内容抽象。
	 */
	private interface Content {

		Flux<DataBuffer> content();

		Mono<Void> transferTo(Path dest);

		Mono<Void> delete();
	}


	/**
	 * 基于数据缓冲区 Flux 的 {@code Content} 实现。
	 */
	private static final class FluxContent implements Content {

		private final Flux<DataBuffer> content;

		public FluxContent(Flux<DataBuffer> content) {
			this.content = content;
		}

		@Override
		public Flux<DataBuffer> content() {
			return this.content;
		}

		@Override
		public Mono<Void> transferTo(Path dest) {
			return DataBufferUtils.write(this.content, dest);
		}

		@Override
		public Mono<Void> delete() {
			return Mono.empty();
		}
	}


	/**
	 * 基于文件的 {@code Content} 实现。
	 */
	private static final class FileContent implements Content {

		private final Path file;

		private final Scheduler scheduler;

		public FileContent(Path file, Scheduler scheduler) {
			this.file = file;
			this.scheduler = scheduler;
		}

		@Override
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readByteChannel(
					() -> Files.newByteChannel(this.file, StandardOpenOption.READ),
							DefaultDataBufferFactory.sharedInstance, 1024)
					.subscribeOn(this.scheduler);
		}

		@Override
		public Mono<Void> transferTo(Path dest) {
			return blockingOperation(() -> Files.copy(this.file, dest, StandardCopyOption.REPLACE_EXISTING));
		}

		@Override
		public Mono<Void> delete() {
			return blockingOperation(() -> {
				Files.delete(this.file);
				return null;
			});
		}

		private Mono<Void> blockingOperation(Callable<?> callable) {
			return Mono.<Void>create(sink -> {
						try {
							callable.call();
							sink.success();
						}
						catch (Exception ex) {
							sink.error(ex);
						}
					})
					.subscribeOn(this.scheduler);
		}
	}

}
