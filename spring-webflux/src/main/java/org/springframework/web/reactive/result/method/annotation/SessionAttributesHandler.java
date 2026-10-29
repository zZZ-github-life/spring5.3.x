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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.server.WebSession;

/**
 * 包级私有类，协助 {@link ModelInitializer} 根据通过 {@link SessionAttributes @SessionAttributes} 声明的模型属性名称和类型，在 {@link WebSession} 中管理模型属性。
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class SessionAttributesHandler {

	private final Set<String> attributeNames = new HashSet<>();

	private final Set<Class<?>> attributeTypes = new HashSet<>();

	private final Set<String> knownAttributeNames = Collections.newSetFromMap(new ConcurrentHashMap<>(4));


	/**
	 * 创建新的会话属性处理器。如果给定类型上存在 {@code @SessionAttributes} 注解，则从中提取会话属性名称和类型。
	 * @param handlerType the controller type
	 */
	public SessionAttributesHandler(Class<?> handlerType) {
		SessionAttributes ann = AnnotatedElementUtils.findMergedAnnotation(handlerType, SessionAttributes.class);
		if (ann != null) {
			Collections.addAll(this.attributeNames, ann.names());
			Collections.addAll(this.attributeTypes, ann.types());
		}
		this.knownAttributeNames.addAll(this.attributeNames);
	}


	/**
	 * 此实例所代表的控制器是否通过 {@link SessionAttributes} 注解声明了任何会话属性。
	 */
	public boolean hasSessionAttributes() {
		return (!this.attributeNames.isEmpty() || !this.attributeTypes.isEmpty());
	}

	/**
	 * 属性名称或类型是否匹配底层控制器上通过 {@code @SessionAttributes} 指定的名称和类型。
	 * <p>通过此方法成功解析的属性会被“记住”，随后用于 {@link #retrieveAttributes(WebSession)} 和 {@link #cleanupAttributes(WebSession)}。
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
	 * 从会话中检索“已知”属性，即在 {@code @SessionAttributes} 中按名称列出的属性，或之前存储在模型中且类型匹配的属性。
	 * @param session the current session
	 * @return a map with handler session attributes, possibly empty
	 */
	public Map<String, Object> retrieveAttributes(WebSession session) {
		Map<String, Object> attributes = new HashMap<>();
		this.knownAttributeNames.forEach(name -> {
			Object value = session.getAttribute(name);
			if (value != null) {
				attributes.put(name, value);
			}
		});
		return attributes;
	}

	/**
	 * 将给定属性的子集存储在会话中。未通过 {@code @SessionAttributes} 声明为会话属性的属性将被忽略。
	 * @param session the current session
	 * @param attributes candidate attributes for session storage
	 */
	public void storeAttributes(WebSession session, Map<String, ?> attributes) {
		attributes.forEach((name, value) -> {
			if (value != null && isHandlerSessionAttribute(name, value.getClass())) {
				session.getAttributes().put(name, value);
			}
		});
	}

	/**
	 * 从会话中移除“已知”属性，即在 {@code @SessionAttributes} 中按名称列出的属性，或之前存储在模型中且类型匹配的属性。
	 * @param session the current session
	 */
	public void cleanupAttributes(WebSession session) {
		this.knownAttributeNames.forEach(name -> session.getAttributes().remove(name));
	}

}
