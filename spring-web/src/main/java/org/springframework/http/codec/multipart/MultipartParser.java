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

import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * 订阅缓冲区流并生成 {@link Token} 实例的 Flux。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class MultipartParser extends BaseSubscriber<DataBuffer> {

	private static final byte CR = '\r';

	private static final byte LF = '\n';

	private static final byte[] CR_LF = {CR, LF};

	private static final byte HYPHEN = '-';

	private static final byte[] TWO_HYPHENS = {HYPHEN, HYPHEN};

	private static final String HEADER_ENTRY_SEPARATOR = "\\r\\n";

	private static final Log logger = LogFactory.getLog(MultipartParser.class);

	private final AtomicReference<State> state;

	private final FluxSink<Token> sink;

	private final byte[] boundary;

	private final int maxHeadersSize;

	private final AtomicBoolean requestOutstanding = new AtomicBoolean();

	private final Charset headersCharset;


	private MultipartParser(FluxSink<Token> sink, byte[] boundary, int maxHeadersSize, Charset headersCharset) {
		this.sink = sink;
		this.boundary = boundary;
		this.maxHeadersSize = maxHeadersSize;
		this.headersCharset = headersCharset;
		this.state = new AtomicReference<>(new PreambleState());
	}

	/**
	 * 将给定的 {@link DataBuffer} 对象流解析为 {@link Token} 对象流。
	 * @param buffers the input buffers
	 * @param boundary the multipart boundary, as found in the {@code Content-Type} header
	 * @param maxHeadersSize the maximum buffered header size
	 * @param headersCharset the charset to use for decoding headers
	 * @return a stream of parsed tokens
	 */
	public static Flux<Token> parse(Flux<DataBuffer> buffers, byte[] boundary, int maxHeadersSize,
			Charset headersCharset) {
		return Flux.create(sink -> {
			MultipartParser parser = new MultipartParser(sink, boundary, maxHeadersSize, headersCharset);
			sink.onCancel(parser::onSinkCancel);
			sink.onRequest(n -> parser.requestBuffer());
			buffers.subscribe(parser);
		});
	}

	@Override
	public Context currentContext() {
		return Context.of(this.sink.contextView());
	}

	@Override
	protected void hookOnSubscribe(Subscription subscription) {
		requestBuffer();
	}

	@Override
	protected void hookOnNext(DataBuffer value) {
		this.requestOutstanding.set(false);
		this.state.get().onNext(value);
	}

	@Override
	protected void hookOnComplete() {
		this.state.get().onComplete();
	}

	@Override
	protected void hookOnError(Throwable throwable) {
		State oldState = this.state.getAndSet(DisposedState.INSTANCE);
		oldState.dispose();
		this.sink.error(throwable);
	}

	private void onSinkCancel() {
		State oldState = this.state.getAndSet(DisposedState.INSTANCE);
		oldState.dispose();
		cancel();
	}

	boolean changeState(State oldState, State newState, @Nullable DataBuffer remainder) {
		if (this.state.compareAndSet(oldState, newState)) {
			if (logger.isTraceEnabled()) {
				logger.trace("Changed state: " + oldState + " -> " + newState);
			}
			oldState.dispose();
			if (remainder != null) {
				if (remainder.readableByteCount() > 0) {
					newState.onNext(remainder);
				}
				else {
					DataBufferUtils.release(remainder);
					requestBuffer();
				}
			}
			return true;
		}
		else {
			DataBufferUtils.release(remainder);
			return false;
		}
	}

	void emitHeaders(HttpHeaders headers) {
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting headers: " + headers);
		}
		this.sink.next(new HeadersToken(headers));
	}

	void emitBody(DataBuffer buffer) {
		if (logger.isTraceEnabled()) {
			logger.trace("Emitting body: " + buffer);
		}
		this.sink.next(new BodyToken(buffer));
	}

	void emitError(Throwable t) {
		cancel();
		this.sink.error(t);
	}

	void emitComplete() {
		cancel();
		this.sink.complete();
	}

	private void requestBuffer() {
		if (upstream() != null &&
				!this.sink.isCancelled() &&
				this.sink.requestedFromDownstream() > 0 &&
				this.requestOutstanding.compareAndSet(false, true)) {
			request(1);
		}
	}


	/**
	 * 表示 {@link #parse(Flux, byte[], int, Charset)} 的输出。
	 */
	public abstract static class Token {

		public abstract HttpHeaders headers();

		public abstract DataBuffer buffer();
	}


	/**
	 * 表示包含 {@link HttpHeaders} 的 token。
	 */
	public final static class HeadersToken extends Token {

		private final HttpHeaders headers;

		public HeadersToken(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public DataBuffer buffer() {
			throw new IllegalStateException();
		}
	}


	/**
	 * 表示包含 {@link DataBuffer} 的 token。
	 */
	public final static class BodyToken extends Token {

		private final DataBuffer buffer;

		public BodyToken(DataBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public HttpHeaders headers() {
			throw new IllegalStateException();
		}

		@Override
		public DataBuffer buffer() {
			return this.buffer;
		}
	}


	/**
	 * 表示 {@link MultipartParser} 的内部状态。
	 * 格式良好的 multipart 消息的流转过程如下：
	 * <p><pre>
	 *     PREAMBLE
	 *         |
	 *         v
	 *  +-->HEADERS--->DISPOSED
	 *  |      |
	 *  |      v
	 *  +----BODY
	 *  </pre>
	 * 对于格式错误的消息，流转过程结束于 DISPOSED，当 sink 被
	 * {@linkplain #onSinkCancel() 取消}时也是如此。
	 */
	private interface State {

		void onNext(DataBuffer buf);

		void onComplete();

		default void dispose() {
		}
	}


	/**
	 * 解析器的初始状态。查找 multipart 消息的第一个边界。
	 * 请注意，第一个边界不一定以 {@code CR LF} 为前缀；只要求
	 * 前缀 {@code --}。
	 */
	private final class PreambleState implements State {

		private final DataBufferUtils.Matcher firstBoundary;


		public PreambleState() {
			this.firstBoundary = DataBufferUtils.matcher(
					MultipartUtils.concat(TWO_HYPHENS, MultipartParser.this.boundary));
		}

		/**
		 * 在给定缓冲区中查找第一个边界。如果找到，则将状态切换为
		 * {@link HeadersState}，并传递缓冲区的剩余部分。
		 */
		@Override
		public void onNext(DataBuffer buf) {
			int endIdx = this.firstBoundary.match(buf);
			if (endIdx != -1) {
				if (logger.isTraceEnabled()) {
					logger.trace("First boundary found @" + endIdx + " in " + buf);
				}
				DataBuffer headersBuf = MultipartUtils.sliceFrom(buf, endIdx);
				DataBufferUtils.release(buf);

				changeState(this, new HeadersState(), headersBuf);
			}
			else {
				DataBufferUtils.release(buf);
				requestBuffer();
			}
		}

		@Override
		public void onComplete() {
			if (changeState(this, DisposedState.INSTANCE, null)) {
				emitError(new DecodingException("Could not find first boundary"));
			}
		}

		@Override
		public String toString() {
			return "PREAMBLE";
		}

	}


	/**
	 * 解析器处理部件头的状态。将头缓冲区解析为
	 * {@link HttpHeaders} 实例，并确保数量不超过
	 * {@link #maxHeadersSize}。
	 */
	private final class HeadersState implements State {

		private final DataBufferUtils.Matcher endHeaders = DataBufferUtils.matcher(MultipartUtils.concat(CR_LF, CR_LF));

		private final AtomicInteger byteCount = new AtomicInteger();

		private final List<DataBuffer> buffers = new ArrayList<>();


		/**
		 * 首先检查导致进入此状态的 multipart 边界是否为最终边界。
		 * 然后在给定缓冲区中查找头与正文之间的边界
		 * （{@code CR LF CR LF}）。如果找到，则检查所有头缓冲区的
		 * 大小是否不超过 {@link #maxHeadersSize}，将目前收集的所有缓冲区
		 * 转换为 {@link HttpHeaders} 对象，并切换到 {@link BodyState}，
		 * 同时传递缓冲区的剩余部分。如果未找到边界，则在大小不超过
		 * {@link #maxHeadersSize} 时收集该缓冲区。
		 */
		@Override
		public void onNext(DataBuffer buf) {
			if (isLastBoundary(buf)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Last boundary found in " + buf);
				}

				if (changeState(this, DisposedState.INSTANCE, buf)) {
					emitComplete();
				}
				return;
			}
			int endIdx = this.endHeaders.match(buf);
			if (endIdx != -1) {
				if (logger.isTraceEnabled()) {
					logger.trace("End of headers found @" + endIdx + " in " + buf);
				}
				long count = this.byteCount.addAndGet(endIdx);
				if (belowMaxHeaderSize(count)) {
					DataBuffer headerBuf = MultipartUtils.sliceTo(buf, endIdx);
					this.buffers.add(headerBuf);
					DataBuffer bodyBuf = MultipartUtils.sliceFrom(buf, endIdx);
					DataBufferUtils.release(buf);

					emitHeaders(parseHeaders());
					changeState(this, new BodyState(), bodyBuf);
				}
			}
			else {
				long count = this.byteCount.addAndGet(buf.readableByteCount());
				if (belowMaxHeaderSize(count)) {
					this.buffers.add(buf);
					requestBuffer();
				}
			}
		}

		/**
		 * 如果给定缓冲区是第一个缓冲区，则检查它是否以 {@code --} 开头。
		 * 如果是第二个缓冲区，则检查它与第一个缓冲区是否共同构成 {@code --}。
		 */
		private boolean isLastBoundary(DataBuffer buf) {
			return (this.buffers.isEmpty() &&
					buf.readableByteCount() >= 2 &&
					buf.getByte(0) == HYPHEN && buf.getByte(1) == HYPHEN)
					||
					(this.buffers.size() == 1 &&
							this.buffers.get(0).readableByteCount() == 1 &&
							this.buffers.get(0).getByte(0) == HYPHEN &&
							buf.readableByteCount() >= 1 &&
							buf.getByte(0) == HYPHEN);
		}

		/**
		 * 检查给定的 {@code count} 是否小于或等于 {@link #maxHeadersSize}，
		 * 如果否则发出 {@link DataBufferLimitException}。
		 */
		private boolean belowMaxHeaderSize(long count) {
			if (count <= MultipartParser.this.maxHeadersSize) {
				return true;
			}
			else {
				emitError(new DataBufferLimitException("Part headers exceeded the memory usage limit of " +
						MultipartParser.this.maxHeadersSize + " bytes"));
				return false;
			}
		}

		/**
		 * 将缓冲区列表解析为 {@link HttpHeaders} 实例。
		 * 使用 ISO-8859-1 将合并后的缓冲区转换为字符串，并
		 * 将该字符串解析为键和值。
		 */
		private HttpHeaders parseHeaders() {
			if (this.buffers.isEmpty()) {
				return HttpHeaders.EMPTY;
			}
			DataBuffer joined = this.buffers.get(0).factory().join(this.buffers);
			this.buffers.clear();
			String string = joined.toString(MultipartParser.this.headersCharset);
			DataBufferUtils.release(joined);
			String[] lines = string.split(HEADER_ENTRY_SEPARATOR);
			HttpHeaders result = new HttpHeaders();
			for (String line : lines) {
				int idx = line.indexOf(':');
				if (idx != -1) {
					String name = line.substring(0, idx);
					String value = line.substring(idx + 1);
					while (value.startsWith(" ")) {
						value = value.substring(1);
					}
					result.add(name, value);
				}
			}
			return result;
		}

		@Override
		public void onComplete() {
			if (changeState(this, DisposedState.INSTANCE, null)) {
				emitError(new DecodingException("Could not find end of headers"));
			}
		}

		@Override
		public void dispose() {
			this.buffers.forEach(DataBufferUtils::release);
		}

		@Override
		public String toString() {
			return "HEADERS";
		}


	}


	/**
	 * 解析器处理 multipart 正文的状态。转发数据缓冲区为
	 * {@link BodyToken}，直到找到边界（更准确地说：
	 * {@code CR LF - - boundary}）。
	 */
	private final class BodyState implements State {

		private final DataBufferUtils.Matcher boundary;

		private final int boundaryLength;

		private final Deque<DataBuffer> queue = new ConcurrentLinkedDeque<>();

		public BodyState() {
			byte[] delimiter = MultipartUtils.concat(CR_LF, TWO_HYPHENS, MultipartParser.this.boundary);
			this.boundary = DataBufferUtils.matcher(delimiter);
			this.boundaryLength = delimiter.length;
		}

		/**
		 * 检查是否能在 {@code buffer} 中找到针（needle）
		 * {@code CR LF - - boundary} 的（结尾）部分。如果找到，该针可能
		 * 跨越到上一个缓冲区，因此我们计算长度并相应地切分当前缓冲区和
		 * 上一个缓冲区。然后切换到 {@link HeadersState} 并传递
		 * {@code buffer} 的剩余部分。如果未找到针，则将 {@code buffer}
		 * 设为上一个缓冲区。
		 */
		@Override
		public void onNext(DataBuffer buffer) {
			int endIdx = this.boundary.match(buffer);
			if (endIdx != -1) {
				if (logger.isTraceEnabled()) {
					logger.trace("Boundary found @" + endIdx + " in " + buffer);
				}
				int len = endIdx - buffer.readPosition() - this.boundaryLength + 1;
				if (len > 0) {
					// whole boundary in buffer.
					// slice off the body part, and flush
					DataBuffer body = buffer.retainedSlice(buffer.readPosition(), len);
					enqueue(body);
					flush();
				}
				else if (len < 0) {
					// boundary spans multiple buffers, and we've just found the end
					// iterate over buffers in reverse order
					DataBuffer prev;
					while ((prev = this.queue.pollLast()) != null) {
						int prevLen = prev.readableByteCount() + len;
						if (prevLen > 0) {
							// slice body part of previous buffer, and flush it
							DataBuffer body = prev.retainedSlice(prev.readPosition(), prevLen);
							DataBufferUtils.release(prev);
							enqueue(body);
							flush();
							break;
						}
						else {
							// previous buffer only contains boundary bytes
							DataBufferUtils.release(prev);
							len += prev.readableByteCount();
						}
					}
				}
				else /* if (len == 0) */ {
					// buffer starts with complete delimiter, flush out the previous buffers
					flush();
				}

				DataBuffer remainder = MultipartUtils.sliceFrom(buffer, endIdx);
				DataBufferUtils.release(buffer);

				changeState(this, new HeadersState(), remainder);
			}
			else {
				enqueue(buffer);
				requestBuffer();
			}
		}

		/**
		 * 存储给定的缓冲区。通过按相反顺序遍历队列并累加缓冲区大小，
		 * 发出不可能包含边界字节的缓冲区。
		 * 第一个超过边界长度的缓冲区及其后续缓冲区将被发出
		 * （按正确的非相反顺序）。
		 */
		private void enqueue(DataBuffer buf) {
			this.queue.add(buf);

			int len = 0;
			Deque<DataBuffer> emit = new ArrayDeque<>();
			for (Iterator<DataBuffer> iterator = this.queue.descendingIterator(); iterator.hasNext(); ) {
				DataBuffer previous = iterator.next();
				if (len > this.boundaryLength) {
					// addFirst to negate iterating in reverse order
					emit.addFirst(previous);
					iterator.remove();
				}
				len += previous.readableByteCount();
			}

			emit.forEach(MultipartParser.this::emitBody);
		}

		private void flush() {
			this.queue.forEach(MultipartParser.this::emitBody);
			this.queue.clear();
		}

		@Override
		public void onComplete() {
			if (changeState(this, DisposedState.INSTANCE, null)) {
				emitError(new DecodingException("Could not find end of body"));
			}
		}

		@Override
		public void dispose() {
			this.queue.forEach(DataBufferUtils::release);
			this.queue.clear();
		}

		@Override
		public String toString() {
			return "BODY";
		}
	}


	/**
	 * 解析器完成时的状态，可能是由于看到了最终边界，
	 * 也可能是由于消息格式错误。释放所有传入的缓冲区。
	 */
	private static final class DisposedState implements State {

		public static final DisposedState INSTANCE = new DisposedState();

		private DisposedState() {
		}

		@Override
		public void onNext(DataBuffer buf) {
			DataBufferUtils.release(buf);
		}

		@Override
		public void onComplete() {
		}

		@Override
		public String toString() {
			return "DISPOSED";
		}
	}


}
