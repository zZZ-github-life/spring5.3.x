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

package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * 表示某个类声明了一个或多个 {@link Bean @Bean} 方法，并且可以由 Spring 容器处理，以便在运行时为这
 * 些 Bean 生成 Bean 定义并处理服务请求，例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate, configure and return bean ...
 *     }
 * }</pre>
 *
 * <h2>引导 {@code @Configuration} 类</h2>
 *
 * <h3>通过 {@code AnnotationConfigApplicationContext}</h3>
 *
 * <p>{@code @Configuration} 类通常使用 {@link AnnotationConfigApplicationContext} 或其支持 Web 的
 * 变体
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext}
 * 来引导。下面是一个使用前者的简单示例：
 *
 * <pre class="code">
 * AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
 * ctx.register(AppConfig.class);
 * ctx.refresh();
 * MyBean myBean = ctx.getBean(MyBean.class);
 * // use myBean ...
 * </pre>
 *
 * <p>更多详情请参见 {@link AnnotationConfigApplicationContext} 的 javadoc，Web 配置说明请参见
 * {@link org.springframework.web.context.support.AnnotationConfigWebApplicationContext AnnotationConfigWebApplicationContext}，
 * 适用于 {@code Servlet} 容器。
 *
 * <h3>通过 Spring {@code <beans>} XML</h3>
 *
 * <p>除了直接将 {@code @Configuration} 类注册到 {@code AnnotationConfigApplicationContext} 之外，
 * 也可以将 {@code @Configuration} 类声明为 Spring XML 文件中的普通 {@code <bean>} 定义：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *    &lt;context:annotation-config/&gt;
 *    &lt;bean class="com.acme.AppConfig"/&gt;
 * &lt;/beans&gt;
 * </pre>
 *
 * <p>在上面的示例中，需要 {@code <context:annotation-config/>} 才能启用
 * {@link ConfigurationClassPostProcessor} 以及其他与注解相关、便于处理 {@code @Configuration} 类的
 * 后处理器。
 *
 * <h3>通过组件扫描</h3>
 *
 * <p>{@code @Configuration} 以 {@link Component @Component} 作为元注解，因此
 * {@code @Configuration} 类是组件扫描的候选（通常使用 Spring XML 的
 * {@code <context:component-scan/>} 元素），并且因此也可以像任何常规 {@code @Component} 一样使用
 * {@link Autowired @Autowired}/{@link javax.inject.Inject @Inject}。特别地，如果只存在一个构造器，
 * 将对该构造器透明地应用自动装配语义：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     private final SomeBean someBean;
 *
 *     public AppConfig(SomeBean someBean) {
 *         this.someBean = someBean;
 *     }
 *
 *     // &#064;Bean definition using "SomeBean"
 *
 * }</pre>
 *
 * <p>{@code @Configuration} 类不仅可以通过组件扫描来引导，其自身还可以使用
 * {@link ComponentScan @ComponentScan} 注解来<em>配置</em>组件扫描：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.acme.app.services")
 * public class AppConfig {
 *     // various &#064;Bean definitions ...
 * }</pre>
 *
 * <p>详情请参见 {@link ComponentScan @ComponentScan} 的 javadoc。
 *
 * <h2>使用外部化的值</h2>
 *
 * <h3>使用 {@code Environment} API</h3>
 *
 * <p>可以通过将 Spring 的 {@link org.springframework.core.env.Environment} 注入到
 * {@code @Configuration} 类中来查找外部化的值 &mdash; 例如，使用 {@code @Autowired} 注解：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Autowired Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         MyBean myBean = new MyBean();
 *         myBean.setName(env.getProperty("bean.name"));
 *         return myBean;
 *     }
 * }</pre>
 *
 * <p>通过 {@code Environment} 解析的属性位于一个或多个“属性源”对象中，{@code @Configuration} 类可
 * 以使用 {@link PropertySource @PropertySource} 注解向 {@code Environment} 对象提供属性源：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Inject Environment env;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(env.getProperty("bean.name"));
 *     }
 * }</pre>
 *
 * <p>更多详情请参见 {@link org.springframework.core.env.Environment Environment} 和
 * {@link PropertySource @PropertySource} 的 javadoc。
 *
 * <h3>使用 {@code @Value} 注解</h3>
 *
 * <p>可以使用 {@link Value @Value} 注解将外部化的值注入到 {@code @Configuration} 类中：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;PropertySource("classpath:/com/acme/app.properties")
 * public class AppConfig {
 *
 *     &#064;Value("${bean.name}") String beanName;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(beanName);
 *     }
 * }</pre>
 *
 * <p>这种方式通常与 Spring 的
 * {@link org.springframework.context.support.PropertySourcesPlaceholderConfigurer PropertySourcesPlaceholderConfigurer}
 * 一起使用，后者可以在 XML 配置中通过 {@code <context:property-placeholder/>} <em>自动</em>启用，
 * 或者在 {@code @Configuration} 类中通过专用的 {@code static} {@code @Bean} 方法<em>显式</em>启用
 * （详情参见 {@link Bean @Bean} javadoc 中“关于返回 BeanFactoryPostProcessor 的 {@code @Bean} 方法
 * 的说明”）。但请注意，通常只有在你需要自定义配置（例如占位符语法等）时，才需要通过 {@code static}
 * {@code @Bean} 方法显式注册 {@code PropertySourcesPlaceholderConfigurer}。具体而言，如果没有 Bean
 * 后处理器（例如 {@code PropertySourcesPlaceholderConfigurer}）为 {@code ApplicationContext} 注册
 * <em>内嵌值解析器</em>，Spring 将注册一个默认的<em>内嵌值解析器</em>，它会针对注册在
 * {@code Environment} 中的属性源解析占位符。关于使用 {@code @ImportResource} 将
 * {@code @Configuration} 类与 Spring XML 组合的内容，请参见下文；关于 {@link Value @Value} 的详情，
 * 请参见其 javadoc；关于使用 {@code BeanFactoryPostProcessor} 类型（例如
 * {@code PropertySourcesPlaceholderConfigurer}）的详情，请参见 {@link Bean @Bean} 的 javadoc。
 *
 * <h2>组合 {@code @Configuration} 类</h2>
 *
 * <h3>使用 {@code @Import} 注解</h3>
 *
 * <p>{@code @Configuration} 类可以使用 {@link Import @Import} 注解来组合，这与 {@code <import>} 在
 * Spring XML 中的工作方式类似。由于 {@code @Configuration} 对象在容器中作为 Spring Bean 管理，被导
 * 入的配置可以被注入 &mdash; 例如，通过构造器注入：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class DatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return DataSource
 *     }
 * }
 *
 * &#064;Configuration
 * &#064;Import(DatabaseConfig.class)
 * public class AppConfig {
 *
 *     private final DatabaseConfig dataConfig;
 *
 *     public AppConfig(DatabaseConfig dataConfig) {
 *         this.dataConfig = dataConfig;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // reference the dataSource() bean method
 *         return new MyBean(dataConfig.dataSource());
 *     }
 * }</pre>
 *
 * <p>现在只需将 {@code AppConfig} 注册到 Spring 上下文中，就可以同时引导 {@code AppConfig} 和被导
 * 入的 {@code DatabaseConfig}：
 *
 * <pre class="code">
 * new AnnotationConfigApplicationContext(AppConfig.class);</pre>
 *
 * <h3>使用 {@code @Profile} 注解</h3>
 *
 * <p>{@code @Configuration} 类可以标注 {@link Profile @Profile} 注解，以表明只有在给定的一个或多个
 * profile <em>处于活动状态</em>时才应处理它们：
 *
 * <pre class="code">
 * &#064;Profile("development")
 * &#064;Configuration
 * public class EmbeddedDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return embedded DataSource
 *     }
 * }
 *
 * &#064;Profile("production")
 * &#064;Configuration
 * public class ProductionDatabaseConfig {
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // instantiate, configure and return production DataSource
 *     }
 * }</pre>
 *
 * <p>或者，你也可以在 {@code @Bean} 方法级别声明 profile 条件 &mdash; 例如，用于同一配置类中的替代
 * Bean 变体：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class ProfileDatabaseConfig {
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("development")
 *     public DataSource embeddedDatabase() { ... }
 *
 *     &#064;Bean("dataSource")
 *     &#064;Profile("production")
 *     public DataSource productionDatabase() { ... }
 * }</pre>
 *
 * <p>更多详情请参见 {@link Profile @Profile} 和 {@link org.springframework.core.env.Environment}
 * 的 javadoc。
 *
 * <h3>通过 {@code @ImportResource} 注解使用 Spring XML</h3>
 *
 * <p>如上所述，{@code @Configuration} 类可以声明为 Spring XML 文件中的常规 Spring {@code <bean>}
 * 定义。也可以使用 {@link ImportResource @ImportResource} 注解将 Spring XML 配置文件导入
 * {@code @Configuration} 类。从 XML 导入的 Bean 定义可以被注入 &mdash; 例如，使用 {@code @Inject}
 * 注解：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ImportResource("classpath:/com/acme/database-config.xml")
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource; // from XML
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // inject the XML-defined dataSource bean
 *         return new MyBean(this.dataSource);
 *     }
 * }</pre>
 *
 * <h3>使用嵌套的 {@code @Configuration} 类</h3>
 *
 * <p>{@code @Configuration} 类可以如下所示相互嵌套：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Inject DataSource dataSource;
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean(dataSource);
 *     }
 *
 *     &#064;Configuration
 *     static class DatabaseConfig {
 *         &#064;Bean
 *         DataSource dataSource() {
 *             return new EmbeddedDatabaseBuilder().build();
 *         }
 *     }
 * }</pre>
 *
 * <p>引导这样的结构时，只需将 {@code AppConfig} 注册到应用上下文。由于是嵌套的
 * {@code @Configuration} 类，{@code DatabaseConfig} <em>将被自动注册</em>。当 {@code AppConfig} 与
 * {@code DatabaseConfig} 之间的关系已经隐含明确时，这样就无需使用 {@code @Import} 注解。
 *
 * <p>另请注意，嵌套的 {@code @Configuration} 类可以与 {@code @Profile} 注解配合使用，以向包含它的
 * {@code @Configuration} 类提供同一 Bean 的两种选择。
 *
 * <h2>配置延迟初始化</h2>
 *
 * <p>默认情况下，{@code @Bean} 方法会在容器引导时被<em>急切实例化</em>。为避免这一点，可以将
 * {@code @Configuration} 与 {@link Lazy @Lazy} 注解一起使用，以表明该类中声明的所有 {@code @Bean}
 * 方法默认都延迟初始化。请注意，{@code @Lazy} 也可以用在单个 {@code @Bean} 方法上。
 *
 * <h2>对 {@code @Configuration} 类的测试支持</h2>
 *
 * <p>{@code spring-test} 模块中提供的 Spring <em>TestContext 框架</em>提供了
 * {@code @ContextConfiguration} 注解，它可以接受一组<em>组件类</em>引用 &mdash; 通常是
 * {@code @Configuration} 或 {@code @Component} 类。
 *
 * <pre class="code">
 * &#064;RunWith(SpringRunner.class)
 * &#064;ContextConfiguration(classes = {AppConfig.class, DatabaseConfig.class})
 * public class MyTests {
 *
 *     &#064;Autowired MyBean myBean;
 *
 *     &#064;Autowired DataSource dataSource;
 *
 *     &#064;Test
 *     public void test() {
 *         // assertions against myBean ...
 *     }
 * }</pre>
 *
 * <p>详情请参见
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/testing.html#testcontext-framework">
 * TestContext framework</a> 参考文档。
 *
 * <h2>使用 {@code @Enable} 注解启用内置的 Spring 特性</h2>
 *
 * <p>诸如异步方法执行、计划任务执行、注解驱动的事务管理甚至 Spring MVC 等 Spring 特性，都可以在
 * {@code @Configuration} 类中使用各自对应的“{@code @Enable}”注解来启用和配置。详情请参见
 * {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync}、
 * {@link org.springframework.scheduling.annotation.EnableScheduling @EnableScheduling}、
 * {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement}、
 * {@link org.springframework.context.annotation.EnableAspectJAutoProxy @EnableAspectJAutoProxy}
 * 以及 {@link org.springframework.web.servlet.config.annotation.EnableWebMvc @EnableWebMvc}。
 *
 * <h2>编写 {@code @Configuration} 类时的约束</h2>
 *
 * <ul>
 * <li>配置类必须以类的方式提供（即不能作为工厂方法返回的实例），以便通过生成子类在运行时进行增强。
 * <li>配置类必须是非 final 的（允许在运行时创建子类），除非 {@link #proxyBeanMethods() proxyBeanMethods} 标志设置为 {@code false}，此时不需要运行时生成的子类。
 * <li>配置类必须是非局部的（即不能在方法内部声明）。
 * <li>任何嵌套配置类都必须声明为 {@code static}。
 * <li>{@code @Bean} 方法不能反过来再创建配置类（任何此类实例都将被视为普通 Bean，其配置注解不会被检测到）。
 * </ul>
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see Bean
 * @see Profile
 * @see Import
 * @see ImportResource
 * @see ComponentScan
 * @see Lazy
 * @see PropertySource
 * @see AnnotationConfigApplicationContext
 * @see ConfigurationClassPostProcessor
 * @see org.springframework.core.env.Environment
 * @see org.springframework.test.context.ContextConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {

	/**
	 * 显式指定与 {@code @Configuration} 类关联的 Spring Bean 定义的名称。如果未指定（常见情况），将自
	 * 动生成 Bean 名称。
	 * <p>仅当 {@code @Configuration} 类通过组件扫描被发现或直接提供给
	 * {@link AnnotationConfigApplicationContext} 时，自定义名称才适用。如果 {@code @Configuration} 类
	 * 注册为传统的 XML Bean 定义，则以 Bean 元素的 name/id 为准。
	 * @return the explicit component name, if any (or empty String otherwise)
	 * @see AnnotationBeanNameGenerator
	 */
	@AliasFor(annotation = Component.class)
	String value() default "";

	/**
	 * 指定 {@code @Bean} 方法是否应被代理，以强制执行 Bean 生命周期行为，例如即使用户代码中直接调用 {@code @Bean} 方法，也返回共享的单例 Bean 实例。此特性需要方法拦截，通过运行时生成的 CGLIB 子类实现，这带来了一些限制，例如配置类及其方法不能声明为 {@code final}。
	 * <p>默认值为 {@code true}，允许在配置类内部通过直接方法调用进行“Bean 间引用”，也允许对此配置的 {@code @Bean} 方法进行外部调用（例如来自另一个配置类）。如果不需要这些行为，因为此配置的每个 {@code @Bean} 方法都是自包含的、设计为供容器使用的普通工厂方法，则将此标志切换为 {@code false}，以避免 CGLIB 子类处理。
	 * <p>关闭 Bean 方法拦截后，{@code @Bean} 方法会像声明在非 {@code @Configuration} 类上时一样被单独处理，也就是“@Bean Lite 模式”（参见 {@link Bean @Bean 的 javadoc}）。因此，其在行为上等同于移除 {@code @Configuration} 构造型。


	 * proxyBeanMethods 配置类是用来指定 @Bean 注解标注的方法是否使用代理，默认是 true 使用代理，直接从 IOC 容器之中取得对象；
	 * 如果设置为 false, 也就是不使用注解，每次调用 @Bean 标注的方法获取到的对象和 IOC 容器中的都不一样，是一个新的对象，
	 * 所以我们可以将此属性设置为 false 来提高性能；


	 * proxyBeanMethods参数设置为true时即为：Full 全模式。使用代理，
	 * proxyBeanMethods参数设置为false时即为：Lite 轻量级模式 。不使用代理

	 * @since 5.2
	 */
	boolean proxyBeanMethods() default true;

}
