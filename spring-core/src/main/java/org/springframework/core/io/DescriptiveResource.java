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

package org.springframework.core.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.lang.Nullable;

/**
 * 简单的 {@link Resource} 实现，它持有一个资源描述，但并不指向实际可读的资源。
 *
 * <p>当 API 期望一个 {@code Resource} 参数但并不一定用于实际读取时，可用作占位符。
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 */
public class DescriptiveResource extends AbstractResource {

	private final String description;


	/**
	 * 创建一个新的 DescriptiveResource。
	 * @param description 资源描述
	 */
	public DescriptiveResource(@Nullable String description) {
		this.description = (description != null ? description : "");
	}


	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(
				getDescription() + " cannot be opened because it does not point to a readable resource");
	}

	@Override
	public String getDescription() {
		return this.description;
	}


	/**
	 * 此实现比较底层描述 String。
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof DescriptiveResource &&
				((DescriptiveResource) other).description.equals(this.description)));
	}

	/**
	 * 此实现返回底层描述 String 的哈希码。
	 */
	@Override
	public int hashCode() {
		return this.description.hashCode();
	}

}
