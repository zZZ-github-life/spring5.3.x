/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.context.request.async;

import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 扩展 {@link NativeWebRequest}，增加了用于异步请求处理的方法。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface AsyncWebRequest extends NativeWebRequest {

	/**
	 * 设置并发处理完成所需的时间。在并发处理进行中（即 {@link #isAsyncStarted()} 为 {@code true}）时
	 * 不应设置此属性。
	 * @param timeout amount of time in milliseconds; {@code null} means no
	 * 	timeout, i.e. rely on the default timeout of the container.
	 */
	void setTimeout(@Nullable Long timeout);

	/**
	 * 添加在并发处理超时时调用的处理器。
	 */
	void addTimeoutHandler(Runnable runnable);

	/**
	 * 添加在并发处理请求期间发生错误时调用的处理器。
	 * @since 5.0
	 */
	void addErrorHandler(Consumer<Throwable> exceptionHandler);

	/**
	 * 添加在请求处理完成时调用的处理器。
	 */
	void addCompletionHandler(Runnable runnable);

	/**
	 * 标记异步请求处理的开始，以便主处理线程退出时响应仍保持打开，供其他线程继续处理。
	 * @throws IllegalStateException if async processing has completed or is not supported
	 */
	void startAsync();

	/**
	 * 返回在调用 {@link #startAsync()} 之后请求是否处于异步模式。如果异步处理从未启动、已经完成，或
	 * 请求已被分派以进一步处理，则返回 "false"。
	 */
	boolean isAsyncStarted();

	/**
	 * 将请求分派到容器，以便在应用线程中并发执行完毕后继续处理。
	 */
	void dispatch();

	/**
	 * 异步处理是否已完成。
	 */
	boolean isAsyncComplete();

}
