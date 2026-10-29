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

package org.springframework.messaging.handler.annotation.support;

import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.validation.Validator;

/**
 * 使用 {@link MessageConverter} 提取并转换消息载荷的解析器。
 * 如果参数带有校验注解，它还会使用 {@link Validator} 校验载荷。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0
 * @deprecated as of 5.2, in favor of {@link PayloadMethodArgumentResolver}
 */
@Deprecated
public class PayloadArgumentResolver extends PayloadMethodArgumentResolver {

	/**
	 * 使用给定 {@link MessageConverter} 创建新的 {@code PayloadArgumentResolver}。
	 * @param messageConverter the MessageConverter to use (required)
	 * @since 4.0.9
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter) {
		this(messageConverter, null);
	}

	/**
	 * 使用给定 {@link MessageConverter} 和 {@link Validator}
	 * 创建新的 {@code PayloadArgumentResolver}。
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator) {
		this(messageConverter, validator, true);
	}

	/**
	 * 使用给定 {@link MessageConverter} 和 {@link Validator}
	 * 创建新的 {@code PayloadArgumentResolver}。
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 * @param useDefaultResolution if "true" (the default) this resolver supports
	 * all parameters; if "false" then only arguments with the {@code @Payload}
	 * annotation are supported.
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator,
			boolean useDefaultResolution) {


		super(messageConverter, validator, useDefaultResolution);
	}

}
