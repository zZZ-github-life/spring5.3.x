/*
 * Copyright 2002-2017 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * 一个 {@link FactoryBean} 实现，接受一个接口，该接口必须有一个或多个签名如 {@code MyType xxx()}
 * 或 {@code MyType xxx(MyIdType id)} 的方法（通常是 {@code MyService getService()} 或
 * {@code MyService getService(String id)}），并创建一个实现该接口的动态代理，将调用委托给底层的
 * {@link org.springframework.beans.factory.BeanFactory}。
 *
 * <p>此类服务定位器通过使用合适的自定义定位器接口，使调用代码与
 * {@link org.springframework.beans.factory.BeanFactory} API 解耦。它们通常用于<b>原型 Bean</b>，即
 * 用于每次调用都应返回新实例的工厂方法。客户端通过 setter 或构造器注入获得对服务定位器的引用，从而
 * 能够按需调用定位器的工厂方法。<b>对于单例 Bean，直接对目标 Bean 进行 setter 或构造器注入更为可取。
 * </b>
 *
 * <p>在调用无参工厂方法，或调用 String 类型的 id 为 {@code null} 或空字符串的单参工厂方法时，如果
 * 工厂中恰好有<b>一个</b> Bean 与工厂方法的返回类型匹配，则返回该 Bean，否则抛出
 * {@link org.springframework.beans.factory.NoSuchBeanDefinitionException}。
 *
 * <p>在使用非 null（且非空）参数调用单参工厂方法时，代理会返回
 * {@link org.springframework.beans.factory.BeanFactory#getBean(String)} 调用的结果，并将传入 id 的
 * 字符串化版本用作 Bean 名称。
 *
 * <p>工厂方法的参数通常是 String，但也可以是 int 或自定义枚举类型，例如通过 {@code toString} 字符
 * 串化。只要在 Bean 工厂中定义了相应的 Bean，生成的 String 就可以直接用作 Bean 名称。或者，也可以
 * 定义服务 ID 与 Bean 名称之间的
 * {@linkplain #setServiceMappings(java.util.Properties) a custom mapping}。
 *
 * <p>作为一个示例，考虑以下服务定位器接口。请注意，此接口不依赖于任何 Spring API。
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService();
 *}</pre>
 *
 * <p>基于 XML 的 {@link org.springframework.beans.factory.BeanFactory} 中的示例配置可能如下所示：
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype bean since we have state --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" singleton="false"/&gt;
 *
 *   &lt;!-- will lookup the above 'myService' bean by *TYPE* --&gt;
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean"&gt;
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>相应的 {@code MyClientBean} 类实现可能类似如下：
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the Spring container
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService();
 *        // use the service object to effect the business logic...
 *    }
 *}</pre>
 *
 * <p>作为按<b>名称</b>查找 Bean 的示例，考虑以下服务定位器接口。同样请注意，此接口不依赖于任何
 * Spring API。
 *
 * <pre class="code">package a.b.c;
 *
 *public interface ServiceFactory {
 *
 *    public MyService getService (String serviceName);
 *}</pre>
 *
 * <p>基于 XML 的 {@link org.springframework.beans.factory.BeanFactory} 中的示例配置可能如下所示：
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype beans since we have state (both extend MyService) --&gt;
 *   &lt;bean id="specialService" class="a.b.c.SpecialService" singleton="false"/&gt;
 *   &lt;bean id="anotherService" class="a.b.c.AnotherService" singleton="false"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *            class="org.springframework.beans.factory.config.ServiceLocatorFactoryBean"&gt;
 *     &lt;property name="serviceLocatorInterface" value="a.b.c.ServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>相应的 {@code MyClientBean} 类实现可能类似如下：
 *
 * <pre class="code">package a.b.c;
 *
 *public class MyClientBean {
 *
 *    private ServiceFactory myServiceFactory;
 *
 *    // actual implementation provided by the Spring container
 *    public void setServiceFactory(ServiceFactory myServiceFactory) {
 *        this.myServiceFactory = myServiceFactory;
 *    }
 *
 *    public void someBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("specialService");
 *        // use the service object to effect the business logic...
 *    }
 *
 *    public void anotherBusinessMethod() {
 *        // get a 'fresh', brand new MyService instance
 *        MyService service = this.myServiceFactory.getService("anotherService");
 *        // use the service object to effect the business logic...
 *    }
 *}</pre>
 *
 * <p>有关替代方法，请参见 {@link ObjectFactoryCreatingFactoryBean}。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.1.4
 * @see #setServiceLocatorInterface
 * @see #setServiceMappings
 * @see ObjectFactoryCreatingFactoryBean
 */
