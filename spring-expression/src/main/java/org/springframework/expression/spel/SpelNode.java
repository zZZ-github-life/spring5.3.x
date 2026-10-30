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
	 * @param expressionState 当前的表达式状态（包含上下文）
	 * @return 在指定状态下对此节点求值得到的值
	 */
	@Nullable
	Object getValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 在提供的表达式状态的上下文中求值该表达式节点，并返回带类型的值。
	 * @param expressionState 当前的表达式状态（包含上下文）
	 * @return 在指定状态下对此节点求值得到的类型化值
	 */
	TypedValue getTypedValue(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 判断此表达式节点是否支持 setValue() 调用。
	 * @param expressionState 当前的表达式状态（包含上下文）
	 * @return 如果该表达式节点允许调用 setValue() 则返回 true
	 * @throws EvaluationException 如果尝试确定该节点是否支持写入时出现问题
	 */
	boolean isWritable(ExpressionState expressionState) throws EvaluationException;

	/**
	 * 将表达式求值为一个节点，然后在该节点上设置新值。例如，如果表达式求值结果为属性引用，则会将该属性
	 * 设置为新值。
	 * @param expressionState 当前的表达式状态（包含上下文）
	 * @param newValue 新值
	 * @throws EvaluationException 如果对表达式求值或设置新值时出现任何问题
	 */
	void setValue(ExpressionState expressionState, @Nullable Object newValue) throws EvaluationException;

	/**
	 * 返回此 AST 节点的字符串形式。
	 * @return 字符串形式
	 */
	String toStringAST();

	/**
	 * 返回此节点下的子节点数量。
	 * @return 子节点数量
	 */
	int getChildCount();

	/**
	 * 返回 SpelNode 而非 Antlr Tree 节点的辅助方法。
	 * @return 转换为 SpelNode 的子节点
	 */
	SpelNode getChild(int index);

	/**
	 * 确定传入对象的类，除非它本身已经是 Class 对象。
	 * @param obj 调用方想要获取其类的对象
	 * @return 该对象的类（如果它本身不是类对象），
	 * 如果该对象为 {@code null} 则返回 {@code null}
	 */
	@Nullable
	Class<?> getObjectClass(@Nullable Object obj);

	/**
	 * 返回此 AST 节点在表达式字符串中的起始位置。
	 * @return 起始位置
	 */
	int getStartPosition();

	/**
	 * 返回此 AST 节点在表达式字符串中的结束位置。
	 * @return 结束位置
	 */
	int getEndPosition();

}
