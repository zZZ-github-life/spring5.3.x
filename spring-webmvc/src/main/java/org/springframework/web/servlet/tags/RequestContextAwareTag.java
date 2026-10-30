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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.JspAwareRequestContext;
import org.springframework.web.servlet.support.RequestContext;

/**
 * 所有需要 {@link RequestContext} 的标签的超类。
 *
 * <p>{@code RequestContext} 实例提供了对当前状态的便捷访问，
 * 例如
 * {@link org.springframework.web.context.WebApplicationContext}、
 * {@link java.util.Locale}、
 * {@link org.springframework.ui.context.Theme} 等。
 *
 * <p>主要面向
 * {@link org.springframework.web.servlet.DispatcherServlet} 请求；
 * 在 {@code DispatcherServlet} 之外使用时将采用回退方案。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.support.RequestContext
 * @see org.springframework.web.servlet.DispatcherServlet
 */
@SuppressWarnings("serial")
public abstract class RequestContextAwareTag extends TagSupport implements TryCatchFinally {

	/**
	 * 页面级 {@link RequestContext} 实例所对应的
	 * {@link javax.servlet.jsp.PageContext} 属性。
	 */
	public static final String REQUEST_CONTEXT_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.REQUEST_CONTEXT";


	/** 可供子类使用的 Logger。 */
	protected final Log logger = LogFactory.getLog(getClass());


	@Nullable
	private RequestContext requestContext;


	/**
	 * 创建并公开当前的 RequestContext。
	 * 实际工作委托给 {@link #doStartTagInternal()}。
	 * @see #REQUEST_CONTEXT_PAGE_ATTRIBUTE
	 * @see org.springframework.web.servlet.support.JspAwareRequestContext
	 */
	@Override
	public final int doStartTag() throws JspException {
		try {
			this.requestContext = (RequestContext) this.pageContext.getAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE);
			if (this.requestContext == null) {
				this.requestContext = new JspAwareRequestContext(this.pageContext);
				this.pageContext.setAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE, this.requestContext);
			}
			return doStartTagInternal();
		}
		catch (JspException | RuntimeException ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw new JspTagException(ex.getMessage());
		}
	}

	/**
	 * 返回当前的 RequestContext。
	 */
	protected final RequestContext getRequestContext() {
		Assert.state(this.requestContext != null, "No current RequestContext");
		return this.requestContext;
	}

	/**
	 * 由 doStartTag 调用以执行实际工作。
	 * @return 与 TagSupport.doStartTag 相同
	 * @throws Exception 任何异常；除 JspException 之外的受检异常
	 * 会被 doStartTag 包装为 JspException
	 * @see javax.servlet.jsp.tagext.TagSupport#doStartTag
	 */
	protected abstract int doStartTagInternal() throws Exception;


	@Override
	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	@Override
	public void doFinally() {
		this.requestContext = null;
	}

}
