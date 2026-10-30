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

import java.util.function.Function;

import io.r2dbc.spi.Connection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.dao.DataAccessException;

/**
 * 声明接受回调 {@link Function}、在 {@link Connection} 作用域内
 * 进行操作的方法的接口。回调函数在所提供的连接上操作，
 * 且不得关闭该连接，因为连接可能来自连接池，
 * 或者会受到其他形式的资源管理。
 *
 * <p> 回调函数负责创建一个 {@link org.reactivestreams.Publisher}，
 * 它定义了所分配的 {@link Connection} 的有效时长。
 * 连接在 publisher 终止后释放。
 *
 * @author Mark Paluch
 * @since 5.3
 */
public interface ConnectionAccessor {

	/**
	 * 在 {@link Connection} 作用域内执行回调 {@link Function}。
	 * 该函数负责创建一个 {@link Mono}。连接在 {@link Mono} 终止
	 * （或订阅被取消）后释放。连接资源不得传递到
	 * {@link Function} 闭包之外，否则资源可能会失效。
	 * @param action 指定连接操作的回调对象
	 * @return 结果 {@link Mono}
	 */
	<T> Mono<T> inConnection(Function<Connection, Mono<T>> action) throws DataAccessException;

	/**
	 * 在 {@link Connection} 作用域内执行回调 {@link Function}。
	 * 该函数负责创建一个 {@link Flux}。连接在 {@link Flux} 终止
	 * （或订阅被取消）后释放。连接资源不得传递到
	 * {@link Function} 闭包之外，否则资源可能会失效。
	 * @param action 指定连接操作的回调对象
	 * @return 结果 {@link Flux}
	 */
	<T> Flux<T> inConnectionMany(Function<Connection, Flux<T>> action) throws DataAccessException;

}
