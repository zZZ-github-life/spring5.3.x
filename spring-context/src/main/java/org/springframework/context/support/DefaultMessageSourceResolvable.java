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

import java.io.Serializable;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Spring 对 {@link MessageSourceResolvable} 接口的默认实现。提供了一种简便方式，用于存储通过
 * {@link org.springframework.context.MessageSource} 解析消息所需的所有必要值。
 *
 * @author Juergen Hoeller
 * @since 13.02.2004
 * @see org.springframework.context.MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 */
@SuppressWarnings("serial")
public class DefaultMessageSourceResolvable implements MessageSourceResolvable, Serializable {

	@Nullable
	private final String[] codes;

	@Nullable
	private final Object[] arguments;

	@Nullable
	private final String defaultMessage;


	/**
	 * 创建一个新的 DefaultMessageSourceResolvable。
	 * @param code 用于解析此消息的代码
	 */
	public DefaultMessageSourceResolvable(String code) {
		this(new String[] {code}, null, null);
	}

	/**
	 * 创建一个新的 DefaultMessageSourceResolvable。
	 * @param codes 用于解析此消息的代码
	 */
	public DefaultMessageSourceResolvable(String[] codes) {
		this(codes, null, null);
	}

	/**
	 * 创建一个新的 DefaultMessageSourceResolvable。
	 * @param codes 用于解析此消息的代码
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public DefaultMessageSourceResolvable(String[] codes, String defaultMessage) {
		this(codes, null, defaultMessage);
	}

	/**
	 * 创建一个新的 DefaultMessageSourceResolvable。
	 * @param codes 用于解析此消息的代码
	 * @param arguments 用于解析此消息的参数数组
	 */
	public DefaultMessageSourceResolvable(String[] codes, Object[] arguments) {
		this(codes, arguments, null);
	}

	/**
	 * 创建一个新的 DefaultMessageSourceResolvable。
	 * @param codes 用于解析此消息的代码
	 * @param arguments 用于解析此消息的参数数组
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public DefaultMessageSourceResolvable(
			@Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

		this.codes = codes;
		this.arguments = arguments;
		this.defaultMessage = defaultMessage;
	}

	/**
	 * 复制构造函数：从另一个可解析对象创建新实例。
	 * @param resolvable 要从中复制的可解析对象
	 */
	public DefaultMessageSourceResolvable(MessageSourceResolvable resolvable) {
		this(resolvable.getCodes(), resolvable.getArguments(), resolvable.getDefaultMessage());
	}


	/**
	 * 返回此可解析对象的默认代码，即 codes 数组中的最后一个。
	 */
	@Nullable
	public String getCode() {
		return (this.codes != null && this.codes.length > 0 ? this.codes[this.codes.length - 1] : null);
	}

	@Override
	@Nullable
	public String[] getCodes() {
		return this.codes;
	}

	@Override
	@Nullable
	public Object[] getArguments() {
		return this.arguments;
	}

	@Override
	@Nullable
	public String getDefaultMessage() {
		return this.defaultMessage;
	}

	/**
	 * 指示指定的默认消息是否需要进行渲染，以替换占位符和/或进行 {@link java.text.MessageFormat} 转义。
	 * @return 如果默认消息可能包含参数占位符，则返回 {@code true}；
	 * 如果它肯定不包含占位符或自定义转义，因而可以原样暴露，则返回 {@code false}
	 * @since 5.1.7
	 * @see #getDefaultMessage()
	 * @see #getArguments()
	 * @see AbstractMessageSource#renderDefaultMessage
	 */
	public boolean shouldRenderDefaultMessage() {
		return true;
	}


	/**
	 * 为此 MessageSourceResolvable 构建默认的 String 表示形式：包括代码、参数和默认消息。
	 */
	protected final String resolvableToString() {
		StringBuilder result = new StringBuilder(64);
		result.append("codes [").append(StringUtils.arrayToDelimitedString(this.codes, ","));
		result.append("]; arguments [").append(StringUtils.arrayToDelimitedString(this.arguments, ","));
		result.append("]; default message [").append(this.defaultMessage).append(']');
		return result.toString();
	}

	/**
	 * 默认实现公开此 MessageSourceResolvable 的属性。<p>可在更具体的子类中覆盖，可能通过
	 * {@code resolvableToString()} 包含可解析内容。
	 * @see #resolvableToString()
	 */
	@Override
	public String toString() {
		return getClass().getName() + ": " + resolvableToString();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof MessageSourceResolvable)) {
			return false;
		}
		MessageSourceResolvable otherResolvable = (MessageSourceResolvable) other;
		return (ObjectUtils.nullSafeEquals(getCodes(), otherResolvable.getCodes()) &&
				ObjectUtils.nullSafeEquals(getArguments(), otherResolvable.getArguments()) &&
				ObjectUtils.nullSafeEquals(getDefaultMessage(), otherResolvable.getDefaultMessage()));
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(getCodes());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getArguments());
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(getDefaultMessage());
		return hashCode;
	}

}
