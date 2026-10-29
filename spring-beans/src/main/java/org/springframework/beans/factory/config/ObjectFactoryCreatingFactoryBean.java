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

import java.io.Serializable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 一个 {@link org.springframework.beans.factory.FactoryBean} 实现，它返回的值是一个
 * {@link org.springframework.beans.factory.ObjectFactory}，后者又返回从
 * {@link org.springframework.beans.factory.BeanFactory} 获取的 Bean。
 *
 * <p>因此，可以用它来避免客户端对象直接调用
 * {@link org.springframework.beans.factory.BeanFactory#getBean(String)} 从
 * {@link org.springframework.beans.factory.BeanFactory} 获取（通常是原型）Bean，因为那样会违反控制
 * 反转原则。取而代之的是，借助此类，可以将一个
 * {@link org.springframework.beans.factory.ObjectFactory} 实例作为属性提供给客户端对象，该实例只直
 * 接返回那一个目标 Bean（同样，通常是一个原型 Bean）。
 *
 * <p>基于 XML 的 {@link org.springframework.beans.factory.BeanFactory} 中的示例配置可能如下所示：
 *
 * <pre class="code">&lt;beans&gt;
 *
 *   &lt;!-- Prototype bean since we have state --&gt;
 *   &lt;bean id="myService" class="a.b.c.MyService" scope="prototype"/&gt;
 *
 *   &lt;bean id="myServiceFactory"
 *       class="org.springframework.beans.factory.config.ObjectFactoryCreatingFactoryBean"&gt;
 *     &lt;property name="targetBeanName"&gt;&lt;idref local="myService"/&gt;&lt;/property&gt;
 *   &lt;/bean&gt;
 *
 *   &lt;bean id="clientBean" class="a.b.c.MyClientBean"&gt;
 *     &lt;property name="myServiceFactory" ref="myServiceFactory"/&gt;
 *   &lt;/bean&gt;
 *
 *&lt;/beans&gt;</pre>
 *
 * <p>相应的 {@code MyClientBean} 类实现可能类似于以下内容：
 *
 * <pre class="code">package a.b.c;
 *
 * import org.springframework.beans.factory.ObjectFactory;
 *
 * public class MyClientBean {
 *
 *   private ObjectFactory&lt;MyService&gt; myServiceFactory;
 *
 *   public void setMyServiceFactory(ObjectFactory&lt;MyService&gt; myServiceFactory) {
 *     this.myServiceFactory = myServiceFactory;
 *   }
 *
 *   public void someBusinessMethod() {
 *     // get a 'fresh', brand new MyService instance
 *     MyService service = this.myServiceFactory.getObject();
 *     // use the service object to effect the business logic...
 *   }
 * }</pre>
 *
 * <p>这种对象创建模式应用的另一种替代方案是使用 {@link ServiceLocatorFactoryBean} 来获取（原型）
 * Bean。{@link ServiceLocatorFactoryBean} 方式的优点是不必依赖任何 Spring 特有的接口（例如
 * {@link org.springframework.beans.factory.ObjectFactory}），但缺点是需要进行运行时类生成。关于此
 * 问题的更完整讨论，请参阅 {@link ServiceLocatorFactoryBean ServiceLocatorFactoryBean JavaDoc}。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.beans.factory.ObjectFactory
 * @see ServiceLocatorFactoryBean
 */
public class ObjectFactoryCreatingFactoryBean extends AbstractFactoryBean<ObjectFactory<Object>> {

	@Nullable
	private String targetBeanName;


	/**
	 * 设置目标 Bean 的名称。
	 * <p>目标<i>不必</i>是非单例 Bean，但实际上总是如此（因为如果目标 Bean 是单例，那么就可以将该单例
	 * Bean 直接注入依赖对象，从而无需这种工厂方式提供的额外间接层）。
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
		super.afterPropertiesSet();
	}


	@Override
	public Class<?> getObjectType() {
		return ObjectFactory.class;
	}

	@Override
	protected ObjectFactory<Object> createInstance() {
		BeanFactory beanFactory = getBeanFactory();
		Assert.state(beanFactory != null, "No BeanFactory available");
		Assert.state(this.targetBeanName != null, "No target bean name specified");
		return new TargetBeanObjectFactory(beanFactory, this.targetBeanName);
	}


	/**
	 * 独立的内部类，用于序列化目的。
	 */
	@SuppressWarnings("serial")
	private static class TargetBeanObjectFactory implements ObjectFactory<Object>, Serializable {

		private final BeanFactory beanFactory;

		private final String targetBeanName;

		public TargetBeanObjectFactory(BeanFactory beanFactory, String targetBeanName) {
			this.beanFactory = beanFactory;
			this.targetBeanName = targetBeanName;
		}

		@Override
		public Object getObject() throws BeansException {
			return this.beanFactory.getBean(this.targetBeanName);
		}
	}

}
