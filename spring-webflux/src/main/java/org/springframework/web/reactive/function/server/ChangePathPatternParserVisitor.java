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

package org.springframework.web.reactive.function.server;

import java.util.Map;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * 更改路径相关请求谓词（即 {@link RequestPredicates.PathPatternPredicate}）上 {@link PathPatternParser} 的 {@link RouterFunctions.Visitor} 实现。
 * @author Arjen Poutsma
 * @since 5.3
 */
class ChangePathPatternParserVisitor implements RouterFunctions.Visitor {

	private final PathPatternParser parser;


	public ChangePathPatternParserVisitor(PathPatternParser parser) {
		Assert.notNull(parser, "Parser must not be null");
		this.parser = parser;
	}

	@Override
	public void startNested(RequestPredicate predicate) {
		changeParser(predicate);
	}

	@Override
	public void endNested(RequestPredicate predicate) {
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		changeParser(predicate);
	}

	@Override
	public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
	}

	private void changeParser(RequestPredicate predicate) {
		if (predicate instanceof Target) {
			Target target = (Target) predicate;
			target.changeParser(this.parser);
		}
	}


	/**
	 * 由可以更改解析器的谓词实现的接口。
	 */
	public interface Target {

		void changeParser(PathPatternParser parser);
	}
}
