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

package org.springframework.web.reactive.result.view;

import java.util.Locale;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * {@link ViewResolver}，允许将符号视图名称直接解析为 URL，而无需显式映射定义。如果符号名称以直接的方式与视图资源名称匹配（即符号名称是资源文件名的唯一部分），并且不需要为每个视图定义专用映射，则此解析器很有用。
 *
 * <p>支持 {@link AbstractUrlBasedView} 子类，例如 {@link org.springframework.web.reactive.result.view.freemarker.FreeMarkerView}。可以通过 "viewClass" 属性指定此解析器生成的所有视图的视图类。
 *
 * <p>视图名称可以是资源 URL 本身，也可以通过指定的前缀和/或后缀进行扩展。明确支持将持有 RequestContext 的属性导出到所有视图。
 *
 * <p>示例：prefix="templates/"、suffix=".ftl"、viewname="test" &rarr; "templates/test.ftl"
 *
 * <p>作为特殊功能，可以通过 "redirect:" 前缀指定重定向 URL。例如："redirect:myAction" 将触发重定向到给定 URL，而不是作为标准视图名称解析。这通常用于在完成表单工作流后重定向到控制器 URL。
 *
 * <p>注意：此类不支持本地化解析，即根据当前区域设置将符号视图名称解析为不同资源。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.0
 */
public class UrlBasedViewResolver extends ViewResolverSupport
		implements ViewResolver, ApplicationContextAware, InitializingBean {

	/**
	 * 指定重定向 URL 的特殊视图名称前缀（通常是在表单提交并处理之后重定向到控制器）。此类视图名称不会按配置的默认方式解析，而是被视为特殊快捷方式。
	 */
	public static final String REDIRECT_URL_PREFIX = "redirect:";


	@Nullable
	private Class<?> viewClass;

	private String prefix = "";

	private String suffix = "";

	@Nullable
	private String[] viewNames;

	private Function<String, RedirectView> redirectViewProvider = RedirectView::new;

	@Nullable
	private String requestContextAttribute;

	@Nullable
	private ApplicationContext applicationContext;


	/**
	 * 设置应用于创建视图的视图类。
	 * @param viewClass a class that is assignable to the required view class
	 * (by default: AbstractUrlBasedView)
	 * @see #requiredViewClass()
	 * @see #instantiateView()
	 * @see AbstractUrlBasedView
	 */
	public void setViewClass(@Nullable Class<?> viewClass) {
		if (viewClass != null && !requiredViewClass().isAssignableFrom(viewClass)) {
			String name = viewClass.getName();
			throw new IllegalArgumentException("Given view class [" + name + "] " +
					"is not of type [" + requiredViewClass().getName() + "]");
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
	 * 设置构建 URL 时追加到视图名称后的后缀。
	 */
	public void setSuffix(@Nullable String suffix) {
		this.suffix = (suffix != null ? suffix : "");
	}

	/**
	 * 返回构建 URL 时追加到视图名称后的后缀。
	 */
	protected String getSuffix() {
		return this.suffix;
	}

	/**
	 * 设置此 {@link ViewResolver} 可以处理的视图名称（或名称模式）。视图名称可以包含简单通配符，例如 'my*'、'*Report' 和 '*Repo*' 都将匹配视图名称 'myReport'。
	 * @see #canHandle
	 */
	public void setViewNames(@Nullable String... viewNames) {
		this.viewNames = viewNames;
	}

	/**
	 * 返回此 {@link ViewResolver} 可以处理的视图名称（或名称模式）。
	 */
	@Nullable
	protected String[] getViewNames() {
		return this.viewNames;
	}

	/**
	 * 基于 URL 的 {@link RedirectView} 提供程序，可用于提供例如具有自定义默认状态代码的重定向视图。
	 */
	public void setRedirectViewProvider(Function<String, RedirectView> redirectViewProvider) {
		this.redirectViewProvider = redirectViewProvider;
	}

	/**
	 * 为所有视图设置 {@link RequestContext} 属性的名称。
	 * @param requestContextAttribute name of the RequestContext attribute
	 * @see AbstractView#setRequestContextAttribute
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回所有视图的 {@link RequestContext} 属性名称（如果有）。
	 */
	@Nullable
	protected String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 接受包含的 {@code ApplicationContext}（如果有）。
	 * <p>用于初始化新创建的 {@link View} 实例、应用生命周期回调并提供对包含环境的访问。
	 * @see #setViewClass
	 * @see #createView
	 * @see #applyLifecycleMethods
	 */
	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * 返回包含的 {@code ApplicationContext}（如果有）。
	 * @see #setApplicationContext
	 */
	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (getViewClass() == null) {
			throw new IllegalArgumentException("Property 'viewClass' is required");
		}
	}


	@Override
	public Mono<View> resolveViewName(String viewName, Locale locale) {
		if (!canHandle(viewName, locale)) {
			return Mono.empty();
		}

		AbstractUrlBasedView urlBasedView;
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
			urlBasedView = this.redirectViewProvider.apply(redirectUrl);
		}
		else {
			urlBasedView = createView(viewName);
		}

		View view = applyLifecycleMethods(viewName, urlBasedView);
		try {
			return (urlBasedView.checkResourceExists(locale) ? Mono.just(view) : Mono.empty());
		}
		catch (Exception ex) {
			return Mono.error(ex);
		}
	}

	/**
	 * 指示此 {@link ViewResolver} 是否可以处理提供的视图名称。如果无法处理，则返回空结果。默认实现会检查配置的 {@link #setViewNames 视图名称}。
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
	 * 返回此解析器所需的视图类型。此实现返回 {@link AbstractUrlBasedView}。
	 * @see #instantiateView()
	 * @see AbstractUrlBasedView
	 */
	protected Class<?> requiredViewClass() {
		return AbstractUrlBasedView.class;
	}

	/**
	 * 实例化指定的视图类。
	 * <p>默认实现使用反射实例化该类。
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
	 * 创建指定视图类的新 View 实例并进行配置。
	 * <p><i>不</i>对预定义 View 实例执行任何查找。
	 * <p>无需在此调用 Bean 容器定义的 Spring 生命周期方法：如果 {@link #setApplicationContext ApplicationContext} 可用，它们将在之后自动应用。
	 * @param viewName the name of the view to build
	 * @return the View instance
	 * @see #getViewClass()
	 * @see #applyLifecycleMethods
	 */
	protected AbstractUrlBasedView createView(String viewName) {
		AbstractUrlBasedView view = instantiateView();
		view.setSupportedMediaTypes(getSupportedMediaTypes());
		view.setDefaultCharset(getDefaultCharset());
		view.setUrl(getPrefix() + viewName + getSuffix());

		String requestContextAttribute = getRequestContextAttribute();
		if (requestContextAttribute != null) {
			view.setRequestContextAttribute(requestContextAttribute);
		}

		return view;
	}

	/**
	 * 如果包含的 {@link ApplicationContext} 可用，则将其生命周期方法应用到给定 {@link View} 实例。
	 * @param viewName the name of the view
	 * @param view the freshly created View instance, pre-configured with
	 * {@link AbstractUrlBasedView}'s properties
	 * @return the {@link View} instance to use (either the original one
	 * or a decorated variant)
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
