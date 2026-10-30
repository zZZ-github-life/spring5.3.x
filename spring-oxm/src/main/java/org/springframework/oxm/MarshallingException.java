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

package org.springframework.oxm;

/**
 * 在发生 marshalling 或 unmarshalling 错误时抛出的异常的基类。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see MarshallingFailureException
 * @see UnmarshallingFailureException
 */
@SuppressWarnings("serial")
public abstract class MarshallingException extends XmlMappingException {

	/**
	 * 使用指定的详细消息构造一个 {@code MarshallingException}。
	 * @param msg 详细信息
	 */
	protected MarshallingException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定的详细消息和嵌套异常构造一个 {@code MarshallingException}。
	 * @param msg 详细信息
	 * @param cause 嵌套异常
	 */
	protected MarshallingException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
