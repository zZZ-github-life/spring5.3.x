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

import java.util.List;

import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.util.ConcurrentLruCache;


/**
 * SQL 翻译支持，允许使用命名参数而非原生占位符。
 *
 * <p>此类在执行时会将包含命名参数的 SQL 展开为原生风格的占位符。
 * 它还允许将 {@link List} 值展开为相应数量的占位符。
 *
 * <p>如果 {@link BindMarkersFactory} 使用
 * {@link BindMarkersFactory#identifiablePlaceholders() 可标识的}占位符，
 * 则对同一参数名的引用会被替换为相同的绑定标记占位符。
 * <p><b>注意：此类实例在配置完成后是线程安全的。</b>
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
class NamedParameterExpander {

	/**
	 * SQL 缓存的默认最大条目数：256。
	 */
	public static final int DEFAULT_CACHE_LIMIT = 256;

	/** 从原始 SQL 字符串到 ParsedSql 表示的缓存。 */
	private final ConcurrentLruCache<String, ParsedSql> parsedSqlCache =
			new ConcurrentLruCache<>(DEFAULT_CACHE_LIMIT, NamedParameterUtils::parseSqlStatement);


	/**
	 * 获取给定 SQL 语句的已解析表示。
	 * <p>默认实现使用上限为 256 个条目的 LRU 缓存。
	 * @param sql the original SQL statement
	 * @return a representation of the parsed SQL statement
	 */
	private ParsedSql getParsedSql(String sql) {
		return this.parsedSqlCache.get(sql);
	}

	/**
	 * 解析 SQL 语句并定位任何占位符或命名参数。
	 * 命名参数会被替换为原生占位符，任何选择列表都会被展开为
	 * 所需数量的占位符。选择列表可能包含对象数组，
	 * 在这种情况下，占位符将被分组并用括号括起来。
	 * 这样便可在 SQL 语句中使用"表达式列表"，例如：
	 * <pre class="code">
	 * select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))
	 * </pre>
	 * <p>传入的参数值用于确定选择列表要使用的占位符数量。
	 * 选择列表应限制为 100 个或更少的元素。数据库不保证支持更多元素，
	 * 并且这严格取决于数据库供应商。
	 * @param sql sql the original SQL statement
	 * @param bindMarkersFactory the bind marker factory
	 * @param paramSource the source for named parameters
	 * @return the expanded sql that accepts bind parameters and allows for execution
	 * without further translation wrapped as {@link PreparedOperation}.
	 */
	public PreparedOperation<String> expand(
			String sql, BindMarkersFactory bindMarkersFactory, BindParameterSource paramSource) {

		ParsedSql parsedSql = getParsedSql(sql);
		return NamedParameterUtils.substituteNamedParameters(parsedSql, bindMarkersFactory, paramSource);
	}

	/**
	 * 解析 SQL 语句并定位任何占位符或命名参数。
	 * 命名参数作为此方法调用的结果返回。
	 * @return the parameter names
	 */
	public List<String> getParameterNames(String sql) {
		return getParsedSql(sql).getParameterNames();
	}

}
