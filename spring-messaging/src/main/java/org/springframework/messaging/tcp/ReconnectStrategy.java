/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.tcp;

import org.springframework.lang.Nullable;

/**
 * 用于确定连接失败后重连尝试频率的契约。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@FunctionalInterface
public interface ReconnectStrategy {

	/**
	 * 返回距下一次重连尝试的时间。
	 * @param attemptCount 已经进行了多少次重连尝试
	 * @return 时间量，单位为毫秒，或 {@code null} 表示停止
	 */
	@Nullable
	Long getTimeToNextAttempt(int attemptCount);

}
