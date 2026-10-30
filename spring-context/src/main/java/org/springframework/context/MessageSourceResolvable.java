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

package org.springframework.context;

import org.springframework.lang.Nullable;

/**
 * 适用于在 {@link MessageSource} 中进行消息解析的对象接口。
 *
 * <p>Spring 自身的校验错误类实现了此接口。
 *
 * @author Juergen Hoeller
 * @see MessageSource#getMessage(MessageSourceResolvable, java.util.Locale)
 * @see org.springframework.validation.ObjectError
 * @see org.springframework.validation.FieldError
 */
@FunctionalInterface
public interface MessageSourceResolvable {

	/**
	 * 返回用于解析此消息的代码，按应尝试的顺序排列。因此最后一个代码将是默认代码。
	 * @return 与此消息关联的代码 String 数组
	 */
	@Nullable
	String[] getCodes();

	/**
	 * 返回用于解析此消息的参数数组。
	 *
	 * <p>默认实现仅返回 {@code null}。
	 * @return 用作参数以替换消息文本中占位符的对象数组
	 * @see java.text.MessageFormat
	 */
	@Nullable
	default Object[] getArguments() {
		return null;
	}

	/**
	 * 返回用于解析此消息的默认消息。
	 *
	 * <p>默认实现仅返回 {@code null}。请注意，默认消息可以与主消息代码（{@link #getCodes()}）相同，这
	 * 实际上相当于对此特定消息强制启用
	 * {@link org.springframework.context.support.AbstractMessageSource#setUseCodeAsDefaultMessage}。
	 * @return 默认消息；如果没有默认消息，则为 {@code null}
	 */
	@Nullable
	default String getDefaultMessage() {
		return null;
	}

}
