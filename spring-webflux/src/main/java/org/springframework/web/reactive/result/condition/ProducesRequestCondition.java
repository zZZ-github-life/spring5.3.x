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

package org.springframework.web.reactive.result.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * 逻辑析取（' || '）请求条件，用于将请求的 'Accept' 头与媒体类型表达式列表进行匹配。支持两种媒体类型表达式，分别在 {@link RequestMapping#produces()} 和 {@link RequestMapping#headers()} 中描述，其中头名称为 'Accept'。无论使用哪种语法，语义都相同。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

	private static final RequestedContentTypeResolver DEFAULT_CONTENT_TYPE_RESOLVER =
			new RequestedContentTypeResolverBuilder().build();

	private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

	private static final String MEDIA_TYPES_ATTRIBUTE = ProducesRequestCondition.class.getName() + ".MEDIA_TYPES";


	private final List<ProduceMediaTypeExpression> mediaTypeAllList =
			Collections.singletonList(new ProduceMediaTypeExpression(MediaType.ALL_VALUE));

	private final List<ProduceMediaTypeExpression> expressions;

	private final RequestedContentTypeResolver contentTypeResolver;


	/**
	 * 根据 "produces" 表达式创建新实例。如果总共提供 0 个表达式，则该条件将匹配任何请求。
	 * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
	 */
	public ProducesRequestCondition(String... produces) {
		this(produces, null);
	}

	/**
	 * 使用 "produces" 和 "header" 表达式创建新实例。头名称不是 'Accept' 或未定义头值的 "header" 表达式将被忽略。如果总共提供 0 个表达式，则该条件将匹配任何请求。
	 * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
	 * @param headers expressions with syntax defined by {@link RequestMapping#headers()}
	 */
	public ProducesRequestCondition(String[] produces, String[] headers) {
		this(produces, headers, null);
	}

	/**
	 * 与 {@link #ProducesRequestCondition(String[], String[])} 相同，但还接受 {@link ContentNegotiationManager}。
	 * @param produces expressions with syntax defined by {@link RequestMapping#produces()}
	 * @param headers expressions with syntax defined by {@link RequestMapping#headers()}
	 * @param resolver used to determine requested content type
	 */
	public ProducesRequestCondition(String[] produces, String[] headers, RequestedContentTypeResolver resolver) {
		this.expressions = parseExpressions(produces, headers);
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
		this.contentTypeResolver = resolver != null ? resolver : DEFAULT_CONTENT_TYPE_RESOLVER;
	}

	private List<ProduceMediaTypeExpression> parseExpressions(String[] produces, String[] headers) {
		Set<ProduceMediaTypeExpression> result = null;
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				HeadersRequestCondition.HeaderExpression expr = new HeadersRequestCondition.HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name)) {
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result = (result != null ? result : new LinkedHashSet<>());
						result.add(new ProduceMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		if (!ObjectUtils.isEmpty(produces)) {
			for (String produce : produces) {
				result = (result != null ? result : new LinkedHashSet<>());
				result.add(new ProduceMediaTypeExpression(produce));
			}
		}
		return (result != null ? new ArrayList<>(result) : Collections.emptyList());
	}

	/**
	 * 内部私有构造函数，用于创建匹配条件。注意表达式列表既未排序，也未深拷贝。
	 */
	private ProducesRequestCondition(List<ProduceMediaTypeExpression> expressions, ProducesRequestCondition other) {
		this.expressions = expressions;
		this.contentTypeResolver = other.contentTypeResolver;
	}


	/**
	 * 返回包含的 "produces" 表达式。
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回包含的可生成媒体类型，不包括否定表达式。
	 */
	public Set<MediaType> getProducibleMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
		for (ProduceMediaTypeExpression expression : this.expressions) {
			if (!expression.isNegated()) {
				result.add(expression.getMediaType());
			}
		}
		return result;
	}

	/**
	 * 此条件是否包含任何媒体类型表达式。
	 */
	@Override
	public boolean isEmpty() {
		return this.expressions.isEmpty();
	}

	@Override
	protected List<ProduceMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 如果 "other" 实例包含任何表达式，则返回它；否则返回 "this" 实例。实际上这意味着方法级 "produces" 会覆盖类型级 "produces" 条件。
	 */
	@Override
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查所包含的任何媒体类型表达式是否匹配给定请求的 'Content-Type' 头，并返回一个保证仅包含匹配表达式的实例。匹配通过 {@link MediaType#isCompatibleWith(MediaType)} 执行。
	 * @param exchange the current exchange
	 * @return the same instance if there are no expressions;
	 * or a new condition with matching expressions;
	 * or {@code null} if no expressions match.
	 */
	@Override
	@Nullable
	public ProducesRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			return EMPTY_CONDITION;
		}
		if (isEmpty()) {
			return this;
		}
		List<ProduceMediaTypeExpression> result = getMatchingExpressions(exchange);
		if (!CollectionUtils.isEmpty(result)) {
			return new ProducesRequestCondition(result, this);
		}
		else {
			try {
				if (MediaType.ALL.isPresentIn(getAcceptedMediaTypes(exchange))) {
					return EMPTY_CONDITION;
				}
			}
			catch (NotAcceptableStatusException | UnsupportedMediaTypeStatusException ex) {
				// Ignore
			}
		}
		return null;
	}

	@Nullable
	private List<ProduceMediaTypeExpression> getMatchingExpressions(ServerWebExchange exchange) {
		List<ProduceMediaTypeExpression> result = null;
		for (ProduceMediaTypeExpression expression : this.expressions) {
			if (expression.match(exchange)) {
				result = result != null ? result : new ArrayList<>();
				result.add(expression);
			}
		}
		return result;
	}

	/**
	 * 按如下方式比较此条件与另一个 "produces" 条件：
	 * <ol>
	 * <li>通过 {@link MediaType#sortByQualityValue(List)} 按质量值对 'Accept' 头媒体类型排序，并遍历该列表。
	 * <li>在每个 "produces" 条件中获取匹配媒体类型的第一个索引，先使用 {@link MediaType#equals(Object)} 匹配，再使用 {@link MediaType#includes(MediaType)} 匹配。
	 * <li>如果找到更低的索引，则该索引处的条件获胜。
	 * <li>如果两个索引相等，则进一步使用 {@link MediaType#SPECIFICITY_COMPARATOR} 比较该索引处的媒体类型。
	 * </ol>
	 * <p>假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，并且每个实例仅包含匹配的可生成媒体类型表达式，或者为空。
	 */
	@Override
	public int compareTo(ProducesRequestCondition other, ServerWebExchange exchange) {
		if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		}
		try {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(exchange);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				int thisIndex = this.indexOfEqualMediaType(acceptedMediaType);
				int otherIndex = other.indexOfEqualMediaType(acceptedMediaType);
				int result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
				thisIndex = this.indexOfIncludedMediaType(acceptedMediaType);
				otherIndex = other.indexOfIncludedMediaType(acceptedMediaType);
				result = compareMatchingMediaTypes(this, thisIndex, other, otherIndex);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
		catch (NotAcceptableStatusException ex) {
			// should never happen
			throw new IllegalStateException("Cannot compare without having any requested media types", ex);
		}
	}

	private List<MediaType> getAcceptedMediaTypes(ServerWebExchange exchange) throws NotAcceptableStatusException {
		List<MediaType> result = exchange.getAttribute(MEDIA_TYPES_ATTRIBUTE);
		if (result == null) {
			result = this.contentTypeResolver.resolveMediaTypes(exchange);
			exchange.getAttributes().put(MEDIA_TYPES_ATTRIBUTE, result);
		}
		return result;
	}

	private int indexOfEqualMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			MediaType currentMediaType = getExpressionsToCompare().get(i).getMediaType();
			if (mediaType.getType().equalsIgnoreCase(currentMediaType.getType()) &&
					mediaType.getSubtype().equalsIgnoreCase(currentMediaType.getSubtype())) {
				return i;
			}
		}
		return -1;
	}

	private int indexOfIncludedMediaType(MediaType mediaType) {
		for (int i = 0; i < getExpressionsToCompare().size(); i++) {
			if (mediaType.includes(getExpressionsToCompare().get(i).getMediaType())) {
				return i;
			}
		}
		return -1;
	}

	private int compareMatchingMediaTypes(ProducesRequestCondition condition1, int index1,
			ProducesRequestCondition condition2, int index2) {

		int result = 0;
		if (index1 != index2) {
			result = index2 - index1;
		}
		else if (index1 != -1) {
			ProduceMediaTypeExpression expr1 = condition1.getExpressionsToCompare().get(index1);
			ProduceMediaTypeExpression expr2 = condition2.getExpressionsToCompare().get(index2);
			result = expr1.compareTo(expr2);
			result = (result != 0 ? result : expr1.getMediaType().compareTo(expr2.getMediaType()));
		}
		return result;
	}

	/**
	 * 返回包含的 "produces" 表达式；如果为空，则返回包含 {@value MediaType#ALL_VALUE} 表达式的列表。
	 */
	private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
		return (this.expressions.isEmpty() ? this.mediaTypeAllList  : this.expressions);
	}


	/**
	 * 用于清除包含已解析的请求媒体类型的 {@link #MEDIA_TYPES_ATTRIBUTE}。
	 * @param exchange the current exchange
	 * @since 5.2
	 */
	public static void clearMediaTypesAttribute(ServerWebExchange exchange) {
		exchange.getAttributes().remove(MEDIA_TYPES_ATTRIBUTE);
	}


	/**
	 * 解析单个媒体类型表达式并将其与请求的 'Accept' 头匹配。
	 */
	class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean matchMediaType(ServerWebExchange exchange) throws NotAcceptableStatusException {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(exchange);
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				if (getMediaType().isCompatibleWith(acceptedMediaType) && matchParameters(acceptedMediaType)) {
					return true;
				}
			}
			return false;
		}

		private boolean matchParameters(MediaType acceptedMediaType) {
			for (String name : getMediaType().getParameters().keySet()) {
				String s1 = getMediaType().getParameter(name);
				String s2 = acceptedMediaType.getParameter(name);
				if (StringUtils.hasText(s1) && StringUtils.hasText(s2) && !s1.equalsIgnoreCase(s2)) {
					return false;
				}
			}
			return true;
		}
	}

}
