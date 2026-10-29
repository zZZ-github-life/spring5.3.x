/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format.number;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * 用于 Number 的抽象格式化器，提供 {@link #getNumberFormat(java.util.Locale)} 模板方法。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 3.0
 */
public abstract class AbstractNumberFormatter implements Formatter<Number> {

	private boolean lenient = false;


	/**
	 * 指定解析是否采用宽松模式。默认为 {@code false}。
	 * <p>在宽松解析下，解析器可能允许与格式不完全匹配的输入。在严格解析下，输入必须与格式完全匹配。
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	@Override
	public String print(Number number, Locale locale) {
		return getNumberFormat(locale).format(number);
	}

	@Override
	public Number parse(String text, Locale locale) throws ParseException {
		NumberFormat format = getNumberFormat(locale);
		ParsePosition position = new ParsePosition(0);
		Number number = format.parse(text, position);
		if (position.getErrorIndex() != -1) {
			throw new ParseException(text, position.getIndex());
		}
		if (!this.lenient) {
			if (text.length() != position.getIndex()) {
				// indicates a part of the string that was not parsed
				throw new ParseException(text, position.getIndex());
			}
		}
		return number;
	}

	/**
	 * 获取指定区域设置的具体 NumberFormat。
	 * @param locale the current locale
	 * @return the NumberFormat instance (never {@code null})
	 */
	protected abstract NumberFormat getNumberFormat(Locale locale);

}
