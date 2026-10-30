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

package org.springframework.context.support;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 消息源实现的基类，提供诸如 {@link java.text.MessageFormat} 处理之类的基础设施支持，但不实现
 * {@link org.springframework.context.MessageSource} 中定义的具体方法。
 *
 * <p>{@link AbstractMessageSource} 派生自该类，提供具体的 {@code getMessage} 实现，这些实现将消息
 * 代码的解析委托给一个中心模板方法。
 *
 * @author Juergen Hoeller
 * @since 2.5.5
 */
public abstract class MessageSourceSupport {

	private static final MessageFormat INVALID_MESSAGE_FORMAT = new MessageFormat("");

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean alwaysUseMessageFormat = false;

	/**
	 * 用于保存每条消息已生成的 MessageFormat 的缓存。用于传入的默认消息。已解析代码对应的
	 * MessageFormat 由子类以特定方式缓存。
	 */
	private final Map<String, Map<Locale, MessageFormat>> messageFormatsPerMessage = new HashMap<>();


	/**
	 * 设置是否始终应用 {@code MessageFormat} 规则，即使消息不带参数也对其进行解析。
	 * <p>默认值为 {@code false}：不带参数的消息默认按原样返回，不通过 {@code MessageFormat} 解析。将此
	 * 值设为 {@code true} 可对所有消息强制使用 {@code MessageFormat}，此时要求所有消息文本都按
	 * {@code MessageFormat} 的转义规则编写。
	 * <p>例如，{@code MessageFormat} 要求将单引号转义为两个相邻的单引号（{@code "''"}）。如果你的消息
	 * 文本全部采用这种转义写法，即使没有定义参数占位符，也需要将此标志设为 {@code true}。否则，只有带
	 * 实际参数的消息文本才应使用 {@code MessageFormat} 转义。
	 * @see java.text.MessageFormat
	 */
	public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
		this.alwaysUseMessageFormat = alwaysUseMessageFormat;
	}

	/**
	 * 返回是否始终应用 {@code MessageFormat} 规则，即使消息不带参数也对其进行解析。
	 */
	protected boolean isAlwaysUseMessageFormat() {
		return this.alwaysUseMessageFormat;
	}


	/**
	 * 渲染给定的默认消息字符串。默认消息由调用方按指定方式传入，可被渲染为呈现给用户的完整格式化默认消
	 * 息。
	 * <p>默认实现将该字符串传给 {@code formatMessage}，解析其中找到的所有参数占位符。子类可覆盖此方法
	 * 以插入对默认消息的自定义处理。
	 * @param defaultMessage 传入的默认消息 String
	 * @param args 用于填充消息内参数的参数数组；如果没有则为 {@code null}。
	 * @param locale 用于格式化的 Locale
	 * @return 渲染后的默认消息（参数已解析）
	 * @see #formatMessage(String, Object[], java.util.Locale)
	 */
	protected String renderDefaultMessage(String defaultMessage, @Nullable Object[] args, Locale locale) {
		return formatMessage(defaultMessage, args, locale);
	}

	/**
	 * 使用缓存的 MessageFormat 格式化给定的消息字符串。默认情况下会针对传入的默认消息调用，以解析其中
	 * 找到的所有参数占位符。
	 * @param msg 要格式化的消息
	 * @param args 用于填充消息内参数的参数数组；如果没有则为 {@code null}
	 * @param locale 用于格式化的 Locale
	 * @return 格式化后的消息（参数已解析）
	 */
	protected String formatMessage(String msg, @Nullable Object[] args, Locale locale) {
		if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
			return msg;
		}
		MessageFormat messageFormat = null;
		synchronized (this.messageFormatsPerMessage) {
			Map<Locale, MessageFormat> messageFormatsPerLocale = this.messageFormatsPerMessage.get(msg);
			if (messageFormatsPerLocale != null) {
				messageFormat = messageFormatsPerLocale.get(locale);
			}
			else {
				messageFormatsPerLocale = new HashMap<>();
				this.messageFormatsPerMessage.put(msg, messageFormatsPerLocale);
			}
			if (messageFormat == null) {
				try {
					messageFormat = createMessageFormat(msg, locale);
				}
				catch (IllegalArgumentException ex) {
					// Invalid message format - probably not intended for formatting,
					// rather using a message structure with no arguments involved...
					if (isAlwaysUseMessageFormat()) {
						throw ex;
					}
					// Silently proceed with raw message if format not enforced...
					messageFormat = INVALID_MESSAGE_FORMAT;
				}
				messageFormatsPerLocale.put(locale, messageFormat);
			}
		}
		if (messageFormat == INVALID_MESSAGE_FORMAT) {
			return msg;
		}
		synchronized (messageFormat) {
			return messageFormat.format(resolveArguments(args, locale));
		}
	}

	/**
	 * 为给定的消息和 Locale 创建一个 {@code MessageFormat}。
	 * @param msg 要为其创建 {@code MessageFormat} 的消息
	 * @param locale 要为其创建 {@code MessageFormat} 的 Locale
	 * @return {@code MessageFormat} 实例
	 */
	protected MessageFormat createMessageFormat(String msg, Locale locale) {
		return new MessageFormat(msg, locale);
	}

	/**
	 * 用于解析参数对象的模板方法。
	 * <p>默认实现只是原样返回给定的参数数组。可在子类中覆盖，以解析特殊类型的参数。
	 * @param args 原始参数数组
	 * @param locale 用于解析的 Locale
	 * @return 解析后的参数数组
	 */
	protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
		return (args != null ? args : new Object[0]);
	}

}
