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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean}，返回静态或实例方法调用的结果值。在大多数场景下，针对同一目的最好直接使用容
 * 器内置的工厂方法支持，因为它在参数转换方面更智能。不过，当需要调用不返回任何值的方法时（例如为了
 * 触发某种初始化而调用的静态类方法），此工厂 Bean 仍然很有用。工厂方法不支持这一场景，因为获取
 * Bean 实例需要返回值。
 *
 * <p>请注意，由于预计主要用于访问工厂方法，此工厂默认以<b>单例</b>方式运行。拥有它的 Bean 工厂第一
 * 次请求 {@link #getObject} 时会触发一次方法调用，其返回值将被缓存以供后续请求使用。可以将内部的
 * {@link #setSingleton singleton} 属性设置为 "false"，使此工厂在每次被请求对象时都调用目标方法。
 *
 * <p><b>NOTE: 如果目标方法不产生可供暴露的结果，请考虑改用 {@link MethodInvokingBean}，它避免了
 * {@link MethodInvokingFactoryBean} 所带来的类型确定和生命周期方面的限制。</b>
 *
 * <p>此调用器支持任意类型的目标方法。可以通过将 {@link #setTargetMethod targetMethod} 属性设置为表
 * 示静态方法名称的 String 来指定静态方法，并通过 {@link #setTargetClass targetClass} 指定定义该静
 * 态方法的 Class。或者，也可以指定目标实例方法：将 {@link #setTargetObject targetObject} 属性设置
 * 为目标对象，并将 {@link #setTargetMethod targetMethod} 属性设置为要在该目标对象上调用的方法名称。
 * 方法的调用参数可以通过设置 {@link #setArguments arguments} 属性来指定。
 *
 * <p>按照 InitializingBean 的约定，此类依赖于在所有属性设置完成后调用一次
 * {@link #afterPropertiesSet()}。
 *
 * <p>以下示例（在基于 XML 的 Bean 工厂定义中）展示了一个使用此类调用静态工厂方法的 Bean 定义：
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="staticMethod" value="com.whatever.MyClassFactory.getInstance"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>以下示例先调用静态方法，再调用实例方法，以获取某个 Java 系统属性。略显冗长，但确实有效。
 *
 * <pre class="code">
 * &lt;bean id="sysProps" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="targetClass" value="java.lang.System"/&gt;
 *   &lt;property name="targetMethod" value="getProperties"/&gt;
 * &lt;/bean&gt;
 *
 * &lt;bean id="javaVersion" class="org.springframework.beans.factory.config.MethodInvokingFactoryBean"&gt;
 *   &lt;property name="targetObject" ref="sysProps"/&gt;
 *   &lt;property name="targetMethod" value="getProperty"/&gt;
 *   &lt;property name="arguments" value="java.version"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 21.11.2003
 * @see MethodInvokingBean
 * @see org.springframework.util.MethodInvoker
 */
public class MethodInvokingFactoryBean extends MethodInvokingBean implements FactoryBean<Object> {

	private boolean singleton = true;

	private boolean initialized = false;

	/** 单例情况下的方法调用结果。 */
	@Nullable
	private Object singletonObject;


	/**
	 * 设置是创建单例，还是在每次 {@link #getObject()} 请求时创建新对象。默认为 "true"。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		prepare();
		if (this.singleton) {
			this.initialized = true;
			this.singletonObject = invokeWithTargetException();
		}
	}


	/**
	 * 如果 singleton 属性设置为 "true"，则每次都返回相同的值；否则返回即时调用所指定方法得到的值。
	 */
	@Override
	@Nullable
	public Object getObject() throws Exception {
		if (this.singleton) {
			if (!this.initialized) {
				throw new FactoryBeanNotInitializedException();
			}
			// Singleton: return shared object.
			return this.singletonObject;
		}
		else {
			// Prototype: new object on each call.
			return invokeWithTargetException();
		}
	}

	/**
	 * 返回此 FactoryBean 所创建对象的类型，如果无法预先确定则返回 {@code null}。
	 */
	@Override
	public Class<?> getObjectType() {
		if (!isPrepared()) {
			// Not fully initialized yet -> return null to indicate "not known yet".
			return null;
		}
		return getPreparedMethod().getReturnType();
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

}
