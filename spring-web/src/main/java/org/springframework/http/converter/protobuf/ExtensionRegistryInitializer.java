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

package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;

/**
 * Google Protocol Message 可以包含消息扩展，前提是在
 * {@code ExtensionRegistry} 中注册了适当的配置。
 *
 * <p>此接口提供了一种填充 {@code ExtensionRegistry} 的机制。
 *
 * @author Alex Antonov
 * @author Sebastien Deleuze
 * @since 4.1
 * @see <a href="https://developers.google.com/protocol-buffers/docs/reference/java/com/google/protobuf/ExtensionRegistry">
 * com.google.protobuf.ExtensionRegistry</a>
 * @deprecated as of Spring Framework 5.1, use {@link ExtensionRegistry} based constructors instead
 */
@Deprecated
public interface ExtensionRegistryInitializer {

	/**
	 * 使用 Protocol Message 扩展初始化 {@code ExtensionRegistry}。
	 * @param registry 要填充的注册表
	 */
	void initializeExtensionRegistry(ExtensionRegistry registry);

}
