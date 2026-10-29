/*
 * Copyright 2002-2021 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.Statement;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.util.Assert;

/**
 * {@code ProfileValueChecker} 是一个自定义 JUnit {@link Statement}，用于通过 Spring 的 {@link IfProfileValue @IfProfileValue} 注解检查测试类或测试方法在当前环境中是否启用。
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see #evaluate()
 * @see IfProfileValue
 * @see ProfileValueUtils
 */
public class ProfileValueChecker extends Statement {

	private final Statement next;

	private final Class<?> testClass;

	@Nullable
	private final Method testMethod;


	/**
	 * 构造一个新的 {@code ProfileValueChecker} 语句。
	 * @param next the next {@code Statement} in the execution chain;
	 * never {@code null}
	 * @param testClass the test class to check; never {@code null}
	 * @param testMethod the test method to check; may be {@code null} if
	 * this {@code ProfileValueChecker} is being applied at the class level
	 */
	public ProfileValueChecker(Statement next, Class<?> testClass, @Nullable Method testMethod) {
		Assert.notNull(next, "The next statement must not be null");
		Assert.notNull(testClass, "The test class must not be null");
		this.next = next;
		this.testClass = testClass;
		this.testMethod = testMethod;
	}


	/**
	 * 确定由 {@linkplain #ProfileValueChecker 构造函数}参数指定的测试在当前环境中是否<em>启用</em>（根据 {@link IfProfileValue &#064;IfProfileValue} 注解的配置）。
	 * <p>如果测试未标注 {@code @IfProfileValue}，则视为已启用。
	 * <p>如果测试未启用，此方法将通过失败的假设中止执行链的进一步评估；否则，此方法将仅评估执行链中的下一个 {@link Statement}。
	 * @throws AssumptionViolatedException if the test is disabled
	 * @throws Throwable if evaluation of the next statement fails
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Method, Class)
	 */
	@Override
	public void evaluate() throws Throwable {
		if (this.testMethod == null) {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testClass)) {
				Annotation ann = AnnotatedElementUtils.findMergedAnnotation(this.testClass, IfProfileValue.class);
				throw new AssumptionViolatedException(String.format(
						"Profile configured via [%s] is not enabled in this environment for test class [%s].",
						ann, this.testClass.getName()));
			}
		}
		else {
			if (!ProfileValueUtils.isTestEnabledInThisEnvironment(this.testMethod, this.testClass)) {
				throw new AssumptionViolatedException(String.format(
						"Profile configured via @IfProfileValue is not enabled in this environment for test method [%s].",
						this.testMethod));
			}
		}

		this.next.evaluate();
	}

}
