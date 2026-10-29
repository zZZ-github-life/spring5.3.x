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

package org.springframework.core.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.lang.Nullable;

/**
 * 默认的 {@link Deserializer} 实现，使用 Java 序列化读取输入流。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @see ObjectInputStream
 */
public class DefaultDeserializer implements Deserializer<Object> {

	@Nullable
	private final ClassLoader classLoader;


	/**
	 * 使用默认的 {@link ObjectInputStream} 配置创建一个 {@code DefaultDeserializer}，并采用“最新的用户
	 * 定义 ClassLoader”。
	 */
	public DefaultDeserializer() {
		this.classLoader = null;
	}

	/**
	 * 创建一个 {@code DefaultDeserializer}，使用给定 {@code ClassLoader} 的 {@link ObjectInputStream}。
	 *
	 * @since 4.2.1
	 * @see ConfigurableObjectInputStream#ConfigurableObjectInputStream(InputStream, ClassLoader)
	 */
	public DefaultDeserializer(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 从提供的 {@code InputStream} 读取内容，并将其反序列化为一个对象。
	 * @see ObjectInputStream#readObject()
	 */
	@Override
	@SuppressWarnings("deprecation")
	public Object deserialize(InputStream inputStream) throws IOException {
		ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream, this.classLoader);
		try {
			return objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex) {
			throw new org.springframework.core.NestedIOException("Failed to deserialize object type", ex);
		}
	}

}
