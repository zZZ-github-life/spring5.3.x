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

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * 用于线程本地用户 {@link DateTimeContext} 的持有者。
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see org.springframework.context.i18n.LocaleContextHolder
 */
public final class DateTimeContextHolder {

	private static final ThreadLocal<DateTimeContext> dateTimeContextHolder =
			new NamedThreadLocal<>("DateTimeContext");


	private DateTimeContextHolder() {
	}


	/**
	 * 重置当前线程的 DateTimeContext。
	 */
	public static void resetDateTimeContext() {
		dateTimeContextHolder.remove();
	}

	/**
	 * 将给定的 DateTimeContext 与当前线程关联。
	 * @param dateTimeContext the current DateTimeContext,
	 * or {@code null} to reset the thread-bound context
	 */
	public static void setDateTimeContext(@Nullable DateTimeContext dateTimeContext) {
		if (dateTimeContext == null) {
			resetDateTimeContext();
		}
		else {
			dateTimeContextHolder.set(dateTimeContext);
		}
	}

	/**
	 * 返回与当前线程关联的 DateTimeContext（如果有）。
	 * @return the current DateTimeContext, or {@code null} if none
	 */
	@Nullable
	public static DateTimeContext getDateTimeContext() {
		return dateTimeContextHolder.get();
	}

	/**
	 * 获取将用户特定设置应用到给定基础格式化器后的 DateTimeFormatter。
	 * @param formatter the base formatter that establishes default formatting rules
	 * (generally user independent)
	 * @param locale the current user locale (may be {@code null} if not known)
	 * @return the user-specific DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, @Nullable Locale locale) {
		DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
		DateTimeContext context = getDateTimeContext();
		return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
	}

}
