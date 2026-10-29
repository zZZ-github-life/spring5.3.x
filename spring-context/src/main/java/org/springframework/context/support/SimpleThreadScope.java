/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.support;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;

/**
 * 一个简单的、基于线程的 {@link Scope} 实现。
 *
 * <p><b>NOTE:</b> 此线程作用域在常见上下文中默认不注册。你需要在自己的配置中显式将其分配给某个作用
 * 域键，方式是通过
 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory#registerScope}，或通过一
 * 个 {@link org.springframework.beans.factory.config.CustomScopeConfigurer} Bean。
 *
 * <p>{@code SimpleThreadScope} <em>不会清理</em>与其关联的任何对象。因此在 Web 环境中，通常更可取
 * 的是使用像 {@code org.springframework.web.context.request.RequestScope} 这样与请求绑定的作用域实
 * 现，它为作用域属性实现了完整的生命周期（包括可靠的销毁）。
 *
 * <p>有关支持销毁回调的基于线程的 {@code Scope} 实现，请参见
 * <a href="https://www.springbyexample.org/examples/custom-thread-scope-module.html">Spring by
 * Example</a>。
 *
 * <p>感谢 Eugene Kuleshov 提交了线程作用域的原始原型！
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.web.context.request.RequestScope
 */
public class SimpleThreadScope implements Scope {

	private static final Log logger = LogFactory.getLog(SimpleThreadScope.class);

	private final ThreadLocal<Map<String, Object>> threadScope =
			new NamedThreadLocal<Map<String, Object>>("SimpleThreadScope") {
				@Override
				protected Map<String, Object> initialValue() {
					return new HashMap<>();
				}
			};


	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Map<String, Object> scope = this.threadScope.get();
		// NOTE: Do NOT modify the following to use Map::computeIfAbsent. For details,
		// see https://github.com/spring-projects/spring-framework/issues/25801.
		Object scopedObject = scope.get(name);
		if (scopedObject == null) {
			scopedObject = objectFactory.getObject();
			scope.put(name, scopedObject);
		}
		return scopedObject;
	}

	@Override
	@Nullable
	public Object remove(String name) {
		Map<String, Object> scope = this.threadScope.get();
		return scope.remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		logger.warn("SimpleThreadScope does not support destruction callbacks. " +
				"Consider using RequestScope in a web environment.");
	}

	@Override
	@Nullable
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return Thread.currentThread().getName();
	}

}
