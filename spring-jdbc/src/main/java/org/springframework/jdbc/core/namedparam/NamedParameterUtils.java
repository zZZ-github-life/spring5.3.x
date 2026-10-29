/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc.core.namedparam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于命名参数解析的辅助方法。
 *
 * <p>仅限在 Spring 的 JDBC 框架内部使用。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Yanming Zhou
 * @since 2.0
 */
public abstract class NamedParameterUtils {

	/**
	 * 可作为注释或引号起始字符的字符集合。
	 */
	private static final String[] START_SKIP = new String[] {"'", "\"", "--", "/*"};

	/**
	 * 与起始字符对应的注释或引号结束字符集合。
	 */
	private static final String[] STOP_SKIP = new String[] {"'", "\"", "\n", "*/"};

	/**
	 * 可作为参数分隔符的字符集合，
	 * 表示 SQL 字符串中的参数名已结束。
	 */
	private static final String PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^";

	/**
	 * 按字符编码索引的分隔符标志数组。
	 * 目前技术上只需要 34 到 124 之间的部分。
	 */
	private static final boolean[] separatorIndex = new boolean[128];

	static {
		for (char c : PARAMETER_SEPARATORS.toCharArray()) {
			separatorIndex[c] = true;
		}
	}


	//-------------------------------------------------------------------------
	// Core methods used by NamedParameterJdbcTemplate and SqlQuery/SqlUpdate
	//-------------------------------------------------------------------------

	/**
	 * 解析 SQL 语句并定位所有占位符或命名参数。
	 * 命名参数会被替换为 JDBC 占位符。
	 * @param sql the SQL statement
	 * @return the parsed statement, represented as {@link ParsedSql} instance
	 */
	public static ParsedSql parseSqlStatement(String sql) {
		Assert.notNull(sql, "SQL must not be null");

		Set<String> namedParameters = new HashSet<>();
		StringBuilder sqlToUse = new StringBuilder(sql);
		List<ParameterHolder> parameterList = new ArrayList<>();

		char[] statement = sql.toCharArray();
		int namedParameterCount = 0;
		int unnamedParameterCount = 0;
		int totalParameterCount = 0;

		int escapes = 0;
		int i = 0;
		while (i < statement.length) {
			int skipToPosition = i;
			while (i < statement.length) {
				skipToPosition = skipCommentsAndQuotes(statement, i);
				if (i == skipToPosition) {
					break;
				}
				else {
					i = skipToPosition;
				}
			}
			if (i >= statement.length) {
				break;
			}
			char c = statement[i];
			if (c == ':' || c == '&') {
				int j = i + 1;
				if (c == ':' && j < statement.length && statement[j] == ':') {
					// Postgres-style "::" casting operator should be skipped
					i = i + 2;
					continue;
				}
				String parameter = null;
				if (c == ':' && j < statement.length && statement[j] == '{') {
					// :{x} style parameter
					while (statement[j] != '}') {
						j++;
						if (j >= statement.length) {
							throw new InvalidDataAccessApiUsageException(
									"Non-terminated named parameter declaration at position " + i +
									" in statement: " + sql);
						}
						if (statement[j] == ':' || statement[j] == '{') {
							throw new InvalidDataAccessApiUsageException(
									"Parameter name contains invalid character '" + statement[j] +
									"' at position " + i + " in statement: " + sql);
						}
					}
					if (j - i > 2) {
						parameter = sql.substring(i + 2, j);
						namedParameterCount = addNewNamedParameter(
								namedParameters, namedParameterCount, parameter);
						totalParameterCount = addNamedParameter(
								parameterList, totalParameterCount, escapes, i, j + 1, parameter);
					}
					j++;
				}
				else {
					while (j < statement.length && !isParameterSeparator(statement[j])) {
						j++;
					}
					if (j - i > 1) {
						parameter = sql.substring(i + 1, j);
						namedParameterCount = addNewNamedParameter(
								namedParameters, namedParameterCount, parameter);
						totalParameterCount = addNamedParameter(
								parameterList, totalParameterCount, escapes, i, j, parameter);
					}
				}
				i = j - 1;
			}
			else {
				if (c == '\\') {
					int j = i + 1;
					if (j < statement.length && statement[j] == ':') {
						// escaped ":" should be skipped
						sqlToUse.deleteCharAt(i - escapes);
						escapes++;
						i = i + 2;
						continue;
					}
				}
				if (c == '?') {
					int j = i + 1;
					if (j < statement.length && (statement[j] == '?' || statement[j] == '|' || statement[j] == '&')) {
						// Postgres-style "??", "?|", "?&" operator should be skipped
						i = i + 2;
						continue;
					}
					unnamedParameterCount++;
					totalParameterCount++;
				}
			}
			i++;
		}
		ParsedSql parsedSql = new ParsedSql(sqlToUse.toString());
		for (ParameterHolder ph : parameterList) {
			parsedSql.addNamedParameter(ph.getParameterName(), ph.getStartIndex(), ph.getEndIndex());
		}
		parsedSql.setNamedParameterCount(namedParameterCount);
		parsedSql.setUnnamedParameterCount(unnamedParameterCount);
		parsedSql.setTotalParameterCount(totalParameterCount);
		return parsedSql;
	}

