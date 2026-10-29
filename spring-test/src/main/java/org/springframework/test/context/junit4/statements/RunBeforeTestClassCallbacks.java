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

import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;

/**
 * {@code RunBeforeTestClassCallbacks} 是一个自定义 JUnit {@link Statement}，它通过在所提供的 {@link TestContextManager} 上调用 {@link TestContextManager#beforeTestClass() beforeTestClass()}，使 <em>Spring TestContext Framework</em> 能够插入 JUnit 执行链。
 *
 * @author Sam Brannen
 * @since 3.0
 * @see #evaluate()
 * @see RunAfterTestMethodCallbacks
 */
public class RunBeforeTestClassCallbacks extends Statement {

	private final Statement next;

	private final TestContextManager testContextManager;


	/**
	 * 构造一个新的 {@code RunBeforeTestClassCallbacks} 语句。
	 * @param next the next {@code Statement} in the execution chain
	 * @param testContextManager the TestContextManager upon which to call
	 * {@code beforeTestClass()}
	 */
	public RunBeforeTestClassCallbacks(Statement next, TestContextManager testContextManager) {
		this.next = next;
		this.testContextManager = testContextManager;
	}


	/**
	 * 调用 {@link TestContextManager#beforeTestClass()}，然后评估执行链中的下一个 {@link Statement}（通常是 {@link org.junit.internal.runners.statements.RunBefores RunBefores} 的实例）。
	 */
	@Override
	public void evaluate() throws Throwable {
		this.testContextManager.beforeTestClass();
		this.next.evaluate();
	}

}
