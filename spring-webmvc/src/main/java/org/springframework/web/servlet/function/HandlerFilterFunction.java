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

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * 表示过滤{@linkplain HandlerFunction 处理器函数}的函数。
 *
 * @author Arjen Poutsma
 * @since 5.2
 * @param <T> the type of the {@linkplain HandlerFunction handler function} to filter
 * @param <R> the type of the response of the function
 * @see RouterFunction#filter(HandlerFilterFunction)
 */
@FunctionalInterface
public interface HandlerFilterFunction<T extends ServerResponse, R extends ServerResponse> {

	/**
	 * 将此过滤器应用于给定的处理器函数。给定的
	 * {@linkplain HandlerFunction 处理器函数}表示链中的下一个实体，
	 * 可以{@linkplain HandlerFunction#handle(ServerRequest) 调用}它以继续处理该实体，
	 * 或者不调用它来阻断链。
	 * @param request the request
	 * @param next the next handler or filter function in the chain
	 * @return the filtered response
	 */
	R filter(ServerRequest request, HandlerFunction<T> next) throws Exception;

	/**
	 * 返回一个组合过滤器函数，先应用此过滤器，然后应用
	 * {@code after} 过滤器。
	 * @param after the filter to apply after this filter is applied
	 * @return a composed filter that first applies this function and then applies the
	 * {@code after} function
	 */
	default HandlerFilterFunction<T, R> andThen(HandlerFilterFunction<T, T> after) {
		Assert.notNull(after, "HandlerFilterFunction must not be null");
		return (request, next) -> {
			HandlerFunction<T> nextHandler = handlerRequest -> after.filter(handlerRequest, next);
			return filter(request, nextHandler);
		};
	}

	/**
	 * 将此过滤器应用于给定的处理器函数，得到一个经过过滤的处理器函数。
	 * @param handler the handler function to filter
	 * @return the filtered handler function
	 */
	default HandlerFunction<R> apply(HandlerFunction<T> handler) {
		Assert.notNull(handler, "HandlerFunction must not be null");
		return request -> this.filter(request, handler);
	}

	/**
	 * 将给定的请求处理函数适配为仅对 {@code ServerRequest} 操作的过滤器函数。
	 * @param requestProcessor the request processor
	 * @return the filter adaptation of the request processor
	 */
	static <T extends ServerResponse> HandlerFilterFunction<T, T>
	ofRequestProcessor(Function<ServerRequest, ServerRequest> requestProcessor) {

		Assert.notNull(requestProcessor, "Function must not be null");
		return (request, next) -> next.handle(requestProcessor.apply(request));
	}

	/**
	 * 将给定的响应处理函数适配为仅对 {@code ServerResponse} 操作的过滤器函数。
	 * @param responseProcessor the response processor
	 * @return the filter adaptation of the request processor
	 */
	static <T extends ServerResponse, R extends ServerResponse> HandlerFilterFunction<T, R>
	ofResponseProcessor(BiFunction<ServerRequest, T, R> responseProcessor) {

		Assert.notNull(responseProcessor, "Function must not be null");
		return (request, next) -> responseProcessor.apply(request, next.handle(request));
	}

	/**
	 * 将给定的谓词和响应提供函数适配为在给定异常时返回
	 * {@code ServerResponse} 的过滤器函数。
	 * @param predicate the predicate to match an exception
	 * @param errorHandler the response provider
	 * @return the filter adaption of the error handler
	 */
	static <T extends ServerResponse> HandlerFilterFunction<T, T>
	ofErrorHandler(Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, T> errorHandler) {

		Assert.notNull(predicate, "Predicate must not be null");
		Assert.notNull(errorHandler, "ErrorHandler must not be null");

		return (request, next) -> {
			try {
				T t = next.handle(request);
				if (t instanceof ErrorHandlingServerResponse) {
					((ErrorHandlingServerResponse) t).addErrorHandler(predicate, errorHandler);
				}
				return t;
			}
			catch (Throwable throwable) {
				if (predicate.test(throwable)) {
					return errorHandler.apply(throwable, request);
				}
				else {
					throw throwable;
				}
			}
		};
	}

}
