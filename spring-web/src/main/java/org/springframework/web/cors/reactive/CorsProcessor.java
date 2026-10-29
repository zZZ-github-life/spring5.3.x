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

package org.springframework.web.cors.reactive;

import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;

/**
 * 基于预先选定的 {@link CorsConfiguration}，对
 * {@link ServerWebExchange} 应用 CORS 校验检查并执行更新的策略，
 * 既可以通过响应拒绝请求，也可以添加与 CORS 相关的头。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 */
public interface CorsProcessor {

	/**
	 * 使用给定的 {@code CorsConfiguration} 处理请求。
	 * @param configuration the CORS configuration to use; possibly {@code null}
	 * in which case pre-flight requests are rejected, but all others allowed.
	 * @param exchange the current exchange
	 * @return {@code false} if the request was rejected, {@code true} otherwise
	 */
	boolean process(@Nullable CorsConfiguration configuration, ServerWebExchange exchange);

}
