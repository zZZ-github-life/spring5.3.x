/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.context.ApplicationEvent;
import org.springframework.test.context.TestContext;

/**
 * 由 {@link EventPublishingTestExecutionListener} 发布的事件的基类。
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 */
@SuppressWarnings("serial")
public abstract class TestContextEvent extends ApplicationEvent {

	/**
	 * 创建一个新的 {@code TestContextEvent}。
	 * @param source 与此事件关联的 {@code TestContext}（不能为 {@code null}）
	 */
	public TestContextEvent(TestContext source) {
		super(source);
	}

	/**
	 * 获取与此事件关联的 {@link TestContext}。
	 * @return 与此事件关联的 {@code TestContext}（从不为 {@code null}）
	 * @see #getTestContext()
	 */
	@Override
	public final TestContext getSource() {
		return (TestContext) super.getSource();
	}

	/**
	 * 作为 {@link #getSource()} 的别名。
	 * <p>在事件处理的 SpEL 表达式{@linkplain org.springframework.context.event.EventListener#condition 条件}中，此方法可能比 {@code getSource()} 更受欢迎——例如，为了提高可读性。
	 * @return 与此事件关联的 {@code TestContext}（从不为 {@code null}）
	 * @see #getSource()
	 */
	public final TestContext getTestContext() {
		return getSource();
	}

}
