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

package org.springframework.web.servlet.view.freemarker;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import freemarker.core.ParseException;
import freemarker.ext.jsp.TaglibFactory;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.ext.servlet.FreemarkerServlet;
import freemarker.ext.servlet.HttpRequestHashModel;
import freemarker.ext.servlet.HttpRequestParametersHashModel;
import freemarker.ext.servlet.HttpSessionHashModel;
import freemarker.ext.servlet.ServletContextHashModel;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.ObjectWrapper;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContextException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.AbstractTemplateView;

/**
 * 使用 FreeMarker 模板引擎的 View。
 *
 * <p>暴露以下 JavaBean 属性：
 * <ul>
 * <li><b>url</b>：要包装的 FreeMarker 模板的位置，
 * 相对于 FreeMarker 模板上下文（目录）。
 * <li><b>encoding</b>（可选，默认值由 FreeMarker 配置决定）：
 * FreeMarker 模板文件的编码
 * </ul>
 *
 * <p>依赖当前 Web 应用上下文中可访问的单个 {@link FreeMarkerConfig} 对象
 * （例如 {@link FreeMarkerConfigurer}），bean 名称不限。
 * 或者，你可以将 FreeMarker {@link Configuration} 对象设置为 bean 属性。
 * 关于这种做法的具体影响，请参阅 {@link #setConfiguration}。
 *
 * <p>注意：Spring 的 FreeMarker 支持要求 FreeMarker 2.3 或更高版本。
 *
 * @author Darren Davison
 * @author Juergen Hoeller
 * @since 03.03.2004
 * @see #setUrl
 * @see #setExposeSpringMacroHelpers
 * @see #setEncoding
 * @see #setConfiguration
 * @see FreeMarkerConfig
 * @see FreeMarkerConfigurer
 */
public class FreeMarkerView extends AbstractTemplateView {

	@Nullable
	private String encoding;

	@Nullable
	private Configuration configuration;

	@Nullable
	private TaglibFactory taglibFactory;

	@Nullable
	private ServletContextHashModel servletContextHashModel;


