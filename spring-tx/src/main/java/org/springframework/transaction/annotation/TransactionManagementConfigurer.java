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

import org.springframework.transaction.TransactionManager;

/**
 * 由标注了@{@link EnableTransactionManagement}的@{@link org.springframework.context.annotation.Configuration
 * Configuration}类实现的接口, 用于(或需要)显式指定用于注解驱动事务管理的默认{@code PlatformTransactionManager} bean
 * (或{@code ReactiveTransactionManager} bean), 而不是默认的按类型查找方式。
 * 一个需要这样做的原因是容器中存在两个{@code PlatformTransactionManager} bean(或两个{@code ReactiveTransactionManager} bean)。
 *
 * <p>一般示例和上下文请参阅@{@link EnableTransactionManagement}; 详细说明请参阅{@link #annotationDrivenTransactionManager()}。
 *
 * <p>请注意, 在按类型查找存在歧义的情况下, 实现此接口的替代方法是简单地将其中一个
 * {@code PlatformTransactionManager} {@code @Bean}方法(或{@code ReactiveTransactionManager} {@code @Bean}方法)
 * 标记为{@link org.springframework.context.annotation.Primary @Primary}。
 * 这通常是首选方案, 因为它不会导致提前初始化{@code TransactionManager} bean。
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableTransactionManagement
 * @see org.springframework.context.annotation.Primary
 * @see org.springframework.transaction.PlatformTransactionManager
 * @see org.springframework.transaction.ReactiveTransactionManager
 */
public interface TransactionManagementConfigurer {

	/**
	 * 返回用于注解驱动数据库事务管理的默认事务管理器bean, 即处理{@code @Transactional}方法时使用的事务管理器。
	 * <p>实现此方法有两种基本方式:
	 * <h3>1. 实现方法并用{@code @Bean}注解</h3>
	 * 在这种情况下, 实现此方法的{@code @Configuration}类用{@code @Bean}标注此方法,
	 * 并在方法体内直接配置和返回事务管理器:
	 * <pre class="code">
	 * &#064;Bean
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }</pre>
	 * <h3>2. 不使用{@code @Bean}实现方法, 委托给另一个已有的{@code @Bean}方法</h3>
	 * <pre class="code">
	 * &#064;Bean
	 * public PlatformTransactionManager txManager() {
	 *     return new DataSourceTransactionManager(dataSource());
	 * }
	 *
	 * &#064;Override
	 * public PlatformTransactionManager annotationDrivenTransactionManager() {
	 *     return txManager(); // reference the existing {@code @Bean} method above
	 * }</pre>
	 * 如果采用第2种方式, 请确保<em>只有一个</em>方法被{@code @Bean}标注!
	 * <p>在场景#1或#2中, 重要的是{@code PlatformTransactionManager}实例作为Spring bean
	 * 在容器中进行管理, 因为大多数{@code PlatformTransactionManager}实现利用了Spring的生命周期回调,
	 * 如{@code InitializingBean}和{@code BeanFactoryAware}。请注意, 同样的准则也适用于{@code ReactiveTransactionManager} bean。
	 * @return {@link org.springframework.transaction.PlatformTransactionManager} 或
	 * {@link org.springframework.transaction.ReactiveTransactionManager} 实现
	 */
	TransactionManager annotationDrivenTransactionManager();

}
