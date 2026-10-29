/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.view.xslt;

import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 实现，它通过将提供的视图名称
 * 转换为 XSLT 样式表的 URL 来解析 {@link XsltView} 实例。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XsltViewResolver extends UrlBasedViewResolver {

	@Nullable
	private String sourceKey;

	@Nullable
	private URIResolver uriResolver;

	@Nullable
	private ErrorListener errorListener;

	private boolean indent = true;

	@Nullable
	private Properties outputProperties;

	private boolean cacheTemplates = true;


	/**
	 * 此解析器要求使用 {@link XsltView}。
	 */
	public XsltViewResolver() {
		setViewClass(requiredViewClass());
	}


	/**
	 * 设置表示 XSLT Source 的 model 属性的名称。
	 * 如果未指定，将在 model map 中搜索匹配的值类型。
	 * <p>开箱即支持以下 source 类型：
	 * {@link javax.xml.transform.Source}、{@link org.w3c.dom.Document}、
	 * {@link org.w3c.dom.Node}、{@link java.io.Reader}、{@link java.io.InputStream}
	 * 和 {@link org.springframework.core.io.Resource}。
	 */
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	/**
	 * 设置转换中使用的 URIResolver。
	 * <p>URIResolver 负责处理对 XSLT {@code document()} 函数的调用。
	 */
	public void setUriResolver(URIResolver uriResolver) {
		this.uriResolver = uriResolver;
	}

	/**
	 * 设置 {@link javax.xml.transform.ErrorListener} 接口的实现，
	 * 用于自定义处理转换错误和警告。
	 * <p>如果未设置，将使用默认的
	 * {@link org.springframework.util.xml.SimpleTransformErrorListener}，
	 * 它仅使用视图类的 logger 实例记录警告，
	 * 并重新抛出错误以中止 XML 转换。
	 * @see org.springframework.util.xml.SimpleTransformErrorListener
	 */
	public void setErrorListener(ErrorListener errorListener) {
		this.errorListener = errorListener;
	}

	/**
	 * 设置 XSLT 转换器在输出结果树时是否可以添加额外的空白。
	 * <p>默认值为 {@code true}（开启）；将其设置为 {@code false}（关闭）
	 * 将不指定 "indent" 键，由样式表自行决定。
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public void setIndent(boolean indent) {
		this.indent = indent;
	}

	/**
	 * 设置要应用于样式表的任意转换器输出属性。
	 * <p>此处指定的任何值都将覆盖此视图以编程方式设置的默认值。
	 * @see javax.xml.transform.Transformer#setOutputProperty
	 */
	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	/**
	 * 开启/关闭 XSLT 模板的缓存。
	 * <p>默认值为 "true"。仅在开发环境中将其设置为 "false"，
	 * 因为在开发环境中缓存不会严重影响性能。
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	@Override
	protected Class<?> requiredViewClass() {
		return XsltView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == XsltView.class ? new XsltView() : super.instantiateView());
	}

	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		XsltView view = (XsltView) super.buildView(viewName);
		if (this.sourceKey != null) {
			view.setSourceKey(this.sourceKey);
		}
		if (this.uriResolver != null) {
			view.setUriResolver(this.uriResolver);
		}
		if (this.errorListener != null) {
			view.setErrorListener(this.errorListener);
		}
		view.setIndent(this.indent);
		if (this.outputProperties != null) {
			view.setOutputProperties(this.outputProperties);
		}
		view.setCacheTemplates(this.cacheTemplates);
		return view;
	}

}
