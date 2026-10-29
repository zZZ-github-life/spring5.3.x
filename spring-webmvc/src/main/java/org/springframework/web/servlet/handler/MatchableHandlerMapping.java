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

package org.springframework.web.servlet.handler;

import javax.servlet.http.HttpServletRequest;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 附加接口，{@link HandlerMapping} 可以实现此接口，以暴露
 * 与其内部请求匹配配置和实现相一致的请求匹配
 * API。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 * @see HandlerMappingIntrospector
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * 返回此 {@code HandlerMapping} 的解析器（如果已配置），
	 * 在这种情况下会使用预先解析的模式。
	 * @since 5.3
	 */
	@Nullable
	default PathPatternParser getPatternParser() {
		return null;
	}

	/**
	 * 确定请求是否匹配给定模式。当 {@link #getPatternParser()}
	 * 返回 {@code null}（表示 {@code HandlerMapping} 正在使用字符串
	 * 模式匹配）时，请使用此方法。
	 * @param request the current request
	 * @param pattern the pattern to match
	 * @return the result from request matching, or {@code null} if none
	 */
	@Nullable
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
