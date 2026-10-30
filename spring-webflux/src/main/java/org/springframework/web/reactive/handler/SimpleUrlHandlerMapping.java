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

package org.springframework.web.reactive.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.util.CollectionUtils;

/**
 * 用于将 URL 映射到请求处理器 bean 的 {@link org.springframework.web.reactive.HandlerMapping} 接口实现。支持映射到 bean 实例和映射到 bean 名称；对于非单例处理器，需要使用后者。
 *
 * <p>"urlMap" 属性适合用 bean 实例填充处理器映射。可以通过 "mappings" 属性设置到 bean 名称的映射，形式为 {@code java.util.Properties} 类所接受的形式，如下所示：
 *
 * <pre class="code">
 * /welcome.html=ticketController
 * /show.html=ticketController</pre>
 *
 * <p>语法为 {@code PATH=HANDLER_BEAN_NAME}。如果路径不以斜杠开头，则会添加一个斜杠。
 *
 * <p>支持直接匹配（例如注册的 "/test" 匹配 "/test"）以及各种 Ant 风格模式匹配（例如注册的 "/t*" 模式同时匹配 "/test" 和 "/team"，"/test/*" 匹配 "/test" 下的所有路径，"/test/**" 匹配 "/test" 下的所有路径）。详情请参见 {@link org.springframework.web.util.pattern.PathPattern} 的 javadoc。
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class SimpleUrlHandlerMapping extends AbstractUrlHandlerMapping {

	private final Map<String, Object> urlMap = new LinkedHashMap<>();


	/**
	 * 使用默认设置创建 {@code SimpleUrlHandlerMapping}。
	 */
	public SimpleUrlHandlerMapping() {
	}

	/**
	 * 使用提供的 URL 映射创建 {@code SimpleUrlHandlerMapping}。
	 * @param urlMap 以 URL 路径为键、以处理器 Bean（或处理器 Bean 名称）
	 * 为值的 Map
	 * @since 5.2
	 * @see #setUrlMap(Map)
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap) {
		setUrlMap(urlMap);
	}

	/**
	 * 使用提供的 URL 映射和顺序创建 {@code SimpleUrlHandlerMapping}。
	 * @param urlMap 以 URL 路径为键、以处理器 Bean（或处理器 Bean 名称）
	 * 为值的 Map
	 * @param order 此 {@code SimpleUrlHandlerMapping} 的顺序值
	 * @since 5.2
	 * @see #setUrlMap(Map)
	 * @see #setOrder(int)
	 */
	public SimpleUrlHandlerMapping(Map<String, ?> urlMap, int order) {
		setUrlMap(urlMap);
		setOrder(order);
	}


	/**
	 * 将 URL 路径映射到处理器 bean 名称。这是配置此 HandlerMapping 的典型方式。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。语法详情请参见 {@link org.springframework.web.util.pattern.PathPattern} 的 javadoc。
	 * @param mappings 以 URL 为键、以 Bean 名称为值的属性
	 * @see #setUrlMap
	 */
	public void setMappings(Properties mappings) {
		CollectionUtils.mergePropertiesIntoMap(mappings, this.urlMap);
	}

	/**
	 * 设置以 URL 路径为键、以处理器 bean（或处理器 bean 名称）为值的 Map。便于使用 bean 引用填充。
	 * <p>支持直接 URL 匹配和 Ant 风格模式匹配。语法详情请参见 {@link org.springframework.web.util.pattern.PathPattern} 的 javadoc。
	 * @param urlMap 以 URL 为键、以 Bean 为值的 Map
	 * @see #setMappings
	 */
	public void setUrlMap(Map<String, ?> urlMap) {
		this.urlMap.putAll(urlMap);
	}

	/**
	 * 允许以 {@code Map} 方式访问 URL 路径映射，并可以选择添加或覆盖特定条目。
	 * <p>这对于直接指定条目很有用，例如通过 "urlMap[myKey]"。在子 bean 定义中添加或覆盖条目时尤其有用。
	 */
	public Map<String, ?> getUrlMap() {
		return this.urlMap;
	}


	/**
	 * 除父类的初始化外，还调用 {@link #registerHandlers} 方法。
	 */
	@Override
	public void initApplicationContext() throws BeansException {
		super.initApplicationContext();
		registerHandlers(this.urlMap);
	}

	/**
	 * 为相应路径注册 URL 映射中指定的所有处理器。
	 * @param urlMap 以 URL 路径为键、以处理器 Bean 或 Bean 名称为值的 Map
	 * @throws BeansException 如果无法注册某个处理器
	 * @throws IllegalStateException 如果存在冲突的已注册处理器
	 */
	protected void registerHandlers(Map<String, Object> urlMap) throws BeansException {
		if (urlMap.isEmpty()) {
			logger.trace("No patterns in " + formatMappingName());
		}
		else {
			for (Map.Entry<String, Object> entry : urlMap.entrySet()) {
				String url = entry.getKey();
				Object handler = entry.getValue();
				// Prepend with slash if not already present.
				if (!url.startsWith("/")) {
					url = "/" + url;
				}
				// Remove whitespace from handler bean name.
				if (handler instanceof String) {
					handler = ((String) handler).trim();
				}
				registerHandler(url, handler);
			}
			logMappings();
		}
	}

	private void logMappings() {
		if (mappingsLogger.isDebugEnabled()) {
			mappingsLogger.debug(formatMappingName() + " " + getHandlerMap());
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("Patterns " + getHandlerMap().keySet() + " in " + formatMappingName());
		}
	}

}
