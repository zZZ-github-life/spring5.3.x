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

package org.springframework.web.cors;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 接收请求和 {@link CorsConfiguration} 并更新响应的策略。
 *
 * <p>此组件不负责如何选择 {@code CorsConfiguration}，而是执行后续操作，
 * 例如应用 CORS 校验检查，并拒绝响应或向响应添加 CORS 头。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.2
 * @see <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping#setCorsProcessor
 */
public interface CorsProcessor {

	/**
	 * 使用给定的 {@code CorsConfiguration} 处理请求。
	 * @param configuration the applicable CORS configuration (possibly {@code null})
	 * @param request the current request
	 * @param response the current response
	 * @return {@code false} if the request is rejected, {@code true} otherwise
	 */
	boolean processRequest(@Nullable CorsConfiguration configuration, HttpServletRequest request,
			HttpServletResponse response) throws IOException;

}
