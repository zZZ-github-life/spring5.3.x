/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.format.support;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link FormattingConversionService} 的专用版本，默认配置了适用于大多数应用的转换器和格式化器。
 *
 * <p>设计用于直接实例化，但也公开了静态工具方法 {@link #addDefaultFormatters}，以便针对任意
 * {@code FormatterRegistry} 实例临时使用，就像 {@code DefaultConversionService} 公开其自身的
 * {@link DefaultConversionService#addDefaultConverters addDefaultConverters} 方法一样。
 *
 * <p>自动为 JSR-354 Money &amp; Currency、JSR-310 Date-Time 和/或 Joda-Time 2.x 注册格式化器，具体
 * 取决于类路径上是否存在相应的 API。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class DefaultFormattingConversionService extends FormattingConversionService {

	private static final boolean jsr354Present;

	private static final boolean jodaTimePresent;

	static {
		ClassLoader classLoader = DefaultFormattingConversionService.class.getClassLoader();
		jsr354Present = ClassUtils.isPresent("javax.money.MonetaryAmount", classLoader);
		jodaTimePresent = ClassUtils.isPresent("org.joda.time.YearMonth", classLoader);
	}

	/**
	 * 创建一个新的 {@code DefaultFormattingConversionService}，其中包含
	 * {@linkplain DefaultConversionService#addDefaultConverters 默认转换器} 和
	 * {@linkplain #addDefaultFormatters 默认格式化器}。
	 */
	public DefaultFormattingConversionService() {
		this(null, true);
	}

	/**
	 * 创建一个新的 {@code DefaultFormattingConversionService}，其中包含
	 * {@linkplain DefaultConversionService#addDefaultConverters 默认转换器}，并根据
	 * {@code registerDefaultFormatters} 的值包含 {@linkplain #addDefaultFormatters 默认格式化器}。
	 * @param registerDefaultFormatters whether to register default formatters
	 */
	public DefaultFormattingConversionService(boolean registerDefaultFormatters) {
		this(null, registerDefaultFormatters);
	}

	/**
	 * 创建一个新的 {@code DefaultFormattingConversionService}，其中包含
	 * {@linkplain DefaultConversionService#addDefaultConverters 默认转换器}，并根据
	 * {@code registerDefaultFormatters} 的值包含 {@linkplain #addDefaultFormatters 默认格式化器}。
	 * @param embeddedValueResolver delegated to {@link #setEmbeddedValueResolver(StringValueResolver)}
	 * prior to calling {@link #addDefaultFormatters}.
	 * @param registerDefaultFormatters whether to register default formatters
	 */
	public DefaultFormattingConversionService(
			@Nullable StringValueResolver embeddedValueResolver, boolean registerDefaultFormatters) {

		if (embeddedValueResolver != null) {
			setEmbeddedValueResolver(embeddedValueResolver);
		}
		DefaultConversionService.addDefaultConverters(this);
		if (registerDefaultFormatters) {
			addDefaultFormatters(this);
		}
	}


	/**
	 * 添加适用于大多数环境的格式化器：包括数字格式化器、JSR-354 Money &amp; Currency 格式化器、JSR-310
	 * Date-Time 和/或 Joda-Time 格式化器，具体取决于类路径上是否存在相应的 API。
	 * @param formatterRegistry the service to register default formatters with
	 */
	@SuppressWarnings("deprecation")
	public static void addDefaultFormatters(FormatterRegistry formatterRegistry) {
		// Default handling of number values
		formatterRegistry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());

		// Default handling of monetary values
		if (jsr354Present) {
			formatterRegistry.addFormatter(new CurrencyUnitFormatter());
			formatterRegistry.addFormatter(new MonetaryAmountFormatter());
			formatterRegistry.addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
		}

		// Default handling of date-time values

		// just handling JSR-310 specific date and time types
		new DateTimeFormatterRegistrar().registerFormatters(formatterRegistry);

		if (jodaTimePresent) {
			// handles Joda-specific types as well as Date, Calendar, Long
			new org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar().registerFormatters(formatterRegistry);
		}
		else {
			// regular DateFormat-based Date, Calendar, Long converters
			new DateFormatterRegistrar().registerFormatters(formatterRegistry);
		}
	}

}