	private static int addNamedParameter(List<ParameterHolder> parameterList,
			int totalParameterCount, int escapes, int i, int j, String parameter) {

		parameterList.add(new ParameterHolder(parameter, i - escapes, j - escapes));
		totalParameterCount++;
		return totalParameterCount;
	}

	private static int addNewNamedParameter(Set<String> namedParameters, int namedParameterCount, String parameter) {
		if (!namedParameters.contains(parameter)) {
			namedParameters.add(parameter);
			namedParameterCount++;
		}
		return namedParameterCount;
	}

	/**
	 * 跳过 SQL 语句中的注释和带引号的名称。
	 * @param statement character array containing SQL statement
	 * @param position current position of statement
	 * @return next position to process after any comments or quotes are skipped
	 */
	private static int skipCommentsAndQuotes(char[] statement, int position) {
		for (int i = 0; i < START_SKIP.length; i++) {
			if (statement[position] == START_SKIP[i].charAt(0)) {
				boolean match = true;
				for (int j = 1; j < START_SKIP[i].length(); j++) {
					if (statement[position + j] != START_SKIP[i].charAt(j)) {
						match = false;
						break;
					}
				}
				if (match) {
					int offset = START_SKIP[i].length();
					for (int m = position + offset; m < statement.length; m++) {
						if (statement[m] == STOP_SKIP[i].charAt(0)) {
							boolean endMatch = true;
							int endPos = m;
							for (int n = 1; n < STOP_SKIP[i].length(); n++) {
								if (m + n >= statement.length) {
									// last comment not closed properly
									return statement.length;
								}
								if (statement[m + n] != STOP_SKIP[i].charAt(n)) {
									endMatch = false;
									break;
								}
								endPos = m + n;
							}
							if (endMatch) {
								// found character sequence ending comment or quote
								return endPos + 1;
							}
						}
					}
					// character sequence ending comment or quote not found
					return statement.length;
				}
			}
		}
		return position;
	}

	/**
	 * 解析 SQL 语句并定位所有占位符或命名参数。命名参数会被替换为
	 * JDBC 占位符，任何选择列表都会被扩展为所需数量的占位符。
	 * 选择列表可以包含对象数组，此时占位符会被分组并用括号括起来。
	 * 这允许在 SQL 语句中使用“表达式列表”，例如：<br /><br />
	 * {@code select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))}
	 * <p>传入的参数值用于确定选择列表要使用的占位符数量。
	 * 选择列表应限制在 100 个或更少的元素。数量更多时不能保证
	 * 数据库支持，并且完全取决于具体厂商。
	 * @param parsedSql the parsed representation of the SQL statement
	 * @param paramSource the source for named parameters
	 * @return the SQL statement with substituted parameters
	 * @see #parseSqlStatement
	 */
	public static String substituteNamedParameters(ParsedSql parsedSql, @Nullable SqlParameterSource paramSource) {
		String originalSql = parsedSql.getOriginalSql();
		List<String> paramNames = parsedSql.getParameterNames();
		if (paramNames.isEmpty()) {
			return originalSql;
		}

		StringBuilder actualSql = new StringBuilder(originalSql.length());
		int lastIndex = 0;
		for (int i = 0; i < paramNames.size(); i++) {
			String paramName = paramNames.get(i);
			int[] indexes = parsedSql.getParameterIndexes(i);
			int startIndex = indexes[0];
			int endIndex = indexes[1];
			actualSql.append(originalSql, lastIndex, startIndex);
			if (paramSource != null && paramSource.hasValue(paramName)) {
				Object value = paramSource.getValue(paramName);
				if (value instanceof SqlParameterValue) {
					value = ((SqlParameterValue) value).getValue();
				}
				if (value instanceof Iterable) {
					Iterator<?> entryIter = ((Iterable<?>) value).iterator();
					int k = 0;
					while (entryIter.hasNext()) {
						if (k > 0) {
							actualSql.append(", ");
						}
						k++;
						Object entryItem = entryIter.next();
						if (entryItem instanceof Object[]) {
							Object[] expressionList = (Object[]) entryItem;
							actualSql.append('(');
							for (int m = 0; m < expressionList.length; m++) {
								if (m > 0) {
									actualSql.append(", ");
								}
								actualSql.append('?');
							}
							actualSql.append(')');
						}
						else {
							actualSql.append('?');
						}
					}
				}
				else {
					actualSql.append('?');
				}
			}
			else {
				actualSql.append('?');
			}
			lastIndex = endIndex;
		}
		actualSql.append(originalSql, lastIndex, originalSql.length());
		return actualSql.toString();
	}

