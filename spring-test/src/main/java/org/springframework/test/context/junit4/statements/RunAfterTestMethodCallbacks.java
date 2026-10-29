/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;

import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunAfterTestMethodCallbacks} 是一个自定义 JUnit {@link Statement}，它通过在所提供的 {@link TestContextManager} 上调用 {@link TestContextManager#afterTestMethod afterTestMethod()}，使 <em>Spring TestContext Framework</em> 能够插入 JUnit 执行链。
 * <p><strong>注意：</strong>此类要求 JUnit 4.9 或更高版本。
 *
 * @author Sam Brannen
 * @since 3.0
 * @see #evaluate()
 * @see RunBeforeTestMethodCallbacks
 */
public class RunAfterTestMethodCallbacks extends Statement {

	private final Statement next;

	private final Object testInstance;

	private final Method testMethod;

	private final TestContextManager testContextManager;


	/**
	 * 构造一个新的 {@code RunAfterTestMethodCallbacks} 语句。
	 * @param next the next {@code Statement} in the execution chain
	 * @param testInstance the current test instance (never {@code null})
	 * @param testMethod the test method which has just been executed on the
	 * test instance
	 * @param testContextManager the TestContextManager upon which to call
	 * {@code afterTestMethod()}
	 */
	public RunAfterTestMethodCallbacks(Statement next, Object testInstance, Method testMethod,
			TestContextManager testContextManager) {

		this.next = next;
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testContextManager = testContextManager;
	}


	/**
	 * 评估执行链中的下一个 {@link Statement}（通常是 {@link org.junit.internal.runners.statements.RunAfters RunAfters} 的实例），捕获抛出的任何异常，然后调用 {@link TestContextManager#afterTestMethod(Object, Method, Throwable)} 并传入第一个捕获的异常（如果有）。
	 * <p>如果调用 {@code afterTestMethod()} 抛出异常，该异常也会被跟踪。多个异常将合并为 {@link MultipleFailureException}。
	 */
	@Override
	public void evaluate() throws Throwable {
		Throwable testException = null;
		List<Throwable> errors = new ArrayList<>();
		try {
			this.next.evaluate();
		}
		catch (Throwable ex) {
			testException = ex;
			errors.add(ex);
		}

		try {
			this.testContextManager.afterTestMethod(this.testInstance, this.testMethod, testException);
		}
		catch (Throwable ex) {
			errors.add(ex);
		}

		MultipleFailureException.assertEmpty(errors);
	}

}
