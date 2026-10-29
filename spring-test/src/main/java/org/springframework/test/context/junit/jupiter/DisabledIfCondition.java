/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code DisabledIfCondition} 是一个 {@link org.junit.jupiter.api.extension.ExecutionCondition}，在将 <em>Spring TestContext Framework</em> 与 JUnit 5 的 <em>Jupiter</em> 编程模型结合使用时支持 {@link DisabledIf @DisabledIf} 注解。
 * <p>如果在不存在 {@link DisabledIf @DisabledIf} 的情况下尝试使用 {@code DisabledIfCondition}，将得到<em>已启用</em>的 {@link ConditionEvaluationResult}。
 *
 * @author Sam Brannen
 * @since 5.0
 * @see DisabledIf
 * @see EnabledIf
 * @see SpringExtension
 */
public class DisabledIfCondition extends AbstractExpressionEvaluatingCondition {

	/**
	 * 如果相应测试类或测试方法上存在 {@code @DisabledIf} 且配置的表达式求值为 {@code true}，则容器和测试将被禁用。
	 */
	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return evaluateAnnotation(DisabledIf.class, DisabledIf::expression, DisabledIf::reason,
				DisabledIf::loadContext, false, context);
	}

}
