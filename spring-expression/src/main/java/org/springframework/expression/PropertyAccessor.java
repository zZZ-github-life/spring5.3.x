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

package org.springframework.expression;

import org.springframework.lang.Nullable;

/**
 * 属性访问器能够读取（并可能写入）对象的属性。
 *
 * <p>此接口不作任何限制，因此实现者可以自由地以字段形式直接访问属性，或通过 getter 访问，或采用其
 * 认为合适的任何其他方式。
 *
 * <p>解析器可以选择性地指定一个目标类数组，它应针对这些类被调用。但是，如果它从
 * {@link #getSpecificTargetClasses()} 返回 {@code null}，则会对所有属性引用调用它，并给它机会判断
 * 能否读取或写入这些属性。
 *
 * <p>属性解析器被视为有序的，每个都会被依次调用。影响调用顺序的唯一规则是：任何在
 * {@link #getSpecificTargetClasses()} 中直接指定目标类的解析器都会先于通用解析器被调用。
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface PropertyAccessor {

	/**
	 * 返回应调用此解析器的类数组。
	 * <p>返回 {@code null} 表示这是一个通用解析器，可被调用以尝试解析任何类型上的属性。
	 * @return 此解析器适用的类数组
	 * （如果是通用解析器则为 {@code null}）
	 */
	@Nullable
	Class<?>[] getSpecificTargetClasses();

	/**
	 * 调用以判断解析器实例是否能够访问指定目标对象上的指定属性。
	 * @param context 尝试进行访问所在的求值上下文
	 * @param target 访问属性所针对的目标对象
	 * @param name 所访问属性的名称
	 * @return 如果此解析器能够读取该属性则返回 true
	 * @throws AccessException 如果确定该属性是否可读时出现任何问题
	 */
	boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

	/**
	 * 调用以从指定目标对象读取属性。仅当 {@link #canRead} 也返回 {@code true} 时才应成功。
	 * @param context 尝试进行访问所在的求值上下文
	 * @param target 访问属性所针对的目标对象
	 * @param name 所访问属性的名称
	 * @return 包装所读取属性值的 TypedValue 对象
	 * 以及该值的类型描述符
	 * @throws AccessException 如果访问属性值时出现任何问题
	 */
	TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

	/**
	 * 调用以判断解析器实例是否能够写入指定目标对象上的指定属性。
	 * @param context 尝试进行访问所在的求值上下文
	 * @param target 访问属性所针对的目标对象
	 * @param name 所访问属性的名称
	 * @return 如果此解析器能够写入该属性则返回 true
	 * @throws AccessException 如果确定该属性是否可写时出现任何问题
	 */
	boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException;

	/**
	 * 调用以写入指定目标对象上的属性。仅当 {@link #canWrite} 也返回 {@code true} 时才应成功。
	 * @param context 尝试进行访问所在的求值上下文
	 * @param target 访问属性所针对的目标对象
	 * @param name 所访问属性的名称
	 * @param newValue 属性的新值
	 * @throws AccessException 如果写入属性值时出现任何问题
	 */
	void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException;

}
