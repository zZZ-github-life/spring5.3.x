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

package org.springframework.expression.spel;

/**
 * 包装真实的解析异常。该异常会流向顶层解析方法，然后抛出被包装的异常作为真正的问题。
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class InternalParseException extends RuntimeException {

	public InternalParseException(SpelParseException cause) {
		super(cause);
	}

	@Override
	public SpelParseException getCause() {
		return (SpelParseException) super.getCause();
	}

}
