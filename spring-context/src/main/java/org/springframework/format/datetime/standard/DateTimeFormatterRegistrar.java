/*
 * Copyright 2002-2018 the original author or authors.
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * 配置 JSR-310 <code>java.time</code> 格式化系统以供 Spring 使用。
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 4.0
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see #setUseIsoFormat
 * @see org.springframework.format.FormatterRegistrar#registerFormatters
 * @see org.springframework.format.datetime.DateFormatterRegistrar
 * @see org.springframework.format.datetime.joda.DateTimeFormatterFactoryBean
 */
public class DateTimeFormatterRegistrar implements FormatterRegistrar {

	private enum Type {DATE, TIME, DATE_TIME}


	/**
	 * 用户定义的格式化器。
	 */
	private final Map<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);

	/**
	 * 在未指定特定格式化器时使用的工厂。
	 */
	private final Map<Type, DateTimeFormatterFactory> factories = new EnumMap<>(Type.class);


	public DateTimeFormatterRegistrar() {
		for (Type type : Type.values()) {
			this.factories.put(type, new DateTimeFormatterFactory());
		}
	}


	/**
	 * 设置是否对所有日期/时间类型应用标准 ISO 格式化。默认为 "false"（否）。
	 * <p>如果设置为 "true"，则 "dateStyle"、"timeStyle" 和 "dateTimeStyle" 属性实际上会被忽略。
	 */
	public void setUseIsoFormat(boolean useIsoFormat) {
		this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
		this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
		this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
	}

	/**
	 * 设置 {@link java.time.LocalDate} 对象的默认格式样式。默认为
	 * {@link java.time.format.FormatStyle#SHORT}。
	 */
	public void setDateStyle(FormatStyle dateStyle) {
		this.factories.get(Type.DATE).setDateStyle(dateStyle);
	}

	/**
	 * 设置 {@link java.time.LocalTime} 对象的默认格式样式。默认为
	 * {@link java.time.format.FormatStyle#SHORT}。
	 */
	public void setTimeStyle(FormatStyle timeStyle) {
		this.factories.get(Type.TIME).setTimeStyle(timeStyle);
	}

	/**
	 * 设置 {@link java.time.LocalDateTime} 对象的默认格式样式。默认为
	 * {@link java.time.format.FormatStyle#SHORT}。
	 */
	public void setDateTimeStyle(FormatStyle dateTimeStyle) {
		this.factories.get(Type.DATE_TIME).setDateTimeStyle(dateTimeStyle);
	}

	/**
	 * 设置用于表示日期值的对象的格式化器。
	 * <p>此格式化器将用于 {@link LocalDate} 类型。指定后，{@link #setDateStyle dateStyle} 和
	 * {@link #setUseIsoFormat useIsoFormat} 属性将被忽略。
	 * @param formatter the formatter to use
	 * @see #setTimeFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setDateFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE, formatter);
	}

	/**
	 * 设置用于表示时间值的对象的格式化器。
	 * <p>此格式化器将用于 {@link LocalTime} 和 {@link OffsetTime} 类型。指定后，
	 * {@link #setTimeStyle timeStyle} 和 {@link #setUseIsoFormat useIsoFormat} 属性将被忽略。
	 * @param formatter the formatter to use
	 * @see #setDateFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.TIME, formatter);
	}

	/**
	 * 设置用于表示日期和时间值的对象的格式化器。
	 * <p>此格式化器将用于 {@link LocalDateTime}、{@link ZonedDateTime} 和 {@link OffsetDateTime} 类型。
	 * 指定后，{@link #setDateTimeStyle dateTimeStyle} 和 {@link #setUseIsoFormat useIsoFormat} 属性
	 * 将被忽略。
	 * @param formatter the formatter to use
	 * @see #setDateFormatter
	 * @see #setTimeFormatter
	 */
	public void setDateTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE_TIME, formatter);
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		DateTimeConverters.registerConverters(registry);

		DateTimeFormatter df = getFormatter(Type.DATE);
		DateTimeFormatter tf = getFormatter(Type.TIME);
		DateTimeFormatter dtf = getFormatter(Type.DATE_TIME);

		// Efficient ISO_LOCAL_* variants for printing since they are twice as fast...

		registry.addFormatterForFieldType(LocalDate.class,
				new TemporalAccessorPrinter(
						df == DateTimeFormatter.ISO_DATE ? DateTimeFormatter.ISO_LOCAL_DATE : df),
				new TemporalAccessorParser(LocalDate.class, df));

		registry.addFormatterForFieldType(LocalTime.class,
				new TemporalAccessorPrinter(
						tf == DateTimeFormatter.ISO_TIME ? DateTimeFormatter.ISO_LOCAL_TIME : tf),
				new TemporalAccessorParser(LocalTime.class, tf));

		registry.addFormatterForFieldType(LocalDateTime.class,
				new TemporalAccessorPrinter(
						dtf == DateTimeFormatter.ISO_DATE_TIME ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : dtf),
				new TemporalAccessorParser(LocalDateTime.class, dtf));

		registry.addFormatterForFieldType(ZonedDateTime.class,
				new TemporalAccessorPrinter(dtf),
				new TemporalAccessorParser(ZonedDateTime.class, dtf));

		registry.addFormatterForFieldType(OffsetDateTime.class,
				new TemporalAccessorPrinter(dtf),
				new TemporalAccessorParser(OffsetDateTime.class, dtf));

		registry.addFormatterForFieldType(OffsetTime.class,
				new TemporalAccessorPrinter(tf),
				new TemporalAccessorParser(OffsetTime.class, tf));

		registry.addFormatterForFieldType(Instant.class, new InstantFormatter());
		registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
		registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
		registry.addFormatterForFieldType(Year.class, new YearFormatter());
		registry.addFormatterForFieldType(Month.class, new MonthFormatter());
		registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
		registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());

		registry.addFormatterForFieldAnnotation(new Jsr310DateTimeFormatAnnotationFormatterFactory());
	}

	private DateTimeFormatter getFormatter(Type type) {
		DateTimeFormatter formatter = this.formatters.get(type);
		if (formatter != null) {
			return formatter;
		}
		DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);
		return this.factories.get(type).createDateTimeFormatter(fallbackFormatter);
	}

	private DateTimeFormatter getFallbackFormatter(Type type) {
		switch (type) {
			case DATE: return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
			case TIME: return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
			default: return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
		}
	}

}
