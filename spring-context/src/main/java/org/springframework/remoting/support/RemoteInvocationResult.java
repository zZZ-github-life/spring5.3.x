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

package org.springframework.remoting.support;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.springframework.lang.Nullable;

/**
 * 封装远程调用结果，其中保存结果值或异常。用于基于 HTTP 的序列化调用器。
 *
 * <p>这是一个 SPI 类，通常不由应用程序直接使用。可以对其子类化以添加额外的调用参数。
 *
 * <p>{@link RemoteInvocation} 和 {@link RemoteInvocationResult} 均可与标准 Java 序列化以及
 * JavaBean 风格序列化配合使用。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see RemoteInvocation
 */
public class RemoteInvocationResult implements Serializable {

	/** 使用 Spring 1.1 中的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = 2138555143707773549L;


	@Nullable
	private Object value;

	@Nullable
	private Throwable exception;


	/**
	 * 为给定的结果值创建一个新的 RemoteInvocationResult。
	 * @param value the result value returned by a successful invocation
	 * of the target method
	 */
	public RemoteInvocationResult(@Nullable Object value) {
		this.value = value;
	}

	/**
	 * 为给定的异常创建一个新的 RemoteInvocationResult。
	 * @param exception the exception thrown by an unsuccessful invocation
	 * of the target method
	 */
	public RemoteInvocationResult(@Nullable Throwable exception) {
		this.exception = exception;
	}

	/**
	 * 为 JavaBean 风格反序列化（例如使用 Jackson）创建一个新的 RemoteInvocationResult。
	 * @see #setValue
	 * @see #setException
	 */
	public RemoteInvocationResult() {
	}


	/**
	 * 设置目标方法成功调用后返回的结果值（如果有）。
	 * <p>此设置方法用于 JavaBean 风格反序列化。否则请使用 {@link #RemoteInvocationResult(Object)}。
	 * @see #RemoteInvocationResult()
	 */
	public void setValue(@Nullable Object value) {
		this.value = value;
	}

	/**
	 * 返回目标方法成功调用后返回的结果值（如果有）。
	 * @see #hasException
	 */
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 设置目标方法调用失败时抛出的异常（如果有）。
	 * <p>此设置方法用于 JavaBean 风格反序列化。否则请使用 {@link #RemoteInvocationResult(Throwable)}。
	 * @see #RemoteInvocationResult()
	 */
	public void setException(@Nullable Throwable exception) {
		this.exception = exception;
	}

	/**
	 * 返回目标方法调用失败时抛出的异常（如果有）。
	 * @see #hasException
	 */
	@Nullable
	public Throwable getException() {
		return this.exception;
	}

	/**
	 * 返回此调用结果是否包含异常。如果返回 {@code false}，则以结果值为准（即使它为 {@code null}）。
	 * @see #getValue
	 * @see #getException
	 */
	public boolean hasException() {
		return (this.exception != null);
	}

	/**
	 * 返回此调用结果是否包含由目标方法自身调用所抛出的 InvocationTargetException。
	 * @see #hasException()
	 */
	public boolean hasInvocationTargetException() {
		return (this.exception instanceof InvocationTargetException);
	}


	/**
	 * 重建调用结果：如果目标方法调用成功，则返回结果值；否则重新抛出目标方法抛出的异常。
	 * @return the result value, if any
	 * @throws Throwable the exception, if any
	 */
	@Nullable
	public Object recreate() throws Throwable {
		if (this.exception != null) {
			Throwable exToThrow = this.exception;
			if (this.exception instanceof InvocationTargetException) {
				exToThrow = ((InvocationTargetException) this.exception).getTargetException();
			}
			RemoteInvocationUtils.fillInClientStackTraceIfPossible(exToThrow);
			throw exToThrow;
		}
		else {
			return this.value;
		}
	}

}