	/**
	 * 将命名参数值的 Map 转换为相应的数组。
	 * @param parsedSql the parsed SQL statement
	 * @param paramSource the source for named parameters
	 * @param declaredParams the List of declared SqlParameter objects
	 * (may be {@code null}). If specified, the parameter metadata will
	 * be built into the value array in the form of SqlParameterValue objects.
	 * @return the array of values
	 */
	public static Object[] buildValueArray(
			ParsedSql parsedSql, SqlParameterSource paramSource, @Nullable List<SqlParameter> declaredParams) {

		Object[] paramArray = new Object[parsedSql.getTotalParameterCount()];
		if (parsedSql.getNamedParameterCount() > 0 && parsedSql.getUnnamedParameterCount() > 0) {
			throw new InvalidDataAccessApiUsageException(
					"Not allowed to mix named and traditional ? placeholders. You have " +
					parsedSql.getNamedParameterCount() + " named parameter(s) and " +
					parsedSql.getUnnamedParameterCount() + " traditional placeholder(s) in statement: " +
					parsedSql.getOriginalSql());
		}
		List<String> paramNames = parsedSql.getParameterNames();
		for (int i = 0; i < paramNames.size(); i++) {
			String paramName = paramNames.get(i);
			try {
				SqlParameter param = findParameter(declaredParams, paramName, i);
				Object paramValue = paramSource.getValue(paramName);
				if (paramValue instanceof SqlParameterValue) {
					paramArray[i] = paramValue;
				}
				else {
					paramArray[i] = (param != null ? new SqlParameterValue(param, paramValue) :
							SqlParameterSourceUtils.getTypedValue(paramSource, paramName));
				}
			}
			catch (IllegalArgumentException ex) {
				throw new InvalidDataAccessApiUsageException(
						"No value supplied for the SQL parameter '" + paramName + "': " + ex.getMessage());
			}
		}
		return paramArray;
	}

	/**
	 * 在给定的已声明参数列表中查找匹配的参数。
	 * @param declaredParams the declared SqlParameter objects
	 * @param paramName the name of the desired parameter
	 * @param paramIndex the index of the desired parameter
	 * @return the declared SqlParameter, or {@code null} if none found
	 */
	@Nullable
	private static SqlParameter findParameter(
			@Nullable List<SqlParameter> declaredParams, String paramName, int paramIndex) {

		if (declaredParams != null) {
			// First pass: Look for named parameter match.
			for (SqlParameter declaredParam : declaredParams) {
				if (paramName.equals(declaredParam.getName())) {
					return declaredParam;
				}
			}
			// Second pass: Look for parameter index match.
			if (paramIndex < declaredParams.size()) {
				SqlParameter declaredParam = declaredParams.get(paramIndex);
				// Only accept unnamed parameters for index matches.
				if (declaredParam.getName() == null) {
					return declaredParam;
				}
			}
		}
		return null;
	}

	/**
	 * 判断参数名是否在当前位置结束，
	 * 即给定字符是否可作为分隔符。
	 */
	private static boolean isParameterSeparator(char c) {
		return (c < 128 && separatorIndex[c]) || Character.isWhitespace(c);
	}

