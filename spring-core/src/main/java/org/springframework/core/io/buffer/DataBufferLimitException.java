/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.io.buffer;

/**
 * 异常，表示从 {@link DataBuffer DataBuffer} 流中消耗的累计字节数超过了某个预配置的限制。当数据缓
 * 冲区被缓存并聚合时（例如 {@link DataBufferUtils#join}）可能抛出此异常。或者，当数据缓冲区已被释
 * 放但正在聚合解析后的表示形式时（例如使用 Jackson 进行异步解析、SSE 解析并按事件聚合行）也可能抛
 * 出此异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.1.11
 */
@SuppressWarnings("serial")
public class DataBufferLimitException extends IllegalStateException {


	public DataBufferLimitException(String message) {
		super(message);
	}

}
