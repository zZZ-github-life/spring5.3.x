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

package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 提供内置 {@link ExchangeFilterFunction} 实现（用于基本认证、错误处理等）的静态工厂方法。
 * @author Rob Winch
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class ExchangeFilterFunctions {

	/**
	 * 带有 {@link Credentials} 的请求属性名称，供 {@link #basicAuthentication()} 使用。
	 * @deprecated as of Spring 5.1 in favor of using
	 * {@link HttpHeaders#setBasicAuth(String, String)} while building the request.
	 */
	@Deprecated
	public static final String BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE =
			ExchangeFilterFunctions.class.getName() + ".basicAuthenticationCredentials";


	/**
	 * 从响应正文消费最多指定数量的字节，如果还有更多数据到达则取消。
	 * <p>内部委托给 {@link DataBufferUtils#takeUntilByteCount}。
	 * @param maxByteCount the limit as number of bytes
	 * @return the filter to limit the response size with
	 * @since 5.1
	 */
	public static ExchangeFilterFunction limitResponseSize(long maxByteCount) {
		return (request, next) ->
				next.exchange(request).map(response ->
						response.mutate()
								.body(body -> DataBufferUtils.takeUntilByteCount(body, maxByteCount))
								.build());
	}

	/**
	 * 返回当给定 {@link HttpStatus} 谓词匹配时生成错误信号的过滤器。
	 * @param statusPredicate the predicate to check the HTTP status with
	 * @param exceptionFunction the function to create the exception
	 * @return the filter to generate an error signal
	 */
	public static ExchangeFilterFunction statusError(Predicate<HttpStatus> statusPredicate,
			Function<ClientResponse, ? extends Throwable> exceptionFunction) {

		Assert.notNull(statusPredicate, "Predicate must not be null");
		Assert.notNull(exceptionFunction, "Function must not be null");

		return ExchangeFilterFunction.ofResponseProcessor(
				response -> (statusPredicate.test(response.statusCode()) ?
						Mono.error(exceptionFunction.apply(response)) : Mono.just(response)));
	}

	/**
	 * 返回通过 {@link HttpHeaders#setBasicAuth(String)} 和 {@link HttpHeaders#encodeBasicAuth(String, String, Charset)} 将 HTTP 基本认证应用到请求头的过滤器。
	 * @param username the username
	 * @param password the password
	 * @return the filter to add authentication headers with
	 * @see HttpHeaders#encodeBasicAuth(String, String, Charset)
	 * @see HttpHeaders#setBasicAuth(String)
	 */
	public static ExchangeFilterFunction basicAuthentication(String username, String password) {
		String encodedCredentials = HttpHeaders.encodeBasicAuth(username, password, null);
		return (request, next) ->
				next.exchange(ClientRequest.from(request)
						.headers(headers -> headers.setBasicAuth(encodedCredentials))
						.build());
	}

	/**
	 * {@link #basicAuthentication(String, String)} 的变体，从{@link #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE 请求属性}中查找 {@link Credentials Credentials}。
	 * @return the filter to use
	 * @see Credentials
	 * @deprecated as of Spring 5.1 in favor of using
	 * {@link HttpHeaders#setBasicAuth(String, String)} while building the request.
	 */
	@Deprecated
	public static ExchangeFilterFunction basicAuthentication() {
		return (request, next) -> {
			Object attr = request.attributes().get(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE);
			if (attr instanceof Credentials) {
				Credentials cred = (Credentials) attr;
				return next.exchange(ClientRequest.from(request)
						.headers(headers -> headers.setBasicAuth(cred.username, cred.password))
						.build());
			}
			else {
				return next.exchange(request);
			}
		};
	}


	/**
	 * 存储用于 HTTP 基本认证的用户名和密码。
	 * @deprecated as of Spring 5.1 in favor of using
	 * {@link HttpHeaders#setBasicAuth(String, String)} while building the request.
	 */
	@Deprecated
	public static final class Credentials {

		private final String username;

		private final String password;

		/**
		 * 使用给定的用户名和密码创建新的 {@code Credentials} 实例。
		 * @param username the username
		 * @param password the password
		 */
		public Credentials(String username, String password) {
			Assert.notNull(username, "'username' must not be null");
			Assert.notNull(password, "'password' must not be null");
			this.username = username;
			this.password = password;
		}

		/**
		 * 返回一个 {@literal Consumer}，它将给定用户名和密码存储为 {@code Credentials} 类型的请求属性，随后由 {@link ExchangeFilterFunctions#basicAuthentication()} 使用。
		 * @param username the username
		 * @param password the password
		 * @return a consumer that can be passed into
		 * {@linkplain ClientRequest.Builder#attributes(java.util.function.Consumer)}
		 * @see ClientRequest.Builder#attributes(java.util.function.Consumer)
		 * @see #BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE
		 */
		public static Consumer<Map<String, Object>> basicAuthenticationCredentials(String username, String password) {
			Credentials credentials = new Credentials(username, password);
			return (map -> map.put(BASIC_AUTHENTICATION_CREDENTIALS_ATTRIBUTE, credentials));
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Credentials)) {
				return false;
			}
			Credentials otherCred = (Credentials) other;
			return (this.username.equals(otherCred.username) && this.password.equals(otherCred.password));
		}

		@Override
		public int hashCode() {
			return 31 * this.username.hashCode() + this.password.hashCode();
		}
	}

}
