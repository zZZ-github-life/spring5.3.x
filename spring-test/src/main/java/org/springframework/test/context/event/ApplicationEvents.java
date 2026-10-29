/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.stream.Stream;

import org.springframework.context.ApplicationEvent;

/**
 * {@code ApplicationEvents} 封装了在单个测试方法执行期间发布的所有{@linkplain ApplicationEvent 应用程序事件}。
 * <p>要在测试中使用 {@code ApplicationEvents}，请执行以下操作。
 * <ul>
 * <li>确保测试类标注或元标注了 {@link RecordApplicationEvents @RecordApplicationEvents}。</li>
 * <li>确保 {@link ApplicationEventsTestExecutionListener} 已注册。但请注意，它默认已注册，只有在通过 {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners} 进行自定义配置且未包含默认监听器时，才需要手动注册它。</li>
 * <li>使用 {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 注解 {@code ApplicationEvents} 类型的字段，并在测试方法和生命周期方法中使用该 {@code ApplicationEvents} 实例。</li>
 * <li>使用 JUnit Jupiter 时，可以选择在测试或生命周期方法中声明 {@code ApplicationEvents} 类型的参数，作为测试类中 {@code @Autowired} 字段的替代方案。</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 * @see RecordApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 * @see org.springframework.context.ApplicationEvent
 */
public interface ApplicationEvents {

	/**
	 * 流式获取测试执行期间发布的所有应用程序事件。
	 * @return a stream of all application events
	 * @see #stream(Class)
	 * @see #clear()
	 */
	Stream<ApplicationEvent> stream();

	/**
	 * 流式获取测试执行期间发布的指定类型的所有应用程序事件或事件负载。
	 * @param <T> the event type
	 * @param type the type of events or payloads to stream; never {@code null}
	 * @return a stream of all application events or event payloads of the
	 * specified type
	 * @see #stream()
	 * @see #clear()
	 */
	<T> Stream<T> stream(Class<T> type);

	/**
	 * 清除此 {@code ApplicationEvents} 实例记录的所有应用程序事件。
	 * <p>后续对 {@link #stream()} 或 {@link #stream(Class)} 的调用将只包含自调用此方法以来记录的事件。
	 * @see #stream()
	 * @see #stream(Class)
	 */
	void clear();

}
