/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于集成测试的方法级注解，需要向 {@code Environment} 的
 * {@code PropertySources} 集合添加具有动态值的属性。
 *
 * <p>此注解及其支持基础设施最初旨在让基于
 * <a href="https://www.testcontainers.org/">Testcontainers</a> 的测试中的属性能够
 * 轻松暴露给 Spring 集成测试。不过，此功能也可用于
 * 生命周期在测试的 {@code ApplicationContext} 之外维护的任何形式的外部资源。
 *
 * <p>使用 {@code @DynamicPropertySource} 注解的方法必须是 {@code static}，
 * 并且必须具有单个 {@link DynamicPropertyRegistry} 参数，用于
 * 向 {@code Environment} 的 {@code PropertySources} 集合添加<em>名称-值</em>对。
 * 值是动态的，通过 {@link java.util.function.Supplier} 提供，仅在解析属性时
 * 才会调用。通常使用方法引用来提供值，如下例所示。
 *
 * <p>从 Spring Framework 5.3.2 开始，来自使用 {@code @DynamicPropertySource}
 * 注解的方法的动态属性将从封闭测试类<em>继承</em>，
 * 类似于从超类和接口继承。详情请参见
 * {@link NestedTestConfiguration @NestedTestConfiguration}。
 *
 * <p><strong>注意</strong>：如果在基类中使用 {@code @DynamicPropertySource}，
 * 并发现子类中的测试失败，因为动态属性在子类之间发生变化，
 * 则可能需要使用 {@link org.springframework.test.annotation.DirtiesContext @DirtiesContext}
 * 注解基类，以确保每个子类获得具有正确动态属性的自己的
 * {@code ApplicationContext}。
 *
 * <h3>优先级</h3>
 * <p>动态属性的优先级高于从
 * {@link TestPropertySource @TestPropertySource}、操作系统
 * 环境、Java 系统属性或应用程序通过
 * {@link org.springframework.context.annotation.PropertySource @PropertySource}
 * 以声明方式或编程方式添加的属性源加载的属性。因此，动态属性可用于有选择地
 * 覆盖通过 {@code @TestPropertySource}、系统属性源
 * 和应用程序属性源加载的属性。
 *
 * <h3>示例</h3>
 * <pre class="code">
 * &#064;SpringJUnitConfig(...)
 * &#064;Testcontainers
 * class ExampleIntegrationTests {
 *
 * &#064;Container
 * static GenericContainer redis =
 * new GenericContainer("redis:5.0.3-alpine").withExposedPorts(6379);
 *
 * // ...
 *
 * &#064;DynamicPropertySource
 * static void redisProperties(DynamicPropertyRegistry registry) {
 * registry.add("redis.host", redis::getHost);
 * registry.add("redis.port", redis::getFirstMappedPort);
 * }
 *
 * }</pre>
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertyRegistry
 * @see ContextConfiguration
 * @see TestPropertySource
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.test.annotation.DirtiesContext
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicPropertySource {
}
