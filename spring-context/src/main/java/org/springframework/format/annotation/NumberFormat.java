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

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明字段或方法参数应按数字格式化。
 *
 * <p>支持按样式或自定义模式字符串进行格式化。可应用于任何 JDK {@code Number} 类型，如
 * {@code Double} 和 {@code Long}。
 *
 * <p>对于基于样式的格式化，将 {@link #style} 属性设置为所需的 {@link Style}。对于自定义格式化，将
 * {@link #pattern} 属性设置为数字模式，如 {@code #, ###.##}。
 *
 * <p>各属性互斥，因此每个注解实例只应设置一个属性（即最适合你格式化需求的那个）。如果指定了
 * {@link #pattern} 属性，它将优先于 {@link #style} 属性。如果未指定任何注解属性，则根据被标注的字
 * 段或方法参数类型，默认应用基于样式的数字格式或货币格式。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see java.text.NumberFormat
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface NumberFormat {

	/**
	 * 用于格式化字段的样式模式。
	 * <p>对于大多数被标注类型，默认值为 {@link Style#DEFAULT}，即通用数字格式；货币类型除外，默认使用
	 * 货币格式。如果希望按照默认样式之外的常见样式格式化字段，请设置此属性。
	 */
	Style style() default Style.DEFAULT;

	/**
	 * 用于格式化字段的自定义模式。
	 * <p>默认值为空字符串，表示未指定自定义模式字符串。如果希望按照样式无法表示的自定义数字模式格式化
	 * 字段，请设置此属性。
	 */
	String pattern() default "";


	/**
	 * 常见的数字格式样式。
	 */
	enum Style {

		/**
		 * 被标注类型的默认格式：通常是 'number'，但对于货币类型可能是 'currency'（例如
		 * {@code javax.money.MonetaryAmount)}。
		 * @since 4.2
		 */
		DEFAULT,

		/**
		 * 当前区域设置的通用数字格式。
		 */
		NUMBER,

		/**
		 * 当前区域设置的百分比格式。
		 */
		PERCENT,

		/**
		 * 当前区域设置的货币格式。
		 */
		CURRENCY
	}

}
