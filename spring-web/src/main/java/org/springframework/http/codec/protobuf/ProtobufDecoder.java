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

package org.springframework.http.codec.protobuf;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;

/**
 * 使用 <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a>
 * 读取 {@link com.google.protobuf.Message} 的 {@code Decoder}。
 *
 * <p>通过
 * {@link #decode(Publisher, ResolvableType, MimeType, Map)} 反序列化的 Flux 应使用
 * <a href="https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming">
 * 带分隔符的 Protobuf 消息</a>，每条消息的大小需在消息本身之前指定。通过
 * {@link #decodeToMono(Publisher, ResolvableType, MimeType, Map)} 反序列化的单个值
 * 应使用常规的 Protobuf 消息格式（不在消息前附加大小）。
 *
 * <p>请注意，Protobuf 消息的默认实例会产生空字节数组，
 * 因此通过网络发送的 {@code Mono.just(Msg.getDefaultInstance())}
 * 将被反序列化为空的 {@link Mono}。
 *
 * <p>要生成 {@code Message} Java 类，需要安装
 * {@code protoc} 二进制文件。
 *
 * <p>此解码器要求 Protobuf 3 或更高版本，并支持
 * {@code "application/x-protobuf"} 和 {@code "application/octet-stream"}，
 * 需使用官方的 {@code "com.google.protobuf:protobuf-java"} 库。
 *
 * @author Sebastien Deleuze
 * @since 5.1
 * @see ProtobufEncoder
 */
public class ProtobufDecoder extends ProtobufCodecSupport implements Decoder<Message> {

	/** 聚合消息的默认最大大小。 */
	protected static final int DEFAULT_MESSAGE_MAX_SIZE = 256 * 1024;

	private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();


	private final ExtensionRegistry extensionRegistry;

	private int maxMessageSize = DEFAULT_MESSAGE_MAX_SIZE;


	/**
	 * 构造一个新的 {@code ProtobufDecoder}。
	 */
	public ProtobufDecoder() {
		this(ExtensionRegistry.newInstance());
	}

	/**
	 * 构造一个新的 {@code ProtobufDecoder}，并提供一个允许
	 * 注册消息扩展的初始化器。
	 * @param extensionRegistry a message extension registry
	 */
	public ProtobufDecoder(ExtensionRegistry extensionRegistry) {
		Assert.notNull(extensionRegistry, "ExtensionRegistry must not be null");
		this.extensionRegistry = extensionRegistry;
	}


	/**
	 * 每条消息允许的最大大小。
	 * <p>默认设置为 256K。
	 * @param maxMessageSize the max size per message, or -1 for unlimited
	 */
	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	/**
	 * 返回{@link #setMaxMessageSize 已配置}的消息大小限制。
	 * @since 5.1.11
	 */
	public int getMaxMessageSize() {
		return this.maxMessageSize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
	}

	@Override
	public Flux<Message> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		MessageDecoderFunction decoderFunction =
				new MessageDecoderFunction(elementType, this.maxMessageSize);

