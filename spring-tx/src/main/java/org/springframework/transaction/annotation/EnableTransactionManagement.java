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

package org.springframework.transaction.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

/**
 * 启用Spring的注解驱动事务管理能力, 类似于Spring的{@code <tx:*>} XML命名空间中的支持。
 * 用于{@link org.springframework.context.annotation.Configuration @Configuration}类上,
 * 以配置传统的命令式事务管理或响应式事务管理。
 *
 * <p>以下示例演示了使用{@link org.springframework.transaction.PlatformTransactionManager
 * PlatformTransactionManager}的命令式事务管理。对于响应式事务管理, 需要配置
 * {@link org.springframework.transaction.ReactiveTransactionManager
 * ReactiveTransactionManager}。
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 * }</pre>
 *
 * <p>作为参考, 上述示例可以与以下Spring XML配置进行比较:
 *
 * <pre class="code">
 * &lt;beans&gt;
 *
 *     &lt;tx:annotation-driven/&gt;
 *
 *     &lt;bean id="fooRepository" class="com.foo.JdbcFooRepository"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 *     &lt;bean id="dataSource" class="com.vendor.VendorDataSource"/&gt;
 *
 *     &lt;bean id="transactionManager" class="org.sfwk...DataSourceTransactionManager"&gt;
 *         &lt;constructor-arg ref="dataSource"/&gt;
 *     &lt;/bean&gt;
 *
 * &lt;/beans&gt;
 * </pre>
 *
 * 在上述两个场景中, {@code @EnableTransactionManagement}和{@code
 * <tx:annotation-driven/>}负责注册驱动注解事务管理所需的Spring基础组件,
 * 例如TransactionInterceptor以及基于代理或AspectJ的通知, 该通知在调用
 * {@code JdbcFooRepository}的{@code @Transactional}方法时将拦截器织入调用链。
 *
 * <p>两个示例之间的一个细微差别在于{@code TransactionManager} bean的命名:
 * 在{@code @Bean}方式中, 名称为<em>"txManager"</em>(根据方法名); 在XML方式中,
 * 名称为<em>"transactionManager"</em>。{@code <tx:annotation-driven/>}默认硬编码
 * 查找名为"transactionManager"的bean, 而{@code @EnableTransactionManagement}更加灵活;
 * 它将回退为按类型查找容器中的任何{@code TransactionManager} bean。因此名称可以是
 * "txManager"、"transactionManager"或"tm": 这无关紧要。
 *
 * <p>对于那些希望在{@code @EnableTransactionManagement}和要使用的确切事务管理器bean之间
 * 建立更直接关系的人, 可以实现{@link TransactionManagementConfigurer}回调接口 -
 * 请注意下面的{@code implements}子句和{@code @Override}标注的方法:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableTransactionManagement
 * public class AppConfig implements TransactionManagementConfigurer {
 *
 *     &#064;Bean
 *     public FooRepository fooRepository() {
 *         // configure and return a class having &#064;Transactional methods
 *         return new JdbcFooRepository(dataSource());
 *     }
 *
 *     &#064;Bean
 *     public DataSource dataSource() {
 *         // configure and return the necessary JDBC DataSource
 *     }
 *
 *     &#064;Bean
 *     public PlatformTransactionManager txManager() {
 *         return new DataSourceTransactionManager(dataSource());
 *     }
 *
 *     &#064;Override
 *     public PlatformTransactionManager annotationDrivenTransactionManager() {
 *         return txManager();
 *     }
 * }</pre>
 *
 * <p>这种方式可能是可取的, 因为它更加明确, 或者在容器中存在两个{@code TransactionManager}
 * bean时需要区分它们。顾名思义, {@code annotationDrivenTransactionManager()}将用于
 * 处理{@code @Transactional}方法。详见{@link TransactionManagementConfigurer}的Javadoc。
 *
 * <p>{@link #mode}属性控制通知的应用方式: 如果模式为{@link AdviceMode#PROXY}(默认值),
 * 则其他属性控制代理的行为。请注意, 代理模式仅允许拦截通过代理的调用; 同一类内的本地调用
 * 无法以这种方式被拦截。
 *
 * <p>请注意, 如果{@linkplain #mode}设置为{@link AdviceMode#ASPECTJ}, 则
 * {@link #proxyTargetClass}属性的值将被忽略。同时请注意, 在这种情况下,
 * {@code spring-aspects}模块JAR必须存在于类路径上, 通过编译时织入或加载时织入
 * 将切面应用到受影响的类。这种场景不涉及代理; 本地调用也将被拦截。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see TransactionManagementConfigurer
 * @see TransactionManagementConfigurationSelector
 * @see ProxyTransactionManagementConfiguration
 * @see org.springframework.transaction.aspectj.AspectJTransactionManagementConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TransactionManagementConfigurationSelector.class)
public @interface EnableTransactionManagement {

	/**
	 * 指示是否创建基于子类的(CGLIB)代理({@code true}), 而不是标准的基于Java接口的代理({@code false})。
	 * 默认值为{@code false}。<strong>仅在{@link #mode()}设置为{@link AdviceMode#PROXY}时适用</strong>。
	 * <p>请注意, 将此属性设置为{@code true}将影响<em>所有</em>需要代理的Spring托管bean,
	 * 而不仅仅是标记了{@code @Transactional}的bean。例如, 其他标记了Spring的{@code @Async}注解
	 * 的bean也将同时升级为子类代理。这种方式在实践中不会产生负面影响, 除非明确期望某种类型的代理
	 * 而非另一种, 例如在测试中。
	 */
	boolean proxyTargetClass() default false;

	/**
	 * 指示事务通知的应用方式。
	 * <p><b>默认值为{@link AdviceMode#PROXY}。</b>
	 * 请注意, 代理模式仅允许拦截通过代理的调用。同一类内的本地调用无法以这种方式被拦截;
	 * 在本地调用中, 此类方法上的{@link Transactional}注解将被忽略, 因为Spring的拦截器
	 * 甚至不会为这种运行时场景启动。对于更高级的拦截模式, 请考虑切换为{@link AdviceMode#ASPECTJ}。
	 */
	AdviceMode mode() default AdviceMode.PROXY;

	/**
	 * 指示在特定连接点应用多个通知时事务通知器的执行顺序。
	 * <p>默认值为{@link Ordered#LOWEST_PRECEDENCE}。
	 */
	int order() default Ordered.LOWEST_PRECEDENCE;

}
