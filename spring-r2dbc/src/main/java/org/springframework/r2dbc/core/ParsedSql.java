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

import java.util.ArrayList;
import java.util.List;

/**
 * 保存已解析 SQL 语句的相关信息。
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 5.3
 */
class ParsedSql {

	private final String originalSql;

	private final List<String> parameterNames = new ArrayList<>();

	private final List<int[]> parameterIndexes = new ArrayList<>();

	private int namedParameterCount;

	private int unnamedParameterCount;

	private int totalParameterCount;


	/**
	 * 创建一个新的 {@link ParsedSql} 类实例。
	 * @param originalSql the SQL statement that is being (or is to be) parsed
	 */
	ParsedSql(String originalSql) {
		this.originalSql = originalSql;
	}

	/**
	 * 返回正在解析的 SQL 语句。
	 */
	String getOriginalSql() {
		return this.originalSql;
	}


	/**
	 * 添加从此 SQL 语句中解析出的命名参数。
	 * @param parameterName the name of the parameter
	 * @param startIndex the start index in the original SQL String
	 * @param endIndex the end index in the original SQL String
	 */
	void addNamedParameter(String parameterName, int startIndex, int endIndex) {
		this.parameterNames.add(parameterName);
		this.parameterIndexes.add(new int[] {startIndex, endIndex});
	}

	/**
	 * 返回已解析 SQL 语句中的所有参数（绑定变量）。
	 * 同一参数名重复出现的情况也会包含在此处。
	 */
	List<String> getParameterNames() {
		return this.parameterNames;
	}

	/**
	 * 返回指定参数的参数索引。
	 * @param parameterPosition the position of the parameter
	 * (as index in the parameter names List)
	 * @return the start index and end index, combined into
	 * an int array of length 2
	 */
	int[] getParameterIndexes(int parameterPosition) {
		return this.parameterIndexes.get(parameterPosition);
	}

	/**
	 * 设置 SQL 语句中命名参数的数量。
	 * 每个参数名只计一次；重复出现的情况在此不计数。
	 */
	void setNamedParameterCount(int namedParameterCount) {
		this.namedParameterCount = namedParameterCount;
	}

	/**
	 * 返回 SQL 语句中命名参数的数量。
	 * 每个参数名只计一次；重复出现的情况在此不计数。
	 */
	int getNamedParameterCount() {
		return this.namedParameterCount;
	}

	/**
	 * 设置 SQL 语句中所有未命名参数的数量。
	 */
	void setUnnamedParameterCount(int unnamedParameterCount) {
		this.unnamedParameterCount = unnamedParameterCount;
	}

	/**
	 * 返回 SQL 语句中所有未命名参数的数量。
	 */
	int getUnnamedParameterCount() {
		return this.unnamedParameterCount;
	}

	/**
	 * 设置 SQL 语句中所有参数的总数。
	 * 同一参数名的重复出现在此计入。
	 */
	void setTotalParameterCount(int totalParameterCount) {
		this.totalParameterCount = totalParameterCount;
	}

	/**
	 * 返回 SQL 语句中所有参数的总数。
	 * 同一参数名的重复出现在此计入。
	 */
	int getTotalParameterCount() {
		return this.totalParameterCount;
	}


	/**
	 * 暴露原始 SQL 字符串。
	 */
	@Override
	public String toString() {
		return this.originalSql;
	}

}
