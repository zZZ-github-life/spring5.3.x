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

package org.springframework.web.servlet.tags;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * {@code <theme>} 标签在当前页面的作用域内查找主题消息。
 * 消息使用 ApplicationContext 的 ThemeSource 进行查找，
 * 因此应支持国际化。
 *
 * <p>会考虑 HTML 转义设置，该设置可以在此标签实例上、
 * 页面级或 web.xml 级指定。
 *
 * <p>如果 "code" 未设置或无法解析，则将 "text" 用作
 * 默认消息。
 *
 * <p>消息参数可以通过 {@link #setArguments(Object)
 * arguments} 属性指定，也可以通过嵌套的 {@code <spring:argument>} 标签指定。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需？</th>
 * <th>是否支持运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>arguments</td>
 * <td>false</td>
 * <td>true</td>
 * <td>为此标签设置可选的消息参数，可以是（逗号）分隔的
 * String（每个 String 参数可包含 JSP EL）、Object 数组（用作
 * 参数数组），或单个 Object（用作单个参数）。</td>
 * </tr>
 * <tr>
 * <td>argumentSeparator</td>
 * <td>false</td>
 * <td>true</td>
 * <td>用于拆分 arguments 字符串值的分隔符；
 * 默认为 'comma'（','）。</td>
 * </tr>
 * <tr>
 * <td>code</td>
 * <td>false</td>
 * <td>true</td>
 * <td>查找消息时使用的代码（键）。如果未提供 code，
 * 则将使用 text 属性。</td>
 * </tr>
 * <tr>
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 HTML 转义。覆盖当前页面的默认
 * HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 JavaScript 转义。
 * 默认为 {@code false}。</td>
 * </tr>
 * <tr>
 * <td>message</td>
 * <td>false</td>
 * <td>true</td>
 * <td>一个 MessageSourceResolvable 参数（直接指定或通过 JSP EL）。</td>
 * </tr>
 * <tr>
 * <td>scope</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果导出到变量时使用的作用域。此属性
 * 仅在同时设置 var 时使用。可能的值为 page、request、session
 * 和 application。</td>
 * </tr>
 * <tr>
 * <td>text</td>
 * <td>false</td>
 * <td>true</td>
 * <td>当找不到给定代码对应的消息时输出的默认文本。
 * 如果 text 和 code 都未设置，该标签将输出 null。</td>
 * </tr>
 * <tr>
 * <td>var</td>
 * <td>false</td>
 * <td>true</td>
 * <td>将结果绑定到 page、request、session 或 application 作用域时
 * 使用的字符串。如果未指定，结果将输出到
 * writer（即通常直接输出到 JSP）。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see org.springframework.ui.context.Theme
 * @see org.springframework.ui.context.ThemeSource
 * @see #setCode
 * @see #setText
 * @see #setHtmlEscape
 * @see HtmlEscapeTag#setDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#HTML_ESCAPE_CONTEXT_PARAM
 * @see ArgumentTag
 */
@SuppressWarnings("serial")
public class ThemeTag extends MessageTag {

	/**
	 * 使用主题 MessageSource 解析主题消息。
	 */
	@Override
	protected MessageSource getMessageSource() {
		return getRequestContext().getTheme().getMessageSource();
	}

	/**
	 * 返回指示当前主题的异常消息。
	 */
	@Override
	protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
		return "Theme '" + getRequestContext().getTheme().getName() + "': " + ex.getMessage();
	}

}
