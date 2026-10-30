/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.Aware;

/**
 * 由任何希望获知其运行所在 {@link ApplicationContext} 的对象实现的接口。
 *
 * <p>例如，当对象需要访问一组协作 Bean 时，实现此接口是合理的。请注意，通过 Bean 引用进行配置比仅
 * 仅为了查找 Bean 而实现此接口更可取。
 *
 * <p>如果对象需要访问文件资源（即想要调用 {@code getResource}）、希望发布应用事件，或需要访问
 * MessageSource，也可以实现此接口。不过，在这种特定场景下，更可取的做法是实现更具体的
 * {@link ResourceLoaderAware}、{@link ApplicationEventPublisherAware} 或
 * {@link MessageSourceAware} 接口。
 *
 * <p>请注意，文件资源依赖也可以暴露为 {@link org.springframework.core.io.Resource} 类型的 Bean 属
 * 性，由 Bean 工厂通过自动类型转换从 String 填充。这样就无需仅仅为了访问特定文件资源而实现任何回调
 * 接口。
 *
 * <p>{@link org.springframework.context.support.ApplicationObjectSupport} 是面向应用对象的便捷基类，
 * 它实现了此接口。
 *
 * <p>有关所有 Bean 生命周期方法的列表，请参见
 * {@link org.springframework.beans.factory.BeanFactory BeanFactory javadocs}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see ResourceLoaderAware
 * @see ApplicationEventPublisherAware
 * @see MessageSourceAware
 * @see org.springframework.context.support.ApplicationObjectSupport
 * @see org.springframework.beans.factory.BeanFactoryAware
 */
public interface ApplicationContextAware extends Aware {

	/**
	 * 设置此对象运行所在的 ApplicationContext。
	 * 通常此调用将用于初始化该对象。
	 *
	 * <p>在填充常规 Bean 属性之后、但在诸如
	 * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()} 之类的初始化回调
	 * 或自定义 init-method 之前调用。如果适用，则在 {@link ResourceLoaderAware#setResourceLoader}、
	 * {@link ApplicationEventPublisherAware#setApplicationEventPublisher} 和
	 * {@link MessageSourceAware} 之后调用。
	 * @param applicationContext 此对象要使用的 ApplicationContext 对象
	 * @throws ApplicationContextException 如果发生上下文初始化错误
	 * @throws BeansException 如果由应用上下文方法抛出
	 * @see org.springframework.beans.factory.BeanInitializationException
	 */
	void setApplicationContext(ApplicationContext applicationContext) throws BeansException;

}
