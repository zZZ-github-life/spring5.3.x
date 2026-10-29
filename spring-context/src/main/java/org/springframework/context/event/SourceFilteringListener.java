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
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * 一个 {@link org.springframework.context.ApplicationListener} 装饰器，用于过滤来自指定事件源的事
 * 件，仅对匹配的 {@link org.springframework.context.ApplicationEvent} 对象调用其委托监听器。
 *
 * <p>也可以用作基类，此时重写 {@link #onApplicationEventInternal} 方法，而不是指定委托监听器。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0.5
 */
public class SourceFilteringListener implements GenericApplicationListener {

	private final Object source;

	@Nullable
	private GenericApplicationListener delegate;


	/**
	 * 为给定事件源创建一个 SourceFilteringListener。
	 * @param source the event source that this listener filters for,
	 * only processing events from this source
	 * @param delegate the delegate listener to invoke with event
	 * from the specified source
	 */
	public SourceFilteringListener(Object source, ApplicationListener<?> delegate) {
		this.source = source;
		this.delegate = (delegate instanceof GenericApplicationListener ?
				(GenericApplicationListener) delegate : new GenericApplicationListenerAdapter(delegate));
	}

	/**
	 * 为给定事件源创建一个 SourceFilteringListener，并期望子类重写 {@link #onApplicationEventInternal}
	 * 方法（而不是指定委托监听器）。
	 * @param source the event source that this listener filters for,
	 * only processing events from this source
	 */
	protected SourceFilteringListener(Object source) {
		this.source = source;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event.getSource() == this.source) {
			onApplicationEventInternal(event);
		}
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		return (this.delegate == null || this.delegate.supportsEventType(eventType));
	}

	@Override
	public boolean supportsSourceType(@Nullable Class<?> sourceType) {
		return (sourceType != null && sourceType.isInstance(this.source));
	}

	@Override
	public int getOrder() {
		return (this.delegate != null ? this.delegate.getOrder() : Ordered.LOWEST_PRECEDENCE);
	}

	@Override
	public String getListenerId() {
		return (this.delegate != null ? this.delegate.getListenerId() : "");
	}


	/**
	 * 在已按所需事件源进行过滤之后，实际处理该事件。<p>默认实现会调用指定的委托（如果有）。
	 * @param event the event to process (matching the specified source)
	 */
	protected void onApplicationEventInternal(ApplicationEvent event) {
		if (this.delegate == null) {
			throw new IllegalStateException(
					"Must specify a delegate object or override the onApplicationEventInternal method");
		}
		this.delegate.onApplicationEvent(event);
	}

}
