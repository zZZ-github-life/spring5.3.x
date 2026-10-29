/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * 用于 JSR-310 {@link java.time.Instant} 的 {@link Formatter} 实现，遵循 JSR-310 对 Instant 的解析
 * 规则（即不使用可配置的 {@link java.time.format.DateTimeFormatter}）：自 Spring 4.3 起，同时接受
 * 默认的 {@code ISO_INSTANT} 格式以及 {@code RFC_1123_DATE_TIME} 格式（后者常用于 HTTP 日期标头值）。
 *
 * @author Juergen Hoeller
 * @author Andrei Nevedomskii
 * @since 4.0
 * @see java.time.Instant#parse
 * @see java.time.format.DateTimeFormatter#ISO_INSTANT
 * @see java.time.format.DateTimeFormatter#RFC_1123_DATE_TIME
 */
public class InstantFormatter implements Formatter<Instant> {

	@Override
	public Instant parse(String text, Locale locale) throws ParseException {
		if (text.length() > 0 && Character.isAlphabetic(text.charAt(0))) {
			// assuming RFC-1123 value a la "Tue, 3 Jun 2008 11:05:30 GMT"
			return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(text));
		}
		else {
			// assuming UTC instant a la "2007-12-03T10:15:30.00Z"
			return Instant.parse(text);
		}
	}

	@Override
	public String print(Instant object, Locale locale) {
		return object.toString();
	}

}
