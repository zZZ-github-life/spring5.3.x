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

package org.springframework.aop.framework;

import java.io.Closeable;

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * 代理处理器的基类，提供通用功能，特别是 ClassLoader 管理和 {@link #evaluateProxyInterfaces} 算法。
 *
 * @author Juergen Hoeller
 * @since 4.1
 * @see AbstractAdvisingBeanPostProcessor
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
 */
@SuppressWarnings("serial")
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {

	/**
	 * 这应在所有其他处理器之后运行，这样它就可以只向现有代理添加通知器，而不会造成双重代理。
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	@Nullable
	private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

	private boolean classLoaderConfigured = false;


	/**
	 * 设置适用于此处理器对 {@link Ordered} 实现的排序，在应用多个处理器时使用。
	 * <p>默认值为 {@code Ordered.LOWEST_PRECEDENCE}，表示无序。
	 * @param order 排序值
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * 设置用于生成代理类的 ClassLoader。
	 * <p>默认是 Bean 的 ClassLoader，即包含它的 {@link org.springframework.beans.factory.BeanFactory}
	 * 用于加载所有 Bean 类的 ClassLoader。可以在此为特定代理覆盖此设置。
	 */
	public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
		this.proxyClassLoader = classLoader;
		this.classLoaderConfigured = (classLoader != null);
	}

	/**
	 * 返回为此处理器配置的代理 ClassLoader。
	 */
	@Nullable
	protected ClassLoader getProxyClassLoader() {
		return this.proxyClassLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (!this.classLoaderConfigured) {
			this.proxyClassLoader = classLoader;
		}
	}


	/**
	 * 检查给定 Bean 类上的接口，并在适当的情况下将它们应用到 {@link ProxyFactory}。
	 * <p>调用 {@link #isConfigurationCallbackInterface} 和 {@link #isInternalLanguageInterface} 来筛选
	 * 合理的代理接口，否则回退到目标类代理。
	 * @param beanClass Bean 的类
	 * @param proxyFactory 该 Bean 的 ProxyFactory
	 */
	protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
		Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
		boolean hasReasonableProxyInterface = false;
		for (Class<?> ifc : targetInterfaces) {
			if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) &&
					ifc.getMethods().length > 0) {
				hasReasonableProxyInterface = true;
				break;
			}
		}
		if (hasReasonableProxyInterface) {
			// Must allow for introductions; can't just set interfaces to the target's interfaces only.
			for (Class<?> ifc : targetInterfaces) {
				proxyFactory.addInterface(ifc);
			}
		}
		else {
			proxyFactory.setProxyTargetClass(true);
		}
	}

	/**
	 * 判断给定接口是否只是容器回调，因而不能被视为合理的代理接口。
	 * <p>如果对给定 Bean 未找到合理的代理接口，则会使用其完整目标类进行代理，并假定这是用户的意图。
	 * @param ifc 要检查的接口
	 * @return 给定接口是否只是一个容器回调
	 */
	protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
		return (InitializingBean.class == ifc || DisposableBean.class == ifc || Closeable.class == ifc ||
				AutoCloseable.class == ifc || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
	}

	/**
	 * 判断给定接口是否是众所周知的内部语言接口，因而不能被视为合理的代理接口。
	 * <p>如果对给定 Bean 未找到合理的代理接口，则会使用其完整目标类进行代理，并假定这是用户的意图。
	 * @param ifc 要检查的接口
	 * @return 给定接口是否是内部语言接口
	 */
	protected boolean isInternalLanguageInterface(Class<?> ifc) {
		return (ifc.getName().equals("groovy.lang.GroovyObject") ||
				ifc.getName().endsWith(".cglib.proxy.Factory") ||
				ifc.getName().endsWith(".bytebuddy.MockAccess"));
	}

}
