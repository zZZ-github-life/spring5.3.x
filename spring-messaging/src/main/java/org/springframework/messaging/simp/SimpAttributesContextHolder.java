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

package org.springframework.messaging.simp;

import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * 持有类，以线程绑定的 {@link SimpAttributes} 对象形式公开与会话
 * （例如 WebSocket）关联的 SiMP 属性。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class SimpAttributesContextHolder {

	private static final ThreadLocal<SimpAttributes> attributesHolder =
			new NamedThreadLocal<>("SiMP session attributes");


	/**
	 * 重置当前线程的 SimpAttributes。
	 */
	public static void resetAttributes() {
		attributesHolder.remove();
	}

	/**
	 * 将给定 SimpAttributes 绑定到当前线程。
	 * @param attributes 要暴露的 RequestAttributes
	 */
	public static void setAttributes(@Nullable SimpAttributes attributes) {
		if (attributes != null) {
			attributesHolder.set(attributes);
		}
		else {
			resetAttributes();
		}
	}

	/**
	 * 从给定消息中提取 SiMP 会话属性，将其包装在 {@link SimpAttributes} 实例中，
	 * 并绑定到当前线程。
	 * @param message 要从中提取会话属性的消息
	 */
	public static void setAttributesFromMessage(Message<?> message) {
		setAttributes(SimpAttributes.fromMessage(message));
	}

	/**
	 * 返回当前绑定到线程的 SimpAttributes。
	 * @return 属性，如果未绑定则为 {@code null}
	 */
	@Nullable
	public static SimpAttributes getAttributes() {
		return attributesHolder.get();
	}

	/**
	 * 返回当前绑定到线程的 SimpAttributes；如果未绑定，
	 * 则抛出 {@link java.lang.IllegalStateException}。
	 * @return 属性，永不为 {@code null}
	 * @throws java.lang.IllegalStateException 如果属性未绑定
	 */
	public static SimpAttributes currentAttributes() throws IllegalStateException {
		SimpAttributes attributes = getAttributes();
		if (attributes == null) {
			throw new IllegalStateException("No thread-bound SimpAttributes found. " +
					"Your code is probably not processing a client message and executing in " +
					"message-handling methods invoked by the SimpAnnotationMethodMessageHandler?");
		}
		return attributes;
	}

}
