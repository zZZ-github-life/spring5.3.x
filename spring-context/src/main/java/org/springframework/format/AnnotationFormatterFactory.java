/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.format;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 创建格式化器的工厂，用于格式化标注了特定 {@link Annotation} 的字段值。
 *
 * <p>例如，{@code DateTimeFormatAnnotationFormatterFactory} 可以创建格式化器，用于格式化标注了
 * {@code @DateTimeFormat} 的字段上设置的 {@code Date} 值。
 *
 * @author Keith Donald
 * @since 3.0
 * @param <A> the annotation type that should trigger formatting
 */
public interface AnnotationFormatterFactory<A extends Annotation> {

	/**
	 * 可以使用 &lt;A&gt; 注解标注的字段类型。
	 */
	Set<Class<?>> getFieldTypes();

	/**
	 * 获取 Printer，用于打印标注了 {@code annotation} 的 {@code fieldType} 类型字段的值。
	 * <p>如果 Printer 接受的类型 T 不能赋值给 {@code fieldType}，则在调用 Printer 之前会尝试将
	 * {@code fieldType} 强制转换为 T。
	 * @param annotation the annotation instance
	 * @param fieldType the type of field that was annotated
	 * @return the printer
	 */
	Printer<?> getPrinter(A annotation, Class<?> fieldType);

	/**
	 * 获取 Parser，用于解析标注了 {@code annotation} 的 {@code fieldType} 类型字段的提交值。
	 * <p>如果解析器返回的对象不能赋值给 {@code fieldType}，则在设置字段之前会尝试强制转换为
	 * {@code fieldType}。
	 * @param annotation the annotation instance
	 * @param fieldType the type of field that was annotated
	 * @return the parser
	 */
	Parser<?> getParser(A annotation, Class<?> fieldType);

}
