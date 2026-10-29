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

package org.springframework.jdbc.datasource.embedded;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 内部辅助类，用于向 Derby 等嵌入式数据库暴露虚拟的 OutputStream，
 * 以防止创建日志文件。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public final class OutputStreamFactory {

	private OutputStreamFactory() {
	}


	/**
	 * 返回一个忽略所有写入数据的 {@link java.io.OutputStream}。
	 */
	public static OutputStream getNoopOutputStream() {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				// ignore the output
			}
		};
	}

}
