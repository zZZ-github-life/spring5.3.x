/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.handler;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * 将条件映射到消息的契约。
 *
 * <p>消息条件可以组合（例如类型级 + 方法级条件），可以与特定 Message 匹配，
 * 也可以在 Message 的上下文中相互比较，以确定哪一个更接近请求。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> the kind of condition that this condition can be combined with or compared to
 */
public interface MessageCondition<T> {

	/**
	 * 定义将此条件与另一个条件组合的规则。
	 * 例如组合类型级和方法级条件。
	 * @param other the condition to combine with
	 * @return the resulting message condition
	 */
	T combine(T other);

	/**
	 * 检查此条件是否匹配给定 Message，并返回一个内容针对当前消息定制的新条件
	 * （可能是新实例）。例如，包含目标模式的条件可能返回一个仅包含排序后匹配模式的新条件。
	 * @return a condition instance in case of a match; or {@code null} if there is no match.
	 */
	@Nullable
	T getMatchingCondition(Message<?> message);

	/**
	 * 在特定消息的上下文中将此条件与另一个条件进行比较。
	 * 假定两个实例都是通过 {@link #getMatchingCondition(Message)} 获取的，
	 * 以确保它们仅包含与当前消息相关的内容。
	 */
	int compareTo(T other, Message<?> message);

}
