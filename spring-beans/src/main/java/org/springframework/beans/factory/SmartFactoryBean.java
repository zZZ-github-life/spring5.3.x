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

package org.springframework.beans.factory;

/**
 * {@link FactoryBean} 接口的扩展。实现可表明其是否总是返回独立实例，用于其 {@link #isSingleton()}
 * 实现返回 {@code false} 并不能明确表示独立实例的情况。
 *
 * <p>未实现此扩展接口的普通 {@link FactoryBean} 实现，如果其 {@link #isSingleton()} 实现返回
 * {@code false}，就直接假定其总是返回独立实例；所暴露的对象仅按需访问。
 *
 * <p><b>NOTE:</b> 该接口是一个专用接口，主要用于框架内部以及协作框架之间使用。一般而言，应用提供的
 * FactoryBean 应只实现普通的 {@link FactoryBean} 接口。即使在补丁版本中，也可能向该扩展接口添加新
 * 方法。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @param <T> Bean 类型
 * @see #isPrototype()
 * @see #isSingleton()
 */
public interface SmartFactoryBean<T> extends FactoryBean<T> {

	/**
	 * 此工厂所管理的对象是原型吗？也就是说，{@link #getObject()} 是否总是返回独立实例？
	 * <p>FactoryBean 自身的原型状态通常由拥有它的 {@link BeanFactory} 提供；通常必须在其中将其定义为单
	 * 例。
	 * <p>该方法应当严格检查独立实例；对于作用域对象或其他非单例、非独立的对象，不应返回 {@code true}。
	 * 因此，它并不只是 {@link #isSingleton()} 的取反形式。
	 * <p>默认实现返回 {@code false}。
	 * @return 所暴露的对象是否为原型
	 * @see #getObject()
	 * @see #isSingleton()
	 */
	default boolean isPrototype() {
		return false;
	}

	/**
	 * 该 FactoryBean 是否期望急切初始化，即急切地初始化自身，并期望其单例对象（如果有）也急切初始化？
	 * <p>标准的 FactoryBean 不期望急切初始化：即使对象是单例，其 {@link #getObject()} 也仅在真正访问时
	 * 才会被调用。从此方法返回 {@code true} 表示应急切调用 {@link #getObject()}，同时也急切应用后处理
	 * 器。对于 {@link #isSingleton() singleton} 对象，这可能是有意义的，特别是当后处理器期望在启动时被
	 * 应用时。
	 * <p>默认实现返回 {@code false}。
	 * @return 是否适用急切初始化
	 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
	 */
	default boolean isEagerInit() {
		return false;
	}

}
