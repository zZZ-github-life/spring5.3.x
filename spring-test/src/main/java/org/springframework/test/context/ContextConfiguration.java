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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AliasFor;

/**
 * {@code @ContextConfiguration} 定义类级元数据，用于确定
 * 如何为集成测试加载和配置 {@link org.springframework.context.ApplicationContext
 * ApplicationContext}。
 *
 * <h3>支持的资源类型</h3>
 *
 * <p>在 Spring 3.1 之前，仅支持基于路径的资源位置（通常是 XML 配置
 * 文件）。从 Spring 3.1 开始，{@linkplain #loader 上下文加载器}可以
 * 选择支持<em>基于路径</em>或<em>基于类</em>的资源。从
 * Spring 4.0.4 开始，{@linkplain #loader 上下文加载器}可以选择同时支持基于路径
 * <em>和</em>基于类的资源。因此，
 * {@code @ContextConfiguration} 可用于声明基于路径的资源
 * 位置（通过 {@link #locations} 或 {@link #value} 属性）<em>或</em>
 * 组件类（通过 {@link #classes} 属性）。但请注意，大多数
 * {@link SmartContextLoader} 实现仅支持单一资源类型。从
 * Spring 4.1 开始，基于路径的资源位置可以是 XML 配置文件或
 * Groovy 脚本（如果类路径上存在 Groovy）。当然，第三方框架可以
 * 选择支持其他类型的基于路径的资源。
 *
 * <h3>组件类</h3>
 *
 * <p>术语<em>组件类</em>可以指以下任意一种。
 *
 * <ul>
 * <li>使用 {@link org.springframework.context.annotation.Configuration @Configuration} 注解的类</li>
 * <li>组件（即使用
 * {@link org.springframework.stereotype.Component @Component}、
 * {@link org.springframework.stereotype.Service @Service}、
 * {@link org.springframework.stereotype.Repository @Repository} 等注解的类）</li>
 * <li>使用 {@code javax.inject} 注解的符合 JSR-330 的类</li>
 * <li>包含 {@link org.springframework.context.annotation.Bean @Bean} 方法的任何类</li>
 * <li>任何其他打算注册为 Spring 组件（即 {@code ApplicationContext} 中的 Spring Bean）
 * 的类，可能利用单个构造函数的自动自动装配而无需使用 Spring 注解</li>
 * </ul>
 *
 * 将为每个组件类在 {@code ApplicationContext} 中注册一个 Bean，
 * 因此这些 Bean 可以注入到其他 Bean 或
 * 测试类实例中。
 *
 * <p>有关<em>组件类</em>的配置和语义的更多
 * 信息，请参阅 {@link org.springframework.context.annotation.Configuration @Configuration}
 * 和 {@link org.springframework.context.annotation.Bean @Bean} 的 Javadoc。
 *
 * <p>此注解可用作<em>元注解</em>以创建自定义的
 * <em>组合注解</em>。
 *
 * <p>从 Spring Framework 5.3 开始，此注解默认将从
 * 外层测试类继承。详情请参见
 * {@link NestedTestConfiguration @NestedTestConfiguration}。
 *
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.test.context.junit.jupiter.SpringJUnitConfig @SpringJUnitConfig
 * @see org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig @SpringJUnitWebConfig
 * @see ContextHierarchy @ContextHierarchy
 * @see ActiveProfiles @ActiveProfiles
 * @see TestPropertySource @TestPropertySource
 * @see SmartContextLoader
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 * @see org.springframework.context.ApplicationContext ApplicationContext
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ContextConfiguration {

	/**
	 * {@link #locations} 的别名。
	 * <p>此属性<strong>不得</strong>与
	 * {@link #locations} 一起使用，但可以代替 {@link #locations} 使用。
	 * @since 3.0
	 * @see #inheritLocations
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * 用于加载
	 * {@link org.springframework.context.ApplicationContext ApplicationContext} 的资源位置。
	 * <p>请查看
	 * {@link org.springframework.test.context.support.AbstractContextLoader#modifyLocations
	 * AbstractContextLoader.modifyLocations()} 的 Javadoc，了解位置
	 * 在运行时的解释方式，特别是相对
	 * 路径的情况。另外，请查看
	 * {@link org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations
	 * AbstractContextLoader.generateDefaultLocations()} 的文档，了解在未指定任何位置时
	 * 将使用的默认位置。
	 * <p>请注意，上述默认规则仅适用于标准的
	 * {@link org.springframework.test.context.support.AbstractContextLoader
	 * AbstractContextLoader} 子类，例如
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader} 或
	 * {@link org.springframework.test.context.support.GenericGroovyXmlContextLoader GenericGroovyXmlContextLoader}，
	 * 如果配置了
	 * {@code locations}，它们就是运行时实际使用的默认实现。有关默认加载器的
	 * 更多详细信息，请参见 {@link #loader} 的文档。
	 * <p>此属性<strong>不得</strong>与
	 * {@link #value} 一起使用，但可以代替 {@link #value} 使用。
	 * @since 2.5
	 * @see #inheritLocations
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * 用于加载
	 * {@link org.springframework.context.ApplicationContext ApplicationContext} 的<em>组件类</em>。
	 * <p>请查看
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader#detectDefaultConfigurationClasses
	 * AnnotationConfigContextLoader.detectDefaultConfigurationClasses()} 的 Javadoc，了解
	 * 未指定<em>组件类</em>时将如何检测默认配置类。有关默认加载器的
	 * 更多详细信息，请参见 {@link #loader} 的文档。
	 * @since 3.1
	 * @see org.springframework.context.annotation.Configuration
	 * @see org.springframework.test.context.support.AnnotationConfigContextLoader
	 * @see #inheritLocations
	 */
	Class<?>[] classes() default {};

	/**
	 * 用于初始化
	 * {@link ConfigurableApplicationContext} 的应用程序上下文<em>初始化器类</em>。
	 * <p>每个声明的初始化器所支持的具体 {@code ConfigurableApplicationContext} 类型
	 * 必须与所用的 {@link SmartContextLoader} 创建的 {@code ApplicationContext}
	 * 类型兼容。
	 * <p>{@code SmartContextLoader} 实现通常会检测是否
	 * 实现了 Spring 的 {@link org.springframework.core.Ordered Ordered} 接口，或是否存在 @{@link org.springframework.core.annotation.Order Order}
	 * 注解，并在调用它们之前相应地排序实例。
	 * @since 3.2
	 * @see org.springframework.context.ApplicationContextInitializer
	 * @see org.springframework.context.ConfigurableApplicationContext
	 * @see #inheritInitializers
	 * @see #loader
	 */
	Class<? extends ApplicationContextInitializer<?>>[] initializers() default {};

	/**
	 * 是否应<em>继承</em>测试超类和
	 * 封闭类中的{@linkplain #locations 资源位置}或
	 * {@linkplain #classes <em>组件类</em>}。
	 * <p>默认值为 {@code true}。这意味着带注解的测试
	 * 类将<em>继承</em>测试超类和封闭类定义的
	 * 资源位置或组件类。具体而言，
	 * 给定测试类的资源位置或组件类将
	 * 追加到测试超类和封闭类定义的资源位置或组件类
	 * 列表中。因此，子类和嵌套
	 * 类可以选择<em>扩展</em>该资源位置或
	 * 组件类列表。
	 * <p>如果 {@code inheritLocations} 设置为 {@code false}，
	 * 带注解测试类的资源位置或组件类
	 * 将<em>遮蔽</em>并实际替换
	 * 超类和封闭类定义的任何资源位置
	 * 或组件类。
	 * <p>在下面使用基于路径资源位置的示例中，
	 * {@code ExtendedTest} 的
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * 将按顺序从
	 * {@code "base-context.xml"} <strong>和</strong>
	 * {@code "extended-context.xml"} 加载。因此，{@code "extended-context.xml"} 中定义的
	 * Bean 可以覆盖
	 * {@code "base-context.xml"} 中定义的 Bean。
	 * <pre class="code">
	 * &#064;ContextConfiguration("base-context.xml")
	 * public class BaseTest {
	 * // ...
	 * }
	 *
	 * &#064;ContextConfiguration("extended-context.xml")
	 * public class ExtendedTest extends BaseTest {
	 * // ...
	 * }
	 * </pre>
	 * <p>同样，在下面使用组件类的示例中，
	 * {@code ExtendedTest} 的
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * 将按顺序从
	 * {@code BaseConfig} <strong>和</strong> {@code ExtendedConfig}
	 * 配置类加载。因此，
	 * {@code ExtendedConfig} 中定义的 Bean 可以覆盖
	 * {@code BaseConfig} 中定义的 Bean。
	 * <pre class="code">
	 * &#064;ContextConfiguration(classes=BaseConfig.class)
	 * public class BaseTest {
	 * // ...
	 * }
	 *
	 * &#064;ContextConfiguration(classes=ExtendedConfig.class)
	 * public class ExtendedTest extends BaseTest {
	 * // ...
	 * }
	 * </pre>
	 * @since 2.5
	 */
	boolean inheritLocations() default true;

	/**
	 * 是否应<em>继承</em>测试
	 * 超类和封闭类中的{@linkplain #initializers 上下文初始化器}。
	 * <p>默认值为 {@code true}。这意味着带注解的测试
	 * 类将<em>继承</em>测试超类和封闭类定义的
	 * 应用程序上下文初始化器。具体而言，给定测试类的初始化器
	 * 将添加到测试超类和封闭类定义的初始化器
	 * 集合中。因此，子类和嵌套类
	 * 可以选择<em>扩展</em>该初始化器集合。
	 * <p>如果 {@code inheritInitializers} 设置为 {@code false}，
	 * 带注解测试类的初始化器
	 * 将<em>遮蔽</em>并实际替换
	 * 超类和封闭类定义的任何初始化器。
	 * <p>在下面的示例中，
	 * {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * 将使用
	 * {@code BaseInitializer} <strong>和</strong> {@code ExtendedInitializer} 为 {@code ExtendedTest} 初始化。
	 * 但请注意，初始化器的调用顺序
	 * 取决于它们是实现了 {@link org.springframework.core.Ordered
	 * Ordered} 还是使用 {@link org.springframework.core.annotation.Order
	 * &#064;Order} 注解。
	 * <pre class="code">
	 * &#064;ContextConfiguration(initializers = BaseInitializer.class)
	 * public class BaseTest {
	 * // ...
	 * }
	 *
	 * &#064;ContextConfiguration(initializers = ExtendedInitializer.class)
	 * public class ExtendedTest extends BaseTest {
	 * // ...
	 * }
	 * </pre>
	 * @since 3.2
	 */
	boolean inheritInitializers() default true;

	/**
	 * 用于加载 {@link org.springframework.context.ApplicationContext
	 * ApplicationContext} 的 {@link SmartContextLoader}（或 {@link ContextLoader}）类型。
	 * <p>如果未指定，加载器将从第一个使用
	 * {@code @ContextConfiguration} 注解或元注解并指定了显式加载器的
	 * 超类或封闭类继承。如果类型层次结构或封闭类层次结构中
	 * 没有类指定显式加载器，则改用默认加载器。
	 * <p>运行时选择的具体默认实现将是
	 * {@link org.springframework.test.context.support.DelegatingSmartContextLoader
	 * DelegatingSmartContextLoader} 或
	 * {@link org.springframework.test.context.web.WebDelegatingSmartContextLoader
	 * WebDelegatingSmartContextLoader}，取决于是否存在
	 * {@link org.springframework.test.context.web.WebAppConfiguration
	 * &#064;WebAppConfiguration}。有关各种具体 {@code SmartContextLoader} 的默认行为
	 * 的更多详细信息，请查看
	 * {@link org.springframework.test.context.support.AbstractContextLoader AbstractContextLoader}、
	 * {@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader}、
	 * {@link org.springframework.test.context.support.GenericGroovyXmlContextLoader GenericGroovyXmlContextLoader}、
	 * {@link org.springframework.test.context.support.AnnotationConfigContextLoader AnnotationConfigContextLoader}、
	 * {@link org.springframework.test.context.web.GenericXmlWebContextLoader GenericXmlWebContextLoader}、
	 * {@link org.springframework.test.context.web.GenericGroovyXmlWebContextLoader GenericGroovyXmlWebContextLoader} 和
	 * {@link org.springframework.test.context.web.AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader} 的 Javadoc。
	 * @since 2.5
	 */
	Class<? extends ContextLoader> loader() default ContextLoader.class;

	/**
	 * 此配置所表示的上下文层次结构级别的名称。
	 * <p>如果未指定，将根据层次结构中
	 * 所有已声明上下文中的数字级别推断名称。
	 * <p>此属性仅适用于在使用
	 * {@code @ContextHierarchy} 配置的测试类层次结构或封闭类层次结构中使用的情况，
	 * 此时该名称可用于将本配置与超类或封闭类中
	 * 定义的层次结构级别中的同名配置进行<em>合并</em>或<em>覆盖</em>。
	 * 详情请参见 {@link ContextHierarchy @ContextHierarchy} 的 Javadoc。
	 * @since 3.2.2
	 */
	String name() default "";

}
