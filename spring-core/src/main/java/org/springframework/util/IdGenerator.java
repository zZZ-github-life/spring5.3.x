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

package org.springframework.util;

import java.util.UUID;

/**
 * 用于生成通用唯一标识符（{@link UUID UUID}）的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@FunctionalInterface
public interface IdGenerator {

	/**
	 * 生成新的标识符。
	 * @return 生成的标识符
	 */
	UUID generateId();

}
