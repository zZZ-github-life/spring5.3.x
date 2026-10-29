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

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.support.ArgumentConvertingMethodInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 简单的方法调用器 Bean：仅调用目标方法，不期望向容器暴露结果（与
 * {@link MethodInvokingFactoryBean} 相反）。
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
 * <p>以下示例（在基于 XML 的 Bean 工厂定义中）展示了一个使用此类调用静态初始化方法的 Bean 定义：
 *
 * <pre class="code">
 * &lt;bean id="myObject" class="org.springframework.beans.factory.config.MethodInvokingBean"&gt;
 *   &lt;property name="staticMethod" value="com.whatever.MyClass.init"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>以下示例调用实例方法来启动某个服务器 Bean：
 *
 * <pre class="code">
 * &lt;bean id="myStarter" class="org.springframework.beans.factory.config.MethodInvokingBean"&gt;
 *   &lt;property name="targetObject" ref="myServer"/&gt;
 *   &lt;property name="targetMethod" value="start"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 4.0.3
 * @see MethodInvokingFactoryBean
 * @see org.springframework.util.MethodInvoker
 */
public class MethodInvokingBean extends ArgumentConvertingMethodInvoker
		implements BeanClassLoaderAware, BeanFactoryAware, InitializingBean {

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	@Nullable
	private ConfigurableBeanFactory beanFactory;


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, this.beanClassLoader);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.beanFactory = (ConfigurableBeanFactory) beanFactory;
		}
	}

	/**
	 * 如果可能，从运行此 Bean 的 BeanFactory 获取 TypeConverter。
	 * @see ConfigurableBeanFactory#getTypeConverter()
	 */
	@Override
	protected TypeConverter getDefaultTypeConverter() {
		if (this.beanFactory != null) {
			return this.beanFactory.getTypeConverter();
		}
		else {
			return super.getDefaultTypeConverter();
		}
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		prepare();
		invokeWithTargetException();
	}

	/**
	 * 执行调用，并将 InvocationTargetException 转换为底层的目标异常。
	 */
	@Nullable
	protected Object invokeWithTargetException() throws Exception {
		try {
			return invoke();
		}
		catch (InvocationTargetException ex) {
			if (ex.getTargetException() instanceof Exception) {
				throw (Exception) ex.getTargetException();
			}
			if (ex.getTargetException() instanceof Error) {
				throw (Error) ex.getTargetException();
			}
			throw ex;
		}
	}

}
