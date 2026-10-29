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

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Mono;

import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.util.Assert;

/**
 * 非阻塞、响应式的客户端，用于在 Reactive Streams 背压下执行数据库调用请求。
 * 在 R2DBC 客户端库之上提供了更高层次的通用 API。
 *
 * <p>使用静态工厂方法 {@link #create(ConnectionFactory)} 之一，
 * 或获取 {@link DatabaseClient#builder()} 来创建实例。
 *
 * 使用示例：
 * <pre class="code">
 * ConnectionFactory factory = …
 *
 * DatabaseClient client = DatabaseClient.create(factory);
 * Mono&lt;Actor&gt; actor = client.sql("select first_name, last_name from t_actor")
 *     .map(row -&gt; new Actor(row.get("first_name", String.class),
 *          row.get("last_name", String.class)))
 *     .first();
 * </pre>
 *
 * @author Mark Paluch
 * @since 5.3
 */
public interface DatabaseClient extends ConnectionAccessor {

	/**
	 * 返回此客户端使用的 {@link ConnectionFactory}。
	 * @return the connection factory
	 */
	ConnectionFactory getConnectionFactory();

	/**
	 * 指定要运行的静态 {@code sql} 语句。该约定用于指定一次 SQL 调用
	 * 以及导致执行的各项选项。当启用 {@link NamedParameterExpander} 时，
	 * SQL 字符串既可包含原生参数绑定标记，也可包含命名参数
	 * （例如 {@literal :foo, :bar}）。
	 * @param sql the SQL statement
	 * @return a new {@link GenericExecuteSpec}
	 * @see NamedParameterExpander
	 * @see DatabaseClient.Builder#namedParameters(boolean)
	 */
	GenericExecuteSpec sql(String sql);

	/**
	 * 指定一个提供要运行的 SQL 的 {@link Supplier SQL 提供者}。
	 * 该约定用于指定一次 SQL 调用以及导致执行的各项选项。
	 * 当启用 {@link NamedParameterExpander} 时，SQL 字符串既可包含
	 * 原生参数绑定标记，也可包含命名参数（例如 {@literal :foo, :bar}）。
	 * <p>接受 {@link PreparedOperation} 作为 SQL，以及绑定 {@link Supplier}。
	 * <p>{@code DatabaseClient} 实现应尽可能延迟 SQL 字符串的解析，
	 * 理想情况下延迟到 {@code Subscription} 发生之时。默认实现即是如此。
	 * @param sqlSupplier a supplier for the SQL statement
	 * @return a new {@link GenericExecuteSpec}
	 * @see NamedParameterExpander
	 * @see DatabaseClient.Builder#namedParameters(boolean)
	 * @see PreparedOperation
	 */
	GenericExecuteSpec sql(Supplier<String> sqlSupplier);


	// Static factory methods

	/**
	 * 创建一个将使用所提供 {@link ConnectionFactory} 的 {@code DatabaseClient}。
	 * @param factory the {@code ConnectionFactory} to use for obtaining connections
	 * @return a new {@code DatabaseClient}. Guaranteed to be not {@code null}.
	 */
	static DatabaseClient create(ConnectionFactory factory) {
		return new DefaultDatabaseClientBuilder().connectionFactory(factory).build();
	}

	/**
	 * 获取一个 {@code DatabaseClient} 构建器。
	 */
	static DatabaseClient.Builder builder() {
		return new DefaultDatabaseClientBuilder();
	}


	/**
	 * 用于创建 {@link DatabaseClient} 的可变构建器。
	 */
	interface Builder {

		/**
		 * 配置要使用的 {@link BindMarkersFactory BindMarkers}。
		 */
		Builder bindMarkers(BindMarkersFactory bindMarkers);

		/**
		 * 配置 {@link ConnectionFactory R2DBC 连接器}。
		 */
		Builder connectionFactory(ConnectionFactory factory);

		/**
		 * 配置一个用于执行 {@link Statement} 对象的 {@link ExecuteFunction}。
		 * @see Statement#execute()
		 */
		Builder executeFunction(ExecuteFunction executeFunction);

