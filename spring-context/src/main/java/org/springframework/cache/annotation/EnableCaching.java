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

package org.springframework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用 Spring 的注解驱动缓存管理能力，类似于 Spring 的 {@code <cache:*>} XML 命名空间中的支持。需
 * 与 @{@link org.springframework.context.annotation.Configuration Configuration} 类一起使用，如下
 * 所示：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 * }</pre>
 *
 * <p>作为参考，可以将上面的示例与以下 Spring XML 配置进行比较：
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;cache:annotation-driven/&gt;
 *
 *     &lt;bean id="myService" class="com.foo.MyService"/&gt;
 *
 *     &lt;bean id="cacheManager" class="org.springframework.cache.support.SimpleCacheManager"&gt;
 *         &lt;property name="caches"&gt;
 *             &lt;set&gt;
 *                 &lt;bean class="org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean"&gt;
 *                     &lt;property name="name" value="default"/&gt;
 *                 &lt;/bean&gt;
 *             &lt;/set&gt;
 *         &lt;/property&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * 在上述两种场景中，{@code @EnableCaching} 和 {@code <cache:annotation-driven/>} 负责注册支持注解
 * 驱动缓存管理所需的 Spring 组件，例如
 * {@link org.springframework.cache.interceptor.CacheInterceptor CacheInterceptor}，以及当调用
 * {@link org.springframework.cache.annotation.Cacheable @Cacheable} 方法时将该拦截器织入调用栈的基
 * 于代理或 AspectJ 的通知。
 *
 * <p>如果存在 JSR-107 API 和 Spring 的 JCache 实现，则还会注册管理标准缓存注解所需的组件。这会创建
 * 基于代理或 AspectJ 的通知，在调用带有 {@code CacheResult}、{@code CachePut}、{@code CacheRemove}
 * 或 {@code CacheRemoveAll} 注解的方法时将该拦截器织入调用栈。
 *
 * <p><strong>必须注册一个 {@link org.springframework.cache.CacheManager CacheManager} 类型的 Bean
 * </strong>，因为框架没有可用作约定的合理默认值。此外，{@code <cache:annotation-driven>} 元素假定
 * 存在一个<em>名为</em>“cacheManager”的 Bean，而 {@code @EnableCaching} 则<em>按类型</em>搜索缓存
 * 管理器 Bean。因此，缓存管理器 Bean 方法的命名并不重要。
 *
 * <p>对于那些希望在 {@code @EnableCaching} 与要使用的确切缓存管理器 Bean 之间建立更直接关系的人，
 * 可以实现 {@link CachingConfigurer} 回调接口。请注意下面带有 {@code @Override} 注解的方法：
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableCaching
 * public class AppConfig extends CachingConfigurerSupport {
 *
 *     &#064;Bean
 *     public MyService myService() {
 *         // configure and return a class having &#064;Cacheable methods
 *         return new MyService();
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public CacheManager cacheManager() {
 *         // configure and return an implementation of Spring's CacheManager SPI
 *         SimpleCacheManager cacheManager = new SimpleCacheManager();
 *         cacheManager.setCaches(Arrays.asList(new ConcurrentMapCache("default")));
 *         return cacheManager;
 *     }
 *
 *     &#064;Bean
 *     &#064;Override
 *     public KeyGenerator keyGenerator() {
 *         // configure and return an implementation of Spring's KeyGenerator SPI
 *         return new MyKeyGenerator();
 *     }
 * }</pre>
 *
 * 采用此方法可能仅仅是因为它更显式，或者为了区分同一容器中存在的两个 {@code CacheManager} Bean，可
 * 能也是必要的。
 *
 * <p>另请注意上面示例中的 {@code keyGenerator} 方法。它允许按照 Spring 的
 * {@link org.springframework.cache.interceptor.KeyGenerator KeyGenerator} SPI 自定义缓存键生成策略。
 * 通常，{@code @EnableCaching} 会为此配置 Spring 的
 * {@link org.springframework.cache.interceptor.SimpleKeyGenerator SimpleKeyGenerator}，但在实现
 * {@code CachingConfigurer} 时，必须显式提供键生成器。如果无需自定义，请从此方法返回 {@code null}
 * 或 {@code new SimpleKeyGenerator()}。
 *
 * <p>{@link CachingConfigurer} 提供了额外的自定义选项：建议继承
 * {@link org.springframework.cache.annotation.CachingConfigurerSupport CachingConfigurerSupport}，
 * 它为所有方法提供了默认实现，这在不需要自定义所有内容时可能会很有用。有关更多详细信息，请参见
 * {@link CachingConfigurer} 的 Javadoc。
 *
 * <p>{@link #mode} 属性控制通知的应用方式：如果模式为 {@link AdviceMode#PROXY}（默认值），则其他属
 * 性控制代理的行为。请注意，代理模式仅允许拦截通过代理的调用；同一类内部的本地调用无法通过这种方式
 * 被拦截。
 *
 * <p>请注意，如果将 {@linkplain #mode} 设置为 {@link AdviceMode#ASPECTJ}，则
 * {@link #proxyTargetClass} 属性的值将被忽略。另请注意，在这种情况下，类路径上必须存在
 * {@code spring-aspects} 模块 JAR，并通过编译期织入或加载期织入将切面应用到受影响的类。此场景不涉
 * 及代理；本地调用也会被拦截。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see CachingConfigurer
 * @see CachingConfigurationSelector
 * @see ProxyCachingConfiguration
 * @see org.springframework.cache.aspectj.AspectJCachingConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(CachingConfigurationSelector.class)
public @interface EnableCaching {

	/**
	 * 指示是否要创建基于子类（CGLIB）的代理，而不是标准的基于 Java 接口的代理。默认值为 {@code false}。
	 * <strong>仅当 {@link #mode()} 设置为 {@link AdviceMode#PROXY} 时适用</strong>。
	 *
	 * <p>请注意，将此属性设置为 {@code true} 将影响<em>所有</em>需要代理的 Spring 管理 Bean，而不仅仅
	 * 是标有 {@code @Cacheable} 的 Bean。例如，标有 Spring 的 {@code @Transactional} 注解的其他 Bean
	 * 也会同时升级为子类代理。此方法在实践中没有负面影响，除非明确期望一种代理类型而不是另一种，例如在
	 * 测试中。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指示应如何应用缓存通知。
	 *
	 * <p><b>默认值为 {@link AdviceMode#PROXY}。</b>请注意，代理模式仅允许拦截通过代理的调用。同一类内
	 * 部的本地调用无法通过这种方式被拦截；本地调用中此类方法上的缓存注解将被忽略，因为 Spring 的拦截器
	 * 在这种运行时场景下甚至不会介入。如需更高级的拦截模式，请考虑将其切换为
	 * {@link AdviceMode#ASPECTJ}。
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指示当在特定连接点应用多个通知时缓存通知器的执行顺序。
	 *
	 * <p>默认值为 {@link Ordered#LOWEST_PRECEDENCE}。
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
