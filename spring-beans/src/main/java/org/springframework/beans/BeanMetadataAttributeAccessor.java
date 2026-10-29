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

package org.springframework.beans;

import org.springframework.core.AttributeAccessorSupport;
import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.core.AttributeAccessorSupport} 的扩展，以
 * {@link BeanMetadataAttribute} 对象的形式持有属性，以便跟踪定义来源。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
@SuppressWarnings("serial")
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement {

	@Nullable
	private Object source;


	/**
	 * 为此元数据元素设置配置源 {@code Object}。<p>对象的确切类型取决于所使用的配置机制。
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	@Nullable
	public Object getSource() {
		return this.source;
	}


	/**
	 * 将给定的 BeanMetadataAttribute 添加到此访问器的属性集合中。
	 * @param attribute the BeanMetadataAttribute object to register
	 */
	public void addMetadataAttribute(BeanMetadataAttribute attribute) {
		super.setAttribute(attribute.getName(), attribute);
	}

	/**
	 * 在此访问器的属性集合中查找给定的 BeanMetadataAttribute。
	 * @param name the name of the attribute
	 * @return the corresponding BeanMetadataAttribute object,
	 * or {@code null} if no such attribute defined
	 */
	@Nullable
	public BeanMetadataAttribute getMetadataAttribute(String name) {
		return (BeanMetadataAttribute) super.getAttribute(name);
	}

	@Override
	public void setAttribute(String name, @Nullable Object value) {
		super.setAttribute(name, new BeanMetadataAttribute(name, value));
	}

	@Override
	@Nullable
	public Object getAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.getAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}

	@Override
	@Nullable
	public Object removeAttribute(String name) {
		BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.removeAttribute(name);
		return (attribute != null ? attribute.getValue() : null);
	}

}
