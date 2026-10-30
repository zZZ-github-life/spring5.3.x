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

package org.springframework.http.server.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link ServerHttpResponse} 实现的基类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

	/**
	 * COMMITTING -> COMMITTED 是调用 doCommit 之后、但在响应状态和头
	 * 应用到基础响应之前的时期，在此期间提交前操作仍可以更改
	 * 响应的状态和头。
	 */
	private enum State {NEW, COMMITTING, COMMIT_ACTION_FAILED, COMMITTED}


	private final DataBufferFactory dataBufferFactory;

	@Nullable
	private Integer statusCode;

	private final HttpHeaders headers;

	private final MultiValueMap<String, ResponseCookie> cookies;

	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Mono<Void>>> commitActions = new ArrayList<>(4);

	@Nullable
	private HttpHeaders readOnlyHeaders;


	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory) {
		this(dataBufferFactory, new HttpHeaders());
	}

	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory, HttpHeaders headers) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		Assert.notNull(headers, "HttpHeaders must not be null");
		this.dataBufferFactory = dataBufferFactory;
		this.headers = headers;
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public final DataBufferFactory bufferFactory() {
		return this.dataBufferFactory;
	}

	@Override
	public boolean setStatusCode(@Nullable HttpStatus status) {
		if (this.state.get() == State.COMMITTED) {
			return false;
		}
		else {
			this.statusCode = (status != null ? status.value() : null);
			return true;
		}
	}

	@Override
	@Nullable
	public HttpStatus getStatusCode() {
		return (this.statusCode != null ? HttpStatus.resolve(this.statusCode) : null);
	}

	@Override
	public boolean setRawStatusCode(@Nullable Integer statusCode) {
		if (this.state.get() == State.COMMITTED) {
			return false;
		}
		else {
			this.statusCode = statusCode;
			return true;
		}
	}

	@Override
	@Nullable
	public Integer getRawStatusCode() {
		return this.statusCode;
	}

	/**
	 * 设置响应的 HTTP 状态码。
	 * @param statusCode 作为整数值的 HTTP 状态
	 * @since 5.0.1
	 * @deprecated as of 5.2.4 in favor of {@link ServerHttpResponse#setRawStatusCode(Integer)}.
	 */
	@Deprecated
	public void setStatusCodeValue(@Nullable Integer statusCode) {
		if (this.state.get() != State.COMMITTED) {
			this.statusCode = statusCode;
		}
	}

	/**
	 * 返回响应的 HTTP 状态码。
	 * @return 作为整数值的 HTTP 状态
	 * @since 5.0.1
	 * @deprecated as of 5.2.4 in favor of {@link ServerHttpResponse#getRawStatusCode()}.
	 */
	@Nullable
	@Deprecated
	public Integer getStatusCodeValue() {
		return this.statusCode;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		}
		else if (this.state.get() == State.COMMITTED) {
			this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
			return this.readOnlyHeaders;
		}
		else {
			return this.headers;
		}
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return (this.state.get() == State.COMMITTED ?
				CollectionUtils.unmodifiableMultiValueMap(this.cookies) : this.cookies);
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");

		if (this.state.get() == State.COMMITTED) {
			throw new IllegalStateException("Can't add the cookie " + cookie +
					"because the HTTP response has already been committed");
		}
		else {
			getCookies().add(cookie.getName(), cookie);
		}
	}

	/**
	 * 返回底层的服务器响应。
	 * <p><strong>注意：</strong>主要公开此方法供框架内部使用，
	 * 例如 spring-webflux 模块中的 WebSocket 升级。
	 */
	public abstract <T> T getNativeResponse();


	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		this.commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		State state = this.state.get();
		return (state != State.NEW && state != State.COMMIT_ACTION_FAILED);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// For Mono we can avoid ChannelSendOperator and Reactor Netty is more optimized for Mono.
		// We must resolve value first however, for a chance to handle potential error.
		if (body instanceof Mono) {
			return ((Mono<? extends DataBuffer>) body)
					.flatMap(buffer -> {
						touchDataBuffer(buffer);
						AtomicBoolean subscribed = new AtomicBoolean();
						return doCommit(
								() -> {
									try {
										return writeWithInternal(Mono.fromCallable(() -> buffer)
												.doOnSubscribe(s -> subscribed.set(true))
												.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
									}
									catch (Throwable ex) {
										return Mono.error(ex);
									}
								})
								.doOnError(ex -> DataBufferUtils.release(buffer))
								.doOnCancel(() -> {
									if (!subscribed.get()) {
										DataBufferUtils.release(buffer);
									}
								});
					})
					.doOnError(t -> getHeaders().clearContentHeaders())
					.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		}
		else {
			return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeWithInternal(inner)))
					.doOnError(t -> getHeaders().clearContentHeaders());
		}
	}

	@Override
	public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeAndFlushWithInternal(inner)))
				.doOnError(t -> getHeaders().clearContentHeaders());
	}

	@Override
	public Mono<Void> setComplete() {
		return !isCommitted() ? doCommit(null) : Mono.empty();
	}

	/**
	 * {@link #doCommit(Supplier)} 的变体，用于没有正文的响应。
	 * @return 完成发布者
	 */
	protected Mono<Void> doCommit() {
		return doCommit(null);
	}

	/**
	 * 执行 {@link #beforeCommit(Supplier) beforeCommit} 操作，应用
	 * 响应状态和头/cookie，并写入响应体。
	 * @param writeAction 写入响应主体的操作（可以为 {@code null}）
	 * @return 完成发布者
	 */
	protected Mono<Void> doCommit(@Nullable Supplier<? extends Mono<Void>> writeAction) {
		Flux<Void> allActions = Flux.empty();
		if (this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			if (!this.commitActions.isEmpty()) {
				allActions = Flux.concat(Flux.fromIterable(this.commitActions).map(Supplier::get))
						.doOnError(ex -> {
							if (this.state.compareAndSet(State.COMMITTING, State.COMMIT_ACTION_FAILED)) {
								getHeaders().clearContentHeaders();
							}
						});
			}
		}
		else if (this.state.compareAndSet(State.COMMIT_ACTION_FAILED, State.COMMITTING)) {
			// Skip commit actions
		}
		else {
			return Mono.empty();
		}

		allActions = allActions.concatWith(Mono.fromRunnable(() -> {
			applyStatusCode();
			applyHeaders();
			applyCookies();
			this.state.set(State.COMMITTED);
		}));

		if (writeAction != null) {
			allActions = allActions.concatWith(writeAction.get());
		}

		return allActions.then();
	}


	/**
	 * 写入底层响应。
	 * @param body 用于写入的发布者
	 */
	protected abstract Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body);

	/**
	 * 写入底层响应，并在每个 {@code Publisher<DataBuffer>} 之后刷新。
	 * @param body 用于写入并刷新的发布者
	 */
	protected abstract Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body);

	/**
	 * 将状态码写入底层响应。
	 * 此方法只会被调用一次。
	 */
	protected abstract void applyStatusCode();

	/**
	 * 在响应即将提交时调用，允许子类向底层响应应用头值。
	 * <p>请注意，大多数子类使用一个包装了原生响应头适配器的
	 * {@link HttpHeaders} 实例，因此更改会即时传播到底层响应。
	 * 这意味着此回调通常不会用到，除非用于专门的更新，例如设置
	 * Servlet 响应中的 contentType 或 characterEncoding 字段。
	 */
	protected abstract void applyHeaders();

	/**
	 * 将 {@link #getHeaders()} 中的 cookie 添加到基础响应。
	 * 此方法只会被调用一次。
	 */
	protected abstract void applyCookies();

	/**
	 * 如果数据缓冲区是池化缓冲区并支持泄漏跟踪，
	 * 允许子类为其关联一个提示。
	 * @param buffer 要附加提示的缓冲区
	 * @since 5.3.2
	 */
	protected void touchDataBuffer(DataBuffer buffer) {
	}

}
