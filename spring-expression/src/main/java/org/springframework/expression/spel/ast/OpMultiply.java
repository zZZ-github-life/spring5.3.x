/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * 实现 {@code multiply} 运算符。
 *
 * <p>类型转换和提升按照
 * <a href="https://java.sun.com/docs/books/jls/third_edition/html/conversions.html">Java 语言规范
 * 第 5.6.2 节</a> 中的定义处理，此外还增加了 {@code BigDecimal}/{@code BigInteger} 管理：
 *
 * <p>如果任一操作数为引用类型，则执行拆箱转换（第 5.1.8 节）。然后：<br>如果任一操作数的类型为
 * {@code BigDecimal}，则将另一个转换为 {@code BigDecimal}。<br>如果任一操作数为 double 类型，则将
 * 另一个转换为 double。<br>否则，如果任一操作数为 float 类型，则将另一个转换为 float。<br>如果任一
 * 操作数为 {@code BigInteger} 类型，则将另一个转换为 {@code BigInteger}。<br>否则，如果任一操作数
 * 为 long 类型，则将另一个转换为 long。<br>否则，两个操作数都转换为 int 类型。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Giovanni Dall'Oglio Risso
 * @since 3.0
 */
public class OpMultiply extends Operator {

	/**
	 * 重复文本中允许的最大字符数。
	 * @since 5.2.23
	 */
	private static final int MAX_REPEATED_TEXT_SIZE = 256;


	public OpMultiply(int startPos, int endPos, SpelNodeImpl... operands) {
		super("*", startPos, endPos, operands);
	}


	/**
	 * 对于某些支持的操作数类型，直接在此处实现 {@code multiply} 运算符；对于此处不支持的类型，则委托给
	 * 任何已注册的重载器处理。
	 * <p>支持的操作数类型：
	 * <ul>
	 * <li>数字
	 * <li>String 和 int（'abc' * 2 == 'abcabc'）
	 * </ul>
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object leftOperand = getLeftOperand().getValueInternal(state).getValue();
		Object rightOperand = getRightOperand().getValueInternal(state).getValue();

		if (leftOperand instanceof Number && rightOperand instanceof Number) {
			Number leftNumber = (Number) leftOperand;
			Number rightNumber = (Number) rightOperand;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.multiply(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() * rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.multiply(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() * rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() * rightNumber.intValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double multiplication
				return new TypedValue(leftNumber.doubleValue() * rightNumber.doubleValue());
			}
		}

		if (leftOperand instanceof String && rightOperand instanceof Integer) {
			String text = (String) leftOperand;
			int count = (Integer) rightOperand;
			int requestedSize = text.length() * count;
			checkRepeatedTextSize(requestedSize);
			StringBuilder result = new StringBuilder(requestedSize);
			for (int i = 0; i < count; i++) {
				result.append(text);
			}
			return new TypedValue(result.toString());
		}

		return state.operate(Operation.MULTIPLY, leftOperand, rightOperand);
	}

	private void checkRepeatedTextSize(int requestedSize) {
		if (requestedSize > MAX_REPEATED_TEXT_SIZE) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.MAX_REPEATED_TEXT_SIZE_EXCEEDED, MAX_REPEATED_TEXT_SIZE);
		}
	}

	@Override
	public boolean isCompilable() {
		if (!getLeftOperand().isCompilable()) {
			return false;
		}
		if (this.children.length > 1) {
			if (!getRightOperand().isCompilable()) {
				return false;
			}
		}
		return (this.exitTypeDescriptor != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		getLeftOperand().generateCode(mv, cf);
		String leftDesc = getLeftOperand().exitTypeDescriptor;
		String exitDesc = this.exitTypeDescriptor;
		Assert.state(exitDesc != null, "No exit type descriptor");
		char targetDesc = exitDesc.charAt(0);
		CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
		if (this.children.length > 1) {
			cf.enterCompilationScope();
			getRightOperand().generateCode(mv, cf);
			String rightDesc = getRightOperand().exitTypeDescriptor;
			cf.exitCompilationScope();
			CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
			switch (targetDesc) {
				case 'I':
					mv.visitInsn(IMUL);
					break;
				case 'J':
					mv.visitInsn(LMUL);
					break;
				case 'F':
					mv.visitInsn(FMUL);
					break;
				case 'D':
					mv.visitInsn(DMUL);
					break;
				default:
					throw new IllegalStateException(
							"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
