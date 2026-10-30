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

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.util.UrlPathHelper;

/**
 * 用于获取客户端访问静态资源时应使用的公共 URL 路径的核心组件。
 *
 * <p>此类了解用于提供静态资源的 Spring MVC 处理器映射，并使用所配置的 {@code ResourceHttpRequestHandler} 的 {@code ResourceResolver} 链来做出决策。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 */
public class ResourceUrlProvider implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ApplicationContext applicationContext;

	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;

	private PathMatcher pathMatcher = new AntPathMatcher();

	private final Map<String, ResourceHttpRequestHandler> handlerMap = new LinkedHashMap<>();

	private boolean autodetect = true;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * 配置要在 {@link #getForRequestUrl(javax.servlet.http.HttpServletRequest, String)} 中使用的 {@code UrlPathHelper}，以便为目标请求 URL 路径推导出查找路径。
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 返回已配置的 {@code UrlPathHelper}。
	 * @since 4.2.8
	 */
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	/**
	 * 配置在将目标查找路径与资源映射进行比较时要使用的 {@code PathMatcher}。
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

	/**
	 * 返回已配置的 {@code PathMatcher}。
	 */
	public PathMatcher getPathMatcher() {
		return this.pathMatcher;
	}

	/**
	 * 手动配置资源映射。
	 * <p><strong>注意：</strong>默认情况下，资源映射会从 Spring {@code ApplicationContext} 中自动检测。但若使用了此属性，则自动检测会被关闭。
	 */
	public void setHandlerMap(@Nullable Map<String, ResourceHttpRequestHandler> handlerMap) {
		if (handlerMap != null) {
			this.handlerMap.clear();
			this.handlerMap.putAll(handlerMap);
			this.autodetect = false;
		}
	}

	/**
	 * 返回资源映射，可以是手动配置的，也可以是在 Spring {@code ApplicationContext} 刷新时自动检测到的。
	 */
	public Map<String, ResourceHttpRequestHandler> getHandlerMap() {
		return this.handlerMap;
	}

	/**
	 * 如果资源映射是手动配置的则返回 {@code false}，否则返回 {@code true}。
	 */
	public boolean isAutodetect() {
		return this.autodetect;
	}


	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext() == this.applicationContext && isAutodetect()) {
			this.handlerMap.clear();
			detectResourceHandlers(this.applicationContext);
			if (!this.handlerMap.isEmpty()) {
				this.autodetect = false;
			}
		}
	}

	protected void detectResourceHandlers(ApplicationContext appContext) {
		Map<String, SimpleUrlHandlerMapping> beans = appContext.getBeansOfType(SimpleUrlHandlerMapping.class);
		List<SimpleUrlHandlerMapping> mappings = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(mappings);

		for (SimpleUrlHandlerMapping mapping : mappings) {
			for (String pattern : mapping.getHandlerMap().keySet()) {
				Object handler = mapping.getHandlerMap().get(pattern);
				if (handler instanceof ResourceHttpRequestHandler) {
					ResourceHttpRequestHandler resourceHandler = (ResourceHttpRequestHandler) handler;
					this.handlerMap.put(pattern, resourceHandler);
				}
			}
		}

		if (this.handlerMap.isEmpty()) {
			logger.trace("No resource handling mappings found");
		}
	}

	/**
	 * {@link #getForLookupPath(String)} 的变体，接受完整的请求 URL 路径（即包含上下文路径和 servlet 路径），并返回要公开暴露以供使用的完整请求 URL 路径。
	 * @param request 当前请求
	 * @param requestUrl 要解析的请求 URL 路径
	 * @return 解析出的公共 URL 路径；如果无法解析，则为 {@code null}
	 */
	@Nullable
	public final String getForRequestUrl(HttpServletRequest request, String requestUrl) {
		int prefixIndex = getLookupPathIndex(request);
		int suffixIndex = getEndPathIndex(requestUrl);
		if (prefixIndex >= suffixIndex) {
			return null;
		}
		String prefix = requestUrl.substring(0, prefixIndex);
		String suffix = requestUrl.substring(suffixIndex);
		String lookupPath = requestUrl.substring(prefixIndex, suffixIndex);
		String resolvedLookupPath = getForLookupPath(lookupPath);
		return (resolvedLookupPath != null ? prefix + resolvedLookupPath + suffix : null);
	}

	private int getLookupPathIndex(HttpServletRequest request) {
		UrlPathHelper pathHelper = getUrlPathHelper();
		if (request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) == null) {
			pathHelper.resolveAndCacheLookupPath(request);
		}
		String requestUri = pathHelper.getRequestUri(request);
		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
		return requestUri.indexOf(lookupPath);
	}

	private int getEndPathIndex(String lookupPath) {
		int suffixIndex = lookupPath.length();
		int queryIndex = lookupPath.indexOf('?');
		if (queryIndex > 0) {
			suffixIndex = queryIndex;
		}
		int hashIndex = lookupPath.indexOf('#');
		if (hashIndex > 0) {
			suffixIndex = Math.min(suffixIndex, hashIndex);
		}
		return suffixIndex;
	}

	/**
	 * 将给定路径与已配置的资源处理器映射进行比较，如果找到匹配项，则使用匹配的 {@code ResourceHttpRequestHandler} 的 {@code ResourceResolver} 链来解析要公开暴露以供使用的 URL 路径。
	 * <p>预期给定路径是 Spring MVC 用于请求映射的路径，即不包含上下文路径和 servlet 路径部分。
	 * <p>如果多个处理器映射匹配，则将使用配置了最具体模式的处理器。
	 * @param lookupPath 要检查的查找路径
	 * @return 解析出的公共 URL 路径；如果无法解析，则为 {@code null}
	 */
	@Nullable
	public final String getForLookupPath(String lookupPath) {
		// Clean duplicate slashes or pathWithinPattern won't match lookupPath
		String previous;
		do {
			previous = lookupPath;
			lookupPath = StringUtils.replace(lookupPath, "//", "/");
		} while (!lookupPath.equals(previous));

		List<String> matchingPatterns = new ArrayList<>();
		for (String pattern : this.handlerMap.keySet()) {
			if (getPathMatcher().match(pattern, lookupPath)) {
				matchingPatterns.add(pattern);
			}
		}

		if (!matchingPatterns.isEmpty()) {
			Comparator<String> patternComparator = getPathMatcher().getPatternComparator(lookupPath);
			matchingPatterns.sort(patternComparator);
			for (String pattern : matchingPatterns) {
				String pathWithinMapping = getPathMatcher().extractPathWithinPattern(pattern, lookupPath);
				String pathMapping = lookupPath.substring(0, lookupPath.indexOf(pathWithinMapping));
				ResourceHttpRequestHandler handler = this.handlerMap.get(pattern);
				ResourceResolverChain chain = new DefaultResourceResolverChain(handler.getResourceResolvers());
				String resolved = chain.resolveUrlPath(pathWithinMapping, handler.getLocations());
				if (resolved == null) {
					continue;
				}
				return pathMapping + resolved;
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("No match for \"" + lookupPath + "\"");
		}

		return null;
	}

}
