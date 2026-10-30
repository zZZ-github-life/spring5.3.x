/*
 * Copyright 2002-2021 the original author or authors.
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

/**
 * 当我们无法确定比“处理 SQL 脚本时出了点问题”更具体的原因时抛出：
 * 例如，来自 JDBC 的 {@link java.sql.SQLException} 无法进一步精确定位。
 *
 * @author Sam Brannen
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class UncategorizedScriptException extends ScriptException {

	/**
	 * 创建一个新的 {@code UncategorizedScriptException}。
	 * @param message 详细消息
	 */
	public UncategorizedScriptException(String message) {
		super(message);
	}

	/**
	 * 创建一个新的 {@code UncategorizedScriptException}。
	 * @param message 详细消息
	 * @param cause 根本原因
	 */
	public UncategorizedScriptException(String message, Throwable cause) {
		super(message, cause);
	}

}
