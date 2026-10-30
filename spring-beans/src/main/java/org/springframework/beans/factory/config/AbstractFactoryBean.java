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

package org.springframework.beans.factory.config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link FactoryBean} 实现的简单模板超类，根据标志创建单例或原型对象。
 *
 * <p>如果 "singleton" 标志为 {@code true}（默认值），此类将只创建一次它所创建的对象，并在初始化时
 * 创建，随后在所有对 {@link #getObject()} 方法的调用中返回该单例实例。
 *
 * <p>否则，此类将在每次调用 {@link #getObject()} 方法时创建一个新实例。子类负责实现抽象的
 * {@link #createInstance()} 模板方法，以实际创建要暴露的对象。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @since 1.0.2
 * @param <T> 该 Bean 的类型
 * @see #setSingleton
 * @see #createInstance()
 */
public abstract class AbstractFactoryBean<T>
		implements FactoryBean<T>, BeanClassLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

	/** 可供子类使用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private boolean singleton = true;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private BeanFactory beanFactory;

	private boolean initialized = false;

	@Nullable
	private T singletonInstance;

	@Nullable
	private T earlySingletonInstance;


	/**
	 * 设置是应创建单例，还是每次请求时创建新对象。默认为 {@code true}（单例）。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanFactory(@Nullable BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
	 * 返回此 Bean 运行所在的 BeanFactory。
	 */
	@Nullable
	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	/**
	 * 从此外运行所在的 BeanFactory 获取 Bean 类型转换器。通常每次调用都是一个新实例，因为
	 * TypeConverter 通常<i>不是</i>线程安全的。<p>当不在 BeanFactory 中运行时，回退到
	 * SimpleTypeConverter。
	 * @see ConfigurableBeanFactory#getTypeConverter()
	 * @see org.springframework.beans.SimpleTypeConverter
	 */
	protected TypeConverter getBeanTypeConverter() {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
		}
		else {
			return new SimpleTypeConverter();
		}
	}

	/**
	 * 如有必要，提前创建单例实例。
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		if (isSingleton()) {
			this.initialized = true;
			this.singletonInstance = createInstance();
			this.earlySingletonInstance = null;
		}
	}


	/**
	 * 暴露单例实例或创建新的原型实例。
	 * @see #createInstance()
	 * @see #getEarlySingletonInterfaces()
	 */
	@Override
	public final T getObject() throws Exception {
		if (isSingleton()) {
			return (this.initialized ? this.singletonInstance : getEarlySingletonInstance());
		}
		else {
			return createInstance();
		}
	}

	/**
	 * 确定一个 'early singleton'（早期单例）实例，在出现循环引用时暴露。在非循环场景中不会被调用。
	 */
	@SuppressWarnings("unchecked")
	private T getEarlySingletonInstance() throws Exception {
		Class<?>[] ifcs = getEarlySingletonInterfaces();
		if (ifcs == null) {
			throw new FactoryBeanNotInitializedException(
					getClass().getName() + " does not support circular references");
		}
		if (this.earlySingletonInstance == null) {
			this.earlySingletonInstance = (T) Proxy.newProxyInstance(
					this.beanClassLoader, ifcs, new EarlySingletonInvocationHandler());
		}
		return this.earlySingletonInstance;
	}

	/**
	 * 暴露单例实例（以便通过 'early singleton' 代理访问）。
	 * @return 此 FactoryBean 持有的单例实例
	 * @throws IllegalStateException 如果单例实例尚未初始化则抛出
	 */
	@Nullable
	private T getSingletonInstance() throws IllegalStateException {
		Assert.state(this.initialized, "Singleton instance not initialized yet");
		return this.singletonInstance;
	}

	/**
	 * 销毁单例实例（如果有）。
	 * @see #destroyInstance(Object)
	 */
	@Override
	public void destroy() throws Exception {
		if (isSingleton()) {
			destroyInstance(this.singletonInstance);
		}
	}


	/**
	 * 此抽象方法声明镜像了 FactoryBean 接口中的方法，以提供一致的抽象模板方法。
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	@Override
	@Nullable
	public abstract Class<?> getObjectType();

	/**
	 * 子类必须重写的模板方法，用于构造此工厂返回的对象。<p>在单例的情况下，在此 FactoryBean 初始化时调
	 * 用；否则在每次 {@link #getObject()} 调用时调用。
	 * @return 此工厂返回的对象
	 * @throws Exception 如果在对象创建期间发生异常则抛出
	 * @see #getObject()
	 */
	protected abstract T createInstance() throws Exception;

	/**
	 * 返回此 FactoryBean 所暴露的单例对象应实现的接口数组，用于在出现循环引用时暴露的 'early singleton
	 * 代理'。<p>默认实现返回此 FactoryBean 的对象类型（前提是它是一个接口），否则返回 {@code null}。后
	 * 者表示此 FactoryBean 不支持早期单例访问。这将导致抛出 FactoryBeanNotInitializedException。
	 * @return 用于 'early singletons' 的接口，
	 * 或 {@code null} 表示 FactoryBeanNotInitializedException
	 * @see org.springframework.beans.factory.FactoryBeanNotInitializedException
	 */
	@Nullable
	protected Class<?>[] getEarlySingletonInterfaces() {
		Class<?> type = getObjectType();
		return (type != null && type.isInterface() ? new Class<?>[] {type} : null);
	}

	/**
	 * 用于销毁单例实例的回调。子类可以重写此方法以销毁先前创建的实例。<p>默认实现为空。
	 * @param instance 单例实例，由 {@link #createInstance()} 返回
	 * @throws Exception 在关闭出错时抛出
	 * @see #createInstance()
	 */
	protected void destroyInstance(@Nullable T instance) throws Exception {
	}


	/**
	 * 用于延迟访问实际单例对象的反射 InvocationHandler。
	 */
	private class EarlySingletonInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				// Use hashCode of reference proxy.
				return System.identityHashCode(proxy);
			}
			else if (!initialized && ReflectionUtils.isToStringMethod(method)) {
				return "Early singleton proxy for interfaces " +
						ObjectUtils.nullSafeToString(getEarlySingletonInterfaces());
			}
			try {
				return method.invoke(getSingletonInstance(), args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}

}
