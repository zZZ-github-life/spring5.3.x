/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.format.number;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 用于百分比样式数值的格式化器。
 *
 * <p>委托给 {@link java.text.NumberFormat#getPercentInstance(Locale)}。配置 BigDecimal 解析，因此
 * 不会损失精度。{@link #parse(String, Locale)} 方法始终返回 BigDecimal。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setLenient
 */
public class PercentStyleFormatter extends AbstractNumberFormatter {

	@Override
	protected NumberFormat getNumberFormat(Locale locale) {
		NumberFormat format = NumberFormat.getPercentInstance(locale);
		if (format instanceof DecimalFormat) {
			((DecimalFormat) format).setParseBigDecimal(true);
		}
		return format;
	}

}
