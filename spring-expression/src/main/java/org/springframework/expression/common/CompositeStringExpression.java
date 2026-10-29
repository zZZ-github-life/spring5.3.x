/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression.common;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

/**
 * 表示被拆分为若干部分的模板表达式。每个部分都是一个 Expression，但模板中的纯文本部分会表示为
 * LiteralExpression 对象。模板表达式的一个示例可能是：
 *
 * <pre class="code">
 * &quot;Hello ${getName()}&quot;
 * </pre>
 *
 * 它会被表示为由两部分组成的 CompositeStringExpression。第一部分是表示 'Hello ' 的
 * LiteralExpression，第二部分是在调用时执行 {@code getName()} 的真实表达式。
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class CompositeStringExpression implements Expression {

	private final String expressionString;

	/** 组成复合表达式的表达式数组。 */
	private final Expression[] expressions;


	public CompositeStringExpression(String expressionString, Expression[] expressions) {
		this.expressionString = expressionString;
		this.expressions = expressions;
	}


	@Override
	public final String getExpressionString() {
		return this.expressionString;
	}

	public final Expression[] getExpressions() {
		return this.expressions;
	}

	@Override
	public String getValue() throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	@Nullable
	public <T> T getValue(@Nullable Class<T> expectedResultType) throws EvaluationException {
		Object value = getValue();
		return ExpressionUtils.convertTypedValue(null, new TypedValue(value), expectedResultType);
	}

	@Override
	public String getValue(@Nullable Object rootObject) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(rootObject, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	@Nullable
	public <T> T getValue(@Nullable Object rootObject, @Nullable Class<T> desiredResultType) throws EvaluationException {
		Object value = getValue(rootObject);
		return ExpressionUtils.convertTypedValue(null, new TypedValue(value), desiredResultType);
	}

	@Override
	public String getValue(EvaluationContext context) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(context, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	@Nullable
	public <T> T getValue(EvaluationContext context, @Nullable Class<T> expectedResultType)
			throws EvaluationException {

		Object value = getValue(context);
		return ExpressionUtils.convertTypedValue(context, new TypedValue(value), expectedResultType);
	}

	@Override
	public String getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		StringBuilder sb = new StringBuilder();
		for (Expression expression : this.expressions) {
			String value = expression.getValue(context, rootObject, String.class);
			if (value != null) {
				sb.append(value);
			}
		}
		return sb.toString();
	}

	@Override
	@Nullable
	public <T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException {

		Object value = getValue(context,rootObject);
		return ExpressionUtils.convertTypedValue(context, new TypedValue(value), desiredResultType);
	}

	@Override
	public Class<?> getValueType() {
		return String.class;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context) {
		return String.class;
	}

	@Override
	public Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException {
		return String.class;
	}

	@Override
	public Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return String.class;
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor() {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context) {
		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject)
			throws EvaluationException {

		return TypeDescriptor.valueOf(String.class);
	}

	@Override
	public boolean isWritable(@Nullable Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public boolean isWritable(EvaluationContext context) {
		return false;
	}

	@Override
	public boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException {
		return false;
	}

	@Override
	public void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

	@Override
	public void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value) throws EvaluationException {
		throw new EvaluationException(this.expressionString, "Cannot call setValue on a composite expression");
	}

}
