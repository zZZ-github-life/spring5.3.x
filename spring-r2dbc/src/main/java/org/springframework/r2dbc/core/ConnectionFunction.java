/*
 * Copyright 2002-2023 the original author or authors.
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


/**
 * 联合类型，结合 {@link Function} 和 {@link SqlProvider} 以暴露与底层操作
 * 相关的 SQL。SqlProvider 可以支持延迟 / 仅生成一次语义，
 * 在这种情况下，在调用 {@code #apply(Connection)} 方法之前，
 * {@link #getSql()} 可能为 {@code null}。
 *
 * @author Mark Paluch
 * @author Simon Baslé
 * @since 5.3
 * @param <R> the type of the result of the function.
 */
interface ConnectionFunction<R> extends Function<Connection, R>, SqlProvider {
}

