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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.JavaScriptUtils;

/**
 * {@code <escapeBody>} 标签用于转义其所包围的正文内容，
 * 可应用 HTML 转义和/或 JavaScript 转义。
 *
 * <p>提供 "htmlEscape" 属性，用于显式指定是否应用 HTML 转义。
 * 如果未设置，则使用页面级默认值（例如来自 HtmlEscapeTag）或
 * 应用级默认值（web.xml 中的 "defaultHtmlEscape" 上下文参数）。
 *
 * <p>提供 "javaScriptEscape" 属性，用于指定是否应用
 * JavaScript 转义。可与 HTML 转义组合使用，也可单独使用。
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
 * <td>htmlEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 HTML 转义。
 * 覆盖当前页面的默认 HTML 转义设置。</td>
 * </tr>
 * <tr>
 * <td>javaScriptEscape</td>
 * <td>false</td>
 * <td>true</td>
 * <td>以布尔值形式为该标签设置 JavaScript 转义。
 * 默认为 {@code false}。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Juergen Hoeller
 * @since 1.1.1
 * @see org.springframework.web.util.HtmlUtils
 * @see org.springframework.web.util.JavaScriptUtils
 */
@SuppressWarnings("serial")
public class EscapeBodyTag extends HtmlEscapingAwareTag implements BodyTag {

	private boolean javaScriptEscape = false;

	@Nullable
	private BodyContent bodyContent;


	/**
	 * 以布尔值形式为该标签设置 JavaScript 转义。
	 * 默认为 "false"。
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	protected int doStartTagInternal() {
		// do nothing
		return EVAL_BODY_BUFFERED;
	}

	@Override
	public void doInitBody() {
		// do nothing
	}

	@Override
	public void setBodyContent(BodyContent bodyContent) {
		this.bodyContent = bodyContent;
	}

	@Override
	public int doAfterBody() throws JspException {
		try {
			String content = readBodyContent();
			// HTML and/or JavaScript escape, if demanded
			content = htmlEscape(content);
			content = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(content) : content);
			writeBodyContent(content);
		}
		catch (IOException ex) {
			throw new JspException("Could not write escaped body", ex);
		}
		return (SKIP_BODY);
	}

	/**
	 * 从页面读取未转义的正文内容。
	 * @return 原始内容
	 * @throws IOException 如果读取失败
	 */
	protected String readBodyContent() throws IOException {
		Assert.state(this.bodyContent != null, "No BodyContent set");
		return this.bodyContent.getString();
	}

	/**
	 * 将转义后的正文内容写入页面。
	 * <p>可在子类中重写，例如用于测试目的。
	 * @param content 要写入的内容
	 * @throws IOException 如果写入失败
	 */
	protected void writeBodyContent(String content) throws IOException {
		Assert.state(this.bodyContent != null, "No BodyContent set");
		this.bodyContent.getEnclosingWriter().print(content);
	}

}
