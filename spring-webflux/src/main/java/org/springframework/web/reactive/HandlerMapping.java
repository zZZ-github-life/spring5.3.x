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

package org.springframework.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * 定义请求与处理器对象之间映射的对象需要实现的接口。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerMapping {

	/**
	 * 包含最佳匹配模式所映射处理器的 {@link ServerWebExchange#getAttributes() 属性}名称。
	 */
	String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingHandler";

	/**
	 * 包含处理器映射中最佳匹配模式的 {@link ServerWebExchange#getAttributes() 属性}名称。
	 */
	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

	/**
	 * 在模式匹配（例如 {@code "/static/**"}）的情况下，表示处理器映射内的路径；否则表示完整的相关 URI。该名称对应 {@link ServerWebExchange#getAttributes() 属性}。
	 * <p>注意：并非所有 HandlerMapping 实现都要求支持此属性。基于 URL 的 HandlerMapping 通常会支持它，但处理器不应假定该请求属性在所有场景下都存在。
	 */
	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

	/**
	 * 包含将变量名映射到值的 URI 模板映射的 {@link ServerWebExchange#getAttributes() 属性}名称。
	 * <p>注意：并非所有 HandlerMapping 实现都要求支持此属性。基于 URL 的 HandlerMapping 通常会支持它，但处理器不应假定该请求属性在所有场景下都存在。
	 */
	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

	/**
	 * 包含 URI 变量名到对应 URI 矩阵变量 MultiValueMap 的映射的 {@link ServerWebExchange#getAttributes() 属性}名称。
	 * <p>注意：并非所有 HandlerMapping 实现都要求支持此属性，并且根据 HandlerMapping 是否配置为在请求 URI 中保留矩阵变量内容，该属性也可能不存在。
	 */
	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

	/**
	 * 包含适用于所映射处理器的可生成 MediaType 集合的 {@link ServerWebExchange#getAttributes() 属性}名称。
	 * <p>注意：并非所有 HandlerMapping 实现都要求支持此属性。处理器不应假定该请求属性在所有场景下都存在。
	 */
	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";


	/**
	 * 返回用于此请求的处理器。
	 * <p>在返回处理器之前，实现方法应检查与该处理器关联的 CORS 配置，基于该配置执行校验检查，并相应地更新响应。对于预检请求，应基于与预期实际请求相匹配的处理器执行同样的操作。
	 * @param exchange 当前的服务器交换
	 * @return {@link Mono}，发出一个值；在请求无法解析为处理器时
	 * 则不发出任何值
	 */
	Mono<Object> getHandler(ServerWebExchange exchange);

}
