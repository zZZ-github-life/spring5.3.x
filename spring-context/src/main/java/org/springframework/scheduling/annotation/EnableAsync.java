/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.scheduling.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用 Spring 的异步方法执行能力，类似于 Spring 的 {@code <task:*>} XML 命名空间中的功能。
 *
 * <p>按如下方式与 @{@link Configuration Configuration} 类一起使用，可为整个 Spring 应用上下文启用
 * 注解驱动的异步处理：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig {
 *
 * }</pre>
 *
 * {@code MyAsyncBean} 是用户定义的类型，其一个或多个方法标注了 Spring 的 {@code @Async} 注解、EJB
 * 3.1 的 {@code @javax.ejb.Asynchronous} 注解，或通过 {@link #annotation} 属性指定的任何自定义注解。
 * 对于任何已注册的 Bean，切面都会被透明地添加，例如通过以下配置：
 *
 * <pre class="code">
 * &#064;Configuration
 * public class AnotherAppConfig {
 *
 *     &#064;Bean
 *     public MyAsyncBean asyncBean() {
 *         return new MyAsyncBean();
 *     }
 * }</pre>
 *
 * <p>默认情况下，Spring 会查找关联的线程池定义：上下文中的唯一
 * {@link org.springframework.core.task.TaskExecutor} Bean，否则查找名为 "taskExecutor" 的
 * {@link java.util.concurrent.Executor} Bean。如果两者都无法解析，将使用
 * {@link org.springframework.core.task.SimpleAsyncTaskExecutor} 来处理异步方法调用。此外，返回类型
 * 为 {@code void} 的带注解方法无法将任何异常传回调用方。默认情况下，此类未捕获异常只会被记录日志。
 *
 * <p>要自定义所有这些，请实现 {@link AsyncConfigurer} 并提供：
 * <ul>
 * <li>your own {@link java.util.concurrent.Executor Executor} through the
 * {@link AsyncConfigurer#getAsyncExecutor getAsyncExecutor()} method, and</li>
 * <li>your own {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler} through the {@link AsyncConfigurer#getAsyncUncaughtExceptionHandler
 * getAsyncUncaughtExceptionHandler()}
 * method.</li>
 * </ul>
 *
 * <p><b>注意：{@link AsyncConfigurer} 配置类会在应用上下文引导的早期被初始化。如果在那里需要依赖其
 * 他 Bean，请尽可能将其声明为“惰性”（lazy），以便它们也能经过其他后处理器。</b>
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAsync
 * public class AppConfig implements AsyncConfigurer {
 *
 *     &#064;Override
 *     public Executor getAsyncExecutor() {
 *         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *         executor.setCorePoolSize(7);
 *         executor.setMaxPoolSize(42);
 *         executor.setQueueCapacity(11);
 *         executor.setThreadNamePrefix("MyExecutor-");
 *         executor.initialize();
 *         return executor;
 *     }
 *
 *     &#064;Override
 *     public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
 *         return new MyAsyncUncaughtExceptionHandler();
 *     }
 * }</pre>
 *
 * <p>如果只需自定义一项，可以返回 {@code null} 以保留默认设置。在可能的情况下，也可以考虑继承
 * {@link AsyncConfigurerSupport}。
 *
 * <p>注意：在上面的示例中，{@code ThreadPoolTaskExecutor} 并不是完全托管的 Spring Bean。如果需要完
 * 全托管的 Bean，请在 {@code getAsyncExecutor()} 方法上添加 {@code @Bean} 注解。在这种情况下，不再
 * 需要手动调用 {@code executor.initialize()} 方法，因为 Bean 初始化时会自动调用它。
 *
 * <p>作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;task:annotation-driven executor="myExecutor" exception-handler="exceptionHandler"/&gt;
 *
 *     &lt;task:executor id="myExecutor" pool-size="7-42" queue-capacity="11"/&gt;
 *
 *     &lt;bean id="asyncBean" class="com.foo.MyAsyncBean"/&gt;
 *
 *     &lt;bean id="exceptionHandler" class="com.foo.MyAsyncUncaughtExceptionHandler"/&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * 除 {@code Executor} 的<em>线程名前缀</em>设置外，上面基于 XML 和基于 JavaConfig 的示例是等价的；
 * 这是因为 {@code <task:executor>} 元素不暴露此类属性。这展示了基于 JavaConfig 的方式如何通过直接
 * 访问实际组件来实现最大程度的可配置性。
 *
 * <p>{@link #mode} 属性控制通知的应用方式：如果模式为 {@link AdviceMode#PROXY}（默认值），则其他属
 * 性控制代理的行为。请注意，代理模式只允许拦截通过代理进行的调用；同一类内部的本地调用无法以这种方
 * 式被拦截。
 *
 * <p>请注意，如果 {@linkplain #mode} 设置为 {@link AdviceMode#ASPECTJ}，则
 * {@link #proxyTargetClass} 属性的值将被忽略。另请注意，在这种情况下，类路径上必须存在
 * {@code spring-aspects} 模块 JAR，并通过编译期织入或加载期织入将切面应用到受影响的类。此场景中不
 * 涉及代理；本地调用也会被拦截。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.1
 * @see Async
 * @see AsyncConfigurer
 * @see AsyncConfigurationSelector
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AsyncConfigurationSelector.class)
public @interface EnableAsync {

	/**
	 * 指示要在类级别或方法级别检测的 'async' 注解类型。
	 * <p>默认情况下，将同时检测 Spring 的 @{@link Async} 注解和 EJB 3.1 的
	 * {@code @javax.ejb.Asynchronous} 注解。
	 * <p>提供此属性是为了让开发者可以提供自己的自定义注解类型，用于表示某个方法（或给定类的所有方法）
	 * 应异步调用。
	 */
	Class<? extends Annotation> annotation() default Annotation.class;

	/**
	 * 指示是否要创建基于子类的（CGLIB）代理，而不是标准的基于 Java 接口的代理。
	 * <p><strong>仅在 {@link #mode} 设置为 {@link AdviceMode#PROXY} 时适用</strong>。
	 * <p>默认值为 {@code false}。
	 * <p>请注意，将此属性设置为 {@code true} 会影响<em>所有</em>需要代理的 Spring 托管 Bean，而不仅仅
	 * 是标记了 {@code @Async} 的 Bean。例如，其他标记了 Spring 的 {@code @Transactional} 注解的 Bean
	 * 也会同时升级为子类代理。这种做法在实践中没有负面影响，除非明确期望使用某一种代理而非另一种
	 * &mdash; 例如在测试中。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指示应如何应用异步通知。
	 * <p><b>默认值为 {@link AdviceMode#PROXY}。</b>请注意，代理模式只允许拦截通过代理进行的调用。同一
	 * 类内部的本地调用无法以这种方式被拦截；本地调用中此类方法上的 {@link Async} 注解将被忽略，因为在
	 * 这种运行时场景下 Spring 的拦截器根本不会介入。如需更高级的拦截模式，可考虑将其切换为
	 * {@link AdviceMode#ASPECTJ}。
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指示 {@link AsyncAnnotationBeanPostProcessor} 的应用顺序。
	 * <p>默认值为 {@link Ordered#LOWEST_PRECEDENCE}，以便在所有其他后处理器之后运行，从而可以向现有代
	 * 理添加通知器，而不是进行双重代理。
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
