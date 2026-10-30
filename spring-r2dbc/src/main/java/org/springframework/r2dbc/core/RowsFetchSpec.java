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

package org.springframework.r2dbc.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用于获取表格结果的约定。
 *
 * @author Mark Paluch
 * @since 5.3
 * @param <T> 行结果类型
 */
public interface RowsFetchSpec<T> {

	/**
	 * 获取恰好零个或一个结果。
	 * @return 发出一个元素的 Mono；如果没有匹配项，
	 * 则为 {@link Mono#empty()}。
	 * 如果匹配项多于一个，则以 {@code IncorrectResultSizeDataAccessException} 完成
	 */
	Mono<T> one();

	/**
	 * 获取第一个结果，或者不返回结果。
	 * @return 发出第一个元素的 Mono；
	 * 如果没有匹配项，则为 {@link Mono#empty()}
	 */
	Mono<T> first();

	/**
	 * 获取所有匹配的元素。
	 * @return 发出所有结果的 Flux
	 */
	Flux<T> all();

}
