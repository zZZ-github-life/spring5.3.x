/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import org.springframework.core.io.support.EncodedResource;

/**
 * 当 SQL 脚本中的某条语句针对目标数据库执行失败时，由 {@link ScriptUtils} 抛出。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0.5
 */
@SuppressWarnings("serial")
public class ScriptStatementFailedException extends ScriptException {

	/**
	 * 构造一个新的 {@code ScriptStatementFailedException}。
	 * @param stmt 实际执行失败的 SQL 语句
	 * @param stmtNumber SQL 脚本中的语句编号（即资源中出现的第 n<sup>th</sup> 条语句）
	 * @param encodedResource 读取该 SQL 语句所用的资源
	 * @param cause 失败的底层原因
	 */
	public ScriptStatementFailedException(String stmt, int stmtNumber, EncodedResource encodedResource, Throwable cause) {
		super(buildErrorMessage(stmt, stmtNumber, encodedResource), cause);
	}


	/**
	 * 根据提供的参数为 SQL 脚本执行失败构建错误消息。
	 * @param stmt 实际执行失败的 SQL 语句
	 * @param stmtNumber SQL 脚本中的语句编号（即资源中出现的第 n<sup>th</sup> 条语句）
	 * @param encodedResource 读取该 SQL 语句所用的资源
	 * @return 适合用作异常的<em>详细消息</em>或用于日志记录的错误消息
	 * @since 4.2
	 */
	public static String buildErrorMessage(String stmt, int stmtNumber, EncodedResource encodedResource) {
		return String.format("Failed to execute SQL script statement #%s of %s: %s", stmtNumber, encodedResource, stmt);
	}

}
