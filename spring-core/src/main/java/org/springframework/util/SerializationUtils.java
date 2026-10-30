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

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.springframework.lang.Nullable;

/**
 * 用于通过
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/serialization/" target="_blank">
 * Java 对象序列化</a> 进行序列化和反序列化的静态工具。
 *
 * <p><strong>警告</strong>：使用这些工具应谨慎。详情请参见
 * <a href="https://www.oracle.com/java/technologies/javase/seccodeguide.html#8" target="_blank">
 * Java 编程语言安全编码指南</a>。
 *
 * @author Dave Syer
 * @author Loïc Ledoyen
 * @author Sam Brannen
 * @since 3.0.5
 */
public abstract class SerializationUtils {

	/**
	 * 将给定对象序列化为字节数组。
	 * @param object 要序列化的对象
	 * @return 以可移植方式表示该对象的字节数组
	 */
	@Nullable
	public static byte[] serialize(@Nullable Object object) {
		if (object == null) {
			return null;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(object);
			oos.flush();
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to serialize object of type: " + object.getClass(), ex);
		}
		return baos.toByteArray();
	}

	/**
	 * 将字节数组反序列化为对象。
	 * <p><strong>警告</strong>：此工具将在 Spring Framework 6.0 中弃用，因为它使用 Java Object
	 * Serialization，该机制允许运行任意代码，并已知是许多远程代码执行（RCE）漏洞的来源。建议使用外部工
	 * 具（序列化为 JSON、XML 或任何其他格式），并定期检查更新以避免允许 RCE。
	 * @param bytes 已序列化的对象
	 * @return 反序列化这些字节的结果
	 */
	@Nullable
	public static Object deserialize(@Nullable byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
			return ois.readObject();
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to deserialize object", ex);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to deserialize object type", ex);
		}
	}

}
