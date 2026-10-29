/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.http.server;

/**
 * 一种控制接口，可以将 HTTP 请求的处理置于异步模式，
 * 在此期间响应保持打开状态，直到被显式关闭。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ServerHttpAsyncRequestControl {

	/**
	 * 启用异步处理，此后响应将保持打开状态，直到调用
	 * {@link #complete()} 或服务器使请求超时。一旦启用，
	 * 对此方法的后续调用将被忽略。
	 */
	void start();

	/**
	 * {@link #start()} 的变体，允许指定异步处理使用的超时值。
	 * 如果在指定时间内未调用 {@link #complete()}，请求将超时。
	 */
	void start(long timeout);

	/**
	 * 返回是否已启动异步请求处理。
	 */
	boolean isStarted();

	/**
	 * 将异步请求处理标记为已完成。
	 */
	void complete();

	/**
	 * 返回异步请求处理是否已完成。
	 */
	boolean isCompleted();

}
