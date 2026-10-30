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

package org.springframework.core.io.buffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于处理 {@link DataBuffer DataBuffers} 的工具类。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class DataBufferUtils {

	private final static Log logger = LogFactory.getLog(DataBufferUtils.class);

	private static final Consumer<DataBuffer> RELEASE_CONSUMER = DataBufferUtils::release;


	//---------------------------------------------------------------------
	// Reading
	//---------------------------------------------------------------------

	/**
	 * 从给定的 supplier 获取 {@link InputStream}，并将其读入由 {@code DataBuffer} 组成的 {@code Flux}。
	 * 当 Flux 终止时关闭该输入流。
	 * @param inputStreamSupplier 用于提供待读取输入流的提供者
	 * @param bufferFactory 用于创建数据缓冲区的工厂
	 * @param bufferSize 数据缓冲区的最大大小
	 * @return 从给定通道读取的数据缓冲区 Flux
	 */
	public static Flux<DataBuffer> readInputStream(
			Callable<InputStream> inputStreamSupplier, DataBufferFactory bufferFactory, int bufferSize) {

		Assert.notNull(inputStreamSupplier, "'inputStreamSupplier' must not be null");
		return readByteChannel(() -> Channels.newChannel(inputStreamSupplier.call()), bufferFactory, bufferSize);
	}

	/**
	 * 从给定的 supplier 获取 {@link ReadableByteChannel}，并将其读入由 {@code DataBuffer} 组成的
	 * {@code Flux}。当 Flux 终止时关闭该通道。
	 * @param channelSupplier 用于提供待读取通道的提供者
	 * @param bufferFactory 用于创建数据缓冲区的工厂
	 * @param bufferSize 数据缓冲区的最大大小
	 * @return 从给定通道读取的数据缓冲区 Flux
	 */
	public static Flux<DataBuffer> readByteChannel(
			Callable<ReadableByteChannel> channelSupplier, DataBufferFactory bufferFactory, int bufferSize) {

		Assert.notNull(channelSupplier, "'channelSupplier' must not be null");
		Assert.notNull(bufferFactory, "'dataBufferFactory' must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");

		return Flux.using(channelSupplier,
				channel -> Flux.generate(new ReadableByteChannelGenerator(channel, bufferFactory, bufferSize)),
				DataBufferUtils::closeChannel);

		// No doOnDiscard as operators used do not cache
	}

	/**
	 * 从给定的 supplier 获取 {@code AsynchronousFileChannel}，并将其读入由 {@code DataBuffer} 组成的
	 * {@code Flux}。当 Flux 终止时关闭该通道。
	 * @param channelSupplier 用于提供待读取通道的提供者
	 * @param bufferFactory 用于创建数据缓冲区的工厂
	 * @param bufferSize 数据缓冲区的最大大小
	 * @return 从给定通道读取的数据缓冲区 Flux
	 */
	public static Flux<DataBuffer> readAsynchronousFileChannel(
			Callable<AsynchronousFileChannel> channelSupplier, DataBufferFactory bufferFactory, int bufferSize) {

		return readAsynchronousFileChannel(channelSupplier, 0, bufferFactory, bufferSize);
	}

	/**
	 * 从给定的 supplier 获取 {@code AsynchronousFileChannel}，并从给定位置开始将其读入由
	 * {@code DataBuffer} 组成的 {@code Flux}。当 Flux 终止时关闭该通道。
	 * @param channelSupplier 用于提供待读取通道的提供者
	 * @param position 开始读取的位置
	 * @param bufferFactory 用于创建数据缓冲区的工厂
	 * @param bufferSize 数据缓冲区的最大大小
	 * @return 从给定通道读取的数据缓冲区 Flux
	 */
	public static Flux<DataBuffer> readAsynchronousFileChannel(
			Callable<AsynchronousFileChannel> channelSupplier, long position,
			DataBufferFactory bufferFactory, int bufferSize) {

		Assert.notNull(channelSupplier, "'channelSupplier' must not be null");
		Assert.notNull(bufferFactory, "'dataBufferFactory' must not be null");
		Assert.isTrue(position >= 0, "'position' must be >= 0");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");

		Flux<DataBuffer> flux = Flux.using(channelSupplier,
				channel -> Flux.create(sink -> {
					ReadCompletionHandler handler =
							new ReadCompletionHandler(channel, sink, position, bufferFactory, bufferSize);
					sink.onCancel(handler::cancel);
					sink.onRequest(handler::request);
				}),
				channel -> {
					// Do not close channel from here, rather wait for the current read callback
					// and then complete after releasing the DataBuffer.
				});

		return flux.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * 从给定的文件 {@code Path} 读取字节到由 {@code DataBuffer} 组成的 {@code Flux}。该方法确保在 flux
	 * 终止时关闭该文件。
	 * @param path 要读取字节的路径
	 * @param bufferFactory 用于创建数据缓冲区的工厂
	 * @param bufferSize 数据缓冲区的最大大小
	 * @return 从给定通道读取的数据缓冲区 Flux
	 * @since 5.2
	 */
	public static Flux<DataBuffer> read(
			Path path, DataBufferFactory bufferFactory, int bufferSize, OpenOption... options) {

		Assert.notNull(path, "Path must not be null");
		Assert.notNull(bufferFactory, "BufferFactory must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");
		if (options.length > 0) {
			for (OpenOption option : options) {
				Assert.isTrue(!(option == StandardOpenOption.APPEND || option == StandardOpenOption.WRITE),
						() -> "'" + option + "' not allowed");
			}
		}

		return readAsynchronousFileChannel(() -> AsynchronousFileChannel.open(path, options),
				bufferFactory, bufferSize);
	}

	/**
	 * 将给定的 {@code Resource} 读入由 {@code DataBuffer} 组成的 {@code Flux}。
	 * <p>如果该资源是文件，则将其读入 {@code AsynchronousFileChannel}，并通过
	 * {@link #readAsynchronousFileChannel(Callable, DataBufferFactory, int)} 转换为 {@code Flux}，否则
	 * 回退到 {@link #readByteChannel(Callable, DataBufferFactory, int)}。当 flux 终止时关闭该通道。
	 * @param resource 要从中读取的资源
	 * @param bufferFactory 用于创建数据缓冲区的工厂
	 * @param bufferSize 数据缓冲区的最大大小
	 * @return 从给定通道读取的数据缓冲区 Flux
	 */
	public static Flux<DataBuffer> read(Resource resource, DataBufferFactory bufferFactory, int bufferSize) {
		return read(resource, 0, bufferFactory, bufferSize);
	}

	/**
	 * 将给定的 {@code Resource} 从给定位置开始读入由 {@code DataBuffer} 组成的 {@code Flux}。
	 * <p>如果该资源是文件，则将其读入 {@code AsynchronousFileChannel}，并通过
	 * {@link #readAsynchronousFileChannel(Callable, DataBufferFactory, int)} 转换为 {@code Flux}，否则
	 * 回退到 {@link #readByteChannel(Callable, DataBufferFactory, int)}。当 flux 终止时关闭该通道。
	 * @param resource 要从中读取的资源
	 * @param position 开始读取的位置
	 * @param bufferFactory 用于创建数据缓冲区的工厂
	 * @param bufferSize 数据缓冲区的最大大小
	 * @return 从给定通道读取的数据缓冲区 Flux
	 */
	public static Flux<DataBuffer> read(
			Resource resource, long position, DataBufferFactory bufferFactory, int bufferSize) {

		try {
			if (resource.isFile()) {
				File file = resource.getFile();
				return readAsynchronousFileChannel(
						() -> AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ),
						position, bufferFactory, bufferSize);
			}
		}
		catch (IOException ignore) {
			// fallback to resource.readableChannel(), below
		}
		Flux<DataBuffer> result = readByteChannel(resource::readableChannel, bufferFactory, bufferSize);
		return position == 0 ? result : skipUntilByteCount(result, position);
	}


	//---------------------------------------------------------------------
	// Writing
	//---------------------------------------------------------------------

	/**
	 * 将给定的 {@link DataBuffer DataBuffers} 流写入给定的 {@code OutputStream}。当 flux 终止时
	 * <strong>不会</strong> 关闭输出流，也 <strong>不会</strong>
	 * {@linkplain #release(DataBuffer) release} 源中的数据缓冲区。如果需要释放，则使用
	 * {@link #releaseConsumer()} 订阅返回的 {@code Flux}。
	 * <p>请注意，写入过程只有在订阅了返回的 {@code Flux} 之后才会开始。
	 * @param source 要写入的数据缓冲区流
	 * @param outputStream 要写入的输出流
	 * @return 包含与 {@code source} 相同缓冲区的 Flux，
	 * 在订阅时启动写入过程，并发布所有
	 * 写入错误和完成信号
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source, OutputStream outputStream) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(outputStream, "'outputStream' must not be null");

		WritableByteChannel channel = Channels.newChannel(outputStream);
		return write(source, channel);
	}

	/**
	 * 将给定的 {@link DataBuffer DataBuffers} 流写入给定的 {@code WritableByteChannel}。当 flux 终止时
	 * <strong>不会</strong> 关闭该通道，也 <strong>不会</strong>
	 * {@linkplain #release(DataBuffer) release} 源中的数据缓冲区。如果需要释放，则使用
	 * {@link #releaseConsumer()} 订阅返回的 {@code Flux}。
	 * <p>请注意，写入过程只有在订阅了返回的 {@code Flux} 之后才会开始。
	 * @param source 要写入的数据缓冲区流
	 * @param channel 要写入的通道
	 * @return 包含与 {@code source} 相同缓冲区的 Flux，
	 * 在订阅时启动写入过程，并发布所有
	 * 写入错误和完成信号
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source, WritableByteChannel channel) {
		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(channel, "'channel' must not be null");

		Flux<DataBuffer> flux = Flux.from(source);
		return Flux.create(sink -> {
			WritableByteChannelSubscriber subscriber = new WritableByteChannelSubscriber(sink, channel);
			sink.onDispose(subscriber);
			flux.subscribe(subscriber);
		});
	}

	/**
	 * 将给定的 {@link DataBuffer DataBuffers} 流写入给定的 {@code AsynchronousFileChannel}。当 flux 终
	 * 止时 <strong>不会</strong> 关闭该通道，也 <strong>不会</strong>
	 * {@linkplain #release(DataBuffer) release} 源中的数据缓冲区。如果需要释放，则使用
	 * {@link #releaseConsumer()} 订阅返回的 {@code Flux}。
	 * <p>请注意，写入过程只有在订阅了返回的 {@code Flux} 之后才会开始。
	 * @param source 要写入的数据缓冲区流
	 * @param channel 要写入的通道
	 * @return 包含与 {@code source} 相同缓冲区的 Flux，
	 * 在订阅时启动写入过程，并发布所有
	 * 写入错误和完成信号
	 * @since 5.0.10
	 */
	public static Flux<DataBuffer> write(Publisher<DataBuffer> source, AsynchronousFileChannel channel) {
		return write(source, channel, 0);
	}

	/**
	 * 将给定的 {@link DataBuffer DataBuffers} 流写入给定的 {@code AsynchronousFileChannel}。当 flux 终
	 * 止时 <strong>不会</strong> 关闭该通道，也 <strong>不会</strong>
	 * {@linkplain #release(DataBuffer) release} 源中的数据缓冲区。如果需要释放，则使用
	 * {@link #releaseConsumer()} 订阅返回的 {@code Flux}。
	 * <p>请注意，写入过程只有在订阅了返回的 {@code Flux} 之后才会开始。
	 * @param source 要写入的数据缓冲区流
	 * @param channel 要写入的通道
	 * @param position 开始写入的文件位置；必须为非负数
	 * @return 包含与 {@code source} 相同缓冲区的 Flux，
	 * 在订阅时启动写入过程，并发布所有
	 * 写入错误和完成信号
	 */
	public static Flux<DataBuffer> write(
			Publisher<? extends DataBuffer> source, AsynchronousFileChannel channel, long position) {

		Assert.notNull(source, "'source' must not be null");
		Assert.notNull(channel, "'channel' must not be null");
		Assert.isTrue(position >= 0, "'position' must be >= 0");

		Flux<DataBuffer> flux = Flux.from(source);
		return Flux.create(sink -> {
			WriteCompletionHandler handler = new WriteCompletionHandler(sink, channel, position);
			sink.onDispose(handler);
			flux.subscribe(handler);
		});


	}

	/**
	 * 将给定的 {@link DataBuffer DataBuffers} 流写入给定文件 {@link Path}。可选的 {@code options} 参数
	 * 指定文件的创建或打开方式（默认为 {@link StandardOpenOption#CREATE CREATE}、
	 * {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} 和
	 * {@link StandardOpenOption#WRITE WRITE}）。
	 * @param source 要写入的数据缓冲区流
	 * @param destination 文件的路径
	 * @param options 指定文件打开方式的选项
	 * @return 表示完成或错误的 {@link Mono}
	 * @since 5.2
	 */
	public static Mono<Void> write(Publisher<DataBuffer> source, Path destination, OpenOption... options) {
		Assert.notNull(source, "Source must not be null");
		Assert.notNull(destination, "Destination must not be null");

		Set<OpenOption> optionSet = checkWriteOptions(options);

		return Mono.create(sink -> {
			try {
				AsynchronousFileChannel channel = AsynchronousFileChannel.open(destination, optionSet, null);
				sink.onDispose(() -> closeChannel(channel));
				write(source, channel).subscribe(DataBufferUtils::release,
						sink::error,
						sink::success,
						Context.of(sink.contextView()));
			}
			catch (IOException ex) {
				sink.error(ex);
			}
		});
	}

	private static Set<OpenOption> checkWriteOptions(OpenOption[] options) {
		int length = options.length;
		Set<OpenOption> result = new HashSet<>(length + 3);
		if (length == 0) {
			result.add(StandardOpenOption.CREATE);
			result.add(StandardOpenOption.TRUNCATE_EXISTING);
		}
		else {
			for (OpenOption opt : options) {
				if (opt == StandardOpenOption.READ) {
					throw new IllegalArgumentException("READ not allowed");
				}
				result.add(opt);
			}
		}
		result.add(StandardOpenOption.WRITE);
		return result;
	}

	static void closeChannel(@Nullable Channel channel) {
		if (channel != null && channel.isOpen()) {
			try {
				channel.close();
			}
			catch (IOException ignored) {
			}
		}
	}


	//---------------------------------------------------------------------
	// Various
	//---------------------------------------------------------------------

	/**
	 * 从给定的 {@link Publisher} 中转发缓冲区，直到总
	 * {@linkplain DataBuffer#readableByteCount() byte count} 达到给定的最大字节数，或者直到 publisher
	 * 完成。
	 * @param publisher 要过滤的发布者
	 * @param maxByteCount 最大字节数
	 * @return 最大字节数为 {@code maxByteCount} 的 Flux
	 */
	public static Flux<DataBuffer> takeUntilByteCount(Publisher<? extends DataBuffer> publisher, long maxByteCount) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be a positive number");

		return Flux.defer(() -> {
			AtomicLong countDown = new AtomicLong(maxByteCount);
			return Flux.from(publisher)
					.map(buffer -> {
						long remainder = countDown.addAndGet(-buffer.readableByteCount());
						if (remainder < 0) {
							int length = buffer.readableByteCount() + (int) remainder;
							return buffer.slice(0, length);
						}
						else {
							return buffer;
						}
					})
					.takeUntil(buffer -> countDown.get() <= 0);
		});

		// No doOnDiscard as operators used do not cache (and drop) buffers
	}

	/**
	 * 从给定的 {@link Publisher} 中跳过缓冲区，直到总
	 * {@linkplain DataBuffer#readableByteCount() byte count} 达到给定的最大字节数，或者直到 publisher
	 * 完成。
	 * @param publisher 要过滤的发布者
	 * @param maxByteCount 最大字节数
	 * @return 包含给定发布者剩余部分的 Flux
	 */
	public static Flux<DataBuffer> skipUntilByteCount(Publisher<? extends DataBuffer> publisher, long maxByteCount) {
		Assert.notNull(publisher, "Publisher must not be null");
		Assert.isTrue(maxByteCount >= 0, "'maxByteCount' must be a positive number");

		return Flux.defer(() -> {
			AtomicLong countDown = new AtomicLong(maxByteCount);
			return Flux.from(publisher)
					.skipUntil(buffer -> {
						long remainder = countDown.addAndGet(-buffer.readableByteCount());
						return remainder < 0;
					})
					.map(buffer -> {
						long remainder = countDown.get();
						if (remainder < 0) {
							countDown.set(0);
							int start = buffer.readableByteCount() + (int)remainder;
							int length = (int) -remainder;
							return buffer.slice(start, length);
						}
						else {
							return buffer;
						}
					});
		}).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * 如果给定的数据缓冲区是 {@link PooledDataBuffer}，则保留它。
	 * @param dataBuffer 要保留的数据缓冲区
	 * @return 保留的缓冲区
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T retain(T dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer) {
			return (T) ((PooledDataBuffer) dataBuffer).retain();
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * 如果给定的数据缓冲区是池化缓冲区并且支持泄漏跟踪，则将其与给定的提示关联起来。
	 * @param dataBuffer 要附加提示的数据缓冲区
	 * @param hint 要附加的提示
	 * @return 输入缓冲区
	 * @since 5.3.2
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T touch(T dataBuffer, Object hint) {
		if (dataBuffer instanceof PooledDataBuffer) {
			return (T) ((PooledDataBuffer) dataBuffer).touch(hint);
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * 如果给定的数据缓冲区是 {@link PooledDataBuffer} 且已被
	 * {@linkplain PooledDataBuffer#isAllocated() allocated}，则释放它。
	 * @param dataBuffer 要释放的数据缓冲区
	 * @return 如果缓冲区已释放则返回 {@code true}；否则返回 {@code false}。
	 */
	public static boolean release(@Nullable DataBuffer dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer) {
			PooledDataBuffer pooledDataBuffer = (PooledDataBuffer) dataBuffer;
			if (pooledDataBuffer.isAllocated()) {
				try {
					return pooledDataBuffer.release();
				}
				catch (IllegalStateException ex) {
					// Avoid dependency on Netty: IllegalReferenceCountException
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to release PooledDataBuffer: " + dataBuffer, ex);
					}
					return false;
				}
			}
		}
		return false;
	}

	/**
	 * 返回一个 consumer，它对所有传入的数据缓冲区调用 {@link #release(DataBuffer)}。
	 */
	public static Consumer<DataBuffer> releaseConsumer() {
		return RELEASE_CONSUMER;
	}

	/**
	 * 返回一个新的 {@code DataBuffer}，由给定的 {@code dataBuffers} 元素合并而成。根据
	 * {@link DataBuffer} 类型的不同，返回的缓冲区可能是包含所提供缓冲区全部数据的单个缓冲区，也可能是
	 * 对给定缓冲区的零拷贝复合引用。
	 * <p>如果 {@code dataBuffers} 产生错误或有取消信号，则所有已累积的缓冲区都将被
	 * {@linkplain #release(DataBuffer) released}。
	 * <p>请注意，给定的数据缓冲区 <strong>不必</strong> 被释放。它们会作为返回的复合缓冲区的一部分被释
	 * 放。
	 * @param dataBuffers 要组合的数据缓冲区
	 * @return 由 {@code dataBuffers} 参数组合而成的缓冲区
	 * @since 5.0.3
	 */
	public static Mono<DataBuffer> join(Publisher<? extends DataBuffer> dataBuffers) {
		return join(dataBuffers, -1);
	}

	/**
	 * {@link #join(Publisher)} 的变体，其行为在达到指定的最大缓冲字节数之前保持不变。一旦超过该限制，
	 * 将抛出 {@link DataBufferLimitException}。
	 * @param buffers 要组合的数据缓冲区
	 * @param maxByteCount 要缓冲的最大字节数；-1 表示不限制
	 * @return 包含聚合内容的缓冲区；如果超出要缓冲的最大字节数，
	 * 则可能是空 Mono。
	 * @throws DataBufferLimitException 如果超出 maxByteCount
	 * @since 5.1.11
	 */
	@SuppressWarnings("unchecked")
	public static Mono<DataBuffer> join(Publisher<? extends DataBuffer> buffers, int maxByteCount) {
		Assert.notNull(buffers, "'dataBuffers' must not be null");

		if (buffers instanceof Mono) {
			return (Mono<DataBuffer>) buffers;
		}

		return Flux.from(buffers)
				.collect(() -> new LimitedDataBufferList(maxByteCount), LimitedDataBufferList::add)
				.filter(list -> !list.isEmpty())
				.map(list -> list.get(0).factory().join(list))
				.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * 为给定分隔符返回一个 {@link Matcher}。该 matcher 可用于在数据缓冲区流中查找分隔符。
	 * @param delimiter 要查找的分隔符字节
	 * @return 匹配器
	 * @since 5.2
	 */
	public static Matcher matcher(byte[] delimiter) {
		return createMatcher(delimiter);
	}

	/**
	 * 为给定的多个分隔符返回一个 {@link Matcher}。该 matcher 可用于在数据缓冲区流中查找分隔符。
	 * @param delimiters 要查找的多个分隔符字节
	 * @return 匹配器
	 * @since 5.2
	 */
	public static Matcher matcher(byte[]... delimiters) {
		Assert.isTrue(delimiters.length > 0, "Delimiters must not be empty");
		return (delimiters.length == 1 ? createMatcher(delimiters[0]) : new CompositeMatcher(delimiters));
	}

	private static NestedMatcher createMatcher(byte[] delimiter) {
		Assert.isTrue(delimiter.length > 0, "Delimiter must not be empty");
		switch (delimiter.length) {
			case 1:
				return (delimiter[0] == 10 ? SingleByteMatcher.NEWLINE_MATCHER : new SingleByteMatcher(delimiter));
			case 2:
				return new TwoByteMatcher(delimiter);
			default:
				return new KnuthMorrisPrattMatcher(delimiter);
		}
	}


	/**
	 * 用于在一个或多个数据缓冲区中查找分隔符的契约，这些缓冲区可逐个传递给 {@link #match(DataBuffer)}
	 * 方法。
	 *
	 * @since 5.2
	 * @see #match(DataBuffer)
	 */
	public interface Matcher {

		/**
		 * 查找第一个匹配的分隔符，并返回该分隔符最后一个字节的索引，如果未找到则返回 {@code -1}。
		 */
		int match(DataBuffer dataBuffer);

		/**
		 * 返回最近一次调用 {@link #match(DataBuffer)} 时的分隔符。
		 */
		byte[] delimiter();

		/**
		 * 重置此 matcher 的状态。
		 */
		void reset();
	}


	/**
	 * 支持搜索多个分隔符的 Matcher。
	 */
	private static class CompositeMatcher implements Matcher {

		private static final byte[] NO_DELIMITER = new byte[0];


		private final NestedMatcher[] matchers;

		byte[] longestDelimiter = NO_DELIMITER;

		CompositeMatcher(byte[][] delimiters) {
			this.matchers = initMatchers(delimiters);
		}

		private static NestedMatcher[] initMatchers(byte[][] delimiters) {
			NestedMatcher[] matchers = new NestedMatcher[delimiters.length];
			for (int i = 0; i < delimiters.length; i++) {
				matchers[i] = createMatcher(delimiters[i]);
			}
			return matchers;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			this.longestDelimiter = NO_DELIMITER;

			for (int pos = dataBuffer.readPosition(); pos < dataBuffer.writePosition(); pos++) {
				byte b = dataBuffer.getByte(pos);

				for (NestedMatcher matcher : this.matchers) {
					if (matcher.match(b) && matcher.delimiter().length > this.longestDelimiter.length) {
						this.longestDelimiter = matcher.delimiter();
					}
				}

				if (this.longestDelimiter != NO_DELIMITER) {
					reset();
					return pos;
				}
			}
			return -1;
		}

		@Override
		public byte[] delimiter() {
			Assert.state(this.longestDelimiter != NO_DELIMITER, "'delimiter' not set");
			return this.longestDelimiter;
		}

		@Override
		public void reset() {
			for (NestedMatcher matcher : this.matchers) {
				matcher.reset();
			}
		}
	}


	/**
	 * 可嵌套在 {@link CompositeMatcher} 中的 Matcher，其中多个 matcher 使用同一索引一次一个字节地共同
	 * 推进。
	 */
	private interface NestedMatcher extends Matcher {

		/**
		 * 对流的下一字节执行匹配，如果分隔符完全匹配则返回 true。
		 */
		boolean match(byte b);

	}


	/**
	 * 单字节分隔符的 Matcher。
	 */
	private static class SingleByteMatcher implements NestedMatcher {

		static SingleByteMatcher NEWLINE_MATCHER = new SingleByteMatcher(new byte[] {10});

		private final byte[] delimiter;

		SingleByteMatcher(byte[] delimiter) {
			Assert.isTrue(delimiter.length == 1, "Expected a 1 byte delimiter");
			this.delimiter = delimiter;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			for (int pos = dataBuffer.readPosition(); pos < dataBuffer.writePosition(); pos++) {
				byte b = dataBuffer.getByte(pos);
				if (match(b)) {
					return pos;
				}
			}
			return -1;
		}

		@Override
		public boolean match(byte b) {
			return this.delimiter[0] == b;
		}

		@Override
		public byte[] delimiter() {
			return this.delimiter;
		}

		@Override
		public void reset() {
		}
	}


	/**
	 * {@link NestedMatcher} 的基类。
	 */
	private static abstract class AbstractNestedMatcher implements NestedMatcher {

		private final byte[] delimiter;

		private int matches = 0;


		protected AbstractNestedMatcher(byte[] delimiter) {
			this.delimiter = delimiter;
		}

		protected void setMatches(int index) {
			this.matches = index;
		}

		protected int getMatches() {
			return this.matches;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			for (int pos = dataBuffer.readPosition(); pos < dataBuffer.writePosition(); pos++) {
				byte b = dataBuffer.getByte(pos);
				if (match(b)) {
					reset();
					return pos;
				}
			}
			return -1;
		}

		@Override
		public boolean match(byte b) {
			if (b == this.delimiter[this.matches]) {
				this.matches++;
				return (this.matches == delimiter().length);
			}
			return false;
		}

		@Override
		public byte[] delimiter() {
			return this.delimiter;
		}

		@Override
		public void reset() {
			this.matches = 0;
		}
	}


	/**
	 * 具有 2 字节分隔符的 Matcher，不使用 Knuth-Morris-Pratt 后缀-前缀表。
	 */
	private static class TwoByteMatcher extends AbstractNestedMatcher {

		protected TwoByteMatcher(byte[] delimiter) {
			super(delimiter);
			Assert.isTrue(delimiter.length == 2, "Expected a 2-byte delimiter");
		}
	}


	/**
	 * 使用 Knuth-Morris-Pratt 算法的 {@link Matcher} 实现。
	 * @see <a href="https://www.nayuki.io/page/knuth-morris-pratt-string-matching">Knuth-Morris-Pratt string matching</a>
	 */
	private static class KnuthMorrisPrattMatcher extends AbstractNestedMatcher {

		private final int[] table;

		public KnuthMorrisPrattMatcher(byte[] delimiter) {
			super(delimiter);
			this.table = longestSuffixPrefixTable(delimiter);
		}

		private static int[] longestSuffixPrefixTable(byte[] delimiter) {
			int[] result = new int[delimiter.length];
			result[0] = 0;
			for (int i = 1; i < delimiter.length; i++) {
				int j = result[i - 1];
				while (j > 0 && delimiter[i] != delimiter[j]) {
					j = result[j - 1];
				}
				if (delimiter[i] == delimiter[j]) {
					j++;
				}
				result[i] = j;
			}
			return result;
		}

		@Override
		public boolean match(byte b) {
			while (getMatches() > 0 && b != delimiter()[getMatches()]) {
				setMatches(this.table[getMatches() - 1]);
			}
			return super.match(b);
		}
	}


	private static class ReadableByteChannelGenerator implements Consumer<SynchronousSink<DataBuffer>> {

		private final ReadableByteChannel channel;

		private final DataBufferFactory dataBufferFactory;

		private final int bufferSize;

		public ReadableByteChannelGenerator(
				ReadableByteChannel channel, DataBufferFactory dataBufferFactory, int bufferSize) {

			this.channel = channel;
			this.dataBufferFactory = dataBufferFactory;
			this.bufferSize = bufferSize;
		}

		@Override
		public void accept(SynchronousSink<DataBuffer> sink) {
			boolean release = true;
			DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer(this.bufferSize);
			try {
				int read;
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, dataBuffer.capacity());
				if ((read = this.channel.read(byteBuffer)) >= 0) {
					dataBuffer.writePosition(read);
					release = false;
					sink.next(dataBuffer);
				}
				else {
					sink.complete();
				}
			}
			catch (IOException ex) {
				sink.error(ex);
			}
			finally {
				if (release) {
					release(dataBuffer);
				}
			}
		}
	}


	private static class ReadCompletionHandler implements CompletionHandler<Integer, DataBuffer> {

		private final AsynchronousFileChannel channel;

		private final FluxSink<DataBuffer> sink;

		private final DataBufferFactory dataBufferFactory;

		private final int bufferSize;

		private final AtomicLong position;

		private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

		public ReadCompletionHandler(AsynchronousFileChannel channel,
				FluxSink<DataBuffer> sink, long position, DataBufferFactory dataBufferFactory, int bufferSize) {

			this.channel = channel;
			this.sink = sink;
			this.position = new AtomicLong(position);
			this.dataBufferFactory = dataBufferFactory;
			this.bufferSize = bufferSize;
		}

		/**
		 * 当 Reactive Streams 消费者发出需求信号时调用。
		 */
		public void request(long n) {
			tryRead();
		}

		/**
		 * 当 Reactive Streams 消费者取消时调用。
		 */
		public void cancel() {
			this.state.getAndSet(State.DISPOSED);

			// According java.nio.channels.AsynchronousChannel "if an I/O operation is outstanding
			// on the channel and the channel's close method is invoked, then the I/O operation
			// fails with the exception AsynchronousCloseException". That should invoke the failed
			// callback below and the current DataBuffer should be released.

			closeChannel(this.channel);
		}

		private void tryRead() {
			if (this.sink.requestedFromDownstream() > 0 && this.state.compareAndSet(State.IDLE, State.READING)) {
				read();
			}
		}

		private void read() {
			DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer(this.bufferSize);
			ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, this.bufferSize);
			this.channel.read(byteBuffer, this.position.get(), dataBuffer, this);
		}

		@Override
		public void completed(Integer read, DataBuffer dataBuffer) {
			if (this.state.get().equals(State.DISPOSED)) {
				release(dataBuffer);
				closeChannel(this.channel);
				return;
			}

			if (read == -1) {
				release(dataBuffer);
				closeChannel(this.channel);
				this.state.set(State.DISPOSED);
				this.sink.complete();
				return;
			}

			this.position.addAndGet(read);
			dataBuffer.writePosition(read);
			this.sink.next(dataBuffer);

			// Stay in READING mode if there is demand
			if (this.sink.requestedFromDownstream() > 0) {
				read();
				return;
			}

			// Release READING mode and then try again in case of concurrent "request"
			if (this.state.compareAndSet(State.READING, State.IDLE)) {
				tryRead();
			}
		}

		@Override
		public void failed(Throwable exc, DataBuffer dataBuffer) {
			release(dataBuffer);
			closeChannel(this.channel);
			this.state.set(State.DISPOSED);
			this.sink.error(exc);
		}

		private enum State {
			IDLE, READING, DISPOSED
		}
	}


	private static class WritableByteChannelSubscriber extends BaseSubscriber<DataBuffer> {

		private final FluxSink<DataBuffer> sink;

		private final WritableByteChannel channel;

		public WritableByteChannelSubscriber(FluxSink<DataBuffer> sink, WritableByteChannel channel) {
			this.sink = sink;
			this.channel = channel;
		}

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			request(1);
		}

		@Override
		protected void hookOnNext(DataBuffer dataBuffer) {
			try {
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
				while (byteBuffer.hasRemaining()) {
					this.channel.write(byteBuffer);
				}
				this.sink.next(dataBuffer);
				request(1);
			}
			catch (IOException ex) {
				this.sink.next(dataBuffer);
				this.sink.error(ex);
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			this.sink.error(throwable);
		}

		@Override
		protected void hookOnComplete() {
			this.sink.complete();
		}

		@Override
		public Context currentContext() {
			return Context.of(this.sink.contextView());
		}

	}


	private static class WriteCompletionHandler extends BaseSubscriber<DataBuffer>
			implements CompletionHandler<Integer, ByteBuffer> {

		private final FluxSink<DataBuffer> sink;

		private final AsynchronousFileChannel channel;

		private final AtomicBoolean completed = new AtomicBoolean();

		private final AtomicReference<Throwable> error = new AtomicReference<>();

		private final AtomicLong position;

		private final AtomicReference<DataBuffer> dataBuffer = new AtomicReference<>();

		public WriteCompletionHandler(
				FluxSink<DataBuffer> sink, AsynchronousFileChannel channel, long position) {

			this.sink = sink;
			this.channel = channel;
			this.position = new AtomicLong(position);
		}

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			request(1);
		}

		@Override
		protected void hookOnNext(DataBuffer value) {
			if (!this.dataBuffer.compareAndSet(null, value)) {
				throw new IllegalStateException();
			}
			ByteBuffer byteBuffer = value.asByteBuffer();
			this.channel.write(byteBuffer, this.position.get(), byteBuffer, this);
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			this.error.set(throwable);

			if (this.dataBuffer.get() == null) {
				this.sink.error(throwable);
			}
		}

		@Override
		protected void hookOnComplete() {
			this.completed.set(true);

			if (this.dataBuffer.get() == null) {
				this.sink.complete();
			}
		}

		@Override
		public void completed(Integer written, ByteBuffer byteBuffer) {
			long pos = this.position.addAndGet(written);
			if (byteBuffer.hasRemaining()) {
				this.channel.write(byteBuffer, pos, byteBuffer, this);
				return;
			}
			sinkDataBuffer();

			Throwable throwable = this.error.get();
			if (throwable != null) {
				this.sink.error(throwable);
			}
			else if (this.completed.get()) {
				this.sink.complete();
			}
			else {
				request(1);
			}
		}

		@Override
		public void failed(Throwable exc, ByteBuffer byteBuffer) {
			sinkDataBuffer();
			this.sink.error(exc);
		}

		private void sinkDataBuffer() {
			DataBuffer dataBuffer = this.dataBuffer.get();
			Assert.state(dataBuffer != null, "DataBuffer should not be null");
			this.sink.next(dataBuffer);
			this.dataBuffer.set(null);
		}

		@Override
		public Context currentContext() {
			return Context.of(this.sink.contextView());
		}

	}

}
