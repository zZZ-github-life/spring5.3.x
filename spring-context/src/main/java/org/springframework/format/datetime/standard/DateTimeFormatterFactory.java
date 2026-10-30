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

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.TimeZone;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 创建 JSR-310 {@link java.time.format.DateTimeFormatter} 的工厂。
 *
 * <p>将使用已定义的 {@link #setPattern pattern}、{@link #setIso ISO} 和 <code>xxxStyle</code> 方法
 * 创建格式化器（按此顺序考虑）。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see #createDateTimeFormatter()
 * @see #createDateTimeFormatter(DateTimeFormatter)
 * @see #setPattern
 * @see #setIso
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see DateTimeFormatterFactoryBean
 */
public class DateTimeFormatterFactory {

	@Nullable
	private String pattern;

	@Nullable
	private ISO iso;

	@Nullable
	private FormatStyle dateStyle;

	@Nullable
	private FormatStyle timeStyle;

	@Nullable
	private TimeZone timeZone;


	/**
	 * 创建一个新的 {@code DateTimeFormatterFactory} 实例。
	 */
	public DateTimeFormatterFactory() {
	}

	/**
	 * 创建一个新的 {@code DateTimeFormatterFactory} 实例。
	 * @param pattern 用于格式化日期值的模式
	 */
	public DateTimeFormatterFactory(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * 设置用于格式化日期值的模式。
	 * @param pattern 格式模式
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * 设置用于格式化日期值的 ISO 格式。
	 * @param iso ISO 格式
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * 设置用于日期类型的样式。
	 */
	public void setDateStyle(FormatStyle dateStyle) {
		this.dateStyle = dateStyle;
	}

	/**
	 * 设置用于时间类型的样式。
	 */
	public void setTimeStyle(FormatStyle timeStyle) {
		this.timeStyle = timeStyle;
	}

	/**
	 * 设置用于日期和时间类型的样式。
	 */
	public void setDateTimeStyle(FormatStyle dateTimeStyle) {
		this.dateStyle = dateTimeStyle;
		this.timeStyle = dateTimeStyle;
	}

	/**
	 * 设置用于格式化日期值的两个字符，采用 Joda-Time 风格。
	 * <p>第一个字符用于日期样式；第二个字符用于时间样式。支持的字符包括：
	 * <ul>
	 * <li>'S' = Small</li>
	 * <li>'M' = Medium</li>
	 * <li>'L' = Long</li>
	 * <li>'F' = Full</li>
	 * <li>'-' = Omitted</li>
	 * </ul>
	 * <p>此方法模拟 Joda-Time 支持的样式。请注意，JSR-310 原生更倾向于使用
	 * {@link java.time.format.FormatStyle}，如同 {@link #setDateStyle}、{@link #setTimeStyle} 和
	 * {@link #setDateTimeStyle} 中所用。
	 * @param style 取自 {"S", "M", "L", "F", "-"} 集合的两个字符
	 */
	public void setStylePattern(String style) {
		Assert.isTrue(style.length() == 2, "Style pattern must consist of two characters");
		this.dateStyle = convertStyleCharacter(style.charAt(0));
		this.timeStyle = convertStyleCharacter(style.charAt(1));
	}

	@Nullable
	private FormatStyle convertStyleCharacter(char c) {
		switch (c) {
			case 'S': return FormatStyle.SHORT;
			case 'M': return FormatStyle.MEDIUM;
			case 'L': return FormatStyle.LONG;
			case 'F': return FormatStyle.FULL;
			case '-': return null;
			default: throw new IllegalArgumentException("Invalid style character '" + c + "'");
		}
	}

	/**
	 * 设置要将日期值归一化到的 {@code TimeZone}（如果有）。
	 * @param timeZone 时区
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}


	/**
	 * 使用此工厂创建一个新的 {@code DateTimeFormatter}。
	 * <p>如果未定义特定的模式或样式，将使用 {@link FormatStyle#MEDIUM medium date time format}。
	 * @return 新的日期时间格式化器
	 * @see #createDateTimeFormatter(DateTimeFormatter)
	 */
	public DateTimeFormatter createDateTimeFormatter() {
		return createDateTimeFormatter(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

	/**
	 * 使用此工厂创建一个新的 {@code DateTimeFormatter}。
	 * <p>如果未定义特定的模式或样式，将使用提供的 {@code fallbackFormatter}。
	 * @param fallbackFormatter 未设置任何具体的工厂属性时
	 * 要使用的回退格式化器
	 * @return 新的日期时间格式化器
	 */
	public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
		DateTimeFormatter dateTimeFormatter = null;
		if (StringUtils.hasLength(this.pattern)) {
			dateTimeFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(this.pattern);
		}
		else if (this.iso != null && this.iso != ISO.NONE) {
			switch (this.iso) {
				case DATE:
					dateTimeFormatter = DateTimeFormatter.ISO_DATE;
					break;
				case TIME:
					dateTimeFormatter = DateTimeFormatter.ISO_TIME;
					break;
				case DATE_TIME:
					dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
					break;
				default:
					throw new IllegalStateException("Unsupported ISO format: " + this.iso);
			}
		}
		else if (this.dateStyle != null && this.timeStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(this.dateStyle, this.timeStyle);
		}
		else if (this.dateStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(this.dateStyle);
		}
		else if (this.timeStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(this.timeStyle);
		}

		if (dateTimeFormatter != null && this.timeZone != null) {
			dateTimeFormatter = dateTimeFormatter.withZone(this.timeZone.toZoneId());
		}
		return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
	}

}
