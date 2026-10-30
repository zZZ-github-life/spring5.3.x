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

import org.springframework.lang.Nullable;

/**
 * 类型比较器的实例应能够比较对象对是否相等。返回值的规范与 {@link java.lang.Comparable} 相同。
 *
 * @author Andy Clement
 * @since 3.0
 * @see java.lang.Comparable
 */
public interface TypeComparator {

	/**
	 * 如果比较器可以比较这两个对象，则返回 {@code true}。
	 * @param firstObject 第一个对象
	 * @param secondObject 第二个对象
	 * @return 如果比较器能够比较这些对象则返回 {@code true}
	 */
	boolean canCompare(@Nullable Object firstObject, @Nullable Object secondObject);

	/**
	 * 比较两个给定的对象。
	 * @param firstObject 第一个对象
	 * @param secondObject 第二个对象
	 * @return 如果两者相等则为 0；如果第一个小于第二个则为负数；
	 * 如果第一个大于第二个则为正数
	 * @throws EvaluationException 如果比较过程中出现问题
	 * （或两者本来就不可比较）
	 * @see Comparable#compareTo
	 */
	int compare(@Nullable Object firstObject, @Nullable Object secondObject) throws EvaluationException;

}
