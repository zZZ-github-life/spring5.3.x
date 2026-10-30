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

import java.util.List;
import java.util.Map;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * {@code Encoder} 的扩展，公开了 HTTP 请求或响应体编码场景中
 * 相关的额外方法。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> 输入流中的元素类型
 */
public interface HttpMessageEncoder<T> extends Encoder<T> {

	/**
	 * 返回"流式"媒体类型，对于这些类型应自动执行刷新，
	 * 而不是在输入流结束时刷新。
	 */
	List<MediaType> getStreamingMediaTypes();

	/**
	 * 根据服务器请求或目标控制器方法参数上的注解获取解码提示。
	 * @param actualType 要编码的实际源类型，可能是响应式包装类型，
	 * 来源于 {@link org.springframework.core.MethodParameter}，
	 * 即提供对方法注解的访问。
	 * @param elementType 我们要编码的 {@code Flux/Mono} 中的元素类型。
	 * @param request 当前请求
	 * @param response 当前响应
	 * @return 包含提示的 Map，可能为空
	 */
	default Map<String, Object> getEncodeHints(ResolvableType actualType, ResolvableType elementType,
			@Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		return Hints.none();
	}

}
