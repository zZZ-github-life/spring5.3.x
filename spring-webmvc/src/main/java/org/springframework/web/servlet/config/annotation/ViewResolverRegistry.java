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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;

/**
 * 辅助配置由
 * {@link org.springframework.web.servlet.ViewResolver ViewResolver} 实例组成的链。
 * 此类预期通过 {@link WebMvcConfigurer#configureViewResolvers} 使用。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ViewResolverRegistry {

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private ContentNegotiatingViewResolver contentNegotiatingResolver;

	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	@Nullable
	private Integer order;


	/**
	 * 带有 {@link ContentNegotiationManager} 和 {@link ApplicationContext} 的类构造函数。
	 * @since 4.3.12
	 */
	public ViewResolverRegistry(
			ContentNegotiationManager contentNegotiationManager, @Nullable ApplicationContext context) {

		this.contentNegotiationManager = contentNegotiationManager;
		this.applicationContext = context;
	}


	/**
	 * 是否已注册任何视图解析器。
	 */
	public boolean hasRegistrations() {
		return (this.contentNegotiatingResolver != null || !this.viewResolvers.isEmpty());
	}

	/**
	 * 启用 {@link ContentNegotiatingViewResolver}，使其位于所有其他已配置视图解析器的前面，
	 * 并根据客户端请求的媒体类型（例如在 Accept 标头中）在所有选中的 View 中进行选择。
	 * <p>如果多次调用，提供的默认视图将被添加到任何可能已配置的其他默认视图中。
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(View... defaultViews) {
		initContentNegotiatingViewResolver(defaultViews);
	}

	/**
	 * 启用 {@link ContentNegotiatingViewResolver}，使其位于所有其他已配置视图解析器的前面，
	 * 并根据客户端请求的媒体类型（例如在 Accept 标头中）在所有选中的 View 中进行选择。
	 * <p>如果多次调用，提供的默认视图将被添加到任何可能已配置的其他默认视图中。
	 * @see ContentNegotiatingViewResolver#setDefaultViews
	 */
	public void enableContentNegotiation(boolean useNotAcceptableStatus, View... defaultViews) {
		ContentNegotiatingViewResolver vr = initContentNegotiatingViewResolver(defaultViews);
		vr.setUseNotAcceptableStatusCode(useNotAcceptableStatus);
	}

	private ContentNegotiatingViewResolver initContentNegotiatingViewResolver(View[] defaultViews) {
		// ContentNegotiatingResolver in the registry: elevate its precedence!
		this.order = (this.order != null ? this.order : Ordered.HIGHEST_PRECEDENCE);

		if (this.contentNegotiatingResolver != null) {
			if (!ObjectUtils.isEmpty(defaultViews) &&
					!CollectionUtils.isEmpty(this.contentNegotiatingResolver.getDefaultViews())) {
				List<View> views = new ArrayList<>(this.contentNegotiatingResolver.getDefaultViews());
				views.addAll(Arrays.asList(defaultViews));
				this.contentNegotiatingResolver.setDefaultViews(views);
			}
		}
		else {
			this.contentNegotiatingResolver = new ContentNegotiatingViewResolver();
			this.contentNegotiatingResolver.setDefaultViews(Arrays.asList(defaultViews));
			this.contentNegotiatingResolver.setViewResolvers(this.viewResolvers);
			if (this.contentNegotiationManager != null) {
				this.contentNegotiatingResolver.setContentNegotiationManager(this.contentNegotiationManager);
			}
		}
		return this.contentNegotiatingResolver;
	}

	/**
	 * 注册 JSP 视图解析器，默认视图名前缀为 "/WEB-INF/"，默认后缀为 ".jsp"。
	 * <p>当多次调用此方法时，每次调用都会注册一个新的 ViewResolver 实例。
	 * 请注意，由于不转发到 JSP 就难以判断其是否存在，
	 * 使用多个基于 JSP 的视图解析器只有在结合解析器上的 "viewNames" 属性
	 * （指明哪些视图名称由哪个解析器处理）时才有意义。
	 */
	public UrlBasedViewResolverRegistration jsp() {
		return jsp("/WEB-INF/", ".jsp");
	}

	/**
	 * 使用指定的前缀和后缀注册 JSP 视图解析器。
	 * <p>当多次调用此方法时，每次调用都会注册一个新的 ViewResolver 实例。
	 * 请注意，由于不转发到 JSP 就难以判断其是否存在，
	 * 使用多个基于 JSP 的视图解析器只有在结合解析器上的 "viewNames" 属性
	 * （指明哪些视图名称由哪个解析器处理）时才有意义。
	 */
	public UrlBasedViewResolverRegistration jsp(String prefix, String suffix) {
		InternalResourceViewResolver resolver = new InternalResourceViewResolver();
		resolver.setPrefix(prefix);
		resolver.setSuffix(suffix);
		this.viewResolvers.add(resolver);
		return new UrlBasedViewResolverRegistration(resolver);
	}

	/**
	 * 注册 Tiles 3.x 视图解析器。
	 * <p><strong>注意</strong>，还必须通过添加
	 * {@link org.springframework.web.servlet.view.tiles3.TilesConfigurer} bean 来配置 Tiles。
	 */
	public UrlBasedViewResolverRegistration tiles() {
		if (!checkBeanOfType(TilesConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Tiles view resolver " +
					"there must also be a single TilesConfigurer bean in this web application context " +
					"(or its parent).");
		}
		TilesRegistration registration = new TilesRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 注册 FreeMarker 视图解析器，默认视图名前缀为空，默认后缀为 ".ftl"。
	 * <p><strong>注意</strong>，还必须通过添加
	 * {@link org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer} bean
	 * 来配置 FreeMarker。
	 */
	public UrlBasedViewResolverRegistration freeMarker() {
		if (!checkBeanOfType(FreeMarkerConfigurer.class)) {
			throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
					"there must also be a single FreeMarkerConfig bean in this web application context " +
					"(or its parent): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		FreeMarkerRegistration registration = new FreeMarkerRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 注册 Groovy 标记视图解析器，默认视图名前缀为空，默认后缀为 ".tpl"。
	 */
	public UrlBasedViewResolverRegistration groovy() {
		if (!checkBeanOfType(GroovyMarkupConfigurer.class)) {
			throw new BeanInitializationException("In addition to a Groovy markup view resolver " +
					"there must also be a single GroovyMarkupConfig bean in this web application context " +
					"(or its parent): GroovyMarkupConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		GroovyMarkupRegistration registration = new GroovyMarkupRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 注册脚本模板视图解析器，默认视图名前缀和后缀均为空。
	 * @since 4.2
	 */
	public UrlBasedViewResolverRegistration scriptTemplate() {
		if (!checkBeanOfType(ScriptTemplateConfigurer.class)) {
			throw new BeanInitializationException("In addition to a script template view resolver " +
					"there must also be a single ScriptTemplateConfig bean in this web application context " +
					"(or its parent): ScriptTemplateConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		ScriptRegistration registration = new ScriptRegistration();
		this.viewResolvers.add(registration.getViewResolver());
		return registration;
	}

	/**
	 * 注册 bean 名称视图解析器，将视图名称解释为
	 * {@link org.springframework.web.servlet.View} bean 的名称。
	 */
	public void beanName() {
		BeanNameViewResolver resolver = new BeanNameViewResolver();
		this.viewResolvers.add(resolver);
	}

	/**
	 * 注册 {@link ViewResolver} bean 实例。这对于配置自定义（或第三方）解析器实现可能很有用。
	 * 当此类中的其他注册方法未暴露某些需要设置的更高级属性时，
	 * 它也可以作为这些方法的替代方案。
	 */
	public void viewResolver(ViewResolver viewResolver) {
		if (viewResolver instanceof ContentNegotiatingViewResolver) {
			throw new BeanInitializationException(
					"addViewResolver cannot be used to configure a ContentNegotiatingViewResolver. " +
					"Please use the method enableContentNegotiation instead.");
		}
		this.viewResolvers.add(viewResolver);
	}

	/**
	 * 通过此注册表注册的 ViewResolver 被封装在
	 * {@link org.springframework.web.servlet.view.ViewResolverComposite ViewResolverComposite}
	 * 实例中，并遵循注册顺序。
	 * 此属性决定 ViewResolverComposite 本身相对于 Spring 配置中存在的
	 * 任何其他 ViewResolver（未在此注册）的顺序。
	 * <p>默认未设置此属性，这意味着该解析器排序为
	 * {@link Ordered#LOWEST_PRECEDENCE}，除非启用了内容协商，
	 * 此时（如果未显式设置）顺序将改为
	 * {@link Ordered#HIGHEST_PRECEDENCE}。
	 */
	public void order(int order) {
		this.order = order;
	}


	private boolean checkBeanOfType(Class<?> beanType) {
		return (this.applicationContext == null ||
				!ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
						this.applicationContext, beanType, false, false)));
	}

	protected int getOrder() {
		return (this.order != null ? this.order : Ordered.LOWEST_PRECEDENCE);
	}

	protected List<ViewResolver> getViewResolvers() {
		if (this.contentNegotiatingResolver != null) {
			return Collections.singletonList(this.contentNegotiatingResolver);
		}
		else {
			return this.viewResolvers;
		}
	}


	private static class TilesRegistration extends UrlBasedViewResolverRegistration {

		public TilesRegistration() {
			super(new TilesViewResolver());
		}
	}

	private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

		public FreeMarkerRegistration() {
			super(new FreeMarkerViewResolver());
			getViewResolver().setSuffix(".ftl");
		}
	}


	private static class GroovyMarkupRegistration extends UrlBasedViewResolverRegistration {

		public GroovyMarkupRegistration() {
			super(new GroovyMarkupViewResolver());
			getViewResolver().setSuffix(".tpl");
		}
	}


	private static class ScriptRegistration extends UrlBasedViewResolverRegistration {

		public ScriptRegistration() {
			super(new ScriptTemplateViewResolver());
			getViewResolver();
		}
	}

}
