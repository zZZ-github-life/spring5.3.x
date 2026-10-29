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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * 从 YAML 源读取的 {@code Map} 工厂，保留 YAML 中声明的值类型及其结构。
 *
 * <p>YAML 是一种良好的人类可读配置格式，并且具有一些有用的层次化特性。它或多或少是 JSON 的超集，因
 * 此具有许多相似特性。
 *
 * <p>如果提供了多个资源，则后面的资源会按层次覆盖前面资源中的条目；也就是说，任意深度上具有相同嵌
 * 套键（类型为 {@code Map}）的所有条目都会合并。例如：
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: two
 * three: four
 * </pre>
 *
 * 加上（列表中靠后的）
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * five: six
 * </pre>
 *
 * 得到的有效输入为
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * three: four
 * five: six
 * </pre>
 *
 * 请注意，第一份文档中 "foo" 的值不会被第二份文档中的值简单替换，而是其嵌套值会合并。
 *
 * <p>自 Spring Framework 5.0.6 起，需要 SnakeYAML 1.18 或更高版本。
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @since 4.1
 */
public class YamlMapFactoryBean extends YamlProcessor implements FactoryBean<Map<String, Object>>, InitializingBean {

	private boolean singleton = true;

	@Nullable
	private Map<String, Object> map;


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
			this.map = createMap();
		}
	}

	@Override
	@Nullable
	public Map<String, Object> getObject() {
		return (this.map != null ? this.map : createMap());
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}


	/**
	 * 模板方法，子类可以覆盖以构造此工厂返回的对象。
	 * <p>在共享单例的情况下，首次调用 {@link #getObject()} 时延迟调用；否则在每次 {@link #getObject()}
	 * 调用时调用。
	 * <p>默认实现返回合并后的 {@code Map} 实例。
	 * @return the object returned by this factory
	 * @see #process(MatchCallback)
	 */
	protected Map<String, Object> createMap() {
		Map<String, Object> result = new LinkedHashMap<>();
		process((properties, map) -> merge(result, map));
		return result;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void merge(Map<String, Object> output, Map<String, Object> map) {
		map.forEach((key, value) -> {
			Object existing = output.get(key);
			if (value instanceof Map && existing instanceof Map) {
				// Inner cast required by Eclipse IDE.
				Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) existing);
				merge(result, (Map) value);
				output.put(key, result);
			}
			else {
				output.put(key, value);
			}
		});
	}

}
