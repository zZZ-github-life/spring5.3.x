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

package org.springframework.web.servlet.mvc.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * 逻辑析取（' || '）请求条件，用于将请求的 'Accept' 头
 * 与一组媒体类型表达式进行匹配。支持两种媒体类型表达式，
 * 分别在 {@link RequestMapping#produces()} 和
 * {@link RequestMapping#headers()} 中描述（其中头名称为 'Accept'）。
 * 无论使用哪种语法，语义都相同。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ProducesRequestCondition extends AbstractRequestCondition<ProducesRequestCondition> {

	private static final ContentNegotiationManager DEFAULT_CONTENT_NEGOTIATION_MANAGER =
			new ContentNegotiationManager();

	private static final ProducesRequestCondition EMPTY_CONDITION = new ProducesRequestCondition();

	private static final List<ProduceMediaTypeExpression> MEDIA_TYPE_ALL_LIST =
			Collections.singletonList(new ProduceMediaTypeExpression(MediaType.ALL_VALUE));

	private static final String MEDIA_TYPES_ATTRIBUTE = ProducesRequestCondition.class.getName() + ".MEDIA_TYPES";


	private final List<ProduceMediaTypeExpression> expressions;

	private final ContentNegotiationManager contentNegotiationManager;


	/**
	 * 根据 "produces" 表达式创建一个新实例。如果总共提供了 0 个表达式，
	 * 此条件将匹配任何请求。
	 * @param produces 使用 {@link RequestMapping#produces()} 所定义语法的表达式
	 */
	public ProducesRequestCondition(String... produces) {
		this(produces, null, null);
	}

	/**
	 * 使用 "produces" 和 "header" 表达式创建一个新实例。头名称不是
	 * 'Accept' 或未定义头值的 "header" 表达式将被忽略。
	 * 如果总共提供了 0 个表达式，此条件将匹配任何请求。
	 * @param produces 使用 {@link RequestMapping#produces()} 所定义语法的表达式
	 * @param headers 使用 {@link RequestMapping#headers()} 所定义语法的表达式
	 */
	public ProducesRequestCondition(String[] produces, @Nullable String[] headers) {
		this(produces, headers, null);
	}

	/**
	 * 与 {@link #ProducesRequestCondition(String[], String[])} 相同，但还可以
	 * 接受一个 {@link ContentNegotiationManager}。
	 * @param produces 使用 {@link RequestMapping#produces()} 所定义语法的表达式
	 * @param headers 使用 {@link RequestMapping#headers()} 所定义语法的表达式
	 * @param manager 用于确定请求的媒体类型
	 */
	public ProducesRequestCondition(String[] produces, @Nullable String[] headers,
			@Nullable ContentNegotiationManager manager) {

		this.expressions = parseExpressions(produces, headers);
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
		this.contentNegotiationManager = manager != null ? manager : DEFAULT_CONTENT_NEGOTIATION_MANAGER;
	}

	private List<ProduceMediaTypeExpression> parseExpressions(String[] produces, @Nullable String[] headers) {
		Set<ProduceMediaTypeExpression> result = null;
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Accept".equalsIgnoreCase(expr.name) && expr.value != null) {
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
	 * 用于内部创建匹配条件的私有构造函数。
	 * 注意，表达式 List 既未排序也未深拷贝。
	 */
	private ProducesRequestCondition(List<ProduceMediaTypeExpression> expressions, ProducesRequestCondition other) {
		this.expressions = expressions;
		this.contentNegotiationManager = other.contentNegotiationManager;
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
	 * 如果 "other" 实例有任何表达式则返回该实例；
	 * 否则返回 "this" 实例。实际上，这意味着方法级别的 "produces"
	 * 会覆盖类型级别的 "produces" 条件。
	 */
	@Override
	public ProducesRequestCondition combine(ProducesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查所包含的任一媒体类型表达式是否匹配给定
	 * 请求的 'Content-Type' 头，并返回一个保证
	 * 只包含匹配表达式的实例。匹配通过
	 * {@link MediaType#isCompatibleWith(MediaType)} 执行。
	 * @param request 当前请求
	 * @return 如果没有任何表达式，则为同一实例；或包含匹配表达式的新条件；
	 * 如果无表达式匹配，则为 {@code null}。
	 */
	@Override
	@Nullable
	public ProducesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return EMPTY_CONDITION;
		}
		if (isEmpty()) {
			return this;
		}
		List<MediaType> acceptedMediaTypes;
		try {
			acceptedMediaTypes = getAcceptedMediaTypes(request);
		}
		catch (HttpMediaTypeException ex) {
			return null;
		}
		List<ProduceMediaTypeExpression> result = getMatchingExpressions(acceptedMediaTypes);
		if (!CollectionUtils.isEmpty(result)) {
			return new ProducesRequestCondition(result, this);
		}
		else if (MediaType.ALL.isPresentIn(acceptedMediaTypes)) {
			return EMPTY_CONDITION;
		}
		else {
			return null;
		}
	}

	@Nullable
	private List<ProduceMediaTypeExpression> getMatchingExpressions(List<MediaType> acceptedMediaTypes) {
		List<ProduceMediaTypeExpression> result = null;
		for (ProduceMediaTypeExpression expression : this.expressions) {
			if (expression.match(acceptedMediaTypes)) {
				result = result != null ? result : new ArrayList<>();
				result.add(expression);
			}
		}
		return result;
	}

	/**
	 * 按如下方式比较此条件与另一个 "produces" 条件：
	 * <ol>
	 * <li>通过 {@link MediaType#sortByQualityValue(List)} 按质量值对 'Accept' 头
	 * 中的媒体类型进行排序，并遍历该列表。
	 * <li>获取每个 "produces" 条件中匹配媒体类型的第一个索引，
	 * 先使用 {@link MediaType#equals(Object)} 匹配，
	 * 然后使用 {@link MediaType#includes(MediaType)} 匹配。
	 * <li>如果找到的索引更小，则该索引处的条件胜出。
	 * <li>如果两个索引相同，则使用 {@link MediaType#SPECIFICITY_COMPARATOR}
	 * 进一步比较该索引处的媒体类型。
	 * </ol>
	 * <p>假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，且每个实例
	 * 仅包含匹配的可生成媒体类型表达式，或者为空。
	 */
	@Override
	public int compareTo(ProducesRequestCondition other, HttpServletRequest request) {
		if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		}
		try {
			List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(request);
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
		catch (HttpMediaTypeNotAcceptableException ex) {
			// should never happen
			throw new IllegalStateException("Cannot compare without having any requested media types", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private List<MediaType> getAcceptedMediaTypes(HttpServletRequest request)
			throws HttpMediaTypeNotAcceptableException {

		List<MediaType> result = (List<MediaType>) request.getAttribute(MEDIA_TYPES_ATTRIBUTE);
		if (result == null) {
			result = this.contentNegotiationManager.resolveMediaTypes(new ServletWebRequest(request));
			request.setAttribute(MEDIA_TYPES_ATTRIBUTE, result);
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
			result = (result != 0) ? result : expr1.getMediaType().compareTo(expr2.getMediaType());
		}
		return result;
	}

	/**
	 * 返回包含的 "produces" 表达式；如果为空，则返回一个
	 * 包含 {@value MediaType#ALL_VALUE} 表达式的列表。
	 */
	private List<ProduceMediaTypeExpression> getExpressionsToCompare() {
		return (this.expressions.isEmpty() ? MEDIA_TYPE_ALL_LIST : this.expressions);
	}


	/**
	 * 使用此项清除包含已解析的请求媒体类型的
	 * {@link #MEDIA_TYPES_ATTRIBUTE}。
	 * @param request 当前请求
	 * @since 5.2
	 */
	public static void clearMediaTypesAttribute(HttpServletRequest request) {
		request.removeAttribute(MEDIA_TYPES_ATTRIBUTE);
	}


	/**
	 * 解析单个媒体类型表达式并将其与请求的 'Accept' 头进行匹配。
	 */
	static class ProduceMediaTypeExpression extends AbstractMediaTypeExpression {

		ProduceMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		ProduceMediaTypeExpression(String expression) {
			super(expression);
		}

		public final boolean match(List<MediaType> acceptedMediaTypes) {
			boolean match = matchMediaType(acceptedMediaTypes);
			return !isNegated() == match;
		}

		private boolean matchMediaType(List<MediaType> acceptedMediaTypes) {
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