	/**
	 * 将 SqlParameterSource 中的参数类型转换为相应的 int 数组。
	 * 这是为了复用 JdbcTemplate 上已有的方法所必需的。
	 * 所有命名参数类型都会根据解析后的 SQL 语句信息
	 * 放入 Object 数组中的正确位置。
	 * @param parsedSql the parsed SQL statement
	 * @param paramSource the source for named parameters
	 */
	public static int[] buildSqlTypeArray(ParsedSql parsedSql, SqlParameterSource paramSource) {
		int[] sqlTypes = new int[parsedSql.getTotalParameterCount()];
		List<String> paramNames = parsedSql.getParameterNames();
		for (int i = 0; i < paramNames.size(); i++) {
			String paramName = paramNames.get(i);
			sqlTypes[i] = paramSource.getSqlType(paramName);
		}
		return sqlTypes;
	}

	/**
	 * 将 SqlParameterSource 中的参数声明转换为相应的 SqlParameter 列表。
	 * 这是为了复用 JdbcTemplate 上已有的方法所必需的。
	 * 命名参数的 SqlParameter 会根据解析后的 SQL 语句信息
	 * 放入结果列表中的正确位置。
	 * @param parsedSql the parsed SQL statement
	 * @param paramSource the source for named parameters
	 */
	public static List<SqlParameter> buildSqlParameterList(ParsedSql parsedSql, SqlParameterSource paramSource) {
		List<String> paramNames = parsedSql.getParameterNames();
		List<SqlParameter> params = new ArrayList<>(paramNames.size());
		for (String paramName : paramNames) {
			params.add(new SqlParameter(
					paramName, paramSource.getSqlType(paramName), paramSource.getTypeName(paramName)));
		}
		return params;
	}


	//-------------------------------------------------------------------------
	// Convenience methods operating on a plain SQL String
	//-------------------------------------------------------------------------

	/**
	 * 解析 SQL 语句并定位所有占位符或命名参数。
	 * 命名参数会被替换为 JDBC 占位符。
	 * <p>这是
	 * {@link #parseSqlStatement(String)} 与
	 * {@link #substituteNamedParameters(ParsedSql, SqlParameterSource)} 组合的快捷版本。
	 * @param sql the SQL statement
	 * @return the actual (parsed) SQL statement
	 */
	public static String parseSqlStatementIntoString(String sql) {
		ParsedSql parsedSql = parseSqlStatement(sql);
		return substituteNamedParameters(parsedSql, null);
	}

	/**
	 * 解析 SQL 语句并定位所有占位符或命名参数。
	 * 命名参数会被替换为 JDBC 占位符，并且任何选择列表
	 * 都会被扩展为所需数量的占位符。
	 * <p>这是
	 * {@link #substituteNamedParameters(ParsedSql, SqlParameterSource)} 的快捷版本。
	 * @param sql the SQL statement
	 * @param paramSource the source for named parameters
	 * @return the SQL statement with substituted parameters
	 */
	public static String substituteNamedParameters(String sql, SqlParameterSource paramSource) {
		ParsedSql parsedSql = parseSqlStatement(sql);
		return substituteNamedParameters(parsedSql, paramSource);
	}

	/**
	 * 将命名参数值的 Map 转换为相应的数组。
	 * <p>这是
	 * {@link #buildValueArray(ParsedSql, SqlParameterSource, java.util.List)} 的快捷版本。
	 * @param sql the SQL statement
	 * @param paramMap the Map of parameters
	 * @return the array of values
	 */
	public static Object[] buildValueArray(String sql, Map<String, ?> paramMap) {
		ParsedSql parsedSql = parseSqlStatement(sql);
		return buildValueArray(parsedSql, new MapSqlParameterSource(paramMap), null);
	}


	private static class ParameterHolder {

		private final String parameterName;

		private final int startIndex;

		private final int endIndex;

		public ParameterHolder(String parameterName, int startIndex, int endIndex) {
			this.parameterName = parameterName;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
		}

		public String getParameterName() {
			return this.parameterName;
		}

		public int getStartIndex() {
			return this.startIndex;
		}

		public int getEndIndex() {
			return this.endIndex;
		}
	}

}
