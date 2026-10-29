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

package org.springframework.web.reactive.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.result.view.HttpMessageWriterView;
import org.springframework.web.reactive.result.view.UrlBasedViewResolver;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.reactive.result.view.script.ScriptTemplateConfigurer;
import org.springframework.web.reactive.result.view.script.ScriptTemplateViewResolver;

/**
 * 用于协助配置支持不同模板机制的 {@link ViewResolver} 链。
 *
 * <p>此外，还可以配置 {@link #defaultViews(View...) defaultViews}，以便根据所请求的内容类型（例如 JSON、XML 等）进行渲染。
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class ViewResolverRegistry {

	@Nullable
	private final ApplicationContext applicationContext;

	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	private final List<View> defaultViews = new ArrayList<>(4);

	@Nullable
	private Integer order;


	public ViewResolverRegistry(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}


	/**
	 * 注册一个后缀为 ".ftl" 的 {@code FreeMarkerViewResolver}。
	 * <p><strong>注意</strong>：还必须通过添加 {@link FreeMarkerConfigurer} bean 来配置 FreeMarker。
	 */
	public UrlBasedViewResolverRegistration freeMarker() {
		if (!checkBeanOfType(FreeMarkerConfigurer.class)) {
			throw new BeanInitializationException("In addition to a FreeMarker view resolver " +
					"there must also be a single FreeMarkerConfig bean in this web application context " +
					"(or its parent): FreeMarkerConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		FreeMarkerRegistration registration = new FreeMarkerRegistration();
		UrlBasedViewResolver resolver = registration.getViewResolver();
		if (this.applicationContext != null) {
			resolver.setApplicationContext(this.applicationContext);
		}
		this.viewResolvers.add(resolver);
		return registration;
	}

	/**
	 * 注册一个使用空前缀和后缀的脚本模板视图解析器。
	 * <p><strong>注意</strong>：还必须通过添加 {@link ScriptTemplateConfigurer} bean 来配置脚本模板。
	 * @since 5.0.4
	 */
	public UrlBasedViewResolverRegistration scriptTemplate() {
		if (!checkBeanOfType(ScriptTemplateConfigurer.class)) {
			throw new BeanInitializationException("In addition to a script template view resolver " +
					"there must also be a single ScriptTemplateConfig bean in this web application context " +
					"(or its parent): ScriptTemplateConfigurer is the usual implementation. " +
					"This bean may be given any name.");
		}
		ScriptRegistration registration = new ScriptRegistration();
		UrlBasedViewResolver resolver = registration.getViewResolver();
		if (this.applicationContext != null) {
			resolver.setApplicationContext(this.applicationContext);
		}
		this.viewResolvers.add(resolver);
		return registration;
	}

	/**
	 * 注册 {@link ViewResolver} bean 实例。这对于配置第三方解析器实现很有用，或者当本类中的其他注册方法未暴露某些需要设置的高级属性时，可作为它们的替代方案。
	 */
	public void viewResolver(ViewResolver viewResolver) {
		this.viewResolvers.add(viewResolver);
	}

	/**
	 * 设置与任意视图名称关联的默认视图，并根据所请求内容类型的最佳匹配进行选择。
	 * <p>使用 {@link HttpMessageWriterView HttpMessageWriterView} 适配并将任何现有的 {@code HttpMessageWriter}（例如 JSON、XML）用作 {@code View}。
	 */
	public void defaultViews(View... defaultViews) {
		this.defaultViews.addAll(Arrays.asList(defaultViews));
	}

	/**
	 * 是否已注册任何视图解析器。
	 */
	public boolean hasRegistrations() {
		return (!this.viewResolvers.isEmpty());
	}

	/**
	 * 设置 {@link org.springframework.web.reactive.result.view.ViewResolutionResultHandler ViewResolutionResultHandler} 的顺序。
	 * <p>默认不设置此属性，这意味着结果处理器的顺序为 {@link Ordered#LOWEST_PRECEDENCE}。
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
		return this.viewResolvers;
	}

	protected List<View> getDefaultViews() {
		return this.defaultViews;
	}


	private static class FreeMarkerRegistration extends UrlBasedViewResolverRegistration {

		public FreeMarkerRegistration() {
			super(new FreeMarkerViewResolver());
			getViewResolver().setSuffix(".ftl");
		}
	}

	private static class ScriptRegistration extends UrlBasedViewResolverRegistration {

		public ScriptRegistration() {
			super(new ScriptTemplateViewResolver());
			getViewResolver();
		}
	}

}
