/*
 * Copyright 2002-2015 the original author or authors.
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

import javax.servlet.jsp.JspException;

import org.springframework.lang.Nullable;
import org.springframework.web.util.HtmlUtils;

/**
 * 用于输出可能需要进行 HTML 转义内容的标签的超类。
 *
 * <p>提供 "htmlEscape" 属性，用于显式指定是否应用 HTML 转义。
 * 如果未设置，则使用页面级默认值（例如来自 HtmlEscapeTag）或
 * 应用级默认值（{@code web.xml} 中的 "defaultHtmlEscape" 上下文参数）。
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 1.1
 * @see #setHtmlEscape
 * @see HtmlEscapeTag
 * @see org.springframework.web.servlet.support.RequestContext#isDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#getDefaultHtmlEscape
 * @see org.springframework.web.util.WebUtils#getResponseEncodedHtmlEscape
 */
@SuppressWarnings("serial")
public abstract class HtmlEscapingAwareTag extends RequestContextAwareTag {

	@Nullable
	private Boolean htmlEscape;


	/**
	 * 以布尔值形式为该标签设置 HTML 转义。
	 * 覆盖当前页面的默认 HTML 转义设置。
	 * @see HtmlEscapeTag#setDefaultHtmlEscape
	 */
	public void setHtmlEscape(boolean htmlEscape) throws JspException {
		this.htmlEscape = htmlEscape;
	}

	/**
	 * 返回此标签的 HTML 转义设置，
	 * 如果未覆盖则返回默认设置。
	 * @see #isDefaultHtmlEscape()
	 */
	protected boolean isHtmlEscape() {
		if (this.htmlEscape != null) {
			return this.htmlEscape.booleanValue();
		}
		else {
			return isDefaultHtmlEscape();
		}
	}

	/**
	 * 返回此标签适用的默认 HTML 转义设置。
	 * <p>默认实现会检查 RequestContext 的设置，
	 * 如果未给出显式默认值，则回退到 {@code false}。
	 * @see #getRequestContext()
	 */
	protected boolean isDefaultHtmlEscape() {
		return getRequestContext().isDefaultHtmlEscape();
	}

	/**
	 * 返回此标签在使用 HTML 转义时是否使用响应编码的
	 * 适用默认值。
	 * <p>默认实现会检查 RequestContext 的设置，
	 * 如果未给出显式默认值，则回退到 {@code false}。
	 * @since 4.1.2
	 * @see #getRequestContext()
	 */
	protected boolean isResponseEncodedHtmlEscape() {
		return getRequestContext().isResponseEncodedHtmlEscape();
	}

	/**
	 * 对给定的 String 进行 HTML 编码，仅在启用 "htmlEscape" 设置时才执行。
	 * <p>如果同时启用了 "responseEncodedHtmlEscape" 设置，
	 * 则会考虑响应编码。
	 * @param content 要转义的 String
	 * @return 转义后的 String
	 * @since 4.1.2
	 * @see #isHtmlEscape()
	 * @see #isResponseEncodedHtmlEscape()
	 */
	protected String htmlEscape(String content) {
		String out = content;
		if (isHtmlEscape()) {
			if (isResponseEncodedHtmlEscape()) {
				out = HtmlUtils.htmlEscape(content, this.pageContext.getResponse().getCharacterEncoding());
			}
			else {
				out = HtmlUtils.htmlEscape(content);
			}
		}
		return out;
	}

}
