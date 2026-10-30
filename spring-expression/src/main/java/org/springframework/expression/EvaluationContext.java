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

package org.springframework.expression;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;

/**
 * 表达式在求值上下文中执行。在表达式求值过程中遇到引用时，正是在此上下文中解析这些引用。
 *
 * <p>此接口有两个默认实现。
 * <ul>
 * <li>{@link org.springframework.expression.spel.support.SimpleEvaluationContext SimpleEvaluationContext}：更简单的构建器风格 {@code EvaluationContext} 变体，适用于数据绑定目的，可以按需选择启用若干 SpEL 特性。</li>
 * <li>{@link org.springframework.expression.spel.support.StandardEvaluationContext StandardEvaluationContext}：功能强大且高度可配置的 {@code EvaluationContext} 实现，可以扩展，而无需手动实现所有内容。</li>
 * </ul>
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public interface EvaluationContext {

	/**
	 * 返回默认的根上下文对象，未限定的属性/方法等应针对该对象进行解析。在求值表达式时可以覆盖它。
	 */
	TypedValue getRootObject();

	/**
	 * 返回一组访问器，将依次请求它们读取/写入属性。
	 */
	List<PropertyAccessor> getPropertyAccessors();

	/**
	 * 返回一组解析器，将依次请求它们定位构造器。
	 */
	List<ConstructorResolver> getConstructorResolvers();

	/**
	 * 返回一组解析器，将依次请求它们定位方法。
	 */
	List<MethodResolver> getMethodResolvers();

	/**
	 * 返回一个可按名称查找 Bean 的 Bean 解析器。
	 */
	@Nullable
	BeanResolver getBeanResolver();

	/**
	 * 返回一个类型定位器，可用于按短名称或完全限定名查找类型。
	 */
	TypeLocator getTypeLocator();

	/**
	 * 返回一个类型转换器，可将值从一种类型转换（或强制转换）为另一种类型。
	 */
	TypeConverter getTypeConverter();

	/**
	 * 返回一个类型比较器，用于比较对象对是否相等。
	 */
	TypeComparator getTypeComparator();

	/**
	 * 返回一个运算符重载器，它可能支持标准类型集之外的类型之间的数学运算。
	 */
	OperatorOverloader getOperatorOverloader();

	/**
	 * 将指定的 {@link Supplier} 创建的值分配给此求值上下文中的命名变量。
	 * <p>与 {@link #setVariable(String, Object)} 不同，仅当为支持表达式中的赋值运算符（{@code =}）时，
	 * 才应调用此方法。
	 * <p>默认情况下，此方法委托给 {@code setVariable(String, Object)}，并提供由 {@code valueSupplier}
	 * 创建的值。具体实现可以覆盖此<em>默认</em>方法，以提供不同的语义。
	 * @param name 要赋值的变量名称
	 * @param valueSupplier 提供要赋给该变量的值的 Supplier
	 * @return 包装所赋值的 {@link TypedValue}
	 * @since 5.2.24
	 */
	default TypedValue assignVariable(String name, Supplier<TypedValue> valueSupplier) {
		TypedValue typedValue = valueSupplier.get();
		setVariable(name, typedValue.getValue());
		return typedValue;
	}

	/**
	 * 将此求值上下文中的命名变量设置为指定值。
	 * <p>与 {@link #assignVariable(String, Supplier)} 不同，仅当以编程方式直接与
	 * {@code EvaluationContext} 交互时，才应调用此方法 &mdash; 例如，为上下文提供初始配置。
	 * @param name 要设置的变量名称
	 * @param value 要放入该变量的值
	 */
	void setVariable(String name, @Nullable Object value);

	/**
	 * 在此求值上下文中查找命名变量。
	 * @param name 要查找的变量名称
	 * @return 变量的值，如果未找到则为 {@code null}
	 */
	@Nullable
	Object lookupVariable(String name);

}
