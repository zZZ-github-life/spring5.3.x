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

package org.springframework.format;

import java.lang.annotation.Annotation;

import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * 字段格式化逻辑的注册表。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface FormatterRegistry extends ConverterRegistry {

	/**
	 * 添加 Printer，用于打印特定类型的字段。字段类型由参数化的 Printer 实例推断得出。
	 * @param printer the printer to add
	 * @since 5.2
	 * @see #addFormatter(Formatter)
	 */
	void addPrinter(Printer<?> printer);

	/**
	 * 添加 Parser，用于解析特定类型的字段。字段类型由参数化的 Parser 实例推断得出。
	 * @param parser the parser to add
	 * @since 5.2
	 * @see #addFormatter(Formatter)
	 */
	void addParser(Parser<?> parser);

	/**
	 * 添加 Formatter，用于格式化特定类型的字段。字段类型由参数化的 Formatter 实例推断得出。
	 * @param formatter the formatter to add
	 * @since 3.1
	 * @see #addFormatterForFieldType(Class, Formatter)
	 */
	void addFormatter(Formatter<?> formatter);

	/**
	 * 添加 Formatter，用于格式化给定类型的字段。
	 * <p>打印时，如果 Formatter 的类型 T 已声明且 {@code fieldType} 不能赋值给 T，则在委托
	 * {@code formatter} 打印字段值之前会尝试强制转换为 T。解析时，如果 {@code formatter} 返回的解析结
	 * 果不能赋值给运行时字段类型，则在返回解析出的字段值之前会尝试强制转换为该字段类型。
	 * @param fieldType the field type to format
	 * @param formatter the formatter to add
	 */
	void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter);

	/**
	 * 添加 Printer/Parser 对，用于格式化特定类型的字段。该格式化器在打印时会委托给指定的
	 * {@code printer}，在解析时会委托给指定的 {@code parser}。
	 * <p>打印时，如果 Printer 的类型 T 已声明且 {@code fieldType} 不能赋值给 T，则在委托
	 * {@code printer} 打印字段值之前会尝试强制转换为 T。解析时，如果 Parser 返回的对象不能赋值给运行时
	 * 字段类型，则在返回解析出的字段值之前会尝试强制转换为该字段类型。
	 * @param fieldType the field type to format
	 * @param printer the printing part of the formatter
	 * @param parser the parsing part of the formatter
	 */
	void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser);

	/**
	 * 添加 Formatter，用于格式化标注了特定格式注解的字段。
	 * @param annotationFormatterFactory the annotation formatter factory to add
	 */
	void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory);

}
