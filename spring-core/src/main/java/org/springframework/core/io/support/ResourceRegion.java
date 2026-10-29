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

package org.springframework.core.io.support;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link Resource} 实现的区域，由 {@link Resource} 中的 {@code position}
 * 以及该区域长度的字节 {@code count} 具体化。
 *
 * @author Arjen Poutsma
 * @since 4.3
 */
public class ResourceRegion {

	private final Resource resource;

	private final long position;

	private final long count;


	/**
	 * 根据给定的 {@link Resource} 创建一个新的 {@code ResourceRegion}。
	 * 该资源区域由给定 {@code Resource} 中的起始 {@code position}
	 * 和字节 {@code count} 表示。
	 * @param resource a Resource
	 * @param position the start position of the region in that resource
	 * @param count the byte count of the region in that resource
	 */
	public ResourceRegion(Resource resource, long position, long count) {
		Assert.notNull(resource, "Resource must not be null");
		Assert.isTrue(position >= 0, "'position' must be larger than or equal to 0");
		Assert.isTrue(count >= 0, "'count' must be larger than or equal to 0");
		this.resource = resource;
		this.position = position;
		this.count = count;
	}


	/**
	 * 返回此 {@code ResourceRegion} 底层的 {@link Resource}。
	 */
	public Resource getResource() {
		return this.resource;
	}

	/**
	 * 返回此区域在底层 {@link Resource} 中的起始位置。
	 */
	public long getPosition() {
		return this.position;
	}

	/**
	 * 返回此区域在底层 {@link Resource} 中的字节数。
	 */
	public long getCount() {
		return this.count;
	}

}
