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

package org.springframework.test.context.event;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 持有类，用于以线程绑定的 {@link ApplicationEvents} 对象形式公开测试执行期间发布的应用程序事件。
 * <p>{@code ApplicationEvents} 在此持有器中注册，并由 {@link ApplicationEventsTestExecutionListener} 管理。
 * <p>尽管此类是 {@code public} 的，但它仅打算在 <em>Spring TestContext Framework</em> 内或第三方扩展的实现中使用。因此，在使用 JUnit Jupiter 和 {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension} 时，测试作者应允许将当前的 {@code ApplicationEvents} 实例通过 {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 注入测试类的字段中，或通过测试和生命周期方法中的参数注入。
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 * @see ApplicationEvents
 * @see RecordApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 */
public abstract class ApplicationEventsHolder {

	private static final ThreadLocal<DefaultApplicationEvents> applicationEvents = new ThreadLocal<>();


	private ApplicationEventsHolder() {
		// no-op to prevent instantiation of this holder class
	}


	/**
	 * 获取当前线程的 {@link ApplicationEvents}。
	 * @return 当前的 {@code ApplicationEvents}；如果未注册，则为 {@code null}
	 */
	@Nullable
	public static ApplicationEvents getApplicationEvents() {
		return applicationEvents.get();
	}

	/**
	 * 获取当前线程的 {@link ApplicationEvents}。
	 * @return 当前的 {@code ApplicationEvents}
	 * @throws IllegalStateException 如果当前线程尚未注册 {@code ApplicationEvents} 实例
	 */
	public static ApplicationEvents getRequiredApplicationEvents() {
		ApplicationEvents events = applicationEvents.get();
		Assert.state(events != null, "Failed to retrieve ApplicationEvents for the current thread. " +
				"Ensure that your test class is annotated with @RecordApplicationEvents " +
				"and that the ApplicationEventsTestExecutionListener is registered.");
		return events;
	}


	/**
	 * 如有必要，为当前线程注册一个新的 {@link DefaultApplicationEvents} 实例以供使用。
	 * <p>如果已为当前线程调用过 {@link #registerApplicationEvents()}，则此方法不执行任何操作。
	 */
	static void registerApplicationEventsIfNecessary() {
		if (getApplicationEvents() == null) {
			registerApplicationEvents();
		}
	}

	/**
	 * 为当前线程注册一个新的 {@link DefaultApplicationEvents} 实例以供使用。
	 */
	static void registerApplicationEvents() {
		applicationEvents.set(new DefaultApplicationEvents());
	}

	/**
	 * 移除当前线程的 {@link ApplicationEvents} 注册。
	 */
	static void unregisterApplicationEvents() {
		applicationEvents.remove();
	}

}
