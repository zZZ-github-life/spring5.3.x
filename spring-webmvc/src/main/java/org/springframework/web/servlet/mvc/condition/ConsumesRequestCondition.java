/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition.HeaderExpression;

/**
 * 逻辑析取（' || '）请求条件，用于将请求的
 * 'Content-Type' 头与一组媒体类型表达式进行匹配。支持两种
 * 媒体类型表达式，分别在
 * {@link RequestMapping#consumes()} 和 {@link RequestMapping#headers()} 中描述，
 * 其中头名称为 'Content-Type'。无论使用哪种语法，
 * 语义都相同。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	private static final ConsumesRequestCondition EMPTY_CONDITION = new ConsumesRequestCondition();


	private final List<ConsumeMediaTypeExpression> expressions;

	private boolean bodyRequired = true;


	/**
	 * 从 0 个或多个 "consumes" 表达式创建一个新实例。
	 * @param consumes 使用 {@link RequestMapping#consumes()} 中所述语法的表达式；
	 * 如果提供 0 个表达式，则该条件将匹配每个请求
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * 使用 "consumes" 和 "header" 表达式创建一个新实例。
	 * 头名称不是 'Content-Type' 或未定义头值的 "header" 表达式
	 * 将被忽略。如果总共提供了 0 个表达式，
	 * 则该条件将匹配所有请求
	 * @param consumes 如 {@link RequestMapping#consumes()} 中所述
	 * @param headers 如 {@link RequestMapping#headers()} 中所述
	 */
	public ConsumesRequestCondition(String[] consumes, @Nullable String[] headers) {
		this.expressions = parseExpressions(consumes, headers);
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
	}

	private static List<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, @Nullable String[] headers) {
		Set<ConsumeMediaTypeExpression> result = null;
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				HeaderExpression expr = new HeaderExpression(header);
				if ("Content-Type".equalsIgnoreCase(expr.name) && expr.value != null) {
					result = (result != null ? result : new LinkedHashSet<>());
					for (MediaType mediaType : MediaType.parseMediaTypes(expr.value)) {
						result.add(new ConsumeMediaTypeExpression(mediaType, expr.isNegated));
					}
				}
			}
		}
		if (!ObjectUtils.isEmpty(consumes)) {
			result = (result != null ? result : new LinkedHashSet<>());
			for (String consume : consumes) {
				result.add(new ConsumeMediaTypeExpression(consume));
			}
		}
		return (result != null ? new ArrayList<>(result) : Collections.emptyList());
	}

	/**
	 * 用于在创建匹配条件时内部使用的私有构造函数。
	 * 注意，表达式 List 既未排序也未深拷贝。
	 */
	private ConsumesRequestCondition(List<ConsumeMediaTypeExpression> expressions) {
		this.expressions = expressions;
	}


	/**
	 * 返回包含的 MediaType 表达式。
	 */
	public Set<MediaTypeExpression> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	/**
	 * 返回此条件的媒体类型，不包括否定表达式。
	 */
	public Set<MediaType> getConsumableMediaTypes() {
		Set<MediaType> result = new LinkedHashSet<>();
		for (ConsumeMediaTypeExpression expression : this.expressions) {
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
	protected Collection<ConsumeMediaTypeExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " || ";
	}

	/**
	 * 此条件是否应要求请求具有请求体。
	 * <p>默认情况下此设置被设为 {@code true}，此时假定
	 * 请求体是必需的，此条件会匹配 "Content-Type"
	 * 头，或回退到 "Content-Type: application/octet-stream"。
	 * <p>如果设为 {@code false}，且请求没有请求体，则此
	 * 条件会自动匹配，即无需检查表达式。
	 * @param bodyRequired 是否预期请求带有请求体
	 * @since 5.2
	 */
	public void setBodyRequired(boolean bodyRequired) {
		this.bodyRequired = bodyRequired;
	}

	/**
	 * 返回 {@link #setBodyRequired(boolean)} 的设置。
	 * @since 5.2
	 */
	public boolean isBodyRequired() {
		return this.bodyRequired;
	}


	/**
	 * 如果 "other" 实例有任何表达式则返回该实例；
	 * 否则返回 "this" 实例。实际上，这意味着方法级别的 "consumes"
	 * 会覆盖类型级别的 "consumes" 条件。
	 */
	@Override
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查所包含的任一媒体类型表达式是否匹配给定
	 * 请求的 'Content-Type' 头，并返回一个保证
	 * 只包含匹配表达式的实例。匹配通过
	 * {@link MediaType#includes(MediaType)} 执行。
	 * @param request 当前请求
	 * @return 如果条件不包含表达式，则为同一实例；或仅包含匹配表达式的新条件；
	 * 如果无表达式匹配，则为 {@code null}
	 */
	@Override
	@Nullable
	public ConsumesRequestCondition getMatchingCondition(HttpServletRequest request) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return EMPTY_CONDITION;
		}
		if (isEmpty()) {
			return this;
		}
		if (!hasBody(request) && !this.bodyRequired) {
			return EMPTY_CONDITION;
		}

		// Common media types are cached at the level of MimeTypeUtils

		MediaType contentType;
		try {
			contentType = StringUtils.hasLength(request.getContentType()) ?
					MediaType.parseMediaType(request.getContentType()) :
					MediaType.APPLICATION_OCTET_STREAM;
		}
		catch (InvalidMediaTypeException ex) {
			return null;
		}

		List<ConsumeMediaTypeExpression> result = getMatchingExpressions(contentType);
		return !CollectionUtils.isEmpty(result) ? new ConsumesRequestCondition(result) : null;
	}

	private boolean hasBody(HttpServletRequest request) {
		String contentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH);
		String transferEncoding = request.getHeader(HttpHeaders.TRANSFER_ENCODING);
		return StringUtils.hasText(transferEncoding) ||
				(StringUtils.hasText(contentLength) && !contentLength.trim().equals("0"));
	}

	@Nullable
	private List<ConsumeMediaTypeExpression> getMatchingExpressions(MediaType contentType) {
		List<ConsumeMediaTypeExpression> result = null;
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			if (expression.match(contentType)) {
				result = result != null ? result : new ArrayList<>();
				result.add(expression);
			}
		}
		return result;
	}

	/**
	 * 返回：
	 * <ul>
	 * <li>如果两个条件的表达式数量相同，则为 0
	 * <li>如果 "this" 拥有更多或更具体的媒体类型表达式，则小于 0
	 * <li>如果 "other" 拥有更多或更具体的媒体类型表达式，则大于 0
	 * </ul>
	 * <p>假定两个实例都是通过
	 * {@link #getMatchingCondition(HttpServletRequest)} 获得的，且每个实例仅包含
	 * 匹配的可消费媒体类型表达式，或者为空。
	 */
	@Override
	public int compareTo(ConsumesRequestCondition other, HttpServletRequest request) {
		if (this.expressions.isEmpty() && other.expressions.isEmpty()) {
			return 0;
		}
		else if (this.expressions.isEmpty()) {
			return 1;
		}
		else if (other.expressions.isEmpty()) {
			return -1;
		}
		else {
			return this.expressions.get(0).compareTo(other.expressions.get(0));
		}
	}


	/**
	 * 解析单个媒体类型表达式并将其与请求的 'Content-Type' 头进行匹配。
	 */
	static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		public final boolean match(MediaType contentType) {
			boolean match = getMediaType().includes(contentType);
			return !isNegated() == match;
		}
	}

}
