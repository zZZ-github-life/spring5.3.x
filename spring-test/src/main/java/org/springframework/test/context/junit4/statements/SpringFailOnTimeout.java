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

package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import org.junit.runners.model.Statement;

import org.springframework.test.annotation.TestAnnotationUtils;
import org.springframework.util.Assert;

/**
 * {@code SpringFailOnTimeout} 是一个自定义 JUnit {@link Statement}，它通过在执行链中的下一个语句耗时超过指定毫秒数时抛出异常，为 Spring 的 {@link org.springframework.test.annotation.Timed @Timed} 注解提供支持。
 * <p>与 JUnit 的 {@link org.junit.internal.runners.statements.FailOnTimeout FailOnTimeout} 不同，下一个 {@code statement} 将与调用者在同一线程中执行，因此不会被抢先中止。
 *
 * @author Sam Brannen
 * @since 3.0
 * @see #evaluate()
 */
public class SpringFailOnTimeout extends Statement {

	private final Statement next;

	private final long timeout;


	/**
	 * 为所提供的 {@code testMethod} 构造一个新的 {@code SpringFailOnTimeout} 语句，并从所提供方法上的 {@code @Timed} 注解获取配置的超时时间。
	 * @param next the next {@code Statement} in the execution chain
	 * @param testMethod the current test method
	 * @see TestAnnotationUtils#getTimeout(Method)
	 */
	public SpringFailOnTimeout(Statement next, Method testMethod) {
		this(next, TestAnnotationUtils.getTimeout(testMethod));
	}

	/**
	 * 为所提供的 {@code timeout} 构造一个新的 {@code SpringFailOnTimeout} 语句。
	 * <p>如果所提供的 {@code timeout} 为 {@code 0}，则不会对 {@code next} 语句的执行计时。
	 * @param next the next {@code Statement} in the execution chain; never {@code null}
	 * @param timeout the configured {@code timeout} for the current test, in milliseconds;
	 * never negative
	 */
	public SpringFailOnTimeout(Statement next, long timeout) {
		Assert.notNull(next, "next statement must not be null");
		Assert.isTrue(timeout >= 0, "timeout must be non-negative");
		this.next = next;
		this.timeout = timeout;
	}


	/**
	 * 评估执行链中的下一个 {@link Statement statement}（通常是 {@link SpringRepeat} 的实例），如果下一个 {@code statement} 的执行时间超过指定的 {@code timeout}，则抛出 {@link TimeoutException}。
	 */
	@Override
	public void evaluate() throws Throwable {
		if (this.timeout == 0) {
			this.next.evaluate();
		}
		else {
			long startTime = System.currentTimeMillis();
			this.next.evaluate();
			long elapsed = System.currentTimeMillis() - startTime;
			if (elapsed > this.timeout) {
				throw new TimeoutException(
						String.format("Test took %s ms; limit was %s ms.", elapsed, this.timeout));
			}
		}
	}

}
