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

package org.springframework.test.context.junit4.statements;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runners.model.Statement;

import org.springframework.test.annotation.TestAnnotationUtils;

/**
 * {@code SpringRepeat} 是一个自定义 JUnit {@link Statement}，它通过按指定次数重复测试，为 Spring 的 {@link org.springframework.test.annotation.Repeat @Repeat} 注解提供支持。
 *
 * @author Sam Brannen
 * @since 3.0
 * @see #evaluate()
 */
public class SpringRepeat extends Statement {

	protected static final Log logger = LogFactory.getLog(SpringRepeat.class);

	private final Statement next;

	private final Method testMethod;

	private final int repeat;


	/**
	 * 为所提供的 {@code testMethod} 构造一个新的 {@code SpringRepeat} 语句，并从所提供方法上的 {@code @Repeat} 注解获取配置的重复次数。
	 * @param next the next {@code Statement} in the execution chain
	 * @param testMethod the current test method
	 * @see TestAnnotationUtils#getRepeatCount(Method)
	 */
	public SpringRepeat(Statement next, Method testMethod) {
		this(next, testMethod, TestAnnotationUtils.getRepeatCount(testMethod));
	}

	/**
	 * 为所提供的 {@code testMethod} 和 {@code repeat} 次数构造一个新的 {@code SpringRepeat} 语句。
	 * @param next the next {@code Statement} in the execution chain
	 * @param testMethod the current test method
	 * @param repeat the configured repeat count for the current test method
	 */
	public SpringRepeat(Statement next, Method testMethod, int repeat) {
		this.next = next;
		this.testMethod = testMethod;
		this.repeat = Math.max(1, repeat);
	}


	/**
	 * 使用指定的重复次数，反复评估执行链中的下一个 {@link Statement statement}。
	 */
	@Override
	public void evaluate() throws Throwable {
		for (int i = 0; i < this.repeat; i++) {
			if (this.repeat > 1 && logger.isInfoEnabled()) {
				logger.info(String.format("Repetition %d of test %s#%s()", (i + 1),
						this.testMethod.getDeclaringClass().getSimpleName(), this.testMethod.getName()));
			}
			this.next.evaluate();
		}
	}

}
