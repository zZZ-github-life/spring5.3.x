/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.scripting.support;

import javax.script.ScriptException;

/**
 * 用于装饰 JSR-223 脚本求值过程中抛出的 {@link javax.script.ScriptException} 的异常，即
 * {@link javax.script.ScriptEngine#eval} 调用或 {@link javax.script.Invocable#invokeMethod} /
 * {@link javax.script.Invocable#invokeFunction} 调用。
 *
 * <p>此异常不打印 Java 堆栈跟踪，因为 JSR-223 的 {@link ScriptException} 会导致相当复杂的文本输出。
 * 从这个角度看，此异常主要是作为传入外部异常的 {@link ScriptException} 根因的装饰器。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.2.2
 */
@SuppressWarnings("serial")
public class StandardScriptEvalException extends RuntimeException {

	private final ScriptException scriptException;


	/**
	 * 使用指定的原始异常构造一个新的脚本求值异常。
	 */
	public StandardScriptEvalException(ScriptException ex) {
		super(ex.getMessage());
		this.scriptException = ex;
	}


	public final ScriptException getScriptException() {
		return this.scriptException;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
