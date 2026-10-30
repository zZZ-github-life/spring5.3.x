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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * 基于 Web 的主题解析策略接口，既支持通过请求解析主题，
 * 也支持通过请求和响应修改主题。
 *
 * <p>该接口允许基于会话、Cookie 等实现多种策略。默认实现是
 * {@link org.springframework.web.servlet.theme.FixedThemeResolver}，
 * 它仅使用配置的默认主题。
 *
 * <p>请注意，此解析器仅负责确定当前主题名称。
 * 解析出的主题名称对应的 Theme 实例由 DispatcherServlet
 * 通过相应的 ThemeSource（即当前的 WebApplicationContext）查找。
 *
 * <p>在控制器或视图中可使用
 * {@link org.springframework.web.servlet.support.RequestContext#getTheme()}
 * 获取当前主题，而无需关心实际使用的解析策略。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see org.springframework.ui.context.Theme
 * @see org.springframework.ui.context.ThemeSource
 */
public interface ThemeResolver {

	/**
	 * 通过给定的请求解析当前主题名称。
	 * 在任何情况下都应返回默认主题作为回退。
	 * @param request 用于解析的请求
	 * @return 当前主题名称
	 */
	String resolveThemeName(HttpServletRequest request);

	/**
	 * 将当前主题名称设置为给定的名称。
	 * @param request 用于修改主题名称的请求
	 * @param response 用于修改主题名称的响应
	 * @param themeName 新的主题名称（{@code null} 或空表示重置）
	 * @throws UnsupportedOperationException 如果 ThemeResolver 实现不支持
	 * 动态更改主题
	 */
	void setThemeName(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName);

}
