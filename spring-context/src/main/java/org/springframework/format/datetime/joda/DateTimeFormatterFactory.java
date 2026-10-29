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

package org.springframework.format.datetime.joda;

import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 创建 Joda-Time {@link DateTimeFormatter} 的工厂。
 *
 * <p>格式化器将使用定义的 {@link #setPattern pattern}、{@link #setIso ISO} 和
 * {@link #setStyle style} 方法创建（按此顺序考虑）。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.2
 * @see #createDateTimeFormatter()
 * @see #createDateTimeFormatter(DateTimeFormatter)
 * @see #setPattern
 * @see #setStyle
 * @see #setIso
 * @see DateTimeFormatterFactoryBean
 * @deprecated as of 5.3, in favor of standard JSR-310 support
 */
@Deprecated
public class DateTimeFormatterFactory {

	@Nullable
	private String pattern;

	@Nullable
	private ISO iso;

	@Nullable
	private String style;

	@Nullable
	private TimeZone timeZone;


	/**
	 * 创建一个新的 {@code DateTimeFormatterFactory} 实例。
	 */
	public DateTimeFormatterFactory() {
	}

	/**
	 * 创建一个新的 {@code DateTimeFormatterFactory} 实例。
	 * @param pattern the pattern to use to format date values
	 */
	public DateTimeFormatterFactory(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * 设置用于格式化日期值的模式。
	 * @param pattern the format pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 设置用于格式化日期值的 ISO 格式。
	 * @param iso the ISO format
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * 设置用于格式化日期值的两个字符，采用 Joda-Time 风格。<p>第一个字符用于日期样式；第二个字符用于时
	 * 间样式。支持的字符有：
	 * <ul>
	 * <li>'S' = Small</li>
	 * <li>'M' = Medium</li>
	 * <li>'L' = Long</li>
	 * <li>'F' = Full</li>
	 * <li>'-' = Omitted</li>
	 * </ul>
	 * @param style two characters from the set {"S", "M", "L", "F", "-"}
	 */
	public void setStyle(String style) {
		this.style = style;
	}

	/**
	 * 设置用于将日期值规范化到的 {@code TimeZone}（如果有）。
	 * @param timeZone the time zone
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}


	/**
	 * 使用此工厂创建一个新的 {@code DateTimeFormatter}。<p>如果未定义特定的模式或样式，将使用
	 * {@link DateTimeFormat#mediumDateTime() medium date time format}。
	 * @return a new date time formatter
	 * @see #createDateTimeFormatter(DateTimeFormatter)
	 */
	public DateTimeFormatter createDateTimeFormatter() {
		return createDateTimeFormatter(DateTimeFormat.mediumDateTime());
	}

	/**
	 * 使用此工厂创建一个新的 {@code DateTimeFormatter}。<p>如果未定义特定的模式或样式，将使用提供的
	 * {@code fallbackFormatter}。
	 * @param fallbackFormatter the fall-back formatter to use
	 * when no specific factory properties have been set
	 * @return a new date time formatter
	 */
	public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
		DateTimeFormatter dateTimeFormatter = null;
		if (StringUtils.hasLength(this.pattern)) {
			dateTimeFormatter = DateTimeFormat.forPattern(this.pattern);
		}
		else if (this.iso != null && this.iso != ISO.NONE) {
			switch (this.iso) {
				case DATE:
					dateTimeFormatter = ISODateTimeFormat.date();
					break;
				case TIME:
					dateTimeFormatter = ISODateTimeFormat.time();
					break;
				case DATE_TIME:
					dateTimeFormatter = ISODateTimeFormat.dateTime();
					break;
				default:
					throw new IllegalStateException("Unsupported ISO format: " + this.iso);
			}
		}
		else if (StringUtils.hasLength(this.style)) {
			dateTimeFormatter = DateTimeFormat.forStyle(this.style);
		}

		if (dateTimeFormatter != null && this.timeZone != null) {
			dateTimeFormatter = dateTimeFormatter.withZone(DateTimeZone.forTimeZone(this.timeZone));
		}
		return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
	}

}
