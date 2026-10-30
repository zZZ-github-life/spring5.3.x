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

package org.springframework.expression;

import org.springframework.lang.Nullable;

/**
 * 默认情况下，数学运算符 {@link Operation} 支持数字等简单类型。通过提供 OperatorOverloader 的实现，
 * 表达式语言的用户可以在其他类型上支持这些运算。
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface OperatorOverloader {

	/**
	 * 如果运算符重载器支持两个操作数之间的指定运算因而应被调用来处理该运算，则返回 true。
	 * @param operation 要执行的操作
	 * @param leftOperand 左操作数
	 * @param rightOperand 右操作数
	 * @return 如果 OperatorOverloader 支持这两个操作数之间的指定操作则返回 true
	 * @throws EvaluationException 如果执行操作时出现问题
	 */
	boolean overridesOperation(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException;

	/**
	 * 对两个操作数执行指定的运算并返回结果。受支持的运算参见 {@link Operation}。
	 * @param operation 要执行的操作
	 * @param leftOperand 左操作数
	 * @param rightOperand 右操作数
	 * @return 对两个操作数执行该操作的结果
	 * @throws EvaluationException 如果执行操作时出现问题
	 */
	Object operate(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException;

}
