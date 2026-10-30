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

package org.springframework.expression;

import org.springframework.lang.Nullable;

/**
 * 处理表达式时可能发生的异常的父类。
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ExpressionException extends RuntimeException {

	@Nullable
	protected final String expressionString;

	protected int position;  // -1 if not known; should be known in all reasonable cases


	/**
	 * 构造一个新的表达式异常。
	 * @param message 描述性消息
	 */
	public ExpressionException(String message) {
		super(message);
		this.expressionString = null;
		this.position = 0;
	}

	/**
	 * 构造一个新的表达式异常。
	 * @param message 描述性消息
	 * @param cause 此异常的根本原因
	 */
	public ExpressionException(String message, Throwable cause) {
		super(message, cause);
		this.expressionString = null;
		this.position = 0;
	}

	/**
	 * 构造一个新的表达式异常。
	 * @param expressionString 表达式字符串
	 * @param message 描述性消息
	 */
	public ExpressionException(@Nullable String expressionString, String message) {
		super(message);
		this.expressionString = expressionString;
		this.position = -1;
	}

	/**
	 * 构造一个新的表达式异常。
	 * @param expressionString 表达式字符串
	 * @param position 问题在表达式字符串中出现的位置
	 * @param message 描述性消息
	 */
	public ExpressionException(@Nullable String expressionString, int position, String message) {
		super(message);
		this.expressionString = expressionString;
		this.position = position;
	}

	/**
	 * 构造一个新的表达式异常。
	 * @param position 问题在表达式字符串中出现的位置
	 * @param message 描述性消息
	 */
	public ExpressionException(int position, String message) {
		super(message);
		this.expressionString = null;
		this.position = position;
	}

	/**
	 * 构造一个新的表达式异常。
	 * @param position 问题在表达式字符串中出现的位置
	 * @param message 描述性消息
	 * @param cause 此异常的根本原因
	 */
	public ExpressionException(int position, String message, Throwable cause) {
		super(message, cause);
		this.expressionString = null;
		this.position = position;
	}


	/**
	 * 返回表达式字符串。
	 */
	@Nullable
	public final String getExpressionString() {
		return this.expressionString;
	}

	/**
	 * 返回问题在表达式字符串中发生的位置。
	 */
	public final int getPosition() {
		return this.position;
	}

	/**
	 * 返回异常消息。从 Spring 4.0 开始，此方法返回的结果与 {@link #toDetailedString()} 相同。
	 * @see #getSimpleMessage()
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return toDetailedString();
	}

	/**
	 * 返回此异常的详细描述，包括表达式字符串和位置（如果可用）以及实际的异常消息。
	 */
	public String toDetailedString() {
		if (this.expressionString != null) {
			StringBuilder output = new StringBuilder();
			output.append("Expression [");
			output.append(this.expressionString);
			output.append(']');
			if (this.position >= 0) {
				output.append(" @");
				output.append(this.position);
			}
			output.append(": ");
			output.append(getSimpleMessage());
			return output.toString();
		}
		else {
			return getSimpleMessage();
		}
	}

	/**
	 * 返回异常的简单消息，不包括导致失败的表达式。
	 * @since 4.0
	 */
	public String getSimpleMessage() {
		return super.getMessage();
	}

}
