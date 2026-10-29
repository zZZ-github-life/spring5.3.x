/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.util.Assert;

/**
 * 表示路由到{@linkplain HandlerFunction 处理器函数}的函数。
 *
 * @author Arjen Poutsma
 * @since 5.2
 * @param <T> the type of the {@linkplain HandlerFunction handler function} to route to
 * @see RouterFunctions
 */
@FunctionalInterface
public interface RouterFunction<T extends ServerResponse> {

	/**
	 * 返回与给定请求匹配的{@linkplain HandlerFunction 处理器函数}。
	 * @param request the request to route
	 * @return an {@code Optional} describing the {@code HandlerFunction} that matches this request,
	 * or an empty {@code Optional} if there is no match
	 */
	Optional<HandlerFunction<T>> route(ServerRequest request);

	/**
	 * 返回一个组合路由函数，先调用此函数，如果此路由
	 * {@linkplain Optional#empty() 没有结果}，再调用 {@code other} 函数
	 * （响应类型同为 {@code T}）。
	 * @param other the function of type {@code T} to apply when this function has no result
	 * @return a composed function that first routes with this function and then the
	 * {@code other} function if this function has no result
	 * @see #andOther(RouterFunction)
	 */
	default RouterFunction<T> and(RouterFunction<T> other) {
		return new RouterFunctions.SameComposedRouterFunction<>(this, other);
	}

	/**
	 * 返回一个组合路由函数，先调用此函数，如果此路由
	 * {@linkplain Optional#empty() 没有结果}，再调用 {@code other} 函数
	 * （响应类型不同）。
	 * @param other the function to apply when this function has no result
	 * @return a composed function that first routes with this function and then the
	 * {@code other} function if this function has no result
	 * @see #and(RouterFunction)
	 */
	default RouterFunction<?> andOther(RouterFunction<?> other) {
		return new RouterFunctions.DifferentComposedRouterFunction(this, other);
	}

	/**
	 * 返回一个组合路由函数：如果此路由不匹配且给定请求谓词适用，
	 * 则路由到给定的处理器函数。此方法是
	 * {@link #and(RouterFunction)} 和
	 * {@link RouterFunctions#route(RequestPredicate, HandlerFunction)} 的便捷组合。
	 * @param predicate the predicate to test if this route does not match
	 * @param handlerFunction the handler function to route to if this route does not match and
	 * the predicate applies
	 * @return a composed function that route to {@code handlerFunction} if this route does not
	 * match and if {@code predicate} applies
	 */
	default RouterFunction<T> andRoute(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
		return and(RouterFunctions.route(predicate, handlerFunction));
	}

	/**
	 * 返回一个组合路由函数：如果此路由不匹配且给定请求谓词适用，
	 * 则路由到给定的路由函数。此方法是
	 * {@link #and(RouterFunction)} 和
	 * {@link RouterFunctions#nest(RequestPredicate, RouterFunction)} 的便捷组合。
	 * @param predicate the predicate to test if this route does not match
	 * @param routerFunction the router function to route to if this route does not match and
	 * the predicate applies
	 * @return a composed function that route to {@code routerFunction} if this route does not
	 * match and if {@code predicate} applies
	 */
	default RouterFunction<T> andNest(RequestPredicate predicate, RouterFunction<T> routerFunction) {
		return and(RouterFunctions.nest(predicate, routerFunction));
	}

	/**
	 * 使用给定的{@linkplain HandlerFilterFunction 过滤器函数}过滤此函数路由的所有
	 * {@linkplain HandlerFunction 处理器函数}。
	 * @param <S> the filter return type
	 * @param filterFunction the filter to apply
	 * @return the filtered routing function
	 */
	default <S extends ServerResponse> RouterFunction<S> filter(HandlerFilterFunction<T, S> filterFunction) {
		return new RouterFunctions.FilteredRouterFunction<>(this, filterFunction);
	}

	/**
	 * 接受给定的访问者。默认实现调用
	 * {@link RouterFunctions.Visitor#unknown(RouterFunction)}；组合的
	 * {@code RouterFunction} 实现应为构成此路由函数的所有组件调用 {@code accept}。
	 * @param visitor the visitor to accept
	 */
	default void accept(RouterFunctions.Visitor visitor) {
		visitor.unknown(this);
	}

	/**
	 * 返回带有给定属性的新路由函数。
	 * @param name the attribute name
	 * @param value the attribute value
     * @return a function that has the specified attributes
     * @since 5.3
	 */
	default RouterFunction<T> withAttribute(String name, Object value) {
		Assert.hasLength(name, "Name must not be empty");
		Assert.notNull(value, "Value must not be null");

		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put(name, value);
		return new RouterFunctions.AttributesRouterFunction<>(this, attributes);
	}

	/**
	 * 返回一个新的路由函数，其属性由给定消费者操作。
	 * <p>提供给消费者的映射是"实时"的，因此消费者可以用它来
	 * {@linkplain Map#put(Object, Object) 覆盖}现有属性、
	 * {@linkplain Map#remove(Object) 移除}属性，或使用任何其他
	 * {@link Map} 方法。
	 * @param attributesConsumer a function that consumes the attributes map
	 * @return this builder
	 * @since 5.3
	 */
	default RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
		Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

		Map<String, Object> attributes = new LinkedHashMap<>();
		attributesConsumer.accept(attributes);
		return new RouterFunctions.AttributesRouterFunction<>(this, attributes);
	}


}
