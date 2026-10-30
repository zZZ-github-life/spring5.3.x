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

package org.springframework.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * 简单的持有者类，将 LocaleContext 实例与当前线程关联。如果 {@code inheritable} 标志设置为
 * {@code true}，则当前线程派生的任何子线程都会继承该 LocaleContext。
 *
 * <p>在 Spring 中用作当前 Locale 的集中持有者，适用于任何需要的地方：例如在 MessageSourceAccessor
 * 中。DispatcherServlet 会自动在此暴露其当前 Locale。其他应用也可以暴露自己的 Locale，使
 * MessageSourceAccessor 之类的类自动使用该 Locale。
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @since 1.2
 * @see LocaleContext
 * @see org.springframework.context.support.MessageSourceAccessor
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public final class LocaleContextHolder {

	private static final ThreadLocal<LocaleContext> localeContextHolder =
			new NamedThreadLocal<>("LocaleContext");

	private static final ThreadLocal<LocaleContext> inheritableLocaleContextHolder =
			new NamedInheritableThreadLocal<>("LocaleContext");

	// Shared default locale at the framework level
	@Nullable
	private static Locale defaultLocale;

	// Shared default time zone at the framework level
	@Nullable
	private static TimeZone defaultTimeZone;


	private LocaleContextHolder() {
	}


	/**
	 * 重置当前线程的 LocaleContext。
	 */
	public static void resetLocaleContext() {
		localeContextHolder.remove();
		inheritableLocaleContextHolder.remove();
	}

	/**
	 * 将给定的 LocaleContext 与当前线程关联，<i>不</i>将其暴露为可由子线程继承。<p>给定的
	 * LocaleContext 可以是 {@link TimeZoneAwareLocaleContext}，其中包含带有相关时区信息的 locale。
	 * @param localeContext 当前 LocaleContext；若要重置线程绑定的上下文，则为 {@code null}
	 * @see SimpleLocaleContext
	 * @see SimpleTimeZoneAwareLocaleContext
	 */
	public static void setLocaleContext(@Nullable LocaleContext localeContext) {
		setLocaleContext(localeContext, false);
	}

	/**
	 * 将给定的 LocaleContext 与当前线程关联。<p>给定的 LocaleContext 可以是
	 * {@link TimeZoneAwareLocaleContext}，其中包含带有相关时区信息的 locale。
	 * @param localeContext 当前 LocaleContext；若要重置线程绑定的上下文，则为 {@code null}
	 * @param inheritable 是否将 LocaleContext 以可继承方式暴露给子线程（使用 {@link InheritableThreadLocal}）
	 * @see SimpleLocaleContext
	 * @see SimpleTimeZoneAwareLocaleContext
	 */
	public static void setLocaleContext(@Nullable LocaleContext localeContext, boolean inheritable) {
		if (localeContext == null) {
			resetLocaleContext();
		}
		else {
			if (inheritable) {
				inheritableLocaleContextHolder.set(localeContext);
				localeContextHolder.remove();
			}
			else {
				localeContextHolder.set(localeContext);
				inheritableLocaleContextHolder.remove();
			}
		}
	}

	/**
	 * 返回与当前线程关联的 LocaleContext（如果有）。
	 * @return 当前 LocaleContext；如果没有则为 {@code null}
	 */
	@Nullable
	public static LocaleContext getLocaleContext() {
		LocaleContext localeContext = localeContextHolder.get();
		if (localeContext == null) {
			localeContext = inheritableLocaleContextHolder.get();
		}
		return localeContext;
	}

	/**
	 * 将给定的 Locale 与当前线程关联，并保留可能已经设置的任何 TimeZone。<p>将为给定的 Locale 隐式创建
	 * LocaleContext，<i>不</i>将其暴露为可由子线程继承。
	 * @param locale 当前 Locale；若要重置线程绑定上下文的 Locale 部分，则为 {@code null}
	 * @see #setTimeZone(TimeZone)
	 * @see SimpleLocaleContext#SimpleLocaleContext(Locale)
	 */
	public static void setLocale(@Nullable Locale locale) {
		setLocale(locale, false);
	}

	/**
	 * 将给定的 Locale 与当前线程关联，并保留可能已经设置的任何 TimeZone。<p>将为给定的 Locale 隐式创建
	 * LocaleContext。
	 * @param locale 当前 Locale；若要重置线程绑定上下文的 Locale 部分，则为 {@code null}
	 * @param inheritable 是否将 LocaleContext 以可继承方式暴露给子线程（使用 {@link InheritableThreadLocal}）
	 * @see #setTimeZone(TimeZone, boolean)
	 * @see SimpleLocaleContext#SimpleLocaleContext(Locale)
	 */
	public static void setLocale(@Nullable Locale locale, boolean inheritable) {
		LocaleContext localeContext = getLocaleContext();
		TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext ?
				((TimeZoneAwareLocaleContext) localeContext).getTimeZone() : null);
		if (timeZone != null) {
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		}
		else if (locale != null) {
			localeContext = new SimpleLocaleContext(locale);
		}
		else {
			localeContext = null;
		}
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * 在框架级别设置共享的默认 locale，作为 JVM 范围内默认 locale 的替代方案。<p><b>注意：</b>这对于设
	 * 置不同于 JVM 范围内默认 locale 的应用级默认 locale 可能很有用。但是，这要求每个此类应用都针对本
	 * 地部署的 Spring Framework jar 运行。在这种情况下，不要将 Spring 作为服务器级别的共享库部署！
	 * @param locale 默认 Locale（若没有则为 {@code null}，使查找回退到 {@link Locale#getDefault()}）
	 * @since 4.3.5
	 * @see #getLocale()
	 * @see Locale#getDefault()
	 */
	public static void setDefaultLocale(@Nullable Locale locale) {
		LocaleContextHolder.defaultLocale = locale;
	}

	/**
	 * 返回与当前线程关联的 Locale（如果有），否则返回系统默认 Locale。本方法实际上是
	 * {@link java.util.Locale#getDefault()} 的替代品，能够选择性地遵循用户级 Locale 设置。<p>注意：此
	 * 方法会回退到共享的默认 Locale，该默认值可以位于框架级别，也可以位于 JVM 范围的系统级别。如果你想
	 * 检查原始的 LocaleContext 内容（这些内容可能通过 {@code null} 表示没有特定 locale），请使用
	 * {@link #getLocaleContext()} 并调用 {@link LocaleContext#getLocale()}
	 * @return 当前 Locale；如果当前线程未关联特定 Locale，则为系统默认 Locale
	 * @see #getLocaleContext()
	 * @see LocaleContext#getLocale()
	 * @see #setDefaultLocale(Locale)
	 * @see java.util.Locale#getDefault()
	 */
	public static Locale getLocale() {
		return getLocale(getLocaleContext());
	}

	/**
	 * 返回与给定用户上下文关联的 Locale（如果有），否则返回系统默认 Locale。本方法实际上是
	 * {@link java.util.Locale#getDefault()} 的替代品，能够选择性地遵循用户级 Locale 设置。
	 * @param localeContext 要检查的用户级 Locale 上下文
	 * @return 当前 Locale；如果当前线程未关联特定 Locale，则为系统默认 Locale
	 * @since 5.0
	 * @see #getLocale()
	 * @see LocaleContext#getLocale()
	 * @see #setDefaultLocale(Locale)
	 * @see java.util.Locale#getDefault()
	 */
	public static Locale getLocale(@Nullable LocaleContext localeContext) {
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				return locale;
			}
		}
		return (defaultLocale != null ? defaultLocale : Locale.getDefault());
	}

	/**
	 * 将给定的 TimeZone 与当前线程关联，并保留可能已经设置的任何 Locale。<p>将为给定的 Locale 隐式创建
	 * LocaleContext，<i>不</i>将其暴露为可由子线程继承。
	 * @param timeZone 当前 TimeZone；若要重置线程绑定上下文的时区部分，则为 {@code null}
	 * @see #setLocale(Locale)
	 * @see SimpleTimeZoneAwareLocaleContext#SimpleTimeZoneAwareLocaleContext(Locale, TimeZone)
	 */
	public static void setTimeZone(@Nullable TimeZone timeZone) {
		setTimeZone(timeZone, false);
	}

	/**
	 * 将给定的 TimeZone 与当前线程关联，并保留可能已经设置的任何 Locale。<p>将为给定的 Locale 隐式创建
	 * LocaleContext。
	 * @param timeZone 当前 TimeZone；若要重置线程绑定上下文的时区部分，则为 {@code null}
	 * @param inheritable 是否将 LocaleContext 以可继承方式暴露给子线程（使用 {@link InheritableThreadLocal}）
	 * @see #setLocale(Locale, boolean)
	 * @see SimpleTimeZoneAwareLocaleContext#SimpleTimeZoneAwareLocaleContext(Locale, TimeZone)
	 */
	public static void setTimeZone(@Nullable TimeZone timeZone, boolean inheritable) {
		LocaleContext localeContext = getLocaleContext();
		Locale locale = (localeContext != null ? localeContext.getLocale() : null);
		if (timeZone != null) {
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		}
		else if (locale != null) {
			localeContext = new SimpleLocaleContext(locale);
		}
		else {
			localeContext = null;
		}
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * 在框架级别设置共享的默认时区，作为 JVM 范围内默认时区的替代方案。<p><b>注意：</b>这对于设置不同
	 * 于 JVM 范围内默认时区的应用级默认时区可能很有用。但是，这要求每个此类应用都针对本地部署的 Spring
	 * Framework jar 运行。在这种情况下，不要将 Spring 作为服务器级别的共享库部署！
	 * @param timeZone 默认时区（若没有则为 {@code null}，使查找回退到 {@link TimeZone#getDefault()}）
	 * @since 4.3.5
	 * @see #getTimeZone()
	 * @see TimeZone#getDefault()
	 */
	public static void setDefaultTimeZone(@Nullable TimeZone timeZone) {
		defaultTimeZone = timeZone;
	}

	/**
	 * 返回与当前线程关联的 TimeZone（如果有），否则返回系统默认 TimeZone。本方法实际上是
	 * {@link java.util.TimeZone#getDefault()} 的替代品，能够选择性地遵循用户级 TimeZone 设置。<p>注意：
	 * 此方法会回退到共享的默认 TimeZone，该默认值可以位于框架级别，也可以位于 JVM 范围的系统级别。如
	 * 果你想检查原始的 LocaleContext 内容（这些内容可能通过 {@code null} 表示没有特定时区），请使用
	 * {@link #getLocaleContext()}，并在向下转型为 {@link TimeZoneAwareLocaleContext} 后调用
	 * {@link TimeZoneAwareLocaleContext#getTimeZone()}。
	 * @return 当前 TimeZone；如果当前线程未关联特定 TimeZone，则为系统默认 TimeZone
	 * @see #getLocaleContext()
	 * @see TimeZoneAwareLocaleContext#getTimeZone()
	 * @see #setDefaultTimeZone(TimeZone)
	 * @see java.util.TimeZone#getDefault()
	 */
	public static TimeZone getTimeZone() {
		return getTimeZone(getLocaleContext());
	}

	/**
	 * 返回与给定用户上下文关联的 TimeZone（如果有），否则返回系统默认 TimeZone。本方法实际上是
	 * {@link java.util.TimeZone#getDefault()} 的替代品，能够选择性地遵循用户级 TimeZone 设置。
	 * @param localeContext 要检查的用户级 Locale 上下文
	 * @return 当前 TimeZone；如果当前线程未关联特定 TimeZone，则为系统默认 TimeZone
	 * @since 5.0
	 * @see #getTimeZone()
	 * @see TimeZoneAwareLocaleContext#getTimeZone()
	 * @see #setDefaultTimeZone(TimeZone)
	 * @see java.util.TimeZone#getDefault()
	 */
	public static TimeZone getTimeZone(@Nullable LocaleContext localeContext) {
		if (localeContext instanceof TimeZoneAwareLocaleContext) {
			TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			if (timeZone != null) {
				return timeZone;
			}
		}
		return (defaultTimeZone != null ? defaultTimeZone : TimeZone.getDefault());
	}

}
