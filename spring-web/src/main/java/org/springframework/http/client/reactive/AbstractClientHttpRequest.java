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

package org.springframework.http.client.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 用于 {@link ClientHttpRequest} 实现的基类。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

	/**
	 * COMMITTING -> COMMITTED 是调用 doCommit 之后、但在响应状态和头部
	 * 被应用到基础响应之前的时期，在此期间提交前操作仍可以
	 * 更改响应状态和头部。
	 */
	private enum State {NEW, COMMITTING, COMMITTED}


	private final HttpHeaders headers;

	private final MultiValueMap<String, HttpCookie> cookies;

	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	private final List<Supplier<? extends Publisher<Void>>> commitActions = new ArrayList<>(4);

	@Nullable
	private HttpHeaders readOnlyHeaders;


	public AbstractClientHttpRequest() {
		this(new HttpHeaders());
	}

	public AbstractClientHttpRequest(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		this.headers = headers;
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public HttpHeaders getHeaders() {
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		}
		else if (State.COMMITTED.equals(this.state.get())) {
			this.readOnlyHeaders = initReadOnlyHeaders();
			return this.readOnlyHeaders;
		}
		else {
			return this.headers;
		}
	}

	/**
	 * 在请求提交后初始化只读头部。
	 * <p>默认情况下，此方法仅应用只读包装器。
	 * 子类可以对来自原生请求的头部执行相同操作。
	 * @since 5.3.15
	 */
	protected HttpHeaders initReadOnlyHeaders() {
		return HttpHeaders.readOnlyHttpHeaders(this.headers);
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		if (State.COMMITTED.equals(this.state.get())) {
			return CollectionUtils.unmodifiableMultiValueMap(this.cookies);
		}
		return this.cookies;
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		Assert.notNull(action, "Action must not be null");
		this.commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		return (this.state.get() != State.NEW);
	}

	/**
	 * 不带正文的请求的 {@link #doCommit(Supplier)} 变体。
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit() {
		return doCommit(null);
	}

	/**
	 * 应用 {@link #beforeCommit(Supplier) beforeCommit} 操作，应用
	 * 请求头部/Cookie，并写入请求正文。
	 * @param writeAction the action to write the request body (may be {@code null})
	 * @return a completion publisher
	 */
	protected Mono<Void> doCommit(@Nullable Supplier<? extends Publisher<Void>> writeAction) {
		if (!this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			return Mono.empty();
		}

		this.commitActions.add(() ->
				Mono.fromRunnable(() -> {
					applyHeaders();
					applyCookies();
					this.state.set(State.COMMITTED);
				}));

		if (writeAction != null) {
			this.commitActions.add(writeAction);
		}

		List<? extends Publisher<Void>> actions = this.commitActions.stream()
				.map(Supplier::get).collect(Collectors.toList());

		return Flux.concat(actions).then();
	}


	/**
	 * 将 {@link #getHeaders()} 中的头部更改应用到基础请求。
	 * 此方法仅调用一次。
	 */
	protected abstract void applyHeaders();

	/**
	 * 将 {@link #getHeaders()} 中的 Cookie 添加到基础请求。
	 * 此方法仅调用一次。
	 */
	protected abstract void applyCookies();

}
