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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResponseBodyEmitter} 的专门化实现，用于发送
 * <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>。
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.2
 */
public class SseEmitter extends ResponseBodyEmitter {

	private static final MediaType TEXT_PLAIN = new MediaType("text", "plain", StandardCharsets.UTF_8);

	/**
	 * 创建一个新的 SseEmitter 实例。
	 */
	public SseEmitter() {
		super();
	}

	/**
	 * 创建一个带有自定义超时值的 SseEmitter。
	 * <p>默认不设置，此时将使用 MVC Java 配置或 MVC 命名空间
	 * 中配置的默认值；如果也未设置，则超时取决于
	 * 底层服务器的默认值。
	 * @param timeout 超时值，单位为毫秒
	 * @since 4.2.2
	 */
	public SseEmitter(Long timeout) {
		super(timeout);
	}


	@Override
	protected void extendResponse(ServerHttpResponse outputMessage) {
		super.extendResponse(outputMessage);

		HttpHeaders headers = outputMessage.getHeaders();
		if (headers.getContentType() == null) {
			headers.setContentType(MediaType.TEXT_EVENT_STREAM);
		}
	}

	/**
	 * 将对象格式化为单行 SSE "data" 发送。等价于：
	 * <pre>
	 * // static import of SseEmitter.*
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject));
	 * </pre>
	 * <p>有关异常处理的重要说明，请参见
	 * {@link ResponseBodyEmitter#send(Object) parent Javadoc}。
	 * @param object 要写入的对象
	 * @throws IOException 发生 I/O 错误时抛出
	 * @throws java.lang.IllegalStateException 包装任何其他错误
	 */
	@Override
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * 将对象格式化为单行 SSE "data" 发送。等价于：
	 * <pre>
	 * // static import of SseEmitter.*
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject, MediaType.APPLICATION_JSON));
	 * </pre>
	 * <p>有关异常处理的重要说明，请参见
	 * {@link ResponseBodyEmitter#send(Object) parent Javadoc}。
	 * @param object 要写入的对象
	 * @param mediaType 用于选择 HttpMessageConverter 的 MediaType 提示
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	@Override
	public void send(Object object, @Nullable MediaType mediaType) throws IOException {
		send(event().data(object, mediaType));
	}

	/**
	 * 发送使用给定构建器准备的 SSE 事件。例如：
	 * <pre>
	 * // static import of SseEmitter
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().name("update").id("1").data(myObject));
	 * </pre>
	 * @param builder 用于构建 SSE 格式事件的构建器。
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public void send(SseEventBuilder builder) throws IOException {
		Set<DataWithMediaType> dataToSend = builder.build();
		synchronized (this) {
			for (DataWithMediaType entry : dataToSend) {
				super.send(entry.getData(), entry.getMediaType());
			}
		}
	}

	@Override
	public String toString() {
		return "SseEmitter@" + ObjectUtils.getIdentityHexString(this);
	}


	public static SseEventBuilder event() {
		return new SseEventBuilderImpl();
	}


	/**
	 * SSE 事件的构建器。
	 */
	public interface SseEventBuilder {

		/**
		 * 添加 SSE "id" 行。
		 */
		SseEventBuilder id(String id);

		/**
		 * 添加 SSE "event" 行。
		 */
		SseEventBuilder name(String eventName);

		/**
		 * 添加 SSE "retry" 行。
		 */
		SseEventBuilder reconnectTime(long reconnectTimeMillis);

		/**
		 * 添加 SSE "comment" 行。
		 */
		SseEventBuilder comment(String comment);

		/**
		 * 添加 SSE "data" 行。
		 */
		SseEventBuilder data(Object object);

		/**
		 * 添加 SSE "data" 行。
		 */
		SseEventBuilder data(Object object, @Nullable MediaType mediaType);

		/**
		 * 返回一个或多个 Object-MediaType 对，以便通过
		 * {@link #send(Object, MediaType)} 写出。
		 * @since 4.2.3
		 */
		Set<DataWithMediaType> build();
	}


	/**
	 * SseEventBuilder 的默认实现。
	 */
	private static class SseEventBuilderImpl implements SseEventBuilder {

		private final Set<DataWithMediaType> dataToSend = new LinkedHashSet<>(4);

		@Nullable
		private StringBuilder sb;

		@Override
		public SseEventBuilder id(String id) {
			append("id:").append(id).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder name(String name) {
			append("event:").append(name).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
			append("retry:").append(String.valueOf(reconnectTimeMillis)).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder comment(String comment) {
			append(':').append(comment).append('\n');
			return this;
		}

		@Override
		public SseEventBuilder data(Object object) {
			return data(object, null);
		}

		@Override
		public SseEventBuilder data(Object object, @Nullable MediaType mediaType) {
			append("data:");
			saveAppendedText();
			this.dataToSend.add(new DataWithMediaType(object, mediaType));
			append('\n');
			return this;
		}

		SseEventBuilderImpl append(String text) {
			if (this.sb == null) {
				this.sb = new StringBuilder();
			}
			this.sb.append(text);
			return this;
		}

		SseEventBuilderImpl append(char ch) {
			if (this.sb == null) {
				this.sb = new StringBuilder();
			}
			this.sb.append(ch);
			return this;
		}

		@Override
		public Set<DataWithMediaType> build() {
			if (!StringUtils.hasLength(this.sb) && this.dataToSend.isEmpty()) {
				return Collections.emptySet();
			}
			append('\n');
			saveAppendedText();
			return this.dataToSend;
		}

		private void saveAppendedText() {
			if (this.sb != null) {
				this.dataToSend.add(new DataWithMediaType(this.sb.toString(), TEXT_PLAIN));
				this.sb = null;
			}
		}
	}

}
