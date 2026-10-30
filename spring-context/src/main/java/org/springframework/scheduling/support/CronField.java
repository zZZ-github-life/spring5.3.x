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

package org.springframework.scheduling.support;

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;
import java.util.function.BiFunction;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * cron 模式中的单个字段。使用 {@code parse*} 方法创建，主要且唯一的入口点是
 * {@link #nextOrSame(Temporal)}。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class CronField {

	private static final String[] MONTHS = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
			"OCT", "NOV", "DEC"};

	private static final String[] DAYS = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

	private final Type type;


	protected CronField(Type type) {
		this.type = type;
	}

	/**
	 * 返回一个对 0 纳秒启用的 {@code CronField}。
	 */
	public static CronField zeroNanos() {
		return BitsCronField.zeroNanos();
	}

	/**
	 * 将给定值解析为秒的 {@code CronField}，即 cron 表达式的第一个字段。
	 */
	public static CronField parseSeconds(String value) {
		return BitsCronField.parseSeconds(value);
	}

	/**
	 * 将给定值解析为分钟的 {@code CronField}，即 cron 表达式的第二个字段。
	 */
	public static CronField parseMinutes(String value) {
		return BitsCronField.parseMinutes(value);
	}

	/**
	 * 将给定值解析为小时的 {@code CronField}，即 cron 表达式的第三个字段。
	 */
	public static CronField parseHours(String value) {
		return BitsCronField.parseHours(value);
	}

	/**
	 * 将给定值解析为月份的日的 {@code CronField}，即 cron 表达式的第四个字段。
	 */
	public static CronField parseDaysOfMonth(String value) {
		if (!QuartzCronField.isQuartzDaysOfMonthField(value)) {
			return BitsCronField.parseDaysOfMonth(value);
		}
		else {
			return parseList(value, Type.DAY_OF_MONTH, (field, type) -> {
				if (QuartzCronField.isQuartzDaysOfMonthField(field)) {
					return QuartzCronField.parseDaysOfMonth(field);
				}
				else {
					return BitsCronField.parseDaysOfMonth(field);
				}
			});
		}
	}

	/**
	 * 将给定值解析为月的 {@code CronField}，即 cron 表达式的第五个字段。
	 */
	public static CronField parseMonth(String value) {
		value = replaceOrdinals(value, MONTHS);
		return BitsCronField.parseMonth(value);
	}

	/**
	 * 将给定值解析为星期的日的 {@code CronField}，即 cron 表达式的第六个字段。
	 */
	public static CronField parseDaysOfWeek(String value) {
		value = replaceOrdinals(value, DAYS);
		if (!QuartzCronField.isQuartzDaysOfWeekField(value)) {
			return BitsCronField.parseDaysOfWeek(value);
		}
		else {
			return parseList(value, Type.DAY_OF_WEEK, (field, type) -> {
				if (QuartzCronField.isQuartzDaysOfWeekField(field)) {
					return QuartzCronField.parseDaysOfWeek(field);
				}
				else {
					return BitsCronField.parseDaysOfWeek(field);
				}
			});
		}
	}


	private static CronField parseList(String value, Type type, BiFunction<String, Type, CronField> parseFieldFunction) {
		Assert.hasLength(value, "Value must not be empty");
		String[] fields = StringUtils.delimitedListToStringArray(value, ",");
		CronField[] cronFields = new CronField[fields.length];
		for (int i = 0; i < fields.length; i++) {
			cronFields[i] = parseFieldFunction.apply(fields[i], type);
		}
		return CompositeCronField.compose(cronFields, type, value);
	}

	private static String replaceOrdinals(String value, String[] list) {
		value = value.toUpperCase();
		for (int i = 0; i < list.length; i++) {
			String replacement = Integer.toString(i + 1);
			value = StringUtils.replace(value, list[i], replacement);
		}
		return value;
	}


	/**
	 * 获取此 cron 字段匹配序列中下一个或相同的 {@link Temporal}。
	 * @param temporal 种子值
	 * @return 与该模式匹配的下一个或同一 temporal
	 */
	@Nullable
	public abstract <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal);


	protected Type type() {
		return this.type;
	}

	@SuppressWarnings("unchecked")
	protected static <T extends Temporal & Comparable<? super T>> T cast(Temporal temporal) {
		return (T) temporal;
	}


	/**
	 * 表示 cron 字段的类型，即秒、分钟、小时、月中的日、月、星期几。
	 */
	protected enum Type {
		NANO(ChronoField.NANO_OF_SECOND, ChronoUnit.SECONDS),
		SECOND(ChronoField.SECOND_OF_MINUTE, ChronoUnit.MINUTES, ChronoField.NANO_OF_SECOND),
		MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		HOUR(ChronoField.HOUR_OF_DAY, ChronoUnit.DAYS, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_MONTH(ChronoField.DAY_OF_MONTH, ChronoUnit.MONTHS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		MONTH(ChronoField.MONTH_OF_YEAR, ChronoUnit.YEARS, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_WEEK(ChronoField.DAY_OF_WEEK, ChronoUnit.WEEKS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND);


		private final ChronoField field;

		private final ChronoUnit higherOrder;

		private final ChronoField[] lowerOrders;


		Type(ChronoField field, ChronoUnit higherOrder, ChronoField... lowerOrders) {
			this.field = field;
			this.higherOrder = higherOrder;
			this.lowerOrders = lowerOrders;
		}


		/**
		 * 返回此类型在给定 temporal 中的值。
		 * @return 此类型对应的值
		 */
		public int get(Temporal date) {
			return date.get(this.field);
		}

		/**
		 * 返回此类型的一般范围。例如，对于 {@link #MONTH}，此方法将返回 0-31。
		 * @return 此字段的范围
		 */
		public ValueRange range() {
			return this.field.range();
		}

		/**
		 * 检查给定值是否有效，即是否落在 {@linkplain #range() range} 中。
		 * @param value 要检查的值
		 * @return 传入的值
		 * @throws IllegalArgumentException 如果给定的值无效
		 */
		public int checkValidValue(int value) {
			if (this == DAY_OF_WEEK && value == 0) {
				return value;
			}
			else {
				try {
					return this.field.checkValidIntValue(value);
				}
				catch (DateTimeException ex) {
					throw new IllegalArgumentException(ex.getMessage(), ex);
				}
			}
		}

		/**
		 * 使给定的 temporal 按照此字段当前值与目标值之间的差值向前推进。通常，返回的 temporal 会以给定目标
		 * 作为此类型的当前值，但对于 {@link #DAY_OF_MONTH} 并非如此。
		 * @param temporal 要推进的 temporal
		 * @param goal 目标值
		 * @param <T> temporal 的类型
		 * @return 推进后的 temporal，通常此类型的值为 {@code goal}。
		 */
		public <T extends Temporal & Comparable<? super T>> T elapseUntil(T temporal, int goal) {
			int current = get(temporal);
			ValueRange range = temporal.range(this.field);
			if (current < goal) {
				if (range.isValidIntValue(goal)) {
					return cast(temporal.with(this.field, goal));
				}
				else {
					// goal is invalid, eg. 29th Feb, so roll forward
					long amount = range.getMaximum() - current + 1;
					return this.field.getBaseUnit().addTo(temporal, amount);
				}
			}
			else {
				long amount = goal + range.getMaximum() - current + 1 - range.getMinimum();
				return this.field.getBaseUnit().addTo(temporal, amount);
			}
		}

		/**
		 * 将给定的 temporal 向前滚动，直到到达下一个更高阶的字段。调用此方法等价于调用
		 * {@link #elapseUntil(Temporal, int)}，并将目标设为该字段范围的最小值。
		 * @param temporal 要向前滚动的 temporal
		 * @param <T> temporal 的类型
		 * @return 向前滚动后的 temporal
		 */
		public <T extends Temporal & Comparable<? super T>> T rollForward(T temporal) {
			T result = this.higherOrder.addTo(temporal, 1);
			ValueRange range = result.range(this.field);
			return this.field.adjustInto(result, range.getMinimum());
		}

		/**
		 * 将给定 temporal 的此字段及所有更低阶字段重置为其最小值。例如对于 {@link #MINUTE}，此方法会将纳秒、
		 * 秒<strong>和</strong>分钟重置为 0。
		 * @param temporal 要重置的 temporal
		 * @param <T> temporal 的类型
		 * @return 重置后的 temporal
		 */
		public <T extends Temporal> T reset(T temporal) {
			for (ChronoField lowerOrder : this.lowerOrders) {
				if (temporal.isSupported(lowerOrder)) {
					temporal = lowerOrder.adjustInto(temporal, temporal.range(lowerOrder).getMinimum());
				}
			}
			return temporal;
		}

		@Override
		public String toString() {
			return this.field.toString();
		}
	}

}
