/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

/**
 * 构造器解析器尝试定位一个构造器，并返回可用于调用该构造器的 ConstructorExecutor。
 * ConstructorExecutor 会被缓存，但如果它“变得陈旧”，解析器将再次被调用。
 *
 * @author Andy Clement
 * @since 3.0
 */
@FunctionalInterface
public interface ConstructorResolver {

	/**
	 * 在提供的上下文中，在提供的类型上确定一个能够处理指定参数的合适构造器。返回一个可用于调用该构造器
	 * 的 ConstructorExecutor（如果找不到构造器，则返回 {@code null}）。
	 * @param context 当前的求值上下文
	 * @param typeName 要在其上查找构造器的类型
	 * @param argumentTypes 构造器必须能够处理的参数
	 * @return 可调用该构造器的 ConstructorExecutor，如果未找到则为 null
	 */
	@Nullable
	ConstructorExecutor resolve(EvaluationContext context, String typeName, List<TypeDescriptor> argumentTypes)
			throws AccessException;

}
