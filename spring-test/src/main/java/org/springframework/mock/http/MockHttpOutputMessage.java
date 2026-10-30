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

package org.springframework.mock.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.util.StreamUtils;

/**
 * {@link HttpOutputMessage} 的模拟实现。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class MockHttpOutputMessage implements HttpOutputMessage {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final HttpHeaders headers = new HttpHeaders();

	private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);


	/**
	 * 返回头信息。
	 */
	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * 返回正文内容。
	 */
	@Override
	public OutputStream getBody() throws IOException {
		return this.body;
	}

	/**
	 * 以字节数组形式返回正文内容。
	 */
	public byte[] getBodyAsBytes() {
		return this.body.toByteArray();
	}

	/**
	 * 将正文内容按 UTF-8 字符串解释后返回。
	 */
	public String getBodyAsString() {
		return getBodyAsString(DEFAULT_CHARSET);
	}

	/**
	 * 以字符串形式返回正文内容。
	 * @param charset 用于将正文内容转换为 String 的字符集
	 */
	public String getBodyAsString(Charset charset) {
		return StreamUtils.copyToString(this.body, charset);
	}

}
