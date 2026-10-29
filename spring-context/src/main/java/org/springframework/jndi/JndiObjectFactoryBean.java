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

package org.springframework.jndi;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.FactoryBean}，用于查找 JNDI 对象。将 JNDI 中找到的对象
 * 暴露给 Bean 引用，例如在 {@link javax.sql.DataSource} 场景下供数据访问对象的 "dataSource" 属性使
 * 用。
 *
 * <p>典型用法是在应用上下文中将其注册为单例工厂（例如针对某个绑定到 JNDI 的 DataSource），并将
 * Bean 引用提供给需要它的应用服务。
 *
 * <p>默认行为是在启动时查找 JNDI 对象并对其进行缓存。可通过 "lookupOnStartup" 和 "cache" 属性进行
 * 定制，底层使用 {@link JndiObjectTargetSource}。注意，在这种情况下需要指定 "proxyInterface"，因为
 * 实际的 JNDI 对象类型无法预先确定。
 *
 * <p>当然，Spring 环境中的 Bean 类也可以自行从 JNDI 查找例如 DataSource。本类只是让 JNDI 名称可以
 * 集中配置，并便于切换到非 JNDI 的替代方案。后者对于测试环境、在独立客户端中复用等场景尤其方便。
 *
 * <p>注意，切换到例如 DriverManagerDataSource 只是一个配置问题：只需把该 FactoryBean 的定义替换为
 * {@link org.springframework.jdbc.datasource.DriverManagerDataSource} 定义即可！
 *
 * @author Juergen Hoeller
 * @since 22.05.2003
 * @see #setProxyInterface
 * @see #setLookupOnStartup
 * @see #setCache
 * @see JndiObjectTargetSource
 */
