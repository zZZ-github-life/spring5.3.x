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

package org.springframework.expression.spel;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

/**
 * 表示已解析表达式 AST 中的节点。
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface SpelNode {

	/**
	 * 在提供的表达式状态的上下文中求值该表达式节点，并返回值。
	 * @param expressionState the current expression state (includes the context)
	 * @return the value of this node evaluated against the specified state
	 */
	@Nullable
	Object getValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 在提供的表达式状态的上下文中求值该表达式节点，并返回带类型的值。
	 * @param expressionState the current expression state (includes the context)
	 * @return the type value of this node evaluated against the specified state
	 */
	TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 判断此表达式节点是否支持 setValue() 调用。
	 * @param expressionState the current expression state (includes the context)
	 * @return true if the expression node will allow setValue()
	 * @throws EvaluationException if something went wrong trying to determine
	 * if the node supports writing
	 */
	boolean isWritable(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 将表达式求值为一个节点，然后在该节点上设置新值。例如，如果表达式求值结果为属性引用，则会将该属性
	 * 设置为新值。
	 * @param expressionState the current expression state (includes the context)
	 * @param newValue the new value
	 * @throws EvaluationException if any problem occurs evaluating the expression or
	 * setting the new value
	 */
	void setValue(ExpressionState expressionState, @Nullable Object newValue) throws EvaluationException;

	/**
	 * 返回此 AST 节点的字符串形式。
	 * @return the string form
	 */
	String toStringAST();

	/**
	 * 返回此节点下的子节点数量。
	 * @return the child count
	 */
	int getChildCount();

	/**
	 * 返回 SpelNode 而非 Antlr Tree 节点的辅助方法。
	 * @return the child node cast to a SpelNode
	 */
	SpelNode getChild(int index);

	/**
	 * 确定传入对象的类，除非它本身已经是 Class 对象。
	 * @param obj the object that the caller wants the class of
	 * @return the class of the object if it is not already a class object,
	 * or {@code null} if the object is {@code null}
	 */
	@Nullable
	Class<?> getObjectClass(@Nullable Object obj);

	/**
	 * 返回此 AST 节点在表达式字符串中的起始位置。
	 * @return the start position
	 */
	int getStartPosition();

	/**
	 * 返回此 AST 节点在表达式字符串中的结束位置。
	 * @return the end position
	 */
	int getEndPosition();

}
