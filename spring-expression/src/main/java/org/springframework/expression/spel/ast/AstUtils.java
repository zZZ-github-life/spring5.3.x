/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.PropertyAccessor;
import org.springframework.lang.Nullable;

/**
 * 供 AST 类使用的工具方法。
 *
 * @author Andy Clement
 * @since 3.0.2
 */
public abstract class AstUtils {

	/**
	 * 确定应当使用哪一组属性解析器，以尝试访问指定目标类型上的属性。这些解析器被视为处于一个有序列表中，
	 * 但在返回的列表中，凡是与输入目标类型精确匹配的解析器（相对于可用于任何类型的“通用”解析器而言）
	 * 都排在列表开头。此外，还有一些特定解析器，它们精确命名了所涉的类，另一些则命名了某个特定类、但该
	 * 类是当前类的超类型。后者被放在特定解析器集合的末尾，会在精确匹配的访问器之后、通用访问器之前被尝
	 * 试。
	 * @param targetType the type upon which property access is being attempted
	 * @return a list of resolvers that should be tried in order to access the property
	 */
	public static List<PropertyAccessor> getPropertyAccessorsToTry(
			@Nullable Class<?> targetType, List<PropertyAccessor> propertyAccessors) {

		List<PropertyAccessor> specificAccessors = new ArrayList<>();
		List<PropertyAccessor> generalAccessors = new ArrayList<>();
		for (PropertyAccessor resolver : propertyAccessors) {
			Class<?>[] targets = resolver.getSpecificTargetClasses();
			if (targets == null) {  // generic resolver that says it can be used for any type
				generalAccessors.add(resolver);
			}
			else {
				if (targetType != null) {
					for (Class<?> clazz : targets) {
						if (clazz == targetType) {  // put exact matches on the front to be tried first?
							specificAccessors.add(resolver);
						}
						else if (clazz.isAssignableFrom(targetType)) {  // put supertype matches at the end of the
							// specificAccessor list
							generalAccessors.add(resolver);
						}
					}
				}
			}
		}
		List<PropertyAccessor> resolvers = new ArrayList<>(specificAccessors.size() + generalAccessors.size());
		resolvers.addAll(specificAccessors);
		resolvers.addAll(generalAccessors);
		return resolvers;
	}

}
