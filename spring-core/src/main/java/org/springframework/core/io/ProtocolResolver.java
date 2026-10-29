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

package org.springframework.core.io;

import org.springframework.lang.Nullable;

/**
 * 用于协议特定资源句柄的解析策略。
 *
 * <p>用作 {@link DefaultResourceLoader} 的 SPI，允许处理自定义协议而无需子类化加载器实现（或应用上
 * 下文实现）。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see DefaultResourceLoader#addProtocolResolver
 */
@FunctionalInterface
public interface ProtocolResolver {

	/**
	 * 如果此实现的协议匹配，则针对给定的资源加载器解析给定位置。
	 * @param location the user-specified resource location
	 * @param resourceLoader the associated resource loader
	 * @return a corresponding {@code Resource} handle if the given location
	 * matches this resolver's protocol, or {@code null} otherwise
	 */
	@Nullable
	Resource resolve(String location, ResourceLoader resourceLoader);

}
