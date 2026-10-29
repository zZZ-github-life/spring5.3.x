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

/**
 * 此接口的实现者应能够定位类型。它们可以按自己的方式使用自定义的 {@link ClassLoader} 和/或处理常见
 * 的包前缀（例如 {@code java.lang}）。
 *
 * 有关示例实现，请参见 {@link org.springframework.expression.spel.support.StandardTypeLocator}。
 *
 * @author Andy Clement
 * @since 3.0
 */
@FunctionalInterface
public interface TypeLocator {

	/**
	 * 按名称查找类型。名称可以带完整限定，也可以不带（例如 {@code String} 或 {@code java.lang.String}）。
	 * @param typeName the type to be located
	 * @return the {@code Class} object representing that type
	 * @throws EvaluationException if there is a problem finding the type
	 */
	Class<?> findType(String typeName) throws EvaluationException;

}
