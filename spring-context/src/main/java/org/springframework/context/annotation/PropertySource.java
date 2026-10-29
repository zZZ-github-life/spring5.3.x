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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.io.support.PropertySourceFactory;

/**
 * 用于向 Spring 的 {@link org.springframework.core.env.Environment Environment} 添加
 * {@link org.springframework.core.env.PropertySource PropertySource} 的便捷声明式注解。需与
 * {@link Configuration @Configuration} 类配合使用。
 *
 * <h3>示例用法</h3>
 *
 * <p>假设有一个文件 {@code app.properties}，其中包含键值对 {@code testbean.name=myTestBean}，则下
 * 面的 {@code @Configuration} 类使用 {@code @PropertySource} 将 {@code app.properties} 贡献到
 * {@code Environment} 的 {@code PropertySources} 集合中。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>请注意，{@code Environment} 对象通过
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} 注入到配置类中，随后在
 * 填充 {@code TestBean} 对象时使用。给定上述配置，调用 {@code testBean.getName()} 将返回
 * "myTestBean"。
 *
 * <h3>解析 {@code <bean>} 定义和 {@code @Value} 注解中的 <code>${...}</code> 占位符</h3>
 *
 * <p>要使用来自某个 {@code PropertySource} 的属性解析 {@code <bean>} 定义或 {@code @Value} 注解中
 * 的 ${...} 占位符，必须确保在 {@code ApplicationContext} 所使用的 {@code BeanFactory} 中注册了适
 * 当的<em>内嵌值解析器</em>。在 XML 中使用 {@code <context:property-placeholder>} 时会自动完成此操
 * 作。使用 {@code @Configuration} 类时，可以通过 {@code static} 的 {@code @Bean} 方法显式注册
 * {@code PropertySourcesPlaceholderConfigurer} 来实现。但请注意，通常只有在需要自定义占位符语法等
 * 配置时，才需要通过 {@code static} 的 {@code @Bean} 方法显式注册
 * {@code PropertySourcesPlaceholderConfigurer}。有关详细信息和示例，请参阅
 * {@link Configuration @Configuration} 的 javadoc 中“使用外部化值”一节，以及 {@link Bean @Bean} 的
 * javadoc 中“关于返回 BeanFactoryPostProcessor 的 {@code @Bean} 方法的说明”一节。
 *
 * <h3>解析 {@code @PropertySource} 资源位置中的 ${...} 占位符</h3>
 *
 * <p>{@code @PropertySource} 的 {@linkplain #value() 资源位置}中出现的任何 ${...} 占位符都将针对已
 * 注册到环境中的属性源集合进行解析。例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/${my.placeholder:default/path}/app.properties")
 * public class AppConfig {
 *
 *     &#064;Autowired
 *     Environment env;
 *
 *     &#064;Bean
 *     public TestBean testBean() {
 *         TestBean testBean = new TestBean();
 *         testBean.setName(env.getProperty("testbean.name"));
 *         return testBean;
 *     }
 * }</pre>
 *
 * <p>假设 "my.placeholder" 已存在于某个先前注册的属性源中 &mdash; 例如系统属性或环境变量 &mdash;
 * 则该占位符将解析为对应的值。如果不存在，则使用 "default/path" 作为默认值。默认值（以冒号 ":" 分
 * 隔）是可选的。如果未指定默认值且属性无法解析，将抛出 {@code IllegalArgumentException}。
 *
 * <h3>关于使用 {@code @PropertySource} 覆盖属性的说明</h3>
 *
 * <p>如果某个属性键存在于多个 {@code .properties} 文件中，则最后处理的 {@code @PropertySource} 注
 * 解将“胜出”，并覆盖此前任何同名的键。
 *
 * <p>例如，给定两个属性文件 {@code a.properties} 和 {@code b.properties}，考虑下面两个通过
 * {@code @PropertySource} 注解引用它们的配置类：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/a.properties")
 * public class ConfigA { }
 *
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/myco/b.properties")
 * public class ConfigB { }
 * </pre>
 *
 * <p>覆盖顺序取决于这些类注册到应用上下文的顺序。
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(ConfigA.class);
 * ctx.register(ConfigB.class);
 * ctx.refresh();
 * </pre>
 *
 * <p>在上面的场景中，{@code b.properties} 中的属性将覆盖 {@code a.properties} 中存在的任何重复项，
 * 因为 {@code ConfigB} 是最后注册的。
 *
 * <p>在某些情况下，使用 {@code @PropertySource} 注解时可能无法或不便严格控制属性源的顺序。例如，如
 * 果上面的 {@code @Configuration} 类是通过组件扫描注册的，则顺序难以预测。在这种情况下 &mdash; 并
 * 且如果覆盖很重要 &mdash; 建议用户退回到使用编程式的 {@code PropertySource} API。有关详细信息，请
 * 参阅 {@link org.springframework.core.env.ConfigurableEnvironment ConfigurableEnvironment} 和
 * {@link org.springframework.core.env.MutablePropertySources MutablePropertySources} 的 javadoc。
 *
 * <p><b>注意：按照 Java 8 约定，此注解是可重复的。</b>但是，所有此类 {@code @PropertySource} 注解
 * 都需要在同一层级声明：要么直接声明在配置类上，要么作为元注解声明在同一个自定义注解上。不建议混用
 * 直接注解和元注解，因为直接注解实际上会覆盖元注解。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.1
 * @see PropertySources
 * @see Configuration
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.core.env.ConfigurableEnvironment#getPropertySources()
 * @see org.springframework.core.env.MutablePropertySources
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(PropertySources.class)
public @interface PropertySource {

	/**
	 * 指定此属性源的名称。如果省略，{@link #factory} 将根据底层资源生成名称（对于
	 * {@link org.springframework.core.io.support.DefaultPropertySourceFactory}：通过相应的无名称
	 * {@link org.springframework.core.io.support.ResourcePropertySource} 构造函数从资源描述中派生）。
	 * @see org.springframework.core.env.PropertySource#getName()
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	String name() default "";

	/**
	 * 指定要加载的属性文件的一个或多个资源位置。
	 *
	 * <p>同时支持传统格式和基于 XML 的属性文件格式 &mdash; 例如
	 * {@code "classpath:/com/myco/app.properties"} 或 {@code "file:/path/to/file.xml"}。
	 *
	 * <p>不允许使用资源位置通配符（例如 *&#42;/*.properties）；每个位置必须恰好解析为一个
	 * {@code .properties} 或 {@code .xml} 资源。
	 *
	 * <p>${...} 占位符将针对已注册到 {@code Environment} 中的所有属性源进行解析。示例请参见
	 * {@linkplain PropertySource above}。
	 *
	 * <p>每个位置都将作为其自身的属性源添加到所在的 {@code Environment} 中，顺序与声明顺序一致。
	 */
	String[] value();

	/**
	 * 指示是否应忽略找不到 {@link #value property resource} 的情况。
	 *
	 * <p>如果属性文件完全是可选的，则 {@code true} 是合适的选择。
	 *
	 * <p>默认值为 {@code false}。
	 * @since 4.0
	 */
	boolean ignoreResourceNotFound() default false;

	/**
	 * 指定给定资源的具体字符编码，例如 "UTF-8"。
	 * @since 4.3
	 */
	String encoding() default "";

	/**
	 * 如有需要，指定一个自定义的 {@link PropertySourceFactory}。
	 *
	 * <p>默认情况下，将使用针对标准资源文件的默认工厂。
	 * @since 4.3
	 * @see org.springframework.core.io.support.DefaultPropertySourceFactory
	 * @see org.springframework.core.io.support.ResourcePropertySource
	 */
	Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}
