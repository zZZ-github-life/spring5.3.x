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
 * 由希望在销毁时释放资源的 Bean 实现的接口。{@link BeanFactory} 会在作用域 Bean 单独销毁时调用其销
 * 毁方法。{@link org.springframework.context.ApplicationContext} 应在关闭时释放其所有单例，这由应
 * 用生命周期驱动。
 *
 * <p>Spring 管理的 Bean 也可以实现 Java 的 {@link AutoCloseable} 接口以达到相同目的。实现接口的另
 * 一种替代方案是指定自定义销毁方法，例如在 XML Bean 定义中。有关所有 Bean 生命周期方法的列表，请参
 * 见 {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Juergen Hoeller
 * @since 12.08.2003
 * @see InitializingBean
 * @see org.springframework.beans.factory.support.RootBeanDefinition#getDestroyMethodName()
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
 * @see org.springframework.context.ConfigurableApplicationContext#close()
 */
public interface DisposableBean {

	/**
	 * 在 Bean 销毁时由包含它的 {@code BeanFactory} 调用。
	 * @throws Exception in case of shutdown errors. Exceptions will get logged
	 * but not rethrown to allow other beans to release their resources as well.
	 */
	void destroy() throws Exception;

}