public class ServiceLocatorFactoryBean implements FactoryBean<Object>, BeanFactoryAware, InitializingBean {

	@Nullable
	private Class<?> serviceLocatorInterface;

	@Nullable
	private Constructor<Exception> serviceLocatorExceptionConstructor;

	@Nullable
	private Properties serviceMappings;

	@Nullable
	private ListableBeanFactory beanFactory;

	@Nullable
	private Object proxy;


	/**
	 * 设置要使用的服务定位器接口，该接口必须有一个或多个签名如 {@code MyType xxx()} 或
	 * {@code MyType xxx(MyIdType id)} 的方法（通常是 {@code MyService getService()} 或
	 * {@code MyService getService(String id)}）。有关此类方法语义的信息，请参见
	 * {@link ServiceLocatorFactoryBean class-level Javadoc}。
	 */
	public void setServiceLocatorInterface(Class<?> interfaceType) {
		this.serviceLocatorInterface = interfaceType;
	}

	/**
	 * 设置服务查找失败时服务定位器应抛出的异常类。指定的异常类必须具有以下参数类型之一的构造器：
	 * {@code (String, Throwable)} 或 {@code (Throwable)} 或 {@code (String)}。
	 * <p>如果未指定，将抛出 Spring 的 BeansException 的子类，例如 NoSuchBeanDefinitionException。由于
	 * 这些是非受检异常，调用方无需处理它们，因此只要以通用方式处理，抛出 Spring 异常是可以接受的。
	 * @see #determineServiceLocatorExceptionConstructor
	 * @see #createServiceLocatorException
	 */
	public void setServiceLocatorExceptionClass(Class<? extends Exception> serviceLocatorExceptionClass) {
		this.serviceLocatorExceptionConstructor =
				determineServiceLocatorExceptionConstructor(serviceLocatorExceptionClass);
	}

