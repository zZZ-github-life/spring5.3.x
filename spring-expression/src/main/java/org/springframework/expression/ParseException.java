/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.expression;

import org.springframework.lang.Nullable;

/**
 * 表示在表达式解析期间发生的异常。
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ParseException extends ExpressionException {

	/**
	 * 创建一个新的表达式解析异常。
	 * @param expressionString 无法解析的表达式字符串
	 * @param position 问题在表达式字符串中出现的位置
	 * @param message 所发生问题的描述
	 */
	public ParseException(@Nullable String expressionString, int position, String message) {
		super(expressionString, position, message);
	}

	/**
	 * 创建一个新的表达式解析异常。
	 * @param position 问题在表达式字符串中出现的位置
	 * @param message 所发生问题的描述
	 * @param cause 此异常的根本原因
	 */
	public ParseException(int position, String message, Throwable cause) {
		super(position, message, cause);
	}

	/**
	 * 创建一个新的表达式解析异常。
	 * @param position 问题在表达式字符串中出现的位置
	 * @param message 所发生问题的描述
	 */
	public ParseException(int position, String message) {
		super(position, message);
	}

}
