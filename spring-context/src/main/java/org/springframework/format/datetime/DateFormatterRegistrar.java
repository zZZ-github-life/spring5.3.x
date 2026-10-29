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

package org.springframework.format.datetime;

import java.util.Calendar;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 为 Spring 配置基本的日期格式化，主要用于
 * {@link org.springframework.format.annotation.DateTimeFormat} 声明。适用于 {@link Date}、
 * {@link Calendar} 和 {@code long} 类型的字段。
 *
 * <p>设计为可直接实例化，同时也暴露静态的 {@link #addDateConverters(ConverterRegistry)} 工具方法，
 * 以便针对任意 {@code ConverterRegistry} 实例临时使用。
 *
 * @author Phillip Webb
 * @since 3.2
 * @see org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
 * @see org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar
 * @see FormatterRegistrar#registerFormatters
 */
public class DateFormatterRegistrar implements FormatterRegistrar {

	@Nullable
	private DateFormatter dateFormatter;


	/**
	 * 设置要注册的全局日期格式化器。<p>如果未指定，则不会为未标注注解的 {@link Date} 和
	 * {@link Calendar} 字段注册通用格式化器。
	 */
	public void setFormatter(DateFormatter dateFormatter) {
		Assert.notNull(dateFormatter, "DateFormatter must not be null");
		this.dateFormatter = dateFormatter;
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		addDateConverters(registry);
		// In order to retain back compatibility we only register Date/Calendar
		// types when a user defined formatter is specified (see SPR-10105)
		if (this.dateFormatter != null) {
			registry.addFormatter(this.dateFormatter);
			registry.addFormatterForFieldType(Calendar.class, this.dateFormatter);
		}
		registry.addFormatterForFieldAnnotation(new DateTimeFormatAnnotationFormatterFactory());
	}

	/**
	 * 将日期转换器添加到指定的注册表。
	 * @param converterRegistry the registry of converters to add to
	 */
	public static void addDateConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverter(new DateToLongConverter());
		converterRegistry.addConverter(new DateToCalendarConverter());
		converterRegistry.addConverter(new CalendarToDateConverter());
		converterRegistry.addConverter(new CalendarToLongConverter());
		converterRegistry.addConverter(new LongToDateConverter());
		converterRegistry.addConverter(new LongToCalendarConverter());
	}


	private static class DateToLongConverter implements Converter<Date, Long> {

		@Override
		public Long convert(Date source) {
			return source.getTime();
		}
	}


	private static class DateToCalendarConverter implements Converter<Date, Calendar> {

		@Override
		public Calendar convert(Date source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(source);
			return calendar;
		}
	}


	private static class CalendarToDateConverter implements Converter<Calendar, Date> {

		@Override
		public Date convert(Calendar source) {
			return source.getTime();
		}
	}


	private static class CalendarToLongConverter implements Converter<Calendar, Long> {

		@Override
		public Long convert(Calendar source) {
			return source.getTimeInMillis();
		}
	}


	private static class LongToDateConverter implements Converter<Long, Date> {

		@Override
		public Date convert(Long source) {
			return new Date(source);
		}
	}


	private static class LongToCalendarConverter implements Converter<Long, Calendar> {

		@Override
		public Calendar convert(Long source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(source);
			return calendar;
		}
	}

}
