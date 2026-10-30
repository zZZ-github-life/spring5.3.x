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

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * 为与 Spring 一起使用而配置 Joda-Time 的格式化系统。
 *
 * <p><b>NOTE:</b> 自 Spring 4.0 起，Spring 的 Joda-Time 支持要求 Joda-Time 2.x。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.1
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see #setUseIsoFormat
 * @see FormatterRegistrar#registerFormatters
 * @see org.springframework.format.datetime.DateFormatterRegistrar
 * @see DateTimeFormatterFactoryBean
 * @deprecated as of 5.3, in favor of standard JSR-310 support
 */
@Deprecated
public class JodaTimeFormatterRegistrar implements FormatterRegistrar {

	private enum Type {DATE, TIME, DATE_TIME}


	/**
	 * 用户定义的格式化器。
	 */
	private final Map<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);

	/**
	 * 在未指定特定格式化器时使用的工厂。
	 */
	private final Map<Type, DateTimeFormatterFactory> factories;


	public JodaTimeFormatterRegistrar() {
		this.factories = new EnumMap<>(Type.class);
		for (Type type : Type.values()) {
			this.factories.put(type, new DateTimeFormatterFactory());
		}
	}


	/**
	 * 设置是否应对所有日期/时间类型应用标准 ISO 格式化。默认为 "false"（否）。<p>如果设为 "true"，则
	 * "dateStyle"、"timeStyle" 和 "dateTimeStyle" 属性将被忽略。
	 */
	public void setUseIsoFormat(boolean useIsoFormat) {
		this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
		this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
		this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
	}

	/**
	 * 设置 Joda {@link LocalDate} 对象的默认格式样式。默认为 {@link DateTimeFormat#shortDate()}。
	 */
	public void setDateStyle(String dateStyle) {
		this.factories.get(Type.DATE).setStyle(dateStyle + "-");
	}

	/**
	 * 设置 Joda {@link LocalTime} 对象的默认格式样式。默认为 {@link DateTimeFormat#shortTime()}。
	 */
	public void setTimeStyle(String timeStyle) {
		this.factories.get(Type.TIME).setStyle("-" + timeStyle);
	}

	/**
	 * 设置 Joda {@link LocalDateTime} 和 {@link DateTime} 对象以及 JDK {@link Date} 和
	 * {@link Calendar} 对象的默认格式样式。默认为 {@link DateTimeFormat#shortDateTime()}。
	 */
	public void setDateTimeStyle(String dateTimeStyle) {
		this.factories.get(Type.DATE_TIME).setStyle(dateTimeStyle);
	}

	/**
	 * 设置用于表示日期值的对象的格式化器。<p>此格式化器将用于 {@link LocalDate} 类型。指定后，
	 * {@link #setDateStyle(String) dateStyle} 和 {@link #setUseIsoFormat(boolean) useIsoFormat} 属性将
	 * 被忽略。
	 * @param formatter 要使用的格式化器
	 * @since 3.2
	 * @see #setTimeFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setDateFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE, formatter);
	}

	/**
	 * 设置用于表示时间值的对象的格式化器。<p>此格式化器将用于 {@link LocalTime} 类型。指定后，
	 * {@link #setTimeStyle(String) timeStyle} 和 {@link #setUseIsoFormat(boolean) useIsoFormat} 属性将
	 * 被忽略。
	 * @param formatter 要使用的格式化器
	 * @since 3.2
	 * @see #setDateFormatter
	 * @see #setDateTimeFormatter
	 */
	public void setTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.TIME, formatter);
	}

	/**
	 * 设置用于表示日期和时间值的对象的格式化器。<p>此格式化器将用于 {@link LocalDateTime}、
	 * {@link ReadableInstant}、{@link Date} 和 {@link Calendar} 类型。指定后，
	 * {@link #setDateTimeStyle(String) dateTimeStyle} 和
	 * {@link #setUseIsoFormat(boolean) useIsoFormat} 属性将被忽略。
	 * @param formatter 要使用的格式化器
	 * @since 3.2
	 * @see #setDateFormatter
	 * @see #setTimeFormatter
	 */
	public void setDateTimeFormatter(DateTimeFormatter formatter) {
		this.formatters.put(Type.DATE_TIME, formatter);
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		JodaTimeConverters.registerConverters(registry);

		DateTimeFormatter dateFormatter = getFormatter(Type.DATE);
		DateTimeFormatter timeFormatter = getFormatter(Type.TIME);
		DateTimeFormatter dateTimeFormatter = getFormatter(Type.DATE_TIME);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateFormatter),
				new LocalDateParser(dateFormatter),
				LocalDate.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(timeFormatter),
				new LocalTimeParser(timeFormatter),
				LocalTime.class);

		addFormatterForFields(registry,
				new ReadablePartialPrinter(dateTimeFormatter),
				new LocalDateTimeParser(dateTimeFormatter),
				LocalDateTime.class);

		addFormatterForFields(registry,
				new ReadableInstantPrinter(dateTimeFormatter),
				new DateTimeParser(dateTimeFormatter),
				ReadableInstant.class);

		// In order to retain backwards compatibility we only register Date/Calendar
		// types when a user defined formatter is specified (see SPR-10105)
		if (this.formatters.containsKey(Type.DATE_TIME)) {
			addFormatterForFields(registry,
					new ReadableInstantPrinter(dateTimeFormatter),
					new DateTimeParser(dateTimeFormatter),
					Date.class, Calendar.class);
		}

		registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
		registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
		registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
		registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());

		registry.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
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
			case DATE: return DateTimeFormat.shortDate();
			case TIME: return DateTimeFormat.shortTime();
			default: return DateTimeFormat.shortDateTime();
		}
	}

	private void addFormatterForFields(FormatterRegistry registry, Printer<?> printer,
			Parser<?> parser, Class<?>... fieldTypes) {

		for (Class<?> fieldType : fieldTypes) {
			registry.addFormatterForFieldType(fieldType, printer, parser);
		}
	}

}
