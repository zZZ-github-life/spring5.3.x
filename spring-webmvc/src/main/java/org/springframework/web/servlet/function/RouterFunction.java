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
 * @param <T> 要路由到的 {@linkplain HandlerFunction handler function} 的类型
 * @see RouterFunctions
 */
@FunctionalInterface
public interface RouterFunction<T extends ServerResponse> {

	/**
	 * 返回与给定请求匹配的{@linkplain HandlerFunction 处理器函数}。
	 * @param request 要路由的请求
	 * @return 描述匹配此请求的 {@code HandlerFunction} 的 {@code Optional}；
	 * 如果没有匹配项，则为空 {@code Optional}
	 */
	Optional<HandlerFunction<T>> route(ServerRequest request);

	/**
	 * 返回一个组合路由函数，先调用此函数，如果此路由
	 * {@linkplain Optional#empty() 没有结果}，再调用 {@code other} 函数
	 * （响应类型同为 {@code T}）。
	 * @param other 当此函数没有结果时要应用的 {@code T} 类型函数
	 * @return 一个组合函数，先通过此函数进行路由，
	 * 如果此函数没有结果，则再通过 {@code other} 函数进行路由
	 * @see #andOther(RouterFunction)
	 */
	default RouterFunction<T> and(RouterFunction<T> other) {
		return new RouterFunctions.SameComposedRouterFunction<>(this, other);
	}

	/**
	 * 返回一个组合路由函数，先调用此函数，如果此路由
	 * {@linkplain Optional#empty() 没有结果}，再调用 {@code other} 函数
	 * （响应类型不同）。
	 * @param other 当此函数没有结果时要应用的函数
	 * @return 一个组合函数，先通过此函数进行路由，
	 * 如果此函数没有结果，则再通过 {@code other} 函数进行路由
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
	 * @param predicate 用于测试此路由是否不匹配的谓词
	 * @param handlerFunction 如果此路由不匹配且谓词成立，则路由到的处理器函数
	 * @return 一个组合函数，如果此路由不匹配且 {@code predicate} 成立，
	 * 则路由到 {@code handlerFunction}
	 */
	default RouterFunction<T> andRoute(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
		return and(RouterFunctions.route(predicate, handlerFunction));
	}

	/**
	 * 返回一个组合路由函数：如果此路由不匹配且给定请求谓词适用，
	 * 则路由到给定的路由函数。此方法是
	 * {@link #and(RouterFunction)} 和
	 * {@link RouterFunctions#nest(RequestPredicate, RouterFunction)} 的便捷组合。
	 * @param predicate 用于测试此路由是否不匹配的谓词
	 * @param routerFunction 如果此路由不匹配且谓词成立，则路由到的路由函数
	 * @return 一个组合函数，如果此路由不匹配且 {@code predicate} 成立，
	 * 则路由到 {@code routerFunction}
	 */
	default RouterFunction<T> andNest(RequestPredicate predicate, RouterFunction<T> routerFunction) {
		return and(RouterFunctions.nest(predicate, routerFunction));
	}

	/**
	 * 使用给定的{@linkplain HandlerFilterFunction 过滤器函数}过滤此函数路由的所有
	 * {@linkplain HandlerFunction 处理器函数}。
	 * @param <S> 过滤器返回类型
	 * @param filterFunction 要应用的过滤器
	 * @return 经过滤的路由函数
	 */
	default <S extends ServerResponse> RouterFunction<S> filter(HandlerFilterFunction<T, S> filterFunction) {
		return new RouterFunctions.FilteredRouterFunction<>(this, filterFunction);
	}

	/**
	 * 接受给定的访问者。默认实现调用
	 * {@link RouterFunctions.Visitor#unknown(RouterFunction)}；组合的
	 * {@code RouterFunction} 实现应为构成此路由函数的所有组件调用 {@code accept}。
	 * @param visitor 要接受的访问者
	 */
	default void accept(RouterFunctions.Visitor visitor) {
		visitor.unknown(this);
	}

	/**
	 * 返回带有给定属性的新路由函数。
	 * @param name 属性名称
	 * @param value 属性值
     * @return 具有指定属性的函数
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
	 * @param attributesConsumer 消费属性映射的函数
	 * @return 此构建器
	 * @since 5.3
	 */
	default RouterFunction<T> withAttributes(Consumer<Map<String, Object>> attributesConsumer) {
		Assert.notNull(attributesConsumer, "AttributesConsumer must not be null");

		Map<String, Object> attributes = new LinkedHashMap<>();
		attributesConsumer.accept(attributes);
		return new RouterFunctions.AttributesRouterFunction<>(this, attributes);
	}


}
