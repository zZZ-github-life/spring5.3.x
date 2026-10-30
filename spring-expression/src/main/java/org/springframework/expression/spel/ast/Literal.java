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

import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.InternalParseException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.lang.Nullable;

/**
 * 表示字面值（boolean、string、number 等）的节点的公共超类。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public abstract class Literal extends SpelNodeImpl {

	@Nullable
	private final String originalValue;


	public Literal(@Nullable String originalValue, int startPos, int endPos) {
		super(startPos, endPos);
		this.originalValue = originalValue;
	}


	@Nullable
	public final String getOriginalValue() {
		return this.originalValue;
	}

	@Override
	public final TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
		return getLiteralValue();
	}

	@Override
	public String toString() {
		return String.valueOf(getLiteralValue().getValue());
	}

	@Override
	public String toStringAST() {
		return toString();
	}


	public abstract TypedValue getLiteralValue();


	/**
	 * 处理数字的字符串形式，若提供了指定进制则使用该进制，并返回合适的字面值来保存它。将考虑用于表示
	 * long 的任何后缀（支持 'l' 或 'L'）。
	 * @param numberToken 以数字为载荷的 token（例如 1234 或 0xCAFE）
	 * @param radix 数字的进制基数
	 * @return 能够表示该值的 Literal 子类型
	 */
	public static Literal getIntLiteral(String numberToken, int startPos, int endPos, int radix) {
		try {
			int value = Integer.parseInt(numberToken, radix);
			return new IntLiteral(numberToken, startPos, endPos, value);
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_AN_INTEGER, numberToken));
		}
	}

	public static Literal getLongLiteral(String numberToken, int startPos, int endPos, int radix) {
		try {
			long value = Long.parseLong(numberToken, radix);
			return new LongLiteral(numberToken, startPos, endPos, value);
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_LONG, numberToken));
		}
	}

	public static Literal getRealLiteral(String numberToken, int startPos, int endPos, boolean isFloat) {
		try {
			if (isFloat) {
				float value = Float.parseFloat(numberToken);
				return new FloatLiteral(numberToken, startPos, endPos, value);
			}
			else {
				double value = Double.parseDouble(numberToken);
				return new RealLiteral(numberToken, startPos, endPos, value);
			}
		}
		catch (NumberFormatException ex) {
			throw new InternalParseException(new SpelParseException(startPos, ex, SpelMessage.NOT_A_REAL, numberToken));
		}
	}

}
