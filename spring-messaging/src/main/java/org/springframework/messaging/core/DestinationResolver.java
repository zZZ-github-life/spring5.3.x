/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.messaging.core;

/**
 * 将 String 目标名称解析为类型为 {@code <D>} 的实际目标的策略。
 *
 * @author Mark Fisher
 * @since 4.0
 * @param <D> 目标类型
 */
@FunctionalInterface
public interface DestinationResolver<D> {

	/**
	 * 解析给定的目标名称。
	 * @param name 要解析的目的地名
	 * @return 解析得到的目的地（永远不会为 {@code null}）
	 * @throws DestinationResolutionException 如果找不到指定的目的地，或因其他原因无法解析
	 */
	D resolveDestination(String name) throws DestinationResolutionException;

}
