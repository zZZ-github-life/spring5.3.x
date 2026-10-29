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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 用于检索和保存 FlashMap 实例的策略接口。
 * 有关 flash 属性的总体概述，请参见 {@link FlashMap}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see FlashMap
 */
public interface FlashMapManager {

	/**
	 * 查找由先前请求保存的、与当前请求匹配的 FlashMap，
	 * 将其从底层存储中移除，同时移除其他已过期的 FlashMap 实例。
	 * <p>与 {@link #saveOutputFlashMap} 不同（后者仅在有需要保存的
	 * flash 属性时调用 —— 即重定向之前），此方法在每个请求的
	 * 开始时都会被调用。
	 * @param request the current request
	 * @param response the current response
	 * @return a FlashMap matching the current request or {@code null}
	 */
	@Nullable
	FlashMap retrieveAndUpdate(HttpServletRequest request, HttpServletResponse response);

	/**
	 * 将给定的 FlashMap 保存到某个底层存储中，并设置其过期计时的开始时间。
	 * <p><strong>注意：</strong>请在重定向之前调用此方法，
	 * 以便在响应提交之前将 FlashMap 保存到 HTTP 会话或响应 Cookie 中。
	 * @param flashMap the FlashMap to save
	 * @param request the current request
	 * @param response the current response
	 */
	void saveOutputFlashMap(FlashMap flashMap, HttpServletRequest request, HttpServletResponse response);

}
