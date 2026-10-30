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

import org.springframework.context.MessageSource;

/**
 * Theme 可以解析特定于主题的消息、代码、文件路径等（例如 Web 环境中的 CSS 和图像文件）。所暴露的
 * {@link org.springframework.context.MessageSource} 支持特定于主题的参数化和国际化。
 *
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see ThemeSource
 * @see org.springframework.web.servlet.ThemeResolver
 */
public interface Theme {

	/**
	 * 返回主题的名称。
	 * @return 主题的名称（永不为 {@code null}）
	 */
	String getName();

	/**
	 * 返回针对此主题解析消息的特定 MessageSource。
	 * @return 特定于该主题的 MessageSource（永不为 {@code null}）
	 */
	MessageSource getMessageSource();

}
