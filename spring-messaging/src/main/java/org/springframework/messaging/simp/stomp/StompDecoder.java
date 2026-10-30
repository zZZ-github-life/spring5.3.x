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

package org.springframework.messaging.simp.stomp;

import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

/**
 * 解码 {@link ByteBuffer} 中包含的一个或多个 STOMP 帧。
 *
 * <p>会尝试从缓冲区读取所有完整的 STOMP 帧，可能为零个、一个或多个。
 * 如果末尾有任何剩余内容（即不完整的 STOMP 帧），
 * 则将缓冲区重置为指向该部分内容的开头。然后由调用方负责处理该不完整内容，
 * 即缓冲直到有更多输入可用。
 *
 * @author Andy Wilkinson
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompDecoder {

	static final byte[] HEARTBEAT_PAYLOAD = new byte[] {'\n'};

	private static final Log logger = SimpLogging.forLogName(StompDecoder.class);

	@Nullable
	private MessageHeaderInitializer headerInitializer;


	/**
	 * 配置 {@link MessageHeaderInitializer}，应用于来自已解码 STOMP 帧的
	 * {@link Message Messages} 的头。
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * 返回已配置的 {@code MessageHeaderInitializer}（如果有）。
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	/**
	 * 将给定 {@code ByteBuffer} 中的一个或多个 STOMP 帧解码为
	 * {@link Message Messages} 列表。如果输入缓冲区包含部分 STOMP 帧内容，
	 * 或包含带有部分 STOMP 帧的额外内容，则重置缓冲区并返回 {@code null}。
	 * @param byteBuffer 用于解码 STOMP 帧的缓冲区
	 * @return 解码后的消息，如果没有则为空列表
	 * @throws StompConversionException 在解码出现问题时抛出
	 */
	public List<Message<byte[]>> decode(ByteBuffer byteBuffer) {
		return decode(byteBuffer, null);
	}

	/**
	 * 从给定 {@code buffer} 中解码一个或多个 STOMP 帧，并返回
	 * {@link Message Messages} 列表。
	 * <p>如果给定的 ByteBuffer 只包含部分 STOMP 帧内容而没有完整的 STOMP 帧，
	 * 则返回空列表，并将缓冲区重置到原来的位置。
	 * <p>如果缓冲区包含一个或多个 STOMP 帧，则返回这些帧，
	 * 并将缓冲区重置为指向未使用的部分内容的开头。
	 * <p>输出 partialMessageHeaders 映射用于在内容不完整时存储成功解析的头。
	 * 然后调用方可以检查是否读取了 "content-length" 头，
	 * 这有助于确定在下一次尝试解码之前还需要多少内容。
	 * @param byteBuffer 用于解码 STOMP 帧的缓冲区
	 * @param partialMessageHeaders 一个空的输出映射，当缓冲区以不完整的 STOMP 帧结尾、
	 * 导致消息内容不完整时，用于保存最后成功解析的 partialMessageHeaders
	 * @return 解码后的消息，如果没有则为空列表
	 * @throws StompConversionException 在解码出现问题时抛出
	 */
	public List<Message<byte[]>> decode(ByteBuffer byteBuffer,
			@Nullable MultiValueMap<String, String> partialMessageHeaders) {

		List<Message<byte[]>> messages = new ArrayList<>();
		while (byteBuffer.hasRemaining()) {
			Message<byte[]> message = decodeMessage(byteBuffer, partialMessageHeaders);
			if (message != null) {
				messages.add(message);
				skipEol(byteBuffer);
				if (!byteBuffer.hasRemaining()) {
					break;
				}
			}
			else {
				break;
			}
		}
		return messages;
	}

	/**
	 * 从给定 {@code buffer} 中解码单个 STOMP 帧并转换为 {@link Message}。
	 */
	@Nullable
	private Message<byte[]> decodeMessage(ByteBuffer byteBuffer, @Nullable MultiValueMap<String, String> headers) {
		Message<byte[]> decodedMessage = null;
		skipEol(byteBuffer);

		// Explicit mark/reset access via Buffer base type for compatibility
		// with covariant return type on JDK 9's ByteBuffer...
		Buffer buffer = byteBuffer;
		buffer.mark();

		String command = readCommand(byteBuffer);
		if (command.length() > 0) {
			StompHeaderAccessor headerAccessor = null;
			byte[] payload = null;
			if (byteBuffer.remaining() > 0) {
				StompCommand stompCommand = StompCommand.valueOf(command);
				headerAccessor = StompHeaderAccessor.create(stompCommand);
				initHeaders(headerAccessor);
				readHeaders(byteBuffer, headerAccessor);
				payload = readPayload(byteBuffer, headerAccessor);
			}
			if (payload != null) {
				if (payload.length > 0) {
					StompCommand stompCommand = headerAccessor.getCommand();
					if (stompCommand != null && !stompCommand.isBodyAllowed()) {
						throw new StompConversionException(stompCommand +
								" shouldn't have a payload: length=" + payload.length + ", headers=" + headers);
					}
				}
				headerAccessor.updateSimpMessageHeadersFromStompHeaders();
				headerAccessor.setLeaveMutable(true);
				decodedMessage = MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
				if (logger.isTraceEnabled()) {
					logger.trace("Decoded " + headerAccessor.getDetailedLogMessage(payload));
				}
			}
			else {
				logger.trace("Incomplete frame, resetting input buffer...");
				if (headers != null && headerAccessor != null) {
					String name = NativeMessageHeaderAccessor.NATIVE_HEADERS;
					@SuppressWarnings("unchecked")
					MultiValueMap<String, String> map = (MultiValueMap<String, String>) headerAccessor.getHeader(name);
					if (map != null) {
						headers.putAll(map);
					}
				}
				buffer.reset();
			}
		}
		else {
			StompHeaderAccessor headerAccessor = StompHeaderAccessor.createForHeartbeat();
			initHeaders(headerAccessor);
			headerAccessor.setLeaveMutable(true);
			decodedMessage = MessageBuilder.createMessage(HEARTBEAT_PAYLOAD, headerAccessor.getMessageHeaders());
			if (logger.isTraceEnabled()) {
				logger.trace("Decoded " + headerAccessor.getDetailedLogMessage(null));
			}
		}

		return decodedMessage;
	}

	private void initHeaders(StompHeaderAccessor headerAccessor) {
		MessageHeaderInitializer initializer = getHeaderInitializer();
		if (initializer != null) {
			initializer.initHeaders(headerAccessor);
		}
	}

	/**
	 * 跳过给定 ByteBuffer 开头的一个或多个 EOL 字符。
	 * STOMP 第 2.1 节规定：“NULL 八位组后面可以选择性地跟多个 EOL。”
	 */
	protected void skipEol(ByteBuffer byteBuffer) {
		while (true) {
			if (!tryConsumeEndOfLine(byteBuffer)) {
				break;
			}
		}
	}

	private String readCommand(ByteBuffer byteBuffer) {
		ByteArrayOutputStream command = new ByteArrayOutputStream(256);
		while (byteBuffer.remaining() > 0 && !tryConsumeEndOfLine(byteBuffer)) {
			command.write(byteBuffer.get());
		}
		return StreamUtils.copyToString(command, StandardCharsets.UTF_8);
	}

	private void readHeaders(ByteBuffer byteBuffer, StompHeaderAccessor headerAccessor) {
		while (true) {
			ByteArrayOutputStream headerStream = new ByteArrayOutputStream(256);
			boolean headerComplete = false;
			while (byteBuffer.hasRemaining()) {
				if (tryConsumeEndOfLine(byteBuffer)) {
					headerComplete = true;
					break;
				}
				headerStream.write(byteBuffer.get());
			}
			if (headerStream.size() > 0 && headerComplete) {
				String header = StreamUtils.copyToString(headerStream, StandardCharsets.UTF_8);
				int colonIndex = header.indexOf(':');
				if (colonIndex <= 0) {
					if (byteBuffer.remaining() > 0) {
						throw new StompConversionException("Illegal header: '" + header +
								"'. A header must be of the form <name>:[<value>].");
					}
				}
				else {
					String headerName = unescape(header.substring(0, colonIndex));
					String headerValue = unescape(header.substring(colonIndex + 1));
					try {
						headerAccessor.addNativeHeader(headerName, headerValue);
					}
					catch (InvalidMimeTypeException ex) {
						if (byteBuffer.remaining() > 0) {
							throw ex;
						}
					}
				}
			}
			else {
				break;
			}
		}
	}

	/**
	 * 参见 STOMP 规范 1.2：
	 * <a href="https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"值编码"</a>。
	 */
	private String unescape(String inString) {
		int index = inString.indexOf('\\');
		if (index == -1) {
			return inString;
		}
		StringBuilder sb = new StringBuilder(inString.length());
		int pos = 0;  // position in the old string

		while (index >= 0) {
			sb.append(inString, pos, index);
			if (index + 1 >= inString.length()) {
				throw new StompConversionException("Illegal escape sequence at index " + index + ": " + inString);
			}
			char c = inString.charAt(index + 1);
			if (c == 'r') {
				sb.append('\r');
			}
			else if (c == 'n') {
				sb.append('\n');
			}
			else if (c == 'c') {
				sb.append(':');
			}
			else if (c == '\\') {
				sb.append('\\');
			}
			else {
				// should never happen
				throw new StompConversionException("Illegal escape sequence at index " + index + ": " + inString);
			}
			pos = index + 2;
			index = inString.indexOf('\\', pos);
		}

		sb.append(inString.substring(pos));
		return sb.toString();
	}

	@Nullable
	private byte[] readPayload(ByteBuffer byteBuffer, StompHeaderAccessor headerAccessor) {
		Integer contentLength;
		try {
			contentLength = headerAccessor.getContentLength();
		}
		catch (NumberFormatException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring invalid content-length: '" + headerAccessor);
			}
			contentLength = null;
		}

		if (contentLength != null && contentLength >= 0) {
			if (byteBuffer.remaining() > contentLength) {
				byte[] payload = new byte[contentLength];
				byteBuffer.get(payload);
				if (byteBuffer.get() != 0) {
					throw new StompConversionException("Frame must be terminated with a null octet");
				}
				return payload;
			}
			else {
				return null;
			}
		}
		else {
			ByteArrayOutputStream payload = new ByteArrayOutputStream(256);
			while (byteBuffer.remaining() > 0) {
				byte b = byteBuffer.get();
				if (b == 0) {
					return payload.toByteArray();
				}
				else {
					payload.write(b);
				}
			}
		}
		return null;
	}

	/**
	 * 尝试读取一个 EOL，如果成功则递增缓冲区位置。
	 * @return 是否消费了行尾（EOL）
	 */
	private boolean tryConsumeEndOfLine(ByteBuffer byteBuffer) {
		if (byteBuffer.remaining() > 0) {
			byte b = byteBuffer.get();
			if (b == '\n') {
				return true;
			}
			else if (b == '\r') {
				if (byteBuffer.remaining() > 0 && byteBuffer.get() == '\n') {
					return true;
				}
				else {
					throw new StompConversionException("'\\r' must be followed by '\\n'");
				}
			}
			// Explicit cast for compatibility with covariant return type on JDK 9's ByteBuffer
			((Buffer) byteBuffer).position(byteBuffer.position() - 1);
		}
		return false;
	}

}
