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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.NameValueExpression;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@link RequestMappingInfo} 定义请求与处理器方法之间映射的类的抽象基类。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class RequestMappingInfoHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {

	private static final Method HTTP_OPTIONS_HANDLE_METHOD;

	static {
		try {
			HTTP_OPTIONS_HANDLE_METHOD = HttpOptionsHandler.class.getMethod("handle");
		}
		catch (NoSuchMethodException ex) {
			// Should never happen
			throw new IllegalStateException("No handler for HTTP OPTIONS", ex);
		}
	}


	@Override
	protected Set<String> getDirectPaths(RequestMappingInfo info) {
		return info.getDirectPaths();
	}

	/**
	 * 检查给定 RequestMappingInfo 是否匹配当前请求，并返回一个（可能是新的）实例，其条件与当前请求匹配——例如包含 URL 模式的子集。
	 * @return an info in case of a match; or {@code null} otherwise.
	 */
	@Override
	protected RequestMappingInfo getMatchingMapping(RequestMappingInfo info, ServerWebExchange exchange) {
		return info.getMatchingCondition(exchange);
	}

	/**
	 * 提供用于对与请求匹配的 RequestMappingInfo 排序的 Comparator。
	 */
	@Override
	protected Comparator<RequestMappingInfo> getMappingComparator(final ServerWebExchange exchange) {
		return (info1, info2) -> info1.compareTo(info2, exchange);
	}

	@Override
	public Mono<HandlerMethod> getHandlerInternal(ServerWebExchange exchange) {
		exchange.getAttributes().remove(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		return super.getHandlerInternal(exchange)
				.doOnTerminate(() -> ProducesRequestCondition.clearMediaTypesAttribute(exchange));
	}

	/**
	 * 在请求中公开 URI 模板变量、矩阵变量和可生成媒体类型。
	 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#MATRIX_VARIABLES_ATTRIBUTE
	 * @see HandlerMapping#PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE
	 */
	@Override
	protected void handleMatch(RequestMappingInfo info, HandlerMethod handlerMethod,
			ServerWebExchange exchange) {

		super.handleMatch(info, handlerMethod, exchange);

		PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();

		PathPattern bestPattern;
		Map<String, String> uriVariables;
		Map<String, MultiValueMap<String, String>> matrixVariables;

		Set<PathPattern> patterns = info.getPatternsCondition().getPatterns();
		if (patterns.isEmpty()) {
			bestPattern = getPathPatternParser().parse(lookupPath.value());
			uriVariables = Collections.emptyMap();
			matrixVariables = Collections.emptyMap();
		}
		else {
			bestPattern = patterns.iterator().next();
			PathPattern.PathMatchInfo result = bestPattern.matchAndExtract(lookupPath);
			Assert.notNull(result, () ->
					"Expected bestPattern: " + bestPattern + " to match lookupPath " + lookupPath);
			uriVariables = result.getUriVariables();
			matrixVariables = result.getMatrixVariables();
		}

		exchange.getAttributes().put(BEST_MATCHING_HANDLER_ATTRIBUTE, handlerMethod);
		exchange.getAttributes().put(BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern);
		exchange.getAttributes().put(URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriVariables);
		exchange.getAttributes().put(MATRIX_VARIABLES_ATTRIBUTE, matrixVariables);

		ProducesRequestCondition producesCondition = info.getProducesCondition();
		if (!producesCondition.isEmpty()) {
			Set<MediaType> mediaTypes = producesCondition.getProducibleMediaTypes();
			if (!mediaTypes.isEmpty()) {
				exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);
			}
		}
	}

	/**
	 * 再次遍历所有 RequestMappingInfo，至少检查是否有任何 URL 匹配，并相应抛出异常。
	 * @throws MethodNotAllowedException for matches by URL but not by HTTP method
	 * @throws UnsupportedMediaTypeStatusException if there are matches by URL
	 * and HTTP method but not by consumable media types
	 * @throws NotAcceptableStatusException if there are matches by URL and HTTP
	 * method but not by producible media types
	 * @throws ServerWebInputException if there are matches by URL and HTTP
	 * method but not by query parameter conditions
	 */
	@Override
	protected HandlerMethod handleNoMatch(Set<RequestMappingInfo> infos,
			ServerWebExchange exchange) throws Exception {

		if (CollectionUtils.isEmpty(infos)) {
			return null;
		}

		PartialMatchHelper helper = new PartialMatchHelper(infos, exchange);
		if (helper.isEmpty()) {
			return null;
		}

		ServerHttpRequest request = exchange.getRequest();

		if (helper.hasMethodsMismatch()) {
			String httpMethod = request.getMethodValue();
			Set<HttpMethod> methods = helper.getAllowedMethods();
			if (HttpMethod.OPTIONS.matches(httpMethod)) {
				Set<MediaType> mediaTypes = helper.getConsumablePatchMediaTypes();
				HttpOptionsHandler handler = new HttpOptionsHandler(methods, mediaTypes);
				return new HandlerMethod(handler, HTTP_OPTIONS_HANDLE_METHOD);
			}
			throw new MethodNotAllowedException(httpMethod, methods);
		}

		if (helper.hasConsumesMismatch()) {
			Set<MediaType> mediaTypes = helper.getConsumableMediaTypes();
			MediaType contentType;
			try {
				contentType = request.getHeaders().getContentType();
			}
			catch (InvalidMediaTypeException ex) {
				throw new UnsupportedMediaTypeStatusException(ex.getMessage());
			}
			throw new UnsupportedMediaTypeStatusException(contentType, new ArrayList<>(mediaTypes), exchange.getRequest().getMethod());
		}

		if (helper.hasProducesMismatch()) {
			Set<MediaType> mediaTypes = helper.getProducibleMediaTypes();
			throw new NotAcceptableStatusException(new ArrayList<>(mediaTypes));
		}

		if (helper.hasParamsMismatch()) {
			throw new ServerWebInputException(
					"Unsatisfied query parameter conditions: " + helper.getParamConditions() +
							", actual parameters: " + request.getQueryParams());
		}

		return null;
	}


	/**
	 * 汇总所有部分匹配，并公开跨这些匹配进行检查的方法。
	 */
	private static class PartialMatchHelper {

		private final List<PartialMatch> partialMatches = new ArrayList<>();

		PartialMatchHelper(Set<RequestMappingInfo> infos, ServerWebExchange exchange) {
			for (RequestMappingInfo info : infos) {
				if (info.getPatternsCondition().getMatchingCondition(exchange) != null) {
					this.partialMatches.add(new PartialMatch(info, exchange));
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
		 * 是否有针对 "methods" 的部分匹配？
		 */
		public boolean hasMethodsMismatch() {
			return this.partialMatches.stream().
					noneMatch(PartialMatch::hasMethodsMatch);
		}

		/**
		 * 是否有针对 "methods" 和 "consumes" 的部分匹配？
		 */
		public boolean hasConsumesMismatch() {
			return this.partialMatches.stream().
					noneMatch(PartialMatch::hasConsumesMatch);
		}

		/**
		 * 是否有针对 "methods"、"consumes" 和 "produces" 的部分匹配？
		 */
		public boolean hasProducesMismatch() {
			return this.partialMatches.stream().
					noneMatch(PartialMatch::hasProducesMatch);
		}

		/**
		 * 是否有针对 "methods"、"consumes"、"produces" 和 "params" 的部分匹配？
		 */
		public boolean hasParamsMismatch() {
			return this.partialMatches.stream().
					noneMatch(PartialMatch::hasParamsMatch);
		}

		/**
		 * 返回声明的 HTTP 方法。
		 */
		public Set<HttpMethod> getAllowedMethods() {
			return this.partialMatches.stream().
					flatMap(m -> m.getInfo().getMethodsCondition().getMethods().stream()).
					map(requestMethod -> HttpMethod.resolve(requestMethod.name())).
					collect(Collectors.toSet());
		}

		/**
		 * 返回声明的 "consumable" 类型，但仅限于同时匹配 "methods" 条件的类型。
		 */
		public Set<MediaType> getConsumableMediaTypes() {
			return this.partialMatches.stream().filter(PartialMatch::hasMethodsMatch).
					flatMap(m -> m.getInfo().getConsumesCondition().getConsumableMediaTypes().stream()).
					collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * 返回声明的 "producible" 类型，但仅限于同时匹配 "methods" 和 "consumes" 条件的类型。
		 */
		public Set<MediaType> getProducibleMediaTypes() {
			return this.partialMatches.stream().filter(PartialMatch::hasConsumesMatch).
					flatMap(m -> m.getInfo().getProducesCondition().getProducibleMediaTypes().stream()).
					collect(Collectors.toCollection(LinkedHashSet::new));
		}

		/**
		 * 返回声明的 "params" 条件，但仅限于同时匹配 "methods"、"consumes" 和 "params" 条件的条件。
		 */
		public List<Set<NameValueExpression<String>>> getParamConditions() {
			return this.partialMatches.stream().filter(PartialMatch::hasProducesMatch).
					map(match -> match.getInfo().getParamsCondition().getExpressions()).
					collect(Collectors.toList());
		}

		/**
		 * 返回声明的 "consumable" 类型，但仅限于指定了 PATCH 或完全没有方法的类型。
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
		 * 至少匹配 URL 路径的 RequestMappingInfo 容器。
		 */
		private static class PartialMatch {

			private final RequestMappingInfo info;

			private final boolean methodsMatch;

			private final boolean consumesMatch;

			private final boolean producesMatch;

			private final boolean paramsMatch;


			/**
			 * 创建新的 {@link PartialMatch} 实例。
			 * @param info the RequestMappingInfo that matches the URL path
			 * @param exchange the current exchange
			 */
			public PartialMatch(RequestMappingInfo info, ServerWebExchange exchange) {
				this.info = info;
				this.methodsMatch = info.getMethodsCondition().getMatchingCondition(exchange) != null;
				this.consumesMatch = info.getConsumesCondition().getMatchingCondition(exchange) != null;
				this.producesMatch = info.getProducesCondition().getMatchingCondition(exchange) != null;
				this.paramsMatch = info.getParamsCondition().getMatchingCondition(exchange) != null;
			}


			public RequestMappingInfo getInfo() {
				return this.info;
			}

			public boolean hasMethodsMatch() {
				return this.methodsMatch;
			}

			public boolean hasConsumesMatch() {
				return hasMethodsMatch() && this.consumesMatch;
			}

			public boolean hasProducesMatch() {
				return hasConsumesMatch() && this.producesMatch;
			}

			public boolean hasParamsMatch() {
				return hasProducesMatch() && this.paramsMatch;
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


		public HttpOptionsHandler(Set<HttpMethod> declaredMethods, Set<MediaType> acceptPatch) {
			this.headers.setAllow(initAllowedHttpMethods(declaredMethods));
			this.headers.setAcceptPatch(new ArrayList<>(acceptPatch));
		}

		private static Set<HttpMethod> initAllowedHttpMethods(Set<HttpMethod> declaredMethods) {
			if (declaredMethods.isEmpty()) {
				return EnumSet.allOf(HttpMethod.class).stream()
						.filter(method -> method != HttpMethod.TRACE)
						.collect(Collectors.toSet());
			}
			else {
				Set<HttpMethod> result = new LinkedHashSet<>(declaredMethods);
				if (result.contains(HttpMethod.GET)) {
					result.add(HttpMethod.HEAD);
				}
				result.add(HttpMethod.OPTIONS);
				return result;
			}
		}

		@SuppressWarnings("unused")
		public HttpHeaders handle() {
			return this.headers;
		}
	}

}
