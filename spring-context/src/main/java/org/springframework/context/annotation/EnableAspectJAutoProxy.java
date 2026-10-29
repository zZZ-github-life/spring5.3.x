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

/**
 * 启用对带有 AspectJ {@code @Aspect} 注解的组件的处理支持，其功能类似于 Spring 的
 * {@code <aop:aspectj-autoproxy>} XML 元素。需用在 @{@link Configuration} 类上，如下所示：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooService fooService() {
 *         return new FooService();
 *     }
 *
 *     &#064;Bean
 *     public MyAspect myAspect() {
 *         return new MyAspect();
 *     }
 * }</pre>
 *
 * 其中 {@code FooService} 是典型的 POJO 组件，{@code MyAspect} 是 {@code @Aspect} 风格的切面：
 *
 * <pre class="code">
 * public class FooService {
 *
 *     // various methods
 * }</pre>
 *
 * <pre class="code">
 * &#064;Aspect
 * public class MyAspect {
 *
 *     &#064;Before("execution(* FooService+.*(..))")
 *     public void advice() {
 *         // advise FooService methods as appropriate
 *     }
 * }</pre>
 *
 * 在上述场景中，{@code @EnableAspectJAutoProxy} 确保 {@code MyAspect} 得到正确处理，并确保
 * {@code FooService} 被代理并混入其提供的通知。
 *
 * <p>用户可以通过 {@link #proxyTargetClass()} 属性控制为 {@code FooService} 创建的代理类型。以下配
 * 置启用 CGLIB 风格的“子类”代理，而非默认基于接口的 JDK 代理方式。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAspectJAutoProxy(proxyTargetClass=true)
 * public class AppConfig {
 *     // ...
 * }</pre>
 *
 * <p>请注意，{@code @Aspect} Bean 可以像其他任何 Bean 一样被组件扫描。只需同时用 {@code @Aspect}
 * 和 {@code @Component} 标记该切面：
 *
 * <pre class="code">
 * package com.foo;
 *
 * &#064;Component
 * public class FooService { ... }
 *
 * &#064;Aspect
 * &#064;Component
 * public class MyAspect { ... }</pre>
 *
 * 然后使用 @{@link ComponentScan} 注解将两者都扫描进来：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;ComponentScan("com.foo")
 * &#064;EnableAspectJAutoProxy
 * public class AppConfig {
 *
 *     // no explicit &#064;Bean definitions required
 * }</pre>
 *
 * <b>注意：{@code @EnableAspectJAutoProxy} 仅适用于其本地应用上下文，从而允许在不同层级有选择地代
 * 理 Bean。</b> 如果需要在多个层级应用该行为，请在每个独立上下文中重新声明
 * {@code @EnableAspectJAutoProxy}，例如公共的根 Web 应用上下文以及任何独立的
 * {@code DispatcherServlet} 应用上下文。
 *
 * <p>此功能要求类路径上存在 {@code aspectjweaver}。虽然该依赖对于 {@code spring-aop} 总体而言是可
 * 选的，但对于 {@code @EnableAspectJAutoProxy} 及其底层设施则是必需的。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.aspectj.lang.annotation.Aspect
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(AspectJAutoProxyRegistrar.class)
public @interface EnableAspectJAutoProxy {

	/**
	 * 指示是否创建基于子类的（CGLIB）代理，而非标准的基于 Java 接口的代理。默认值为 {@code false}。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指示 AOP 框架应将该代理作为 {@code ThreadLocal} 暴露，以便通过
	 * {@link org.springframework.aop.framework.AopContext} 类获取。默认关闭，即不保证
	 * {@code AopContext} 访问可用。
	 * @since 4.3.1
	 */
	boolean exposeProxy() default false;

}
