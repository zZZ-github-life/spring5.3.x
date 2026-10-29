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

package org.springframework.expression.spel;

import org.springframework.expression.ParseException;
import org.springframework.lang.Nullable;

/**
 * Spring EL 相关异常的根异常。它不持有表示问题的硬编码字符串，而是记录消息键以及消息的插入内容。有
 * 关可能出现的所有消息列表，请参见 {@link SpelMessage}。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class SpelParseException extends ParseException {

	private final SpelMessage message;

	private final Object[] inserts;


	public SpelParseException(@Nullable String expressionString, int position, SpelMessage message, Object... inserts) {
		super(expressionString, position, message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelParseException(int position, SpelMessage message, Object... inserts) {
		super(position, message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelParseException(int position, Throwable cause, SpelMessage message, Object... inserts) {
		super(position, message.formatMessage(inserts), cause);
		this.message = message;
		this.inserts = inserts;
	}


	/**
	 * 返回消息代码。
	 */
	public SpelMessage getMessageCode() {
		return this.message;
	}

	/**
	 * 返回消息插入内容。
	 */
	public Object[] getInserts() {
		return this.inserts;
	}

}
