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

package org.springframework.core.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 用于将对象流式写入 OutputStream 的策略接口。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @param <T> the object type
 * @see Deserializer
 */
@FunctionalInterface
public interface Serializer<T> {

	/**
	 * 将类型为 T 的对象写入给定的 OutputStream。
	 * <p>注意：实现不应关闭给定的 OutputStream（或该 OutputStream 的任何装饰器），而应将此留给调用者处
	 * 理。
	 * @param object the object to serialize
	 * @param outputStream the output stream
	 * @throws IOException in case of errors writing to the stream
	 */
	void serialize(T object, OutputStream outputStream) throws IOException;

	/**
	 * 将类型为 T 的对象转换为序列化的字节数组。
	 * @param object the object to serialize
	 * @return the resulting byte array
	 * @throws IOException in case of serialization failure
	 * @since 5.2.7
	 */
	default byte[] serializeToByteArray(T object) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		serialize(object, out);
		return out.toByteArray();
	}

}
