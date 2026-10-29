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

package org.springframework.test.web.client.response;

import java.io.IOException;
import java.net.URI;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.web.client.ResponseCreator;

/**
 * 用于获取 {@link ResponseCreator} 实例的静态工厂方法。
 *
 * <p><strong>Eclipse 用户：</strong>考虑将此类别添加为 Java 编辑器
 * 收藏项。要导航，请打开“首选项”并输入“favorites”。
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class MockRestResponseCreators {

	/**
	 * 用于 200 响应（OK）的 {@code ResponseCreator}。
	 */
	public static DefaultResponseCreator withSuccess() {
		return new DefaultResponseCreator(HttpStatus.OK);
	}

	/**
	 * 用于带 String 正文的 200 响应（OK）的 {@code ResponseCreator}。
	 * @param body the response body, a "UTF-8" string
	 * @param contentType the type of the content (may be {@code null})
	 */
	public static DefaultResponseCreator withSuccess(String body, @Nullable MediaType contentType) {
		DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
		return (contentType != null ? creator.contentType(contentType) : creator);
	}

	/**
	 * 用于带 byte[] 正文的 200 响应（OK）的 {@code ResponseCreator}。
	 * @param body the response body
	 * @param contentType the type of the content (may be {@code null})
	 */
	public static DefaultResponseCreator withSuccess(byte[] body, @Nullable MediaType contentType) {
		DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
		return (contentType != null ? creator.contentType(contentType) : creator);
	}

	/**
	 * 用于带基于 {@link Resource} 正文的 200 响应（OK）内容的 {@code ResponseCreator}。
	 * @param body the response body
	 * @param contentType the type of the content (may be {@code null})
	 */
	public static DefaultResponseCreator withSuccess(Resource body, @Nullable MediaType contentType) {
		DefaultResponseCreator creator = new DefaultResponseCreator(HttpStatus.OK).body(body);
		return (contentType != null ? creator.contentType(contentType) : creator);
	}

	/**
	 * 用于带 'Location' 头的 201 响应（CREATED）的 {@code ResponseCreator}。
	 * @param location the value for the {@code Location} header
	 */
	public static DefaultResponseCreator withCreatedEntity(URI location) {
		return new DefaultResponseCreator(HttpStatus.CREATED).location(location);
	}

	/**
	 * 用于 204 响应（NO_CONTENT）的 {@code ResponseCreator}。
	 */
	public static DefaultResponseCreator withNoContent() {
		return new DefaultResponseCreator(HttpStatus.NO_CONTENT);
	}

	/**
	 * 用于 400 响应（BAD_REQUEST）的 {@code ResponseCreator}。
	 */
	public static DefaultResponseCreator withBadRequest() {
		return new DefaultResponseCreator(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 用于 401 响应（UNAUTHORIZED）的 {@code ResponseCreator}。
	 */
	public static DefaultResponseCreator withUnauthorizedRequest() {
		return new DefaultResponseCreator(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * 用于 500 响应（SERVER_ERROR）的 {@code ResponseCreator}。
	 */
	public static DefaultResponseCreator withServerError() {
		return new DefaultResponseCreator(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * 用于特定 HTTP 状态的 {@code ResponseCreator}。
	 * @param status the response status
	 */
	public static DefaultResponseCreator withStatus(HttpStatus status) {
		return new DefaultResponseCreator(status);
	}

	/**
	 * 用于自定义 HTTP 状态码的 {@link #withStatus(HttpStatus)} 变体。
	 * @param status the response status
	 * @since 5.3.17
	 */
	public static DefaultResponseCreator withRawStatus(int status) {
		return new DefaultResponseCreator(status);
	}

	/**
	 * 带有内部应用程序 {@code IOException} 的 {@code ResponseCreator}。
	 * <p>例如，可以使用它来模拟 {@code SocketTimeoutException}。
	 * @param ex the {@code Exception} to be thrown at HTTP call time
	 * @since 5.2.2
	 */
	public static ResponseCreator withException(IOException ex) {
		return request -> {
			throw ex;
		};
	}

}
