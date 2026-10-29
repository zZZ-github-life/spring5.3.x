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

import java.util.function.BiFunction;
import java.util.function.Supplier;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 一个 {@link ConnectionFunction}，生成 {@link Result} 的 {@code Flux}，
 * 并将 SQL 的生成延迟到该函数被应用之后。在此之前，{@code getSql()}
 * 方法只返回 {@code null}。SQL 字符串在应用过程中也会被记忆，
 * 因此后续调用 {@link #getSql()} 会返回相同的 {@code String}，
 * 而不会再次调用 {@code Supplier}。
 *
 * @author Mark Paluch
 * @author Simon Baslé
 * @since 5.3.26
 */
final class ResultFunction implements ConnectionFunction<Flux<Result>> {

	final Supplier<String> sqlSupplier;
	final BiFunction<Connection, String, Statement> statementFunction;
	final StatementFilterFunction filterFunction;
	final ExecuteFunction executeFunction;

	@Nullable
	String resolvedSql = null;

	ResultFunction(Supplier<String> sqlSupplier, BiFunction<Connection, String, Statement> statementFunction, StatementFilterFunction filterFunction, ExecuteFunction executeFunction) {
		this.sqlSupplier = sqlSupplier;
		this.statementFunction = statementFunction;
		this.filterFunction = filterFunction;
		this.executeFunction = executeFunction;
	}

	@Override
	public Flux<Result> apply(Connection connection) {
		String sql = this.sqlSupplier.get();
		Assert.state(StringUtils.hasText(sql), "SQL returned by supplier must not be empty");
		this.resolvedSql = sql;
		Statement statement = this.statementFunction.apply(connection, sql);
		return Flux.from(this.filterFunction.filter(statement, this.executeFunction))
				.cast(Result.class).checkpoint("SQL \"" + sql + "\" [DatabaseClient]");
	}

	@Nullable
	@Override
	public String getSql() {
		return this.resolvedSql;
	}
}
