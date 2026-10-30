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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.NumberUtils;

/**
 * 加号运算符将：
 * <ul>
 * <li>相加数字
 * <li>拼接字符串
 * </ul>
 *
 * <p>可将它用作数字的一元运算符。当操作数类型不同时（double+int=double），会执行标准提升。对于其他
 * 情况，它交由已注册的重载器处理。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Ivo Smid
 * @author Giovanni Dall'Oglio Risso
 * @author Sam Brannen
 * @since 3.0
 */
public class OpPlus extends Operator {

	/**
	 * 拼接字符串中允许的最大字符数。
	 * @since 5.2.24
	 */
	private static final int MAX_CONCATENATED_STRING_LENGTH = 100_000;


	public OpPlus(int startPos, int endPos, SpelNodeImpl... operands) {
		super("+", startPos, endPos, operands);
		Assert.notEmpty(operands, "Operands must not be empty");
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl leftOp = getLeftOperand();

		if (this.children.length < 2) {  // if only one operand, then this is unary plus
			Object operandOne = leftOp.getValueInternal(state).getValue();
			if (operandOne instanceof Number) {
				if (operandOne instanceof Double) {
					this.exitTypeDescriptor = "D";
				}
				else if (operandOne instanceof Float) {
					this.exitTypeDescriptor = "F";
				}
				else if (operandOne instanceof Long) {
					this.exitTypeDescriptor = "J";
				}
				else if (operandOne instanceof Integer) {
					this.exitTypeDescriptor = "I";
				}
				return new TypedValue(operandOne);
			}
			return state.operate(Operation.ADD, operandOne, null);
		}

		TypedValue operandOneValue = leftOp.getValueInternal(state);
		Object leftOperand = operandOneValue.getValue();
		TypedValue operandTwoValue = getRightOperand().getValueInternal(state);
		Object rightOperand = operandTwoValue.getValue();

		if (leftOperand instanceof Number && rightOperand instanceof Number) {
			Number leftNumber = (Number) leftOperand;
			Number rightNumber = (Number) rightOperand;

			if (leftNumber instanceof BigDecimal || rightNumber instanceof BigDecimal) {
				BigDecimal leftBigDecimal = NumberUtils.convertNumberToTargetClass(leftNumber, BigDecimal.class);
				BigDecimal rightBigDecimal = NumberUtils.convertNumberToTargetClass(rightNumber, BigDecimal.class);
				return new TypedValue(leftBigDecimal.add(rightBigDecimal));
			}
			else if (leftNumber instanceof Double || rightNumber instanceof Double) {
				this.exitTypeDescriptor = "D";
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
			else if (leftNumber instanceof Float || rightNumber instanceof Float) {
				this.exitTypeDescriptor = "F";
				return new TypedValue(leftNumber.floatValue() + rightNumber.floatValue());
			}
			else if (leftNumber instanceof BigInteger || rightNumber instanceof BigInteger) {
				BigInteger leftBigInteger = NumberUtils.convertNumberToTargetClass(leftNumber, BigInteger.class);
				BigInteger rightBigInteger = NumberUtils.convertNumberToTargetClass(rightNumber, BigInteger.class);
				return new TypedValue(leftBigInteger.add(rightBigInteger));
			}
			else if (leftNumber instanceof Long || rightNumber instanceof Long) {
				this.exitTypeDescriptor = "J";
				return new TypedValue(leftNumber.longValue() + rightNumber.longValue());
			}
			else if (CodeFlow.isIntegerForNumericOp(leftNumber) || CodeFlow.isIntegerForNumericOp(rightNumber)) {
				this.exitTypeDescriptor = "I";
				return new TypedValue(leftNumber.intValue() + rightNumber.intValue());
			}
			else {
				// Unknown Number subtypes -> best guess is double addition
				return new TypedValue(leftNumber.doubleValue() + rightNumber.doubleValue());
			}
		}

		if (leftOperand instanceof String && rightOperand instanceof String) {
			this.exitTypeDescriptor = "Ljava/lang/String";
			String leftString = (String) leftOperand;
			String rightString = (String) rightOperand;
			checkStringLength(leftString);
			checkStringLength(rightString);
			return concatenate(leftString, rightString);
		}

		if (leftOperand instanceof String) {
			String leftString = (String) leftOperand;
			checkStringLength(leftString);
			String rightString = (rightOperand == null ? "null" : convertTypedValueToString(operandTwoValue, state));
			checkStringLength(rightString);
			return concatenate(leftString, rightString);
		}

		if (rightOperand instanceof String) {
			String rightString = (String) rightOperand;
			checkStringLength(rightString);
			String leftString = (leftOperand == null ? "null" : convertTypedValueToString(operandOneValue, state));
			checkStringLength(leftString);
			return concatenate(leftString, rightString);
		}

		return state.operate(Operation.ADD, leftOperand, rightOperand);
	}

	private void checkStringLength(String string) {
		if (string.length() > MAX_CONCATENATED_STRING_LENGTH) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.MAX_CONCATENATED_STRING_LENGTH_EXCEEDED, MAX_CONCATENATED_STRING_LENGTH);
		}
	}

	private TypedValue concatenate(String leftString, String rightString) {
		String result = leftString + rightString;
		checkStringLength(result);
		return new TypedValue(result);
	}

	@Override
	public String toStringAST() {
		if (this.children.length < 2) {  // unary plus
			return "+" + getLeftOperand().toStringAST();
		}
		return super.toStringAST();
	}

	@Override
	public SpelNodeImpl getRightOperand() {
		if (this.children.length < 2) {
			throw new IllegalStateException("No right operand");
		}
		return this.children[1];
	}

	/**
	 * 使用已注册的转换器或 {@code toString} 方法将操作数值转换为字符串。
	 * @param value 要转换的类型化值
	 * @param state 表达式状态
	 * @return 转换为 {@code String} 的 {@code TypedValue} 实例
	 */
	private static String convertTypedValueToString(TypedValue value, ExpressionState state) {
		TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
		TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(String.class);
		if (typeConverter.canConvert(value.getTypeDescriptor(), typeDescriptor)) {
			return String.valueOf(typeConverter.convertValue(value.getValue(),
					value.getTypeDescriptor(), typeDescriptor));
		}
		return String.valueOf(value.getValue());
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

	/**
	 * 遍历由字符串组合而成的可能节点树，并将它们全部追加到同一个（栈上的）StringBuilder 中。
	 */
	private void walk(MethodVisitor mv, CodeFlow cf, @Nullable SpelNodeImpl operand) {
		if (operand instanceof OpPlus) {
			OpPlus plus = (OpPlus)operand;
			walk(mv, cf, plus.getLeftOperand());
			walk(mv, cf, plus.getRightOperand());
		}
		else if (operand != null) {
			cf.enterCompilationScope();
			operand.generateCode(mv,cf);
			if (!"Ljava/lang/String".equals(cf.lastDescriptor())) {
				mv.visitTypeInsn(CHECKCAST, "java/lang/String");
			}
			cf.exitCompilationScope();
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
		}
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		if ("Ljava/lang/String".equals(this.exitTypeDescriptor)) {
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
			walk(mv, cf, getLeftOperand());
			walk(mv, cf, getRightOperand());
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
		}
		else {
			this.children[0].generateCode(mv, cf);
			String leftDesc = this.children[0].exitTypeDescriptor;
			String exitDesc = this.exitTypeDescriptor;
			Assert.state(exitDesc != null, "No exit type descriptor");
			char targetDesc = exitDesc.charAt(0);
			CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, leftDesc, targetDesc);
			if (this.children.length > 1) {
				cf.enterCompilationScope();
				this.children[1].generateCode(mv, cf);
				String rightDesc = this.children[1].exitTypeDescriptor;
				cf.exitCompilationScope();
				CodeFlow.insertNumericUnboxOrPrimitiveTypeCoercion(mv, rightDesc, targetDesc);
				switch (targetDesc) {
					case 'I':
						mv.visitInsn(IADD);
						break;
					case 'J':
						mv.visitInsn(LADD);
						break;
					case 'F':
						mv.visitInsn(FADD);
						break;
					case 'D':
						mv.visitInsn(DADD);
						break;
					default:
						throw new IllegalStateException(
								"Unrecognized exit type descriptor: '" + this.exitTypeDescriptor + "'");
				}
			}
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
