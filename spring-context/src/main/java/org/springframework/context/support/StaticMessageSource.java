/*
 * Copyright 2002-2019 the original author or authors.
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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.context.MessageSource} 的简单实现，允许以编程方式注册消息。此
 * MessageSource 支持基本的国际化。
 *
 * <p>旨在用于测试，而不是用于生产系统。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class StaticMessageSource extends AbstractMessageSource {

	private final Map<String, Map<Locale, MessageHolder>> messageMap = new HashMap<>();


	@Override
	@Nullable
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		Map<Locale, MessageHolder> localeMap = this.messageMap.get(code);
		if (localeMap == null) {
			return null;
		}
		MessageHolder holder = localeMap.get(locale);
		if (holder == null) {
			return null;
		}
		return holder.getMessage();
	}

	@Override
	@Nullable
	protected MessageFormat resolveCode(String code, Locale locale) {
		Map<Locale, MessageHolder> localeMap = this.messageMap.get(code);
		if (localeMap == null) {
			return null;
		}
		MessageHolder holder = localeMap.get(locale);
		if (holder == null) {
			return null;
		}
		return holder.getMessageFormat();
	}

	/**
	 * 将给定消息与给定代码关联。
	 * @param code 查找代码
	 * @param locale 应在其内找到该消息的 Locale
	 * @param msg 与此查找代码关联的消息
	 */
	public void addMessage(String code, Locale locale, String msg) {
		Assert.notNull(code, "Code must not be null");
		Assert.notNull(locale, "Locale must not be null");
		Assert.notNull(msg, "Message must not be null");
		this.messageMap.computeIfAbsent(code, key -> new HashMap<>(4)).put(locale, new MessageHolder(msg, locale));
		if (logger.isDebugEnabled()) {
			logger.debug("Added message [" + msg + "] for code [" + code + "] and Locale [" + locale + "]");
		}
	}

	/**
	 * 将给定的消息值与作为代码的给定键关联。
	 * @param messages 要注册的消息，以消息代码为键、消息文本为值
	 * @param locale 应在其内找到这些消息的 Locale
	 */
	public void addMessages(Map<String, String> messages, Locale locale) {
		Assert.notNull(messages, "Messages Map must not be null");
		messages.forEach((code, msg) -> addMessage(code, locale, msg));
	}


	@Override
	public String toString() {
		return getClass().getName() + ": " + this.messageMap;
	}


	private class MessageHolder {

		private final String message;

		private final Locale locale;

		@Nullable
		private volatile MessageFormat cachedFormat;

		public MessageHolder(String message, Locale locale) {
			this.message = message;
			this.locale = locale;
		}

		public String getMessage() {
			return this.message;
		}

		public MessageFormat getMessageFormat() {
			MessageFormat messageFormat = this.cachedFormat;
			if (messageFormat == null) {
				messageFormat = createMessageFormat(this.message, this.locale);
				this.cachedFormat = messageFormat;
			}
			return messageFormat;
		}

		@Override
		public String toString() {
			return this.message;
		}
	}

}
