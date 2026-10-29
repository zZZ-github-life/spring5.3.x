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

package org.springframework.web.servlet.tags.form;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于将 HTML 内容写入 {@link Writer} 实例的工具类。<p>旨在支持 JSP 标签库的输出。
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class TagWriter {

	/**
	 * 用于写入的 {@link SafeWriter}。
	 */
	private final SafeWriter writer;

	/**
	 * 存储 {@link TagStateEntry 标签状态}。栈模型天然支持标签嵌套。
	 */
	private final Deque<TagStateEntry> tagState = new ArrayDeque<>();


	/**
	 * 创建一个写入给定 {@link PageContext} 的 {@link TagWriter} 类的新实例。
	 * @param pageContext the JSP PageContext to obtain the {@link Writer} from
	 */
	public TagWriter(PageContext pageContext) {
		Assert.notNull(pageContext, "PageContext must not be null");
		this.writer = new SafeWriter(pageContext);
	}

	/**
	 * 创建一个写入给定 {@link Writer} 的 {@link TagWriter} 类的新实例。
	 * @param writer the {@link Writer} to write tag content to
	 */
	public TagWriter(Writer writer) {
		Assert.notNull(writer, "Writer must not be null");
		this.writer = new SafeWriter(writer);
	}


	/**
	 * 以给定名称开始一个新标签。该标签保持打开状态，以便可以向其中写入属性、内部文本或嵌套标签。
	 * @see #endTag()
	 */
	public void startTag(String tagName) throws JspException {
		if (inTag()) {
			closeTagAndMarkAsBlock();
		}
		push(tagName);
		this.writer.append("<").append(tagName);
	}

	/**
	 * 写入具有指定名称和值的 HTML 属性。<p>请确保在写入任何内部文本或嵌套标签<strong>之前</strong>写入所有属性。
	 * @throws IllegalStateException if the opening tag is closed
	 */
	public void writeAttribute(String attributeName, String attributeValue) throws JspException {
		if (currentState().isBlockTag()) {
			throw new IllegalStateException("Cannot write attributes after opening tag is closed.");
		}
		this.writer.append(" ").append(attributeName).append("=\"")
				.append(attributeValue).append("\"");
	}

	/**
	 * {@link #writeAttribute(String, String)} 的变体，用于写入没有值的空 HTML 属性，例如 {@code required}。
	 * @since 5.3.14
	 */
	public void writeAttribute(String attributeName) throws JspException {
		if (currentState().isBlockTag()) {
			throw new IllegalStateException("Cannot write attributes after opening tag is closed.");
		}
		this.writer.append(" ").append(attributeName);
	}

	/**
	 * 如果给定值不为 {@code null} 且长度不为零，则写入 HTML 属性。
	 * @see #writeAttribute(String, String)
	 */
	public void writeOptionalAttributeValue(String attributeName, @Nullable String attributeValue) throws JspException {
		if (StringUtils.hasText(attributeValue)) {
			writeAttribute(attributeName, attributeValue);
		}
	}

	/**
	 * 关闭当前打开的标签（如有必要），并将给定值作为内部文本追加。
	 * @throws IllegalStateException if no tag is open
	 */
	public void appendValue(String value) throws JspException {
		if (!inTag()) {
			throw new IllegalStateException("Cannot write tag value. No open tag available.");
		}
		closeTagAndMarkAsBlock();
		this.writer.append(value);
	}


	/**
	 * 指示当前打开的标签应被关闭并标记为块级元素。<p>当你计划在当前 {@link TagWriter} 上下文之外的正文中写入额外内容时很有用。
	 */
	public void forceBlock() throws JspException {
		if (currentState().isBlockTag()) {
			return; // just ignore since we are already in the block
		}
		closeTagAndMarkAsBlock();
	}

	/**
	 * 关闭当前标签。<p>如果尚未写入任何内部文本或嵌套标签，则正确地写出空标签。
	 */
	public void endTag() throws JspException {
		endTag(false);
	}

	/**
	 * 关闭当前标签，允许强制使用完整的闭合标签。<p>如果尚未写入任何内部文本或嵌套标签，则正确地写出空标签。
	 * @param enforceClosingTag whether a full closing tag should be
	 * rendered in any case, even in case of a non-block tag
	 */
	public void endTag(boolean enforceClosingTag) throws JspException {
		if (!inTag()) {
			throw new IllegalStateException("Cannot write end of tag. No open tag available.");
		}
		boolean renderClosingTag = true;
		if (!currentState().isBlockTag()) {
			// Opening tag still needs to be closed...
			if (enforceClosingTag) {
				this.writer.append(">");
			}
			else {
				this.writer.append("/>");
				renderClosingTag = false;
			}
		}
		if (renderClosingTag) {
			this.writer.append("</").append(currentState().getTagName()).append(">");
		}
		this.tagState.pop();
	}


	/**
	 * 将给定标签名称添加到 {@link #tagState 标签状态}。
	 */
	private void push(String tagName) {
		this.tagState.push(new TagStateEntry(tagName));
	}

	/**
	 * 关闭当前打开的标签并将其标记为块级标签。
	 */
	private void closeTagAndMarkAsBlock() throws JspException {
		if (!currentState().isBlockTag()) {
			currentState().markAsBlockTag();
			this.writer.append(">");
		}
	}

	private boolean inTag() {
		return !this.tagState.isEmpty();
	}

	private TagStateEntry currentState() {
		return this.tagState.element();
	}


	/**
	 * 保存关于标签及其渲染行为的状态。
	 */
	private static class TagStateEntry {

		private final String tagName;

		private boolean blockTag;

		public TagStateEntry(String tagName) {
			this.tagName = tagName;
		}

		public String getTagName() {
			return this.tagName;
		}

		public void markAsBlockTag() {
			this.blockTag = true;
		}

		public boolean isBlockTag() {
			return this.blockTag;
		}
	}


	/**
	 * 简单的 {@link Writer} 包装器，将所有 {@link IOException IOExceptions} 包装为 {@link JspException JspExceptions}。
	 */
	private static final class SafeWriter {

		@Nullable
		private PageContext pageContext;

		@Nullable
		private Writer writer;

		public SafeWriter(PageContext pageContext) {
			this.pageContext = pageContext;
		}

		public SafeWriter(Writer writer) {
			this.writer = writer;
		}

		public SafeWriter append(String value) throws JspException {
			try {
				getWriterToUse().write(String.valueOf(value));
				return this;
			}
			catch (IOException ex) {
				throw new JspException("Unable to write to JspWriter", ex);
			}
		}

		private Writer getWriterToUse() {
			Writer writer = (this.pageContext != null ? this.pageContext.getOut() : this.writer);
			Assert.state(writer != null, "No Writer available");
			return writer;
		}
	}

}
