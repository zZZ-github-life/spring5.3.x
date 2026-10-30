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

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.lang.Nullable;

/**
 * 允许将类路径位置中的属性文件作为 Properties 实例提供给 Bean 工厂。可用于通过 Bean 引用填充任何类
 * 型为 Properties 的 Bean 属性。
 *
 * <p>支持从属性文件加载和/或在此 FactoryBean 上设置本地属性。创建的 Properties 实例将由加载的值和
 * 本地值合并而成。如果既未设置位置也未设置本地属性，则在初始化时将抛出异常。
 *
 * <p>可以创建单例，或在每次请求时创建新对象。默认为单例。
 *
 * @author Juergen Hoeller
 * @see #setLocation
 * @see #setProperties
 * @see #setLocalOverride
 * @see java.util.Properties
 */
public class PropertiesFactoryBean extends PropertiesLoaderSupport
		implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	@Nullable
	private Properties singletonInstance;


	/**
	 * 设置是创建共享的“单例” Properties 实例，还是在每次请求时创建新的 Properties 实例。
	 * <p>默认为 "true"（共享单例）。
	 */
	public final void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public final boolean isSingleton() {
		return this.singleton;
	}


	@Override
	public final void afterPropertiesSet() throws IOException {
		if (this.singleton) {
			this.singletonInstance = createProperties();
		}
	}

	@Override
	@Nullable
	public final Properties getObject() throws IOException {
		if (this.singleton) {
			return this.singletonInstance;
		}
		else {
			return createProperties();
		}
	}

	@Override
	public Class<Properties> getObjectType() {
		return Properties.class;
	}


	/**
	 * 模板方法，子类可以重写它以构造此工厂返回的对象。默认实现返回普通的合并后的 Properties 实例。
	 * <p>在共享单例的情况下，此方法在此 FactoryBean 初始化时调用；否则在每次 {@link #getObject()} 调用
	 * 时调用。
	 * @return 此工厂返回的对象
	 * @throws IOException 如果在加载属性期间发生异常则抛出
	 * @see #mergeProperties()
	 */
	protected Properties createProperties() throws IOException {
		return mergeProperties();
	}

}
