/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.util.Map;

import javax.servlet.ServletRequest;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.HandlerMapping;

/**
 * {@link ServletRequestDataBinder} 的子类，它将 URI 模板变量
 * 添加到用于数据绑定的值中。
 *
 * <p><strong>警告</strong>：数据绑定可能会暴露对象图中不应被外部客户端
 * 访问或修改的部分，从而带来安全问题。因此，在设计和使用数据绑定时，
 * 应仔细考虑安全性。有关更多详细信息，请参阅参考手册中
 * 关于数据绑定的专门章节：
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see ServletRequestDataBinder
 * @see HandlerMapping#URI_TEMPLATE_VARIABLES_ATTRIBUTE
 */
public class ExtendedServletRequestDataBinder extends ServletRequestDataBinder {

	/**
	 * 使用默认对象名称创建一个新实例。
	 * @param target 要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新实例。
	 * @param target 要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则可为 {@code null}）
	 * @param objectName 目标对象的名称
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public ExtendedServletRequestDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将 URI 变量合并到用于数据绑定的属性值中。
	 */
	@Override
	protected void addBindValues(MutablePropertyValues mpvs, ServletRequest request) {
		String attr = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		@SuppressWarnings("unchecked")
		Map<String, String> uriVars = (Map<String, String>) request.getAttribute(attr);
		if (uriVars != null) {
			uriVars.forEach((name, value) -> {
				if (mpvs.contains(name)) {
					if (logger.isDebugEnabled()) {
						logger.debug("URI variable '" + name + "' overridden by request bind value.");
					}
				}
				else {
					mpvs.addPropertyValue(name, value);
				}
			});
		}
	}

}
