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

import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunBeforeTestMethodCallbacks} 是一个自定义 JUnit {@link Statement}，它通过在所提供的 {@link TestContextManager} 上调用 {@link TestContextManager#beforeTestMethod(Object, Method) beforeTestMethod()}，使 <em>Spring TestContext Framework</em> 能够插入 JUnit 执行链。
 *
 * @author Sam Brannen
 * @since 3.0
 * @see #evaluate()
 * @see RunAfterTestMethodCallbacks
 */
public class RunBeforeTestMethodCallbacks extends Statement {

	private final Statement next;

	private final Object testInstance;

	private final Method testMethod;

	private final TestContextManager testContextManager;


	/**
	 * 构造一个新的 {@code RunBeforeTestMethodCallbacks} 语句。
	 * @param next 执行链中的下一个 {@code Statement}
	 * @param testInstance 当前的测试实例（从不为 {@code null}）
	 * @param testMethod 即将在测试实例上执行的测试方法
	 * @param testContextManager 要在其上调用 {@code beforeTestMethod()} 的 TestContextManager
	 */
	public RunBeforeTestMethodCallbacks(Statement next, Object testInstance, Method testMethod,
			TestContextManager testContextManager) {

		this.next = next;
		this.testInstance = testInstance;
		this.testMethod = testMethod;
		this.testContextManager = testContextManager;
	}


	/**
	 * 调用 {@link TestContextManager#beforeTestMethod(Object, Method)}，然后评估执行链中的下一个 {@link Statement}（通常是 {@link org.junit.internal.runners.statements.RunBefores RunBefores} 的实例）。
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.beforeTestMethod(this.testInstance, this.testMethod);
		this.next.evaluate();
	}

}
