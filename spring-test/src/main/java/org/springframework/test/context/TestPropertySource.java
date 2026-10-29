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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code @TestPropertySource} 是一个类级注解，用于为集成测试配置属性文件的 {@link #locations} 以及内联 {@link #properties}，以将它们添加到 {@code ApplicationContext} 的 {@code Environment} 的 {@code PropertySources} 集合中。
 * <h3>优先级</h3>
 * <p>测试属性源的优先级高于从操作系统环境或 Java 系统属性加载的属性源，也高于应用程序通过 {@link org.springframework.context.annotation.PropertySource @PropertySource} 以声明方式或以编程方式（例如通过 {@link org.springframework.context.ApplicationContextInitializer ApplicationContextInitializer} 或其他方式）添加的属性源。因此，测试属性源可用于有选择地覆盖系统和应用程序属性源中定义的属性。此外，内联 {@link #properties} 的优先级高于从资源 {@link #locations} 加载的属性。但请注意，通过 {@link DynamicPropertySource @DynamicPropertySource} 注册的属性的优先级高于通过 {@code @TestPropertySource} 加载的属性。
 * <h3>默认属性文件检测</h3>
 * <p>如果将 {@code @TestPropertySource} 声明为<em>空</em>注解（即未为 {@link #locations} 或 {@link #properties} 指定显式值），将尝试相对于声明该注解的类检测<em>默认</em>属性文件。例如，如果带注解的测试类是 {@code com.example.MyTest}，则对应的默认属性文件是 {@code "classpath:com/example/MyTest.properties"}。如果无法检测到默认值，将抛出 {@link IllegalStateException}。
 * <h3>启用 &#064;TestPropertySource</h3>
 * <p>如果配置的{@linkplain ContextConfiguration#loader 上下文加载器}支持 {@code @TestPropertySource}，则会启用该注解。每个作为 {@link org.springframework.test.context.support.AbstractGenericContextLoader AbstractGenericContextLoader} 或 {@link org.springframework.test.context.web.AbstractGenericWebContextLoader AbstractGenericWebContextLoader} 子类的 {@code SmartContextLoader} 都自动支持 {@code @TestPropertySource}，这包括 Spring TestContext Framework 提供的每个 {@code SmartContextLoader}。
 * <h3>其他</h3>
 * <ul>
 * <li>通常，{@code @TestPropertySource} 会与 {@link ContextConfiguration @ContextConfiguration} 结合使用。</li>
 * <li>从 Spring Framework 5.2 开始，{@code @TestPropertySource} 可用作<em>{@linkplain Repeatable 可重复}</em>注解。</li>
 * <li>此注解可用作<em>元注解</em>以创建自定义<em>组合注解</em>；但是，如果将此注解与 {@code @ContextConfiguration} 组合在一个组合注解上，应格外小心，因为这两个注解的 {@code locations} 和 {@code inheritLocations} 属性可能会在属性解析过程中导致歧义。</li>
 * <li>从 Spring Framework 5.3 开始，此注解默认会从封闭测试类继承。有关详情，请参见 {@link NestedTestConfiguration @NestedTestConfiguration}。</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 4.1
 * @see ContextConfiguration
 * @see DynamicPropertySource
 * @see org.springframework.core.env.Environment
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.context.annotation.PropertySource
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Repeatable(TestPropertySources.class)
public @interface TestPropertySource {

	/**
	 * 用作 {@link #locations} 的别名。
	 * <p>此属性<strong>不得</strong>与 {@link #locations} 一起使用，但可以<em>代替</em> {@link #locations} 使用。
	 * @see #locations
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 要加载到 {@code Environment} 的 {@code PropertySources} 集合中的属性文件的资源位置。每个位置都将按声明顺序作为独立的属性源添加到所属的 {@code Environment} 中。
	 * <h3>支持的文件格式</h3>
	 * <p>同时支持传统和基于 XML 的属性文件格式——例如 {@code "classpath:/com/example/test.properties"} 或 {@code "file:/path/to/file.xml"}。
	 * <h3>路径资源语义</h3>
	 * <p>每个路径都将被解释为 Spring 的 {@link org.springframework.core.io.Resource Resource}。普通路径——例如 {@code "test.properties"}——将被视为相对于定义该测试类所在包的类路径资源。以斜杠开头的路径将被视为<em>绝对</em>类路径资源，例如：{@code "/org/example/test.xml"}。引用 URL 的路径（例如以 {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:}、{@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:}、{@code http:} 等为前缀的路径）将使用指定的资源协议加载。不允许使用资源位置通配符（例如 <code>*&#42;/*.properties</code>）：每个位置必须恰好对应一个 {@code .properties} 或 {@code .xml} 资源。路径中的属性占位符（即 <code>${...}</code>）将针对 {@code Environment} 进行{@linkplain org.springframework.core.env.Environment#resolveRequiredPlaceholders(String) 解析}。
	 * <h3>默认属性文件检测</h3>
	 * <p>有关默认值检测的讨论，请参见类级 Javadoc。
	 * <h3>优先级</h3>
	 * <p>从资源位置加载的属性优先级低于内联的 {@link #properties}。
	 * <p>此属性<strong>不得</strong>与 {@link #value} 一起使用，但可以<em>代替</em> {@link #value} 使用。
	 * @see #inheritLocations
	 * @see #value
	 * @see #properties
	 * @see org.springframework.core.env.PropertySource
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * 是否应<em>继承</em>超类和封闭类中的测试属性源 {@link #locations}。
	 * <p>默认值为 {@code true}，这意味着测试类将<em>继承</em>超类或封闭类定义的属性源位置。具体而言，测试类的属性源位置将追加到超类或封闭类定义的属性源位置列表末尾。因此，子类和嵌套类可以选择<em>扩展</em>测试属性源位置列表。
	 * <p>如果将 {@code inheritLocations} 设置为 {@code false}，则测试类的属性源位置将<em>遮蔽</em>并实际上替换超类或封闭类定义的任何属性源位置。
	 * <p>在以下示例中，{@code BaseTest} 的 {@code ApplicationContext} 将仅使用 {@code "base.properties"} 文件作为测试属性源加载。相比之下，{@code ExtendedTest} 的 {@code ApplicationContext} 将使用 {@code "base.properties"} <strong>和</strong> {@code "extended.properties"} 文件作为测试属性源位置加载。
	 * <pre class="code">
	 * &#064;TestPropertySource(&quot;base.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *     // ...
	 * }
	 *
	 * &#064;TestPropertySource(&quot;extended.properties&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }</pre>
	 * <p>如果将 {@code @TestPropertySource} 用作<em>{@linkplain Repeatable 可重复}</em>注解，则适用以下特殊规则。
	 * <ol>
	 * <li>测试类层次结构中给定层级的所有 {@code @TestPropertySource} 注解（即直接存在于或元存在于测试类上）被视为<em>本地</em>注解，这与从超类继承的 {@code @TestPropertySource} 注解不同。</li>
	 * <li>所有本地 {@code @TestPropertySource} 注解必须为 {@code inheritLocations} 标志声明相同的值。</li>
	 * <li>本地 {@code @TestPropertySource} 注解之间不考虑 {@code inheritLocations} 标志。具体而言，某个本地注解的属性源位置将追加到先前本地注解定义的属性源位置列表末尾。这允许本地注解扩展测试属性源位置列表，并可能覆盖个别属性。</li>
	 * </ol>
	 * @see #locations
	 */
	boolean inheritLocations() default true;

	/**
	 * 以<em>键值</em>对形式存在的<em>内联属性</em>，应在为测试加载 {@code ApplicationContext} 之前添加到 Spring 的 {@link org.springframework.core.env.Environment Environment} 中。所有键值对都将作为一个具有最高优先级的测试 {@code PropertySource} 添加到所属的 {@code Environment} 中。
	 * <h3>支持的语法</h3>
	 * <p>键值对支持的语法与 Java {@linkplain java.util.Properties#load(java.io.Reader) 属性文件}中条目所定义的语法相同：
	 * <ul>
	 * <li>{@code "key=value"}</li>
	 * <li>{@code "key:value"}</li>
	 * <li>{@code "key value"}</li>
	 * </ul>
	 * <h3>优先级</h3>
	 * <p>通过此属性声明的属性优先级高于从资源 {@link #locations} 加载的属性。
	 * <p>此属性可以与 {@link #value} <em>或</em> {@link #locations} 结合使用。
	 * @see #inheritProperties
	 * @see #locations
	 * @see org.springframework.core.env.PropertySource
	 */
	String[] properties() default {};

	/**
	 * 是否应<em>继承</em>超类和封闭类中的内联测试 {@link #properties}。
	 * <p>默认值为 {@code true}，这意味着测试类将<em>继承</em>超类或封闭类定义的内联属性。具体而言，测试类的内联属性将追加到超类或封闭类定义的内联属性列表末尾。因此，子类和嵌套类可以选择<em>扩展</em>内联测试属性列表。
	 * <p>如果将 {@code inheritProperties} 设置为 {@code false}，则测试类的内联属性将<em>遮蔽</em>并实际上替换超类或封闭类定义的任何内联属性。
	 * <p>在以下示例中，{@code BaseTest} 的 {@code ApplicationContext} 将仅使用内联的 {@code key1} 属性加载。相比之下，{@code ExtendedTest} 的 {@code ApplicationContext} 将使用内联的 {@code key1} <strong>和</strong> {@code key2} 属性加载。
	 * <pre class="code">
	 * &#064;TestPropertySource(properties = &quot;key1 = value1&quot;)
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *     // ...
	 * }
	 * &#064;TestPropertySource(properties = &quot;key2 = value2&quot;)
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *     // ...
	 * }</pre>
	 * <p>如果将 {@code @TestPropertySource} 用作<em>{@linkplain Repeatable 可重复}</em>注解，则适用以下特殊规则。
	 * <ol>
	 * <li>测试类层次结构中给定层级的所有 {@code @TestPropertySource} 注解（即直接存在于或元存在于测试类上）被视为<em>本地</em>注解，这与从超类或封闭类继承的 {@code @TestPropertySource} 注解不同。</li>
	 * <li>所有本地 {@code @TestPropertySource} 注解必须为 {@code inheritProperties} 标志声明相同的值。</li>
	 * <li>本地 {@code @TestPropertySource} 注解之间不考虑 {@code inheritProperties} 标志。具体而言，某个本地注解的内联属性将追加到先前本地注解定义的内联属性列表末尾。这允许本地注解扩展内联属性列表，并可能覆盖个别属性。</li>
	 * </ol>
	 * @see #properties
	 */
	boolean inheritProperties() default true;

}
