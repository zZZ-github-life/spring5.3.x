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

package org.springframework.web.bind.support;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.server.ServerWebExchange;

/**
 * 特化的 {@link org.springframework.validation.DataBinder}，用于将 URL 查询参数
 * 或请求数据中的表单数据绑定到 Java 对象。
 *
 * <p><strong>警告</strong>：数据绑定可能会暴露不应被外部客户端访问或修改的
 * 对象图部分，从而带来安全问题。因此，数据绑定的设计和使用应仔细考虑安全性。
 * 更多详情请参阅参考手册中关于数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public class WebExchangeDataBinder extends WebDataBinder {

	/**
	 * 创建一个新实例，使用默认对象名称。
	 * @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebExchangeDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新实例。
	 * @param target 要绑定的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @param objectName 目标对象的名称
	 */
	public WebExchangeDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将查询参数、表单数据或多部分表单数据绑定到绑定器目标。
	 * @param exchange 当前交换
	 * @return 绑定完成时的 {@code Mono<Void>}
	 */
	public Mono<Void> bind(ServerWebExchange exchange) {
		return getValuesToBind(exchange)
				.doOnNext(values -> doBind(new MutablePropertyValues(values)))
				.then();
	}

	/**
	 * 获取用于数据绑定的值。默认情况下，这委托给
	 * {@link #extractValuesToBind(ServerWebExchange)}。
	 * @param exchange 当前交换
	 * @return 绑定值的 Map
	 * @since 5.3
	 */
	public Mono<Map<String, Object>> getValuesToBind(ServerWebExchange exchange) {
		return extractValuesToBind(exchange);
	}


	/**
	 * 将请求体中的多部分表单数据的查询参数和表单数据合并为一个
	 * {@code Map<String, Object>}，其中包含用于数据绑定的值。
	 * @param exchange 当前交换
	 * @return 包含要绑定值的 {@code Mono}
	 * @see org.springframework.http.server.reactive.ServerHttpRequest#getQueryParams()
	 * @see ServerWebExchange#getFormData()
	 * @see ServerWebExchange#getMultipartData()
	 */
	public static Mono<Map<String, Object>> extractValuesToBind(ServerWebExchange exchange) {
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
		Mono<MultiValueMap<String, String>> formData = exchange.getFormData();
		Mono<MultiValueMap<String, Part>> multipartData = exchange.getMultipartData();

		return Mono.zip(Mono.just(queryParams), formData, multipartData)
				.map(tuple -> {
					Map<String, Object> result = new TreeMap<>();
					tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));
					tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
					tuple.getT3().forEach((key, values) -> addBindValue(result, key, values));
					return result;
				});
	}

	protected static void addBindValue(Map<String, Object> params, String key, List<?> values) {
		if (!CollectionUtils.isEmpty(values)) {
			values = values.stream()
					.map(value -> value instanceof FormFieldPart ? ((FormFieldPart) value).value() : value)
					.collect(Collectors.toList());
			params.put(key, values.size() == 1 ? values.get(0) : values);
		}
	}

}
