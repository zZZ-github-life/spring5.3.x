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

package org.springframework.beans.factory;

import org.springframework.lang.Nullable;

/**
 * 由在 {@link BeanFactory} 中使用、且自身是各个对象的工厂的对象实现的接口。如果某个 Bean 实现了此
 * 接口，它将被用作要公开对象的工厂，而不是直接作为其自身被公开的 Bean 实例。
 *
 * <p><b>注意：实现此接口的 Bean 不能用作普通 Bean。</b>FactoryBean 以 Bean 的方式定义，但为 Bean
 * 引用（{@link #getObject()}）公开的对象始终是它创建的对象。
 *
 * <p>FactoryBean 可以支持单例和原型，并且可以按需延迟创建对象，或在启动时急切创建对象。
 * {@link SmartFactoryBean} 接口允许公开更细粒度的行为元数据。
 *
 * <p>此接口在框架内部被大量使用，例如用于 AOP 的
 * {@link org.springframework.aop.framework.ProxyFactoryBean} 或
 * {@link org.springframework.jndi.JndiObjectFactoryBean}。它也可用于自定义组件；不过，这通常只见于
 * 基础设施代码。
 *
 * <p><b>{@code FactoryBean} 是一种编程契约。实现不应依赖注解驱动的注入或其他反射设施。</b>
 * {@link #getObjectType()} 与 {@link #getObject()} 调用可能在引导过程的早期到达，甚至早于任何后处
 * 理器设置。如果需要访问其他 Bean，请实现 {@link BeanFactoryAware} 并以编程方式获取它们。
 *
 * <p><b>容器只负责管理 FactoryBean 实例的生命周期，而不负责管理由 FactoryBean 创建的对象的生命周期。
 * </b>因此，公开的 Bean 对象上的销毁方法（例如 {@link java.io.Closeable#close()}）<i>不会</i>被
 * 自动调用。相反，FactoryBean 应实现 {@link DisposableBean}，并将任何此类关闭调用委托给底层对象。
 *
 * <p>最后，FactoryBean 对象会参与包含它的 BeanFactory 对 Bean 创建的同步。通常除了在 FactoryBean
 * 自身（或类似场景）内进行延迟初始化之外，不需要内部同步。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 08.03.2003
 * @param <T> the bean type
 * @see org.springframework.beans.factory.BeanFactory
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see org.springframework.jndi.JndiObjectFactoryBean
 */
public interface FactoryBean<T> {

	/**
	 * 某个属性的名称，该属性可通过 {@link org.springframework.core.AttributeAccessor#setAttribute set}
	 * 设置到 {@link org.springframework.beans.factory.config.BeanDefinition} 上，以便工厂 Bean 在无法
	 * 从工厂 Bean 类推断其对象类型时表明该类型。
	 * @since 5.2
	 */
	String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";


	/**
	 * 返回由此工厂管理的对象的实例（可能是共享的或独立的）。<p>与 {@link BeanFactory} 一样，这支持
	 * Singleton 和 Prototype 设计模式。<p>如果此 FactoryBean 在调用时尚未完全初始化（例如因为它涉及循
	 * 环引用），则抛出相应的 {@link FactoryBeanNotInitializedException}。<p>自 Spring 2.0 起，
	 * FactoryBean 允许返回 {@code null} 对象。工厂会将其视为要使用的正常值；在这种情况下不再抛出
	 * FactoryBeanNotInitializedException。现在鼓励 FactoryBean 实现在适当时自行抛出
	 * FactoryBeanNotInitializedException。
	 * @return an instance of the bean (can be {@code null})
	 * @throws Exception in case of creation errors
	 * @see FactoryBeanNotInitializedException
	 */
	@Nullable
	T getObject() throws Exception;

	/**
	 * 返回此 FactoryBean 创建的对象类型，若事先未知则返回 {@code null}。<p>这允许在不实例化对象的情况
	 * 下检查特定类型的 Bean，例如在自动装配时。<p>对于创建单例对象的实现，此方法应尽可能避免创建单例；
	 * 而应事先估算类型。对于原型，在此返回有意义的类型也是可取的。<p>此方法可以在此 FactoryBean 完全初
	 * 始化<i>之前</i>被调用。它不得依赖初始化期间创建的状态；当然，如果这些状态可用，它仍然可以使用。
	 * <p><b>注意：</b>自动装配会直接忽略在此返回 {@code null} 的 FactoryBean。因此，强烈建议使用
	 * FactoryBean 的当前状态正确实现此方法。
	 * @return the type of object that this FactoryBean creates,
	 * or {@code null} if not known at the time of the call
	 * @see ListableBeanFactory#getBeansOfType
	 */
	@Nullable
	Class<?> getObjectType();

	/**
	 * 此工厂管理的对象是单例吗？也就是说，{@link #getObject()} 是否总是返回同一对象（一个可以被缓存的
	 * 引用）？<p><b>注意：</b>如果 FactoryBean 表示持有一个单例对象，则从 {@code getObject()} 返回的对
	 * 象可能会被所属的 BeanFactory 缓存。因此，除非 FactoryBean 总是公开同一引用，否则不要返回
	 * {@code true}。<p>FactoryBean 自身的单例状态通常由所属的 BeanFactory 提供；通常必须在其中将其定义
	 * 为单例。<p><b>注意：</b>此方法返回 {@code false} 并不一定表示返回的对象是独立实例。扩展的
	 * {@link SmartFactoryBean} 接口的实现可以通过其 {@link SmartFactoryBean#isPrototype()} 方法显式表
	 * 明独立实例。未实现此扩展接口的普通 {@link FactoryBean} 实现，如果 {@code isSingleton()} 实现返回
	 * {@code false}，则简单地假定其总是返回独立实例。<p>默认实现返回 {@code true}，因为
	 * {@code FactoryBean} 通常管理一个单例实例。
	 * @return whether the exposed object is a singleton
	 * @see #getObject()
	 * @see SmartFactoryBean#isPrototype()
	 */
	default boolean isSingleton() {
		return true;
	}

}
