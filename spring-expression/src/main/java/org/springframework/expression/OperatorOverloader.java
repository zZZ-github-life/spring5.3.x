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
	 * @param operation the operation to be performed
	 * @param leftOperand the left operand
	 * @param rightOperand the right operand
	 * @return true if the OperatorOverloader supports the specified operation
	 * between the two operands
	 * @throws EvaluationException if there is a problem performing the operation
	 */
	boolean overridesOperation(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException;

	/**
	 * 对两个操作数执行指定的运算并返回结果。受支持的运算参见 {@link Operation}。
	 * @param operation the operation to be performed
	 * @param leftOperand the left operand
	 * @param rightOperand the right operand
	 * @return the result of performing the operation on the two operands
	 * @throws EvaluationException if there is a problem performing the operation
	 */
	Object operate(Operation operation, @Nullable Object leftOperand, @Nullable Object rightOperand)
			throws EvaluationException;

}
