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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 用于异步请求处理的控制器方法返回类型，
 * 应用程序可以直接向响应的 {@code OutputStream} 写入数据，
 * 而不会阻塞 Servlet 容器线程。
 *
 * <p><strong>注意：</strong>使用此选项时，强烈建议
 * 显式配置 Spring MVC 中用于执行异步请求的 TaskExecutor。
 * MVC Java 配置和 MVC 命名空间都提供了配置异步处理的选项。
 * 如果不使用这些方式，应用程序可以设置
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * RequestMappingHandlerAdapter} 的 {@code taskExecutor} 属性。
 * @author Rossen Stoyanchev
 * @since 4.2
 */
@FunctionalInterface
public interface StreamingResponseBody {

	/**
	 * 用于向响应体写入数据的回调。
	 * @param outputStream the stream for the response body
	 * @throws IOException an exception while writing
	 */
	void writeTo(OutputStream outputStream) throws IOException;

}
