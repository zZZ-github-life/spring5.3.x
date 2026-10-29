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

package org.springframework.messaging.simp.user;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * 一种“用户”目标解析策略，将其转换为一个或多个实际目标，每个活跃用户会话对应一个。
 * 向用户目标发送消息时，目标必须包含用户名，以便提取该用户名并用于查找用户会话。
 * 订阅用户目标时，目标不必包含用户自己的名称，我们只需使用当前会话。
 *
 * <p>有关示例目标，请参阅实现类和文档。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see org.springframework.messaging.simp.user.DefaultUserDestinationResolver
 * @see UserDestinationMessageHandler
 */
@FunctionalInterface
public interface UserDestinationResolver {

	/**
	 * 将带有用户目标的给定消息解析为一个或多个带有实际目标的消息，
	 * 每个活跃用户会话对应一个。
	 * @param message the message to try to resolve
	 * @return 0 or more target messages (one for each active session), or
	 * {@code null} if the source message does not contain a user destination.
	 */
	@Nullable
	UserDestinationResult resolveDestination(Message<?> message);

}
