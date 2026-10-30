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

import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.BooleanTypedValue;

/**
 * 表示 between 运算符。between 的左操作数必须是单个值，右操作数必须是列表 —— 如果左操作数位于（使
 * 用已注册的比较器）列表中两个元素之间，则该运算符返回 true。between 的包含性定义遵循 SQL BETWEEN
 * 的定义。
 *
 * @author Andy Clement
 * @since 3.0
 */
public class OperatorBetween extends Operator {

	public OperatorBetween(int startPos, int endPos, SpelNodeImpl... operands) {
		super("between", startPos, endPos, operands);
	}


	/**
	 * 根据某个值是否在表达的范围之内返回布尔值。第一个操作数是任意值，而第二个操作数是包含两个值的列表
	 * —— 这两个值就是第一个操作数所允许的边界（包含边界）。
	 * @param state 表达式状态
	 * @return 如果左操作数位于指定范围内则返回 true，否则返回 false
	 * @throws EvaluationException 如果对表达式求值时出现问题
	 */
	@Override
	public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		Object left = getLeftOperand().getValueInternal(state).getValue();
		Object right = getRightOperand().getValueInternal(state).getValue();
		if (!(right instanceof List) || ((List<?>) right).size() != 2) {
			throw new SpelEvaluationException(getRightOperand().getStartPosition(),
					SpelMessage.BETWEEN_RIGHT_OPERAND_MUST_BE_TWO_ELEMENT_LIST);
		}

		List<?> list = (List<?>) right;
		Object low = list.get(0);
		Object high = list.get(1);
		TypeComparator comp = state.getTypeComparator();
		try {
			return BooleanTypedValue.forValue(comp.compare(left, low) >= 0 && comp.compare(left, high) <= 0);
		}
		catch (SpelEvaluationException ex) {
			ex.setPosition(getStartPosition());
			throw ex;
		}
	}

}
