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

package org.springframework.r2dbc.connection.init;

/**
 * 当无法确定比"处理 SQL 脚本时出现了问题"更具体的原因时抛出：
 * 例如，来自 R2DBC 的 {@link io.r2dbc.spi.R2dbcException} 无法更精确地定位。
 *
 * @author Sam Brannen
 * @author Mark Paluch
 * @since 5.3
 */
@SuppressWarnings("serial")
public class UncategorizedScriptException extends ScriptException {

	/**
	 * 创建一个新的 {@code UncategorizedScriptException}。
	 * @param message detailed message
	 */
	public UncategorizedScriptException(String message) {
		super(message);
	}

	/**
	 * 创建一个新的 {@code UncategorizedScriptException}。
	 * @param message detailed message
	 * @param cause the root cause
	 */
	public UncategorizedScriptException(String message, Throwable cause) {
		super(message, cause);
	}

}
