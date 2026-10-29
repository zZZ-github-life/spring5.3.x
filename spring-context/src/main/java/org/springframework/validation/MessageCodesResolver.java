/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.validation;

import org.springframework.lang.Nullable;

/**
 * 用于根据校验错误代码构建消息代码的策略接口。由 DataBinder 用于为 ObjectErrors 和 FieldErrors 构
 * 建代码列表。
 *
 * <p>生成的消息代码对应于 MessageSourceResolvable 的代码（由 ObjectError 和 FieldError 实现）。
 *
 * @author Juergen Hoeller
 * @since 1.0.1
 * @see DataBinder#setMessageCodesResolver
 * @see ObjectError
 * @see FieldError
 * @see org.springframework.context.MessageSourceResolvable#getCodes()
 */
public interface MessageCodesResolver {

	/**
	 * 为给定错误代码和对象名构建消息代码。用于为 ObjectError 构建代码列表。
	 * @param errorCode the error code used for rejecting the object
	 * @param objectName the name of the object
	 * @return the message codes to use
	 */
	String[] resolveMessageCodes(String errorCode, String objectName);

	/**
	 * 为给定错误代码和字段规格构建消息代码。用于为 FieldError 构建代码列表。
	 * @param errorCode the error code used for rejecting the value
	 * @param objectName the name of the object
	 * @param field the field name
	 * @param fieldType the field type (may be {@code null} if not determinable)
	 * @return the message codes to use
	 */
	String[] resolveMessageCodes(String errorCode, String objectName, String field, @Nullable Class<?> fieldType);

}
