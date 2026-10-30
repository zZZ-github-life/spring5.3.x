/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.oxm.support;

import java.io.IOException;

import org.xml.sax.InputSource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * 用于处理 SAX 的便捷工具方法。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class SaxResourceUtils {

	/**
	 * 根据给定资源创建一个 SAX {@code InputSource}。
	 * <p>如果可用，将系统标识符设置为该资源的 {@code URL}。
	 * @param resource 资源
	 * @return 根据该资源创建的输入源
	 * @throws IOException 如果发生 I/O 异常
	 * @see InputSource#setSystemId(String)
	 * @see #getSystemId(org.springframework.core.io.Resource)
	 */
	public static InputSource createInputSource(Resource resource) throws IOException {
		InputSource inputSource = new InputSource(resource.getInputStream());
		inputSource.setSystemId(getSystemId(resource));
		return inputSource;
	}

	/**
	 * 获取给定资源的 URL 作为 System ID。
	 * <p>如果无法打开，则返回 {@code null}。
	 */
	@Nullable
	private static String getSystemId(Resource resource) {
		try {
			return resource.getURI().toString();
		}
		catch (IOException ex) {
			return null;
		}
	}

}
