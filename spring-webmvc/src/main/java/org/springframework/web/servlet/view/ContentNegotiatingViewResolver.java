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

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * {@link ViewResolver} 的实现，根据请求的文件名
 * 或 {@code Accept} 标头解析视图。
 *
 * <p>{@code ContentNegotiatingViewResolver} 本身不解析视图，而是委托给
 * 其他 {@link ViewResolver ViewResolvers}。默认情况下，这些其他视图解析器会从应用上下文中
 * 自动获取，不过也可以通过
 * {@link #setViewResolvers viewResolvers} 属性显式设置。<strong>注意</strong>，为使此
 * 视图解析器正常工作，需要将 {@link #setOrder order} 属性设置为比其他视图解析器
 * 更高的优先级（默认为 {@link Ordered#HIGHEST_PRECEDENCE}）。
 *
 * <p>此视图解析器使用请求的{@linkplain MediaType 媒体类型}为请求选择合适的
 * {@link View}。请求的媒体类型通过所配置的
 * {@link ContentNegotiationManager} 确定。一旦确定了请求的媒体类型，此解析器
 * 就会向每个委托视图解析器查询 {@link View}，并判断请求的媒体类型
 * 是否与视图的{@linkplain View#getContentType() 内容类型}
 * {@linkplain MediaType#includes(MediaType) 兼容}。将返回最兼容的视图。
 *
 * <p>此外，此视图解析器还公开了 {@link #setDefaultViews(List) defaultViews} 属性，
 * 允许你覆盖由各视图解析器提供的视图。请注意，这些默认视图作为候选项提供，
 * 仍然需要匹配所请求的内容类型（通过上文所述的文件扩展名、参数
 * 或 {@code Accept} 标头）。
 *
 * <p>例如，如果请求路径为 {@code /view.html}，此视图解析器将查找
 * 具有 {@code text/html} 内容类型的视图（基于 {@code html} 文件扩展名）。带有 {@code text/html} 请求 {@code Accept} 标头的
 * {@code /view} 请求会得到相同的结果。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 3.0
 * @see ViewResolver
 * @see InternalResourceViewResolver
 * @see BeanNameViewResolver
 */
public class ContentNegotiatingViewResolver extends WebApplicationObjectSupport
		implements ViewResolver, Ordered, InitializingBean {

	@Nullable
	private ContentNegotiationManager contentNegotiationManager;

	private final ContentNegotiationManagerFactoryBean cnmFactoryBean = new ContentNegotiationManagerFactoryBean();

	private boolean useNotAcceptableStatusCode = false;

	@Nullable
	private List<View> defaultViews;

	@Nullable
	private List<ViewResolver> viewResolvers;

	private int order = Ordered.HIGHEST_PRECEDENCE;


	/**
	 * 设置用于确定请求媒体类型的 {@link ContentNegotiationManager}。
	 * <p>如果未设置，将使用 ContentNegotiationManager 的默认构造函数，
	 * 从而应用 {@link org.springframework.web.accept.HeaderContentNegotiationStrategy}。
	 * @see ContentNegotiationManager#ContentNegotiationManager()
	 */
	public void setContentNegotiationManager(@Nullable ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回用于确定请求媒体类型的 {@link ContentNegotiationManager}。
	 * @since 4.1.9
	 */
	@Nullable
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	/**
	 * 指示在找不到合适视图时是否应返回
	 * {@link HttpServletResponse#SC_NOT_ACCEPTABLE 406 Not Acceptable} 状态码。
	 * <p>默认值为 {@code false}，这意味着当找不到可接受的视图时，此视图解析器
	 * 对 {@link #resolveViewName(String, Locale)} 返回 {@code null}。
	 * 这将允许视图解析器链。当此属性设为 {@code true} 时，
	 * {@link #resolveViewName(String, Locale)} 将改为返回一个将响应状态设为
	 * {@code 406 Not Acceptable} 的视图。
	 */
	public void setUseNotAcceptableStatusCode(boolean useNotAcceptableStatusCode) {
		this.useNotAcceptableStatusCode = useNotAcceptableStatusCode;
	}

	/**
	 * 在无法从 {@link ViewResolver} 链获得更具体的视图时，
	 * 是否返回 HTTP 406 状态码。
	 */
	public boolean isUseNotAcceptableStatusCode() {
		return this.useNotAcceptableStatusCode;
	}

	/**
	 * 设置当无法从 {@link ViewResolver} 链获得更具体的视图时
	 * 要使用的默认视图。
	 */
	public void setDefaultViews(List<View> defaultViews) {
		this.defaultViews = defaultViews;
	}

	public List<View> getDefaultViews() {
		return (this.defaultViews != null ? Collections.unmodifiableList(this.defaultViews) :
				Collections.emptyList());
	}

	/**
	 * 设置要由此视图解析器包装的视图解析器。
	 * <p>如果未设置此属性，视图解析器将被自动检测。
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	public List<ViewResolver> getViewResolvers() {
		return (this.viewResolvers != null ? Collections.unmodifiableList(this.viewResolvers) :
				Collections.emptyList());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}


	@Override
	protected void initServletContext(ServletContext servletContext) {
		Collection<ViewResolver> matchingBeans =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(obtainApplicationContext(), ViewResolver.class).values();
		if (this.viewResolvers == null) {
			this.viewResolvers = new ArrayList<>(matchingBeans.size());
			for (ViewResolver viewResolver : matchingBeans) {
				if (this != viewResolver) {
					this.viewResolvers.add(viewResolver);
				}
			}
		}
		else {
			for (int i = 0; i < this.viewResolvers.size(); i++) {
				ViewResolver vr = this.viewResolvers.get(i);
				if (matchingBeans.contains(vr)) {
					continue;
				}
				String name = vr.getClass().getName() + i;
				obtainApplicationContext().getAutowireCapableBeanFactory().initializeBean(vr, name);
			}

		}
		AnnotationAwareOrderComparator.sort(this.viewResolvers);
		this.cnmFactoryBean.setServletContext(servletContext);
	}

	@Override
	public void afterPropertiesSet() {
		if (this.contentNegotiationManager == null) {
			this.contentNegotiationManager = this.cnmFactoryBean.build();
		}
		if (this.viewResolvers == null || this.viewResolvers.isEmpty()) {
			logger.warn("No ViewResolvers configured");
		}
	}


	@Override
	@Nullable
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
		Assert.state(attrs instanceof ServletRequestAttributes, "No current ServletRequestAttributes");
		List<MediaType> requestedMediaTypes = getMediaTypes(((ServletRequestAttributes) attrs).getRequest());
		if (requestedMediaTypes != null) {
			List<View> candidateViews = getCandidateViews(viewName, locale, requestedMediaTypes);
			View bestView = getBestView(candidateViews, requestedMediaTypes, attrs);
			if (bestView != null) {
				return bestView;
			}
		}

		String mediaTypeInfo = logger.isDebugEnabled() && requestedMediaTypes != null ?
				" given " + requestedMediaTypes.toString() : "";

		if (this.useNotAcceptableStatusCode) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using 406 NOT_ACCEPTABLE" + mediaTypeInfo);
			}
			return NOT_ACCEPTABLE_VIEW;
		}
		else {
			logger.debug("View remains unresolved" + mediaTypeInfo);
			return null;
		}
	}

	/**
	 * 确定给定 {@link HttpServletRequest} 的 {@link MediaType} 列表。
	 * @param request 当前的 Servlet 请求
	 * @return 请求的媒体类型列表（如果有）
	 */
	@Nullable
	protected List<MediaType> getMediaTypes(HttpServletRequest request) {
		Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
		try {
			ServletWebRequest webRequest = new ServletWebRequest(request);
			List<MediaType> acceptableMediaTypes = this.contentNegotiationManager.resolveMediaTypes(webRequest);
			List<MediaType> producibleMediaTypes = getProducibleMediaTypes(request);
			Set<MediaType> compatibleMediaTypes = new LinkedHashSet<>();
			for (MediaType acceptable : acceptableMediaTypes) {
				for (MediaType producible : producibleMediaTypes) {
					if (acceptable.isCompatibleWith(producible)) {
						compatibleMediaTypes.add(getMostSpecificMediaType(acceptable, producible));
					}
				}
			}
			List<MediaType> selectedMediaTypes = new ArrayList<>(compatibleMediaTypes);
			MediaType.sortBySpecificityAndQuality(selectedMediaTypes);
			return selectedMediaTypes;
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(ex.getMessage());
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getProducibleMediaTypes(HttpServletRequest request) {
		Set<MediaType> mediaTypes = (Set<MediaType>)
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(mediaTypes)) {
			return new ArrayList<>(mediaTypes);
		}
		else {
			return Collections.singletonList(MediaType.ALL);
		}
	}

	/**
	 * 返回可接受媒体类型与可生成媒体类型中更具体的那个，
	 * 并采用前者的 q 值。
	 */
	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		return (MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) < 0 ? acceptType : produceType);
	}

	private List<View> getCandidateViews(String viewName, Locale locale, List<MediaType> requestedMediaTypes)
			throws Exception {

		List<View> candidateViews = new ArrayList<>();
		if (this.viewResolvers != null) {
			Assert.state(this.contentNegotiationManager != null, "No ContentNegotiationManager set");
			for (ViewResolver viewResolver : this.viewResolvers) {
				View view = viewResolver.resolveViewName(viewName, locale);
				if (view != null) {
					candidateViews.add(view);
				}
				for (MediaType requestedMediaType : requestedMediaTypes) {
					List<String> extensions = this.contentNegotiationManager.resolveFileExtensions(requestedMediaType);
					for (String extension : extensions) {
						String viewNameWithExtension = viewName + '.' + extension;
						view = viewResolver.resolveViewName(viewNameWithExtension, locale);
						if (view != null) {
							candidateViews.add(view);
						}
					}
				}
			}
		}
		if (!CollectionUtils.isEmpty(this.defaultViews)) {
			candidateViews.addAll(this.defaultViews);
		}
		return candidateViews;
	}

	@Nullable
	private View getBestView(List<View> candidateViews, List<MediaType> requestedMediaTypes, RequestAttributes attrs) {
		for (View candidateView : candidateViews) {
			if (candidateView instanceof SmartView) {
				SmartView smartView = (SmartView) candidateView;
				if (smartView.isRedirectView()) {
					return candidateView;
				}
			}
		}
		for (MediaType mediaType : requestedMediaTypes) {
			for (View candidateView : candidateViews) {
				if (StringUtils.hasText(candidateView.getContentType())) {
					MediaType candidateContentType = MediaType.parseMediaType(candidateView.getContentType());
					if (mediaType.isCompatibleWith(candidateContentType)) {
						mediaType = mediaType.removeQualityValue();
						if (logger.isDebugEnabled()) {
							logger.debug("Selected '" + mediaType + "' given " + requestedMediaTypes);
						}
						attrs.setAttribute(View.SELECTED_CONTENT_TYPE, mediaType, RequestAttributes.SCOPE_REQUEST);
						return candidateView;
					}
				}
			}
		}
		return null;
	}


	private static final View NOT_ACCEPTABLE_VIEW = new View() {

		@Override
		@Nullable
		public String getContentType() {
			return null;
		}

		@Override
		public void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
			response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	};

}
