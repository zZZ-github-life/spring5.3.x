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

package org.springframework.context.support;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;

/**
 * 辅助类，用于便捷地访问 MessageSource 中的消息，提供各种重载的 getMessage 方法。
 *
 * <p>可从 ApplicationObjectSupport 获取，但也可以作为独立辅助类在应用程序对象中委托使用。
 *
 * @author Juergen Hoeller
 * @since 23.10.2003
 * @see ApplicationObjectSupport#getMessageSourceAccessor
 */
public class MessageSourceAccessor {

	private final MessageSource messageSource;

	@Nullable
	private final Locale defaultLocale;


	/**
	 * 创建一个新的 MessageSourceAccessor，使用 LocaleContextHolder 的区域设置作为默认区域设置。
	 * @param messageSource the MessageSource to wrap
	 * @see org.springframework.context.i18n.LocaleContextHolder#getLocale()
	 */
	public MessageSourceAccessor(MessageSource messageSource) {
		this.messageSource = messageSource;
		this.defaultLocale = null;
	}

	/**
	 * 创建一个新的 MessageSourceAccessor，使用给定的默认区域设置。
	 * @param messageSource the MessageSource to wrap
	 * @param defaultLocale the default locale to use for message access
	 */
	public MessageSourceAccessor(MessageSource messageSource, Locale defaultLocale) {
		this.messageSource = messageSource;
		this.defaultLocale = defaultLocale;
	}


	/**
	 * 返回在未给出显式区域设置时要使用的默认区域设置。<p>默认实现返回传入相应构造函数的默认区域设置，
	 * 或回退到 LocaleContextHolder 的区域设置。可以在子类中覆盖。
	 * @see #MessageSourceAccessor(org.springframework.context.MessageSource, java.util.Locale)
	 * @see org.springframework.context.i18n.LocaleContextHolder#getLocale()
	 */
	protected Locale getDefaultLocale() {
		return (this.defaultLocale != null ? this.defaultLocale : LocaleContextHolder.getLocale());
	}

	/**
	 * 获取给定代码和默认区域设置对应的消息。
	 * @param code the code of the message
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, String defaultMessage) {
		String msg = this.messageSource.getMessage(code, null, defaultMessage, getDefaultLocale());
		return (msg != null ? msg : "");
	}

	/**
	 * 获取给定代码和给定区域设置对应的消息。
	 * @param code the code of the message
	 * @param defaultMessage the String to return if the lookup fails
	 * @param locale the Locale in which to do lookup
	 * @return the message
	 */
	public String getMessage(String code, String defaultMessage, Locale locale) {
		String msg = this.messageSource.getMessage(code, null, defaultMessage, locale);
		return (msg != null ? msg : "");
	}

	/**
	 * 获取给定代码和默认区域设置对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @return the message
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
		String msg = this.messageSource.getMessage(code, args, defaultMessage, getDefaultLocale());
		return (msg != null ? msg : "");
	}

	/**
	 * 获取给定代码和给定区域设置对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param defaultMessage the String to return if the lookup fails
	 * @param locale the Locale in which to do lookup
	 * @return the message
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage, Locale locale) {
		String msg = this.messageSource.getMessage(code, args, defaultMessage, locale);
		return (msg != null ? msg : "");
	}

	/**
	 * 获取给定代码和默认区域设置对应的消息。
	 * @param code the code of the message
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, null, getDefaultLocale());
	}

	/**
	 * 获取给定代码和给定区域设置对应的消息。
	 * @param code the code of the message
	 * @param locale the Locale in which to do lookup
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, null, locale);
	}

	/**
	 * 获取给定代码和默认区域设置对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, getDefaultLocale());
	}

	/**
	 * 获取给定代码和给定区域设置对应的消息。
	 * @param code the code of the message
	 * @param args arguments for the message, or {@code null} if none
	 * @param locale the Locale in which to do lookup
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, locale);
	}

	/**
	 * 在默认区域设置中获取给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable the MessageSourceResolvable
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, getDefaultLocale());
	}

	/**
	 * 在给定区域设置中获取给定的 MessageSourceResolvable（例如 ObjectError 实例）。
	 * @param resolvable the MessageSourceResolvable
	 * @param locale the Locale in which to do lookup
	 * @return the message
	 * @throws org.springframework.context.NoSuchMessageException if not found
	 */
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, locale);
	}

}
