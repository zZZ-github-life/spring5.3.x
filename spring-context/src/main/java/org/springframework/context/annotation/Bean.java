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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.core.annotation.AliasFor;

/**
 * 指示某个方法会生成一个由 Spring 容器管理的 Bean。
 *
 * <h3>概述</h3>
 *
 * <p>此注解各属性的名称和语义有意与 Spring XML schema 中 {@code <bean/>} 元素的属性保持相似。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Bean 名称</h3>
 *
 * <p>虽然提供了 {@link #name} 属性，但确定 Bean 名称的默认策略是使用 {@code @Bean} 方法的名称。这
 * 种方式方便且直观，但如果需要显式命名，可以使用 {@code name} 属性（或其别名 {@code value}）。另请
 * 注意，{@code name} 接受字符串数组，从而允许为单个 Bean 指定多个名称（即一个主 Bean 名称加上一个
 * 或多个别名）。
 *
 * <pre class="code">
 *     &#064;Bean({"b1", "b2"}) // bean available as 'b1' and 'b2', but not 'myBean'
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * <h3>Profile、Scope、Lazy、DependsOn、Primary、Order</h3>
 *
 * <p>请注意，{@code @Bean} 注解并不提供用于 profile、scope、lazy、depends-on 或 primary 的属性。相
 * 反，它应与 {@link Scope @Scope}、{@link Lazy @Lazy}、{@link DependsOn @DependsOn} 和
 * {@link Primary @Primary} 注解结合使用来声明这些语义。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     &#064;Profile("production")
 *     &#064;Scope("prototype")
 *     public MyBean myBean() {
 *         // instantiate and configure MyBean obj
 *         return obj;
 *     }
 * </pre>
 *
 * 上述注解的语义与它们在组件类层级的使用方式一致：{@code @Profile} 允许有选择地包含某些 Bean。
 * {@code @Scope} 将 Bean 的作用域从 singleton 更改为指定的作用域。{@code @Lazy} 仅在默认的
 * singleton 作用域下才实际生效。{@code @DependsOn} 除了 Bean 通过直接引用表达的依赖之外，还会强制
 * 在创建此 Bean 之前创建特定的其他 Bean，这通常对 singleton 的启动很有帮助。{@code @Primary} 是一
 * 种在注入点层级解决歧义的机制，适用于需要注入单个目标组件但有多个 Bean 按类型匹配的情况。
 *
 * <p>此外，{@code @Bean} 方法还可以声明限定符注解以及
 * {@link org.springframework.core.annotation.Order @Order} 值，以便在注入点解析时被考虑，这一点与
 * 相应组件类上的对应注解相同，但可能因 Bean 定义而异（在具有相同 Bean 类的多个定义的情况下）。限定
 * 符在初始类型匹配之后缩小候选范围；order 值决定集合注入点中已解析元素的顺序（有多个目标 Bean 按类
 * 型和限定符匹配时）。
 *
 * <p><b>注意：</b>{@code @Order} 值可能会影响注入点的优先级，但请注意它们不会影响 singleton 的启动
 * 顺序，后者是一个正交的关注点，由依赖关系和上文提到的 {@code @DependsOn} 声明决定。此外，
 * {@link javax.annotation.Priority} 在此层级不可用，因为它不能声明在方法上；其语义可以通过
 * {@code @Order} 值结合每个类型上单个 Bean 的 {@code @Primary} 来建模。
 *
 * <h3>{@code @Configuration} 类中的 {@code @Bean} 方法</h3>
 *
 * <p>通常，{@code @Bean} 方法声明在 {@code @Configuration} 类中。在这种情况下，Bean 方法可以通过
 * <i>直接</i>调用同一类中的其他 {@code @Bean} 方法来引用它们。这确保了 Bean 之间的引用是强类型且可
 * 导航的。这些所谓的<em>“Bean 间引用”</em>保证遵循作用域和 AOP 语义，就像 {@code getBean()} 查找一
 * 样。这些是源自最初的 “Spring JavaConfig” 项目的语义，它要求在运行时对每个这样的配置类进行 CGLIB
 * 子类化。因此，在此模式下，{@code @Configuration} 类及其工厂方法不得标记为 final 或 private。例如：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService(fooRepository());
 *     }
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     // ...
 * }</pre>
 *
 * <h3>{@code @Bean} <em>Lite</em> 模式</h3>
 *
 * <p>{@code @Bean} 方法也可以声明在<em>未</em>标注 {@code @Configuration} 的类中。例如，Bean 方法
 * 可以声明在 {@code @Component} 类中，甚至声明在<em>普通旧式类</em>中。在这些情况下，{@code @Bean}
 * 方法将以所谓的<em>“lite”</em> 模式处理。
 *
 * <p><em>lite</em> 模式下的 Bean 方法会被容器视为普通的<em>工厂方法</em>（类似于 XML 中的
 * {@code factory-method} 声明），并正确应用作用域和生命周期回调。在这种情况下，包含类保持不变，对
 * 包含类或工厂方法也没有任何特殊约束。
 *
 * <p>与 {@code @Configuration} 类中 Bean 方法的语义不同，<em>lite</em> 模式不支持<em>“Bean 间引用”
 * </em>。相反，当一个 {@code @Bean} 方法在 <em>lite</em> 模式下调用另一个 {@code @Bean} 方法时，该
 * 调用是标准的 Java 方法调用；Spring 不会通过 CGLIB 代理拦截该调用。这类似于
 * {@code @Transactional} 方法之间的调用：在代理模式下，Spring 不会拦截该调用&mdash;只有在 AspectJ
 * 模式下才会拦截。
 *
 * <p>例如：
 *
 * <pre class="code">
 * &#064;Component
 * public class Calculator {
 *     public int sum(int a, int b) {
 *         return a+b;
 *     }
 *
 *     &#064;Bean
 *     public MyBean myBean() {
 *         return new MyBean();
 *     }
 * }</pre>
 *
 * <h3>引导</h3>
 *
 * <p>有关更多细节（包括如何使用 {@link AnnotationConfigApplicationContext} 等类引导容器），请参阅
 * &#64;{@link Configuration} 的 Javadoc。
 *
 * <h3>返回 {@code BeanFactoryPostProcessor} 的 {@code @Bean} 方法</h3>
 *
 * <p>对于返回 Spring
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessor}
 * （{@code BFPP}）类型的 {@code @Bean} 方法，必须特别留意。由于 {@code BFPP} 对象必须在容器生命周
 * 期的很早期实例化，它们可能会干扰 {@code @Configuration} 类中诸如 {@code @Autowired}、
 * {@code @Value} 和 {@code @PostConstruct} 等注解的处理。为避免这些生命周期问题，请将返回
 * {@code BFPP} 的 {@code @Bean} 方法标记为 {@code static}。例如：
 *
 * <pre class="code">
 *     &#064;Bean
 *     public static PropertySourcesPlaceholderConfigurer pspc() {
 *         // instantiate, configure and return pspc ...
 *     }
 * </pre>
 *
 * 通过将该方法标记为 {@code static}，可以在不触发其所属 {@code @Configuration} 类实例化的情况下调
 * 用它，从而避免上述生命周期冲突。但请注意，如上所述，{@code static} 的 {@code @Bean} 方法不会针对
 * 作用域和 AOP 语义进行增强。这在 {@code BFPP} 场景下没有问题，因为它们通常不会被其他
 * {@code @Bean} 方法引用。提醒一下，对于返回类型可赋值给 {@code BeanFactoryPostProcessor} 的任何非
 * static {@code @Bean} 方法，都会发出 INFO 级别的日志消息。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see Configuration
 * @see Scope
 * @see DependsOn
 * @see Lazy
 * @see Primary
 * @see org.springframework.stereotype.Component
 * @see org.springframework.beans.factory.annotation.Autowired
 * @see org.springframework.beans.factory.annotation.Value
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

	/**
	 * 是 {@link #name} 的别名。<p>用于不需要其他属性的场景，例如：{@code @Bean("customBeanName")}。
	 * @since 4.3.3
	 * @see #name
	 */
	@AliasFor("name")
	String[] value() default {};

	/**
	 * 此 Bean 的名称；如果有多个名称，则为主 Bean 名称加上别名。<p>如果未指定，则 Bean 的名称为所标注
	 * 方法的名称。如果指定了，则忽略方法名称。<p>如果没有声明其他属性，Bean 名称和别名也可以通过
	 * {@link #value} 属性配置。
	 * @see #value
	 */
	@AliasFor("value")
	String[] name() default {};

	/**
	 * 依赖是否通过基于约定的按名称或按类型自动装配来注入？<p>请注意，此自动装配模式仅涉及基于 Bean 属
	 * 性 setter 方法、按约定的外部驱动自动装配，类似于 XML Bean 定义。<p>默认模式确实允许注解驱动的自
	 * 动装配。“no”仅指外部驱动的自动装配，不影响 Bean 类自身通过注解表达的自动装配需求。
	 * @see Autowire#BY_NAME
	 * @see Autowire#BY_TYPE
	 * @deprecated as of 5.1, since {@code @Bean} factory method argument resolution and
	 * {@code @Autowired} processing supersede name/type-based bean property injection
	 */
	@Deprecated
	Autowire autowire() default Autowire.NO;

	/**
	 * 此 Bean 是否可作为被自动装配到其他 Bean 中的候选者？<p>默认值为 {@code true}；对于不打算妨碍其他
	 * 位置同类型 Bean 的内部委托对象，请将其设置为 {@code false}。
	 * @since 5.1
	 */
	boolean autowireCandidate() default true;

	/**
	 * 要在初始化期间在 Bean 实例上调用的可选方法名。不常用，因为该方法可以直接在标注了 Bean 的方法体内
	 * 以编程方式调用。<p>默认值为 {@code ""}，表示不调用任何初始化方法。
	 * @see org.springframework.beans.factory.InitializingBean
	 * @see org.springframework.context.ConfigurableApplicationContext#refresh()
	 */
	String initMethod() default "";

	/**
	 * 要在关闭应用上下文时在 Bean 实例上调用的可选方法名，例如 JDBC {@code DataSource} 实现上的
	 * {@code close()} 方法，或 Hibernate {@code SessionFactory} 对象上的相应方法。该方法必须没有参数，
	 * 但可以抛出任何异常。<p>为方便用户，容器会尝试针对 {@code @Bean} 方法返回的对象推断销毁方法。例如，
	 * 给定一个返回 Apache Commons DBCP {@code BasicDataSource} 的 {@code @Bean} 方法，容器会注意到该
	 * 对象上可用的 {@code close()} 方法，并自动将其注册为 {@code destroyMethod}。此“销毁方法推断”目前
	 * 仅限于检测名为 “close” 或 “shutdown” 的公共无参方法。该方法可以声明在继承层次的任何层级，并且无
	 * 论 {@code @Bean} 方法的返回类型是什么都会被检测到（即检测是在创建时针对 Bean 实例本身以反射方式
	 * 进行的）。<p>要为特定的 {@code @Bean} 禁用销毁方法推断，请将值指定为空字符串，例如
	 * {@code @Bean(destroyMethod="")}。请注意，
	 * {@link org.springframework.beans.factory.DisposableBean} 回调接口仍会被检测到并调用相应的销毁方
	 * 法：换言之，{@code destroyMethod=""} 只影响自定义的 close/shutdown 方法以及声明为
	 * {@link java.io.Closeable}/{@link java.lang.AutoCloseable} 的 close 方法。<p>注意：仅对生命周期完
	 * 全由工厂控制的 Bean 调用，对于 singleton 始终如此，但对其他任何作用域则不作保证。
	 * @see org.springframework.beans.factory.DisposableBean
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	String destroyMethod() default AbstractBeanDefinition.INFER_METHOD;

}
