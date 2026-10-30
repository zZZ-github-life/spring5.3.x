/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.oxm;

import java.lang.reflect.Type;

/**
 * {@link Unmarshaller} 的子接口，提供对泛型的支持。
 *
 * @author Arjen Poutsma
 * @since 3.0.1
 */
public interface GenericUnmarshaller extends Unmarshaller {

	/**
	 * 指示此 unmarshaller 是否能够 unmarshal 所提供泛型类型的实例。
	 * @param genericType 询问此 marshaller 是否能编组的类型
	 * @return 如果此 marshaller 确实能编组所提供类型的实例，则返回 {@code true}；否则返回 {@code false}
	 */
	boolean supports(Type genericType);

}
