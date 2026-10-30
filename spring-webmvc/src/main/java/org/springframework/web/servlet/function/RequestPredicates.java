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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * {@link RequestPredicate} 的实现，提供各种有用的请求匹配操作，
 * 例如基于路径、HTTP 方法等的匹配。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public abstract class RequestPredicates {

	private static final Log logger = LogFactory.getLog(RequestPredicates.class);

	/**
	 * 返回始终匹配的 {@code RequestPredicate}。
	 * @return 始终匹配的谓词
	 */
	public static RequestPredicate all() {
		return request -> true;
	}

	/**
	 * 返回一个 {@code RequestPredicate}，当请求的 HTTP 方法等于给定方法时匹配。
	 * @param httpMethod 要匹配的 HTTP 方法
	 * @return 针对给定 HTTP 方法进行测试的谓词
	 */
	public static RequestPredicate method(HttpMethod httpMethod) {
		return new HttpMethodPredicate(httpMethod);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，当请求的 HTTP 方法等于给定方法之一时匹配。
	 * @param httpMethods 要匹配的 HTTP 方法
	 * @return 针对给定 HTTP 方法进行测试的谓词
	 */
	public static RequestPredicate methods(HttpMethod... httpMethods) {
		return new HttpMethodPredicate(httpMethods);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，将请求路径与给定的路径模式进行测试。
	 * @param pattern 要匹配的模式
	 * @return 针对给定路径模式进行测试的谓词
	 */
	public static RequestPredicate path(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null");
		if (!pattern.isEmpty() && !pattern.startsWith("/")) {
			pattern = "/" + pattern;
		}
		return pathPredicates(PathPatternParser.defaultInstance).apply(pattern);
	}

	/**
	 * 返回一个函数，它使用给定的 {@link PathPatternParser} 从模式字符串创建新的
	 * 路径匹配 {@code RequestPredicates}。
	 * <p>解析路径模式时，此方法可用于指定非默认的自定义
	 * {@code PathPatternParser}。
	 * @param patternParser 用于解析传给所返回函数的模式的解析器
	 * @return 将模式 String 解析为路径匹配的
	 * {@code RequestPredicates} 实例的函数
	 */
	public static Function<String, RequestPredicate> pathPredicates(PathPatternParser patternParser) {
		Assert.notNull(patternParser, "PathPatternParser must not be null");
		return pattern -> new PathPatternPredicate(patternParser.parse(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，将请求的标头与给定的标头谓词进行测试。
	 * @param headersPredicate 针对请求头进行测试的谓词
	 * @return 针对给定请求头谓词进行测试的谓词
	 */
	public static RequestPredicate headers(Predicate<ServerRequest.Headers> headersPredicate) {
		return new HeadersPredicate(headersPredicate);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，测试请求的
	 * {@linkplain ServerRequest.Headers#contentType() 内容类型}是否被任何给定媒体类型
	 * {@linkplain MediaType#includes(MediaType) 包含}。
	 * @param mediaTypes 用于匹配请求内容类型的媒体类型
	 * @return 用给定媒体类型测试请求内容类型的谓词
	 */
	public static RequestPredicate contentType(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		return new ContentTypePredicate(mediaTypes);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，测试请求的
	 * {@linkplain ServerRequest.Headers#accept() accept} 标头是否与任何给定媒体类型
	 * {@linkplain MediaType#isCompatibleWith(MediaType) 兼容}。
	 * @param mediaTypes 用于匹配请求 Accept 头的媒体类型
	 * @return 用给定媒体类型测试请求 Accept 头的谓词
	 */
	public static RequestPredicate accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		return new AcceptPredicate(mediaTypes);
	}

	/**
	 * 返回一个 {@code RequestPredicate}：当请求的 HTTP 方法为 {@code GET}
	 * 且给定的 {@code pattern} 与请求路径匹配时匹配。
	 * @param pattern 要匹配的路径模式
	 * @return 当请求方法为 GET 且给定模式
	 * 匹配请求路径时匹配的谓词
	 */
	public static RequestPredicate GET(String pattern) {
		return method(HttpMethod.GET).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}：当请求的 HTTP 方法为 {@code HEAD}
	 * 且给定的 {@code pattern} 与请求路径匹配时匹配。
	 * @param pattern 要匹配的路径模式
	 * @return 当请求方法为 HEAD 且给定模式
	 * 匹配请求路径时匹配的谓词
	 */
	public static RequestPredicate HEAD(String pattern) {
		return method(HttpMethod.HEAD).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}：当请求的 HTTP 方法为 {@code POST}
	 * 且给定的 {@code pattern} 与请求路径匹配时匹配。
	 * @param pattern 要匹配的路径模式
	 * @return 当请求方法为 POST 且给定模式
	 * 匹配请求路径时匹配的谓词
	 */
	public static RequestPredicate POST(String pattern) {
		return method(HttpMethod.POST).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}：当请求的 HTTP 方法为 {@code PUT}
	 * 且给定的 {@code pattern} 与请求路径匹配时匹配。
	 * @param pattern 要匹配的路径模式
	 * @return 当请求方法为 PUT 且给定模式
	 * 匹配请求路径时匹配的谓词
	 */
	public static RequestPredicate PUT(String pattern) {
		return method(HttpMethod.PUT).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}：当请求的 HTTP 方法为 {@code PATCH}
	 * 且给定的 {@code pattern} 与请求路径匹配时匹配。
	 * @param pattern 要匹配的路径模式
	 * @return 当请求方法为 PATCH 且给定模式
	 * 匹配请求路径时匹配的谓词
	 */
	public static RequestPredicate PATCH(String pattern) {
		return method(HttpMethod.PATCH).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}：当请求的 HTTP 方法为 {@code DELETE}
	 * 且给定的 {@code pattern} 与请求路径匹配时匹配。
	 * @param pattern 要匹配的路径模式
	 * @return 当请求方法为 DELETE 且给定模式
	 * 匹配请求路径时匹配的谓词
	 */
	public static RequestPredicate DELETE(String pattern) {
		return method(HttpMethod.DELETE).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}：当请求的 HTTP 方法为 {@code OPTIONS}
	 * 且给定的 {@code pattern} 与请求路径匹配时匹配。
	 * @param pattern 要匹配的路径模式
	 * @return 当请求方法为 OPTIONS 且给定模式
	 * 匹配请求路径时匹配的谓词
	 */
	public static RequestPredicate OPTIONS(String pattern) {
		return method(HttpMethod.OPTIONS).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，当请求路径具有给定扩展名时匹配。
	 * @param extension 要匹配的路径扩展名（忽略大小写）
	 * @return 当请求路径具有给定文件扩展名时匹配的谓词
	 */
	public static RequestPredicate pathExtension(String extension) {
		Assert.notNull(extension, "'extension' must not be null");
		return new PathExtensionPredicate(extension);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，当请求路径与给定谓词匹配时匹配。
	 * @param extensionPredicate 用于测试请求路径扩展名的谓词
	 * @return 当给定谓词匹配请求路径的文件扩展名时
	 * 匹配的谓词
	 */
	public static RequestPredicate pathExtension(Predicate<String> extensionPredicate) {
		return new PathExtensionPredicate(extensionPredicate);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，当请求中给定名称的参数具有给定值时匹配。
	 * @param name 要测试的参数名称
	 * @param value 要测试的参数值
	 * @return 当参数具有给定值时匹配的谓词
	 * @see ServerRequest#param(String)
	 */
	public static RequestPredicate param(String name, String value) {
		return new ParamPredicate(name, value);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，将请求中给定名称的参数与给定谓词进行测试。
	 * @param name 要测试的参数名称
	 * @param predicate 用于测试参数值的谓词
	 * @return 用给定谓词匹配指定名称参数的谓词
	 * @see ServerRequest#param(String)
	 */
	public static RequestPredicate param(String name, Predicate<String> predicate) {
		return new ParamPredicate(name, predicate);
	}


	private static void traceMatch(String prefix, Object desired, @Nullable Object actual, boolean match) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s \"%s\" %s against value \"%s\"",
					prefix, desired, match ? "matches" : "does not match", actual));
		}
	}

	private static void restoreAttributes(ServerRequest request, Map<String, Object> attributes) {
		request.attributes().clear();
		request.attributes().putAll(attributes);
	}

	private static Map<String, String> mergePathVariables(Map<String, String> oldVariables,
			Map<String, String> newVariables) {

		if (!newVariables.isEmpty()) {
			Map<String, String> mergedVariables = new LinkedHashMap<>(oldVariables);
			mergedVariables.putAll(newVariables);
			return mergedVariables;
		}
		else {
			return oldVariables;
		}
	}

	private static PathPattern mergePatterns(@Nullable PathPattern oldPattern, PathPattern newPattern) {
		if (oldPattern != null) {
			return oldPattern.combine(newPattern);
		}
		else {
			return newPattern;
		}

	}


	/**
	 * 接收来自请求谓词逻辑结构的通知。
	 */
	public interface Visitor {

		/**
		 * 接收 HTTP 方法谓词的通知。
		 * @param methods 构成该谓词的 HTTP 方法
		 * @see RequestPredicates#method(HttpMethod)
		 */
		void method(Set<HttpMethod> methods);

		/**
		 * 接收路径谓词的通知。
		 * @param pattern 构成该谓词的路径模式
		 * @see RequestPredicates#path(String)
		 */
		void path(String pattern);

		/**
		 * 接收路径扩展名谓词的通知。
		 * @param extension 构成该谓词的路径扩展名
		 * @see RequestPredicates#pathExtension(String)
		 */
		void pathExtension(String extension);

		/**
		 * 接收 HTTP 标头谓词的通知。
		 * @param name 要检查的 HTTP 头名称
		 * @param value HTTP 头的期望值
		 * @see RequestPredicates#headers(Predicate)
		 * @see RequestPredicates#contentType(MediaType...)
		 * @see RequestPredicates#accept(MediaType...)
		 */
		void header(String name, String value);

		/**
		 * 接收参数谓词的通知。
		 * @param name 参数名称
		 * @param value 参数的期望值
		 * @see RequestPredicates#param(String, String)
		 */
		void param(String name, String value);

		/**
		 * 接收逻辑 AND 谓词的第一个通知。
		 * 接下来的第一个通知将包含 AND 谓词的左侧，
		 * 随后是 {@link #and()}，然后是右侧，最后是 {@link #endAnd()}。
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void startAnd();

		/**
		 * 接收逻辑 AND 谓词的"中间"通知。
		 * 接下来的通知包含右侧，随后是 {@link #endAnd()}。
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void and();

		/**
		 * 接收逻辑 AND 谓词的最后一个通知。
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void endAnd();

		/**
		 * 接收逻辑 OR 谓词的第一个通知。
		 * 接下来的第一个通知将包含 OR 谓词的左侧；
		 * 第二个通知包含右侧，随后是 {@link #endOr()}。
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void startOr();

		/**
		 * 接收逻辑 OR 谓词的"中间"通知。
		 * 接下来的通知包含右侧，随后是 {@link #endOr()}。
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void or();

		/**
		 * 接收逻辑 OR 谓词的最后一个通知。
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void endOr();

		/**
		 * 接收被否定谓词的第一个通知。
		 * 接下来的第一个通知将包含被否定的谓词，随后
		 * 是 {@link #endNegate()}。
		 * @see RequestPredicate#negate()
		 */
		void startNegate();

		/**
		 * 接收被否定谓词的最后一个通知。
		 * @see RequestPredicate#negate()
		 */
		void endNegate();

		/**
		 * 接收未知谓词的第一个通知。
		 */
		void unknown(RequestPredicate predicate);
	}


	private static class HttpMethodPredicate implements RequestPredicate {

		private final Set<HttpMethod> httpMethods;

		public HttpMethodPredicate(HttpMethod httpMethod) {
			Assert.notNull(httpMethod, "HttpMethod must not be null");
			this.httpMethods = EnumSet.of(httpMethod);
		}

		public HttpMethodPredicate(HttpMethod... httpMethods) {
			Assert.notEmpty(httpMethods, "HttpMethods must not be empty");
			this.httpMethods = EnumSet.copyOf(Arrays.asList(httpMethods));
		}

		@Override
		public boolean test(ServerRequest request) {
			HttpMethod method = method(request);
			boolean match = this.httpMethods.contains(method);
			traceMatch("Method", this.httpMethods, method, match);
			return match;
		}

		@Nullable
		private static HttpMethod method(ServerRequest request) {
			if (CorsUtils.isPreFlightRequest(request.servletRequest())) {
				String accessControlRequestMethod =
						request.headers().firstHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
				return HttpMethod.resolve(accessControlRequestMethod);
			}
			else {
				return request.method();
			}
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.method(Collections.unmodifiableSet(this.httpMethods));
		}

		@Override
		public String toString() {
			if (this.httpMethods.size() == 1) {
				return this.httpMethods.iterator().next().toString();
			}
			else {
				return this.httpMethods.toString();
			}
		}
	}


	private static class PathPatternPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		private PathPattern pattern;

		public PathPatternPredicate(PathPattern pattern) {
			Assert.notNull(pattern, "'pattern' must not be null");
			this.pattern = pattern;
		}

		@Override
		public boolean test(ServerRequest request) {
			PathContainer pathContainer = request.requestPath().pathWithinApplication();
			PathPattern.PathMatchInfo info = this.pattern.matchAndExtract(pathContainer);
			traceMatch("Pattern", this.pattern.getPatternString(), request.path(), info != null);
			if (info != null) {
				mergeAttributes(request, info.getUriVariables(), this.pattern);
				return true;
			}
			else {
				return false;
			}
		}

		private static void mergeAttributes(ServerRequest request, Map<String, String> variables,
				PathPattern pattern) {
			Map<String, String> pathVariables = mergePathVariables(request.pathVariables(), variables);
			request.attributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
						Collections.unmodifiableMap(pathVariables));

			pattern = mergePatterns(
					(PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					pattern);
			request.attributes().put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return Optional.ofNullable(this.pattern.matchStartOfPath(request.requestPath().pathWithinApplication()))
					.map(info -> new SubPathServerRequestWrapper(request, info, this.pattern));
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.path(this.pattern.getPatternString());
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			String patternString = this.pattern.getPatternString();
			this.pattern = parser.parse(patternString);
		}

		@Override
		public String toString() {
			return this.pattern.getPatternString();
		}

	}


	private static class HeadersPredicate implements RequestPredicate {

		private final Predicate<ServerRequest.Headers> headersPredicate;

		public HeadersPredicate(Predicate<ServerRequest.Headers> headersPredicate) {
			Assert.notNull(headersPredicate, "Predicate must not be null");
			this.headersPredicate = headersPredicate;
		}

		@Override
		public boolean test(ServerRequest request) {
			if (CorsUtils.isPreFlightRequest(request.servletRequest())) {
				return true;
			}
			else {
				return this.headersPredicate.test(request.headers());
			}
		}

		@Override
		public String toString() {
			return this.headersPredicate.toString();
		}
	}

	private static class ContentTypePredicate extends HeadersPredicate {

		private final Set<MediaType> mediaTypes;

		public ContentTypePredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		private ContentTypePredicate(Set<MediaType> mediaTypes) {
			super(headers -> {
				MediaType contentType =
						headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
				boolean match = mediaTypes.stream()
						.anyMatch(mediaType -> mediaType.includes(contentType));
				traceMatch("Content-Type", mediaTypes, contentType, match);
				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.CONTENT_TYPE,
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}

		@Override
		public String toString() {
			return String.format("Content-Type: %s",
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}
	}

	private static class AcceptPredicate extends HeadersPredicate {

		private final Set<MediaType> mediaTypes;

		public AcceptPredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		private AcceptPredicate(Set<MediaType> mediaTypes) {
			super(headers -> {
				List<MediaType> acceptedMediaTypes = acceptedMediaTypes(headers);
				boolean match = acceptedMediaTypes.stream()
						.anyMatch(acceptedMediaType -> mediaTypes.stream()
								.anyMatch(acceptedMediaType::isCompatibleWith));
				traceMatch("Accept", mediaTypes, acceptedMediaTypes, match);
				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		@NonNull
		private static List<MediaType> acceptedMediaTypes(ServerRequest.Headers headers) {
			List<MediaType> acceptedMediaTypes = headers.accept();
			if (acceptedMediaTypes.isEmpty()) {
				acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
			}
			else {
				MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
			}
			return acceptedMediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.ACCEPT,
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}

		@Override
		public String toString() {
			return String.format("Accept: %s",
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}
	}


	private static class PathExtensionPredicate implements RequestPredicate {

		private final Predicate<String> extensionPredicate;

		@Nullable
		private final String extension;

		public PathExtensionPredicate(Predicate<String> extensionPredicate) {
			Assert.notNull(extensionPredicate, "Predicate must not be null");
			this.extensionPredicate = extensionPredicate;
			this.extension = null;
		}

		public PathExtensionPredicate(String extension) {
			Assert.notNull(extension, "Extension must not be null");

			this.extensionPredicate = s -> {
				boolean match = extension.equalsIgnoreCase(s);
				traceMatch("Extension", extension, s, match);
				return match;
			};
			this.extension = extension;
		}

		@Override
		public boolean test(ServerRequest request) {
			String pathExtension = UriUtils.extractFileExtension(request.path());
			return this.extensionPredicate.test(pathExtension);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.pathExtension(
					(this.extension != null) ?
							this.extension :
							this.extensionPredicate.toString());
		}

		@Override
		public String toString() {
			return String.format("*.%s",
					(this.extension != null) ?
							this.extension :
							this.extensionPredicate);
		}

	}


	private static class ParamPredicate implements RequestPredicate {

		private final String name;

		private final Predicate<String> valuePredicate;

		@Nullable
		private final String value;

		public ParamPredicate(String name, Predicate<String> valuePredicate) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(valuePredicate, "Predicate must not be null");
			this.name = name;
			this.valuePredicate = valuePredicate;
			this.value = null;
		}

		public ParamPredicate(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(value, "Value must not be null");
			this.name = name;
			this.valuePredicate = value::equals;
			this.value = value;
		}

		@Override
		public boolean test(ServerRequest request) {
			Optional<String> s = request.param(this.name);
			return s.filter(this.valuePredicate).isPresent();
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.param(this.name,
					(this.value != null) ?
							this.value :
							this.valuePredicate.toString());
		}

		@Override
		public String toString() {
			return String.format("?%s %s", this.name,
					(this.value != null) ?
							this.value :
							this.valuePredicate);
		}
	}


	/**
	 * {@link RequestPredicate}，要求 {@code left} 和 {@code right} 谓词都匹配。
	 */
	static class AndRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		private final RequestPredicate left;

		private final RequestPredicate right;

		public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean test(ServerRequest request) {
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			if (this.left.test(request) && this.right.test(request)) {
				return true;
			}
			restoreAttributes(request, oldAttributes);
			return false;
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return this.left.nest(request).flatMap(this.right::nest);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.startAnd();
			this.left.accept(visitor);
			visitor.and();
			this.right.accept(visitor);
			visitor.endAnd();
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			if (this.left instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.left).changeParser(parser);
			}
			if (this.right instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.right).changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s && %s)", this.left, this.right);
		}
	}

	/**
	 * 对委托谓词取反的 {@link RequestPredicate}。
	 */
	static class NegateRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		private final RequestPredicate delegate;

		public NegateRequestPredicate(RequestPredicate delegate) {
			Assert.notNull(delegate, "Delegate must not be null");
			this.delegate = delegate;
		}

		@Override
		public boolean test(ServerRequest request) {
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());
			boolean result = !this.delegate.test(request);
			if (!result) {
				restoreAttributes(request, oldAttributes);
			}
			return result;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.startNegate();
			this.delegate.accept(visitor);
			visitor.endNegate();
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			if (this.delegate instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.delegate).changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return "!" + this.delegate.toString();
		}
	}

	/**
	 * {@link RequestPredicate}，其中 {@code left} 或 {@code right} 谓词可以匹配。
	 */
	static class OrRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		private final RequestPredicate left;

		private final RequestPredicate right;

		public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean test(ServerRequest request) {
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			if (this.left.test(request)) {
				return true;
			}
			else {
				restoreAttributes(request, oldAttributes);
				if (this.right.test(request)) {
					return true;
				}
			}
			restoreAttributes(request, oldAttributes);
			return false;
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			Optional<ServerRequest> leftResult = this.left.nest(request);
			if (leftResult.isPresent()) {
				return leftResult;
			}
			else {
				return this.right.nest(request);
			}
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.startOr();
			this.left.accept(visitor);
			visitor.or();
			this.right.accept(visitor);
			visitor.endOr();
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			if (this.left instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.left).changeParser(parser);
			}
			if (this.right instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.right).changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s || %s)", this.left, this.right);
		}
	}


	private static class SubPathServerRequestWrapper implements ServerRequest {

		private final ServerRequest request;

		private RequestPath requestPath;

		private final Map<String, Object> attributes;

		public SubPathServerRequestWrapper(ServerRequest request,
				PathPattern.PathRemainingMatchInfo info, PathPattern pattern) {
			this.request = request;
			this.requestPath = requestPath(request.requestPath(), info);
			this.attributes = mergeAttributes(request, info.getUriVariables(), pattern);
		}

		private static RequestPath requestPath(RequestPath original, PathPattern.PathRemainingMatchInfo info) {
			StringBuilder contextPath = new StringBuilder(original.contextPath().value());
			contextPath.append(info.getPathMatched().value());
			int length = contextPath.length();
			if (length > 0 && contextPath.charAt(length - 1) == '/') {
				contextPath.setLength(length - 1);
			}
			return original.modifyContextPath(contextPath.toString());
		}

		private static Map<String, Object> mergeAttributes(ServerRequest request,
		Map<String, String> pathVariables, PathPattern pattern) {
			Map<String, Object> result = new ConcurrentHashMap<>(request.attributes());

			result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
					mergePathVariables(request.pathVariables(), pathVariables));

			pattern = mergePatterns(
					(PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					pattern);
			result.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
			return result;
		}

		@Override
		public HttpMethod method() {
			return this.request.method();
		}

		@Override
		public String methodName() {
			return this.request.methodName();
		}

		@Override
		public URI uri() {
			return this.request.uri();
		}

		@Override
		public UriBuilder uriBuilder() {
			return this.request.uriBuilder();
		}

		@Override
		public RequestPath requestPath() {
			return this.requestPath;
		}

		@Override
		public Headers headers() {
			return this.request.headers();
		}

		@Override
		public MultiValueMap<String, Cookie> cookies() {
			return this.request.cookies();
		}

		@Override
		public Optional<InetSocketAddress> remoteAddress() {
			return this.request.remoteAddress();
		}

		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return this.request.messageConverters();
		}

		@Override
		public <T> T body(Class<T> bodyType) throws ServletException, IOException {
			return this.request.body(bodyType);
		}

		@Override
		public <T> T body(ParameterizedTypeReference<T> bodyType)
				throws ServletException, IOException {
			return this.request.body(bodyType);
		}

		@Override
		public Optional<Object> attribute(String name) {
			return this.request.attribute(name);
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public Optional<String> param(String name) {
			return this.request.param(name);
		}

		@Override
		public MultiValueMap<String, String> params() {
			return this.request.params();
		}

		@Override
		public MultiValueMap<String, Part> multipartData() throws IOException, ServletException {
			return this.request.multipartData();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Map<String, String> pathVariables() {
			return (Map<String, String>) this.attributes.getOrDefault(
					RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
		}

		@Override
		public HttpSession session() {
			return this.request.session();
		}



		@Override
		public Optional<Principal> principal() {
			return this.request.principal();
		}

		@Override
		public HttpServletRequest servletRequest() {
			return this.request.servletRequest();
		}

		@Override
		public Optional<ServerResponse> checkNotModified(Instant lastModified) {
			return this.request.checkNotModified(lastModified);
		}

		@Override
		public Optional<ServerResponse> checkNotModified(String etag) {
			return this.request.checkNotModified(etag);
		}

		@Override
		public Optional<ServerResponse> checkNotModified(Instant lastModified, String etag) {
			return this.request.checkNotModified(lastModified, etag);
		}

		@Override
		public String toString() {
			return method() + " " +  path();
		}

	}

}
