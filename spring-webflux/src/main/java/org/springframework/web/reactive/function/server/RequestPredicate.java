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

package org.springframework.web.reactive.function.server;

import java.util.Optional;

/**
 * 表示对给定 {@link ServerRequest} 求值的函数。可在 {@link RequestPredicates} 中找到对常见请求属性求值的此函数实例。
 * @author Arjen Poutsma
 * @since 5.0
 * @see RequestPredicates
 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
 */
@FunctionalInterface
public interface RequestPredicate {

	/**
	 * 对给定请求求值此谓词。
	 * @param request the request to match against
	 * @return {@code true} if the request matches the predicate; {@code false} otherwise
	 */
	boolean test(ServerRequest request);

	/**
	 * 返回组合请求谓词，对此谓词和 {@code other} 谓词都进行测试（AND）。求值组合谓词时，如果此谓词为 {@code false}，则不会对 {@code other} 谓词求值。
	 * @param other a predicate that will be logically-ANDed with this predicate
	 * @return a predicate composed of this predicate AND the {@code other} predicate
	 */
	default RequestPredicate and(RequestPredicate other) {
		return new RequestPredicates.AndRequestPredicate(this, other);
	}

	/**
	 * 返回表示此谓词逻辑取反的谓词。
	 * @return a predicate that represents the logical negation of this predicate
	 */
	default RequestPredicate negate() {
		return new RequestPredicates.NegateRequestPredicate(this);
	}

	/**
	 * 返回组合请求谓词，对此谓词或 {@code other} 谓词进行测试（OR）。求值组合谓词时，如果此谓词为 {@code true}，则不会对 {@code other} 谓词求值。
	 * @param other a predicate that will be logically-ORed with this predicate
	 * @return a predicate composed of this predicate OR the {@code other} predicate
	 */
	default RequestPredicate or(RequestPredicate other) {
		return new RequestPredicates.OrRequestPredicate(this, other);
	}

	/**
	 * 将给定请求转换为用于嵌套路由的请求。例如，基于路径的谓词可以返回匹配后剩余路径的 {@code ServerRequest}。
	 * <p>默认实现会在 {@link #test(ServerRequest)} 求值为 {@code true} 时返回包装给定请求的 {@code Optional}；如果求值为 {@code false}，则返回 {@link Optional#empty()}。
	 * @param request the request to be nested
	 * @return the nested request
	 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
	 */
	default Optional<ServerRequest> nest(ServerRequest request) {
		return (test(request) ? Optional.of(request) : Optional.empty());
	}

	/**
	 * 接受给定的访问者。默认实现调用 {@link RequestPredicates.Visitor#unknown(RequestPredicate)}；组合式 {@code RequestPredicate} 实现应为构成此请求谓词的所有组件调用 {@code accept}。
	 * @param visitor the visitor to accept
	 */
	default void accept(RequestPredicates.Visitor visitor) {
		visitor.unknown(this);
	}

}
