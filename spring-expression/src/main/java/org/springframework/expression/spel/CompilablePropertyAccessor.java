/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.expression.PropertyAccessor;

/**
 * 可编译的属性访问器能够生成表示访问操作的字节码，从而便于将使用该访问器的表达式编译为字节码。
 *
 * @author Andy Clement
 * @since 4.1
 */
public interface CompilablePropertyAccessor extends PropertyAccessor, Opcodes {

	/**
	 * 如果此属性访问器当前适合编译，则返回 {@code true}。
	 */
	boolean isCompilable();

	/**
	 * 返回被访问属性的类型——可能只有在发生访问后才能确定。
	 */
	Class<?> getPropertyType();

	/**
	 * 在必要时使用 codeflow 中的上下文信息，将执行访问操作的字节码生成到指定的 MethodVisitor 中。
	 * @param propertyName 属性名称
	 * @param mv 生成代码的目标 Asm 方法访问器
	 * @param cf 表达式编译器的当前状态
	 */
	void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf);

}
