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

package org.springframework.http.codec;

import java.time.Duration;

import org.springframework.lang.Nullable;

/**
 * 表示用于 Spring 响应式 Web 支持的 Server-Sent Event。
 * {@code Flux<ServerSentEvent>} 或 {@code Observable<ServerSentEvent>}
 * 相当于 Spring MVC 的 {@code SseEmitter} 的响应式版本。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> 此事件所包含数据的类型
 * @see ServerSentEventHttpMessageWriter
 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
 */
public final class ServerSentEvent<T> {

	@Nullable
	private final String id;

	@Nullable
	private final String event;

	@Nullable
	private final Duration retry;

	@Nullable
	private final String comment;

	@Nullable
	private final T data;


	private ServerSentEvent(@Nullable String id, @Nullable String event, @Nullable Duration retry,
			@Nullable String comment, @Nullable T data) {

		this.id = id;
		this.event = event;
		this.retry = retry;
		this.comment = comment;
		this.data = data;
	}


	/**
	 * 返回此事件的 {@code id} 字段（如果可用）。
	 */
	@Nullable
	public String id() {
		return this.id;
	}

	/**
	 * 返回此事件的 {@code event} 字段（如果可用）。
	 */
	@Nullable
	public String event() {
		return this.event;
	}

	/**
	 * 返回此事件的 {@code retry} 字段（如果可用）。
	 */
	@Nullable
	public Duration retry() {
		return this.retry;
	}

	/**
	 * 返回此事件的注释（如果可用）。
	 */
	@Nullable
	public String comment() {
		return this.comment;
	}

	/**
	 * 返回此事件的 {@code data} 字段（如果可用）。
	 */
	@Nullable
	public T data() {
		return this.data;
	}


	@Override
	public String toString() {
		return ("ServerSentEvent [id = '" + this.id + "\', event='" + this.event + "\', retry=" +
				this.retry + ", comment='" + this.comment + "', data=" + this.data + ']');
	}


	/**
	 * 返回 {@code SseEvent} 的构建器。
	 * @param <T> 此事件所包含数据的类型
	 * @return 构建器
	 */
	public static <T> Builder<T> builder() {
		return new BuilderImpl<>();
	}

	/**
	 * 返回 {@code SseEvent} 的构建器，并使用给定的{@linkplain #data() 数据}填充。
	 * @param <T> 此事件所包含数据的类型
	 * @return 构建器
	 */
	public static <T> Builder<T> builder(T data) {
		return new BuilderImpl<>(data);
	}


	/**
	 * {@code SseEvent} 的可变构建器。
	 *
	 * @param <T> 此事件所包含数据的类型
	 */
	public interface Builder<T> {

		/**
		 * 设置 {@code id} 字段的值。
		 * @param id id 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> id(String id);

		/**
		 * 设置 {@code event} 字段的值。
		 * @param event event 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> event(String event);

		/**
		 * 设置 {@code retry} 字段的值。
		 * @param retry retry 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> retry(Duration retry);

		/**
		 * 设置 SSE 注释。如果提供的是多行注释，将按照
		 * Server-Sent Events W3C 建议转换为多行 SSE 注释。
		 * @param comment 要设置的注释
		 * @return {@code this} 构建器
		 */
		Builder<T> comment(String comment);

		/**
		 * 设置 {@code data} 字段的值。如果 {@code data} 参数是多行
		 * {@code String}，将按照 Server-Sent Events W3C 建议转换为
		 * 多行 {@code data} 字段。如果 {@code data} 不是 String，
		 * 将被{@linkplain org.springframework.http.codec.json.Jackson2JsonEncoder 编码}为 JSON。
		 * @param data data 字段的值
		 * @return {@code this} 构建器
		 */
		Builder<T> data(@Nullable T data);

		/**
		 * 构建事件。
		 * @return 构建的事件
		 */
		ServerSentEvent<T> build();
	}


	private static class BuilderImpl<T> implements Builder<T> {

		@Nullable
		private String id;

		@Nullable
		private String event;

		@Nullable
		private Duration retry;

		@Nullable
		private String comment;

		@Nullable
		private T data;

		public BuilderImpl() {
		}

		public BuilderImpl(T data) {
			this.data = data;
		}

		@Override
		public Builder<T> id(String id) {
			this.id = id;
			return this;
		}

		@Override
		public Builder<T> event(String event) {
			this.event = event;
			return this;
		}

		@Override
		public Builder<T> retry(Duration retry) {
			this.retry = retry;
			return this;
		}

		@Override
		public Builder<T> comment(String comment) {
			this.comment = comment;
			return this;
		}

		@Override
		public Builder<T> data(@Nullable T data) {
			this.data = data;
			return this;
		}

		@Override
		public ServerSentEvent<T> build() {
			return new ServerSentEvent<>(this.id, this.event, this.retry, this.comment, this.data);
		}
	}

}
