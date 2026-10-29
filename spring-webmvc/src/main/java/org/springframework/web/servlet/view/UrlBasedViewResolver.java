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

package org.springframework.web.servlet.view;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.servlet.View;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 接口的简单实现，
 * 允许将符号视图名称直接解析为 URL，而无需显式的映射定义。
 * 如果你的符号名称以简单直接的方式与视图资源的名称匹配
 * （即符号名称是资源文件名中唯一的部分），
 * 并且无需为每个视图定义专门的映射，那么此实现非常有用。
 *
 * <p>支持 {@link AbstractUrlBasedView} 的子类，例如 {@link InternalResourceView}
 * 和 {@link org.springframework.web.servlet.view.freemarker.FreeMarkerView}。
 * 此解析器生成的所有视图的视图类可以通过 "viewClass" 属性指定。
 *
 * <p>视图名称既可以是资源 URL 本身，也可以通过指定的前缀和/或后缀进行扩充。
 * 显式支持向所有视图导出保存有 RequestContext 的属性。
 *
 * <p>示例：prefix="/WEB-INF/jsp/"、suffix=".jsp"、viewname="test" &rarr;
 * "/WEB-INF/jsp/test.jsp"
 *
 * <p>作为一项特殊功能，可以通过 "redirect:" 前缀指定重定向 URL。
 * 例如："redirect:myAction" 将触发到给定 URL 的重定向，
 * 而不是将其作为标准视图名称解析。这通常用于在完成表单工作流后重定向到控制器 URL。
 *
 * <p>此外，可以通过 "forward:" 前缀指定转发 URL。
 * 例如："forward:myAction" 将触发到给定 URL 的转发，而不是将其作为标准视图名称解析。
 * 这通常用于控制器 URL；不应将其用于 JSP URL —— 这类场景请使用逻辑视图名称。
 *
 * <p>注意：此类不支持本地化解析，即根据当前 locale 将符号视图名称解析为不同的资源。
 *
 * <p><b>注意：</b>当链接多个 ViewResolver 时，UrlBasedViewResolver 会检查
 * {@linkplain AbstractUrlBasedView#checkResource 指定的资源是否实际存在}。
 * 但对于 {@link InternalResourceView}，通常无法预先确定目标资源是否存在。
 * 在这种情况下，UrlBasedViewResolver 对任何给定的视图名称都会始终返回一个 View；
 * 因此，应将其配置为链中的最后一个 ViewResolver。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 13.12.2003
 * @see #setViewClass
 * @see #setPrefix
 * @see #setSuffix
 * @see #setRequestContextAttribute
 * @see #REDIRECT_URL_PREFIX
 * @see AbstractUrlBasedView
 * @see InternalResourceView
 * @see org.springframework.web.servlet.view.freemarker.FreeMarkerView
 */
public class UrlBasedViewResolver extends AbstractCachingViewResolver implements Ordered {

	/**
	 * 用于指定重定向 URL（通常是在表单提交并处理完毕后重定向到控制器）的特殊视图名称前缀。
	 * 此类视图名称不会按配置的默认方式解析，而是作为特殊的快捷方式处理。
	 */
	public static final String REDIRECT_URL_PREFIX = "redirect:";

	/**
	 * 用于指定转发 URL（通常是在表单提交并处理完毕后转发到控制器）的特殊视图名称前缀。
	 * 此类视图名称不会按配置的默认方式解析，而是作为特殊的快捷方式处理。
	 */
	public static final String FORWARD_URL_PREFIX = "forward:";


	@Nullable
	private Class<?> viewClass;

	private String prefix = "";

	private String suffix = "";

	@Nullable
	private String contentType;

	private boolean redirectContextRelative = true;

	private boolean redirectHttp10Compatible = true;

	@Nullable
	private String[] redirectHosts;

	@Nullable
	private String requestContextAttribute;

	/** 静态属性映射，以属性名称（String）为键。 */
	private final Map<String, Object> staticAttributes = new HashMap<>();

	@Nullable
	private Boolean exposePathVariables;

	@Nullable
	private Boolean exposeContextBeansAsAttributes;

	@Nullable
	private String[] exposedContextBeanNames;

	@Nullable
	private String[] viewNames;

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * 设置用于创建视图的视图类。
	 * @param viewClass a class that is assignable to the required view class
	 * (by default: AbstractUrlBasedView)
	 * @see #requiredViewClass()
	 * @see #instantiateView()
	 * @see AbstractUrlBasedView
	 */
	public void setViewClass(@Nullable Class<?> viewClass) {
		if (viewClass != null && !requiredViewClass().isAssignableFrom(viewClass)) {
			throw new IllegalArgumentException("Given view class [" + viewClass.getName() +
					"] is not of type [" + requiredViewClass().getName() + "]");
		}
		this.viewClass = viewClass;
	}

	/**
	 * 返回用于创建视图的视图类。
	 * @see #setViewClass
	 */
	@Nullable
	protected Class<?> getViewClass() {
		return this.viewClass;
	}

	/**
	 * 设置构建 URL 时添加到视图名称前面的前缀。
	 */
	public void setPrefix(@Nullable String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回构建 URL 时添加到视图名称前面的前缀。
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 设置构建 URL 时追加到视图名称后面的后缀。
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回构建 URL 时追加到视图名称后面的后缀。
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * 设置所有视图的内容类型。
	 * <p>如果假定视图本身会设置内容类型（例如 JSP 的情况），
	 * 视图类可能会忽略此设置。
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回所有视图的内容类型（如果有）。
	 */
	@Nullable
	protected String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置是否将以斜杠（"/"）开头的给定重定向 URL 解释为相对于当前 ServletContext，
	 * 即相对于 Web 应用根路径。
	 * <p>默认值为 "true"：以斜杠开头的重定向 URL 将被解释为相对于 Web 应用根路径，
	 * 即会将 context path 添加到 URL 前面。
	 * <p><b>重定向 URL 可以通过 "redirect:" 前缀指定。</b>
	 * 例如："redirect:myAction"
	 * @see RedirectView#setContextRelative
	 * @see #REDIRECT_URL_PREFIX
	 */
	public void setRedirectContextRelative(boolean redirectContextRelative) {
		this.redirectContextRelative = redirectContextRelative;
	}

	/**
	 * 返回是否将以斜杠（"/"）开头的给定重定向 URL 解释为相对于当前 ServletContext，
	 * 即相对于 Web 应用根路径。
	 */
	protected boolean isRedirectContextRelative() {
		return this.redirectContextRelative;
	}

	/**
	 * 设置重定向是否应保持与 HTTP 1.0 客户端兼容。
	 * <p>在默认实现中，无论如何都会强制使用 HTTP 状态码 302，
	 * 即委托给 {@code HttpServletResponse.sendRedirect}。
	 * 关闭此选项将发送 HTTP 状态码 303，这是适用于 HTTP 1.1 客户端的正确状态码，
	 * 但 HTTP 1.0 客户端无法理解。
	 * <p>许多 HTTP 1.1 客户端对待 302 与 303 的方式相同，并无区别。
	 * 不过，有些客户端在 POST 请求之后进行重定向时依赖 303；
	 * 在这种场景下请关闭此标志。
	 * <p><b>重定向 URL 可以通过 "redirect:" 前缀指定。</b>
	 * 例如："redirect:myAction"
	 * @see RedirectView#setHttp10Compatible
	 * @see #REDIRECT_URL_PREFIX
	 */
	public void setRedirectHttp10Compatible(boolean redirectHttp10Compatible) {
		this.redirectHttp10Compatible = redirectHttp10Compatible;
	}

	/**
	 * 返回重定向是否应保持与 HTTP 1.0 客户端兼容。
	 */
	protected boolean isRedirectHttp10Compatible() {
		return this.redirectHttp10Compatible;
	}

	/**
	 * 配置与应用关联的一个或多个主机。
	 * 所有其他主机都将被视为外部主机。
	 * <p>实际上，此属性提供了一种方式：对于带有主机且该主机未被列为已知主机的 URL，
	 * 在重定向时关闭通过 {@link HttpServletResponse#encodeRedirectURL} 进行的编码。
	 * <p>如果未设置（默认情况），则所有 URL 都会通过响应进行编码。
	 * @param redirectHosts one or more application hosts
	 * @since 4.3
	 */
	public void setRedirectHosts(@Nullable String... redirectHosts) {
		this.redirectHosts = redirectHosts;
	}

	/**
	 * 返回为重定向目的配置的应用主机。
	 * @since 4.3
	 */
	@Nullable
	public String[] getRedirectHosts() {
		return this.redirectHosts;
	}

	/**
	 * 设置所有视图的 RequestContext 属性名称。
	 * @param requestContextAttribute name of the RequestContext attribute
	 * @see AbstractView#setRequestContextAttribute
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回所有视图的 RequestContext 属性名称（如果有）。
	 */
	@Nullable
	protected String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 从此解析器返回的所有视图的 {@code java.util.Properties} 对象中设置静态属性。
	 * <p>这是设置静态属性最便捷的方式。请注意，如果 model 中包含同名值，
	 * 静态属性可以被动态属性覆盖。
	 * <p>可以通过 String 类型的 "value"（由 PropertiesEditor 解析）
	 * 或 XML bean 定义中的 "props" 元素来填充。
	 * @see org.springframework.beans.propertyeditors.PropertiesEditor
	 * @see AbstractView#setAttributes
	 */
	public void setAttributes(Properties props) {
		CollectionUtils.mergePropertiesIntoMap(props, this.staticAttributes);
	}

	/**
	 * 从此解析器返回的所有视图的 Map 中设置静态属性。
	 * 这样可以设置任意类型的属性值，例如 bean 引用。
	 * <p>可以通过 XML bean 定义中的 "map" 或 "props" 元素来填充。
	 * @param attributes a Map with name Strings as keys and attribute objects as values
	 * @see AbstractView#setAttributesMap
	 */
	public void setAttributesMap(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			this.staticAttributes.putAll(attributes);
		}
	}

	/**
	 * 允许以 {@code Map} 方式访问此解析器返回视图的静态属性，
	 * 并可以选择添加或覆盖特定条目。
	 * <p>这对于直接指定条目非常有用，例如通过 {@code attributesMap[myKey]}。
	 * 它在添加或覆盖子视图定义中的条目时尤其有用。
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * 指定此解析器解析的视图是否应将路径变量添加到 model 中。
	 * <p>默认设置是由每个 View 自行决定
	 * （参见 {@link AbstractView#setExposePathVariables}）。
	 * 不过，你可以使用此属性覆盖该行为。
	 * @param exposePathVariables
	 * <ul>
	 * <li>{@code true} - 此解析器解析的所有 View 都将暴露路径变量
	 * <li>{@code false} - 此解析器解析的任何 View 都不暴露路径变量
	 * <li>{@code null} - 各个 View 可以自行决定（默认使用此设置）
	 * </ul>
	 * @see AbstractView#setExposePathVariables
	 */
	public void setExposePathVariables(@Nullable Boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * 返回此解析器解析的视图是否应将路径变量添加到 model 中。
	 */
	@Nullable
	protected Boolean getExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * 设置是否通过惰性检查（在访问属性时进行检查）使应用上下文中的所有 Spring bean
	 * 可作为请求属性访问。
	 * <p>这将使这些 bean 可以在 JSP 2.0 页面中的普通 {@code ${...}}
	 * 表达式中访问，也可以在 JSTL 的 {@code c:out} 值表达式中访问。
	 * <p>默认值为 "false"。
	 * @see AbstractView#setExposeContextBeansAsAttributes
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	@Nullable
	protected Boolean getExposeContextBeansAsAttributes() {
		return this.exposeContextBeansAsAttributes;
	}

	/**
	 * 指定上下文中应被暴露的 bean 名称。
	 * 如果此值非 null，则只有指定的 bean 才能作为属性暴露。
	 * @see AbstractView#setExposedContextBeanNames
	 */
	public void setExposedContextBeanNames(@Nullable String... exposedContextBeanNames) {
		this.exposedContextBeanNames = exposedContextBeanNames;
	}

	@Nullable
	protected String[] getExposedContextBeanNames() {
		return this.exposedContextBeanNames;
	}

	/**
	 * 设置此 {@link org.springframework.web.servlet.ViewResolver} 可以处理的
	 * 视图名称（或名称模式）。视图名称可以包含简单通配符，
	 * 例如 'my*'、'*Report' 和 '*Repo*' 都将匹配视图名称 'myReport'。
	 * @see #canHandle
	 */
	public void setViewNames(@Nullable String... viewNames) {
		this.viewNames = viewNames;
	}

	/**
	 * 返回此 {@link org.springframework.web.servlet.ViewResolver} 可以处理的
	 * 视图名称（或名称模式）。
	 */
	@Nullable
	protected String[] getViewNames() {
		return this.viewNames;
	}

	/**
	 * 指定此 ViewResolver bean 的 order 值。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示不排序。
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	protected void initApplicationContext() {
		super.initApplicationContext();
		if (getViewClass() == null) {
			throw new IllegalArgumentException("Property 'viewClass' is required");
		}
	}


	/**
	 * 此实现仅返回视图名称，因为此 ViewResolver 不支持本地化解析。
	 */
	@Override
	protected Object getCacheKey(String viewName, Locale locale) {
		return viewName;
	}

	/**
	 * 重写此方法以实现对 "redirect:" 前缀的检查。
	 * <p>在 {@code loadView} 中无法实现这一点，因为子类中重写的
	 * {@code loadView} 版本可能依赖父类始终创建所需视图类的实例。
	 * @see #loadView
	 * @see #requiredViewClass
	 */
	@Override
	protected View createView(String viewName, Locale locale) throws Exception {
		// If this resolver is not supposed to handle the given view,
		// return null to pass on to the next resolver in the chain.
		if (!canHandle(viewName, locale)) {
			return null;
		}

		// Check for special "redirect:" prefix.
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			RedirectView view = new RedirectView(redirectUrl,
					isRedirectContextRelative(), isRedirectHttp10Compatible());
			String[] hosts = getRedirectHosts();
			if (hosts != null) {
				view.setHosts(hosts);
			}
			return applyLifecycleMethods(REDIRECT_URL_PREFIX, view);
		}

		// Check for special "forward:" prefix.
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
			String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
			InternalResourceView view = new InternalResourceView(forwardUrl);
			return applyLifecycleMethods(FORWARD_URL_PREFIX, view);
		}

		// Else fall back to superclass implementation: calling loadView.
		return super.createView(viewName, locale);
	}

	/**
	 * 指示此 {@link org.springframework.web.servlet.ViewResolver} 能否处理提供的视图名称。
	 * 如果不能，{@link #createView(String, java.util.Locale)} 将返回 {@code null}。
	 * 默认实现会根据已配置的 {@link #setViewNames 视图名称}进行检查。
	 * @param viewName the name of the view to retrieve
	 * @param locale the Locale to retrieve the view for
	 * @return whether this resolver applies to the specified view
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean canHandle(String viewName, Locale locale) {
		String[] viewNames = getViewNames();
		return (viewNames == null || PatternMatchUtils.simpleMatch(viewNames, viewName));
	}

	/**
	 * 返回此解析器所需的视图类型。
	 * 此实现返回 {@link AbstractUrlBasedView}。
	 * @see #instantiateView()
	 * @see AbstractUrlBasedView
	 */
	protected Class<?> requiredViewClass() {
		return AbstractUrlBasedView.class;
	}

	/**
	 * 实例化指定的视图类。
	 * <p>默认实现使用反射来实例化该类。
	 * @return a new instance of the view class
	 * @since 5.3
	 * @see #setViewClass
	 */
	protected AbstractUrlBasedView instantiateView() {
		Class<?> viewClass = getViewClass();
		Assert.state(viewClass != null, "No view class");
		return (AbstractUrlBasedView) BeanUtils.instantiateClass(viewClass);
	}

	/**
	 * 委托给 {@code buildView} 来创建指定视图类的新实例。
	 * 会应用以下 Spring 生命周期方法（由通用 Spring bean 工厂支持）：
	 * <ul>
	 * <li>ApplicationContextAware 的 {@code setApplicationContext}
	 * <li>InitializingBean 的 {@code afterPropertiesSet}
	 * </ul>
	 * @param viewName the name of the view to retrieve
	 * @return the View instance
	 * @throws Exception if the view couldn't be resolved
	 * @see #buildView(String)
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		AbstractUrlBasedView view = buildView(viewName);
		View result = applyLifecycleMethods(viewName, view);
		return (view.checkResource(locale) ? result : null);
	}

	/**
	 * 创建指定视图类的新 View 实例并对其进行配置。
	 * <i>不会</i> 查找预定义的 View 实例。
	 * <p>此处无需调用由 bean 容器定义的 Spring 生命周期方法；
	 * 这些方法将在此方法返回后由 {@code loadView} 方法应用。
	 * <p>子类通常应先调用 {@code super.buildView(viewName)}，
	 * 然后再自行设置其他属性。{@code loadView} 随后将在此过程结束时
	 * 应用 Spring 生命周期方法。
	 * @param viewName the name of the view to build
	 * @return the View instance
	 * @throws Exception if the view couldn't be resolved
	 * @see #loadView(String, java.util.Locale)
	 */
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		AbstractUrlBasedView view = instantiateView();
		view.setUrl(getPrefix() + viewName + getSuffix());
		view.setAttributesMap(getAttributesMap());

		String contentType = getContentType();
		if (contentType != null) {
			view.setContentType(contentType);
		}

		String requestContextAttribute = getRequestContextAttribute();
		if (requestContextAttribute != null) {
			view.setRequestContextAttribute(requestContextAttribute);
		}

		Boolean exposePathVariables = getExposePathVariables();
		if (exposePathVariables != null) {
			view.setExposePathVariables(exposePathVariables);
		}
		Boolean exposeContextBeansAsAttributes = getExposeContextBeansAsAttributes();
		if (exposeContextBeansAsAttributes != null) {
			view.setExposeContextBeansAsAttributes(exposeContextBeansAsAttributes);
		}
		String[] exposedContextBeanNames = getExposedContextBeanNames();
		if (exposedContextBeanNames != null) {
			view.setExposedContextBeanNames(exposedContextBeanNames);
		}

		return view;
	}

	/**
	 * 如果存在包含 {@link ApplicationContext}，则将它的生命周期方法
	 * 应用到给定的 {@link View} 实例上。
	 * @param viewName the name of the view
	 * @param view the freshly created View instance, pre-configured with
	 * {@link AbstractUrlBasedView}'s properties
	 * @return the {@link View} instance to use (either the original one
	 * or a decorated variant)
	 * @since 5.0
	 * @see #getApplicationContext()
	 * @see ApplicationContext#getAutowireCapableBeanFactory()
	 * @see org.springframework.beans.factory.config.AutowireCapableBeanFactory#initializeBean
	 */
	protected View applyLifecycleMethods(String viewName, AbstractUrlBasedView view) {
		ApplicationContext context = getApplicationContext();
		if (context != null) {
			Object initialized = context.getAutowireCapableBeanFactory().initializeBean(view, viewName);
			if (initialized instanceof View) {
				return (View) initialized;
			}
		}
		return view;
	}

}
