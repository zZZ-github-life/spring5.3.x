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

package org.springframework.format.number;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;

import org.springframework.lang.Nullable;

/**
 * 用于货币样式数值的 BigDecimal 格式化器。
 *
 * <p>委托给 {@link java.text.NumberFormat#getCurrencyInstance(Locale)}。配置 BigDecimal 解析，因此
 * 不会损失精度。可以将指定的 {@link java.math.RoundingMode} 应用于解析后的值。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 4.2
 * @see #setLenient
 * @see #setRoundingMode
 */
public class CurrencyStyleFormatter extends AbstractNumberFormatter {

	private int fractionDigits = 2;

	@Nullable
	private RoundingMode roundingMode;

	@Nullable
	private Currency currency;

	@Nullable
	private String pattern;


	/**
	 * 指定所需的小数位数。默认为 2。
	 */
	public void setFractionDigits(int fractionDigits) {
		this.fractionDigits = fractionDigits;
	}

	/**
	 * 指定用于十进制解析的舍入模式。默认为 {@link java.math.RoundingMode#UNNECESSARY}。
	 */
	public void setRoundingMode(RoundingMode roundingMode) {
		this.roundingMode = roundingMode;
	}

	/**
	 * 指定货币（如果已知）。
	 */
	public void setCurrency(Currency currency) {
		this.currency = currency;
	}

	/**
	 * 指定用于格式化数值的模式。如果未指定，则使用默认的 DecimalFormat 模式。
	 * @see java.text.DecimalFormat#applyPattern(String)
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}


	@Override
	public BigDecimal parse(String text, Locale locale) throws ParseException {
		BigDecimal decimal = (BigDecimal) super.parse(text, locale);
		if (this.roundingMode != null) {
			decimal = decimal.setScale(this.fractionDigits, this.roundingMode);
		}
		else {
			decimal = decimal.setScale(this.fractionDigits);
		}
		return decimal;
	}

	@Override
	protected NumberFormat getNumberFormat(Locale locale) {
		DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
		format.setParseBigDecimal(true);
		format.setMaximumFractionDigits(this.fractionDigits);
		format.setMinimumFractionDigits(this.fractionDigits);
		if (this.roundingMode != null) {
			format.setRoundingMode(this.roundingMode);
		}
		if (this.currency != null) {
			format.setCurrency(this.currency);
		}
		if (this.pattern != null) {
			format.applyPattern(this.pattern);
		}
		return format;
	}

}
