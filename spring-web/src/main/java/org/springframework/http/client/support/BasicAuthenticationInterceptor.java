/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.client.support;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;

/**
 * {@link ClientHttpRequestInterceptor}，用于应用给定的 HTTP 基本身份验证
 * 用户名/密码对，除非已设置自定义 {@code Authorization} 头部。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.1.1
 * @see HttpHeaders#setBasicAuth
 * @see HttpHeaders#AUTHORIZATION
 */
public class BasicAuthenticationInterceptor implements ClientHttpRequestInterceptor {

	private final String encodedCredentials;


	/**
	 * 创建一个新的拦截器，为给定的用户名和密码
	 * 添加基本身份验证。
	 * @param username the username to use
	 * @param password the password to use
	 * @see HttpHeaders#setBasicAuth(String, String)
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 */
	public BasicAuthenticationInterceptor(String username, String password) {
		this(username, password, null);
	}

	/**
	 * 创建一个新的拦截器，为给定的用户名和密码添加基本身份验证，
	 * 并使用指定的字符集进行编码。
	 * @param username the username to use
	 * @param password the password to use
	 * @param charset the charset to use
	 * @see HttpHeaders#setBasicAuth(String, String, Charset)
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 */
	public BasicAuthenticationInterceptor(String username, String password, @Nullable Charset charset) {
		this.encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, charset);
	}


	@Override
	public ClientHttpResponse intercept(
			HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

		HttpHeaders headers = request.getHeaders();
		if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
			headers.setBasicAuth(this.encodedCredentials);
		}
		return execution.execute(request, body);
	}

}
