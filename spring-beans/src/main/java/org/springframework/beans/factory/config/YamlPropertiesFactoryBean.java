/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Properties;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.lang.Nullable;

/**
 * 从 YAML 源读取的 {@link java.util.Properties} 工厂，暴露 String 属性值的扁平结构。
 *
 * <p>YAML 是一种良好的人类可读配置格式，并且具有一些有用的层次化特性。它或多或少是 JSON 的超集，因
 * 此具有许多相似特性。
 *
 * <p><b>注意：所有暴露的值都是 {@code String} 类型</b>，以便通过常用的
 * {@link Properties#getProperty} 方法访问（例如通过
 * {@link PropertyResourceConfigurer#setProperties(Properties)} 进行配置属性解析时）。如果不希望如
 * 此，请改用 {@link YamlMapFactoryBean}。
 *
 * <p>此工厂创建的 Properties 对层次化对象使用嵌套路径，因此例如以下 YAML
 *
 * <pre class="code">
 * environments:
 *   dev:
 *     url: https://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: https://foo.bar.com
 *     name: My Cool App
 * </pre>
 *
 * 会被转换为以下属性：
 *
 * <pre class="code">
 * environments.dev.url=https://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=https://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * 列表会使用 <code>[]</code> 解引用符拆分为属性键，例如以下 YAML：
 *
 * <pre class="code">
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * 会变成如下属性：
 *
 * <pre class="code">
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 *
 * <p>自 Spring Framework 5.0.6 起，需要 SnakeYAML 1.18 或更高版本。
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	@Nullable
	private Properties properties;


	/**
	 * 设置是创建单例，还是在每次请求时创建新对象。默认是 {@code true}（单例）。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void afterPropertiesSet() {
		if (isSingleton()) {
			this.properties = createProperties();
		}
	}

	@Override
	@Nullable
	public Properties getObject() {
		return (this.properties != null ? this.properties : createProperties());
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}


	/**
	 * 模板方法，子类可以覆盖以构造此工厂返回的对象。默认实现返回包含所有资源内容的 properties。
	 * <p>在共享单例的情况下，首次调用 {@link #getObject()} 时延迟调用；否则在每次 {@link #getObject()}
	 * 调用时调用。
	 * @return 由该工厂返回的对象
	 * @see #process(MatchCallback)
	 */
	protected Properties createProperties() {
		Properties result = CollectionFactory.createStringAdaptingProperties();
		process((properties, map) -> result.putAll(properties));
		return result;
	}

}