public class JndiObjectFactoryBean extends JndiObjectLocator
		implements FactoryBean<Object>, BeanFactoryAware, BeanClassLoaderAware {

	@Nullable
	private Class<?>[] proxyInterfaces;

	private boolean lookupOnStartup = true;

	private boolean cache = true;

	private boolean exposeAccessContext = false;

	@Nullable
	private Object defaultObject;

	@Nullable
	private ConfigurableBeanFactory beanFactory;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private Object jndiObject;


	/**
	 * 指定用于 JNDI 对象的代理接口。
	 * <p>通常与 "lookupOnStartup"=false 和/或 "cache"=false 配合使用。在延迟查找的情况下需要指定该属性，
	 * 因为实际的 JNDI 对象类型无法预先确定。
	 * @see #setProxyInterfaces
	 * @see #setLookupOnStartup
	 * @see #setCache
	 */
	public void setProxyInterface(Class<?> proxyInterface) {
		this.proxyInterfaces = new Class<?>[] {proxyInterface};
	}

	/**
	 * 指定用于 JNDI 对象的多个代理接口。
	 * <p>通常与 "lookupOnStartup"=false 和/或 "cache"=false 配合使用。注意，如有必要，代理接口会从指定
	 * 的 "expectedType" 自动检测。
	 * @see #setExpectedType
	 * @see #setLookupOnStartup
	 * @see #setCache
	 */
	public void setProxyInterfaces(Class<?>... proxyInterfaces) {
		this.proxyInterfaces = proxyInterfaces;
	}

	/**
	 * 设置是否在启动时查找 JNDI 对象。默认值为 "true"。
	 * <p>可以关闭该选项，以允许 JNDI 对象稍后才可用。在这种情况下，JNDI 对象将在首次访问时获取。
	 * <p>对于延迟查找，需要指定代理接口。
	 * @see #setProxyInterface
	 * @see #setCache
	 */
	public void setLookupOnStartup(boolean lookupOnStartup) {
		this.lookupOnStartup = lookupOnStartup;
	}

	/**
	 * 设置 JNDI 对象被定位后是否对其进行缓存。默认值为 "true"。
	 * <p>可以关闭该选项，以允许 JNDI 对象热重新部署。在这种情况下，每次调用都会获取 JNDI 对象。
	 * <p>对于热重新部署，需要指定代理接口。
	 * @see #setProxyInterface
	 * @see #setLookupOnStartup
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * 设置是否对所有访问目标对象的操作（即对暴露的对象引用上的所有方法调用）暴露 JNDI 环境上下文。
	 * <p>默认值为 "false"，即只在对象查找时暴露 JNDI 上下文。将该标志切换为 "true"，以便在每次方法调用
	 * 时暴露 JNDI 环境（包括授权上下文），这是 WebLogic 对带授权要求且通过 JNDI 获取的工厂（例如 JDBC
	 * DataSource、JMS ConnectionFactory）的需要。
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}

	/**
	 * 指定在 JNDI 查找失败时回退使用的默认对象。默认没有。
	 * <p>这可以是任意的 Bean 引用或字面值。它通常用于这样的场景：JNDI 环境可能定义了特定的配置设置，但
	 * 这些设置并不要求一定存在，此时使用字面值。
	 * <p>注意：仅在启动时查找的情况下受支持。如果与 {@link #setExpectedType} 一起指定，则所指定的值必
	 * 须是该类型或可转换为该类型。
	 * @see #setLookupOnStartup
	 * @see ConfigurableBeanFactory#getTypeConverter()
	 * @see SimpleTypeConverter
	 */
	public void setDefaultObject(Object defaultObject) {
		this.defaultObject = defaultObject;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			// Just optional - for getting a specifically configured TypeConverter if needed.
			// We'll simply fall back to a SimpleTypeConverter if no specific one available.
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	/**
	 * 查找 JNDI 对象并将其存储。
	 */
	@Override
	public void afterPropertiesSet() throws IllegalArgumentException, NamingException {
		super.afterPropertiesSet();

		if (this.proxyInterfaces != null || !this.lookupOnStartup || !this.cache || this.exposeAccessContext) {
			// We need to create a proxy for this...
			if (this.defaultObject != null) {
				throw new IllegalArgumentException(
						"'defaultObject' is not supported in combination with 'proxyInterface'");
			}
			// We need a proxy and a JndiObjectTargetSource.
			this.jndiObject = JndiObjectProxyFactory.createJndiObjectProxy(this);
		}
		else {
			if (this.defaultObject != null && getExpectedType() != null &&
					!getExpectedType().isInstance(this.defaultObject)) {
				TypeConverter converter = (this.beanFactory != null ?
						this.beanFactory.getTypeConverter() : new SimpleTypeConverter());
				try {
					this.defaultObject = converter.convertIfNecessary(this.defaultObject, getExpectedType());
				}
				catch (TypeMismatchException ex) {
					throw new IllegalArgumentException("Default object [" + this.defaultObject + "] of type [" +
							this.defaultObject.getClass().getName() + "] is not of expected type [" +
							getExpectedType().getName() + "] and cannot be converted either", ex);
				}
			}
			// Locate specified JNDI object.
			this.jndiObject = lookupWithFallback();
		}
	}

	/**
	 * 查找的变体，在查找失败时返回指定的 "defaultObject"（如果有）。
	 * @return the located object, or the "defaultObject" as fallback
	 * @throws NamingException in case of lookup failure without fallback
	 * @see #setDefaultObject
	 */
	protected Object lookupWithFallback() throws NamingException {
		ClassLoader originalClassLoader = ClassUtils.overrideThreadContextClassLoader(this.beanClassLoader);
		try {
			return lookup();
		}
		catch (TypeMismatchNamingException ex) {
			// Always let TypeMismatchNamingException through -
			// we don't want to fall back to the defaultObject in this case.
			throw ex;
		}
		catch (NamingException ex) {
			if (this.defaultObject != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("JNDI lookup failed - returning specified default object instead", ex);
				}
				else if (logger.isDebugEnabled()) {
					logger.debug("JNDI lookup failed - returning specified default object instead: " + ex);
				}
				return this.defaultObject;
			}
			throw ex;
		}
		finally {
			if (originalClassLoader != null) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
	}


	/**
	 * 返回单例 JNDI 对象。
	 */
	@Override
	@Nullable
	public Object getObject() {
		return this.jndiObject;
	}

	@Override
	public Class<?> getObjectType() {
		if (this.proxyInterfaces != null) {
			if (this.proxyInterfaces.length == 1) {
				return this.proxyInterfaces[0];
			}
			else if (this.proxyInterfaces.length > 1) {
				return createCompositeInterface(this.proxyInterfaces);
			}
		}
		if (this.jndiObject != null) {
			return this.jndiObject.getClass();
		}
		else {
			return getExpectedType();
		}
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 为给定的接口创建一个组合接口 Class，在单个 Class 中实现给定的接口。
	 * <p>默认实现会为给定的接口构建一个 JDK 代理类。
	 * @param interfaces the interfaces to merge
	 * @return the merged interface as Class
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	protected Class<?> createCompositeInterface(Class<?>[] interfaces) {
		return ClassUtils.createCompositeInterface(interfaces, this.beanClassLoader);
	}


	/**
	 * 内部类，仅用于在实际创建代理时引入 AOP 依赖。
	 */
	private static class JndiObjectProxyFactory {

		private static Object createJndiObjectProxy(JndiObjectFactoryBean jof) throws NamingException {
			// Create a JndiObjectTargetSource that mirrors the JndiObjectFactoryBean's configuration.
			JndiObjectTargetSource targetSource = new JndiObjectTargetSource();
			targetSource.setJndiTemplate(jof.getJndiTemplate());
			String jndiName = jof.getJndiName();
			Assert.state(jndiName != null, "No JNDI name specified");
			targetSource.setJndiName(jndiName);
			targetSource.setExpectedType(jof.getExpectedType());
			targetSource.setResourceRef(jof.isResourceRef());
			targetSource.setLookupOnStartup(jof.lookupOnStartup);
			targetSource.setCache(jof.cache);
			targetSource.afterPropertiesSet();

			// Create a proxy with JndiObjectFactoryBean's proxy interface and the JndiObjectTargetSource.
			ProxyFactory proxyFactory = new ProxyFactory();
			if (jof.proxyInterfaces != null) {
				proxyFactory.setInterfaces(jof.proxyInterfaces);
			}
			else {
				Class<?> targetClass = targetSource.getTargetClass();
				if (targetClass == null) {
					throw new IllegalStateException(
							"Cannot deactivate 'lookupOnStartup' without specifying a 'proxyInterface' or 'expectedType'");
				}
				Class<?>[] ifcs = ClassUtils.getAllInterfacesForClass(targetClass, jof.beanClassLoader);
				for (Class<?> ifc : ifcs) {
					if (Modifier.isPublic(ifc.getModifiers())) {
						proxyFactory.addInterface(ifc);
					}
				}
			}
			if (jof.exposeAccessContext) {
				proxyFactory.addAdvice(new JndiContextExposingInterceptor(jof.getJndiTemplate()));
			}
			proxyFactory.setTargetSource(targetSource);
			return proxyFactory.getProxy(jof.beanClassLoader);
		}
	}


	/**
	 * 拦截器，按照 JndiObjectFactoryBean 的 "exposeAccessContext" 标志，为所有方法调用暴露 JNDI 上下文。
	 */
	private static class JndiContextExposingInterceptor implements MethodInterceptor {

		private final JndiTemplate jndiTemplate;

		public JndiContextExposingInterceptor(JndiTemplate jndiTemplate) {
			this.jndiTemplate = jndiTemplate;
		}

		@Override
		@Nullable
		public Object invoke(MethodInvocation invocation) throws Throwable {
			Context ctx = (isEligible(invocation.getMethod()) ? this.jndiTemplate.getContext() : null);
			try {
				return invocation.proceed();
			}
			finally {
				this.jndiTemplate.releaseContext(ctx);
			}
		}

		protected boolean isEligible(Method method) {
			return (Object.class != method.getDeclaringClass());
		}
	}

}