	/**
	 * 设置服务 id（传入服务定位器）与 Bean 名称（在 Bean 工厂中）之间的映射。此处未定义的服务 id 将直
	 * 接作为 Bean 名称处理。
	 * <p>空字符串作为服务 id 键时，定义了 {@code null} 和空字符串的映射，以及无参数工厂方法的映射。如
	 * 果未定义，则将从 Bean 工厂中检索单个匹配的 Bean。
	 * @param serviceMappings mappings between service ids and bean names,
	 * with service ids as keys as bean names as values
	 */
	public void setServiceMappings(Properties serviceMappings) {
		this.serviceMappings = serviceMappings;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ListableBeanFactory)) {
			throw new FatalBeanException(
					"ServiceLocatorFactoryBean needs to run in a BeanFactory that is a ListableBeanFactory");
		}
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.serviceLocatorInterface == null) {
			throw new IllegalArgumentException("Property 'serviceLocatorInterface' is required");
		}

		// Create service locator proxy.
		this.proxy = Proxy.newProxyInstance(
				this.serviceLocatorInterface.getClassLoader(),
				new Class<?>[] {this.serviceLocatorInterface},
				new ServiceLocatorInvocationHandler());
	}


	/**
	 * 确定用于给定服务定位器异常类的构造器。仅在自定义服务定位器异常的情况下调用。
	 * <p>默认实现会查找具有以下参数类型之一的构造器：{@code (String, Throwable)} 或
	 * {@code (Throwable)} 或 {@code (String)}。
	 * @param exceptionClass the exception class
	 * @return the constructor to use
	 * @see #setServiceLocatorExceptionClass
	 */
	@SuppressWarnings("unchecked")
	protected Constructor<Exception> determineServiceLocatorExceptionConstructor(Class<? extends Exception> exceptionClass) {
		try {
			return (Constructor<Exception>) exceptionClass.getConstructor(String.class, Throwable.class);
		}
		catch (NoSuchMethodException ex) {
			try {
				return (Constructor<Exception>) exceptionClass.getConstructor(Throwable.class);
			}
			catch (NoSuchMethodException ex2) {
				try {
					return (Constructor<Exception>) exceptionClass.getConstructor(String.class);
				}
				catch (NoSuchMethodException ex3) {
					throw new IllegalArgumentException(
							"Service locator exception [" + exceptionClass.getName() +
							"] neither has a (String, Throwable) constructor nor a (String) constructor");
				}
			}
		}
	}

	/**
	 * 为给定原因创建服务定位器异常。仅在自定义服务定位器异常的情况下调用。
	 * <p>默认实现可以处理消息和异常参数的所有变体。
	 * @param exceptionConstructor the constructor to use
	 * @param cause the cause of the service lookup failure
	 * @return the service locator exception to throw
	 * @see #setServiceLocatorExceptionClass
	 */
	protected Exception createServiceLocatorException(Constructor<Exception> exceptionConstructor, BeansException cause) {
		Class<?>[] paramTypes = exceptionConstructor.getParameterTypes();
		Object[] args = new Object[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			if (String.class == paramTypes[i]) {
				args[i] = cause.getMessage();
			}
			else if (paramTypes[i].isInstance(cause)) {
				args[i] = cause;
			}
		}
		return BeanUtils.instantiateClass(exceptionConstructor, args);
	}


	@Override
	@Nullable
	public Object getObject() {
		return this.proxy;
	}

	@Override
	public Class<?> getObjectType() {
		return this.serviceLocatorInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


	/**
	 * 将服务定位器调用委托给 Bean 工厂的调用处理器。
	 */
	private class ServiceLocatorInvocationHandler implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (ReflectionUtils.isEqualsMethod(method)) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			}
			else if (ReflectionUtils.isHashCodeMethod(method)) {
				// Use hashCode of service locator proxy.
				return System.identityHashCode(proxy);
			}
			else if (ReflectionUtils.isToStringMethod(method)) {
				return "Service locator: " + serviceLocatorInterface;
			}
			else {
				return invokeServiceLocatorMethod(method, args);
			}
		}

		private Object invokeServiceLocatorMethod(Method method, Object[] args) throws Exception {
			Class<?> serviceLocatorMethodReturnType = getServiceLocatorMethodReturnType(method);
			try {
				String beanName = tryGetBeanName(args);
				Assert.state(beanFactory != null, "No BeanFactory available");
				if (StringUtils.hasLength(beanName)) {
					// Service locator for a specific bean name
					return beanFactory.getBean(beanName, serviceLocatorMethodReturnType);
				}
				else {
					// Service locator for a bean type
					return beanFactory.getBean(serviceLocatorMethodReturnType);
				}
			}
			catch (BeansException ex) {
				if (serviceLocatorExceptionConstructor != null) {
					throw createServiceLocatorException(serviceLocatorExceptionConstructor, ex);
				}
				throw ex;
			}
		}

		/**
		 * 检查是否传入了服务 id。
		 */
		private String tryGetBeanName(@Nullable Object[] args) {
			String beanName = "";
			if (args != null && args.length == 1 && args[0] != null) {
				beanName = args[0].toString();
			}
			// Look for explicit serviceId-to-beanName mappings.
			if (serviceMappings != null) {
				String mappedName = serviceMappings.getProperty(beanName);
				if (mappedName != null) {
					beanName = mappedName;
				}
			}
			return beanName;
		}

		private Class<?> getServiceLocatorMethodReturnType(Method method) throws NoSuchMethodException {
			Assert.state(serviceLocatorInterface != null, "No service locator interface specified");
			Class<?>[] paramTypes = method.getParameterTypes();
			Method interfaceMethod = serviceLocatorInterface.getMethod(method.getName(), paramTypes);
			Class<?> serviceLocatorReturnType = interfaceMethod.getReturnType();

			// Check whether the method is a valid service locator.
			if (paramTypes.length > 1 || void.class == serviceLocatorReturnType) {
				throw new UnsupportedOperationException(
						"May only call methods with signature '<type> xxx()' or '<type> xxx(<idtype> id)' " +
						"on factory interface, but tried to call: " + interfaceMethod);
			}
			return serviceLocatorReturnType;
		}
	}

}
