/*
 * Copyright 2002-2016 the original author or authors.
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

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.Formatter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 在 {@link Formatter} 与 {@link PropertyEditor} 之间进行桥接的适配器。
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class FormatterPropertyEditorAdapter extends PropertyEditorSupport {

	private final Formatter<Object> formatter;


	/**
	 * 为给定的 {@link Formatter} 创建一个新的 {@code FormatterPropertyEditorAdapter}。
	 * @param formatter the {@link Formatter} to wrap
	 */
	@SuppressWarnings("unchecked")
	public FormatterPropertyEditorAdapter(Formatter<?> formatter) {
		Assert.notNull(formatter, "Formatter must not be null");
		this.formatter = (Formatter<Object>) formatter;
	}


	/**
	 * 确定 {@link Formatter} 所声明的字段类型。
	 * @return the field type declared in the wrapped {@link Formatter} implementation
	 * (never {@code null})
	 * @throws IllegalArgumentException if the {@link Formatter}-declared field type
	 * cannot be inferred
	 */
	public Class<?> getFieldType() {
		return FormattingConversionService.getFieldType(this.formatter);
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			try {
				setValue(this.formatter.parse(text, LocaleContextHolder.getLocale()));
			}
			catch (IllegalArgumentException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? this.formatter.print(value, LocaleContextHolder.getLocale()) : "");
	}

}
