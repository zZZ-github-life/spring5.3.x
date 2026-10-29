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

import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;

/**
 * 用于保存用户特定的 <code>java.time</code>（JSR-310）设置的上下文，例如用户的 Chronology（日历系
 * 统）和时区。
 * <p>{@code null} 属性值表示用户未指定该设置。
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see DateTimeContextHolder
 */
public class DateTimeContext {

	@Nullable
	private Chronology chronology;

	@Nullable
	private ZoneId timeZone;


	/**
	 * 设置用户的 Chronology（日历系统）。
	 */
	public void setChronology(@Nullable Chronology chronology) {
		this.chronology = chronology;
	}

	/**
	 * 返回用户的 Chronology（日历系统）（如果有）。
	 */
	@Nullable
	public Chronology getChronology() {
		return this.chronology;
	}

	/**
	 * 设置用户的时区。
	 * <p>或者，在 {@link LocaleContextHolder} 上设置 {@link TimeZoneAwareLocaleContext}。如果此处未提
	 * 供设置，此上下文类将回退为检查 locale 上下文。
	 * @see org.springframework.context.i18n.LocaleContextHolder#getTimeZone()
	 * @see org.springframework.context.i18n.LocaleContextHolder#setLocaleContext
	 */
	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * 返回用户的时区（如果有）。
	 */
	@Nullable
	public ZoneId getTimeZone() {
		return this.timeZone;
	}


	/**
	 * 获取将本上下文的设置应用到基础 {@code formatter} 后的 DateTimeFormatter。
	 * @param formatter the base formatter that establishes default
	 * formatting rules, generally context-independent
	 * @return the contextual DateTimeFormatter
	 */
	public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
		if (this.chronology != null) {
			formatter = formatter.withChronology(this.chronology);
		}
		if (this.timeZone != null) {
			formatter = formatter.withZone(this.timeZone);
		}
		else {
			LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
			if (localeContext instanceof TimeZoneAwareLocaleContext) {
				TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
				if (timeZone != null) {
					formatter = formatter.withZone(timeZone.toZoneId());
				}
			}
		}
		return formatter;
	}

}
