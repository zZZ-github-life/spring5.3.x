/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping} 接口的实现，
 * 将 URL 映射到请求处理器 bean。同时支持映射到 bean
 * 实例和映射到 bean 名称；对于非单例处理器，后者是必需的。
 *
 * <p>"urlMap" 属性适合用于为处理器映射填充
 * bean 引用，例如通过 XML bean 定义中的 map 元素。
 *
 * <p>可以通过 "mappings" 属性设置到 bean 名称的映射，其形式
 * 为 {@code java.util.Properties} 类所接受的格式，如下所示：
 *
 * <pre class="code">
 * /welcome.html=ticketController
 * /show.html=ticketController</pre>
 *
 * <p>语法为 {@code PATH=HANDLER_BEAN_NAME}。如果路径不以
 * 斜杠开头，则会添加一个斜杠。
 *
 * <p>支持直接匹配（给定 "/test" -&gt; 已注册的 "/test"）和 "*"
 * 模式匹配（给定 "/test" -&gt; 已注册的 "/t*"）。请注意，默认情况下
 * 会在当前 Servlet 映射内进行映射（如果适用）；
 * 详情请参阅 {@link #setAlwaysUseFullPath "alwaysUseFullPath"} 属性。
 * 有关模式选项的详情，请参阅 {@link org.springframework.util.AntPathMatcher} 的 javadoc。

 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see #setMappings
 * @see #setUrlMap
 * @see BeanNameUrlHandlerMapping
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

	private final Map<String, Object> urlMap = new LinkedHashMap<>();


	/**
	 * 使用默认设置创建 {@code SimpleUrlHandlerMapping}。
	 */
	public SimpleUrlHandlerMapping() {
	}

	/**
	 * 使用所提供的 URL 映射创建 {@code SimpleUrlHandlerMapping}。
	 * @param urlMap map with URL paths as keys and handler beans (or handler
	 * bean names) as values
	 * @since 5.2
	 * @see #setUrlMap(Map)
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap) {
		setUrlMap(urlMap);
	}

	/**
	 * 使用所提供的 URL 映射和顺序创建 {@code SimpleUrlHandlerMapping}。
	 * @param urlMap map with URL paths as keys and handler beans (or handler
	 * bean names) as values
	 * @param order the order value for this {@code SimpleUrlHandlerMapping}
	 * @since 5.2
	 * @see #setUrlMap(Map)
	 * @see #setOrder(int)
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap, int order) {
		setUrlMap(urlMap);
		setOrder(order);
	}


	/**
	 * 将 URL 路径映射到处理器 bean 名称。
	 * 这是配置此 HandlerMapping 的典型方式。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。有关语法
	 * 详情，请参阅 {@link org.springframework.util.AntPathMatcher} 的 javadoc。
	 * @param mappings properties with URLs as keys and bean names as values
	 * @see #setUrlMap
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 * 设置一个映射，以 URL 路径为键，以处理器 bean（或处理器 bean 名称）
	 * 为值。便于为 bean 引用填充。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。有关语法
	 * 详情，请参阅 {@link org.springframework.util.AntPathMatcher} 的 javadoc。
	 * @param urlMap map with URLs as keys and beans as values
	 * @see #setMappings
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 * 允许对 URL 路径映射进行 {@code Map} 访问，并可以选择添加或
	 * 覆盖特定条目。
	 * <p>对于直接指定条目很有用，例如通过 "urlMap[myKey]"。
	 * 这对于添加或覆盖子 bean 定义中的条目
	 * 尤其有用。
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * 除父类的初始化之外，还会调用 {@link #registerHandlers} 方法。
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * 为对应路径注册 URL 映射中指定的所有处理器。
	 * @param urlMap a Map with URL paths as keys and handler beans or bean names as values
	 * @throws BeansException if a handler couldn't be registered
	 * @throws IllegalStateException if there is a conflicting handler registered
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.trace("No patterns in " + formatMappingName());
		}
		else {
			urlMap.forEach((url, handler) -> {
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				// Remove whitespace from handler bean name.
				if (handler instanceof String) {
					handler = ((String) handler).trim();
				}
				registerHandler(url, handler);
			});
			logMappings();
		}
	}

	private void logMappings() {
		if (mappingsLogger.isDebugEnabled()) {
			Map<String, Object> map = new LinkedHashMap<>(getHandlerMap());
			if (getRootHandler() != null) {
				map.put("/", getRootHandler());
			}
			if (getDefaultHandler() != null) {
				map.put("/**", getDefaultHandler());
			}
			mappingsLogger.debug(formatMappingName() + " " + map);
		}
		else if (logger.isDebugEnabled()) {
			List<String> patterns = new ArrayList<>();
			if (getRootHandler() != null) {
				patterns.add("/");
			}
			if (getDefaultHandler() != null) {
				patterns.add("/**");
			}
			patterns.addAll(getHandlerMap().keySet());
			logger.debug("Patterns " + patterns + " in " + formatMappingName());
		}
	}

}
