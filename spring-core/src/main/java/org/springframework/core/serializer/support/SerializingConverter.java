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

package org.springframework.core.serializer.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.util.Assert;

/**
 * 一个 {@link Converter}，委托给 {@link org.springframework.core.serializer.Serializer}，将对象转
 * 换为字节数组。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @since 3.0.5
 */
public class SerializingConverter implements Converter<Object, byte[]> {

	private final Serializer<Object> serializer;


	/**
	 * 创建一个使用标准 Java 序列化的默认 {@code SerializingConverter}。
	 */
	public SerializingConverter() {
		this.serializer = new DefaultSerializer();
	}

	/**
	 * 创建一个委托给所提供 {@link Serializer} 的 {@code SerializingConverter}。
	 */
	public SerializingConverter(Serializer<Object> serializer) {
		Assert.notNull(serializer, "Serializer must not be null");
		this.serializer = serializer;
	}


	/**
	 * 序列化源对象并返回字节数组结果。
	 */
	@Override
	public byte[] convert(Object source) {
		try  {
			return this.serializer.serializeToByteArray(source);
		}
		catch (Throwable ex) {
			throw new SerializationFailedException("Failed to serialize object using " +
					this.serializer.getClass().getSimpleName(), ex);
		}
	}

}
