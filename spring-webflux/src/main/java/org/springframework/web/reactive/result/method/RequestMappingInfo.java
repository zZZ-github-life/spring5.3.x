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

package org.springframework.web.reactive.result.method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.HeadersRequestCondition;
import org.springframework.web.reactive.result.condition.ParamsRequestCondition;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.condition.ProducesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.reactive.result.condition.RequestConditionHolder;
import org.springframework.web.reactive.result.condition.RequestMethodsRequestCondition;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 请求映射信息。封装以下请求映射条件：
 * <ol>
 * <li>{@link PatternsRequestCondition}
 * <li>{@link RequestMethodsRequestCondition}
 * <li>{@link ParamsRequestCondition}
 * <li>{@link HeadersRequestCondition}
 * <li>{@link ConsumesRequestCondition}
 * <li>{@link ProducesRequestCondition}
 * <li>{@code RequestCondition}（可选，自定义请求条件）
 * </ol>
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	private static final PatternsRequestCondition EMPTY_PATTERNS = new PatternsRequestCondition();

	private static final RequestMethodsRequestCondition EMPTY_REQUEST_METHODS = new RequestMethodsRequestCondition();

	private static final ParamsRequestCondition EMPTY_PARAMS = new ParamsRequestCondition();

	private static final HeadersRequestCondition EMPTY_HEADERS = new HeadersRequestCondition();

	private static final ConsumesRequestCondition EMPTY_CONSUMES = new ConsumesRequestCondition();

	private static final ProducesRequestCondition EMPTY_PRODUCES = new ProducesRequestCondition();

	private static final RequestConditionHolder EMPTY_CUSTOM = new RequestConditionHolder(null);


	@Nullable
	private final String name;

	private final PatternsRequestCondition patternsCondition;

	private final RequestMethodsRequestCondition methodsCondition;

	private final ParamsRequestCondition paramsCondition;

	private final HeadersRequestCondition headersCondition;

	private final ConsumesRequestCondition consumesCondition;

	private final ProducesRequestCondition producesCondition;

	private final RequestConditionHolder customConditionHolder;

	private final int hashCode;

	private final BuilderConfiguration options;


	/**
	 * 带映射名称的完整构造函数。
	 * @deprecated as of 5.3.4 in favor using {@link RequestMappingInfo.Builder} via {@link #paths(String...)}.
	 */
	@Deprecated
	public RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
			@Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
			@Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
			@Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {

		this(name, patterns, methods, params, headers, consumes, produces, custom, new BuilderConfiguration());
	}

	/**
	 * 使用给定条件创建实例。
	 * @deprecated as of 5.3.4 in favor using {@link RequestMappingInfo.Builder} via {@link #paths(String...)}.
	 */
	@Deprecated
	public RequestMappingInfo(@Nullable PatternsRequestCondition patterns,
			@Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
			@Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
			@Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {

		this(null, patterns, methods, params, headers, consumes, produces, custom);
	}

	/**
	 * 使用给定的自定义请求条件重新创建 RequestMappingInfo。
	 * @deprecated since 5.3.4 in favor of using a {@link Builder} via {@link #mutate()}.
	 */
	@Deprecated
	public RequestMappingInfo(RequestMappingInfo info, @Nullable RequestCondition<?> customRequestCondition) {
		this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition,
				info.consumesCondition, info.producesCondition, customRequestCondition);
	}

	private RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
			@Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
			@Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
			@Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom,
			BuilderConfiguration options) {

		this.name = (StringUtils.hasText(name) ? name : null);
		this.patternsCondition = (patterns != null ? patterns : EMPTY_PATTERNS);
		this.methodsCondition = (methods != null ? methods : EMPTY_REQUEST_METHODS);
		this.paramsCondition = (params != null ? params : EMPTY_PARAMS);
		this.headersCondition = (headers != null ? headers : EMPTY_HEADERS);
		this.consumesCondition = (consumes != null ? consumes : EMPTY_CONSUMES);
		this.producesCondition = (produces != null ? produces : EMPTY_PRODUCES);
		this.customConditionHolder = (custom != null ? new RequestConditionHolder(custom) : EMPTY_CUSTOM);
		this.options = options;

		this.hashCode = calculateHashCode(
				this.patternsCondition, this.methodsCondition, this.paramsCondition, this.headersCondition,
				this.consumesCondition, this.producesCondition, this.customConditionHolder);
	}


	/**
	 * 返回此映射的名称，或 {@code null}。
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 URL 模式；如果没有模式，则返回包含 0 个模式的实例，绝不会是 {@code null}。
	 */
	public PatternsRequestCondition getPatternsCondition() {
		return this.patternsCondition;
	}

	/**
	 * 返回不是模式的映射路径。
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		return this.patternsCondition.getDirectPaths();
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 HTTP 请求方法；如果没有请求方法，则返回包含 0 个请求方法的实例，绝不会是 {@code null}。
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return this.methodsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "parameters" 条件；如果没有参数表达式，则返回包含 0 个参数表达式的实例，绝不会是 {@code null}。
	 */
	public ParamsRequestCondition getParamsCondition() {
		return this.paramsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "headers" 条件；如果没有头表达式，则返回包含 0 个头表达式的实例，绝不会是 {@code null}。
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return this.headersCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "consumes" 条件；如果没有 consumes 表达式，则返回包含 0 个 consumes 表达式的实例，绝不会是 {@code null}。
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return this.consumesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "produces" 条件；如果没有 produces 表达式，则返回包含 0 个 produces 表达式的实例，绝不会是 {@code null}。
	 */
	public ProducesRequestCondition getProducesCondition() {
		return this.producesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "custom" 条件；或 {@code null}。
	 */
	@Nullable
	public RequestCondition<?> getCustomCondition() {
		return this.customConditionHolder.getCondition();
	}


	/**
	 * 将“此”请求映射信息（即当前实例）与另一个请求映射信息实例组合。
	 * <p>示例：组合类型级和方法级请求映射。
	 * @return a new request mapping info instance; never {@code null}
	 */
	@Override
	public RequestMappingInfo combine(RequestMappingInfo other) {
		String name = combineNames(other);
		PatternsRequestCondition patterns = this.patternsCondition.combine(other.patternsCondition);
		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		return new RequestMappingInfo(name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition(), this.options);
	}

	@Nullable
	private String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			return this.name + "#" + other.name;
		}
		else if (this.name != null) {
			return this.name;
		}
		else {
			return other.name;
		}
	}

	/**
	 * 检查此请求映射信息中的所有条件是否匹配提供的请求，并返回一个可能的新请求映射信息，其中的条件针对当前请求进行了调整。
	 * <p>例如，返回的实例可能包含与当前请求匹配的 URL 模式子集，并按最佳匹配模式排在前面排序。
	 * @return a new instance in case all conditions match; or {@code null} otherwise
	 */
	@Override
	@Nullable
	public RequestMappingInfo getMatchingCondition(ServerWebExchange exchange) {
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(exchange);
		if (methods == null) {
			return null;
		}
		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(exchange);
		if (params == null) {
			return null;
		}
		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(exchange);
		if (headers == null) {
			return null;
		}
		// Match "Content-Type" and "Accept" (parsed ones and cached) before patterns
		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(exchange);
		if (consumes == null) {
			return null;
		}
		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(exchange);
		if (produces == null) {
			return null;
		}
		PatternsRequestCondition patterns = this.patternsCondition.getMatchingCondition(exchange);
		if (patterns == null) {
			return null;
		}
		RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(exchange);
		if (custom == null) {
			return null;
		}
		return new RequestMappingInfo(this.name, patterns,
				methods, params, headers, consumes, produces, custom.getCondition(), this.options);
	}

	/**
	 * 在请求的上下文中比较“此”信息（即当前实例）与另一个信息。
	 * <p>注意：假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，以确保它们的条件包含与当前请求相关的内容。
	 */
	@Override
	public int compareTo(RequestMappingInfo other, ServerWebExchange exchange) {
		int result = this.patternsCondition.compareTo(other.getPatternsCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.paramsCondition.compareTo(other.getParamsCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.headersCondition.compareTo(other.getHeadersCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.consumesCondition.compareTo(other.getConsumesCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.producesCondition.compareTo(other.getProducesCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.methodsCondition.compareTo(other.getMethodsCondition(), exchange);
		if (result != 0) {
			return result;
		}
		result = this.customConditionHolder.compareTo(other.customConditionHolder, exchange);
		if (result != 0) {
			return result;
		}
		return 0;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RequestMappingInfo)) {
			return false;
		}
		RequestMappingInfo otherInfo = (RequestMappingInfo) other;
		return (this.patternsCondition.equals(otherInfo.patternsCondition) &&
				this.methodsCondition.equals(otherInfo.methodsCondition) &&
				this.paramsCondition.equals(otherInfo.paramsCondition) &&
				this.headersCondition.equals(otherInfo.headersCondition) &&
				this.consumesCondition.equals(otherInfo.consumesCondition) &&
				this.producesCondition.equals(otherInfo.producesCondition) &&
				this.customConditionHolder.equals(otherInfo.customConditionHolder));
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	private static int calculateHashCode(
			PatternsRequestCondition patterns, RequestMethodsRequestCondition methods,
			ParamsRequestCondition params, HeadersRequestCondition headers,
			ConsumesRequestCondition consumes, ProducesRequestCondition produces,
			RequestConditionHolder custom) {

		return patterns.hashCode() * 31 + methods.hashCode() + params.hashCode() +
				headers.hashCode() + consumes.hashCode() + produces.hashCode() + custom.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		if (!this.methodsCondition.isEmpty()) {
			Set<RequestMethod> httpMethods = this.methodsCondition.getMethods();
			builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
		}
		if (!this.patternsCondition.isEmpty()) {
			Set<PathPattern> patterns = this.patternsCondition.getPatterns();
			builder.append(' ').append(patterns.size() == 1 ? patterns.iterator().next() : patterns);
		}
		if (!this.paramsCondition.isEmpty()) {
			builder.append(", params ").append(this.paramsCondition);
		}
		if (!this.headersCondition.isEmpty()) {
			builder.append(", headers ").append(this.headersCondition);
		}
		if (!this.consumesCondition.isEmpty()) {
			builder.append(", consumes ").append(this.consumesCondition);
		}
		if (!this.producesCondition.isEmpty()) {
			builder.append(", produces ").append(this.producesCondition);
		}
		if (!this.customConditionHolder.isEmpty()) {
			builder.append(", and ").append(this.customConditionHolder);
		}
		builder.append('}');
		return builder.toString();
	}


	/**
	 * 返回用于通过修改此实例来创建新的 RequestMappingInfo 的构建器。
	 * @return a builder to create a new, modified instance
	 * @since 5.3.4
	 */
	public Builder mutate() {
		return new MutateBuilder(this);
	}

	/**
	 * 使用给定路径创建新的 {@code RequestMappingInfo.Builder}。
	 * @param paths the paths to use
	 */
	public static Builder paths(String... paths) {
		return new DefaultBuilder(paths);
	}


	/**
	 * 定义用于创建 RequestMappingInfo 的构建器。
	 */
	public interface Builder {

		/**
		 * 设置路径模式。
		 */
		Builder paths(String... paths);

		/**
		 * 设置请求方法条件。
		 */
		Builder methods(RequestMethod... methods);

		/**
		 * 设置请求参数条件。
		 */
		Builder params(String... params);

		/**
		 * 设置头条件。
		 * <p>默认不设置。
		 */
		Builder headers(String... headers);

		/**
		 * 设置 consumes 条件。
		 */
		Builder consumes(String... consumes);

		/**
		 * 设置 produces 条件。
		 */
		Builder produces(String... produces);

		/**
		 * 设置映射名称。
		 */
		Builder mappingName(String name);

		/**
		 * 设置要使用的自定义条件。
		 */
		Builder customCondition(RequestCondition<?> condition);

		/**
		 * 提供请求映射所需的附加配置。
		 */
		Builder options(BuilderConfiguration options);

		/**
		 * 构建 RequestMappingInfo。
		 */
		RequestMappingInfo build();
	}


	private static class DefaultBuilder implements Builder {

		private String[] paths;

		@Nullable
		private RequestMethod[] methods;

		@Nullable
		private String[] params;

		@Nullable
		private String[] headers;

		@Nullable
		private String[] consumes;

		@Nullable
		private String[] produces;

		private boolean hasContentType;

		private boolean hasAccept;

		@Nullable
		private String mappingName;

		@Nullable
		private RequestCondition<?> customCondition;

		private BuilderConfiguration options = new BuilderConfiguration();


		public DefaultBuilder(String... paths) {
			this.paths = paths;
		}

		@Override
		public Builder paths(String... paths) {
			this.paths = paths;
			return this;
		}

		@Override
		public DefaultBuilder methods(RequestMethod... methods) {
			this.methods = methods;
			return this;
		}

		@Override
		public DefaultBuilder params(String... params) {
			this.params = params;
			return this;
		}

		@Override
		public DefaultBuilder headers(String... headers) {
			for (String header : headers) {
				this.hasContentType = this.hasContentType ||
						header.contains("Content-Type") || header.contains("content-type");
				this.hasAccept = this.hasAccept ||
						header.contains("Accept") || header.contains("accept");
			}
			this.headers = headers;
			return this;
		}

		@Override
		public DefaultBuilder consumes(String... consumes) {
			this.consumes = consumes;
			return this;
		}

		@Override
		public DefaultBuilder produces(String... produces) {
			this.produces = produces;
			return this;
		}

		@Override
		public DefaultBuilder mappingName(String name) {
			this.mappingName = name;
			return this;
		}

		@Override
		public DefaultBuilder customCondition(RequestCondition<?> condition) {
			this.customCondition = condition;
			return this;
		}

		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		@Override
		public RequestMappingInfo build() {
			PathPatternParser parser = (this.options.getPatternParser() != null ?
					this.options.getPatternParser() : PathPatternParser.defaultInstance);

			RequestedContentTypeResolver contentTypeResolver = this.options.getContentTypeResolver();

			return new RequestMappingInfo(this.mappingName,
					isEmpty(this.paths) ? null : new PatternsRequestCondition(parse(this.paths, parser)),
					ObjectUtils.isEmpty(this.methods) ?
							null : new RequestMethodsRequestCondition(this.methods),
					ObjectUtils.isEmpty(this.params) ?
							null : new ParamsRequestCondition(this.params),
					ObjectUtils.isEmpty(this.headers) ?
							null : new HeadersRequestCondition(this.headers),
					ObjectUtils.isEmpty(this.consumes) && !this.hasContentType ?
							null : new ConsumesRequestCondition(this.consumes, this.headers),
					ObjectUtils.isEmpty(this.produces) && !this.hasAccept ?
							null : new ProducesRequestCondition(this.produces, this.headers, contentTypeResolver),
					this.customCondition,
					this.options);
		}

		static List<PathPattern> parse(String[] patterns, PathPatternParser parser) {
			if (isEmpty(patterns)) {
				return Collections.emptyList();
			}
			List<PathPattern> result = new ArrayList<>(patterns.length);
			for (String path : patterns) {
				if (StringUtils.hasText(path) && !path.startsWith("/")) {
					path = "/" + path;
				}
				result.add(parser.parse(path));
			}
			return result;
		}

		static boolean isEmpty(String[] patterns) {
			if (!ObjectUtils.isEmpty(patterns)) {
				for (String pattern : patterns) {
					if (StringUtils.hasText(pattern)) {
						return false;
					}
				}
			}
			return true;
		}
	}


	private static class MutateBuilder implements Builder {

		@Nullable
		private String name;

		@Nullable
		private PatternsRequestCondition patternsCondition;

		private RequestMethodsRequestCondition methodsCondition;

		private ParamsRequestCondition paramsCondition;

		private HeadersRequestCondition headersCondition;

		private ConsumesRequestCondition consumesCondition;

		private ProducesRequestCondition producesCondition;

		private RequestConditionHolder customConditionHolder;

		private BuilderConfiguration options;

		public MutateBuilder(RequestMappingInfo other) {
			this.name = other.name;
			this.patternsCondition = other.patternsCondition;
			this.methodsCondition = other.methodsCondition;
			this.paramsCondition = other.paramsCondition;
			this.headersCondition = other.headersCondition;
			this.consumesCondition = other.consumesCondition;
			this.producesCondition = other.producesCondition;
			this.customConditionHolder = other.customConditionHolder;
			this.options = other.options;
		}

		@Override
		public Builder paths(String... paths) {
			PathPatternParser parser = (this.options.getPatternParser() != null ?
					this.options.getPatternParser() : PathPatternParser.defaultInstance);
			this.patternsCondition = (DefaultBuilder.isEmpty(paths) ?
					null : new PatternsRequestCondition(DefaultBuilder.parse(paths, parser)));
			return this;
		}

		@Override
		public Builder methods(RequestMethod... methods) {
			this.methodsCondition = (ObjectUtils.isEmpty(methods) ?
					EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(methods));
			return this;
		}

		@Override
		public Builder params(String... params) {
			this.paramsCondition = (ObjectUtils.isEmpty(params) ?
					EMPTY_PARAMS : new ParamsRequestCondition(params));
			return this;
		}

		@Override
		public Builder headers(String... headers) {
			this.headersCondition = (ObjectUtils.isEmpty(headers) ?
					EMPTY_HEADERS : new HeadersRequestCondition(headers));
			return this;
		}

		@Override
		public Builder consumes(String... consumes) {
			this.consumesCondition = (ObjectUtils.isEmpty(consumes) ?
					EMPTY_CONSUMES : new ConsumesRequestCondition(consumes));
			return this;
		}

		@Override
		public Builder produces(String... produces) {
			this.producesCondition = (ObjectUtils.isEmpty(produces) ?
					EMPTY_PRODUCES :
					new ProducesRequestCondition(produces, null, this.options.getContentTypeResolver()));
			return this;
		}

		@Override
		public Builder mappingName(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder customCondition(RequestCondition<?> condition) {
			this.customConditionHolder = new RequestConditionHolder(condition);
			return this;
		}

		@Override
		public Builder options(BuilderConfiguration options) {
			this.options = options;
			return this;
		}

		@Override
		public RequestMappingInfo build() {
			return new RequestMappingInfo(this.name, this.patternsCondition,
					this.methodsCondition, this.paramsCondition, this.headersCondition,
					this.consumesCondition, this.producesCondition,
					this.customConditionHolder, this.options);
		}
	}


	/**
	 * 用于请求映射目的的配置选项容器。此类配置是创建 RequestMappingInfo 实例所必需的，但通常在所有 RequestMappingInfo 实例之间共享。
	 * @see Builder#options
	 */
	public static class BuilderConfiguration {

		@Nullable
		private PathPatternParser patternParser;

		@Nullable
		private RequestedContentTypeResolver contentTypeResolver;

		public void setPatternParser(PathPatternParser patternParser) {
			this.patternParser = patternParser;
		}

		@Nullable
		public PathPatternParser getPatternParser() {
			return this.patternParser;
		}

		/**
		 * 设置用于 ProducesRequestCondition 的 ContentNegotiationManager。
		 * <p>默认不设置。
		 */
		public void setContentTypeResolver(RequestedContentTypeResolver resolver) {
			this.contentTypeResolver = resolver;
		}

		@Nullable
		public RequestedContentTypeResolver getContentTypeResolver() {
			return this.contentTypeResolver;
		}
	}

}
