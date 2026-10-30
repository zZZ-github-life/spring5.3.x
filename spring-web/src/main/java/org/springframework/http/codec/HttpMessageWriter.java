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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;

/**
 * 用于编码类型为 {@code <T>} 的对象流，并将编码后的字节流
 * 写入 {@link ReactiveHttpOutputMessage} 的策略。
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 5.0
 * @param <T> 输入流中的对象类型
 */
public interface HttpMessageWriter<T> {

	/**
	 * 返回此写入器支持的媒体类型列表。该列表未必适用于所有可能的
	 * 目标元素类型，对此方法的调用通常应通过
	 * {@link #canWrite(ResolvableType, MediaType) canWrite(elementType, null)}
	 * 进行保护。该列表还可能不包含仅针对特定元素类型支持的媒体类型。
	 * 或者，使用 {@link #getWritableMediaTypes(ResolvableType)} 获取更精确的列表。
	 * @return 所支持媒体类型的通用列表
	 */
	List<MediaType> getWritableMediaTypes();

	/**
	 * 返回此写入器针对给定元素类型所支持的媒体类型列表。如果写入器
	 * 不支持该元素类型，或仅支持其中的一部分媒体类型，此列表可能与
	 * {@link #getWritableMediaTypes()} 不同。
	 * @param elementType 要编码的元素类型
	 * @return 针对给定类所支持的媒体类型列表
	 * @since 5.3.4
	 */
	default List<MediaType> getWritableMediaTypes(ResolvableType elementType) {
		return (canWrite(elementType, null) ? getWritableMediaTypes() : Collections.emptyList());
	}

	/**
	 * 此写入器是否支持给定的对象类型。
	 * @param elementType 要检查的对象类型
	 * @param mediaType 用于写入的媒体类型（可能为 {@code null}）
	 * @return 如果可写则返回 {@code true}，否则返回 {@code false}
	 */
	boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType);

	/**
	 * 将给定的对象流写入输出消息。
	 * @param inputStream 要写入的对象
	 * @param elementType 流中的对象类型，且必须已通过
	 * {@link #canWrite(ResolvableType, MediaType)} 检查
	 * @param mediaType 用于写入的内容类型（可能为 {@code null}，表示必须使用写入器的默认内容类型）
	 * @param message 要写入到的消息
	 * @param hints 关于如何编码和写入的附加信息
	 * @return 表示完成或出错
	 */
	Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType elementType,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints);

	/**
	 * 仅限服务器端的替代方法，用于
	 * {@link #write(Publisher, ResolvableType, MediaType, ReactiveHttpOutputMessage, Map)}，
	 * 可提供额外的上下文。
	 * @param actualType 返回值方法的实际返回类型；对于注解控制器，可以通过
	 * {@link ResolvableType#getSource()} 访问 {@link MethodParameter}。
	 * @param elementType 输入流中的对象类型
	 * @param mediaType 要使用的内容类型（可能为 {@code null}，表示应使用写入器的默认内容类型）
	 * @param request 当前请求
	 * @param response 当前响应
	 * @return 表示写入完成或出错的 {@link Mono}
	 */
	default Mono<Void> write(Publisher<? extends T> inputStream, ResolvableType actualType,
			ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		return write(inputStream, elementType, mediaType, response, hints);
	}

}
