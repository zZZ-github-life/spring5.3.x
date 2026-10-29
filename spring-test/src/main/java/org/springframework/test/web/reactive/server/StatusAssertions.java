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

package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.AssertionErrors;

/**
 * 对响应状态的断言。
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectStatus()
 */
public class StatusAssertions {

	private final ExchangeResult exchangeResult;

	private final WebTestClient.ResponseSpec responseSpec;


	StatusAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
		this.exchangeResult = result;
		this.responseSpec = spec;
	}


	/**
	 * 以 {@link HttpStatus} 形式断言响应状态。
	 */
	public WebTestClient.ResponseSpec isEqualTo(HttpStatus status) {
		return isEqualTo(status.value());
	}

	/**
	 * 以整数形式断言响应状态。
	 */
	public WebTestClient.ResponseSpec isEqualTo(int status) {
		int actual = this.exchangeResult.getRawStatusCode();
		this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", status, actual));
		return this.responseSpec;
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.OK} (200)。
	 */
	public WebTestClient.ResponseSpec isOk() {
		return assertStatusAndReturn(HttpStatus.OK);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.CREATED} (201)。
	 */
	public WebTestClient.ResponseSpec isCreated() {
		return assertStatusAndReturn(HttpStatus.CREATED);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.ACCEPTED} (202)。
	 */
	public WebTestClient.ResponseSpec isAccepted() {
		return assertStatusAndReturn(HttpStatus.ACCEPTED);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.NO_CONTENT} (204)。
	 */
	public WebTestClient.ResponseSpec isNoContent() {
		return assertStatusAndReturn(HttpStatus.NO_CONTENT);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.FOUND} (302)。
	 */
	public WebTestClient.ResponseSpec isFound() {
		return assertStatusAndReturn(HttpStatus.FOUND);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.SEE_OTHER} (303)。
	 */
	public WebTestClient.ResponseSpec isSeeOther() {
		return assertStatusAndReturn(HttpStatus.SEE_OTHER);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.NOT_MODIFIED} (304)。
	 */
	public WebTestClient.ResponseSpec isNotModified() {
		return assertStatusAndReturn(HttpStatus.NOT_MODIFIED);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.TEMPORARY_REDIRECT} (307)。
	 */
	public WebTestClient.ResponseSpec isTemporaryRedirect() {
		return assertStatusAndReturn(HttpStatus.TEMPORARY_REDIRECT);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.PERMANENT_REDIRECT} (308)。
	 */
	public WebTestClient.ResponseSpec isPermanentRedirect() {
		return assertStatusAndReturn(HttpStatus.PERMANENT_REDIRECT);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.BAD_REQUEST} (400)。
	 */
	public WebTestClient.ResponseSpec isBadRequest() {
		return assertStatusAndReturn(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.UNAUTHORIZED} (401)。
	 */
	public WebTestClient.ResponseSpec isUnauthorized() {
		return assertStatusAndReturn(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.FORBIDDEN} (403)。
	 * @since 5.0.2
	 */
	public WebTestClient.ResponseSpec isForbidden() {
		return assertStatusAndReturn(HttpStatus.FORBIDDEN);
	}

	/**
	 * 断言响应状态码为 {@code HttpStatus.NOT_FOUND} (404)。
	 */
	public WebTestClient.ResponseSpec isNotFound() {
		return assertStatusAndReturn(HttpStatus.NOT_FOUND);
	}

	/**
	 * 断言响应错误消息。
	 */
	public WebTestClient.ResponseSpec reasonEquals(String reason) {
		String actual = this.exchangeResult.getStatus().getReasonPhrase();
		this.exchangeResult.assertWithDiagnostics(() ->
				AssertionErrors.assertEquals("Response status reason", reason, actual));
		return this.responseSpec;
	}

	/**
	 * 断言响应状态码在 1xx 范围内。
	 */
	public WebTestClient.ResponseSpec is1xxInformational() {
		return assertSeriesAndReturn(HttpStatus.Series.INFORMATIONAL);
	}

	/**
	 * 断言响应状态码在 2xx 范围内。
	 */
	public WebTestClient.ResponseSpec is2xxSuccessful() {
		return assertSeriesAndReturn(HttpStatus.Series.SUCCESSFUL);
	}

	/**
	 * 断言响应状态码在 3xx 范围内。
	 */
	public WebTestClient.ResponseSpec is3xxRedirection() {
		return assertSeriesAndReturn(HttpStatus.Series.REDIRECTION);
	}

	/**
	 * 断言响应状态码在 4xx 范围内。
	 */
	public WebTestClient.ResponseSpec is4xxClientError() {
		return assertSeriesAndReturn(HttpStatus.Series.CLIENT_ERROR);
	}

	/**
	 * 断言响应状态码在 5xx 范围内。
	 */
	public WebTestClient.ResponseSpec is5xxServerError() {
		return assertSeriesAndReturn(HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * 使用 Hamcrest 匹配器匹配响应状态值。
	 * @param matcher the matcher to use
	 * @since 5.1
	 */
	public WebTestClient.ResponseSpec value(Matcher<? super Integer> matcher) {
		int actual = this.exchangeResult.getRawStatusCode();
		this.exchangeResult.assertWithDiagnostics(() -> MatcherAssert.assertThat("Response status", actual, matcher));
		return this.responseSpec;
	}

	/**
	 * 以整数形式消费响应状态值。
	 * @param consumer the consumer to use
	 * @since 5.1
	 */
	public WebTestClient.ResponseSpec value(Consumer<Integer> consumer) {
		int actual = this.exchangeResult.getRawStatusCode();
		this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(actual));
		return this.responseSpec;
	}


	private WebTestClient.ResponseSpec assertStatusAndReturn(HttpStatus expected) {
		HttpStatus actual = this.exchangeResult.getStatus();
		this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", expected, actual));
		return this.responseSpec;
	}

	private WebTestClient.ResponseSpec assertSeriesAndReturn(HttpStatus.Series expected) {
		HttpStatus status = this.exchangeResult.getStatus();
		this.exchangeResult.assertWithDiagnostics(() ->
				AssertionErrors.assertEquals("Range for response status value " + status, expected, status.series()));
		return this.responseSpec;
	}

}
