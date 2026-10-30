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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * 一种能够针对上下文对象对自身求值的表达式。封装了先前解析的表达式字符串的细节。为表达式求值提供了
 * 通用抽象。
 *
 * @author Keith Donald
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface Expression {

	/**
	 * 返回用于创建此表达式的原始字符串（未经修改）。
	 * @return 原始表达式字符串
	 */
	String getExpressionString();

	/**
	 * 在默认的标准上下文中求值此表达式。
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	Object getValue() throws EvaluationException;

	/**
	 * 在默认上下文中求值表达式。如果求值结果与预期的结果类型不匹配（且无法转换为该类型），则返回一个异
	 * 常。
	 * @param desiredResultType 调用方希望结果所属的类
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	<T> T getValue(@Nullable Class<T> desiredResultType) throws EvaluationException;

	/**
	 * 针对指定的根对象求值此表达式。
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	Object getValue(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * 在默认上下文中针对指定的根对象求值表达式。如果求值结果与预期的结果类型不匹配（且无法转换为该类型），
	 * 则返回一个异常。
	 * @param rootObject 用于对该表达式求值的根对象
	 * @param desiredResultType 调用方希望结果所属的类
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	<T> T getValue(@Nullable Object rootObject, @Nullable Class<T> desiredResultType) throws EvaluationException;

	/**
	 * 在提供的上下文中求值此表达式，并返回求值结果。
	 * @param context 对表达式进行求值所在的上下文
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	Object getValue(EvaluationContext context) throws EvaluationException;

	/**
	 * 在提供的上下文中求值此表达式并返回求值结果，但使用提供的根上下文覆盖上下文中指定的任何默认根对象。
	 * @param context 对表达式进行求值所在的上下文
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	Object getValue(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException;

	/**
	 * 在指定的上下文中求值表达式，该上下文可以解析对属性、方法、类型等的引用。求值结果预期为某个特定类
	 * 的类型；如果结果不是该类且无法转换为该类，将抛出异常。
	 * @param context 对表达式进行求值所在的上下文
	 * @param desiredResultType 调用方希望结果所属的类
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	<T> T getValue(EvaluationContext context, @Nullable Class<T> desiredResultType) throws EvaluationException;

	/**
	 * 在指定的上下文中求值表达式，该上下文可以解析对属性、方法、类型等的引用。求值结果预期为某个特定类
	 * 的类型；如果结果不是该类且无法转换为该类，将抛出异常。提供的根对象会覆盖所提供上下文中指定的任何
	 * 默认值。
	 * @param context 对表达式进行求值所在的上下文
	 * @param rootObject 用于对该表达式求值的根对象
	 * @param desiredResultType 调用方希望结果所属的类
	 * @return 求值结果
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	@Nullable
	<T> T getValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Class<T> desiredResultType)
			throws EvaluationException;

	/**
	 * 返回使用默认上下文时可以传递给 {@link #setValue} 方法的最通用类型。
	 * @return 可在此上下文上设置的最通用值类型
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	Class<?> getValueType() throws EvaluationException;

	/**
	 * 返回使用默认上下文时可以传递给 {@link #setValue(Object, Object)} 方法的最通用类型。
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 可在此上下文上设置的最通用值类型
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	Class<?> getValueType(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * 返回针对给定上下文可以传递给 {@link #setValue(EvaluationContext, Object)} 方法的最通用类型。
	 * @param context 对表达式进行求值所在的上下文
	 * @return 可在此上下文上设置的最通用值类型
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	Class<?> getValueType(EvaluationContext context) throws EvaluationException;

	/**
	 * 返回针对给定上下文可以传递给 {@link #setValue(EvaluationContext, Object, Object)} 方法的最通用类
	 * 型。提供的根对象会覆盖上下文中指定的任何对象。
	 * @param context 对表达式进行求值所在的上下文
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 可在此上下文上设置的最通用值类型
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	Class<?> getValueType(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException;

	/**
	 * 返回使用默认上下文时可以传递给 {@link #setValue} 方法的最通用类型。
	 * @return 可在此上下文上设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor() throws EvaluationException;

	/**
	 * 返回使用默认上下文时可以传递给 {@link #setValue(Object, Object)} 方法的最通用类型。
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 可在此上下文上设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * 返回针对给定上下文可以传递给 {@link #setValue(EvaluationContext, Object)} 方法的最通用类型。
	 * @param context 对表达式进行求值所在的上下文
	 * @return 可在此上下文上设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException;

	/**
	 * 返回针对给定上下文可以传递给 {@link #setValue(EvaluationContext, Object, Object)} 方法的最通用类
	 * 型。提供的根对象会覆盖上下文中指定的任何对象。
	 * @param context 对表达式进行求值所在的上下文
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 可在此上下文上设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时出现问题
	 */
	@Nullable
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException;

	/**
	 * 判断表达式是否可写入，即是否可以调用 setValue()。
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 如果表达式可写则返回 {@code true}；否则返回 {@code false}
	 * @throws EvaluationException 如果确定其是否可写时出现问题
	 */
	boolean isWritable(@Nullable Object rootObject) throws EvaluationException;

	/**
	 * 判断表达式是否可写入，即是否可以调用 setValue()。
	 * @param context 检查表达式所依据的上下文
	 * @return 如果表达式可写则返回 {@code true}；否则返回 {@code false}
	 * @throws EvaluationException 如果确定其是否可写时出现问题
	 */
	boolean isWritable(EvaluationContext context) throws EvaluationException;

	/**
	 * 判断表达式是否可写入，即是否可以调用 setValue()。提供的根对象会覆盖上下文中指定的任何对象。
	 * @param context 检查表达式所依据的上下文
	 * @param rootObject 用于对该表达式求值的根对象
	 * @return 如果表达式可写则返回 {@code true}；否则返回 {@code false}
	 * @throws EvaluationException 如果确定其是否可写时出现问题
	 */
	boolean isWritable(EvaluationContext context, @Nullable Object rootObject) throws EvaluationException;

	/**
	 * 将提供的上下文中的此表达式设置为提供的值。
	 * @param rootObject 用于对该表达式求值的根对象
	 * @param value 新值
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	void setValue(@Nullable Object rootObject, @Nullable Object value) throws EvaluationException;

	/**
	 * 将提供的上下文中的此表达式设置为提供的值。
	 * @param context 设置表达式值所在的上下文
	 * @param value 新值
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	void setValue(EvaluationContext context, @Nullable Object value) throws EvaluationException;

	/**
	 * 将提供的上下文中的此表达式设置为提供的值。提供的根对象会覆盖上下文中指定的任何对象。
	 * @param context 设置表达式值所在的上下文
	 * @param rootObject 用于对该表达式求值的根对象
	 * @param value 新值
	 * @throws EvaluationException 如果求值过程中出现问题
	 */
	void setValue(EvaluationContext context, @Nullable Object rootObject, @Nullable Object value) throws EvaluationException;

}
