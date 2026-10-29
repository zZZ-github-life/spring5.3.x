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

package org.springframework.web.method.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * 管理通过 {@link SessionAttributes @SessionAttributes} 声明的
 * 控制器专属会话属性。实际存储委托给 {@link SessionAttributeStore} 实例。
 *
 * <p>当标注了 {@code @SessionAttributes} 的控制器向模型中添加属性时，
 * 这些属性会根据 {@code @SessionAttributes} 指定的名称和类型进行检查。
 * 匹配的模型属性会保存到 HTTP 会话中，并一直保留到控制器调用
 * {@link SessionStatus#setComplete()} 为止。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class SessionAttributesHandler {

	private final Set<String> attributeNames = new HashSet<>();

	private final Set<Class<?>> attributeTypes = new HashSet<>();

	private final Set<String> knownAttributeNames = Collections.newSetFromMap(new ConcurrentHashMap<>(4));

	private final SessionAttributeStore sessionAttributeStore;


	/**
	 * 创建一个新的会话属性处理器。如果给定类型上存在 {@code @SessionAttributes} 注解，
	 * 则从该注解中提取会话属性名称和类型。
	 * @param handlerType the controller type
	 * @param sessionAttributeStore used for session access
	 */
	public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore sessionAttributeStore) {
		Assert.notNull(sessionAttributeStore, "SessionAttributeStore may not be null");
		this.sessionAttributeStore = sessionAttributeStore;

		SessionAttributes ann = AnnotatedElementUtils.findMergedAnnotation(handlerType, SessionAttributes.class);
		if (ann != null) {
			Collections.addAll(this.attributeNames, ann.names());
			Collections.addAll(this.attributeTypes, ann.types());
		}
		this.knownAttributeNames.addAll(this.attributeNames);
	}


	/**
	 * 此实例所代表的控制器是否通过 {@link SessionAttributes} 注解
	 * 声明了任何会话属性。
	 */
	public boolean hasSessionAttributes() {
		return (!this.attributeNames.isEmpty() || !this.attributeTypes.isEmpty());
	}

	/**
	 * 属性名称或类型是否与底层控制器上 {@code @SessionAttributes}
	 * 指定的名称和类型匹配。
	 * <p>通过此方法成功解析的属性会被“记住”，随后在
	 * {@link #retrieveAttributes(WebRequest)} 和
	 * {@link #cleanupAttributes(WebRequest)} 中使用。
	 * @param attributeName the attribute name to check
	 * @param attributeType the type for the attribute
	 */
	public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
		Assert.notNull(attributeName, "Attribute name must not be null");
		if (this.attributeNames.contains(attributeName) || this.attributeTypes.contains(attributeType)) {
			this.knownAttributeNames.add(attributeName);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 将给定属性中的一个子集存储到会话中。未通过 {@code @SessionAttributes}
	 * 声明为会话属性的属性将被忽略。
	 * @param request the current request
	 * @param attributes candidate attributes for session storage
	 */
	public void storeAttributes(WebRequest request, Map<String, ?> attributes) {
		attributes.forEach((name, value) -> {
			if (value != null && isHandlerSessionAttribute(name, value.getClass())) {
				this.sessionAttributeStore.storeAttribute(request, name, value);
			}
		});
	}

	/**
	 * 从会话中获取“已知”属性，即在 {@code @SessionAttributes} 中按名称列出的属性，
	 * 或之前在模型中存储且按类型匹配的属性。
	 * @param request the current request
	 * @return a map with handler session attributes, possibly empty
	 */
	public Map<String, Object> retrieveAttributes(WebRequest request) {
		Map<String, Object> attributes = new HashMap<>();
		for (String name : this.knownAttributeNames) {
			Object value = this.sessionAttributeStore.retrieveAttribute(request, name);
			if (value != null) {
				attributes.put(name, value);
			}
		}
		return attributes;
	}

	/**
	 * 从会话中移除“已知”属性，即在 {@code @SessionAttributes} 中按名称列出的属性，
	 * 或之前在模型中存储且按类型匹配的属性。
	 * @param request the current request
	 */
	public void cleanupAttributes(WebRequest request) {
		for (String attributeName : this.knownAttributeNames) {
			this.sessionAttributeStore.cleanupAttribute(request, attributeName);
		}
	}

	/**
	 * 对底层 {@link SessionAttributeStore} 的透传调用。
	 * @param request the current request
	 * @param attributeName the name of the attribute of interest
	 * @return the attribute value, or {@code null} if none
	 */
	@Nullable
	Object retrieveAttribute(WebRequest request, String attributeName) {
		return this.sessionAttributeStore.retrieveAttribute(request, attributeName);
	}

}