		return Flux.from(inputStream)
				.flatMapIterable(decoderFunction)
				.doOnTerminate(decoderFunction::discard);
	}

	@Override
	public Mono<Message> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(inputStream, this.maxMessageSize)
				.map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints));
	}

	@Override
	public Message decode(DataBuffer dataBuffer, ResolvableType targetType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		try {
			Message.Builder builder = getMessageBuilder(targetType.toClass());
			ByteBuffer buffer = dataBuffer.asByteBuffer();
			builder.mergeFrom(CodedInputStream.newInstance(buffer), this.extensionRegistry);
			return builder.build();
		}
		catch (IOException ex) {
			throw new DecodingException("I/O error while parsing input stream", ex);
		}
		catch (Exception ex) {
			throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
		}
		finally {
			DataBufferUtils.release(dataBuffer);
		}
	}


	/**
	 * 为给定的类创建新的 {@code Message.Builder} 实例。
	 * <p>此方法使用 ConcurrentHashMap 缓存方法查找。
	 */
	private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
		Method method = methodCache.get(clazz);
		if (method == null) {
			method = clazz.getMethod("newBuilder");
			methodCache.put(clazz, method);
		}
		return (Message.Builder) method.invoke(clazz);
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return getMimeTypes();
	}


	private class MessageDecoderFunction implements Function<DataBuffer, Iterable<? extends Message>> {

		private final ResolvableType elementType;

		private final int maxMessageSize;

		@Nullable
		private DataBuffer output;

		private int messageBytesToRead;

		private int offset;


		public MessageDecoderFunction(ResolvableType elementType, int maxMessageSize) {
			this.elementType = elementType;
			this.maxMessageSize = maxMessageSize;
		}


		@Override
		public Iterable<? extends Message> apply(DataBuffer input) {
			try {
				List<Message> messages = new ArrayList<>();
				int remainingBytesToRead;
				int chunkBytesToRead;

				do {
					if (this.output == null) {
						if (!readMessageSize(input)) {
							return messages;
						}
						if (this.maxMessageSize > 0 && this.messageBytesToRead > this.maxMessageSize) {
							throw new DataBufferLimitException(
									"The number of bytes to read for message " +
											"(" + this.messageBytesToRead + ") exceeds " +
											"the configured limit (" + this.maxMessageSize + ")");
						}
						this.output = input.factory().allocateBuffer(this.messageBytesToRead);
					}

					chunkBytesToRead = Math.min(this.messageBytesToRead, input.readableByteCount());
					remainingBytesToRead = input.readableByteCount() - chunkBytesToRead;

					byte[] bytesToWrite = new byte[chunkBytesToRead];
					input.read(bytesToWrite, 0, chunkBytesToRead);
					this.output.write(bytesToWrite);
					this.messageBytesToRead -= chunkBytesToRead;

					if (this.messageBytesToRead == 0) {
						CodedInputStream stream = CodedInputStream.newInstance(this.output.asByteBuffer());
						DataBufferUtils.release(this.output);
						this.output = null;
						Message message = getMessageBuilder(this.elementType.toClass())
								.mergeFrom(stream, extensionRegistry)
								.build();
						messages.add(message);
					}
				} while (remainingBytesToRead > 0);
				return messages;
			}
			catch (DecodingException ex) {
				throw ex;
			}
			catch (IOException ex) {
				throw new DecodingException("I/O error while parsing input stream", ex);
			}
			catch (Exception ex) {
				throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
			}
			finally {
				DataBufferUtils.release(input);
			}
		}

		/**
		 * 从输入流中解析 varint 形式的消息大小，必要时更新 {@code messageBytesToRead} 和
		 * {@code offset} 字段，以便处理后续的数据块。
		 * 灵感来自 {@link CodedInputStream#readRawVarint32(int, java.io.InputStream)}
		 * @return {@code true} when the message size is parsed successfully, {@code false} when the message size is
		 * truncated
		 * @see <a href="https://developers.google.com/protocol-buffers/docs/encoding#varints">Base 128 Varints</a>
		 */
		private boolean readMessageSize(DataBuffer input) {
			if (this.offset == 0) {
				if (input.readableByteCount() == 0) {
					return false;
				}
				int firstByte = input.read();
				if ((firstByte & 0x80) == 0) {
					this.messageBytesToRead = firstByte;
					return true;
				}
				this.messageBytesToRead = firstByte & 0x7f;
				this.offset = 7;
			}

			if (this.offset < 32) {
				for (; this.offset < 32; this.offset += 7) {
					if (input.readableByteCount() == 0) {
						return false;
					}
					final int b = input.read();
					this.messageBytesToRead |= (b & 0x7f) << this.offset;
					if ((b & 0x80) == 0) {
						this.offset = 0;
						return true;
					}
				}
			}
			// Keep reading up to 64 bits.
			for (; this.offset < 64; this.offset += 7) {
				if (input.readableByteCount() == 0) {
					return false;
				}
				final int b = input.read();
				if ((b & 0x80) == 0) {
					this.offset = 0;
					return true;
				}
			}
			this.offset = 0;
			throw new DecodingException("Cannot parse message size: malformed varint");
		}

		public void discard() {
			if (this.output != null) {
				DataBufferUtils.release(this.output);
			}
		}
	}

}
