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

package org.springframework.format.datetime.joda;

import java.util.Locale;

import org.joda.time.format.DateTimeFormatter;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * 线程本地 {@link JodaTimeContext} 的持有者，包含用户特定的 Joda-Time 设置。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @deprecated as of 5.3, in favor of standard JSR-310 support
 */
@Deprecated
public final class JodaTimeContextHolder {

	private static final ThreadLocal<JodaTimeContext> jodaTimeContextHolder =
			new NamedThreadLocal<>("JodaTimeContext");


	private JodaTimeContextHolder() {
	}


	/**
	 * 重置当前线程的 JodaTimeContext。
	 */
	public static void resetJodaTimeContext() {
		jodaTimeContextHolder.remove();
	}

	/**
	 * 将给定的 JodaTimeContext 关联到当前线程。
	 * @param jodaTimeContext the current JodaTimeContext,
	 * or {@code null} to reset the thread-bound context
	 */
	public static void setJodaTimeContext(@Nullable JodaTimeContext jodaTimeContext) {
		if (jodaTimeContext == null) {
			resetJodaTimeContext();
		}
		else {
			jodaTimeContextHolder.set(jodaTimeContext);
		}
	}

	/**
	 * 返回与当前线程关联的 JodaTimeContext（如果有）。
	 * @return the current JodaTimeContext, or {@code null} if none
	 */
	@Nullable
	public static JodaTimeContext getJodaTimeContext() {
		return jodaTimeContextHolder.get();
	}


	/**
	 * 获取应用了用户特定设置的 DateTimeFormatter，作用于给定的基础 Formatter。
	 * @param formatter the base formatter that establishes default formatting rules
	 * (generally user independent)
	 * @param locale the current user locale (may be {@code null} if not known)
	 * @return the user-specific DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, @Nullable Locale locale) {
		DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
		JodaTimeContext context = getJodaTimeContext();
		return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
	}

}
