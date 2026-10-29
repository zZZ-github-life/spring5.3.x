/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.objenesis;

import org.springframework.core.SpringProperties;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.objenesis.strategy.InstantiatorStrategy;
import org.springframework.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * {@link ObjenesisStd} / {@link ObjenesisBase} 的 Spring 特有变体，提供基于 {@code Class} 键而非类
 * 名的缓存，并允许有选择地使用该缓存。
 *
 * @author Juergen Hoeller
 * @since 4.2
 * @see #isWorthTrying()
 * @see #newInstance(Class, boolean)
 */
public class SpringObjenesis implements Objenesis {

	/**
	 * 系统属性，指示 Spring 忽略 Objenesis，甚至不尝试使用它。将此标志设置为 "true" 相当于让 Spring 发
	 * 现 Objenesis 在运行时无法工作，并立即触发回退代码路径：最重要的是，这意味着所有 CGLIB AOP 代理都
	 * 将通过默认构造函数以常规实例化方式创建。
	 */
	public static final String IGNORE_OBJENESIS_PROPERTY_NAME = "spring.objenesis.ignore";


	private final InstantiatorStrategy strategy;

	private final ConcurrentReferenceHashMap<Class<?>, ObjectInstantiator<?>> cache =
			new ConcurrentReferenceHashMap<>();

	private volatile Boolean worthTrying;


	/**
	 * 使用标准实例化器策略创建一个新的 {@code SpringObjenesis} 实例。
	 */
	public SpringObjenesis() {
		this(null);
	}

	/**
	 * 使用给定的标准实例化器策略创建一个新的 {@code SpringObjenesis} 实例。
	 * @param strategy the instantiator strategy to use
	 */
	public SpringObjenesis(InstantiatorStrategy strategy) {
		this.strategy = (strategy != null ? strategy : new StdInstantiatorStrategy());

		// Evaluate the "spring.objenesis.ignore" property upfront...
		if (SpringProperties.getFlag(SpringObjenesis.IGNORE_OBJENESIS_PROPERTY_NAME)) {
			this.worthTrying = Boolean.FALSE;
		}
	}


	/**
	 * 返回此 Objenesis 实例是否值得尝试用于实例创建，即它是否尚未被使用或已知可用。
	 * <p>如果已确定所配置的 Objenesis 实例化器策略在当前 JVM 上完全无法工作，或者
	 * "spring.objenesis.ignore" 属性已被设置为 "true"，则此方法返回 {@code false}。
	 */
	public boolean isWorthTrying() {
		return (this.worthTrying != Boolean.FALSE);
	}

	/**
	 * 通过 Objenesis 创建给定类的新实例。
	 * @param clazz the class to create an instance of
	 * @param useCache whether to use the instantiator cache
	 * (typically {@code true} but can be set to {@code false}
	 * e.g. for reloadable classes)
	 * @return the new instance (never {@code null})
	 * @throws ObjenesisException if instance creation failed
	 */
	public <T> T newInstance(Class<T> clazz, boolean useCache) {
		if (!useCache) {
			return newInstantiatorOf(clazz).newInstance();
		}
		return getInstantiatorOf(clazz).newInstance();
	}

	public <T> T newInstance(Class<T> clazz) {
		return getInstantiatorOf(clazz).newInstance();
	}

	@SuppressWarnings("unchecked")
	public <T> ObjectInstantiator<T> getInstantiatorOf(Class<T> clazz) {
		ObjectInstantiator<?> instantiator = this.cache.get(clazz);
		if (instantiator == null) {
			ObjectInstantiator<T> newInstantiator = newInstantiatorOf(clazz);
			instantiator = this.cache.putIfAbsent(clazz, newInstantiator);
			if (instantiator == null) {
				instantiator = newInstantiator;
			}
		}
		return (ObjectInstantiator<T>) instantiator;
	}

	protected <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> clazz) {
		Boolean currentWorthTrying = this.worthTrying;
		try {
			ObjectInstantiator<T> instantiator = this.strategy.newInstantiatorOf(clazz);
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.TRUE;
			}
			return instantiator;
		}
		catch (ObjenesisException ex) {
			if (currentWorthTrying == null) {
				Throwable cause = ex.getCause();
				if (cause instanceof ClassNotFoundException || cause instanceof IllegalAccessException) {
					// Indicates that the chosen instantiation strategy does not work on the given JVM.
					// Typically a failure to initialize the default SunReflectionFactoryInstantiator.
					// Let's assume that any subsequent attempts to use Objenesis will fail as well...
					this.worthTrying = Boolean.FALSE;
				}
			}
			throw ex;
		}
		catch (NoClassDefFoundError err) {
			// Happening on the production version of Google App Engine, coming out of the
			// restricted "sun.reflect.ReflectionFactory" class...
			if (currentWorthTrying == null) {
				this.worthTrying = Boolean.FALSE;
			}
			throw new ObjenesisException(err);
		}
	}

}
