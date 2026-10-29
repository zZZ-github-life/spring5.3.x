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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;

/**
 * {@link RequestMappingInfoHandlerMapping} 的扩展，根据类级和方法级 {@link RequestMapping @RequestMapping} 注解创建 {@link RequestMappingInfo} 实例。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements EmbeddedValueResolverAware {

	private final Map<String, Predicate<Class<?>>> pathPrefixes = new LinkedHashMap<>();

	private RequestedContentTypeResolver contentTypeResolver = new RequestedContentTypeResolverBuilder().build();

	@Nullable
	private StringValueResolver embeddedValueResolver;

	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();


	/**
	 * 配置要应用于控制器方法的路径前缀。
	 * <p>前缀用于丰富每个 {@code @RequestMapping} 方法的映射，这些方法的控制器类型与映射中对应的 {@code Predicate} 匹配。假定输入映射具有可预测的顺序，将使用第一个匹配谓词的前缀。
	 * <p>考虑使用 {@link org.springframework.web.method.HandlerTypePredicate HandlerTypePredicate} 对控制器进行分组。
	 * @param prefixes a map with path prefixes as key
	 * @since 5.1
	 * @see org.springframework.web.method.HandlerTypePredicate
	 */
	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
		this.pathPrefixes.clear();
		prefixes.entrySet().stream()
				.filter(entry -> StringUtils.hasText(entry.getKey()))
				.forEach(entry -> this.pathPrefixes.put(entry.getKey(), entry.getValue()));
	}

	/**
	 * 配置的路径前缀，为只读映射，可能为空。
	 * @since 5.1
	 */
	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return Collections.unmodifiableMap(this.pathPrefixes);
	}

	/**
	 * 设置用于确定请求媒体类型的 {@link RequestedContentTypeResolver}。如果未设置，则使用默认构造函数。
	 */
	public void setContentTypeResolver(RequestedContentTypeResolver contentTypeResolver) {
		Assert.notNull(contentTypeResolver, "'contentTypeResolver' must not be null");
		this.contentTypeResolver = contentTypeResolver;
	}

	/**
	 * 返回配置的 {@link RequestedContentTypeResolver}。
	 */
	public RequestedContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	public void afterPropertiesSet() {
		this.config = new RequestMappingInfo.BuilderConfiguration();
		this.config.setPatternParser(getPathPatternParser());
		this.config.setContentTypeResolver(getContentTypeResolver());

		super.afterPropertiesSet();
	}


	/**
	 * {@inheritDoc}
	 * 期望处理器具有类型级 @{@link Controller} 注解。
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
				AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
	}

	/**
	 * 使用方法级和类型级 @{@link RequestMapping} 注解创建 RequestMappingInfo。
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
			for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
				if (entry.getValue().test(handlerType)) {
					String prefix = entry.getKey();
					if (this.embeddedValueResolver != null) {
						prefix = this.embeddedValueResolver.resolveStringValue(prefix);
					}
					info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
					break;
				}
			}
		}
		return info;
	}

	/**
	 * 委托给 {@link #createRequestMappingInfo(RequestMapping, RequestCondition)}，并根据提供的 {@code annotatedElement} 是类还是方法提供相应的自定义 {@link RequestCondition}。
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
	 * 提供自定义类型级请求条件。自定义 {@link RequestCondition} 可以是任何类型，只要对此方法的所有调用返回相同的条件类型，以确保自定义请求条件可以组合和比较。
	 * <p>考虑为自定义条件类型扩展 {@link org.springframework.web.reactive.result.condition.AbstractRequestCondition AbstractRequestCondition}，并使用 {@link org.springframework.web.reactive.result.condition.CompositeRequestCondition CompositeRequestCondition} 提供多个自定义条件。
	 * @param handlerType the handler type for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@SuppressWarnings("UnusedParameters")
	@Nullable
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * 提供自定义方法级请求条件。自定义 {@link RequestCondition} 可以是任何类型，只要对此方法的所有调用返回相同的条件类型，以确保自定义请求条件可以组合和比较。
	 * <p>考虑为自定义条件类型扩展 {@link org.springframework.web.reactive.result.condition.AbstractRequestCondition AbstractRequestCondition}，并使用 {@link org.springframework.web.reactive.result.condition.CompositeRequestCondition CompositeRequestCondition} 提供多个自定义条件。
	 * @param method the handler method for which to create the condition
	 * @return the condition, or {@code null}
	 */
	@SuppressWarnings("UnusedParameters")
	@Nullable
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	/**
	 * 根据提供的 {@link RequestMapping @RequestMapping} 注解创建 {@link RequestMappingInfo}，该注解可以是直接声明的注解、元注解，或注解层次结构中合并注解属性后的合成结果。
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

		if (annotation.maxAge() >= 0) {
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
