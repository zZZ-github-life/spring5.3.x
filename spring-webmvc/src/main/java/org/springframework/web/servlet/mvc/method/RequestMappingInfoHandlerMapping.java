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

package org.springframework.web.servlet.mvc.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.condition.NameValueExpression;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.WebUtils;
import org.springframework.web.util.pattern.PathPattern;

/**
 * 抽象基类，适用于由 {@link RequestMappingInfo} 定义
 * 请求与处理器方法之间映射的类。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	private static final Method HTTP_OPTIONS_HANDLE_METHOD;

	static {
		try {
			HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
		}
		catch (NoSuchMethodException ex) {
			// Should never happen
			throw new IllegalStateException("Failed to retrieve internal handler method for HTTP OPTIONS", ex);
		}
	}


	protected RequestMappingInfoHandlerMapping() {
		setHandlerMethodMappingNamingStrategy(new RequestMappingInfoHandlerMethodMappingNamingStrategy());
	}


	/**
	 * 获取与所提供 {@link RequestMappingInfo} 关联的 URL 路径模式。
	 */
	@Override
	@SuppressWarnings("deprecation")
	protected Set<String> getMappingPathPatterns(RequestMappingInfo info) {
		return info.getPatternValues();
	}

	@Override
	protected Set<String> getDirectPaths(RequestMappingInfo info) {
		return info.getDirectPaths();
	}

	/**
	 * 检查给定的 RequestMappingInfo 是否匹配当前请求，并
	 * 返回一个（可能是新的）实例，其条件与当前请求匹配
	 * ——例如包含 URL 模式的子集。
	 * @return 匹配时的 info；否则为 {@code null}。
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, HttpServletRequest request) {
		return info.getMatchingCondition(request);
	}

	/**
	 * 提供一个 Comparator，用于对匹配请求的 RequestMappingInfo 进行排序。
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final HttpServletRequest request) {
		return (info1, info2) -> info1.compareTo(info2, request);
	}

	@Override
	@Nullable
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		try {
			return super.getHandlerInternal(request);
		}
		finally {
			ProducesRequestCondition.clearMediaTypesAttribute(request);
		}
	}

	/**
	 * 在请求中暴露 URI 模板变量、矩阵变量和可生成媒体类型。
	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#MATRIX_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, String lookupPath, HttpServletRequest request) {
		super.handleMatch(info, lookupPath, request);

		RequestCondition<?> condition = info.getActivePatternsCondition();
		if (condition instanceof PathPatternsRequestCondition) {
			extractMatchDetails((PathPatternsRequestCondition) condition, lookupPath, request);
		}
		else {
			extractMatchDetails((PatternsRequestCondition) condition, lookupPath, request);
		}

		ProducesRequestCondition producesCondition = info.getProducesCondition();
		if (!producesCondition.isEmpty()) {
			Set<MediaType> mediaTypes = producesCondition.getProducibleMediaTypes();
			if (!mediaTypes.isEmpty()) {
				request.setAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
			}
		}
	}

	private void extractMatchDetails(
			PathPatternsRequestCondition condition, String lookupPath, HttpServletRequest request) {

		PathPattern bestPattern;
		Map<String, String> uriVariables;
		if (condition.isEmptyPathMapping()) {
			bestPattern = condition.getFirstPattern();
			uriVariables = Collections.emptyMap();
		}
		else {
			PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
			bestPattern = condition.getFirstPattern();
			PathPattern.PathMatchInfo result = bestPattern.matchAndExtract(path);
			Assert.notNull(result, () ->
					"Expected bestPattern: " + bestPattern + " to match lookupPath " + path);
			uriVariables = result.getUriVariables();
			request.setAttribute(MATRIX_VARIABLES_ATTRIBUTE, result.getMatrixVariables());
		}
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern.getPatternString());
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
	}

	private void extractMatchDetails(
			PatternsRequestCondition condition, String lookupPath, HttpServletRequest request) {

		String bestPattern;
		Map<String, String> uriVariables;
		if (condition.isEmptyPathMapping()) {
			bestPattern = lookupPath;
			uriVariables = Collections.emptyMap();
		}
		else {
			bestPattern = condition.getPatterns().iterator().next();
			uriVariables = getPathMatcher().extractUriTemplateVariables(bestPattern, lookupPath);
			if (!getUrlPathHelper().shouldRemoveSemicolonContent()) {
				request.setAttribute(MATRIX_VARIABLES_ATTRIBUTE, extractMatrixVariables(request, uriVariables));
			}
			uriVariables = getUrlPathHelper().decodePathVariables(request, uriVariables);
		}
		request.setAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
	}

	private Map<String, MultiValueMap<String, String>> extractMatrixVariables(
			HttpServletRequest request, Map<String, String> uriVariables) {

		Map<String, MultiValueMap<String, String>> result = new LinkedHashMap<>();
		uriVariables.forEach((uriVarKey, uriVarValue) -> {

			int equalsIndex = uriVarValue.indexOf('=');
			if (equalsIndex == -1) {
				return;
			}

			int semicolonIndex = uriVarValue.indexOf(';');
			if (semicolonIndex != -1 && semicolonIndex != 0) {
				uriVariables.put(uriVarKey, uriVarValue.substring(0, semicolonIndex));
			}

			String matrixVariables;
			if (semicolonIndex == -1 || semicolonIndex == 0 || equalsIndex < semicolonIndex) {
				matrixVariables = uriVarValue;
			}
			else {
				matrixVariables = uriVarValue.substring(semicolonIndex + 1);
			}

			MultiValueMap<String, String> vars = WebUtils.parseMatrixVariables(matrixVariables);
			result.put(uriVarKey, getUrlPathHelper().decodeMatrixVariables(request, vars));
		});
		return result;
	}

	/**
	 * 再次遍历所有 RequestMappingInfo，检查是否至少有按 URL 匹配的项，
	 * 并根据不匹配的内容抛出异常。
	 * @throws HttpRequestMethodNotSupportedException 如果存在按 URL 匹配但未按 HTTP 方法匹配的情况
	 * @throws HttpMediaTypeNotAcceptableException 如果存在按 URL 匹配但未按可消费/可生成的媒体类型匹配的情况
	 */
	@Override
	protected HandlerMethod handleNoMatch(
			Set<RequestMappingInfo> infos, String lookupPath, HttpServletRequest request) throws ServletException {

		if (CollectionUtils.isEmpty(infos)) {
			return null;
		}

		PartialMatchHelper helper = new PartialMatchHelper(infos, request);
		if (helper.isEmpty()) {
			return null;
		}

		if (helper.hasMethodsMismatch()) {
			Set<String> methods = helper.getAllowedMethods();
			if (HttpMethod.OPTIONS.matches(request.getMethod())) {
				Set<MediaType> mediaTypes = helper.getConsumablePatchMediaTypes();
				HttpOptionsHandler handler = new HttpOptionsHandler(methods, mediaTypes);
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}
			throw new HttpRequestMethodNotSupportedException(request.getMethod(), methods);
		}

		if (helper.hasConsumesMismatch()) {
			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
			MediaType contentType = null;
			if (StringUtils.hasLength(request.getContentType())) {
				try {
					contentType = MediaType.parseMediaType(request.getContentType());
				}
				catch (InvalidMediaTypeException ex) {
					throw new HttpMediaTypeNotSupportedException(ex.getMessage());
				}
			}
			throw new HttpMediaTypeNotSupportedException(contentType, new ArrayList<>(mediaTypes));
		}

		if (helper.hasProducesMismatch()) {
			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
			throw new HttpMediaTypeNotAcceptableException(new ArrayList<>(mediaTypes));
		}

		if (helper.hasParamsMismatch()) {
			List<String[]> conditions = helper.getParamConditions();
			throw new UnsatisfiedServletRequestParameterException(conditions, request.getParameterMap());
		}

		return null;
	}


	/**
	 * 聚合所有部分匹配，并暴露用于检查这些匹配的方法。
	 */
	private static class PartialMatchHelper {

		private final List<PartialMatch> partialMatches = new ArrayList<>();

		PartialMatchHelper(Set<RequestMappingInfo> infos, HttpServletRequest request) {
			for (RequestMappingInfo info : infos) {
				if (info.getActivePatternsCondition().getMatchingCondition(request) != null) {
					this.partialMatches.add(new PartialMatch(info, request));
				}
			}
		}

		/**
		 * 是否存在任何部分匹配。
		 */
		public boolean isEmpty() {
			return this.partialMatches.isEmpty();
		}

		/**
		 * "methods" 是否有任何部分匹配？
		 */
		public boolean hasMethodsMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasMethodsMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * "methods" 和 "consumes" 是否有任何部分匹配？
		 */
		public boolean hasConsumesMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasConsumesMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * "methods"、"consumes" 和 "produces" 是否有任何部分匹配？
		 */
		public boolean hasProducesMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasProducesMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * "methods"、"consumes"、"produces" 和 "params" 是否有任何部分匹配？
		 */
		public boolean hasParamsMismatch() {
			for (PartialMatch match : this.partialMatches) {
				if (match.hasParamsMatch()) {
					return false;
				}
			}
			return true;
		}

		/**
		 * 返回声明的 HTTP 方法。
		 */
		public Set<String> getAllowedMethods() {
			Set<String> result = new LinkedHashSet<>();
			for (PartialMatch match : this.partialMatches) {
				for (RequestMethod method : match.getInfo().getMethodsCondition().getMethods()) {
					result.add(method.name());
				}
			}
			return result;
		}

		/**
		 * 返回声明的 "consumable" 类型，但仅限那些同时
		 * 匹配 "methods" 条件的类型。
		 */
		public Set<MediaType> getConsumableMediaTypes() {
			Set<MediaType> result = new LinkedHashSet<>();
			for (PartialMatch match : this.partialMatches) {
				if (match.hasMethodsMatch()) {
					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
				}
			}
			return result;
		}

		/**
		 * 返回声明的 "producible" 类型，但仅限那些同时
		 * 匹配 "methods" 和 "consumes" 条件的类型。
		 */
		public Set<MediaType> getProducibleMediaTypes() {
			Set<MediaType> result = new LinkedHashSet<>();
			for (PartialMatch match : this.partialMatches) {
				if (match.hasConsumesMatch()) {
					result.addAll(match.getInfo().getProducesCondition().getProducibleMediaTypes());
				}
			}
			return result;
		}

		/**
		 * 返回声明的 "params" 条件，但仅限那些同时
		 * 匹配 "methods"、"consumes" 和 "params" 条件的条件。
		 */
		public List<String[]> getParamConditions() {
			List<String[]> result = new ArrayList<>();
			for (PartialMatch match : this.partialMatches) {
				if (match.hasProducesMatch()) {
					Set<NameValueExpression<String>> set = match.getInfo().getParamsCondition().getExpressions();
					if (!CollectionUtils.isEmpty(set)) {
						int i = 0;
						String[] array = new String[set.size()];
						for (NameValueExpression<String> expression : set) {
							array[i++] = expression.toString();
						}
						result.add(array);
					}
				}
			}
			return result;
		}

		/**
		 * 返回声明的 "consumable" 类型，但仅限那些指定了
		 * PATCH 或完全未指定方法的类型。
		 */
		public Set<MediaType> getConsumablePatchMediaTypes() {
			Set<MediaType> result = new LinkedHashSet<>();
			for (PartialMatch match : this.partialMatches) {
				Set<RequestMethod> methods = match.getInfo().getMethodsCondition().getMethods();
				if (methods.isEmpty() || methods.contains(RequestMethod.PATCH)) {
					result.addAll(match.getInfo().getConsumesCondition().getConsumableMediaTypes());
				}
			}
			return result;
		}


		/**
		 * 至少匹配 URL 路径的 RequestMappingInfo 的容器。
		 */
		private static class PartialMatch {

			private final RequestMappingInfo info;

			private final boolean methodsMatch;

			private final boolean consumesMatch;

			private final boolean producesMatch;

			private final boolean paramsMatch;

			/**
			 * 创建一个新的 {@link PartialMatch} 实例。
			 * @param info 与 URL 路径匹配的 RequestMappingInfo。
			 * @param request 当前请求
			 */
			public PartialMatch(RequestMappingInfo info, HttpServletRequest request) {
				this.info = info;
				this.methodsMatch = (info.getMethodsCondition().getMatchingCondition(request) != null);
				this.consumesMatch = (info.getConsumesCondition().getMatchingCondition(request) != null);
				this.producesMatch = (info.getProducesCondition().getMatchingCondition(request) != null);
				this.paramsMatch = (info.getParamsCondition().getMatchingCondition(request) != null);
			}

			public RequestMappingInfo getInfo() {
				return this.info;
			}

			public boolean hasMethodsMatch() {
				return this.methodsMatch;
			}

			public boolean hasConsumesMatch() {
				return (hasMethodsMatch() && this.consumesMatch);
			}

			public boolean hasProducesMatch() {
				return (hasConsumesMatch() && this.producesMatch);
			}

			public boolean hasParamsMatch() {
				return (hasProducesMatch() && this.paramsMatch);
			}

			@Override
			public String toString() {
				return this.info.toString();
			}
		}
	}


	/**
	 * HTTP OPTIONS 的默认处理器。
	 */
	private static class HttpOptionsHandler {

		private final HttpHeaders headers = new HttpHeaders();

		public HttpOptionsHandler(Set<String> declaredMethods, Set<MediaType> acceptPatch) {
			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
			this.headers.setAcceptPatch(new ArrayList<>(acceptPatch));
		}

		private static Set<HttpMethod> initAllowedHttpMethods(Set<String> declaredMethods) {
			Set<HttpMethod> result = new LinkedHashSet<>(declaredMethods.size());
			if (declaredMethods.isEmpty()) {
				for (HttpMethod method : HttpMethod.values()) {
					if (method != HttpMethod.TRACE) {
						result.add(method);
					}
				}
			}
			else {
				for (String method : declaredMethods) {
					HttpMethod httpMethod = HttpMethod.valueOf(method);
					result.add(httpMethod);
					if (httpMethod == HttpMethod.GET) {
						result.add(HttpMethod.HEAD);
					}
				}
				result.add(HttpMethod.OPTIONS);
			}
			return result;
		}

		@SuppressWarnings("unused")
		public HttpHeaders handle() {
			return this.headers;
		}
	}

}
