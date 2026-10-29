/*
 * Copyright 2002-2021 the original author or authors.
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
 * 方法解析器尝试定位一个方法，并返回可用于调用该方法的命令执行器。命令执行器会被缓存，但如果它“变
 * 得陈旧”，解析器将再次被调用。
 *
 * @author Andy Clement
 * @since 3.0
 */
public interface MethodResolver {

	/**
	 * 在提供的上下文中，在提供的对象上确定一个能够处理指定参数的合适方法。返回一个可用于调用该方法的
	 * {@link MethodExecutor}，如果找不到方法则返回 {@code null}。
	 * @param context the current evaluation context
	 * @param targetObject the object upon which the method is being called
	 * @param argumentTypes the arguments that the constructor must be able to handle
	 * @return a MethodExecutor that can invoke the method, or null if the method cannot be found
	 */
	@Nullable
	MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
			List<TypeDescriptor> argumentTypes) throws AccessException;

}
