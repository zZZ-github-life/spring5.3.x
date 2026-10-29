/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
import org.springframework.web.servlet.mvc.condition.CompositeRequestCondition;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 从 {@link Controller @Controller} 类中的类型级别和方法级别
 * {@link RequestMapping @RequestMapping} 注解创建
 * {@link RequestMappingInfo} 实例。
 *
 * <p><strong>弃用说明：</strong></p>在 5.2.4 中，
 * {@link #setUseSuffixPatternMatch(boolean) useSuffixPatternMatch} 和
 * {@link #setUseRegisteredSuffixPatternMatch(boolean) useRegisteredSuffixPatternMatch}
 * 被弃用，以不鼓励在请求映射和内容协商中使用路径扩展名
 * （{@link org.springframework.web.accept.ContentNegotiationManagerFactoryBean
 * ContentNegotiationManagerFactoryBean} 中也有类似的弃用）。
 * 如需进一步了解背景，请阅读 issue
 * <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24179</a>。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.1
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements MatchableHandlerMapping, EmbeddedValueResolverAware {

	private boolean useSuffixPatternMatch = false;

	private boolean useRegisteredSuffixPatternMatch = false;

	private boolean useTrailingSlashMatch = true;

	private Map<String, Predicate<Class<?>>> pathPrefixes = Collections.emptyMap();

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	@Nullable
	private StringValueResolver embeddedValueResolver;

	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();


	/**
	 * 在将模式与请求匹配时是否使用后缀模式匹配（".*"）。
	 * 如果启用，映射到 "/users" 的方法也将匹配 "/users.*"。
	 * <p>默认情况下此值设置为 {@code false}。
	 * <p>另请参见 {@link #setUseRegisteredSuffixPatternMatch(boolean)}，
	 * 以更细粒度地控制允许的特定后缀。
	 * <p><strong>注意：</strong>当配置了
	 * {@link #setPatternParser(PathPatternParser)} 时，此属性会被忽略。
	 * @deprecated as of 5.2.4. See class level note on the deprecation of
	 * path extension config options. As there is no replacement for this method,
	 * in 5.2.x it is necessary to set it to {@code false}. In 5.3 the default
	 * changes to {@code false} and use of this property becomes unnecessary.
	 */
	@Deprecated
	public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
	}

	/**
	 * 后缀模式匹配是否只针对显式注册到
	 * {@link ContentNegotiationManager} 的路径扩展名生效。通常建议这样做，
	 * 以减少歧义并避免诸如路径中因其他原因出现 "." 时的问题。
	 * <p>默认情况下此值设置为 "false"。
	 * <p><strong>注意：</strong>当配置了
	 * {@link #setPatternParser(PathPatternParser)} 时，此属性会被忽略。
	 * @deprecated as of 5.2.4. See class level note on the deprecation of
	 * path extension config options.
	 */
	@Deprecated
	public void setUseRegisteredSuffixPatternMatch(boolean useRegisteredSuffixPatternMatch) {
		this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
		this.useSuffixPatternMatch = (useRegisteredSuffixPatternMatch || this.useSuffixPatternMatch);
	}

	/**
	 * 是否忽略尾部斜杠的存在而匹配 URL。
	 * 如果启用，映射到 "/users" 的方法也将匹配 "/users/"。
	 * <p>默认值为 {@code true}。
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		if (getPatternParser() != null) {
			getPatternParser().setMatchOptionalTrailingSeparator(useTrailingSlashMatch);
		}
	}

	/**
	 * 配置要应用到控制器方法的路径前缀。
	 * <p>前缀用于丰富每个 {@code @RequestMapping} 方法的映射，
	 * 这些方法的控制器类型由相应的
	 * {@code Predicate} 匹配。将使用第一个匹配的谓词对应的前缀。
	 * <p>可考虑使用 {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} 对控制器进行分组。
	 * @param prefixes a map with path prefixes as key
	 * @since 5.1
	 */
	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
		this.pathPrefixes = (!prefixes.isEmpty() ?
				Collections.unmodifiableMap(new LinkedHashMap<>(prefixes)) :
				Collections.emptyMap());
	}

	/**
	 * 已配置的路径前缀，为只读的、可能为空的 map。
	 * @since 5.1
	 */
	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}

	/**
	 * 设置用于确定所请求媒体类型的 {@link ContentNegotiationManager}。
	 * 如果未设置，则使用默认构造函数创建的实例。
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回已配置的 {@link ContentNegotiationManager}。
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void afterPropertiesSet() {
		this.config = new RequestMappingInfo.BuilderConfiguration();
		this.config.setTrailingSlashMatch(useTrailingSlashMatch());
		this.config.setContentNegotiationManager(getContentNegotiationManager());

		if (getPatternParser() != null) {
			this.config.setPatternParser(getPatternParser());
			Assert.isTrue(!this.useSuffixPatternMatch && !this.useRegisteredSuffixPatternMatch,
					"Suffix pattern matching not supported with PathPatternParser.");
		}
		else {
			this.config.setSuffixPatternMatch(useSuffixPatternMatch());
			this.config.setRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch());
			this.config.setPathMatcher(getPathMatcher());
		}

		super.afterPropertiesSet();
	}


	/**
	 * 是否使用已注册的后缀进行模式匹配。
	 * @deprecated as of 5.2.4. See deprecation notice on
	 * {@link #setUseSuffixPatternMatch(boolean)}.
	 */
	@Deprecated
	public boolean useSuffixPatternMatch() {
		return this.useSuffixPatternMatch;
	}

	/**
	 * 是否使用已注册的后缀进行模式匹配。
	 * @deprecated as of 5.2.4. See deprecation notice on
	 * {@link #setUseRegisteredSuffixPatternMatch(boolean)}.
	 */
	@Deprecated
	public boolean useRegisteredSuffixPatternMatch() {
		return this.useRegisteredSuffixPatternMatch;
	}

	/**
	 * 是否忽略尾部斜杠的存在而匹配 URL。
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * 返回用于后缀模式匹配的文件扩展名。
	 * @deprecated as of 5.2.4. See class-level note on the deprecation of path
	 * extension config options.
	 */
	@Nullable
	@Deprecated
	@SuppressWarnings("deprecation")
	public List<String> getFileExtensions() {
		return this.config.getFileExtensions();
	}

	/**
	 * 获取一个 {@link RequestMappingInfo.BuilderConfiguration}，它能够反映
	 * 此 {@code HandlerMapping} 的内部配置，并可用于
	 * 设置 {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}。
	 * <p>这对于通过
	 * {@link #registerHandlerMethod(Object, Method, RequestMappingInfo)} 以编程方式注册请求映射很有用。
	 * @return the builder configuration that reflects the internal state
	 * @since 5.3.14
	 */
	public RequestMappingInfo.BuilderConfiguration getBuilderConfiguration() {
		return this.config;
	}


	/**
	 * {@inheritDoc}
	 * <p>要求处理器具有类型级别的 @{@link Controller}
	 * 注解或类型级别的 @{@link RequestMapping} 注解。
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
				AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
	}

	/**
	 * 使用方法和类型级别的 @{@link RequestMapping} 注解创建
	 * RequestMappingInfo。
	 * @return the created RequestMappingInfo, or {@code null} if the method
	 * does not have a {@code @RequestMapping} annotation.
	 * @see #getCustomMethodCondition(Method)
	 * @see #getCustomTypeCondition(Class)
	 */
	@Override
	@Nullable
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMappingInfo info = createRequestMappingInfo(method);
		if (info != null) {
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
			if (typeInfo != null) {
				info = typeInfo.combine(info);
			}
			String prefix = getPathPrefix(handlerType);
			if (prefix != null) {
				info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
			}
		}
		return info;
	}

	@Nullable
	String getPathPrefix(Class<?> handlerType) {
		for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
			if (entry.getValue().test(handlerType)) {
				String prefix = entry.getKey();
				if (this.embeddedValueResolver != null) {
					prefix = this.embeddedValueResolver.resolveStringValue(prefix);
				}
				return prefix;
			}
		}
		return null;
	}

	/**
	 * 委托给 {@link #createRequestMappingInfo(RequestMapping, RequestCondition)}，
	 * 并根据所提供的 {@code annotatedElement} 是类还是方法，
	 * 提供相应的自定义 {@link RequestCondition}。
	 * @see #getCustomTypeCondition(Class)
	 * @see #getCustomMethodCondition(Method)
	 */
	@Nullable
	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
		RequestCondition<?> condition = (element instanceof Class ?
				getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
		return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
	}

	/**
	 * 提供自定义的类型级别请求条件。
	 * 自定义 {@link RequestCondition} 可以是任何类型，只要
	 * 对此方法的所有调用都返回相同的条件类型，
	 * 以确保自定义请求条件能够组合和比较。
	 * <p>可考虑为自定义条件类型扩展 {@link AbstractRequestCondition}，
	 * 并使用 {@link CompositeRequestCondition} 提供
	 * 多个自定义条件。
	 * @param handlerType the handler type for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@Nullable
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * 提供自定义的方法级别请求条件。
	 * 自定义 {@link RequestCondition} 可以是任何类型，只要
	 * 对此方法的所有调用都返回相同的条件类型，
	 * 以确保自定义请求条件能够组合和比较。
	 * <p>可考虑为自定义条件类型扩展 {@link AbstractRequestCondition}，
	 * 并使用 {@link CompositeRequestCondition} 提供
	 * 多个自定义条件。
	 * @param method the handler method for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@Nullable
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	/**
	 * 根据所提供的 {@link RequestMapping @RequestMapping} 注解创建
	 * {@link RequestMappingInfo}，该注解可以是直接声明的注解、
	 * 元注解，或者是注解层次结构中注解属性合并后的
	 * 合成结果。
	 */
	protected RequestMappingInfo createRequestMappingInfo(
			RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {

		RequestMappingInfo.Builder builder = RequestMappingInfo
				.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
				.methods(requestMapping.method())
				.params(requestMapping.params())
				.headers(requestMapping.headers())
				.consumes(requestMapping.consumes())
				.produces(requestMapping.produces())
				.mappingName(requestMapping.name());
		if (customCondition != null) {
			builder.customCondition(customCondition);
		}
		return builder.options(this.config).build();
	}

	/**
	 * 解析给定模式数组中的占位符值。
	 * @return a new array with updated patterns
	 */
	protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
		if (this.embeddedValueResolver == null) {
			return patterns;
		}
		else {
			String[] resolvedPatterns = new String[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
			}
			return resolvedPatterns;
		}
	}

	@Override
	public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
		super.registerMapping(mapping, handler, method);
		updateConsumesCondition(mapping, method);
	}

	/**
	 * {@inheritDoc}
	 * <p><strong>注意：</strong>要创建 {@link RequestMappingInfo}，
	 * 请使用 {@link #getBuilderConfiguration()} 并在
	 * {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}
	 * 上设置选项，使其与此 {@code HandlerMapping} 的配置方式一致。这
	 * 很重要，例如可以确保使用
	 * {@link org.springframework.web.util.pattern.PathPattern} 还是
	 * {@link org.springframework.util.PathMatcher} 进行匹配。
	 * @param handler the bean name of the handler or the handler instance
	 * @param method the method to register
	 * @param mapping the mapping conditions associated with the handler method
	 */
	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		super.registerHandlerMethod(handler, method, mapping);
		updateConsumesCondition(mapping, method);
	}

	private void updateConsumesCondition(RequestMappingInfo info, Method method) {
		ConsumesRequestCondition condition = info.getConsumesCondition();
		if (!condition.isEmpty()) {
			for (Parameter parameter : method.getParameters()) {
				MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);
				if (annot.isPresent()) {
					condition.setBodyRequired(annot.getBoolean("required"));
					break;
				}
			}
		}
	}

	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		Assert.isNull(getPatternParser(), "This HandlerMapping requires a PathPattern");
		RequestMappingInfo info = RequestMappingInfo.paths(pattern).options(this.config).build();
		RequestMappingInfo match = info.getMatchingCondition(request);
		return (match != null && match.getPatternsCondition() != null ?
				new RequestMatchResult(
						match.getPatternsCondition().getPatterns().iterator().next(),
						UrlPathHelper.getResolvedLookupPath(request),
						getPathMatcher()) : null);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		HandlerMethod handlerMethod = createHandlerMethod(handler, method);
		Class<?> beanType = handlerMethod.getBeanType();
		CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
		CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);

		if (typeAnnotation == null && methodAnnotation == null) {
			return null;
		}

		CorsConfiguration config = new CorsConfiguration();
		updateCorsConfig(config, typeAnnotation);
		updateCorsConfig(config, methodAnnotation);

		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}
		return config.applyPermitDefaultValues();
	}

	private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
		if (annotation == null) {
			return;
		}
		for (String origin : annotation.origins()) {
			config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
		}
		for (String patterns : annotation.originPatterns()) {
			config.addAllowedOriginPattern(resolveCorsAnnotationValue(patterns));
		}
		for (RequestMethod method : annotation.methods()) {
			config.addAllowedMethod(method.name());
		}
		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(resolveCorsAnnotationValue(header));
		}
		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(resolveCorsAnnotationValue(header));
		}

		String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
		if ("true".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(true);
		}
		else if ("false".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(false);
		}
		else if (!allowCredentials.isEmpty()) {
			throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
					"or an empty string (\"\"): current value is [" + allowCredentials + "]");
		}

		if (annotation.maxAge() >= 0 ) {
			config.setMaxAge(annotation.maxAge());
		}
	}

	private String resolveCorsAnnotationValue(String value) {
		if (this.embeddedValueResolver != null) {
			String resolved = this.embeddedValueResolver.resolveStringValue(value);
			return (resolved != null ? resolved : "");
		}
		else {
			return value;
		}
	}

}
