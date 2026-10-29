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

package org.springframework.beans.factory.parsing;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 对 {@link Resource 资源}中任意位置进行建模的类。
 *
 * <p>通常用于跟踪 XML 配置文件中存在问题或错误元数据的位置。例如，{@link #getSource() 源}位置可能
 * 是“beans.properties 第 76 行定义的 Bean 的 Class 无效”；另一个源可能是来自已解析 XML
 * {@link org.w3c.dom.Document} 的实际 DOM 元素；或者源对象可能只是 {@code null}。
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class Location {

	private final Resource resource;

	@Nullable
	private final Object source;


	/**
	 * 创建 {@link Location} 类的新实例。
	 * @param resource the resource with which this location is associated
	 */
	public Location(Resource resource) {
		this(resource, null);
	}

	/**
	 * 创建 {@link Location} 类的新实例。
	 * @param resource the resource with which this location is associated
	 * @param source the actual location within the associated resource
	 * (may be {@code null})
	 */
	public Location(Resource resource, @Nullable Object source) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
		this.source = source;
	}


	/**
	 * 获取与此位置关联的资源。
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 获取关联的{@link #getResource() 资源}内的实际位置（可能为 {@code null}）。
	 *
	 * <p>有关返回对象实际类型的示例，请参见{@link Location 此类的类级 javadoc}。
	 */
	@Nullable
	public Object getSource() {
		return this.source;
	}

}
