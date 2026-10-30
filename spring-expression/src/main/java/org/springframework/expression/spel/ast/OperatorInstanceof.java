/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Type;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 'instanceof' 运算符检查对象是否为右操作数中指定的类的实例，其方式与 Java 中的 {@code instanceof}
 * 相同。
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorInstanceof extends Operator {

	@Nullable
	private Class<?> type;


	public OperatorInstanceof(int startPos, int endPos, SpelNodeImpl... operands) {
		super("instanceof", startPos, endPos, operands);
	}


	/**
	 * 比较左操作数，以查看它是否为右操作数所指定类型的实例。右操作数必须是一个类。
	 * @param state 表达式状态
	 * @return 如果左操作数是右操作数的 instanceof 则返回 {@code true}，
	 * 否则返回 {@code false}
	 * @throws EvaluationException 如果对表达式求值时出现问题
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		SpelNodeImpl rightOperand = getRightOperand();
		TypedValue left = getLeftOperand().getValueInternal(state);
		TypedValue right = rightOperand.getValueInternal(state);
		Object leftValue = left.getValue();
		Object rightValue = right.getValue();
		BooleanTypedValue result;
		if (!(rightValue instanceof Class)) {
			throw new SpelEvaluationException(getRightOperand().getStartPosition(),
					SpelMessage.INSTANCEOF_OPERATOR_NEEDS_CLASS_OPERAND,
					(rightValue == null ? "null" : rightValue.getClass().getName()));
		}
		Class<?> rightClass = (Class<?>) rightValue;
		if (leftValue == null) {
			result = BooleanTypedValue.FALSE;  // null is not an instanceof anything
		}
		else {
			result = BooleanTypedValue.forValue(rightClass.isAssignableFrom(leftValue.getClass()));
		}
		this.type = rightClass;
		if (rightOperand instanceof TypeReference) {
			// Can only generate bytecode where the right operand is a direct type reference,
			// not if it is indirect (for example when right operand is a variable reference)
			this.exitTypeDescriptor = "Z";
		}
		return result;
	}

	@Override
	public boolean isCompilable() {
		return (this.exitTypeDescriptor != null && getLeftOperand().isCompilable());
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		getLeftOperand().generateCode(mv, cf);
		CodeFlow.insertBoxIfNecessary(mv, cf.lastDescriptor());
		Assert.state(this.type != null, "No type available");
		if (this.type.isPrimitive()) {
			// always false - but left operand code always driven
			// in case it had side effects
			mv.visitInsn(POP);
			mv.visitInsn(ICONST_0); // value of false
		}
		else {
			mv.visitTypeInsn(INSTANCEOF, Type.getInternalName(this.type));
		}
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