		/**
		 * 配置是否使用命名参数展开。
		 * 默认值为 {@code true}。
		 * @param enabled {@code true} to use named parameter expansion;
		 * {@code false} to disable named parameter expansion
		 * @see NamedParameterExpander
		 */
		Builder namedParameters(boolean enabled);

		/**
		 * 配置一个用于配置此构建器的 {@link Consumer}。
		 */
		Builder apply(Consumer<Builder> builderConsumer);

		/**
		 * 构建 {@link DatabaseClient} 实例。
		 */
		DatabaseClient build();
	}


	/**
	 * 用于指定一次 SQL 调用以及导致执行的各项选项的约定。
	 */
	interface GenericExecuteSpec {

		/**
		 * 将一个非 {@code null} 值绑定到由其 {@code index} 标识的参数。
		 * {@code value} 可以是标量值，也可以是 {@link Parameter}。
		 * @param index zero based index to bind the parameter to
		 * @param value either a scalar value or {@link Parameter}
		 */
		GenericExecuteSpec bind(int index, Object value);

		/**
		 * 将一个 {@code null} 值绑定到由其 {@code index} 标识的参数。
		 * @param index zero based index to bind the parameter to
		 * @param type the parameter type
		 */
		GenericExecuteSpec bindNull(int index, Class<?> type);

		/**
		 * 将一个非 {@code null} 值绑定到由其 {@code name} 标识的参数。
		 * @param name the name of the parameter
		 * @param value the value to bind
		 */
		GenericExecuteSpec bind(String name, Object value);

		/**
		 * 将一个 {@code null} 值绑定到由其 {@code name} 标识的参数。
		 * @param name the name of the parameter
		 * @param type the parameter type
		 */
		GenericExecuteSpec bindNull(String name, Class<?> type);

		/**
		 * 将给定的过滤器添加到过滤器链的末尾。
		 * <p>过滤器函数通常用于在 Statement 执行之前调用其上的方法。例如：
		 * <pre class="code">
		 * DatabaseClient client = …;
		 * client.sql("SELECT book_id FROM book").filter(statement -&gt; statement.fetchSize(100))
		 * </pre>
		 * @param filterFunction the filter to be added to the chain
		 */
		default GenericExecuteSpec filter(Function<? super Statement, ? extends Statement> filterFunction) {
			Assert.notNull(filterFunction, "Filter function must not be null");
			return filter((statement, next) -> next.execute(filterFunction.apply(statement)));
		}

		/**
		 * 将给定的过滤器添加到过滤器链的末尾。
		 * <p>过滤器函数通常用于在 Statement 执行之前调用其上的方法。例如：
		 * <pre class="code">
		 * DatabaseClient client = …;
		 * client.sql("SELECT book_id FROM book").filter((statement, next) -&gt; next.execute(statement.fetchSize(100)))
		 * </pre>
		 * @param filter the filter to be added to the chain
		 */
		GenericExecuteSpec filter(StatementFilterFunction filter);

		/**
		 * 配置结果映射 {@link Function 函数}并进入执行阶段。
		 * @param mappingFunction a function that maps from {@link Row} to the result type
		 * @param <R> the result type
		 * @return a {@link FetchSpec} for configuration what to fetch
		 */
		default <R> RowsFetchSpec<R> map(Function<Row, R> mappingFunction) {
			Assert.notNull(mappingFunction, "Mapping function must not be null");
			return map((row, rowMetadata) -> mappingFunction.apply(row));
		}

		/**
		 * 配置结果映射 {@link BiFunction 函数}并进入执行阶段。
		 * @param mappingFunction a function that maps from {@link Row} and {@link RowMetadata}
		 * to the result type
		 * @param <R> the result type
		 * @return a {@link FetchSpec} for configuration what to fetch
		 */
		<R> RowsFetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction);

		/**
		 * 执行 SQL 调用，并通过进入执行阶段来获取结果。
		 */
		FetchSpec<Map<String, Object>> fetch();

		/**
		 * 执行 SQL 调用，并返回一个在语句完成时不带结果地完成的 {@link Mono}。
		 * @return a {@link Mono} ignoring its payload (actively dropping)
		 */
		Mono<Void> then();
	}

}
