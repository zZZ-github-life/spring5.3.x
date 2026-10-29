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

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段或方法参数应按日期或时间格式化。
 *
 * <p>支持按样式模式、ISO 日期时间模式或自定义格式模式字符串进行格式化。可应用于
 * {@link java.util.Date}、{@link java.util.Calendar}、{@link Long}（用于毫秒时间戳）以及 JSR-310
 * {@code java.time} 值类型。
 *
 * <p>对于基于样式的格式化，将 {@link #style} 属性设置为所需的样式模式代码。代码的第一个字符是日期
 * 样式，第二个字符是时间样式。使用字符 'S' 表示短样式，'M' 表示中等样式，'L' 表示长样式，'F' 表示
 * 完整样式。可以通过指定样式字符 '-' &mdash; 来省略日期或时间；例如，'M-' 指定不含时间的中等日期格
 * 式。
 *
 * <p>对于基于 ISO 的格式化，将 {@link #iso} 属性设置为所需的 {@link ISO} 格式，例如
 * {@link ISO#DATE}。
 *
 * <p>对于自定义格式化，将 {@link #pattern} 属性设置为日期时间模式，例如
 * {@code "yyyy/MM/dd hh:mm:ss a"}。
 *
 * <p>各属性互斥，因此每个注解实例只应设置一个属性（即最适合你格式化需求的那个）。
 *
 * <ul>
 * <li>指定 pattern 属性时，它的优先级高于 style 和 ISO 属性。</li>
 * <li>指定 {@link #iso} 属性时，它的优先级高于 style 属性。</li>
 * <li>未指定任何注解属性时，默认应用基于样式的格式，样式代码为 'SS'（短日期、短时间）。</li>
 * </ul>
 *
 * <h3>时区</h3>
 * <p>每当使用 {@link #style} 或 {@link #pattern} 属性时，格式化 {@link java.util.Date} 值都会使用
 * JVM 的 {@linkplain java.util.TimeZone#getDefault() 默认时区}。每当使用 {@link #iso} 属性格式化
 * {@link java.util.Date} 值时，将使用 {@code UTC} 作为时区。同样的时区也会应用于任何
 * {@linkplain #fallbackPatterns 回退模式}。为了强制一致地使用 {@code UTC} 作为时区，可以在启动 JVM
 * 时指定 {@code -Duser.timezone=UTC}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see java.time.format.DateTimeFormatter
 * @see org.joda.time.format.DateTimeFormat
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DateTimeFormat {

	/**
	 * 用于格式化字段或方法参数的样式模式。
	 * <p>默认值为 'SS'，即短日期、短时间。如果希望按照默认样式之外的常见样式格式化字段或方法参数，请设
	 * 置此属性。
	 * @see #fallbackPatterns
	 */
	String style() default "SS";

	/**
	 * 用于格式化字段或方法参数的 ISO 模式。
	 * <p>支持的 ISO 模式在 {@link ISO} 枚举中定义。
	 * <p>默认值为 {@link ISO#NONE}，表示应忽略此属性。如果希望按照 ISO 格式格式化字段或方法参数，请设
	 * 置此属性。
	 * @see #fallbackPatterns
	 */
	ISO iso() default ISO.NONE;

	/**
	 * 用于格式化字段或方法参数的自定义模式。
	 * <p>默认值为空字符串，表示未指定自定义模式字符串。如果希望按照样式或 ISO 格式无法表示的自定义日期
	 * 时间模式格式化字段或方法参数，请设置此属性。
	 * <p>注意：此模式沿用原 {@link java.text.SimpleDateFormat} 风格（Joda-Time 也支持该风格），对溢出
	 * 采用严格解析语义（例如拒绝非闰年的 2 月 29 日）。因此，'yy' 字符表示传统风格中的年份，而不是
	 * {@link java.time.format.DateTimeFormatter} 规范中的“纪元年份”（即在使用严格解析模式的
	 * {@code DateTimeFormatter} 处理时，'yy' 会变成 'uu'）。
	 * @see #fallbackPatterns
	 */
	String pattern() default "";

	/**
	 * 在主要 {@link #pattern}、{@link #iso} 或 {@link #style} 属性解析失败时用作回退的自定义模式集合。
	 * <p>例如，如果希望使用 ISO 日期格式进行解析和打印，但允许对用户输入的各种日期格式进行宽松解析，可
	 * 以配置类似下面的内容。
	 * <pre style="code">
	 * {@literal @}DateTimeFormat(iso = ISO.DATE, fallbackPatterns = { "M/d/yy", "dd.MM.yyyy" })
	 * </pre>
	 * <p>回退模式仅用于解析，不用于将值打印为字符串。打印时始终使用主要的 {@link #pattern}、
	 * {@link #iso} 或 {@link #style} 属性。关于回退模式使用哪个时区，请参见
	 * {@linkplain DateTimeFormat 类级文档}。
	 * <p>Joda-Time 值类型不支持回退模式。
	 * @since 5.3.5
	 */
	String[] fallbackPatterns() default {};


	/**
	 * 常见的 ISO 日期时间格式模式。
	 */
	enum ISO {

		/**
		 * 最常见的 ISO 日期格式 {@code yyyy-MM-dd} &mdash; 例如 "2000-10-31"。
		 */
		DATE,

		/**
		 * 最常见的 ISO 时间格式 {@code HH:mm:ss.SSSXXX} &mdash; 例如 "01:30:00.000-05:00"。
		 */
		TIME,

		/**
		 * 最常见的 ISO 日期时间格式 {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX} &mdash; 例如
		 * "2000-10-31T01:30:00.000-05:00"。
		 */
		DATE_TIME,

		/**
		 * 表示不应应用任何基于 ISO 的格式模式。
		 */
		NONE
	}

}
