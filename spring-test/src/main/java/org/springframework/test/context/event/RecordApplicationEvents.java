/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @RecordApplicationEvents} 是一个类级注解，用于指示 <em>Spring TestContext Framework</em> 记录在单个测试执行期间发布到 {@link org.springframework.context.ApplicationContext ApplicationContext} 中的所有{@linkplain org.springframework.context.ApplicationEvent 应用程序事件}。
 * <p>可以在测试中通过 {@link ApplicationEvents} API 访问记录的事件。
 * <p>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>。
 * <p>此注解默认会从封闭测试类继承。有关详情，请参见 {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 5.3.3
 * @see ApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RecordApplicationEvents {
}
