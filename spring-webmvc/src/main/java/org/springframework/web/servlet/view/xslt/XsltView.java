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

package org.springframework.web.servlet.view.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleTransformErrorListener;
import org.springframework.util.xml.TransformerUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.util.WebUtils;

/**
 * 由 XSLT 驱动的 View，允许将响应上下文内容作为 XSLT 转换的结果渲染。
 *
 * <p>XSLT Source 对象作为 model 中的参数提供，
 * 然后在渲染响应时被{@link #locateSource 检测}。
 * 用户可以通过 {@link #setSourceKey sourceKey} 属性在 model 中指定特定条目，
 * 或者让 Spring 自行定位 Source 对象。此类还提供了将对象基本转换为
 * Source 实现的功能。详情请参见{@link #getSourceTypes() 此处}。
 *
 * <p>所有 model 参数都会作为参数传递给 XSLT Transformer。
 * 此外，用户还可以配置要传递给 Transformer 的
 * {@link #setOutputProperties 输出属性}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XsltView extends AbstractUrlBasedView {

	@Nullable
	private Class<? extends TransformerFactory> transformerFactoryClass;

	@Nullable
	private String sourceKey;

	@Nullable
	private URIResolver uriResolver;

	private ErrorListener errorListener = new SimpleTransformErrorListener(logger);

	private boolean indent = true;

	@Nullable
	private Properties outputProperties;

	private boolean cacheTemplates = true;

	@Nullable
	private TransformerFactory transformerFactory;

	@Nullable
	private Templates cachedTemplates;


	/**
	 * 指定要使用的 XSLT TransformerFactory 类。
	 * <p>将调用指定类的默认构造器来为此视图构建 TransformerFactory。
	 */
	public void setTransformerFactoryClass(Class<? extends TransformerFactory> transformerFactoryClass) {
		this.transformerFactoryClass = transformerFactoryClass;
	}

	/**
	 * 设置表示 XSLT Source 的 model 属性名称。
	 * 如果未指定，将在 model map 中搜索匹配的值类型。
	 * <p>开箱即支持以下 source 类型：
	 * {@link Source}、{@link Document}、{@link Node}、{@link Reader}、
	 * {@link InputStream} 和 {@link Resource}。
	 * @see #getSourceTypes
	 * @see #convertSource
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
	 * 为自定义处理转换错误和警告，设置
	 * {@link javax.xml.transform.ErrorListener} 接口的实现。
	 * <p>如果未设置，将使用默认的
	 * {@link org.springframework.util.xml.SimpleTransformErrorListener}，
	 * 它只是使用视图类的 logger 实例记录警告，
	 * 并重新抛出错误以终止 XML 转换。
	 * @see org.springframework.util.xml.SimpleTransformErrorListener
	 */
	public void setErrorListener(@Nullable ErrorListener errorListener) {
		this.errorListener = (errorListener != null ? errorListener : new SimpleTransformErrorListener(logger));
	}

	/**
	 * 设置 XSLT transformer 在输出结果树时是否可以添加额外的空白字符。
	 * <p>默认值为 {@code true}（开启）；将其设为 {@code false}（关闭）
	 * 则不会指定 "indent" 键，而由样式表自行决定。
	 * @see javax.xml.transform.OutputKeys#INDENT
	 */
	public void setIndent(boolean indent) {
		this.indent = indent;
	}

	/**
	 * 设置要应用于样式表的任意 transformer 输出属性。
	 * <p>此处指定的任何值都将覆盖此视图以编程方式设置的默认值。
	 * @see javax.xml.transform.Transformer#setOutputProperty
	 */
	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	/**
	 * 开启/关闭对 XSLT {@link Templates} 实例的缓存。
	 * <p>默认值为 "true"。只有在开发环境中、缓存对性能影响不大时，
	 * 才应将其设为 "false"。
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	/**
	 * 初始化此 XsltView 的 TransformerFactory。
	 */
	@Override
	protected void initApplicationContext() throws BeansException {
		this.transformerFactory = newTransformerFactory(this.transformerFactoryClass);
		this.transformerFactory.setErrorListener(this.errorListener);
		if (this.uriResolver != null) {
			this.transformerFactory.setURIResolver(this.uriResolver);
		}
		if (this.cacheTemplates) {
			this.cachedTemplates = loadTemplates();
		}
	}

	/**
	 * 为此视图实例化一个新的 TransformerFactory。
	 * <p>默认实现只是调用
	 * {@link javax.xml.transform.TransformerFactory#newInstance()}。
	 * 如果显式指定了 {@link #setTransformerFactoryClass "transformerFactoryClass"}，
	 * 则将改为调用指定类的默认构造器。
	 * <p>可以在子类中重写。
	 * @param transformerFactoryClass the specified factory class (if any)
	 * @return the new TransactionFactory instance
	 * @see #setTransformerFactoryClass
	 * @see #getTransformerFactory()
	 */
	protected TransformerFactory newTransformerFactory(
			@Nullable Class<? extends TransformerFactory> transformerFactoryClass) {

		if (transformerFactoryClass != null) {
			try {
				return ReflectionUtils.accessibleConstructor(transformerFactoryClass).newInstance();
			}
			catch (Exception ex) {
				throw new TransformerFactoryConfigurationError(ex, "Could not instantiate TransformerFactory");
			}
		}
		else {
			return TransformerFactory.newInstance();
		}
	}

	/**
	 * 返回此 XsltView 使用的 TransformerFactory。
	 * @return the TransformerFactory (never {@code null})
	 */
	protected final TransformerFactory getTransformerFactory() {
		Assert.state(this.transformerFactory != null, "No TransformerFactory available");
		return this.transformerFactory;
	}


	@Override
	protected void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		Templates templates = this.cachedTemplates;
		if (templates == null) {
			templates = loadTemplates();
		}

		Transformer transformer = createTransformer(templates);
		configureTransformer(model, response, transformer);
		configureResponse(model, response, transformer);
		Source source = null;
		try {
			source = locateSource(model);
			if (source == null) {
				throw new IllegalArgumentException("Unable to locate Source object in model: " + model);
			}
			transformer.transform(source, createResult(response));
		}
		finally {
			closeSourceIfNecessary(source);
		}
	}

	/**
	 * 创建用于渲染转换结果的 XSLT {@link Result}。
	 * <p>默认实现创建一个 {@link StreamResult}，
	 * 其中包装了所提供的 HttpServletResponse 的
	 * {@link HttpServletResponse#getOutputStream() OutputStream}。
	 * @param response current HTTP response
	 * @return the XSLT Result to use
	 * @throws Exception if the Result cannot be built
	 */
	protected Result createResult(HttpServletResponse response) throws Exception {
		return new StreamResult(response.getOutputStream());
	}

	/**
	 * <p>在提供的 model 中定位 {@link Source} 对象，
	 * 并根据需要进行对象转换。
	 * 默认实现会首先尝试在已配置的
	 * {@link #setSourceKey source key}（如果有）下查找，
	 * 然后再尝试定位 {@link #getSourceTypes() 受支持类型}的对象。
	 * @param model the merged model Map
	 * @return the XSLT Source object (or {@code null} if none found)
	 * @throws Exception if an error occurred during locating the source
	 * @see #setSourceKey
	 * @see #convertSource
	 */
	@Nullable
	protected Source locateSource(Map<String, Object> model) throws Exception {
		if (this.sourceKey != null) {
			return convertSource(model.get(this.sourceKey));
		}
		Object source = CollectionUtils.findValueOfType(model.values(), getSourceTypes());
		return (source != null ? convertSource(source) : null);
	}

	/**
	 * 返回转换为 XSLT {@link Source} 时受支持的
	 * {@link Class Classes} 数组。
	 * <p>目前支持 {@link Source}、{@link Document}、{@link Node}、
	 * {@link Reader}、{@link InputStream} 和 {@link Resource}。
	 * @return the supported source types
	 */
	protected Class<?>[] getSourceTypes() {
		return new Class<?>[] {Source.class, Document.class, Node.class, Reader.class, InputStream.class, Resource.class};
	}

	/**
	 * 如果提供的 {@link Object} 类型是{@link #getSourceTypes() 受支持的类型}，
	 * 则将其转换为 XSLT {@link Source}。
	 * @param source the original source object
	 * @return the adapted XSLT Source
	 * @throws IllegalArgumentException if the given Object is not of a supported type
	 */
	protected Source convertSource(Object source) throws Exception {
		if (source instanceof Source) {
			return (Source) source;
		}
		else if (source instanceof Document) {
			return new DOMSource(((Document) source).getDocumentElement());
		}
		else if (source instanceof Node) {
			return new DOMSource((Node) source);
		}
		else if (source instanceof Reader) {
			return new StreamSource((Reader) source);
		}
		else if (source instanceof InputStream) {
			return new StreamSource((InputStream) source);
		}
		else if (source instanceof Resource) {
			Resource resource = (Resource) source;
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		}
		else {
			throw new IllegalArgumentException("Value '" + source + "' cannot be converted to XSLT Source");
		}
	}

	/**
	 * 配置提供的 {@link Transformer} 实例。
	 * <p>默认实现会将 model 中的参数复制到
	 * Transformer 的 {@link Transformer#setParameter 参数集合}中。
	 * 此实现还会将 {@link #setOutputProperties 输出属性}
	 * 复制到 {@link Transformer} 的
	 * {@link Transformer#setOutputProperty 输出属性}中。
	 * 同时也会设置缩进属性。
	 * @param model merged output Map (never {@code null})
	 * @param response current HTTP response
	 * @param transformer the target transformer
	 * @see #copyModelParameters(Map, Transformer)
	 * @see #copyOutputProperties(Transformer)
	 * @see #configureIndentation(Transformer)
	 */
	protected void configureTransformer(Map<String, Object> model, HttpServletResponse response,
			Transformer transformer) {

		copyModelParameters(model, transformer);
		copyOutputProperties(transformer);
		configureIndentation(transformer);
	}

	/**
	 * 为提供的 {@link Transformer} 配置缩进设置。
	 * @param transformer the target transformer
	 * @see org.springframework.util.xml.TransformerUtils#enableIndenting(javax.xml.transform.Transformer)
	 * @see org.springframework.util.xml.TransformerUtils#disableIndenting(javax.xml.transform.Transformer)
	 */
	protected final void configureIndentation(Transformer transformer) {
		if (this.indent) {
			TransformerUtils.enableIndenting(transformer);
		}
		else {
			TransformerUtils.disableIndenting(transformer);
		}
	}

	/**
	 * 将已配置的输出 {@link Properties}（如果有）复制到
	 * 所提供的 {@link Transformer} 的
	 * {@link Transformer#setOutputProperty 输出属性集合}中。
	 * @param transformer the target transformer
	 */
	protected final void copyOutputProperties(Transformer transformer) {
		if (this.outputProperties != null) {
			Enumeration<?> en = this.outputProperties.propertyNames();
			while (en.hasMoreElements()) {
				String name = (String) en.nextElement();
				transformer.setOutputProperty(name, this.outputProperties.getProperty(name));
			}
		}
	}

	/**
	 * 将所提供的 Map 中的所有条目复制到
	 * 所提供的 {@link Transformer} 的
	 * {@link Transformer#setParameter(String, Object) 参数集合}中。
	 * @param model merged output Map (never {@code null})
	 * @param transformer the target transformer
	 */
	protected final void copyModelParameters(Map<String, Object> model, Transformer transformer) {
		model.forEach(transformer::setParameter);
	}

	/**
	 * 配置提供的 {@link HttpServletResponse}。
	 * <p>此方法的默认实现会根据 {@link Transformer} 中指定的
	 * "media-type" 和 "encoding" 输出属性，
	 * 设置 {@link HttpServletResponse#setContentType content type}
	 * 和 {@link HttpServletResponse#setCharacterEncoding encoding}。
	 * @param model merged output Map (never {@code null})
	 * @param response current HTTP response
	 * @param transformer the target transformer
	 */
	protected void configureResponse(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
		String contentType = getContentType();
		String mediaType = transformer.getOutputProperty(OutputKeys.MEDIA_TYPE);
		String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);
		if (StringUtils.hasText(mediaType)) {
			contentType = mediaType;
		}
		if (StringUtils.hasText(encoding)) {
			// Only apply encoding if content type is specified but does not contain charset clause already.
			if (contentType != null && !contentType.toLowerCase().contains(WebUtils.CONTENT_TYPE_CHARSET_PREFIX)) {
				contentType = contentType + WebUtils.CONTENT_TYPE_CHARSET_PREFIX + encoding;
			}
		}
		response.setContentType(contentType);
	}

	/**
	 * 为配置位置处的样式表加载 {@link Templates} 实例。
	 */
	private Templates loadTemplates() throws ApplicationContextException {
		Source stylesheetSource = getStylesheetSource();
		try {
			Templates templates = getTransformerFactory().newTemplates(stylesheetSource);
			return templates;
		}
		catch (TransformerConfigurationException ex) {
			throw new ApplicationContextException("Can't load stylesheet from '" + getUrl() + "'", ex);
		}
		finally {
			closeSourceIfNecessary(stylesheetSource);
		}
	}

	/**
	 * 创建用于执行 XSLT 转换的 {@link Transformer} 实例。
	 * <p>默认实现只是调用 {@link Templates#newTransformer()}，
	 * 如果指定了自定义 {@link URIResolver}，则用它来配置 {@link Transformer}。
	 * @param templates the XSLT Templates instance to create a Transformer for
	 * @return the Transformer object
	 * @throws TransformerConfigurationException in case of creation failure
	 */
	protected Transformer createTransformer(Templates templates) throws TransformerConfigurationException {
		Transformer transformer = templates.newTransformer();
		if (this.uriResolver != null) {
			transformer.setURIResolver(this.uriResolver);
		}
		return transformer;
	}

	/**
	 * 获取 {@link #setUrl 已配置 URL} 下 XSLT 模板对应的 XSLT {@link Source}。
	 * @return the Source object
	 */
	protected Source getStylesheetSource() {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		if (logger.isDebugEnabled()) {
			logger.debug("Applying stylesheet [" + url + "]");
		}
		try {
			Resource resource = obtainApplicationContext().getResource(url);
			return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Can't load XSLT stylesheet from '" + url + "'", ex);
		}
	}

	/**
	 * 如果适用，关闭所提供的 {@link Source} 管理的底层资源。
	 * <p>仅适用于 {@link StreamSource StreamSources}。
	 * @param source the XSLT Source to close (may be {@code null})
	 */
	private void closeSourceIfNecessary(@Nullable Source source) {
		if (source instanceof StreamSource) {
			StreamSource streamSource = (StreamSource) source;
			if (streamSource.getReader() != null) {
				try {
					streamSource.getReader().close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
			if (streamSource.getInputStream() != null) {
				try {
					streamSource.getInputStream().close();
				}
				catch (IOException ex) {
					// ignore
				}
			}
		}
	}

}
