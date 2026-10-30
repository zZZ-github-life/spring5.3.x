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

package org.springframework.format.datetime.standard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

/**
 * 使用 JDK 8 中的 JSR-310 <code>java.time</code> 包来格式化带有 {@link DateTimeFormat} 注解的字段。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 * @see org.springframework.format.annotation.DateTimeFormat
 */
public class Jsr310DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport
		implements AnnotationFormatterFactory<DateTimeFormat> {

	private static final Set<Class<?>> FIELD_TYPES;

	static {
		// Create the set of field types that may be annotated with @DateTimeFormat.
		Set<Class<?>> fieldTypes = new HashSet<>(8);
		fieldTypes.add(LocalDate.class);
		fieldTypes.add(LocalTime.class);
		fieldTypes.add(LocalDateTime.class);
		fieldTypes.add(ZonedDateTime.class);
		fieldTypes.add(OffsetDateTime.class);
		fieldTypes.add(OffsetTime.class);
		FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
	}


	@Override
	public final Set<Class<?>> getFieldTypes() {
		return FIELD_TYPES;
	}

	@Override
	public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);

		// Efficient ISO_LOCAL_* variants for printing since they are twice as fast...
		if (formatter == DateTimeFormatter.ISO_DATE) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_DATE;
			}
		}
		else if (formatter == DateTimeFormatter.ISO_TIME) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_TIME;
			}
		}
		else if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
			if (isLocal(fieldType)) {
				formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			}
		}

		return new TemporalAccessorPrinter(formatter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatter formatter = getFormatter(annotation, fieldType);

		List<String> resolvedFallbackPatterns = new ArrayList<>();
		for (String fallbackPattern : annotation.fallbackPatterns()) {
			String resolvedFallbackPattern = resolveEmbeddedValue(fallbackPattern);
			if (StringUtils.hasLength(resolvedFallbackPattern)) {
				resolvedFallbackPatterns.add(resolvedFallbackPattern);
			}
		}

		return new TemporalAccessorParser((Class<? extends TemporalAccessor>) fieldType,
				formatter, resolvedFallbackPatterns.toArray(new String[0]), annotation);
	}

	/**
	 * 用于创建 {@link DateTimeFormatter} 的工厂方法。
	 * @param annotation 该字段的格式注解
	 * @param fieldType 字段的声明类型
	 * @return {@link DateTimeFormatter} 实例
	 */
	protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
		DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
		String style = resolveEmbeddedValue(annotation.style());
		if (StringUtils.hasLength(style)) {
			factory.setStylePattern(style);
		}
		factory.setIso(annotation.iso());
		String pattern = resolveEmbeddedValue(annotation.pattern());
		if (StringUtils.hasLength(pattern)) {
			factory.setPattern(pattern);
		}
		return factory.createDateTimeFormatter();
	}

	private boolean isLocal(Class<?> fieldType) {
		return fieldType.getSimpleName().startsWith("Local");
	}

}
