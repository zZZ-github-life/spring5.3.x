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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 用于将 InputStream 中的数据转换为 Object 的策略接口。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @param <T> 对象类型
 * @see Serializer
 */
@FunctionalInterface
public interface Deserializer<T> {

	/**
	 * 从给定的 InputStream 中读取（组装）类型为 T 的对象。
	 * <p>注意：实现不应关闭给定的 InputStream（或该 InputStream 的任何装饰器），而应将此留给调用者处理。
	 *
	 * @param inputStream 输入流
	 * @return 反序列化后的对象
	 * @throws IOException 如果从流中读取时发生错误
	 */
	T deserialize(InputStream inputStream) throws IOException;

	/**
	 * 从给定的字节数组中读取（组装）类型为 T 的对象。
	 * @param serialized 字节数组
	 * @return 反序列化后的对象
	 * @throws IOException 如果反序列化失败
	 * @since 5.2.7
	 */
	default T deserializeFromByteArray(byte[] serialized) throws IOException {
		return deserialize(new ByteArrayInputStream(serialized));
	}

}