	/**
	 * 设置 FreeMarker 模板文件的编码。默认值由 FreeMarker Configuration 决定：
	 * 如果未另行指定，则为 "ISO-8859-1"。
	 * <p>如果你的所有模板共享同一编码，请在 FreeMarker Configuration 中指定编码，
	 * 而不是为每个模板分别指定。
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 返回 FreeMarker 模板使用的编码。
	 */
	@Nullable
	protected String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置此视图使用的 FreeMarker Configuration。
	 * <p>如果未设置，则会进行默认查找：期望当前 Web 应用上下文中存在
	 * 单个 {@link FreeMarkerConfig}，bean 名称不限。
	 * <strong>注意：</strong>使用此方法将导致为每个 {@link FreeMarkerView} 实例
	 * 都创建一个新的 {@link TaglibFactory} 实例。这在内存和初始 CPU 开销方面
	 * 可能相当昂贵。在生产环境中，建议使用暴露单个共享 {@link TaglibFactory}
	 * 的 {@link FreeMarkerConfig}。
	 */
	public void setConfiguration(@Nullable Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * 返回此视图使用的 FreeMarker 配置。
	 */
	@Nullable
	protected Configuration getConfiguration() {
		return this.configuration;
	}

	/**
	 * 获取实际使用的 FreeMarker 配置。
	 * @return the FreeMarker configuration (never {@code null})
	 * @throws IllegalStateException in case of no Configuration object set
	 * @since 5.0
	 */
	protected Configuration obtainConfiguration() {
		Configuration configuration = getConfiguration();
		Assert.state(configuration != null, "No Configuration set");
		return configuration;
	}


	/**
	 * 在启动时调用。查找单个 FreeMarkerConfig bean，
	 * 以找到此工厂相关的 Configuration。
	 * <p>检查是否能找到默认 Locale 对应的模板：
	 * 如果找不到特定 locale 的模板，FreeMarker 会检查非 locale 特定的模板。
	 * @see freemarker.cache.TemplateCache#getTemplate
	 */
	@Override
	protected void initServletContext(ServletContext servletContext) throws BeansException {
		if (getConfiguration() != null) {
			this.taglibFactory = new TaglibFactory(servletContext);
		}
		else {
			FreeMarkerConfig config = autodetectConfiguration();
			setConfiguration(config.getConfiguration());
			this.taglibFactory = config.getTaglibFactory();
		}

		GenericServlet servlet = new GenericServletAdapter();
		try {
			servlet.init(new DelegatingServletConfig());
		}
		catch (ServletException ex) {
			throw new BeanInitializationException("Initialization of GenericServlet adapter failed", ex);
		}
		this.servletContextHashModel = new ServletContextHashModel(servlet, getObjectWrapper());
	}

	/**
	 * 通过 ApplicationContext 自动检测 {@link FreeMarkerConfig} 对象。
	 * @return the Configuration instance to use for FreeMarkerViews
	 * @throws BeansException if no Configuration instance could be found
	 * @see #getApplicationContext
	 * @see #setConfiguration
	 */
	protected FreeMarkerConfig autodetectConfiguration() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					obtainApplicationContext(), FreeMarkerConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException(
					"Must define a single FreeMarkerConfig bean in this web application context " +
					"(may be inherited): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.", ex);
		}
	}

	/**
	 * 返回已配置的 FreeMarker {@link ObjectWrapper}，
	 * 如果未指定则返回 {@link ObjectWrapper#DEFAULT_WRAPPER default wrapper}。
	 * @see freemarker.template.Configuration#getObjectWrapper()
	 */
	protected ObjectWrapper getObjectWrapper() {
		ObjectWrapper ow = obtainConfiguration().getObjectWrapper();
		return (ow != null ? ow :
				new DefaultObjectWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build());
	}

	/**
	 * 检查此视图使用的 FreeMarker 模板是否存在且有效。
	 * <p>可以重写此方法以自定义行为，
	 * 例如需要将多个模板渲染到单个视图中的情况。
	 */
	@Override
	public boolean checkResource(Locale locale) throws Exception {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");

		try {
			// Check that we can get the template, even if we might subsequently get it again.
			getTemplate(url, locale);
			return true;
		}
		catch (FileNotFoundException ex) {
			// Allow for ViewResolver chaining...
			return false;
		}
		catch (ParseException ex) {
			throw new ApplicationContextException("Failed to parse [" + url + "]", ex);
		}
		catch (IOException ex) {
			throw new ApplicationContextException("Failed to load [" + url + "]", ex);
		}
	}


	/**
	 * 通过将 model map 与 FreeMarker 模板合并来处理该 model。
	 * 输出将写入 servlet response。
	 * <p>如果需要自定义行为，可以重写此方法。
	 */
	@Override
	protected void renderMergedTemplateModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		exposeHelpers(model, request);
		doRender(model, request, response);
	}

	/**
	 * 暴露每次渲染操作独有的辅助对象。这是必要的，
	 * 以免不同的渲染操作相互覆盖对方的格式等设置。
	 * <p>由 {@code renderMergedTemplateModel} 调用。默认实现为空。
	 * 可以重写此方法以向 model 添加自定义辅助对象。
	 * @param model the model that will be passed to the template at merge time
	 * @param request current HTTP request
	 * @throws Exception if there's a fatal error while we're adding information to the context
	 * @see #renderMergedTemplateModel
	 */
	protected void exposeHelpers(Map<String, Object> model, HttpServletRequest request) throws Exception {
	}

	/**
	 * 使用包含完整模板 model 的给定 model map，将 FreeMarker 视图渲染到给定的 response。
	 * <p>默认实现会渲染由 "url" bean 属性指定的模板，
	 * 该模板通过 {@code getTemplate} 获取。它委托给 {@code processTemplate} 方法，
	 * 将模板实例与给定的模板 model 合并。
	 * <p>向 model 中添加标准的 FreeMarker hash model：请求参数、
	 * request、session 和 application（ServletContext），以及 JSP 标签库 hash model。
	 * <p>可以重写此方法以自定义行为，例如将多个模板渲染到单个视图中。
	 * @param model the model to use for rendering
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @throws IOException if the template file could not be retrieved
	 * @throws Exception if rendering failed
	 * @see #setUrl
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getLocale
	 * @see #getTemplate(java.util.Locale)
	 * @see #processTemplate
	 * @see freemarker.ext.servlet.FreemarkerServlet
	 */
	protected void doRender(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		// Expose model to JSP tags (as request attributes).
		exposeModelAsRequestAttributes(model, request);
		// Expose all standard FreeMarker hash models.
		SimpleHash fmModel = buildTemplateModel(model, request, response);

		// Grab the locale-specific version of the template.
		Locale locale = RequestContextUtils.getLocale(request);
		processTemplate(getTemplate(locale), fmModel, response);
	}

	/**
	 * 为给定的 model Map 构建 FreeMarker 模板 model。
	 * <p>默认实现构建一个 {@link AllHttpScopesHashModel}。
	 * @param model the model to use for rendering
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @return the FreeMarker template model, as a {@link SimpleHash} or subclass thereof
	 */
	protected SimpleHash buildTemplateModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) {

		AllHttpScopesHashModel fmModel = new AllHttpScopesHashModel(getObjectWrapper(), getServletContext(), request);
		fmModel.put(FreemarkerServlet.KEY_JSP_TAGLIBS, this.taglibFactory);
		fmModel.put(FreemarkerServlet.KEY_APPLICATION, this.servletContextHashModel);
		fmModel.put(FreemarkerServlet.KEY_SESSION, buildSessionModel(request, response));
		fmModel.put(FreemarkerServlet.KEY_REQUEST, new HttpRequestHashModel(request, response, getObjectWrapper()));
		fmModel.put(FreemarkerServlet.KEY_REQUEST_PARAMETERS, new HttpRequestParametersHashModel(request));
		fmModel.putAll(model);
		return fmModel;
	}

	/**
	 * 为给定请求构建 FreeMarker {@link HttpSessionHashModel}，
	 * 同时检测 session 是否已存在并据此做出相应处理。
	 * @param request current HTTP request
	 * @param response current servlet response
	 * @return the FreeMarker HttpSessionHashModel
	 */
	private HttpSessionHashModel buildSessionModel(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			return new HttpSessionHashModel(session, getObjectWrapper());
		}
		else {
			return new HttpSessionHashModel(null, request, response, getObjectWrapper());
		}
	}

	/**
	 * 获取适用于给定 locale、供此视图渲染的 FreeMarker 模板。
	 * <p>默认会获取由 "url" bean 属性指定的模板。
	 * @param locale the current locale
	 * @return the FreeMarker template to render
	 * @throws IOException if the template file could not be retrieved
	 * @see #setUrl
	 * @see #getTemplate(String, java.util.Locale)
	 */
	protected Template getTemplate(Locale locale) throws IOException {
		String url = getUrl();
		Assert.state(url != null, "'url' not set");
		return getTemplate(url, locale);
	}

	/**
	 * 获取由给定名称指定的 FreeMarker 模板，
	 * 并使用由 "encoding" bean 属性指定的编码。
	 * <p>子类可以调用此方法来获取特定模板，
	 * 例如将多个模板渲染到单个视图中。
	 * @param name the file name of the desired template
	 * @param locale the current locale
	 * @return the FreeMarker template
	 * @throws IOException if the template file could not be retrieved
	 */
	protected Template getTemplate(String name, Locale locale) throws IOException {
		return (getEncoding() != null ?
				obtainConfiguration().getTemplate(name, locale, getEncoding()) :
				obtainConfiguration().getTemplate(name, locale));
	}

	/**
	 * 处理 FreeMarker 模板并将结果写入 servlet response。
	 * <p>可以重写此方法以自定义行为。
	 * @param template the template to process
	 * @param model the model for the template
	 * @param response servlet response (use this to get the OutputStream or Writer)
	 * @throws IOException if the template file could not be retrieved
	 * @throws TemplateException if thrown by FreeMarker
	 * @see freemarker.template.Template#process(Object, java.io.Writer)
	 */
	protected void processTemplate(Template template, SimpleHash model, HttpServletResponse response)
			throws IOException, TemplateException {

		template.process(model, response.getWriter());
	}


	/**
	 * 扩展 {@link GenericServlet} 的简单适配器类。
	 * FreeMarker 中的 JSP 访问需要使用它。
	 */
	@SuppressWarnings("serial")
	private static class GenericServletAdapter extends GenericServlet {

		@Override
		public void service(ServletRequest servletRequest, ServletResponse servletResponse) {
			// no-op
		}
	}


	/**
	 * {@link ServletConfig} 接口的内部实现，
	 * 将被传递给 servlet 适配器。
	 */
	private class DelegatingServletConfig implements ServletConfig {

		@Override
		@Nullable
		public String getServletName() {
			return FreeMarkerView.this.getBeanName();
		}

		@Override
		@Nullable
		public ServletContext getServletContext() {
			return FreeMarkerView.this.getServletContext();
		}

		@Override
		@Nullable
		public String getInitParameter(String paramName) {
			return null;
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(Collections.emptySet());
		}
	}

}
