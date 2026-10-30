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

package org.springframework.web.servlet.mvc.method;

import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestConditionHolder;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 请求映射信息。以下条件的组合体：
 * <ol>
 * <li>使用解析后 {@code PathPatterns} 的 {@link PathPatternsRequestCondition}，或
 * 通过 {@code PathMatcher} 使用字符串模式的 {@link PatternsRequestCondition}
 * <li>{@link RequestMethodsRequestCondition}
 * <li>{@link ParamsRequestCondition}
 * <li>{@link HeadersRequestCondition}
 * <li>{@link ConsumesRequestCondition}
 * <li>{@link ProducesRequestCondition}
 * <li>{@code RequestCondition}（可选的自定义请求条件）
 * </ol>
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

	private static final PathPatternsRequestCondition EMPTY_PATH_PATTERNS = new PathPatternsRequestCondition();

	private static final PatternsRequestCondition EMPTY_PATTERNS = new PatternsRequestCondition();

	private static final RequestMethodsRequestCondition EMPTY_REQUEST_METHODS = new RequestMethodsRequestCondition();

	private static final ParamsRequestCondition EMPTY_PARAMS = new ParamsRequestCondition();

	private static final HeadersRequestCondition EMPTY_HEADERS = new HeadersRequestCondition();

	private static final ConsumesRequestCondition EMPTY_CONSUMES = new ConsumesRequestCondition();

	private static final ProducesRequestCondition EMPTY_PRODUCES = new ProducesRequestCondition();

	private static final RequestConditionHolder EMPTY_CUSTOM = new RequestConditionHolder(null);


	@Nullable
	private final String name;

	@Nullable
	private final PathPatternsRequestCondition pathPatternsCondition;

	@Nullable
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
	 * @deprecated as of 5.3 in favor using {@link RequestMappingInfo.Builder} via
	 * {@link #paths(String...)}.
	 */
	@Deprecated
	public RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns,
			@Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params,
			@Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes,
			@Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {

		this(name, null,
				(patterns != null ? patterns : EMPTY_PATTERNS),
				(methods != null ? methods : EMPTY_REQUEST_METHODS),
				(params != null ? params : EMPTY_PARAMS),
				(headers != null ? headers : EMPTY_HEADERS),
				(consumes != null ? consumes : EMPTY_CONSUMES),
				(produces != null ? produces : EMPTY_PRODUCES),
				(custom != null ? new RequestConditionHolder(custom) : EMPTY_CUSTOM),
				new BuilderConfiguration());
	}

	/**
	 * 使用给定条件创建实例。
	 * @deprecated as of 5.3 in favor using {@link RequestMappingInfo.Builder} via
	 * {@link #paths(String...)}.
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
	 * @deprecated since 5.3 in favor of using {@link #addCustomCondition(RequestCondition)}.
	 */
	@Deprecated
	public RequestMappingInfo(RequestMappingInfo info, @Nullable RequestCondition<?> customRequestCondition) {
		this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition,
				info.consumesCondition, info.producesCondition, customRequestCondition);
	}

	private RequestMappingInfo(@Nullable String name,
			@Nullable PathPatternsRequestCondition pathPatternsCondition,
			@Nullable PatternsRequestCondition patternsCondition,
			RequestMethodsRequestCondition methodsCondition, ParamsRequestCondition paramsCondition,
			HeadersRequestCondition headersCondition, ConsumesRequestCondition consumesCondition,
			ProducesRequestCondition producesCondition, RequestConditionHolder customCondition,
			BuilderConfiguration options) {

		Assert.isTrue(pathPatternsCondition != null || patternsCondition != null,
				"Neither PathPatterns nor String patterns condition");

		this.name = (StringUtils.hasText(name) ? name : null);
		this.pathPatternsCondition = pathPatternsCondition;
		this.patternsCondition = patternsCondition;
		this.methodsCondition = methodsCondition;
		this.paramsCondition = paramsCondition;
		this.headersCondition = headersCondition;
		this.consumesCondition = consumesCondition;
		this.producesCondition = producesCondition;
		this.customConditionHolder = customCondition;
		this.options = options;

		this.hashCode = calculateHashCode(
				this.pathPatternsCondition, this.patternsCondition,
				this.methodsCondition, this.paramsCondition, this.headersCondition,
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
	 * 返回在解析模式
	 * {@link AbstractHandlerMapping#usesPathPatterns() 启用}时使用的模式条件。
	 * <p>这与 {@link #getPatternsCondition()} 互斥，即其中一个返回 {@code null} 时，
	 * 另一个会返回实例。
	 * @since 5.3
	 * @see #getActivePatternsCondition()
	 */
	@Nullable
	public PathPatternsRequestCondition getPathPatternsCondition() {
		return this.pathPatternsCondition;
	}

	/**
	 * 返回在使用通过 {@link PathMatcher} 进行字符串模式匹配时的模式条件。
	 * <p>这与 {@link #getPathPatternsCondition()} 互斥，
	 * 即其中一个返回 {@code null} 时，另一个会返回实例。
	 */
	@Nullable
	public PatternsRequestCondition getPatternsCondition() {
		return this.patternsCondition;
	}

	/**
	 * 根据哪一个不为 null，返回 {@link #getPathPatternsCondition()} 或
	 * {@link #getPatternsCondition()}。
	 * @since 5.3
	 */
	@SuppressWarnings("unchecked")
	public <T> RequestCondition<T> getActivePatternsCondition() {
		if (this.pathPatternsCondition != null) {
			return (RequestCondition<T>) this.pathPatternsCondition;
		}
		else if (this.patternsCondition != null) {
			return (RequestCondition<T>) this.patternsCondition;
		}
		else {
			// Already checked in the constructor...
			throw new IllegalStateException();
		}
	}

	/**
	 * 返回不是模式的映射路径。
	 * @since 5.3
	 */
	public Set<String> getDirectPaths() {
		RequestCondition<?> condition = getActivePatternsCondition();
		return (condition instanceof PathPatternsRequestCondition ?
				((PathPatternsRequestCondition) condition).getDirectPaths() :
				((PatternsRequestCondition) condition).getDirectPaths());
	}

	/**
	 * 返回 {@link #getActivePatternsCondition() 活动}
	 * 模式条件的模式，以字符串表示。
	 * @since 5.3
	 */
	public Set<String> getPatternValues() {
		RequestCondition<?> condition = getActivePatternsCondition();
		return (condition instanceof PathPatternsRequestCondition ?
				((PathPatternsRequestCondition) condition).getPatternValues() :
				((PatternsRequestCondition) condition).getPatterns());
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 HTTP 请求方法；
	 * 如果没有则为包含 0 个请求方法的实例（绝不为 {@code null}）。
	 */
	public RequestMethodsRequestCondition getMethodsCondition() {
		return this.methodsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "parameters" 条件；
	 * 如果没有则为包含 0 个参数表达式的实例（绝不为 {@code null}）。
	 */
	public ParamsRequestCondition getParamsCondition() {
		return this.paramsCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "headers" 条件；
	 * 如果没有则为包含 0 个头表达式的实例（绝不为 {@code null}）。
	 */
	public HeadersRequestCondition getHeadersCondition() {
		return this.headersCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "consumes" 条件；
	 * 如果没有则为包含 0 个 consumes 表达式的实例（绝不为 {@code null}）。
	 */
	public ConsumesRequestCondition getConsumesCondition() {
		return this.consumesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "produces" 条件；
	 * 如果没有则为包含 0 个 produces 表达式的实例（绝不为 {@code null}）。
	 */
	public ProducesRequestCondition getProducesCondition() {
		return this.producesCondition;
	}

	/**
	 * 返回此 {@link RequestMappingInfo} 的 "custom" 条件，或 {@code null}。
	 */
	@Nullable
	public RequestCondition<?> getCustomCondition() {
		return this.customConditionHolder.getCondition();
	}

	/**
	 * 基于当前实例创建一个新实例，同时添加给定的
	 * 自定义条件。
	 * @param customCondition 要添加的自定义条件
	 * @since 5.3
	 */
	public RequestMappingInfo addCustomCondition(RequestCondition<?> customCondition) {
		return new RequestMappingInfo(this.name,
				this.pathPatternsCondition, this.patternsCondition,
				this.methodsCondition, this.paramsCondition, this.headersCondition,
				this.consumesCondition, this.producesCondition,
				new RequestConditionHolder(customCondition), this.options);
	}

	/**
	 * 将 "this" 请求映射信息（即当前实例）与
	 * 另一个请求映射信息实例组合起来。
	 * <p>示例：组合类型级别和方法级别的请求映射。
	 * @return 新的请求映射信息实例；永不为 {@code null}
	 */
	@Override
	public RequestMappingInfo combine(RequestMappingInfo other) {
		String name = combineNames(other);

		PathPatternsRequestCondition pathPatterns =
				(this.pathPatternsCondition != null && other.pathPatternsCondition != null ?
						this.pathPatternsCondition.combine(other.pathPatternsCondition) : null);

		PatternsRequestCondition patterns =
				(this.patternsCondition != null && other.patternsCondition != null ?
						this.patternsCondition.combine(other.patternsCondition) : null);

		RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
		ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
		HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
		ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
		ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
		RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);

		return new RequestMappingInfo(name, pathPatterns, patterns,
				methods, params, headers, consumes, produces, custom, this.options);
	}

	@Nullable
	private String combineNames(RequestMappingInfo other) {
		if (this.name != null && other.name != null) {
			String separator = RequestMappingInfoHandlerMethodMappingNamingStrategy.SEPARATOR;
			return this.name + separator + other.name;
		}
		else if (this.name != null) {
			return this.name;
		}
		else {
			return other.name;
		}
	}

	/**
	 * 检查此请求映射信息中的所有条件是否匹配所提供的
	 * 请求，并返回一个可能为当前请求量身定制条件的
	 * 新请求映射信息。
	 * <p>例如，返回的实例可能包含与当前请求匹配的 URL
	 * 模式子集，并按最匹配的模式排在前面进行排序。
	 * @return 匹配时为新实例；否则为 {@code null}
	 */
	@Override
	@Nullable
	public RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
		RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(request);
		if (methods == null) {
			return null;
		}
		ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(request);
		if (params == null) {
			return null;
		}
		HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(request);
		if (headers == null) {
			return null;
		}
		ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(request);
		if (consumes == null) {
			return null;
		}
		ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(request);
		if (produces == null) {
			return null;
		}
		PathPatternsRequestCondition pathPatterns = null;
		if (this.pathPatternsCondition != null) {
			pathPatterns = this.pathPatternsCondition.getMatchingCondition(request);
			if (pathPatterns == null) {
				return null;
			}
		}
		PatternsRequestCondition patterns = null;
		if (this.patternsCondition != null) {
			patterns = this.patternsCondition.getMatchingCondition(request);
			if (patterns == null) {
				return null;
			}
		}
		RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(request);
		if (custom == null) {
			return null;
		}
		return new RequestMappingInfo(this.name, pathPatterns, patterns,
				methods, params, headers, consumes, produces, custom, this.options);
	}

	/**
	 * 在请求的上下文中比较 "this" 信息（即当前实例）
	 * 与另一个信息。
	 * <p>注意：假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，以确保它们具有
	 * 与当前请求相关的内容条件。
	 */
	@Override
	public int compareTo(RequestMappingInfo other, HttpServletRequest request) {
		int result;
		// Automatic vs explicit HTTP HEAD mapping
		if (HttpMethod.HEAD.matches(request.getMethod())) {
			result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
			if (result != 0) {
				return result;
			}
		}
		result = getActivePatternsCondition().compareTo(other.getActivePatternsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.paramsCondition.compareTo(other.getParamsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.headersCondition.compareTo(other.getHeadersCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.consumesCondition.compareTo(other.getConsumesCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.producesCondition.compareTo(other.getProducesCondition(), request);
		if (result != 0) {
			return result;
		}
		// Implicit (no method) vs explicit HTTP method mappings
		result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
		if (result != 0) {
			return result;
		}
		result = this.customConditionHolder.compareTo(other.customConditionHolder, request);
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
		return (getActivePatternsCondition().equals(otherInfo.getActivePatternsCondition()) &&
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

	@SuppressWarnings("ConstantConditions")
	private static int calculateHashCode(
			@Nullable PathPatternsRequestCondition pathPatterns, @Nullable PatternsRequestCondition patterns,
			RequestMethodsRequestCondition methods, ParamsRequestCondition params, HeadersRequestCondition headers,
			ConsumesRequestCondition consumes, ProducesRequestCondition produces, RequestConditionHolder custom) {

		return (pathPatterns != null ? pathPatterns : patterns).hashCode() * 31 +
				methods.hashCode() + params.hashCode() +
				headers.hashCode() + consumes.hashCode() + produces.hashCode() +
				custom.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{");
		if (!this.methodsCondition.isEmpty()) {
			Set<RequestMethod> httpMethods = this.methodsCondition.getMethods();
			builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
		}

		// Patterns conditions are never empty and have "" (empty path) at least.
		builder.append(' ').append(getActivePatternsCondition());

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
	 * 返回一个构建器，用于通过修改此实例来创建新的 RequestMappingInfo。
	 * @return 用于创建新的修改后实例的构建器
	 * @since 5.3.4
	 */
	public Builder mutate() {
		return new MutateBuilder(this);
	}


	/**
	 * 使用给定路径创建一个新的 {@code RequestMappingInfo.Builder}。
	 * @param paths 要使用的路径
	 * @since 4.2
	 */
	public static Builder paths(String... paths) {
		return new DefaultBuilder(paths);
	}


	/**
	 * 定义用于创建 RequestMappingInfo 的构建器。
	 * @since 4.2
	 */
	public interface Builder {

		/**
		 * 设置 URL 路径模式。
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

		private RequestMethod[] methods = new RequestMethod[0];

		private String[] params = new String[0];

		private String[] headers = new String[0];

		private String[] consumes = new String[0];

		private String[] produces = new String[0];

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
		@SuppressWarnings("deprecation")
		public RequestMappingInfo build() {

			PathPatternsRequestCondition pathPatterns = null;
			PatternsRequestCondition patterns = null;

			if (this.options.patternParser != null) {
				pathPatterns = (ObjectUtils.isEmpty(this.paths) ?
						EMPTY_PATH_PATTERNS :
						new PathPatternsRequestCondition(this.options.patternParser, this.paths));
			}
			else {
				patterns = (ObjectUtils.isEmpty(this.paths) ?
						EMPTY_PATTERNS :
						new PatternsRequestCondition(
								this.paths, null, this.options.getPathMatcher(),
								this.options.useSuffixPatternMatch(), this.options.useTrailingSlashMatch(),
								this.options.getFileExtensions()));
			}

			ContentNegotiationManager manager = this.options.getContentNegotiationManager();

			return new RequestMappingInfo(
					this.mappingName, pathPatterns, patterns,
					ObjectUtils.isEmpty(this.methods) ?
							EMPTY_REQUEST_METHODS : new RequestMethodsRequestCondition(this.methods),
					ObjectUtils.isEmpty(this.params) ?
							EMPTY_PARAMS : new ParamsRequestCondition(this.params),
					ObjectUtils.isEmpty(this.headers) ?
							EMPTY_HEADERS : new HeadersRequestCondition(this.headers),
					ObjectUtils.isEmpty(this.consumes) && !this.hasContentType ?
							EMPTY_CONSUMES : new ConsumesRequestCondition(this.consumes, this.headers),
					ObjectUtils.isEmpty(this.produces) && !this.hasAccept ?
							EMPTY_PRODUCES : new ProducesRequestCondition(this.produces, this.headers, manager),
					this.customCondition != null ?
							new RequestConditionHolder(this.customCondition) : EMPTY_CUSTOM,
					this.options);
		}
	}


	private static class MutateBuilder implements Builder {

		@Nullable
		private String name;

		@Nullable
		private PathPatternsRequestCondition pathPatternsCondition;

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
			this.pathPatternsCondition = other.pathPatternsCondition;
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
		@SuppressWarnings("deprecation")
		public Builder paths(String... paths) {
			if (this.options.patternParser != null) {
				this.pathPatternsCondition = (ObjectUtils.isEmpty(paths) ?
						EMPTY_PATH_PATTERNS : new PathPatternsRequestCondition(this.options.patternParser, paths));
			}
			else {
				this.patternsCondition = (ObjectUtils.isEmpty(paths) ?
						EMPTY_PATTERNS :
						new PatternsRequestCondition(
								paths, null, this.options.getPathMatcher(),
								this.options.useSuffixPatternMatch(), this.options.useTrailingSlashMatch(),
								this.options.getFileExtensions()));
			}
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
					new ProducesRequestCondition(produces, null, this.options.getContentNegotiationManager()));
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
			return new RequestMappingInfo(this.name,
					this.pathPatternsCondition, this.patternsCondition,
					this.methodsCondition, this.paramsCondition, this.headersCondition,
					this.consumesCondition, this.producesCondition,
					this.customConditionHolder, this.options);
		}
	}


	/**
	 * 用于请求映射目的的配置选项容器。
	 * 此类配置是创建 RequestMappingInfo 实例所必需的，
	 * 但通常在所有 RequestMappingInfo 实例之间共用。
	 * @since 4.2
	 * @see Builder#options
	 */
	public static class BuilderConfiguration {

		@Nullable
		private PathPatternParser patternParser;

		@Nullable
		private PathMatcher pathMatcher;

		private boolean trailingSlashMatch = true;

		private boolean suffixPatternMatch = false;

		private boolean registeredSuffixPatternMatch = false;

		@Nullable
		private ContentNegotiationManager contentNegotiationManager;


		/**
		 * 启用解析后的 {@link PathPattern}，如
		 * {@link AbstractHandlerMapping#setPatternParser(PathPatternParser)} 所述。
		 * <p><strong>注意：</strong>此属性与
		 * {@link #setPathMatcher(PathMatcher)} 互斥。
		 * <p>默认不启用。
		 * @since 5.3
		 */
		public void setPatternParser(@Nullable PathPatternParser patternParser) {
			this.patternParser = patternParser;
		}

		/**
		 * 返回 {@link #setPatternParser(PathPatternParser) configured}
		 * 的 {@code PathPatternParser}，或 {@code null}。
		 * @since 5.3
		 */
		@Nullable
		public PathPatternParser getPatternParser() {
			return this.patternParser;
		}

		/**
		 * 设置用于 PatternsRequestCondition 的自定义 UrlPathHelper。
		 * <p>默认不设置。
		 * @since 4.2.8
		 * @deprecated as of 5.3, the path is resolved externally and obtained with
		 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)}
		 */
		@Deprecated
		public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		}

		/**
		 * 返回已配置的 UrlPathHelper。
		 * @deprecated as of 5.3, the path is resolved externally and obtained with
		 * {@link ServletRequestPathUtils#getCachedPathValue(ServletRequest)};
		 * this method always returns {@link UrlPathHelper#defaultInstance}.
		 */
		@Nullable
		@Deprecated
		public UrlPathHelper getUrlPathHelper() {
			return UrlPathHelper.defaultInstance;
		}

		/**
		 * 设置用于 PatternsRequestCondition 的自定义 PathMatcher。
		 * <p>默认不设置。
		 */
		public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
			this.pathMatcher = pathMatcher;
		}

		/**
		 * 返回用于 PatternsRequestCondition 的自定义 PathMatcher（如果有）。
		 */
		@Nullable
		public PathMatcher getPathMatcher() {
			return this.pathMatcher;
		}

		/**
		 * 设置是否在 PatternsRequestCondition 中应用尾部斜杠匹配。
		 * <p>默认值为 'true'。
		 */
		public void setTrailingSlashMatch(boolean trailingSlashMatch) {
			this.trailingSlashMatch = trailingSlashMatch;
		}

		/**
		 * 返回是否在 PatternsRequestCondition 中应用尾部斜杠匹配。
		 */
		public boolean useTrailingSlashMatch() {
			return this.trailingSlashMatch;
		}

		/**
		 * 设置是否在 PatternsRequestCondition 中应用后缀模式匹配。
		 * <p>默认值为 'false'。
		 * @see #setRegisteredSuffixPatternMatch(boolean)
		 * @deprecated as of 5.2.4. See deprecation note on
		 * {@link RequestMappingHandlerMapping#setUseSuffixPatternMatch(boolean)}.
		 */
		@Deprecated
		public void setSuffixPatternMatch(boolean suffixPatternMatch) {
			this.suffixPatternMatch = suffixPatternMatch;
		}

		/**
		 * 返回是否在 PatternsRequestCondition 中应用后缀模式匹配。
		 * @deprecated as of 5.2.4. See deprecation note on
		 * {@link RequestMappingHandlerMapping#setUseSuffixPatternMatch(boolean)}.
		 */
		@Deprecated
		public boolean useSuffixPatternMatch() {
			return this.suffixPatternMatch;
		}

		/**
		 * 设置后缀模式匹配是否应仅限于已注册的文件扩展名。
		 * 设置此属性还会将 {@code suffixPatternMatch=true}，并且需要
		 * 同时配置 {@link #setContentNegotiationManager}，以便
		 * 获取已注册的文件扩展名。
		 * @deprecated as of 5.2.4. See class-level note in
		 * {@link RequestMappingHandlerMapping} on the deprecation of path
		 * extension config options.
		 */
		@Deprecated
		public void setRegisteredSuffixPatternMatch(boolean registeredSuffixPatternMatch) {
			this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
			this.suffixPatternMatch = (registeredSuffixPatternMatch || this.suffixPatternMatch);
		}

		/**
		 * 返回后缀模式匹配是否应仅限于已注册的
		 * 文件扩展名。
		 * @deprecated as of 5.2.4. See class-level note in
		 * {@link RequestMappingHandlerMapping} on the deprecation of path
		 * extension config options.
		 */
		@Deprecated
		public boolean useRegisteredSuffixPatternMatch() {
			return this.registeredSuffixPatternMatch;
		}

		/**
		 * 返回用于后缀模式匹配的文件扩展名。如果
		 * {@code registeredSuffixPatternMatch=true}，则扩展名从
		 * 已配置的 {@code contentNegotiationManager} 获取。
		 * @deprecated as of 5.2.4. See class-level note in
		 * {@link RequestMappingHandlerMapping} on the deprecation of path
		 * extension config options.
		 */
		@Nullable
		@Deprecated
		public List<String> getFileExtensions() {
			if (useRegisteredSuffixPatternMatch() && this.contentNegotiationManager != null) {
				return this.contentNegotiationManager.getAllFileExtensions();
			}
			return null;
		}

		/**
		 * 设置用于 ProducesRequestCondition 的 ContentNegotiationManager。
		 * <p>默认不设置。
		 */
		public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
			this.contentNegotiationManager = contentNegotiationManager;
		}

		/**
		 * 返回用于 ProducesRequestCondition 的 ContentNegotiationManager
		 * （如果有）。
		 */
		@Nullable
		public ContentNegotiationManager getContentNegotiationManager() {
			return this.contentNegotiationManager;
		}
	}

}
