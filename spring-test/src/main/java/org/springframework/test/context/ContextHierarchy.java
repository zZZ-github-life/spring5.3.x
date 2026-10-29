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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ContextHierarchy} 是一个类级注解，用于为集成测试定义
 * {@link org.springframework.context.ApplicationContext
 * ApplicationContext} 的层次结构。
 *
 * <h3>示例</h3>
 * <p>以下基于 JUnit 的示例演示了需要上下文层次结构的集成测试
 * 的常见配置场景。
 *
 * <h4>带上下文层次结构的单个测试类</h4>
 * <p>{@code ControllerIntegrationTests} 通过声明由两个级别组成的上下文层次结构，
 * 展示了 Spring MVC Web 应用程序的典型集成测试场景：
 * 一个用于<em>根</em> {@code WebApplicationContext}
 * （使用 {@code TestAppConfig}），另一个用于<em>调度器 Servlet</em>
 * {@code WebApplicationContext}（使用 {@code WebConfig}）。<em>自动装配</em>到测试实例中的
 * {@code WebApplicationContext} 是子上下文的（即层次结构中最低的上下文）。
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;WebAppConfiguration
 * &#064;ContextHierarchy({
 * &#064;ContextConfiguration(classes = TestAppConfig.class),
 * &#064;ContextConfiguration(classes = WebConfig.class)
 * })
 * public class ControllerIntegrationTests {
 *
 * &#064;Autowired
 * private WebApplicationContext wac;
 *
 * // ...
 * }</pre>
 *
 * <h4>带隐式父上下文的类层次结构</h4>
 * <p>以下测试类在测试类层次结构中定义上下文层次结构。
 * {@code AbstractWebTests} 声明了 Spring 驱动的 Web 应用程序中根
 * {@code WebApplicationContext} 的配置。但请注意，
 * {@code AbstractWebTests} 并未声明 {@code @ContextHierarchy}；
 * 因此，{@code AbstractWebTests} 的子类可以选择参与
 * 上下文层次结构，或遵循 {@code @ContextConfiguration} 的标准语义。
 * {@code SoapWebServiceTests} 和 {@code RestWebServiceTests} 都扩展了
 * {@code AbstractWebTests}，并通过 {@code @ContextHierarchy} 定义上下文层次结构。
 * 结果是，将加载三个应用程序上下文（每个
 * {@code @ContextConfiguration} 声明对应一个），并且基于
 * {@code AbstractWebTests} 中的配置加载的应用程序上下文将设置为
 * 具体子类所加载的每个上下文的父上下文。
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;WebAppConfiguration
 * &#064;ContextConfiguration("file:src/main/webapp/WEB-INF/applicationContext.xml")
 * public abstract class AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/spring/soap-ws-config.xml"))
 * public class SoapWebServiceTests extends AbstractWebTests {}
 *
 * &#064;ContextHierarchy(&#064;ContextConfiguration("/spring/rest-ws-config.xml"))
 * public class RestWebServiceTests extends AbstractWebTests {}</pre>
 *
 * <h4>带合并上下文层次结构配置的类层次结构</h4>
 * <p>以下类演示了如何使用<em>命名</em>层次结构级别
 * 来<em>合并</em>上下文层次结构中特定级别的配置。
 * {@code BaseTests} 定义了层次结构中的两个级别：{@code parent}
 * 和 {@code child}。{@code ExtendedTests} 扩展了 {@code BaseTests}，并指示
 * Spring TestContext Framework 合并
 * {@code child} 层次结构级别的上下文配置，只需确保通过
 * {@link ContextConfiguration#name} 声明的名称都是 {@code "child"}。结果是，
 * 将加载三个应用程序上下文：一个用于 {@code "/app-config.xml"}，
 * 一个用于 {@code "/user-config.xml"}，还有一个用于 <code>{"/user-config.xml",
 * "/order-config.xml"}</code>。与前一个示例一样，从
 * {@code "/app-config.xml"} 加载的应用程序上下文将设置为
 * 从 {@code "/user-config.xml"} 和 <code>{"/user-config.xml",
 * "/order-config.xml"}</code> 加载的上下文的父上下文。
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;ContextHierarchy({
 * &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 * &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 * &#064;ContextConfiguration(name = "child",  locations = "/order-config.xml")
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <h4>带覆盖上下文层次结构配置的类层次结构</h4>
 * <p>与前一个示例相比，此示例演示如何通过将
 * {@link ContextConfiguration#inheritLocations} 标志设置为 {@code false}
 * 来<em>覆盖</em>上下文层次结构中给定命名级别的配置。
 * 因此，{@code ExtendedTests} 的应用程序上下文将仅从
 * {@code "/test-user-config.xml"} 加载，并将其父上下文设置为
 * 从 {@code "/app-config.xml"} 加载的上下文。
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;ContextHierarchy({
 * &#064;ContextConfiguration(name = "parent", locations = "/app-config.xml"),
 * &#064;ContextConfiguration(name = "child",  locations = "/user-config.xml")
 * })
 * public class BaseTests {}
 *
 * &#064;ContextHierarchy(
 * &#064;ContextConfiguration(name = "child",  locations = "/test-user-config.xml", inheritLocations = false)
 * )
 * public class ExtendedTests extends BaseTests {}</pre>
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * <p>从 Spring Framework 5.3 开始，此注解默认将从
 * 外层测试类继承。详情请参见
 * {@link NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 3.2.2
 * @see ContextConfiguration
 * @see org.springframework.context.ApplicationContext
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextHierarchy {

	/**
	 * 一组 {@link ContextConfiguration @ContextConfiguration} 实例，
	 * 每个实例定义上下文层次结构中的一个级别。
	 * <p>如果需要在测试类层次结构中合并或覆盖上下文层次结构
	 * 给定级别的配置，必须在
	 * 类层次结构的每个级别向 {@code @ContextConfiguration} 的 {@link ContextConfiguration#name
	 * name} 属性提供相同的值来显式命名该级别。示例请参见类级 Javadoc。
	 */
	ContextConfiguration[] value();

}
