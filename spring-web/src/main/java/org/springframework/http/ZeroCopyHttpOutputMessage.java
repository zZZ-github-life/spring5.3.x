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

package org.springframework.http;

import java.io.File;
import java.nio.file.Path;

import reactor.core.publisher.Mono;

/**
 * 支持 "零拷贝" 文件传输的 {@code ReactiveOutputMessage} 子接口。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 * @see <a href="https://en.wikipedia.org/wiki/Zero-copy">Zero-copy</a>
 */
public interface ZeroCopyHttpOutputMessage extends ReactiveHttpOutputMessage {

	/**
	 * 使用给定的 {@link File} 将消息正文写入底层 HTTP 层。
	 * @param file 要传输的文件
	 * @param position 文件内开始传输的位置
	 * @param count 要传输的字节数
	 * @return 指示完成或错误的发布者。
	 */
	default Mono<Void> writeWith(File file, long position, long count) {
		return writeWith(file.toPath(), position, count);
	}

	/**
	 * 使用给定的 {@link Path} 将消息正文写入底层 HTTP 层。
	 * @param file 要传输的文件
	 * @param position 文件内开始传输的位置
	 * @param count 要传输的字节数
	 * @return 指示完成或错误的发布者。
	 * @since 5.1
	 */
	Mono<Void> writeWith(Path file, long position, long count);

}
