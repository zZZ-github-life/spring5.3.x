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

package org.springframework.web.client;

import java.io.IOException;
import java.lang.reflect.Type;

import org.springframework.http.client.ClientHttpRequest;

/**
 * 用于操作 {@link ClientHttpRequest} 的代码的回调接口。
 * 允许操作请求头并写入请求体。
 *
 * <p>由 {@link RestTemplate} 内部使用，但也适用于应用代码。
 * 提供了若干可用的工厂方法：
 * <ul>
 * <li>{@link RestTemplate#acceptHeaderRequestCallback(Class)}
 * <li>{@link RestTemplate#httpEntityCallback(Object)}
 * <li>{@link RestTemplate#httpEntityCallback(Object, Type)}
 * </ul>
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see RestTemplate#execute
 */
@FunctionalInterface
public interface RequestCallback {

	/**
	 * 由 {@link RestTemplate#execute} 在打开 {@code ClientHttpRequest} 后调用。
	 * 无需关心关闭请求或处理错误：这些都将由 {@code RestTemplate} 处理。
	 * @param request 当前活动的 HTTP 请求
	 * @throws IOException 发生 I/O 错误时
	 */
	void doWithRequest(ClientHttpRequest request) throws IOException;

}
