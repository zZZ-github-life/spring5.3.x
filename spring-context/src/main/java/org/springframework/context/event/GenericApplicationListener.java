/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;

/**
 * 标准 {@link ApplicationListener} 接口的扩展变体，公开更多元数据，如支持的事件类型和源类型。
 *
 * <p>自 Spring Framework 4.2 起，此接口取代基于 Class 的 {@link SmartApplicationListener}，可完整
 * 处理泛型事件类型。自 5.3.5 起，它正式扩展 {@link SmartApplicationListener}，并通过默认方法将
 * {@link #supportsEventType(Class)} 适配为 {@link #supportsEventType(ResolvableType)}。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.2
 * @see SmartApplicationListener
 * @see GenericApplicationListenerAdapter
 */
public interface GenericApplicationListener extends SmartApplicationListener {

	/**
	 * 重写 {@link SmartApplicationListener#supportsEventType(Class)}，委托给
	 * {@link #supportsEventType(ResolvableType)}。
	 */
	@Override
	default boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return supportsEventType(ResolvableType.forClass(eventType));
	}

	/**
	 * 判断此监听器是否实际支持给定的事件类型。
	 * @param eventType the event type (never {@code null})
	 */
	boolean supportsEventType(ResolvableType eventType);

}
