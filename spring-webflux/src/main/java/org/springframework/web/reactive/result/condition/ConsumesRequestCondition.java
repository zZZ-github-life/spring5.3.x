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

package org.springframework.web.reactive.result.condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * 逻辑析取（' || '）请求条件，用于将请求的 'Content-Type' 头与媒体类型表达式列表进行匹配。支持两种媒体类型表达式，分别在 {@link RequestMapping#consumes()} 和 {@link RequestMapping#headers()} 中描述，其中头名称为 'Content-Type'。无论使用哪种语法，语义都相同。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ConsumesRequestCondition extends AbstractRequestCondition<ConsumesRequestCondition> {

	private static final ConsumesRequestCondition EMPTY_CONDITION = new ConsumesRequestCondition();


	private final List<ConsumeMediaTypeExpression> expressions;

	private boolean bodyRequired = true;


	/**
	 * 根据 0 个或多个 "consumes" 表达式创建新实例。
	 * @param consumes 表达式，语法见 {@link RequestMapping#consumes()}；
	 * 如果提供的表达式为 0 个，
	 * 则该条件将匹配所有请求
	 */
	public ConsumesRequestCondition(String... consumes) {
		this(consumes, null);
	}

	/**
	 * 使用 "consumes" 和 "header" 表达式创建新实例。头名称不是 'Content-Type' 或未定义头值的 "header" 表达式将被忽略。如果总共提供 0 个表达式，则该条件将匹配所有请求。
	 * @param consumes 如 {@link RequestMapping#consumes()} 所述
	 * @param headers 如 {@link RequestMapping#headers()} 所述
	 */
	public ConsumesRequestCondition(String[] consumes, String[] headers) {
		this.expressions = parseExpressions(consumes, headers);
		if (this.expressions.size() > 1) {
			Collections.sort(this.expressions);
		}
	}

	private static List<ConsumeMediaTypeExpression> parseExpressions(String[] consumes, String[] headers) {
		Set<ConsumeMediaTypeExpression> result = null;
		if (!ObjectUtils.isEmpty(headers)) {
			for (String header : headers) {
				HeadersRequestCondition.HeaderExpression expr = new HeadersRequestCondition.HeaderExpression(header);
				if ("Content-Type".equalsIgnoreCase(expr.name)) {
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
	 * 内部私有构造函数，用于创建匹配条件。
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
	 * 此条件是否应要求请求包含正文。
	 * <p>默认设置为 {@code true}，此时假定需要请求正文，该条件匹配 "Content-Type" 头，或回退到 "Content-Type: application/octet-stream"。
	 * <p>如果设置为 {@code false} 且请求没有正文，则该条件自动匹配，即无需检查表达式。
	 * @param bodyRequired 是否期望请求具有正文
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
	 * 如果 "other" 实例包含任何表达式，则返回它；否则返回 "this" 实例。实际上这意味着方法级 "consumes" 会覆盖类型级 "consumes" 条件。
	 */
	@Override
	public ConsumesRequestCondition combine(ConsumesRequestCondition other) {
		return (!other.expressions.isEmpty() ? other : this);
	}

	/**
	 * 检查所包含的任何媒体类型表达式是否匹配给定请求的 'Content-Type' 头，并返回一个保证仅包含匹配表达式的实例。匹配通过 {@link MediaType#includes(MediaType)} 执行。
	 * @param exchange 当前交换
	 * @return 如果该条件不包含任何表达式，则返回同一实例；
	 * 否则返回仅包含匹配表达式的新条件；
	 * 如果没有表达式匹配，则返回 {@code null}。
	 */
	@Override
	public ConsumesRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		ServerHttpRequest request = exchange.getRequest();
		if (CorsUtils.isPreFlightRequest(request)) {
			return EMPTY_CONDITION;
		}
		if (isEmpty()) {
			return this;
		}
		if (!hasBody(request) && !this.bodyRequired) {
			return EMPTY_CONDITION;
		}
		List<ConsumeMediaTypeExpression> result = getMatchingExpressions(exchange);
		return !CollectionUtils.isEmpty(result) ? new ConsumesRequestCondition(result) : null;
	}

	private boolean hasBody(ServerHttpRequest request) {
		String contentLength = request.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
		String transferEncoding = request.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING);
		return StringUtils.hasText(transferEncoding) ||
				(StringUtils.hasText(contentLength) && !contentLength.trim().equals("0"));
	}

	@Nullable
	private List<ConsumeMediaTypeExpression> getMatchingExpressions(ServerWebExchange exchange) {
		List<ConsumeMediaTypeExpression> result = null;
		for (ConsumeMediaTypeExpression expression : this.expressions) {
			if (expression.match(exchange)) {
				result = result != null ? result : new ArrayList<>();
				result.add(expression);
			}
		}
		return result;
	}

	/**
	 * 返回值：
	 * <ul>
	 * <li>如果两个条件具有相同数量的表达式，则返回 0
	 * <li>如果 "this" 具有更多或更具体的媒体类型表达式，则小于 0
	 * <li>如果 "other" 具有更多或更具体的媒体类型表达式，则大于 0
	 * </ul>
	 * <p>假定两个实例均通过 {@link #getMatchingCondition(ServerWebExchange)} 获得，并且每个实例仅包含匹配的可消费媒体类型表达式，或者为空。
	 */
	@Override
	public int compareTo(ConsumesRequestCondition other, ServerWebExchange exchange) {
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
	 * 解析单个媒体类型表达式并将其与请求的 'Content-Type' 头匹配。
	 */
	static class ConsumeMediaTypeExpression extends AbstractMediaTypeExpression {

		ConsumeMediaTypeExpression(String expression) {
			super(expression);
		}

		ConsumeMediaTypeExpression(MediaType mediaType, boolean negated) {
			super(mediaType, negated);
		}

		@Override
		protected boolean matchMediaType(ServerWebExchange exchange) throws UnsupportedMediaTypeStatusException {
			try {
				MediaType contentType = exchange.getRequest().getHeaders().getContentType();
				contentType = (contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM);
				return getMediaType().includes(contentType);
			}
			catch (InvalidMediaTypeException ex) {
				throw new UnsupportedMediaTypeStatusException("Can't parse Content-Type [" +
						exchange.getRequest().getHeaders().getFirst("Content-Type") +
						"]: " + ex.getMessage());
			}
		}
	}

}
