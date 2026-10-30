/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.tags.form;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 许多使用 {@link AbstractHtmlElementTag AbstractHtmlElementTag} 的数据绑定特性
 * 渲染内容的 HTML 标签的便捷超类。子标签唯一需要做的
 * 就是覆盖 {@link #renderDefaultContent(TagWriter)}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlElementBodyTag extends AbstractHtmlElementTag implements BodyTag {

	@Nullable
	private BodyContent bodyContent;

	@Nullable
	private TagWriter tagWriter;


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		onWriteTagContent();
		this.tagWriter = tagWriter;
		if (shouldRender()) {
			exposeAttributes();
			return EVAL_BODY_BUFFERED;
		}
		else {
			return SKIP_BODY;
		}
	}

	/**
	 * 如果{@link #shouldRender 需要渲染}，则刷新任何缓冲的
	 * {@link BodyContent}；如果未提供 {@link BodyContent}，
	 * 则{@link #renderDefaultContent 渲染默认内容}。
	 * @return 一个 {@link javax.servlet.jsp.tagext.Tag#EVAL_PAGE} 结果
	 */
	@Override
	public int doEndTag() throws JspException {
		if (shouldRender()) {
			Assert.state(this.tagWriter != null, "No TagWriter set");
			if (this.bodyContent != null && StringUtils.hasText(this.bodyContent.getString())) {
				renderFromBodyContent(this.bodyContent, this.tagWriter);
			}
			else {
				renderDefaultContent(this.tagWriter);
			}
		}
		return EVAL_PAGE;
	}

	/**
	 * 基于提供的 {@link BodyContent} 渲染标签内容。
	 * <p>默认实现简单地将 {@link BodyContent}
	 * 直接{@link #flushBufferedBodyContent 刷新}到输出中。子类可以选择
	 * 覆盖此方法以向输出添加额外内容。
	 */
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		flushBufferedBodyContent(bodyContent);
	}

	/**
	 * 清理任何属性和存储的资源。
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		removeAttributes();
		this.tagWriter = null;
		this.bodyContent = null;
	}


	//---------------------------------------------------------------------
	// Template methods
	//---------------------------------------------------------------------

	/**
	 * 在 {@link #writeTagContent} 开始时调用，允许子类执行任何
	 * 可能必要的前置条件检查或设置任务。
	 */
	protected void onWriteTagContent() {
	}

	/**
	 * 此标签是否应进行渲染。默认返回 '{@code true}'，
	 * 即始终进行渲染。如果子类提供条件渲染，
	 * 可以覆盖此方法。
	 */
	protected boolean shouldRender() throws JspException {
		return true;
	}

	/**
	 * 在 {@link #writeTagContent} 期间调用，允许子类按需向
	 * {@link javax.servlet.jsp.PageContext} 添加属性。
	 */
	protected void exposeAttributes() throws JspException {
	}

	/**
	 * 由 {@link #doFinally} 调用，允许子类按需从
	 * {@link javax.servlet.jsp.PageContext} 中移除属性。
	 */
	protected void removeAttributes() {
	}

	/**
	 * 用户自定义了错误消息的输出——将缓冲的内容刷新到
	 * 主 {@link javax.servlet.jsp.JspWriter}。
	 */
	protected void flushBufferedBodyContent(BodyContent bodyContent) throws JspException {
		try {
			bodyContent.writeOut(bodyContent.getEnclosingWriter());
		}
		catch (IOException ex) {
			throw new JspException("Unable to write buffered body content.", ex);
		}
	}

	protected abstract void renderDefaultContent(TagWriter tagWriter) throws JspException;


	//---------------------------------------------------------------------
	// BodyTag implementation
	//---------------------------------------------------------------------

	@Override
	public void doInitBody() throws JspException {
		// no op
	}

	@Override
	public void setBodyContent(BodyContent bodyContent) {
		this.bodyContent = bodyContent;
	}

}
