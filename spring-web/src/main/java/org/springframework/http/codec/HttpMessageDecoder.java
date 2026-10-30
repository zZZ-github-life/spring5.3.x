/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * {@code Decoder} 的扩展，公开了 HTTP 请求或响应体解码场景中
 * 相关的额外方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 输出流中的元素类型
 */
public interface HttpMessageDecoder<T> extends Decoder<T> {

	/**
	 * 根据服务器请求或目标控制器方法参数上的注解获取解码提示。
	 * @param actualType 要解码成的实际目标类型，可能是响应式包装类型，
	 * 来源于 {@link org.springframework.core.MethodParameter}，
	 * 即提供对方法参数注解的访问
	 * @param elementType 我们要解码成的 {@code Flux/Mono} 中的元素类型
	 * @param request 当前请求
	 * @param response 当前响应
	 * @return 包含提示的 Map，可能为空
	 */
	Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
			ServerHttpRequest request, ServerHttpResponse response);

}
