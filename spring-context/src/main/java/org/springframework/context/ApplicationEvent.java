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

package org.springframework.context;

import java.time.Clock;
import java.util.EventObject;

/**
 * 所有应用事件都应扩展的类。
 * 由于通用事件被直接发布没有意义，因此该类是抽象的。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.EventListener
 */
public abstract class ApplicationEvent extends EventObject {

	/** 使用 Spring 1.2 的 serialVersionUID 以实现互操作性。 */
	private static final long serialVersionUID = 7099057708183571937L;

	/** 事件发生时的系统时间。 */
	private final long timestamp;


	/**
	 * 创建一个新的 {@code ApplicationEvent}，其 {@link #getTimestamp() 时间戳} 设置为
	 * {@link System#currentTimeMillis()}。
	 * @param source 事件最初发生或与之关联的对象（永不为 {@code null}）
	 * @see #ApplicationEvent(Object, Clock)
	 */
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * 创建一个新的 {@code ApplicationEvent}，其 {@link #getTimestamp() 时间戳} 设置为所提供的
	 * {@link Clock} 中 {@link Clock#millis()} 返回的值。
	 *
	 * <p>此构造函数通常用于测试场景。
	 * @param source 事件最初发生或与之关联的对象（永不为 {@code null}）
	 * @param clock 用于提供时间戳的时钟
	 * @since 5.3.8
	 * @see #ApplicationEvent(Object)
	 */
	public ApplicationEvent(Object source, Clock clock) {
		super(source);
		this.timestamp = clock.millis();
	}


	/**
	 * 返回事件发生的时间，以毫秒为单位。
	 * @see #ApplicationEvent(Object)
	 * @see #ApplicationEvent(Object, Clock)
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

}
