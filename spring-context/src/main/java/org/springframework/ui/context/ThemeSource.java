/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.ui.context;

import org.springframework.lang.Nullable;

/**
 * 由能够解析 {@link Theme Themes} 的对象实现的接口。它使得针对给定“主题”的消息参数化和国际化成为可
 * 能。
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see Theme
 */
public interface ThemeSource {

	/**
	 * 返回给定主题名称对应的 Theme 实例。<p>返回的 Theme 将解析特定于主题的消息、代码、文件路径等（例
	 * 如 Web 环境中的 CSS 和图像文件）。
	 * @param themeName 主题的名称
	 * @return 相应的 Theme，如果未定义则为 {@code null}。
	 * 注意，按照约定，ThemeSource 至少应能为默认主题名
	 * “theme” 返回默认 Theme，但也可以为其他主题名返回默认 Theme。
	 * @see org.springframework.web.servlet.theme.AbstractThemeResolver#ORIGINAL_DEFAULT_THEME_NAME
	 */
	@Nullable
	Theme getTheme(String themeName);

}
