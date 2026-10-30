/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * 辅助类，用于从将为特定请求提供服务的 {@code HandlerMapping}
 * 获取信息。
 *
 * <p>提供以下方法：
 * <ul>
 * <li>{@link #getMatchableHandlerMapping} &mdash; 获取一个 {@code HandlerMapping}
 * 以对照检查请求匹配条件。
 * <li>{@link #getCorsConfiguration} &mdash; 获取该请求的
 * CORS 配置。
 * </ul>
 *
 * <p><strong>注意：</strong>这主要是一个 SPI，用于让 Spring Security
 * 将其模式匹配与 Spring MVC 对给定请求所用的模式匹配
 * 对齐，从而避免安全问题。
 * 应避免出于其他目的使用此内省器，因为它会带来
 * 为请求解析处理器的开销。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class HandlerMappingIntrospector
		implements CorsConfigurationSource, ApplicationContextAware, InitializingBean {

	@Nullable
	private ApplicationContext applicationContext;

	@Nullable
	private List<HandlerMapping> handlerMappings;

	private Map<HandlerMapping, PathPatternMatchableHandlerMapping> pathPatternMappings = Collections.emptyMap();


	/**
	 * 供 {@link ApplicationContextAware} 使用的构造函数。
	 */
	public HandlerMappingIntrospector() {
	}

	/**
	 * 构造函数，在给定 {@code ApplicationContext} 中检测所配置的
	 * {@code HandlerMapping}，或者像 {@code DispatcherServlet} 那样
	 * 回退到 "DispatcherServlet.properties"。
	 * @deprecated 自 4.3.12 起，建议使用 {@link #setApplicationContext}
	 */
	@Deprecated
	public HandlerMappingIntrospector(ApplicationContext context) {
		this.handlerMappings = initHandlerMappings(context);
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.handlerMappings == null) {
			Assert.notNull(this.applicationContext, "No ApplicationContext");
			this.handlerMappings = initHandlerMappings(this.applicationContext);

			this.pathPatternMappings = this.handlerMappings.stream()
					.filter(m -> m instanceof MatchableHandlerMapping && ((MatchableHandlerMapping) m).getPatternParser() != null)
					.map(mapping -> (MatchableHandlerMapping) mapping)
					.collect(Collectors.toMap(mapping -> mapping, PathPatternMatchableHandlerMapping::new));
		}
	}

	private static List<HandlerMapping> initHandlerMappings(ApplicationContext context) {

		Map<String, HandlerMapping> beans =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);

		if (!beans.isEmpty()) {
			List<HandlerMapping> mappings = new ArrayList<>(beans.values());
			AnnotationAwareOrderComparator.sort(mappings);
			return Collections.unmodifiableList(mappings);
		}

		return Collections.unmodifiableList(initFallback(context));
	}

	private static List<HandlerMapping> initFallback(ApplicationContext applicationContext) {
		Properties properties;
		try {
			Resource resource = new ClassPathResource("DispatcherServlet.properties", DispatcherServlet.class);
			properties = PropertiesLoaderUtils.loadProperties(resource);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not load DispatcherServlet.properties: " + ex.getMessage());
		}

		String value = properties.getProperty(HandlerMapping.class.getName());
		String[] names = StringUtils.commaDelimitedListToStringArray(value);
		List<HandlerMapping> result = new ArrayList<>(names.length);
		for (String name : names) {
			try {
				Class<?> clazz = ClassUtils.forName(name, DispatcherServlet.class.getClassLoader());
				Object mapping = applicationContext.getAutowireCapableBeanFactory().createBean(clazz);
				result.add((HandlerMapping) mapping);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Could not find default HandlerMapping [" + name + "]");
			}
		}
		return result;
	}


	/**
	 * 返回所配置或检测到的 {@code HandlerMapping}。
	 */
	public List<HandlerMapping> getHandlerMappings() {
		return (this.handlerMappings != null ? this.handlerMappings : Collections.emptyList());
	}


	/**
	 * 查找将处理给定请求的 {@link HandlerMapping}，
	 * 并返回一个用于路径匹配的 {@link MatchableHandlerMapping}。
	 * @param request 当前请求
	 * @return 解析得到的 {@code MatchableHandlerMapping}，或 {@code null}
	 * @throws IllegalStateException 如果匹配的 HandlerMapping 不是 {@link MatchableHandlerMapping} 的实例
	 * @throws Exception 如果任一 HandlerMapping 抛出异常
	 */
	@Nullable
	public MatchableHandlerMapping getMatchableHandlerMapping(HttpServletRequest request) throws Exception {
		HttpServletRequest wrappedRequest = new AttributesPreservingRequest(request);

		return doWithHandlerMapping(wrappedRequest, false, (mapping, executionChain) -> {
			if (mapping instanceof MatchableHandlerMapping) {
				PathPatternMatchableHandlerMapping pathPatternMapping = this.pathPatternMappings.get(mapping);
				if (pathPatternMapping != null) {
					RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(wrappedRequest);
					return new LookupPathMatchableHandlerMapping(pathPatternMapping, requestPath);
				}
				else {
					String lookupPath = (String) wrappedRequest.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
					return new LookupPathMatchableHandlerMapping((MatchableHandlerMapping) mapping, lookupPath);
				}
			}
			throw new IllegalStateException("HandlerMapping is not a MatchableHandlerMapping");
		});
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		AttributesPreservingRequest wrappedRequest = new AttributesPreservingRequest(request);
		return doWithHandlerMappingIgnoringException(wrappedRequest, (handlerMapping, executionChain) -> {
			for (HandlerInterceptor interceptor : executionChain.getInterceptorList()) {
				if (interceptor instanceof CorsConfigurationSource) {
					return ((CorsConfigurationSource) interceptor).getCorsConfiguration(wrappedRequest);
				}
			}
			if (executionChain.getHandler() instanceof CorsConfigurationSource) {
				return ((CorsConfigurationSource) executionChain.getHandler()).getCorsConfiguration(wrappedRequest);
			}
			return null;
		});
	}

	@Nullable
	private <T> T doWithHandlerMapping(
			HttpServletRequest request, boolean ignoreException,
			BiFunction<HandlerMapping, HandlerExecutionChain, T> extractor) throws Exception {

		Assert.state(this.handlerMappings != null, "HandlerMapping's not initialized");

		boolean parsePath = !this.pathPatternMappings.isEmpty();
		RequestPath previousPath = null;
		if (parsePath) {
			previousPath = (RequestPath) request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE);
			ServletRequestPathUtils.parseAndCache(request);
		}
		try {
			for (HandlerMapping handlerMapping : this.handlerMappings) {
				HandlerExecutionChain chain = null;
				try {
					chain = handlerMapping.getHandler(request);
				}
				catch (Exception ex) {
					if (!ignoreException) {
						throw ex;
					}
				}
				if (chain == null) {
					continue;
				}
				return extractor.apply(handlerMapping, chain);
			}
		}
		finally {
			if (parsePath) {
				ServletRequestPathUtils.setParsedRequestPath(previousPath, request);
			}
		}
		return null;
	}

	@Nullable
	private <T> T doWithHandlerMappingIgnoringException(
			HttpServletRequest request, BiFunction<HandlerMapping, HandlerExecutionChain, T> matchHandler) {

		try {
			return doWithHandlerMapping(request, true, matchHandler);
		}
		catch (Exception ex) {
			throw new IllegalStateException("HandlerMapping exception not suppressed", ex);
		}
	}


	/**
	 * 请求包装器，用于缓冲请求属性，以保护
	 * 底层请求免受属性更改的影响。
	 */
	private static class AttributesPreservingRequest extends HttpServletRequestWrapper {

		private final Map<String, Object> attributes;

		AttributesPreservingRequest(HttpServletRequest request) {
			super(request);
			this.attributes = initAttributes(request);
		}

		private Map<String, Object> initAttributes(HttpServletRequest request) {
			Map<String, Object> map = new HashMap<>();
			Enumeration<String> names = request.getAttributeNames();
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				map.put(name, request.getAttribute(name));
			}
			return map;
		}

		@Override
		public void setAttribute(String name, Object value) {
			this.attributes.put(name, value);
		}

		@Override
		public Object getAttribute(String name) {
			return this.attributes.get(name);
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			return Collections.enumeration(this.attributes.keySet());
		}

		@Override
		public void removeAttribute(String name) {
			this.attributes.remove(name);
		}
	}


	private static class LookupPathMatchableHandlerMapping implements MatchableHandlerMapping {

		private final MatchableHandlerMapping delegate;

		private final Object lookupPath;

		private final String pathAttributeName;

		LookupPathMatchableHandlerMapping(MatchableHandlerMapping delegate, Object lookupPath) {
			this.delegate = delegate;
			this.lookupPath = lookupPath;
			this.pathAttributeName = (lookupPath instanceof RequestPath ?
					ServletRequestPathUtils.PATH_ATTRIBUTE : UrlPathHelper.PATH_ATTRIBUTE);
		}

		@Nullable
		@Override
		public RequestMatchResult match(HttpServletRequest request, String pattern) {
			pattern = (StringUtils.hasLength(pattern) && !pattern.startsWith("/") ? "/" + pattern : pattern);
			Object previousPath = request.getAttribute(this.pathAttributeName);
			request.setAttribute(this.pathAttributeName, this.lookupPath);
			try {
				return this.delegate.match(request, pattern);
			}
			finally {
				request.setAttribute(this.pathAttributeName, previousPath);
			}
		}

		@Nullable
		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
			return this.delegate.getHandler(request);
		}
	}

}
